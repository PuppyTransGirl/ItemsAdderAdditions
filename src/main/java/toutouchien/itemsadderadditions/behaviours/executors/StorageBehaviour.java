package toutouchien.itemsadderadditions.behaviours.executors;

import dev.lone.itemsadder.api.CustomBlock;
import dev.lone.itemsadder.api.CustomStack;
import dev.lone.itemsadder.api.Events.*;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.annotations.Parameter;
import toutouchien.itemsadderadditions.behaviours.BehaviourExecutor;
import toutouchien.itemsadderadditions.behaviours.BehaviourHost;
import toutouchien.itemsadderadditions.behaviours.annotations.Behaviour;
import toutouchien.itemsadderadditions.behaviours.executors.storage.*;
import toutouchien.itemsadderadditions.utils.SoundUtils;
import toutouchien.itemsadderadditions.utils.other.ItemCategory;
import toutouchien.itemsadderadditions.utils.other.Log;

import java.util.*;

/**
 * Adds persistent inventory storage to custom blocks and furniture.
 *
 * <h3>Storage modes (the {@code type} parameter)</h3>
 * <ul>
 *   <li>{@code STORAGE}  - shared container; all players see the same inventory.</li>
 *   <li>{@code SHULKER}  - portable storage; contents are serialised into the dropped
 *       item and restored when re-placed.</li>
 *   <li>{@code DISPOSAL} - trash can; contents are silently discarded on close.</li>
 * </ul>
 *
 * <h3>Open variant ({@code open_variant} parameter)</h3>
 * When an {@code open_variant} ID is provided, the behaviour swaps the block or furniture
 * for a different model when any player has the GUI open, and restores the original when
 * the last session closes.
 *
 * <h3>Minimal YAML example</h3>
 * <pre>{@code
 * behaviours:
 *   storage:
 *     type: STORAGE
 *     rows: 3
 *     title: "<dark_gray>My Chest"
 *     open_variant: "my_pack:my_chest_open"
 *     open_sound:
 *       name: "minecraft:block.chest.open"
 *     close_sound:
 *       name: "minecraft:block.chest.close"
 * }</pre>
 */
@SuppressWarnings("unused")
@NullMarked
@Behaviour(key = "storage")
public final class StorageBehaviour extends BehaviourExecutor implements Listener {
    /**
     * Global set of all namespaced IDs that have {@link StorageType#SHULKER} storage.
     * Shared across all instances so {@link StorageGuiGuard} can block nesting of any shulker-type item.
     */
    private static final Set<String> SHULKER_ITEM_IDS =
            Collections.synchronizedSet(new HashSet<>());

    /**
     * Contents pre-loaded from blocks before CustomBlockData's {@code MONITOR} listener
     * deletes the PDC on break.
     */
    private final Map<BlockCoord, ItemStack[]> preloadedBlockContents = new HashMap<>();

    @Parameter(key = "type", type = String.class, required = true)
    private String typeName;

    @Parameter(key = "rows", type = Integer.class, min = 1, max = 6)
    private int rows = 3;

    @Parameter(key = "title", type = String.class)
    @Nullable
    private String titleRaw;

    @Parameter(key = "open_variant", type = String.class)
    @Nullable
    private String openVariant;

    @Nullable
    private Sound openSound;

    @Nullable
    private Sound closeSound;

    // Resolved during onLoad
    private StorageType storageType = StorageType.STORAGE;
    private String namespacedID = "";
    private ItemCategory category = ItemCategory.BLOCK;
    private NamespacedKey contentsKey;
    private JavaPlugin plugin;
    private StorageSessionManager sessionManager;
    private ShulkerDropTracker shulkerDropTracker;
    private StorageGuiGuard guiGuard;

    @Nullable
    private OpenVariantConfig openVariantConfig;

    /**
     * Non-null when {@code open_variant} is configured and compatible with the holder's category.
     */
    @Nullable
    private OpenVariantTransformer openVariantTransformer;

    /**
     * Drops items at {@code loc} if {@code contents} is non-null and contains non-air stacks.
     */
    private static void dropItems(Location loc, @Nullable ItemStack @Nullable [] contents) {
        if (contents == null) return;

        for (ItemStack item : contents) {
            if (item != null && item.getType() != org.bukkit.Material.AIR) {
                loc.getWorld().dropItemNaturally(loc, item);
            }
        }
    }

