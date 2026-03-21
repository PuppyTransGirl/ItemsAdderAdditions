package toutouchien.itemsadderadditions.behaviours.executors;

import dev.lone.itemsadder.api.CustomBlock;
import dev.lone.itemsadder.api.CustomEntity;
import dev.lone.itemsadder.api.CustomFurniture;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitTask;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.annotations.Parameter;
import toutouchien.itemsadderadditions.behaviours.BehaviourExecutor;
import toutouchien.itemsadderadditions.behaviours.BehaviourHost;
import toutouchien.itemsadderadditions.behaviours.annotations.Behaviour;
import toutouchien.itemsadderadditions.utils.ItemCategory;
import toutouchien.itemsadderadditions.utils.other.PotionUtils;

import java.util.*;
import java.util.function.Predicate;

/**
 * Deals contact damage to players that touch a custom block or complex-furniture
 * entity - equivalent to vanilla cactus behaviour.
 *
 * <h3>Block type hitboxes</h3>
 * ItemsAdder custom blocks are backed by different vanilla block types, each with a
 * different physical hitbox size. The threshold used in the AABB check is derived from
 * the actual vanilla hitbox of the block material:
 *
 * <table>
 *   <tr><th>ItemsAdder type</th><th>Vanilla block</th><th>Threshold (block + 0.3 player)</th></tr>
 *   <tr><td>REAL / REAL_NOTE / REAL_WIRE / REAL_TRANSPARENT</td><td>Mushroom / Note / Tripwire / Chorus</td><td>0.800</td></tr>
 * </table>
 *
 * <p>REAL_TRANSPARENT should technically use 0.4375 (14/16÷2), but chorus plant
 * sometimes acts as a full block (connecting to adjacent plants), so 0.500 is used
 * for safety.
 *
 * <h3>Inside-block detection</h3>
 * The player's foot block and head block are checked first, so stepping <em>into</em>
 * the block also triggers damage - not just brushing the side.
 *
 * <h3>Example</h3>
 * <pre>{@code
 * behaviours:
 *   contact_damage:
 *     amount: 1.0
 *     interval: 20          # ticks between damage pulses (default 20, min 1, max 200)
 *     fire_duration: 40     # ticks of fire on hit       (default 0,  min 0, max 200)
 *     block_faces:          # which faces deal damage     (all true by default)
 *       top:   true
 *       north: false
 *       south: false
 *       west:  false
 *       east:  false
 * }</pre>
 */
@SuppressWarnings("unused")
@NullMarked
@Behaviour(key = "contact_damage")
public final class ContactDamageBehaviour extends BehaviourExecutor {
    private static final DamageSource DAMAGE_SOURCE = DamageSource.builder(DamageType.CACTUS).build();
    private static final double HALF_PLAYER_WIDTH = 0.3;
    private static final double HALF_BLOCK_SIZE = 0.5;
    public static final double ENTITY_SEARCH_RADIUS = HALF_BLOCK_SIZE + HALF_PLAYER_WIDTH + 1.0;
    public static final double THRESHOLD = HALF_BLOCK_SIZE + HALF_PLAYER_WIDTH + 0.01;
    private static final int PLAYER_HEIGHT_BLOCKS = 2;

    // Faces on which this block deals damage (populated during configure())
    private final EnumSet<BlockFace> activeFaces = EnumSet.noneOf(BlockFace.class);

    // Effects given when the block deals damage to the player (populated during configure())
    private List<PotionEffect> potionEffects = new ArrayList<>();

    @Parameter(key = "amount", type = Double.class, required = true, min = 0.5, max = 100.0)
    private Double amount;

    @Parameter(key = "interval", type = Integer.class, min = 1, max = 200)
    private Integer interval = 20;

    @Parameter(key = "fire_duration", type = Integer.class, min = 0, max = 200)
    private Integer fireDuration = 0;

    @Parameter(key = "damage_when_sneaking", type = Boolean.class)
    private Boolean damageWhenSneaking = true;

    /**
     * Whether standing on top of the block counts as contact.
     */
    private boolean topFaceActive;

    @Nullable private BukkitTask task;
    @Nullable private String namespacedID;
    @Nullable private ItemCategory category;

    /**
     * UUIDs of players that were touching this block on the previous tick.
     * Tracked per-player so we only reset immunity for the individual player that
     * left contact - a shared boolean flag would incorrectly affect unrelated players.
     */
    private final Set<UUID> touchingLastTick = new HashSet<>();

    @Override
    public boolean configure(Object configData, String namespacedID) {
        // Inject @Parameter fields (amount, interval, fire_duration, damage_when_sneaking)
        super.configure(configData, namespacedID);

        ConfigurationSection section = (ConfigurationSection) configData;
        ConfigurationSection facesSection = section.getConfigurationSection("block_faces");

        this.topFaceActive = readFace(facesSection, "top", BlockFace.UP);
        readFace(facesSection, "north", BlockFace.NORTH);
        readFace(facesSection, "south", BlockFace.SOUTH);
        readFace(facesSection, "west", BlockFace.WEST);
        readFace(facesSection, "east", BlockFace.EAST);

        for (String key : section.getKeys(false)) {
            if (!key.startsWith("potion_effect"))
                continue;

            ConfigurationSection potionSection = section.getConfigurationSection(key);
            PotionEffect potionEffect = PotionUtils.parsePotion(potionSection);
            if (potionEffect != null)
                this.potionEffects.add(potionEffect);
        }

        return true;
    }

