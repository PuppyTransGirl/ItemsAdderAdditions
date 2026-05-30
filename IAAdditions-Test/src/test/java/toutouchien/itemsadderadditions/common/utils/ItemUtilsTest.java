package toutouchien.itemsadderadditions.common.utils;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ItemUtilsTest {
    private static ServerMock server;
    private PlayerMock player;

    @BeforeAll
    static void setup() {
        server = MockBukkit.mock();
    }

    @AfterAll
    static void teardown() {
        MockBukkit.unmock();
    }

    private static ItemStack diamond(int amount) {
        return ItemStack.of(Material.DIAMOND, amount);
    }

    private static int countDiamonds(PlayerMock p) {
        return countMaterial(p, Material.DIAMOND);
    }

    private static int countMaterial(PlayerMock p, Material mat) {
        int count = 0;
        for (ItemStack stack : p.getInventory().getContents()) {
            if (stack != null && stack.getType() == mat) {
                count += stack.getAmount();
            }
        }
        return count;
    }

    @BeforeEach
    void freshPlayer() {
        player = server.addPlayer();
        player.getInventory().clear();
    }

    @Test
    void removeExactAmount() {
        player.getInventory().addItem(diamond(5));
        ItemUtils.removeItemsFromInventory(player, diamond(1), 3);

        int remaining = countDiamonds(player);
        assertEquals(2, remaining);
    }

    @Test
    void removeEntireStack() {
        player.getInventory().addItem(diamond(4));
        ItemUtils.removeItemsFromInventory(player, diamond(1), 4);

        assertEquals(0, countDiamonds(player));
    }

    @Test
    void removeAcrossMultipleStacks() {
        player.getInventory().setItem(0, diamond(5));
        player.getInventory().setItem(1, diamond(5));
        ItemUtils.removeItemsFromInventory(player, diamond(1), 7);

        assertEquals(3, countDiamonds(player));
    }

    @Test
    void removeMoreThanAvailableRemovesAll() {
        player.getInventory().addItem(diamond(3));
        ItemUtils.removeItemsFromInventory(player, diamond(1), 100);

        assertEquals(0, countDiamonds(player));
    }

    @Test
    void nonMatchingItemsNotTouched() {
        player.getInventory().addItem(diamond(5));
        player.getInventory().addItem(ItemStack.of(Material.GOLD_INGOT, 5));
        ItemUtils.removeItemsFromInventory(player, diamond(1), 5);

        // diamonds gone, gold ingots untouched
        assertEquals(0, countMaterial(player, Material.DIAMOND));
        assertEquals(5, countMaterial(player, Material.GOLD_INGOT));
    }

    @Test
    void removeZeroItemsDoesNothing() {
        player.getInventory().addItem(diamond(5));
        ItemUtils.removeItemsFromInventory(player, diamond(1), 0);

        assertEquals(5, countDiamonds(player));
    }
}
