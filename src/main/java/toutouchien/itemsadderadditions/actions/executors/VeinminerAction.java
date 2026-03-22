package toutouchien.itemsadderadditions.actions.executors;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.actions.ActionContext;
import toutouchien.itemsadderadditions.actions.ActionExecutor;
import toutouchien.itemsadderadditions.actions.TriggerType;
import toutouchien.itemsadderadditions.actions.annotations.Action;
import toutouchien.itemsadderadditions.annotations.Parameter;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Veinminer: when a block is broken with this item, all connected
 * blocks of the same material are also broken.
 * Restricted to {@link TriggerType#ITEM_BREAK_BLOCK} only.
 * <p>
 * Example:
 * <pre>{@code
 * veinminer:
 *   max_blocks: 64
 * }</pre>
 */
@SuppressWarnings("unused")
@NullMarked
@Action(key = "veinminer", triggers = {TriggerType.ITEM_BREAK_BLOCK, TriggerType.ITEM_INTERACT})
public final class VeinminerAction extends ActionExecutor {
    private static final Set<Location> currentlyBreaking = Collections.synchronizedSet(new HashSet<>());

    private static final BlockFace[] FACES = {
            BlockFace.NORTH, BlockFace.SOUTH,
            BlockFace.EAST, BlockFace.WEST,
            BlockFace.UP, BlockFace.DOWN
    };

    @Parameter(key = "max_blocks", type = Integer.class, required = true, min = 1, max = 1024)
    private Integer maxBlocks;

    @Override
    protected void execute(ActionContext context) {
        Block origin = context.block();
        Player player = context.player();
        if (origin == null)
            return;

        // Already being broken as part of a chain - skip to avoid recursion.
        if (currentlyBreaking.contains(origin.getLocation()))
            return;

        Material targetType = origin.getType();
        ItemStack tool = player.getInventory().getItemInMainHand();

        // No drops means the tool can't mine this material; don't veinmine.
        if (origin.getDrops(tool, player).isEmpty())
            return;

        Set<Block> toBreak = findConnectedBlocks(origin, targetType, maxBlocks);
        if (toBreak.isEmpty())
            return;

        Set<Location> locations = toBreak.stream()
                .map(Block::getLocation)
                .collect(Collectors.toSet());

        currentlyBreaking.addAll(locations);
        try {
            for (Block block : toBreak)
                block.breakNaturally(tool);
        } finally {
            currentlyBreaking.removeAll(locations);
        }
    }

    // BFS to collect connected same-type blocks up to the specified limit
    private Set<Block> findConnectedBlocks(Block origin, Material targetType, int limit) {
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
                if (visited.contains(loc) || neighbor.getType() != targetType)
                    continue;

                visited.add(loc);
                queue.add(neighbor);
            }
        }

        return toBreak;
    }
}
