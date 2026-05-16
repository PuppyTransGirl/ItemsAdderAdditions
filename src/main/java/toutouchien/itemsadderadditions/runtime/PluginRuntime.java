package toutouchien.itemsadderadditions.runtime;

import net.momirealms.antigrieflib.AntiGriefLib;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.common.namespace.NamespaceUtils;
import toutouchien.itemsadderadditions.feature.action.ActionsListener;
import toutouchien.itemsadderadditions.feature.action.ActionsManager;
import toutouchien.itemsadderadditions.feature.behaviour.BehavioursManager;
import toutouchien.itemsadderadditions.feature.creative.CreativeMenuManager;
import toutouchien.itemsadderadditions.feature.creative.CreativeRegistryReloader;
import toutouchien.itemsadderadditions.feature.painting.CustomPaintingManager;
import toutouchien.itemsadderadditions.feature.recipe.RecipeManager;
import toutouchien.itemsadderadditions.feature.update.UpdateChecker;
import toutouchien.itemsadderadditions.feature.worldgen.FurniturePopulatorWorldListener;
import toutouchien.itemsadderadditions.integration.itemsadder.ItemsAdderLoadListener;
import toutouchien.itemsadderadditions.nms.api.NmsManager;
import toutouchien.itemsadderadditions.plugin.ItemsAdderAdditions;
import toutouchien.itemsadderadditions.runtime.reload.ReloadCoordinator;
import toutouchien.itemsadderadditions.runtime.reload.ReloadResult;
import toutouchien.itemsadderadditions.settings.PluginSettings;
import toutouchien.itemsadderadditions.settings.migration.*;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

/**
 * Owns the enabled runtime state of the plugin.
 *
 * <p>The Bukkit entrypoint should stay boring: load patches, create the runtime,
 * and delegate lifecycle calls. Managers and reload order live here so future
 * features can be added without turning {@link ItemsAdderAdditions} into a god
 * class again.</p>
 */
@NullMarked
public final class PluginRuntime {
    private static final String MODRINTH_PROJECT_ID = "z7nRcGQf";
    private static final int BSTATS_PLUGIN_ID = 30264;

    private final ItemsAdderAdditions plugin;

    private PluginSettings settings;
    private ActionsManager actionsManager;
    private BehavioursManager behavioursManager;
    private CustomPaintingManager customPaintingManager;
    private RecipeManager recipeManager;
    private @Nullable CreativeMenuManager creativeMenuManager;
    private ReloadCoordinator reloadCoordinator;
    private CreativeRegistryReloader creativeRegistryReloader;

    private Metrics bStats;
    private AntiGriefLib antiGriefLib;

    public PluginRuntime(ItemsAdderAdditions plugin) {
        this.plugin = plugin;
    }

    private static void runConfigConverters() {
        ConverterV100V101.run();
        ConverterV101V102.run();
        ConverterV102V106.run();
        ConverterV106V107.run();
        ConverterV107V108.run();
    }

    private static void registerListeners(JavaPlugin plugin) {
        PluginManager pm = plugin.getServer().getPluginManager();
        for (Listener listener : listeners()) {
            pm.registerEvents(listener, plugin);
        }
    }

    private static List<Listener> listeners() {
        List<Listener> listeners = new ArrayList<>();
        listeners.addAll(ActionsListener.createAll());
        listeners.add(new FurniturePopulatorWorldListener());
        listeners.add(new ItemsAdderLoadListener());
        return List.copyOf(listeners);
    }

    private static <T> T require(@Nullable T value, String name) {
        return Objects.requireNonNull(value, "Plugin runtime has not initialized " + name);
    }

