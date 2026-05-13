package toutouchien.itemsadderadditions.plugin;

import net.momirealms.antigrieflib.AntiGriefLib;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.feature.action.ActionsManager;
import toutouchien.itemsadderadditions.feature.behaviour.BehavioursManager;
import toutouchien.itemsadderadditions.feature.creative.CreativeMenuManager;
import toutouchien.itemsadderadditions.feature.painting.CustomPaintingManager;
import toutouchien.itemsadderadditions.feature.recipe.RecipeManager;
import toutouchien.itemsadderadditions.patch.PatchManager;
import toutouchien.itemsadderadditions.patch.Version;
import toutouchien.itemsadderadditions.runtime.PluginRuntime;
import toutouchien.itemsadderadditions.runtime.reload.ReloadCoordinator;
import toutouchien.itemsadderadditions.runtime.reload.ReloadResult;
import toutouchien.itemsadderadditions.settings.PluginSettings;

import java.util.Objects;

/**
 * Bukkit entrypoint for ItemsAdderAdditions.
 *
 * <p>This class intentionally stays thin. Runtime services, listener
 * registration and reload orchestration are owned by {@link PluginRuntime}; the
 * entrypoint only handles Bukkit lifecycle callbacks and keeps the public access
 * methods used by the rest of the plugin.</p>
 */
public class ItemsAdderAdditions extends JavaPlugin {
    private static ItemsAdderAdditions instance;

    private @Nullable PluginRuntime runtime;

    public static ItemsAdderAdditions instance() {
        return Objects.requireNonNull(instance, "ItemsAdderAdditions has not been loaded yet");
    }

    @Override
    public void onLoad() {
        instance = this;

        Plugin itemsAdder = getServer().getPluginManager().getPlugin("ItemsAdder");
        if (itemsAdder == null) {
            throw new IllegalStateException("ItemsAdder must be installed before ItemsAdderAdditions can load.");
        }

        Version version = Version.of(
                Bukkit.getMinecraftVersion(),
                itemsAdder.getPluginMeta().getVersion()
        );

        PatchManager.applyAll(version);
    }

    @Override
    public void onEnable() {
        this.runtime = new PluginRuntime(this);
        runtime.enable();
    }

    @Override
    public void onDisable() {
        if (runtime != null) {
            runtime.disable();
            runtime = null;
        }
    }

    /**
     * Reloads {@code config.yml}, reapplies runtime settings, and reloads all
     * ItemsAdder-backed systems using the same path as the ItemsAdder load event.
     */
    public ReloadResult reload() {
        return runtime().reload();
    }

    public ReloadResult reloadItemsAdderData() {
        return runtime().reloadItemsAdderData();
    }

    public PluginSettings settings() {
        return runtime().settings();
    }

    public ReloadCoordinator reloadCoordinator() {
        return runtime().reloadCoordinator();
    }

    public ActionsManager actionsManager() {
        return runtime().actionsManager();
    }

    public BehavioursManager behavioursManager() {
        return runtime().behavioursManager();
    }

    public @Nullable CreativeMenuManager creativeMenuManager() {
        return runtime().creativeMenuManager();
    }

    public CustomPaintingManager customPaintingManager() {
        return runtime().customPaintingManager();
    }

    public RecipeManager recipeManager() {
        return runtime().recipeManager();
    }

    public AntiGriefLib antiGriefLib() {
        return runtime().antiGriefLib();
    }

    private PluginRuntime runtime() {
        return Objects.requireNonNull(runtime, "ItemsAdderAdditions runtime is not enabled");
    }
}
