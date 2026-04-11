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

@SuppressWarnings("unused")
@NullMarked
@Behaviour(key = "storage")
public final class StorageBehaviour extends BehaviourExecutor implements Listener {
    private static final Set<String> SHULKER_ITEM_IDS = Collections.synchronizedSet(new HashSet<>());
    /**
     * Block contents pre-loaded before CustomBlockData's MONITOR listener deletes PDC on break.
     */
    private final Map<BlockCoord, ItemStack[]> preloadedBlockContents = new HashMap<>();

    @Parameter(key = "type", type = String.class, required = true) private String typeName;
    @Parameter(key = "rows", type = Integer.class, min = 1, max = 6) private int rows = 3;
    @Parameter(key = "title", type = String.class) @Nullable private String titleRaw;

    @Nullable private Sound openSound;
    @Nullable private Sound closeSound;

    private StorageType storageType = StorageType.STORAGE;
    private String namespacedID = "";
    private ItemCategory category = ItemCategory.BLOCK;
    private NamespacedKey contentsKey;
    private JavaPlugin plugin;
    private StorageSessionManager sessionManager;
    private ShulkerDropTracker shulkerDropTracker;
    private StorageGuiGuard guiGuard;

    private static void dropItems(Location loc, @Nullable ItemStack @Nullable [] contents) {
        if (contents == null) return;
        for (ItemStack item : contents)
            if (item != null && item.getType() != org.bukkit.Material.AIR)
                loc.getWorld().dropItemNaturally(loc, item);
    }

    @Override
    public boolean configure(Object configData, String namespacedID) {
        if (!super.configure(configData, namespacedID)) return false;
        if (!(configData instanceof ConfigurationSection section)) return false;

        if (section.contains("open_sound")) {
            ConfigurationSection soundSection = section.getConfigurationSection("open_sound");
            Sound parsed = SoundUtils.parseSound(soundSection);
            if (parsed == null && soundSection != null) {
                String src = soundSection.getString("source", "");
                if (!src.isBlank() && SoundUtils.parseSource(src) == null) {
                    Log.warn("Behaviours", "storage: invalid open_sound source '{}' - valid values: master, music, record, weather, block, hostile, neutral, player, ambient, voice, ui", src);
                    return false;
                }
            }

            openSound = parsed;
        }

        if (section.contains("close_sound")) {
            ConfigurationSection soundSection = section.getConfigurationSection("close_sound");
            Sound parsed = SoundUtils.parseSound(soundSection);
            if (parsed == null && soundSection != null) {
                String src = soundSection.getString("source", "");
                if (!src.isBlank() && SoundUtils.parseSource(src) == null) {
                    Log.warn("Behaviours", "storage: invalid close_sound source '{}' - valid values: master, music, record, weather, block, hostile, neutral, player, ambient, voice, ui", src);
                    return false;
                }
            }

            closeSound = parsed;
        }

        return true;
    }

    @Override
    protected void onLoad(BehaviourHost host) {
        this.namespacedID = host.namespacedID();
        this.category = host.category();
        this.plugin = host.plugin();

        contentsKey = new NamespacedKey(plugin, "storage_" + namespacedID.replace(":", "_"));
        NamespacedKey uniqueIdKey =
                new NamespacedKey(plugin, "storage_uid_" + namespacedID.replace(":", "_"));

        try {
            storageType = StorageType.valueOf(typeName.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            Log.warn("Storage",
                    "Unknown type '{}' for '{}'. Defaulting to STORAGE.", typeName, namespacedID);
        }

        Component title = buildTitle();

        sessionManager = new StorageSessionManager(rows, title, storageType, contentsKey, plugin, openSound, closeSound);
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
        sessionManager.clear();
        shulkerDropTracker.clear();
        preloadedBlockContents.clear();
        SHULKER_ITEM_IDS.remove(namespacedID);
        HandlerList.unregisterAll(this);
        HandlerList.unregisterAll(shulkerDropTracker);
        HandlerList.unregisterAll(guiGuard);
    }

    private Component buildTitle() {
        if (titleRaw != null)
            return MiniMessage.miniMessage().deserialize(titleRaw);

        Component component = CustomStack.getInstance(namespacedID).itemName();
        return component.color() == NamedTextColor.WHITE
                ? component.color(NamedTextColor.DARK_GRAY)
                : component;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockInteract(PlayerInteractEvent event) {
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (category != ItemCategory.BLOCK) return;
        if (event.getPlayer().isSneaking()) return;

        Block block = event.getClickedBlock();
        if (block == null) return;

        CustomBlock cb = CustomBlock.byAlreadyPlaced(block);
        if (cb == null || !cb.getNamespacedID().equals(namespacedID)) return;

        event.setCancelled(true);
        sessionManager.openForBlock(event.getPlayer(), block);
    }

    /**
     * Pre-loads contents before CustomBlockData's MONITOR listener deletes them.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreakPreLoad(BlockBreakEvent event) {
        if (category != ItemCategory.BLOCK) return;

        Block block = event.getBlock();
        CustomBlock cb = CustomBlock.byAlreadyPlaced(block);
        if (cb == null || !cb.getNamespacedID().equals(namespacedID)) return;

        ItemStack[] contents = StorageInventoryManager.loadFromBlock(block, contentsKey, plugin);
        if (contents != null)
            preloadedBlockContents.put(BlockCoord.of(block.getLocation()), contents);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(CustomBlockBreakEvent event) {
        if (!event.getNamespacedID().equals(namespacedID)) return;

        Block block = event.getBlock();
        sessionManager.closeSessionsAt(block.getLocation(), preloadedBlockContents);

        ItemStack[] contents = preloadedBlockContents.remove(BlockCoord.of(block.getLocation()));

        if (storageType == StorageType.STORAGE)
            dropItems(block.getLocation(), contents);
        else if (storageType == StorageType.SHULKER && contents != null)
            shulkerDropTracker.stageDrop(block.getLocation(), contents);

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

        ItemStack[] contents = StorageInventoryManager.loadFromEntity(entity, contentsKey);

        if (storageType == StorageType.STORAGE)
            dropItems(entity.getLocation(), contents);
        else if (storageType == StorageType.SHULKER && contents != null)
            shulkerDropTracker.stageDrop(entity.getLocation(), contents);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFurniturePlaced(FurniturePlacedEvent event) {
        if (!event.getNamespacedID().equals(namespacedID)) return;
        if (storageType != StorageType.SHULKER) return;

        ItemStack[] stored = shulkerDropTracker.consumePlaceContents(event.getPlayer().getUniqueId());
        if (stored == null) return;

        StorageInventoryManager.saveToEntity(event.getBukkitEntity(), stored, contentsKey);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (!(event.getInventory().getHolder(false) instanceof StorageInventoryHolder)) return;

        StorageSession session = sessionManager.remove(player.getUniqueId());
        if (session == null) return;
        if (session.type() == StorageType.DISPOSAL) return;

        sessionManager.saveSessionContents(session, true);
    }

    @Nullable
    private ItemStack[] extractFromHand(Player player) {
        ItemStack[] stored = StorageInventoryManager.extractFromItem(
                player.getInventory().getItemInMainHand(), contentsKey);

        if (stored != null) return stored;
        return StorageInventoryManager.extractFromItem(
                player.getInventory().getItemInOffHand(), contentsKey);
    }
}