    public void enable() {
        plugin.saveDefaultConfig();
        runConfigConverters();
        reloadSettings();

        startMetrics();
        setupAntiGriefLib();
        NamespaceUtils.initVanillaCache();
        NmsManager.initialize(plugin.getComponentLogger());

        this.actionsManager = new ActionsManager(settings);
        this.behavioursManager = new BehavioursManager(settings);
        this.customPaintingManager = new CustomPaintingManager(plugin);
        this.recipeManager = new RecipeManager(plugin);

        setupCreativeInventoryIntegration();
        this.creativeRegistryReloader = new CreativeRegistryReloader(plugin);
        this.reloadCoordinator = new ReloadCoordinator(
                actionsManager,
                behavioursManager,
                customPaintingManager,
                recipeManager,
                creativeRegistryReloader
        );

        registerListeners(plugin);
        startUpdateCheckerIfEnabled();
    }

    public void disable() {
        bStats.shutdown();

        actionsManager.shutdown();
        behavioursManager.shutdown();
        recipeManager.shutdown();

        plugin.getServer().getScheduler().cancelTasks(plugin);
        NmsManager.shutdown();
    }

    public ReloadResult reload() {
        plugin.reloadConfig();
        reloadSettings();
        applySettingsToManagers();
        return reloadItemsAdderData();
    }

    public ReloadResult reloadItemsAdderData() {
        return reloadCoordinator().reloadItemsAdderData();
    }

    public PluginSettings settings() {
        return require(settings, "settings");
    }

    public ReloadCoordinator reloadCoordinator() {
        return require(reloadCoordinator, "reloadCoordinator");
    }

    public ActionsManager actionsManager() {
        return require(actionsManager, "actionsManager");
    }

    public BehavioursManager behavioursManager() {
        return require(behavioursManager, "behavioursManager");
    }

    public @Nullable CreativeMenuManager creativeMenuManager() {
        return creativeMenuManager;
    }

    public CustomPaintingManager customPaintingManager() {
        return require(customPaintingManager, "customPaintingManager");
    }

    public RecipeManager recipeManager() {
        return require(recipeManager, "recipeManager");
    }

    public AntiGriefLib antiGriefLib() {
        return require(antiGriefLib, "antiGriefLib");
    }

    private void reloadSettings() {
        this.settings = PluginSettings.load(plugin.getConfig());
    }

    private void applySettingsToManagers() {
        actionsManager().applySettings(settings());
        behavioursManager().applySettings(settings());
    }

    private void startMetrics() {
        this.bStats = new Metrics(plugin, BSTATS_PLUGIN_ID);
        bStats.addCustomChart(new SimplePie("platform", this::detectPlatform));
        bStats.addCustomChart(new SimplePie("default_pack", () -> isUsingDefaultPack() ? "Yes" : "No"));
    }

    private String detectPlatform() {
        try (InputStream is = plugin.getClass().getClassLoader()
                .getResourceAsStream("iaadditions_platform.properties")) {
            if (is != null) {
                Properties props = new Properties();
                props.load(is);
                String platform = props.getProperty("platform");
                if (platform != null && !platform.isBlank()) {
                    return platform;
                }
            }
        } catch (Exception ignored) {
        }
        return "Other";
    }

    private boolean isUsingDefaultPack() {
        Plugin itemsAdder = plugin.getServer().getPluginManager().getPlugin("ItemsAdder");
        if (itemsAdder == null) return false;
        return new File(itemsAdder.getDataFolder(), "contents/iaadditions").isDirectory();
    }

    private void setupAntiGriefLib() {
        this.antiGriefLib = AntiGriefLib.builder(plugin)
                .build();
    }

    private void setupCreativeInventoryIntegration() {
        if (NmsManager.instance().handler().creativeMenu() == null) {
            return;
        }

        this.creativeMenuManager = new CreativeMenuManager();
        this.creativeMenuManager.setup();
        NmsManager.instance().handler().creativeMenu().injectListeners(plugin);
    }

    private void startUpdateCheckerIfEnabled() {
        if (settings().updateChecker().enabled()) {
            new UpdateChecker(plugin, MODRINTH_PROJECT_ID);
        }
    }
}
