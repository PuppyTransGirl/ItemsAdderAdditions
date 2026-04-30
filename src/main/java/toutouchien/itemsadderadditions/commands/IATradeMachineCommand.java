package toutouchien.itemsadderadditions.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.lone.itemsadder.api.CustomStack;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import net.kyori.adventure.key.Key;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import toutouchien.itemsadderadditions.bridge.TradeMachineBridge;
import toutouchien.itemsadderadditions.utils.CommandUtils;
import toutouchien.itemsadderadditions.utils.other.Log;

import java.util.List;

/**
 * {@code /iatrademachine <player> <namespace:id> [-silent]}
 *
 * <p>Opens an ItemsAdder {@code trade_machine} or {@code block_trade_machine}
 * GUI for the target player without a physical block in the world.</p>
 *
 * <p>{@code -silent} suppresses the confirmation message sent to the executor.</p>
 *
 * <p>Permission: {@code ia.admin.iatrademachine}</p>
 */
public final class IATradeMachineCommand {
    private static final String PREFIX =
            "<gradient:#AC52D4:#6C3484>ItemsAdderAdditions</gradient><#999999>)</#999999> ";

    private IATradeMachineCommand() {
        throw new IllegalStateException("Command class");
    }

    public static LiteralCommandNode<CommandSourceStack> get() {
        var idArgument = Commands.argument("id", ArgumentTypes.key())
                .suggests((ctx, builder) -> {
                    try {
                        String input = builder.getRemaining().toLowerCase();
                        for (String iaId : CustomStack.getNamespacedIdsInRegistry()) {
                            if (iaId.toLowerCase().startsWith(input))
                                builder.suggest(iaId);
                        }
                    } catch (Exception ignored) {
                        // IA not fully loaded yet
                    }
                    return builder.buildFuture();
                })
                .executes(ctx -> {
                    Key id = ctx.getArgument("id", Key.class);
                    List<Player> targets = ctx.getArgument("player", PlayerSelectorArgumentResolver.class)
                            .resolve(ctx.getSource());
                    return run(CommandUtils.sender(ctx), targets, id, false);
                })
                .then(
                        Commands.literal("-silent")
                                .executes(ctx -> {
                                    Key id = ctx.getArgument("id", Key.class);
                                    List<Player> targets = ctx.getArgument("player", PlayerSelectorArgumentResolver.class)
                                            .resolve(ctx.getSource());
                                    return run(CommandUtils.sender(ctx), targets, id, true);
                                })
                );

        return Commands.literal("iatrademachine")
                .requires(css -> CommandUtils.defaultRequirements(css, "ia.admin.iatrademachine"))
                .then(Commands.argument("player", ArgumentTypes.player()).then(idArgument))
                .build();
    }

    private static int run(CommandSender sender, List<Player> targets, Key key, boolean silent) {
        if (targets.isEmpty()) {
            sender.sendRichMessage(PREFIX + "<#F27474>Player not found or not online.</#F27474>");
            return Command.SINGLE_SUCCESS;
        }
        return execute(sender, targets.get(0), key, silent);
    }

    private static int execute(CommandSender sender, Player target, Key key, boolean silent) {
        String id = key.asString();
        try {
            boolean opened = TradeMachineBridge.openTradeMachine(target, id);

            if (opened) {
                if (!silent) {
                    sender.sendRichMessage(PREFIX +
                            "<#7AF291>Opened trade machine <white>" + id +
                            "</white> for <white>" + target.getName() + "</white>.</#7AF291>");

                    Log.info("TradeMachine",
                            "Opened '" + id + "' for " + target.getName() +
                                    " (by " + sender.getName() + ")" + (silent ? " [silent]" : ""));
                }
            } else {
                sender.sendRichMessage(PREFIX +
                        "<#F27474>No <white>trade_machine</white> or " +
                        "<white>block_trade_machine</white> behaviour found for ID " +
                        "<white>" + id + "</white>. Check the ID and behaviour type.</#F27474>");
            }

        } catch (IllegalStateException e) {
            Log.error("TradeMachine", e.getMessage(), e);
            sender.sendRichMessage(PREFIX +
                    "<#F27474>ItemsAdder is not ready yet. Try again in a moment.</#F27474>");

        } catch (Exception e) {
            Log.error("TradeMachine",
                    "Failed to open trade machine '" + id + "' for " + target.getName(), e);
            sender.sendRichMessage(PREFIX +
                    "<#F27474>An unexpected error occurred. Check the console for details.</#F27474>");
        }

        return Command.SINGLE_SUCCESS;
    }
}