    /**
     * Reads a face toggle and registers the face in {@link #activeFaces} if enabled.
     */
    private boolean readFace(@Nullable ConfigurationSection section, String key, BlockFace face) {
        boolean enabled = section == null || section.getBoolean(key, true);
        if (enabled) activeFaces.add(face);
        return enabled;
    }

    @Override
    protected void onLoad(BehaviourHost host) {
        this.namespacedID = host.namespacedID();
        this.category = host.category();
        // Delay first tick by 5s, there's no need to tick before that
        task = Bukkit.getScheduler().runTaskTimer(host.plugin(), this::tick, 100L, 2L);
    }

    @Override
    protected void onUnload(BehaviourHost host) {
        if (task != null) {
            task.cancel();
            task = null;
        }

        namespacedID = null;
        category = null;
        touchingLastTick.clear();
    }

    private void tick() {
        if (namespacedID == null || category == null)
            return;

        Set<UUID> touchingThisTick = new HashSet<>();

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!isTouching(player) || (player.isSneaking() && !damageWhenSneaking)) {
                // Player left contact - restore vanilla damage-immunity window for this player only.
                if (touchingLastTick.contains(player.getUniqueId()))
                    player.setMaximumNoDamageTicks(20);
                continue;
            }

            touchingThisTick.add(player.getUniqueId());

            // Shorten immunity window when interval is faster than vanilla's 20 ticks,
            // otherwise the configured damage rate would be silently skipped.
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
        return switch (category) {
            case BLOCK, ITEM, FURNITURE -> overlapsAnyBlock(player, this::isMatchingBlock);
            case COMPLEX_FURNITURE -> isTouchingComplexFurniture(player);
            case null -> false;
        };
    }

    /**
     * Returns {@code true} if the player is touching the barrier block that backs
     * a matching complex-furniture armor-stand entity.
     *
     * <p>Complex furniture places a {@code BARRIER} block at the entity's block
     * coordinates for collision. We locate every nearby entity whose {@link CustomEntity}
     * ID matches, derive its barrier-block position, then delegate to
     * {@link #overlapsAnyBlock} anchored to that barrier.
     */
    private boolean isTouchingComplexFurniture(Player player) {
        if (namespacedID == null)
            return false;

        Collection<Entity> nearby = player.getWorld().getNearbyEntities(
                player.getLocation(),
                ENTITY_SEARCH_RADIUS, ENTITY_SEARCH_RADIUS, ENTITY_SEARCH_RADIUS
        );

        for (Entity entity : nearby) {
            CustomEntity ce = CustomEntity.byAlreadySpawned(entity);
            if (ce == null || !namespacedID.equals(ce.getNamespacedID()))
                continue;

            Block barrierBlock = entity.getLocation().getBlock();
            if (overlapsAnyBlock(player, barrierBlock::equals))
                return true;
        }

        return false;
    }

    /**
     * Returns {@code true} if the player's hitbox overlaps any block accepted by
     * {@code isTarget}.
     *
     * <p>Three cases are checked per Y-level of the player hitbox:
     * <ol>
     *   <li><b>Inside</b>: the player's foot/head block itself satisfies {@code isTarget}.</li>
     *   <li><b>On top</b> (foot level only): the block directly below the feet
     *       satisfies {@code isTarget} and {@code topFaceActive} is {@code true}.</li>
     *   <li><b>Adjacent</b>: a horizontal neighbour satisfies {@code isTarget} and the
     *       player's centre is within {@code THRESHOLD} of the block's centre on X and Z.</li>
     * </ol>
     */
    private boolean overlapsAnyBlock(Player player, Predicate<Block> isTarget) {
        Location loc = player.getLocation();
        double px = loc.getX();
        double pz = loc.getZ();
        int footY = loc.getBlockY();
        int bx0 = loc.getBlockX();
        int bz0 = loc.getBlockZ();

        for (int dy = 0; dy < PLAYER_HEIGHT_BLOCKS; dy++) {
            Block origin = player.getWorld().getBlockAt(bx0, footY + dy, bz0);
            if (isTarget.test(origin))
                return true;

            if (dy == 0 && topFaceActive && isTarget.test(origin.getRelative(BlockFace.DOWN)))
                return true;

            for (BlockFace face : activeFaces) {
                if (face == BlockFace.UP)
                    continue; // UP already handled above

                Block neighbour = origin.getRelative(face);
                if (!isTarget.test(neighbour))
                    continue;

                double cx = neighbour.getX() + 0.5;
                double cz = neighbour.getZ() + 0.5;
                if (Math.abs(px - cx) <= THRESHOLD && Math.abs(pz - cz) <= THRESHOLD)
                    return true;
            }
        }

        return false;
    }

    private boolean isMatchingBlock(Block block) {
        if (namespacedID == null)
            return false;

        CustomBlock cb = CustomBlock.byAlreadyPlaced(block);
        if (cb != null && namespacedID.equals(cb.getNamespacedID()))
            return true;

        CustomFurniture cf = CustomFurniture.byAlreadySpawned(block);
        return cf != null && namespacedID.equals(cf.getNamespacedID());
    }
}
