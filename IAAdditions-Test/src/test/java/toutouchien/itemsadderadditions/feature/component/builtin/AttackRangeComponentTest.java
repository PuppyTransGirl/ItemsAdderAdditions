package toutouchien.itemsadderadditions.feature.component.builtin;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.AttackRange;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import toutouchien.itemsadderadditions.common.version.VersionUtils;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("UnstableApiUsage")
class AttackRangeComponentTest {
    private static final float DELTA = 0.0001F;

    @BeforeAll
    static void setup() {
        MockBukkit.mock();
    }

    @AfterAll
    static void teardown() {
        MockBukkit.unmock();
    }

    private static YamlConfiguration yamlOf(String yaml) {
        YamlConfiguration cfg = new YamlConfiguration();
        try {
            cfg.loadFromString(yaml);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return cfg;
    }

    private static AttackRange applied(YamlConfiguration cfg) {
        AttackRangeComponent component = new AttackRangeComponent();
        assertTrue(component.configure(cfg, "test:item"));
        ItemStack stack = component.apply(new ItemStack(Material.DIAMOND_SWORD), "test:item");
        AttackRange range = stack.getData(DataComponentTypes.ATTACK_RANGE);
        assertNotNull(range);
        return range;
    }

    @Test
    void requiresMinimumVersion() {
        assertEquals(VersionUtils.v1_21_11, new AttackRangeComponent().minimumVersion());
    }

    @Test
    void supportedOnCurrentTestServer() {
        // The test server runs 26.1.2, which is newer than 1.21.11.
        assertTrue(new AttackRangeComponent().isSupportedOnCurrentVersion());
    }

    @Test
    void emptyConfigAppliesDefaults() {
        AttackRange range = applied(new YamlConfiguration());

        assertEquals(0F, range.minReach(), DELTA);
        assertEquals(3F, range.maxReach(), DELTA);
        assertEquals(0F, range.minCreativeReach(), DELTA);
        assertEquals(5F, range.maxCreativeReach(), DELTA);
        assertEquals(0.3F, range.hitboxMargin(), DELTA);
        assertEquals(1F, range.mobFactor(), DELTA);
    }

    @Test
    void reachAboveMaxIsClamped() {
        AttackRange range = applied(yamlOf("max_reach: 100.0"));

        assertEquals(64F, range.maxReach(), DELTA); // clamped from 100 to the 64 max
    }

    @Test
    void reachBelowMinIsClamped() {
        AttackRange range = applied(yamlOf("min_reach: -10.0"));

        assertEquals(0F, range.minReach(), DELTA); // clamped up to the 0 min
    }

    @Test
    void customValuesAreApplied() {
        AttackRange range = applied(yamlOf("""
                min_reach: 1.0
                max_reach: 6.0
                mob_factor: 1.5
                """));

        assertEquals(1F, range.minReach(), DELTA);
        assertEquals(6F, range.maxReach(), DELTA);
        assertEquals(1.5F, range.mobFactor(), DELTA);
    }

    @Test
    void hitboxMarginIsApplied() {
        AttackRange range = applied(yamlOf("hitbox_margin: 0.75"));

        assertEquals(0.75F, range.hitboxMargin(), DELTA);
    }
}
