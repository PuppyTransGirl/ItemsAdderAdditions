package toutouchien.itemsadderadditions;

import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import toutouchien.itemsadderadditions.actions.ActionsListener;
import toutouchien.itemsadderadditions.actions.ActionsManager;
import toutouchien.itemsadderadditions.behaviours.BehavioursManager;
import toutouchien.itemsadderadditions.components.ComponentsManager;
import toutouchien.itemsadderadditions.converter.ConverterV100V101;
import toutouchien.itemsadderadditions.converter.ConverterV101V102;
import toutouchien.itemsadderadditions.converter.ConverterV105V106;
import toutouchien.itemsadderadditions.creative.CreativeMenuManager;
import toutouchien.itemsadderadditions.listeners.ItemsAdderLoadListener;
import toutouchien.itemsadderadditions.nms.api.NmsManager;
import toutouchien.itemsadderadditions.patches.PatchManager;
import toutouchien.itemsadderadditions.patches.Version;
import toutouchien.itemsadderadditions.recipes.RecipeManager;
import toutouchien.itemsadderadditions.updatechecker.UpdateChecker;

import java.util.List;

public class ItemsAdderAdditions extends JavaPlugin {
    private static final String MODRINTH_PROJECT_ID = "z7nRcGQf";
    private static final int BSTATS_PLUGIN_ID = 30264;

    private static ItemsAdderAdditions instance;

    private ActionsManager actionsManager;
    private BehavioursManager behavioursManager;
    private ComponentsManager componentsManager;
    private CreativeMenuManager creativeMenuManager;
    private RecipeManager recipeManager;

    private Metrics bStats;

    public static ItemsAdderAdditions instance() {
        return instance;
    }

    @Override
    public void onLoad() {
        instance = this;

        Version version = Version.of(
                Bukkit.getMinecraftVersion(),
                getServer().getPluginManager()
                        .getPlugin("ItemsAdder")
                        .getPluginMeta().getVersion()
        );

        PatchManager.applyAll(version);
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();

        runConverters();

        this.bStats = new Metrics(this, BSTATS_PLUGIN_ID);

        NmsManager.initialize(this.getComponentLogger());

        this.actionsManager = new ActionsManager();
        this.behavioursManager = new BehavioursManager();
//            this.componentsManager = new ComponentsManager();
//            this.componentsManager.applyComponents();

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

    private void runConverters() {
        ConverterV100V101.run();
        ConverterV101V102.run();
        ConverterV105V106.run();
    }

    public void reload() {
        this.reloadConfig();
    }

    @Override
    public void onDisable() {
        this.bStats.shutdown();
        getServer().getScheduler().cancelTasks(this);

        NmsManager.shutdown();
    }

    private void registerListeners() {
        PluginManager pm = getServer().getPluginManager();

        List.of(
                new ActionsListener(),
                new ItemsAdderLoadListener()
        ).forEach(listener -> pm.registerEvents(listener, this));
    }

    public ActionsManager actionsManager() {
        return actionsManager;
    }

    public BehavioursManager behavioursManager() {
        return behavioursManager;
    }

    public ComponentsManager componentsManager() {
        return componentsManager;
    }

    public CreativeMenuManager creativeMenuManager() {
        return creativeMenuManager;
    }

    public RecipeManager recipeManager() {
        return recipeManager;
    }
}
