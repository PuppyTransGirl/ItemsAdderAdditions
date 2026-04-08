package toutouchien.itemsadderadditions.behaviours.executors;

import dev.lone.itemsadder.api.CustomBlock;
import dev.lone.itemsadder.api.CustomStack;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.behaviours.BehaviourExecutor;
import toutouchien.itemsadderadditions.behaviours.BehaviourHost;
import toutouchien.itemsadderadditions.behaviours.annotations.Behaviour;
import toutouchien.itemsadderadditions.utils.SoundUtils;
import toutouchien.itemsadderadditions.utils.other.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Implements a progressive stacking mechanic similar to vanilla Candles or Pink Petals.
 *
 * <p>When a player right-clicks the item's custom block while holding a specific item,
 * the block is replaced with the next stage in the configured sequence.
 *
 * <h3>Configuration formats</h3>
 *
 * <h4>1. Simple ordered list</h4>
 * The quickest form: each entry is the namespaced ID of the next-stage block.
 * The trigger item defaults to the item this behaviour is attached to.
 * <pre>{@code
 * behaviours:
 *   stackable:
 *     - "my_namespace:flower_stage_2"
 *     - "my_namespace:flower_stage_3"
 * }</pre>
 *
 * <h4>2. Shared-items list</h4>
 * All stages share the same trigger item(s), decrement amount, and optional sound.
 * <pre>{@code
 * behaviours:
 *   stackable:
 *     blocks:
 *       - "my_namespace:flower_stage_2"
 *       - "my_namespace:flower_stage_3"
 *     items:
 *       - "minecraft:bone_meal"
 *     decrement_amount: 1   # optional, default 1
 *     sound:                # optional
 *       name: "minecraft:block.grass.place"
 *       source: master
 *       volume: 1.0
 *       pitch:  1.0
 * }</pre>
 *
 * <h4>3. Named steps with per-step overrides</h4>
 * Each named key defines one stage independently.
 * <pre>{@code
 * behaviours:
 *   stackable:
 *     stage_2:
 *       block: "my_namespace:flower_stage_2"
 *       items:
 *         - "minecraft:apple"
 *       decrement_amount: 1
 *     stage_3:
 *       block: "my_namespace:flower_stage_3"
 *       items:
 *         - "my_namespace:rare_item"
 * }</pre>
 *
 * <h3>Logic</h3>
 * <ul>
 *   <li>The clicked block must be the base item's block, or a result block from an
 *       earlier step in the chain.</li>
 *   <li>The held item must match one of the step's trigger items.</li>
 *   <li>On success: consumes one item (unless Creative), swings the arm, plays the
 *       optional sound, and replaces the block with the next stage.</li>
 * </ul>
 */
@NullMarked
@Behaviour(key = "stackable")
public final class StackableBehaviour extends BehaviourExecutor implements Listener {
    private final List<StackStep> steps = new ArrayList<>();
    private String namespacedID = "";

    @Override
    public boolean configure(Object configData, String namespacedID) {
        if (configData instanceof List<?> list) {
            // Format 1: plain list of block IDs
            for (Object entry : list)
                addStep(entry.toString(), null, namespacedID);
        } else if (configData instanceof ConfigurationSection section) {
            if (section.contains("blocks")) {
                // Format 2: shared-items section (has a "blocks" key at the top level)
                for (String block : section.getStringList("blocks"))
                    addStep(block, section, namespacedID);
            } else {
                // Format 3: named steps - every key is a stage name
                for (String stageKey : section.getKeys(false)) {
                    ConfigurationSection stageSection = section.getConfigurationSection(stageKey);
                    if (stageSection == null)
                        continue;

                    String block = stageSection.getString("block");
                    addStep(block, stageSection, namespacedID);
                }
            }
        }

        return !steps.isEmpty();
    }

    private void addStep(@Nullable String blockId, @Nullable ConfigurationSection section, String fallbackItemID) {
        if (blockId == null || blockId.isBlank()) return;

        StackStep step = new StackStep(blockId);

        if (section != null && section.contains("items")) {
            step.items.addAll(normalizeIds(section.getStringList("items")));
            step.decrementAmount = Math.clamp(section.getInt("decrement_amount", 1), 0, 256);
        } else {
            step.items.add(fallbackItemID);
        }

        if (section != null && section.contains("sound")) {
            ConfigurationSection soundSection = section.getConfigurationSection("sound");
            Sound parsed = SoundUtils.parseSound(soundSection);
            if (parsed == null && soundSection != null) {
                String src = soundSection.getString("source", "");
                if (!src.isBlank() && SoundUtils.parseSource(src) == null)
                    Log.warn("Behaviours", "stackable: invalid sound source '{}' - valid values: master, music, record, weather, block, hostile, neutral, player, ambient, voice", src);
            }
            step.sound = parsed;
        }

        steps.add(step);
    }

    private static List<String> normalizeIds(List<String> ids) {
        return ids.stream()
                .map(id -> id.toLowerCase(Locale.ROOT))
                .map(id -> id.contains(":") ? id : "minecraft:" + id)
                .toList();
    }

    @Override
    protected void onLoad(BehaviourHost host) {
        this.namespacedID = host.namespacedID();
        Bukkit.getPluginManager().registerEvents(this, host.plugin());
    }

    @Override
    protected void onUnload(BehaviourHost host) {
        HandlerList.unregisterAll(this);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;

        Block block = event.getClickedBlock();
        CustomBlock customBlock = CustomBlock.byAlreadyPlaced(block);
        if (customBlock == null)
            return;

        ItemStack item = event.getItem();
        if (item == null)
            return;

        Player player = event.getPlayer();
        CustomStack customStack = CustomStack.byItemStack(item);

        String heldId = customStack != null
                ? customStack.getNamespacedID()
                : "minecraft:" + item.getType().name().toLowerCase(Locale.ROOT);

        // Walk the step chain: the clicked block must match either the base block
        // (namespacedID) or a previous step's result block before we consider a step.
        String lastResultBlock = "";
        for (StackStep step : steps) {
            String clickedId = customBlock.getNamespacedID();
            boolean isBaseBlock = clickedId.equals(namespacedID);
            boolean isPrevResult = !lastResultBlock.isEmpty() && clickedId.equalsIgnoreCase(lastResultBlock);

            if (!isBaseBlock && !isPrevResult) {
                lastResultBlock = step.resultBlock;
                continue;
            }

            lastResultBlock = step.resultBlock;

            if (step.items.contains(heldId)) {
                applyStep(event, step, player, item, block);
                return;
            }
        }
    }

    private void applyStep(PlayerInteractEvent event, StackStep step, Player player, ItemStack item, Block block) {
        if (player.getGameMode() != GameMode.CREATIVE) {
            int remaining = item.getAmount() - step.decrementAmount;
            if (remaining <= 0)
                player.getInventory().removeItem(item);
            else
                item.setAmount(remaining);
        }

        player.swingHand(event.getHand());

        if (step.sound != null)
            player.getWorld().playSound(step.sound, block.getX(), block.getY(), block.getZ());

        CustomBlock.place(step.resultBlock, block.getLocation());
        event.setCancelled(true);
    }

    private static final class StackStep {
        final String resultBlock;
        final List<String> items = new ArrayList<>();
        int decrementAmount = 1;
        @Nullable Sound sound;

        StackStep(String resultBlock) {
            this.resultBlock = resultBlock;
        }
    }
}
