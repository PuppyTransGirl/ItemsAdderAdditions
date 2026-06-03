package toutouchien.itemsadderadditions.feature.advancement;

import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.feature.advancement.trigger.*;
import toutouchien.itemsadderadditions.patch.Version;
import toutouchien.itemsadderadditions.patch.VersionRange;

import java.util.ArrayList;
import java.util.List;

@NullMarked
public final class AdvancementRuntimeService {
    private final AdvancementRegistry registry;
    private final Plugin plugin;
    private final List<Listener> activeListeners = new ArrayList<>();

    public AdvancementRuntimeService(AdvancementRegistry registry, Plugin plugin) {
        this.registry = registry;
        this.plugin = plugin;
    }

    public void register(Plugin plugin) {
        List<Listener> listeners = new ArrayList<>(List.of(
                new ObtainItemTriggerHandler(registry),
                new ConsumeItemTriggerHandler(registry),
                new PlaceBlockTriggerHandler(registry),
                new BreakBlockTriggerHandler(registry),
                new PlaceFurnitureTriggerHandler(registry),
                new BreakFurnitureTriggerHandler(registry),
                new InteractFurnitureTriggerHandler(registry),
                new CraftRecipeTriggerHandler(registry),
                new KillEntityWithItemTriggerHandler(registry),
                new PermissionTriggerHandler(registry),
                new InBiomeTriggerHandler(registry),
                new UsingItemTriggerHandler(registry),
                new TameAnimalTriggerHandler(registry),
                new VillagerTradeTriggerHandler(registry),
                new EnchantedItemTriggerHandler(registry),
                new SleptInBedTriggerHandler(registry),
                new BredAnimalsTriggerHandler(registry),
                new ChangedDimensionTriggerHandler(registry),
                new PlayerHurtEntityTriggerHandler(registry),
                new EntityHurtPlayerTriggerHandler(registry),
                new ShootBowTriggerHandler(registry),
                new FishingRodHookedTriggerHandler(registry),
                new FilledBucketTriggerHandler(registry),
                new PlayerKilledEntityTriggerHandler(registry),
                new RecipeUnlockedTriggerHandler(registry),
                new UsedTotemTriggerHandler(registry),
                new EffectsChangedTriggerHandler(registry),
                new FallFromHeightTriggerHandler(registry),
                new UsedEnderEyeTriggerHandler(registry),
                new BeeNestDestroyedTriggerHandler(registry),
                new EntityKilledPlayerTriggerHandler(registry),
                new ItemDurabilityChangedTriggerHandler(registry),
                new ItemUsedOnBlockTriggerHandler(registry),
                new KilledByArrowTriggerHandler(registry),
                new PlayerInteractedWithEntityTriggerHandler(registry),
                new PlayerShearedEquipmentTriggerHandler(registry),
                new RecipeCraftedTriggerHandler(registry),
                new ShotCrossbowTriggerHandler(registry),
                new StartedRidingTriggerHandler(registry),
                new HeldItemTriggerHandler(registry),
                new TickTriggerHandler(registry),
                new AdvancementCompletionListener(registry),
                new AdvancementPlayerJoinListener(registry, plugin)
        ));

        // Sit/unsit and trade-machine events only exist in ItemsAdder 4.0.17+.
        // Instantiating these handlers references those event classes, so guard
        // them behind a version check to avoid NoClassDefFoundError on 4.0.16 and
        // older.
        if (itemsAdderAtLeast4017()) {
            listeners.add(new SitFurnitureTriggerHandler(registry));
            listeners.add(new UnsitFurnitureTriggerHandler(registry));
            listeners.add(new OpenTradeMachineTriggerHandler(registry));
        }

        for (Listener l : listeners) {
            Bukkit.getPluginManager().registerEvents(l, plugin);
            activeListeners.add(l);
        }
    }

    private boolean itemsAdderAtLeast4017() {
        Plugin itemsAdder = plugin.getServer().getPluginManager().getPlugin("ItemsAdder");
        if (itemsAdder == null) return false;

        Version version = Version.of(
                Bukkit.getMinecraftVersion(),
                itemsAdder.getPluginMeta().getVersion()
        );
        return VersionRange.ia("4.0.17", null).test(version);
    }

    public void unregister() {
        for (Listener l : activeListeners) {
            HandlerList.unregisterAll(l);
        }
        activeListeners.clear();
    }
}
