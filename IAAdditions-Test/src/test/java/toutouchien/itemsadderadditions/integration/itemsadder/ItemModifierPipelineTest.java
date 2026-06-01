package toutouchien.itemsadderadditions.integration.itemsadder;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import static org.junit.jupiter.api.Assertions.*;

class ItemModifierPipelineTest {
    @BeforeAll
    static void setup() {
        MockBukkit.mock();
    }

    @AfterAll
    static void teardown() {
        MockBukkit.unmock();
    }

    private static ItemStack sword() {
        return ItemStack.of(Material.DIAMOND_SWORD);
    }

    @Test
    void noContributorsReturnsOriginal() {
        ItemModifierPipeline pipeline = new ItemModifierPipeline(null);
        ItemStack stack = sword();
        assertSame(stack, pipeline.apply("test:item", stack));
    }

    @Test
    void singleContributorReceivesCorrectArgs() {
        ItemModifierPipeline pipeline = new ItemModifierPipeline(null);
        String[] capturedId = {null};
        ItemStack stack = sword();
        pipeline.addContributor((id, item) -> {
            capturedId[0] = id;
            return item;
        });
        pipeline.apply("test:item", stack);
        assertEquals("test:item", capturedId[0]);
    }

    @Test
    void contributorsCalledInInsertionOrder() {
        ItemModifierPipeline pipeline = new ItemModifierPipeline(null);
        StringBuilder order = new StringBuilder();
        pipeline.addContributor((_, item) -> {
            order.append("A");
            return item;
        });
        pipeline.addContributor((_, item) -> {
            order.append("B");
            return item;
        });
        pipeline.apply("test:item", sword());
        assertEquals("AB", order.toString());
    }

    @Test
    void contributorCanTransformItem() {
        ItemModifierPipeline pipeline = new ItemModifierPipeline(null);
        pipeline.addContributor((_, _) -> ItemStack.of(Material.GOLDEN_SWORD));
        ItemStack result = pipeline.apply("test:item", sword());
        assertEquals(Material.GOLDEN_SWORD, result.getType());
    }

    @Test
    void eachContributorReceivesOutputOfPrevious() {
        ItemModifierPipeline pipeline = new ItemModifierPipeline(null);
        pipeline.addContributor((_, _) -> ItemStack.of(Material.GOLDEN_SWORD));
        pipeline.addContributor((_, item) -> {
            assertEquals(Material.GOLDEN_SWORD, item.getType());
            return item;
        });
        pipeline.apply("test:item", sword());
    }

    @Test
    void shutdownMakesApplyReturnInputUnchanged() {
        ItemModifierPipeline pipeline = new ItemModifierPipeline(null);
        boolean[] called = {false};
        pipeline.addContributor((_, item) -> {
            called[0] = true;
            return item;
        });
        pipeline.shutdown();
        ItemStack stack = sword();
        assertSame(stack, pipeline.apply("test:item", stack));
        assertFalse(called[0]);
    }

    @Test
    void addContributorNullThrows() {
        ItemModifierPipeline pipeline = new ItemModifierPipeline(null);
        assertThrows(NullPointerException.class, () -> pipeline.addContributor(null));
    }
}
