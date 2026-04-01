package toutouchien.itemsadderadditions;

import org.bstats.bukkit.Metrics;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import toutouchien.itemsadderadditions.actions.ActionsListener;
import toutouchien.itemsadderadditions.actions.ActionsManager;
import toutouchien.itemsadderadditions.behaviours.BehavioursManager;
import toutouchien.itemsadderadditions.components.ComponentsManager;
import toutouchien.itemsadderadditions.creative.BytePacketListener;
import toutouchien.itemsadderadditions.creative.CreativeMenuManager;
import toutouchien.itemsadderadditions.creative.PacketListener;
import toutouchien.itemsadderadditions.listeners.ItemsAdderLoadListener;
import toutouchien.itemsadderadditions.updatechecker.UpdateChecker;
import toutouchien.itemsadderadditions.utils.VersionUtils;
import toutouchien.itemsadderadditions.utils.other.Log;

import java.util.List;

public class ItemsAdderAdditions extends JavaPlugin {
    private static final String MODRINTH_PROJECT_ID = "z7nRcGQf";
    private static final int BSTATS_PLUGIN_ID = 30264;

    private static ItemsAdderAdditions instance;

    private ActionsManager actionsManager;
    private BehavioursManager behavioursManager;
    private ComponentsManager componentsManager;
    private CreativeMenuManager creativeMenuManager;

    private Metrics bStats;

    @Override
    public void onLoad() {
        instance = this;
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.bStats = new Metrics(this, BSTATS_PLUGIN_ID);

        this.actionsManager = new ActionsManager();
        this.behavioursManager = new BehavioursManager();
        if (VersionUtils.isHigherThanOrEquals(VersionUtils.v1_21_11)) {
            this.componentsManager = new ComponentsManager();
            this.creativeMenuManager = new CreativeMenuManager();

            this.creativeMenuManager.setup();
            this.componentsManager.applyComponents();
        } else {
            Log.info("CreativeMenu", "Disabled - version has to be 1.21.11 or higher.");
        }

        registerListeners();

        if (this.getConfig().getBoolean("update-checker.enabled", true))
            new UpdateChecker(this, MODRINTH_PROJECT_ID);
    }

    @Override
    public void onDisable() {
        this.bStats.shutdown();
        getServer().getScheduler().cancelTasks(this);
    }

    private void registerListeners() {
        PluginManager pm = getServer().getPluginManager();

        List.of(
            new ActionsListener(),
            new ItemsAdderLoadListener()
        ).forEach(listener -> pm.registerEvents(listener, this));

        BytePacketListener.inject();
        PacketListener.inject();
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

    public static ItemsAdderAdditions instance() {
        return instance;
    }
}