    /**
     * Parses an Adventure {@link Sound} from a named sub-section of {@code section}.
     *
     * <table>
     *   <tr><th>Condition</th><th>Result</th></tr>
     *   <tr><td>Key absent</td><td>{@link SoundParseResult#absent()}</td></tr>
     *   <tr><td>Section present, valid</td><td>{@link SoundParseResult#ok(Sound)}</td></tr>
     *   <tr><td>Section present, invalid</td>
     *       <td>{@link SoundParseResult#malformed()} (error logged when applicable)</td></tr>
     * </table>
     */
    private static SoundParseResult parseSoundField(
            ConfigurationSection section,
            String key,
            String namespacedID
    ) {
        if (!section.contains(key)) return SoundParseResult.absent();

        ConfigurationSection soundSection = section.getConfigurationSection(key);
        Sound parsed = SoundUtils.parseSound(soundSection);
        if (parsed != null) return SoundParseResult.ok(parsed);

        // Section present but parse failed - only warn when the source string is the culprit.
        if (soundSection != null) {
            String src = soundSection.getString("source", "");
            if (!src.isBlank() && SoundUtils.parseSource(src) == null) {
                Log.warn(
                        "Behaviours",
                        "storage '{}': invalid {} source '{}' - valid values: "
                                + "master, music, record, weather, block, hostile, neutral, "
                                + "player, ambient, voice, ui",
                        namespacedID,
                        key,
                        src
                );
            }
        }

        return SoundParseResult.malformed();
    }

