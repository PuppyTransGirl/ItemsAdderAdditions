package toutouchien.itemsadderadditions.behaviours.executors;

import dev.lone.itemsadder.api.CustomStack;
import org.apache.commons.io.FilenameUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.annotations.Parameter;
import toutouchien.itemsadderadditions.behaviours.BehaviourExecutor;
import toutouchien.itemsadderadditions.behaviours.BehaviourHost;
import toutouchien.itemsadderadditions.behaviours.annotations.Behaviour;
import toutouchien.itemsadderadditions.utils.lava_fishing.LavaBobberState;
import toutouchien.itemsadderadditions.utils.lava_fishing.LavaBobberTask;
import toutouchien.itemsadderadditions.utils.lava_fishing.LavaFishingSessionManager;
import toutouchien.itemsadderadditions.utils.other.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@NullMarked
@Behaviour( key = "lava_fishing" )
public class LavaFishingBehaviour extends BehaviourExecutor implements Listener {
    @Parameter(path = "catch_times", key = "min", type = Integer.class, min = 100.0) private int minCatchTime = 400;
    @Parameter(path = "catch_times", key = "min", type = Integer.class, min = 100.0) private int maxCatchTime = 600;
    @Parameter(path = "catch_times", key = "reel", type = Integer.class, min = 1.0) private int reelCatchTime = 70;
    @Parameter(key = "apply_lure_reduction", type = Boolean.class) private boolean applyLureReduction = true;
    private final List<String> worlds = new ArrayList<>();

    private final Random random = new Random();

    @Override
    public boolean configure(Object configData, String namespacedID) {
        if (configData instanceof ConfigurationSection section) {
            if (section.contains("worlds")) {
                worlds.clear();

                Object data = section.get("worlds");
                switch (data) {
                    case String str -> worlds.add(str);
                    case List<?> list -> list.forEach(entry -> worlds.add(String.valueOf(entry)));
                    case null, default -> {
                        if (data == null) {
                            Log.itemSkip("Behaviours", namespacedID,
                                    "Item has worlds configuration with null value.");
                        } else {
                            Log.itemSkip("Behaviours", namespacedID,
                                    "Item has worlds configuration with unsupported type " + data.getClass().getTypeName());
                        }

                        return false;
                    }
                }
            }
        }

        return true;
    }

    @Override
    protected void onLoad(BehaviourHost host) {
        Bukkit.getPluginManager().registerEvents(this, host.plugin());
    }

    @Override
    protected void onUnload(BehaviourHost host) {
        HandlerList.unregisterAll(this);
    }

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event){
        if (!(event.getEntity() instanceof FishHook hook)) return;
        if (!(hook.getShooter() instanceof Player player)) return;
        if (!validWorld(player)) return;

        CustomStack item = CustomStack.byItemStack(player.getInventory().getItemInMainHand());
        if (item == null || !item.getNamespacedID().equalsIgnoreCase(host().namespacedID())) return;

        if (LavaFishingSessionManager.hasSession(player.getUniqueId())) {
            LavaFishingSessionManager.removeSession(player.getUniqueId());
        }

        hook.setInvulnerable(true);

        int lureLevel = applyLureReduction ? item.getItemStack().getEnchantmentLevel(Enchantment.LURE) : 0;

        // Each lure level reduces catch wait by 5 seconds (100 ticks):
        // https://minecraft.wiki/w/Fishing#Catching_fish (First bullet point)
        int minTicks = Math.max(100, minCatchTime - lureLevel * 100);
        int maxTicks = Math.max(minTicks, maxCatchTime - lureLevel * 100);

        // Override vanilla catch time, just in case.
        hook.setWaitTime(Integer.MAX_VALUE, Integer.MAX_VALUE);
        LavaBobberState state = new LavaBobberState(hook, minTicks, maxTicks);
        LavaFishingSessionManager.addSession(player.getUniqueId(), state);

        LavaBobberTask task = new LavaBobberTask(player.getUniqueId(),state, reelCatchTime);
        task.runTaskTimer(host().plugin(), 1L, 1L);
    }

    @EventHandler
    public void onFish(PlayerFishEvent event){
        Player player = event.getPlayer();

        if (!LavaFishingSessionManager.hasSession(player.getUniqueId())) return;

        CustomStack item = CustomStack.byItemStack(player.getInventory().getItemInMainHand());
        if (item == null || !item.getNamespacedID().equalsIgnoreCase(host().namespacedID())) return;

        LavaBobberState state = LavaFishingSessionManager.getSession(player.getUniqueId());

        if (event.getState() == PlayerFishEvent.State.REEL_IN || event.getState() == PlayerFishEvent.State.FAILED_ATTEMPT) {
            // Calculate damage based on if on ground and if it caught something.
            int damage;
            if (state.hasCatch()) {
                damage = handleCatch(player, state);
            } else {
                if (state.getHook().isOnGround()) {
                    damage = 2;
                } else {
                    damage = 1;
                }
            }

            LavaFishingSessionManager.removeSession(player.getUniqueId());

            if (damage > 0) {
                player.damageItemStack(item.getItemStack(), damage);
            }

            event.setCancelled(true);
        }
    }

    private boolean validWorld(Player player) {
        if (worlds.isEmpty()) return true;

        for (String world : worlds) {
            if (FilenameUtils.wildcardMatch(player.getWorld().getName(), world)) {
                return true;
            }
        }

        return false;
    }

    private int handleCatch(Player player, LavaBobberState state) {
        if (!state.getHook().isValid() || !player.isOnline()) return -1;

        Location hookLocation = state.getHook().getLocation();
        Location playerLocation = player.getLocation();

        // TODO: Implement Fishing loot mechanic.
        ItemStack item = new ItemStack(Material.STONE);

        // Drop the item and shoot it towards the player.
        hookLocation.getWorld().dropItem(hookLocation, item, drop -> {
            double dx = playerLocation.getX() - drop.getX();
            double dy = playerLocation.getY() - drop.getY();
            double dz = playerLocation.getZ() - drop.getZ();
            double dv = 0.1;

            // Prevent it being destroyed by Lava.
            // Item won't be invulnerable after pickup.
            drop.setInvulnerable(true);

            Vector velocity = new Vector(
                    dx * dv,
                    dy * dv + Math.sqrt(Math.sqrt(dx * dx + dy * dy + dz * dz)) * 0.08,
                    dz * dv);
            drop.setVelocity(velocity);
        });

        synchronized (random) {
            // give between 1 and 6 XP
            player.giveExp(random.nextInt(1, 7));
        }

        return 1;
    }
}
