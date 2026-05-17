package toutouchien.itemsadderadditions.feature.behaviour.builtin;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.common.item.ItemCategory;
import toutouchien.itemsadderadditions.common.logging.Log;
import toutouchien.itemsadderadditions.feature.behaviour.BehaviourExecutor;
import toutouchien.itemsadderadditions.feature.behaviour.BehaviourHost;
import toutouchien.itemsadderadditions.feature.behaviour.annotation.Behaviour;
import toutouchien.itemsadderadditions.feature.behaviour.builtin.textdisplay.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Behaviour that attaches packet-based text displays to a block or furniture item.
 * <p>
 * Reads its configuration via {@link TextDisplayConfigLoader} and delegates all
 * runtime management to {@link TextDisplayRuntime}.
 */
@SuppressWarnings("unused")
@NullMarked
@Behaviour(key = "text_display")
public final class TextDisplayBehaviour extends BehaviourExecutor {
    private static final String LOG_TAG = "TextDisplay";

    private final List<Listener> registeredListeners = new ArrayList<>();

    private List<TextDisplaySpec> specs = List.of();
    @Nullable
    private TextDisplayRuntime runtime;

    @Override
    public boolean configure(Object configData, String namespacedID) {
        if (!(configData instanceof ConfigurationSection section)) {
            Log.warn(LOG_TAG, "text_display '{}': config must be a section.", namespacedID);
            return false;
        }

        specs = TextDisplayConfigLoader.load(section, namespacedID);
        if (specs.isEmpty()) {
            Log.warn(LOG_TAG, "text_display '{}': no valid displays loaded; skipping behaviour.", namespacedID);
            return false;
        }
        return true;
    }

    @Override
    protected void onLoad(BehaviourHost host) {
        if (!isSupportedCategory(host.category())) {
            Log.warn(LOG_TAG, "text_display '{}': unsupported holder category '{}'. Only block, furniture, and complex furniture holders are supported.", host.namespacedID(), host.category());
            return;
        }

        TextDisplayRuntime createdRuntime = new TextDisplayRuntime(host.plugin(), host.namespacedID(), host.category(), specs);
        runtime = createdRuntime;

        if (host.category() == ItemCategory.BLOCK) {
            registeredListeners.add(new TextDisplayBlockListener(createdRuntime));
        } else {
            registeredListeners.add(new TextDisplayFurnitureListener(createdRuntime));
        }
        registeredListeners.add(new TextDisplayChunkListener(createdRuntime));
        registeredListeners.add(new TextDisplayPlayerListener(host.plugin(), createdRuntime));

        for (Listener listener : registeredListeners) {
            Bukkit.getPluginManager().registerEvents(listener, host.plugin());
        }

        createdRuntime.start();
    }

    @Override
    protected void onUnload(BehaviourHost host) {
        if (runtime != null) {
            runtime.stop();
            runtime.destroyAll();
            runtime = null;
        }

        registeredListeners.forEach(HandlerList::unregisterAll);
        registeredListeners.clear();
    }

    private boolean isSupportedCategory(ItemCategory category) {
        return category == ItemCategory.BLOCK
                || category == ItemCategory.FURNITURE
                || category == ItemCategory.COMPLEX_FURNITURE;
    }
}
