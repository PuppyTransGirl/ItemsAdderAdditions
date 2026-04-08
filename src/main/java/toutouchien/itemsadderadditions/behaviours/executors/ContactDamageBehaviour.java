package toutouchien.itemsadderadditions.behaviours.executors;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.annotations.Parameter;
import toutouchien.itemsadderadditions.behaviours.BehaviourExecutor;
import toutouchien.itemsadderadditions.behaviours.BehaviourHost;
import toutouchien.itemsadderadditions.behaviours.annotations.Behaviour;
import toutouchien.itemsadderadditions.behaviours.executors.contact.ContactDetector;
import toutouchien.itemsadderadditions.utils.PotionUtils;
import toutouchien.itemsadderadditions.utils.Task;
import toutouchien.itemsadderadditions.utils.other.ItemCategory;

import java.util.*;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("unused")
@NullMarked
@Behaviour(key = "contact_damage")
public final class ContactDamageBehaviour extends BehaviourExecutor {
    private static final DamageSource DAMAGE_SOURCE =
            DamageSource.builder(DamageType.CACTUS).build();

    @Parameter(key = "amount", type = Double.class, required = true,
            min = 0.5, max = 100.0)
    private Double amount;

    @Parameter(key = "interval", type = Integer.class, min = 1, max = 200)
    private Integer interval = 20;

    @Parameter(key = "fire_duration", type = Integer.class, min = 0, max = 200)
    private Integer fireDuration = 0;

    @Parameter(key = "damage_when_sneaking", type = Boolean.class)
    private Boolean damageWhenSneaking = true;

    // Block faces - read from "block_faces" sub-section
    @Parameter(key = "top", path = "block_faces", type = Boolean.class)
    private boolean topFaceActive = true;

    @Parameter(key = "north", path = "block_faces", type = Boolean.class)
    private boolean northFaceActive = true;

    @Parameter(key = "south", path = "block_faces", type = Boolean.class)
    private boolean southFaceActive = true;

    @Parameter(key = "west", path = "block_faces", type = Boolean.class)
    private boolean westFaceActive = true;

    @Parameter(key = "east", path = "block_faces", type = Boolean.class)
    private boolean eastFaceActive = true;

    private final EnumSet<BlockFace> activeFaces = EnumSet.noneOf(BlockFace.class);
    private final List<PotionEffect> potionEffects = new ArrayList<>();

    private @Nullable ScheduledTask task;
    private @Nullable ContactDetector detector;
    private @Nullable ItemCategory category;
    private final Set<UUID> touchingLastTick = new HashSet<>();

    @Override
    public boolean configure(Object configData, String namespacedID) {
        if (!(configData instanceof ConfigurationSection section))
            return false;
        if (!super.configure(configData, namespacedID))
            return false;

        // Build active faces from parameters
        if (topFaceActive)
            activeFaces.add(BlockFace.UP);
        if (northFaceActive)
            activeFaces.add(BlockFace.NORTH);
        if (southFaceActive)
            activeFaces.add(BlockFace.SOUTH);
        if (westFaceActive)
            activeFaces.add(BlockFace.WEST);
        if (eastFaceActive)
            activeFaces.add(BlockFace.EAST);

        // Parse potion effects from nested sections
        for (String key : section.getKeys(false)) {
            if (!key.startsWith("potion_effect"))
                continue;
            PotionEffect pe = PotionUtils.parsePotion(
                    section.getConfigurationSection(key));
            if (pe != null)
                potionEffects.add(pe);
        }

        return true;
    }

    @Override
    protected void onLoad(BehaviourHost host) {
        this.category = host.category();
        this.detector = new ContactDetector(
                host.namespacedID(), activeFaces, topFaceActive);
        task = Task.syncRepeat(
                t -> tick(), host.plugin(), 100L * 50L, 2L * 50L,
                TimeUnit.MILLISECONDS);
    }

    @Override
    protected void onUnload(BehaviourHost host) {
        if (task != null) {
            task.cancel();
            task = null;
        }
        detector = null;
        category = null;
        touchingLastTick.clear();
    }

    private void tick() {
        if (detector == null || category == null)
            return;

        Set<UUID> touchingThisTick = new HashSet<>();

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!isTouching(player)
                    || (player.isSneaking() && !damageWhenSneaking)) {
                if (touchingLastTick.contains(player.getUniqueId()))
                    player.setMaximumNoDamageTicks(20);
                continue;
            }

            touchingThisTick.add(player.getUniqueId());

            if (interval < 20)
                player.setMaximumNoDamageTicks(interval);
            player.damage(amount, DAMAGE_SOURCE);
            if (fireDuration > 0)
                player.setFireTicks(fireDuration);
            potionEffects.forEach(player::addPotionEffect);
        }

        touchingLastTick.clear();
        touchingLastTick.addAll(touchingThisTick);
    }

    private boolean isTouching(Player player) {
        if (detector == null || category == null)
            return false;

        return switch (category) {
            case BLOCK, ITEM, FURNITURE -> detector.isTouchingBlock(player);
            case COMPLEX_FURNITURE ->
                    detector.isTouchingComplexFurniture(player);
        };
    }
}
