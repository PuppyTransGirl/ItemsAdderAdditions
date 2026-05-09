package toutouchien.itemsadderadditions;

import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import toutouchien.itemsadderadditions.actions.ActionsListener;
import toutouchien.itemsadderadditions.actions.ActionsManager;
import toutouchien.itemsadderadditions.behaviours.BehavioursManager;
import toutouchien.itemsadderadditions.converter.ConverterV100V101;
import toutouchien.itemsadderadditions.converter.ConverterV101V102;
import toutouchien.itemsadderadditions.converter.ConverterV102V106;
import toutouchien.itemsadderadditions.converter.ConverterV106V107;
import toutouchien.itemsadderadditions.creative.CreativeMenuManager;
import toutouchien.itemsadderadditions.listeners.ItemsAdderLoadListener;
import toutouchien.itemsadderadditions.nms.api.NmsManager;
import toutouchien.itemsadderadditions.patches.PatchManager;
import toutouchien.itemsadderadditions.patches.Version;
import toutouchien.itemsadderadditions.recipes.RecipeManager;
import toutouchien.itemsadderadditions.updatechecker.UpdateChecker;
import toutouchien.itemsadderadditions.utils.NamespaceUtils;
import toutouchien.itemsadderadditions.worldgen.FurniturePopulatorWorldListener;

import java.util.List;

/**
 * Main plugin class for ItemsAdder Additions.
 *
 * <p>Manages the lifecycle of all sub-systems and acts as the single shared access point
 * ({@link #instance()}) for other classes that need to reach a manager.
 */
public class ItemsAdderAdditions extends JavaPlugin {
    private static final String MODRINTH_PROJECT_ID = "z7nRcGQf";
    private static final int BSTATS_PLUGIN_ID = 30264;

    private static ItemsAdderAdditions instance;

    private ActionsManager actionsManager;
    private BehavioursManager behavioursManager;
    private CreativeMenuManager creativeMenuManager;
    private RecipeManager recipeManager;

    private Metrics bStats;

    public static ItemsAdderAdditions instance() {
        return instance;
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
        saveDefaultConfig();
        runConverters();

        this.bStats = new Metrics(this, BSTATS_PLUGIN_ID);
        bStats.addCustomChart(new SimplePie("platform", () -> "Other"));

        NamespaceUtils.initVanillaCache();
        NmsManager.initialize(this.getComponentLogger());

        this.actionsManager = new ActionsManager();
        this.behavioursManager = new BehavioursManager();
        this.recipeManager = new RecipeManager(this);

        registerListeners();

        if (NmsManager.instance().handler().creativeMenu() != null) {
            this.creativeMenuManager = new CreativeMenuManager();
            this.creativeMenuManager.setup();
            NmsManager.instance().handler().creativeMenu().injectListeners(this);
        }

        if (this.getConfig().getBoolean("update-checker.enabled", true))
            new UpdateChecker(this, MODRINTH_PROJECT_ID);
    }

    @Override
    public void onDisable() {
        if (this.bStats != null) {
            this.bStats.shutdown();
        }
        getServer().getScheduler().cancelTasks(this);
        NmsManager.shutdown();
    }

    /**
     * Reloads the plugin configuration from disk.
     *
     * <p>Note: sub-system data (actions, behaviours, recipes, etc.) is reloaded
     * automatically by {@link ItemsAdderLoadListener} when ItemsAdder fires
     * {@code ItemsAdderLoadDataEvent}. Call this method only when you want to
     * refresh the raw {@code config.yml} values without triggering a full reload.
     */
    public void reload() {
        this.reloadConfig();
    }

    /**
     * Returns the actions sub-system manager.
     */
    public ActionsManager actionsManager() {
        return actionsManager;
    }

    /**
     * Returns the behaviours sub-system manager.
     */
    public BehavioursManager behavioursManager() {
        return behavioursManager;
    }

    /**
     * Returns the creative menu manager, or {@code null} if NMS support is unavailable.
     */
    @org.jspecify.annotations.Nullable
    public CreativeMenuManager creativeMenuManager() {
        return creativeMenuManager;
    }

    /**
     * Returns the recipe manager.
     */
    public RecipeManager recipeManager() {
        return recipeManager;
    }

    private void runConverters() {
        ConverterV100V101.run();
        ConverterV101V102.run();
        ConverterV102V106.run();
        ConverterV106V107.run();
    }

    private void registerListeners() {
        PluginManager pm = getServer().getPluginManager();

        List.of(
                new ActionsListener(),
                new FurniturePopulatorWorldListener(),
                new ItemsAdderLoadListener()
        ).forEach(listener -> pm.registerEvents(listener, this));
    }
}