    /**
     * Resolves the {@link StorageType} from its name, defaulting to
     * {@link StorageType#STORAGE} and logging a warning when the name is unrecognised.
     */
    private static StorageType resolveStorageType(String typeName, String namespacedID) {
        try {
            return StorageType.valueOf(typeName.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            Log.warn(
                    "Storage",
                    "Unknown type '{}' for '{}'. Defaulting to STORAGE.",
                    typeName,
                    namespacedID
            );
            return StorageType.STORAGE;
        }
    }

    @Override
    public boolean configure(Object configData, String namespacedID) {
        if (!super.configure(configData, namespacedID)) return false;
        if (!(configData instanceof ConfigurationSection section)) return false;

        SoundParseResult openResult = parseSoundField(section, "open_sound", namespacedID);
        if (openResult.status() == SoundParseStatus.MALFORMED) return false;
        openSound = openResult.sound();

        SoundParseResult closeResult = parseSoundField(section, "close_sound", namespacedID);
        if (closeResult.status() == SoundParseStatus.MALFORMED) return false;
        closeSound = closeResult.sound();

        return true;
    }

    @Override
    protected void onLoad(BehaviourHost host) {
        this.namespacedID = host.namespacedID();
        this.category = host.category();
        this.plugin = host.plugin();

        contentsKey = new NamespacedKey(plugin, "storage_" + namespacedID.replace(":", "_"));
        NamespacedKey uniqueIdKey = new NamespacedKey(
                plugin,
                "storage_uid_" + namespacedID.replace(":", "_")
        );

        storageType = resolveStorageType(typeName, namespacedID);

        OpenVariantConfig resolvedVariantConfig =
                OpenVariantConfig.resolve(openVariant, namespacedID);
        if (resolvedVariantConfig != null) {
            if (isOpenVariantCompatible(category, resolvedVariantConfig)) {
                this.openVariantConfig = resolvedVariantConfig;
                this.openVariantTransformer =
                        new OpenVariantTransformer(resolvedVariantConfig);
            } else {
                Log.warn(
                        "Storage",
                        "storage '{}': incompatible open_variant '{}'. "
                                + "Block holders may only use block open_variant values; "
                                + "furniture/complex-furniture holders may only use furniture or "
                                + "item_display values.",
                        namespacedID,
                        resolvedVariantConfig.id()
                );
            }
        }

        sessionManager = new StorageSessionManager(
                rows,
                buildTitle(),
                storageType,
                contentsKey,
                plugin,
                openSound,
                closeSound,
                namespacedID,
                openVariantTransformer
        );

        shulkerDropTracker = new ShulkerDropTracker(namespacedID, contentsKey, uniqueIdKey);
        guiGuard = new StorageGuiGuard(SHULKER_ITEM_IDS);

        StorageInventoryManager.ensureCustomBlockDataRegistered(plugin);

        if (storageType == StorageType.SHULKER) SHULKER_ITEM_IDS.add(namespacedID);

        Bukkit.getPluginManager().registerEvents(this, plugin);
        Bukkit.getPluginManager().registerEvents(shulkerDropTracker, plugin);
        Bukkit.getPluginManager().registerEvents(guiGuard, plugin);
    }

    @Override
    protected void onUnload(BehaviourHost host) {
        if (sessionManager != null) sessionManager.clear();
        if (shulkerDropTracker != null) shulkerDropTracker.clear();

        preloadedBlockContents.clear();

        if (openVariantTransformer != null) openVariantTransformer.clear();

        SHULKER_ITEM_IDS.remove(namespacedID);

        HandlerList.unregisterAll(this);
        if (shulkerDropTracker != null) HandlerList.unregisterAll(shulkerDropTracker);
        if (guiGuard != null) HandlerList.unregisterAll(guiGuard);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockInteract(PlayerInteractEvent event) {
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getPlayer().isSneaking()) return;

        Block block = event.getClickedBlock();
        if (block == null) return;

        CustomBlock cb = CustomBlock.byAlreadyPlaced(block);
        if (cb == null || !cb.getNamespacedID().equals(namespacedID)) return;

        event.setCancelled(true);
        sessionManager.openForBlock(event.getPlayer(), block);
    }

    /**
     * Pre-loads block contents before CustomBlockData's {@code MONITOR} listener deletes them.
     * Also stops the break if a GUI session is currently live at this location.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreakPreLoad(BlockBreakEvent event) {
        if (category != ItemCategory.BLOCK) return;

        Block block = event.getBlock();

        // Let the open-variant handler deal with breaks on the open-form block.
        if (openVariantTransformer != null
                && openVariantConfig != null
                && !openVariantConfig.isFurnitureBased()
                && openVariantTransformer.isTransformed(block.getLocation())) {
            CustomBlock cb = CustomBlock.byAlreadyPlaced(block);
            if (cb != null && cb.getNamespacedID().equals(openVariantConfig.id())) return;
        }

        CustomBlock cb = CustomBlock.byAlreadyPlaced(block);
        if (cb == null || !cb.getNamespacedID().equals(namespacedID)) return;

        ItemStack[] contents =
                StorageInventoryManager.loadFromBlock(block, contentsKey, plugin);
        if (contents != null) {
            preloadedBlockContents.put(BlockCoord.of(block.getLocation()), contents);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(CustomBlockBreakEvent event) {
        if (!event.getNamespacedID().equals(namespacedID)) return;

        Block block = event.getBlock();
        sessionManager.closeSessionsAt(block.getLocation(), preloadedBlockContents);

        ItemStack[] contents =
                preloadedBlockContents.remove(BlockCoord.of(block.getLocation()));

        if (storageType == StorageType.STORAGE) {
            dropItems(block.getLocation(), contents);
        } else if (storageType == StorageType.SHULKER && contents != null) {
            shulkerDropTracker.stageDrop(block.getLocation(), contents);
        }

        StorageInventoryManager.clearBlock(block, plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(CustomBlockPlaceEvent event) {
        if (!event.getNamespacedID().equals(namespacedID)) return;
        if (storageType != StorageType.SHULKER) return;

        ItemStack[] stored = extractFromHand(event.getPlayer());
        if (stored == null) return;

        StorageInventoryManager.saveToBlock(event.getBlock(), stored, contentsKey, plugin);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFurnitureInteract(FurnitureInteractEvent event) {
        if (!event.getNamespacedID().equals(namespacedID)) return;
        if (event.getPlayer().isSneaking()) return;

        event.setCancelled(true);
        sessionManager.openForEntity(event.getPlayer(), event.getBukkitEntity());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFurnitureBreak(FurnitureBreakEvent event) {
        if (!event.getNamespacedID().equals(namespacedID)) return;

        Entity entity = event.getBukkitEntity();
        sessionManager.closeSessionsAt(entity.getLocation(), null);

        ItemStack[] contents =
                StorageInventoryManager.loadFromEntity(entity, contentsKey);

        if (storageType == StorageType.STORAGE) {
            dropItems(entity.getLocation(), contents);
        } else if (storageType == StorageType.SHULKER && contents != null) {
            shulkerDropTracker.stageDrop(entity.getLocation(), contents);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFurniturePlaced(FurniturePlacedEvent event) {
        if (!event.getNamespacedID().equals(namespacedID)) return;
        if (storageType != StorageType.SHULKER) return;

        ItemStack[] stored =
                shulkerDropTracker.consumePlaceContents(event.getPlayer().getUniqueId());
        if (stored == null) return;

        StorageInventoryManager.saveToEntity(event.getBukkitEntity(), stored, contentsKey);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (!(event.getInventory().getHolder(false) instanceof StorageInventoryHolder holder)) {
            return;
        }

        StorageSession session = sessionManager.remove(player.getUniqueId());
        if (session == null) return;

        if (session.type() == StorageType.DISPOSAL) {
            sessionManager.executeClose(holder.location(), true);
            return;
        }

        sessionManager.saveSessionContents(session, true);
    }

    /**
     * Player right-clicks the open-form <em>block</em>.
     * Opens the storage GUI without triggering the transformer again.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onOpenVariantBlockInteract(PlayerInteractEvent event) {
        if (openVariantTransformer == null || openVariantConfig == null) return;
        if (openVariantConfig.isFurnitureBased()) return;
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getPlayer().isSneaking()) return;

        Block block = event.getClickedBlock();
        if (block == null) return;

        CustomBlock cb = CustomBlock.byAlreadyPlaced(block);
        if (cb == null || !cb.getNamespacedID().equals(openVariantConfig.id())) return;
        if (!openVariantTransformer.isTransformed(block.getLocation())) return;

        event.setCancelled(true);
        sessionManager.openForPlayerAtTransformedLocation(
                event.getPlayer(),
                block.getLocation(),
                block,
                null
        );
    }

    /**
     * Pre-loads block contents for the open-form block before CustomBlockData wipes them.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onOpenVariantBlockBreakPreLoad(BlockBreakEvent event) {
        if (openVariantTransformer == null || openVariantConfig == null) return;
        if (openVariantConfig.isFurnitureBased()) return;

        Block block = event.getBlock();
        if (!openVariantTransformer.isTransformed(block.getLocation())) return;

        CustomBlock cb = CustomBlock.byAlreadyPlaced(block);
        if (cb == null || !cb.getNamespacedID().equals(openVariantConfig.id())) return;

        // Prefer live GUI contents (authoritative); fall back to stored block data.
        ItemStack[] live = sessionManager.getLiveContentsAt(block.getLocation());
        ItemStack[] contents = live != null
                ? live
                : StorageInventoryManager.loadFromBlock(block, contentsKey, plugin);

        if (contents != null) {
            preloadedBlockContents.put(BlockCoord.of(block.getLocation()), contents);
        }
    }

    /**
     * The open-form <em>block</em> was broken.
     * Closes sessions, cancels the variant drop, and drops the original item
     * (with contents for SHULKER; as loot for STORAGE; nothing for DISPOSAL).
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onOpenVariantBlockBreak(CustomBlockBreakEvent event) {
        if (openVariantTransformer == null || openVariantConfig == null) return;
        if (!event.getNamespacedID().equals(openVariantConfig.id())) return;

        Block block = event.getBlock();
        if (!openVariantTransformer.isTransformed(block.getLocation())) return;

        ItemStack[] contents =
                preloadedBlockContents.remove(BlockCoord.of(block.getLocation()));

        sessionManager.closeSessionsAt(block.getLocation(), null);
        openVariantTransformer.forceRemove(block.getLocation());

        handleOpenVariantBreakDrops(block.getLocation(), contents, event.getPlayer());
        StorageInventoryManager.clearBlock(block, plugin);
    }

    /**
     * Player right-clicks the open-form <em>furniture</em>.
     * Opens the storage GUI without triggering the transformer again.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onOpenVariantFurnitureInteract(FurnitureInteractEvent event) {
        if (openVariantTransformer == null || openVariantConfig == null) return;
        if (!openVariantConfig.isFurnitureBased()) return;
        if (!event.getNamespacedID().equals(openVariantConfig.id())) return;
        if (event.getPlayer().isSneaking()) return;

        Entity entity = event.getBukkitEntity();
        if (!openVariantTransformer.isTransformed(entity.getLocation())) return;

        event.setCancelled(true);
        sessionManager.openForPlayerAtTransformedLocation(
                event.getPlayer(),
                entity.getLocation(),
                null,
                entity
        );
    }

    /**
     * The open-form <em>furniture</em> was broken.
     * Closes all sessions and drops the original item with its contents.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onOpenVariantFurnitureBreak(FurnitureBreakEvent event) {
        if (openVariantTransformer == null || openVariantConfig == null) return;
        if (!event.getNamespacedID().equals(openVariantConfig.id())) return;

        Entity entity = event.getBukkitEntity();
        if (!openVariantTransformer.isTransformed(entity.getLocation())) return;

        // Live contents from any open session are authoritative.
        ItemStack[] contents = sessionManager.getLiveContentsAt(entity.getLocation());

        sessionManager.closeSessionsAt(entity.getLocation(), null);
        openVariantTransformer.forceRemove(entity.getLocation());

        handleOpenVariantBreakDrops(entity.getLocation(), contents, event.getPlayer());
    }

    /**
     * Handles item drops after an open-form block or furniture is broken.
     *
     * <ul>
     *   <li>{@link StorageType#STORAGE}  - contents scattered; original item dropped clean.</li>
     *   <li>{@link StorageType#SHULKER}  - contents injected into the dropped original item.</li>
     *   <li>{@link StorageType#DISPOSAL} - nothing dropped.</li>
     * </ul>
     */
    private void handleOpenVariantBreakDrops(
            Location loc,
            @Nullable ItemStack[] contents,
            @Nullable Player breaker
    ) {
        if (storageType == StorageType.DISPOSAL) return;

        CustomStack original = CustomStack.getInstance(namespacedID);
        if (original == null) {
            Log.warn(
                    "StorageBehaviour",
                    "Could not find original CustomStack '{}' to drop after open-form break.",
                    namespacedID
            );
            return;
        }

        ItemStack drop = original.getItemStack();

        if (storageType == StorageType.SHULKER && contents != null) {
            NamespacedKey uidKey = new NamespacedKey(
                    plugin,
                    "storage_uid_" + namespacedID.replace(":", "_")
            );
            StorageInventoryManager.injectIntoItem(drop, contents, contentsKey);
            StorageInventoryManager.stampUniqueId(drop, uidKey);
        } else if (storageType == StorageType.STORAGE) {
            dropItems(loc, contents);
        }

        loc.getWorld().dropItemNaturally(loc, drop);
    }

    /**
     * Returns {@code true} if {@code openVariantConfig} is compatible with the given holder
     * category. Block holders may only use block variants; furniture holders may use
     * furniture or item_display variants.
     */
    private boolean isOpenVariantCompatible(
            ItemCategory holderCategory,
            OpenVariantConfig cfg
    ) {
        if (holderCategory == ItemCategory.BLOCK) {
            return cfg.type() == OpenVariantConfig.FormType.BLOCK;
        }

        return cfg.type() == OpenVariantConfig.FormType.FURNITURE
                || cfg.type() == OpenVariantConfig.FormType.ITEM_DISPLAY;
    }

    /**
     * Builds the GUI title component.
     * Falls back to the item's display name (with a dark-gray colour when the original is white).
     */
    private Component buildTitle() {
        if (titleRaw != null) {
            return MiniMessage.miniMessage().deserialize(titleRaw);
        }

        Component component = CustomStack.getInstance(namespacedID).itemName();
        return component.color() == NamedTextColor.WHITE
                ? component.color(NamedTextColor.DARK_GRAY)
                : component;
    }

    /**
     * Tries to extract shulker contents from the player's main-hand item, then offhand.
     *
     * @return the stored content array, or {@code null} if neither hand carries any
     */
    @Nullable
    private ItemStack[] extractFromHand(Player player) {
        ItemStack[] stored = StorageInventoryManager.extractFromItem(
                player.getInventory().getItemInMainHand(),
                contentsKey
        );
        if (stored != null) return stored;

        return StorageInventoryManager.extractFromItem(
                player.getInventory().getItemInOffHand(),
                contentsKey
        );
    }

    private enum SoundParseStatus {
        ABSENT,
        OK,
        MALFORMED
    }

    /**
     * Result type for {@link #parseSoundField}.
     *
     * <ul>
     *   <li>{@link #status()} = {@link SoundParseStatus#ABSENT} - section absent</li>
     *   <li>{@link #status()} = {@link SoundParseStatus#OK} - section present and parsed</li>
     *   <li>{@link #status()} = {@link SoundParseStatus#MALFORMED} - section present but invalid;
     *       error already logged</li>
     * </ul>
     */
    private record SoundParseResult(@Nullable Sound sound, SoundParseStatus status) {
        static SoundParseResult absent() {
            return new SoundParseResult(null, SoundParseStatus.ABSENT);
        }

        static SoundParseResult ok(Sound sound) {
            return new SoundParseResult(sound, SoundParseStatus.OK);
        }

        static SoundParseResult malformed() {
            return new SoundParseResult(null, SoundParseStatus.MALFORMED);
        }
    }
}
