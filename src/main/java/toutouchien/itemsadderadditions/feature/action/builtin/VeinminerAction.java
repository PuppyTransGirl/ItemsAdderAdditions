package toutouchien.itemsadderadditions.feature.action.builtin;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.common.annotation.Parameter;
import toutouchien.itemsadderadditions.feature.action.ActionContext;
import toutouchien.itemsadderadditions.feature.action.ActionExecutor;
import toutouchien.itemsadderadditions.feature.action.TriggerType;
import toutouchien.itemsadderadditions.feature.action.annotation.Action;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Veinminer: when a block is broken with this item, all connected
 * blocks of the same material are also broken in one sweep.
 *
 * <p>Only fires on {@link TriggerType#ITEM_BREAK_BLOCK} - that is the only trigger
 * guaranteed to provide a block context via {@link ActionContext#block()}.
 *
 * <p>Re-entrancy guard: when a veinminer break triggers another {@code ITEM_BREAK_BLOCK}
 * event for a block we are already processing, we skip it to avoid infinite recursion.
 * The guard is thread-local to be safe on Folia, where different regions can
 * process block events concurrently.
 *
 * <p>Example:
 * <pre>{@code
 * veinminer:
 *   max_blocks: 64
 * }</pre>
 */
@SuppressWarnings("unused")
@NullMarked
@Action(key = "veinminer", triggers = {TriggerType.ITEM_BREAK_BLOCK})
public final class VeinminerAction extends ActionExecutor {
    /**
     * Re-entrancy guard: tracks block locations being processed in the current call
     * chain. {@code ThreadLocal} is correct here - on both standard Bukkit (main thread)
     * and Folia (per-region threads), each thread owns its own break cycle.
     */
    private static final ThreadLocal<Set<Location>> BREAKING =
            ThreadLocal.withInitial(HashSet::new);

    private static final BlockFace[] FACES = {
            BlockFace.NORTH, BlockFace.SOUTH,
            BlockFace.EAST, BlockFace.WEST,
            BlockFace.UP, BlockFace.DOWN
    };

    @Parameter(key = "max_blocks", type = Integer.class, required = true, min = 1, max = 1024)
    private Integer maxBlocks;

    /**
     * BFS: collects up to {@code limit - 1} additional connected same-type blocks.
     */
    private static Set<Block> findConnectedBlocks(Block origin, Material targetType, int limit) {
        Set<Block> toBreak = new LinkedHashSet<>();
        Deque<Block> queue = new ArrayDeque<>();
        Set<Location> visited = new HashSet<>();

        queue.add(origin);
        visited.add(origin.getLocation());

        while (!queue.isEmpty() && toBreak.size() < limit - 1) {
            Block current = queue.poll();
            if (current != origin) toBreak.add(current);

            for (BlockFace face : FACES) {
                Block neighbor = current.getRelative(face);
                Location loc = neighbor.getLocation();
                if (visited.contains(loc) || neighbor.getType() != targetType) continue;

                visited.add(loc);
                queue.add(neighbor);
            }
        }

        return toBreak;
    }

    @Override
    protected void execute(ActionContext context) {
        Entity runOn = context.runOn();
        if (!(runOn instanceof HumanEntity human)) return;

        Block origin = context.block();
        if (origin == null) return;

        Set<Location> breaking = BREAKING.get();

        // Already being broken as part of a chain - skip to avoid recursion.
        if (breaking.contains(origin.getLocation())) return;

        Material targetType = origin.getType();
        ItemStack tool = human.getInventory().getItemInMainHand();

        // No drops means the tool cannot mine this material; do not vein-mine.
        if (origin.getDrops(tool, human).isEmpty()) return;

        Set<Block> toBreak = findConnectedBlocks(origin, targetType, maxBlocks);
        if (toBreak.isEmpty()) return;

        Set<Location> locations = toBreak.stream()
                .map(Block::getLocation)
                .collect(Collectors.toSet());

        breaking.addAll(locations);
        try {
            for (Block block : toBreak) {
                block.breakNaturally(tool);
            }
        } finally {
            breaking.removeAll(locations);
        }
    }
}
