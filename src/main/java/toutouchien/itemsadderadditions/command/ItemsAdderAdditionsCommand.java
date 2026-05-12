package toutouchien.itemsadderadditions.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.command.CommandSender;
import toutouchien.itemsadderadditions.common.logging.Log;
import toutouchien.itemsadderadditions.common.util.CommandUtils;
import toutouchien.itemsadderadditions.common.util.MathUtils;
import toutouchien.itemsadderadditions.common.util.Task;
import toutouchien.itemsadderadditions.plugin.ItemsAdderAdditions;
import toutouchien.itemsadderadditions.runtime.reload.ReloadResult;

public class ItemsAdderAdditionsCommand {
    private ItemsAdderAdditionsCommand() {
        throw new IllegalStateException("Command class");
    }

    public static LiteralCommandNode<CommandSourceStack> get() {
        return Commands.literal("itemsadderadditions")
                .requires(css -> CommandUtils.defaultRequirements(css, "itemsadderadditions.command.itemsadderadditions"))
                .then(debugCommand())
                .then(reloadCommand())
                .build();
    }

    private static LiteralArgumentBuilder<CommandSourceStack> debugCommand() {
        return Commands.literal("debug")
                .requires(css -> CommandUtils.defaultRequirements(css, "itemsadderadditions.command.itemsadderadditions.debug"))
                .executes(ctx -> {
                    CommandSender sender = CommandUtils.sender(ctx);
                    boolean debug = Log.debug();
                    if (debug)
                        sender.sendRichMessage("<gradient:#AC52D4:#6C3484>ItemsAdderAdditions</gradient><#999999>)</#999999> <#F27474>Debug mode has been disabled.</#F27474>");
                    else
                        sender.sendRichMessage("<gradient:#AC52D4:#6C3484>ItemsAdderAdditions</gradient><#999999>)</#999999> <#7AF291>Debug mode has been enabled.</#7AF291>");

                    Log.toggleDebug();
                    return Command.SINGLE_SUCCESS;
                });
    }

    private static LiteralArgumentBuilder<CommandSourceStack> reloadCommand() {
        return Commands.literal("reload")
                .requires(css -> CommandUtils.defaultRequirements(css, "itemsadderadditions.command.itemsadderadditions.reload"))
                .executes(ctx -> {
                    CommandSender sender = CommandUtils.sender(ctx);

                    sender.sendRichMessage("<gradient:#AC52D4:#6C3484>ItemsAdderAdditions</gradient><#999999>)</#999999> <#B0AEC1>Reloading ItemsAdderAdditions...</#B0AEC1>");
                    long startNanos = System.nanoTime();

                    Task.sync(task -> {
                        try {
                            ReloadResult result = ItemsAdderAdditions.instance().reload();
                            double timeTaken = (System.nanoTime() - startNanos) / 1_000_000D;

                            sender.sendRichMessage("<gradient:#AC52D4:#6C3484>ItemsAdderAdditions</gradient><#999999>)</#999999> <#7AF291>ItemsAdderAdditions has been reloaded. (%s ms, %s file(s) scanned, %s tagged)</#7AF291>"
                                    .formatted(
                                            MathUtils.decimalRound(timeTaken, 2),
                                            result.filesScanned(),
                                            result.filesTagged()
                                    )
                            );
                        } catch (Exception e) {
                            Log.error("Reload", "Failed to reload ItemsAdderAdditions", e);

                            sender.sendRichMessage("<gradient:#AC52D4:#6C3484>ItemsAdderAdditions</gradient><#999999>)</#999999> <#F27474>An error occurred while reloading ItemsAdderAdditions. Please check the console for more information.</#F27474>");
                        }
                    }, ItemsAdderAdditions.instance());

                    return Command.SINGLE_SUCCESS;
                });
    }
}
