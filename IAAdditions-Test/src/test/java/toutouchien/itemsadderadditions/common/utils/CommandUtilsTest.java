package toutouchien.itemsadderadditions.common.utils;

import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CommandUtilsTest {
    private static ServerMock server;
    private PlayerMock player;

    @BeforeAll
    static void boot() {
        server = MockBukkit.mock();
    }

    @AfterAll
    static void shutdown() {
        MockBukkit.unmock();
    }

    @BeforeEach
    void setUp() {
        player = server.addPlayer();
    }

    @SuppressWarnings("unchecked")
    private CommandSourceStack css(CommandSender sender, org.bukkit.entity.Entity executor) {
        CommandSourceStack css = mock(CommandSourceStack.class);
        when(css.getSender()).thenReturn(sender);
        when(css.getExecutor()).thenReturn(executor);
        return css;
    }

    @Test
    void nullCssThrows() {
        assertThrows(NullPointerException.class,
                () -> CommandUtils.defaultRequirements(null, "perm"));
    }

    @Test
    void nullPermissionThrows() {
        assertThrows(NullPointerException.class,
                () -> CommandUtils.defaultRequirements(css(player, player), null));
    }

    @Test
    void allowsConsoleWithPermission() {
        player.setOp(true);
        assertTrue(CommandUtils.defaultRequirements(css(player, null), "iaa.cmd"));
    }

    @Test
    void deniesWhenSenderLacksPermission() {
        PlayerMock noPerm = server.addPlayer();
        noPerm.setOp(false);
        assertFalse(CommandUtils.defaultRequirements(css(noPerm, null), "iaa.denied"));
    }

    @Test
    void requiresPlayerRejectsNonPlayerExecutor() {
        player.setOp(true);
        assertFalse(CommandUtils.defaultRequirements(css(player, null), "iaa.cmd", true));
    }

    @Test
    void requiresPlayerAcceptsPlayerExecutorWithPermission() {
        player.setOp(true);
        assertTrue(CommandUtils.defaultRequirements(css(player, player), "iaa.cmd", true));
    }

    @Test
    void executorWithoutPermissionFails() {
        PlayerMock executor = server.addPlayer();
        executor.setOp(false);
        player.setOp(true);
        assertFalse(CommandUtils.defaultRequirements(css(player, executor), "iaa.denied"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void senderReturnsExecutorWhenPresent() {
        CommandSourceStack source = css(server.getConsoleSender(), player);
        CommandContext<CommandSourceStack> ctx = mock(CommandContext.class);
        when(ctx.getSource()).thenReturn(source);
        assertEquals(player, CommandUtils.sender(ctx));
    }

    @Test
    @SuppressWarnings("unchecked")
    void senderFallsBackToSenderWhenNoExecutor() {
        CommandSender console = server.getConsoleSender();
        CommandSourceStack source = css(console, null);
        CommandContext<CommandSourceStack> ctx = mock(CommandContext.class);
        when(ctx.getSource()).thenReturn(source);
        assertEquals(console, CommandUtils.sender(ctx));
    }

    @Test
    void nullCtxThrows() {
        assertThrows(NullPointerException.class, () -> CommandUtils.sender(null));
    }
}
