package toutouchien.itemsadderadditions.feature.behaviour.builtin;

import dev.lone.itemsadder.api.CustomStack;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.common.annotation.Parameter;
import toutouchien.itemsadderadditions.common.item.ItemCategory;
import toutouchien.itemsadderadditions.common.logging.Log;
import toutouchien.itemsadderadditions.feature.behaviour.BehaviourExecutor;
import toutouchien.itemsadderadditions.feature.behaviour.BehaviourHost;
import toutouchien.itemsadderadditions.feature.behaviour.annotation.Behaviour;
import toutouchien.itemsadderadditions.feature.behaviour.builtin.storage.*;
import toutouchien.itemsadderadditions.feature.behaviour.builtin.storage.listener.*;

import java.util.*;

/**
 * Adds persistent inventory storage to custom blocks and furniture.
 *
 * <p>This class is deliberately limited to configuration and lifecycle. Runtime event
 * handling is split into focused listeners under {@code storage.listeners}; they all
 * share a single {@link StorageRuntime} instance for the loaded behaviour.</p>
 *
 * <h3>Storage modes</h3>
 * <ul>
 *   <li>{@code STORAGE}  - shared container; all players see the same inventory.</li>
 *   <li>{@code SHULKER}  - portable storage; contents are serialised into the dropped item.</li>
 *   <li>{@code DISPOSAL} - trash can; contents are silently discarded on close.</li>
 * </ul>
 */
@SuppressWarnings("unused")
@NullMarked
@Behaviour(key = "storage")
public final class StorageBehaviour extends BehaviourExecutor {
    /**
     * Global set of namespaced IDs using {@link StorageType#SHULKER}; the GUI guard
     * uses it to prevent nesting portable storage items inside each other.
     */
    private static final Set<String> SHULKER_ITEM_IDS = Collections.synchronizedSet(new HashSet<>());
    private final List<Listener> registeredListeners = new ArrayList<>();
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
    private String namespacedID = "";
    @Nullable
    private StorageRuntime runtime;

    @Override
    public boolean configure(Object configData, String namespacedID) {
        if (!super.configure(configData, namespacedID)) return false;
        if (!(configData instanceof ConfigurationSection section)) return false;

        StorageSounds sounds = StorageSoundParser.parse(section, namespacedID);
        if (sounds == null) return false;

        openSound = sounds.open();
        closeSound = sounds.close();
        return true;
    }

    @Override
    protected void onLoad(BehaviourHost host) {
        namespacedID = host.namespacedID();
        JavaPlugin plugin = host.plugin();

        NamespacedKey contentsKey = storageKey(plugin, "storage_");
        NamespacedKey uniqueIdKey = storageKey(plugin, "storage_uid_");
        StorageType storageType = StorageTypes.resolve(typeName, namespacedID);

        OpenVariantConfig resolvedVariantConfig = resolveOpenVariant(host.category());
        @Nullable
        OpenVariantTransformer transformer = resolvedVariantConfig != null
                ? new OpenVariantTransformer(resolvedVariantConfig)
                : null;

        StorageSessionManager sessionManager = new StorageSessionManager(
                rows,
                buildTitle(),
                storageType,
                contentsKey,
                plugin,
                openSound,
                closeSound,
                namespacedID,
                transformer
        );

        ShulkerDropTracker shulkerDropTracker = new ShulkerDropTracker(
                namespacedID,
                contentsKey,
                uniqueIdKey
        );

        runtime = new StorageRuntime(
                plugin,
                namespacedID,
                host.category(),
                storageType,
                contentsKey,
                uniqueIdKey,
                sessionManager,
                shulkerDropTracker,
                resolvedVariantConfig,
                transformer
        );

        StorageInventoryManager.ensureCustomBlockDataRegistered(plugin);
        if (storageType == StorageType.SHULKER) SHULKER_ITEM_IDS.add(namespacedID);

        registerStorageListeners(plugin, runtime, shulkerDropTracker);
    }

    @Override
    protected void onUnload(BehaviourHost host) {
        if (runtime != null) runtime.clear();
        SHULKER_ITEM_IDS.remove(namespacedID);

        registeredListeners.forEach(HandlerList::unregisterAll);
        registeredListeners.clear();
        runtime = null;
    }

    private void registerStorageListeners(
            JavaPlugin plugin,
            StorageRuntime runtime,
            ShulkerDropTracker shulkerDropTracker
    ) {
        StorageGuiGuard guiGuard = new StorageGuiGuard(SHULKER_ITEM_IDS);

        registeredListeners.add(new StorageBlockListener(runtime));
        registeredListeners.add(new StorageFurnitureListener(runtime));
        registeredListeners.add(new StorageInventoryCloseListener(runtime));
        registeredListeners.add(new StorageOpenVariantBlockListener(runtime));
        registeredListeners.add(new StorageOpenVariantFurnitureListener(runtime));
        registeredListeners.add(shulkerDropTracker);
        registeredListeners.add(guiGuard);

        for (Listener listener : registeredListeners) {
            Bukkit.getPluginManager().registerEvents(listener, plugin);
        }
    }

    private NamespacedKey storageKey(JavaPlugin plugin, String prefix) {
        return new NamespacedKey(plugin, prefix + namespacedID.replace(":", "_"));
    }

    @Nullable
    private OpenVariantConfig resolveOpenVariant(ItemCategory holderCategory) {
        OpenVariantConfig resolved = OpenVariantConfig.resolve(openVariant, namespacedID);
        if (resolved == null) return null;
        if (isOpenVariantCompatible(holderCategory, resolved)) return resolved;

        Log.warn(
                "Storage",
                "storage '{}': incompatible open_variant '{}'. Block holders may only use "
                        + "block open_variant values; furniture/complex-furniture holders may "
                        + "only use furniture or item_display values.",
                namespacedID,
                resolved.id()
        );
        return null;
    }

    private boolean isOpenVariantCompatible(ItemCategory holderCategory, OpenVariantConfig config) {
        if (holderCategory == ItemCategory.BLOCK) {
            return config.type() == OpenVariantConfig.FormType.BLOCK;
        }

        return config.type() == OpenVariantConfig.FormType.FURNITURE
                || config.type() == OpenVariantConfig.FormType.ITEM_DISPLAY;
    }

    /**
     * Builds the GUI title component. Falls back to the item's display name, using
     * dark gray when ItemsAdder returns the default white name color.
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
}
