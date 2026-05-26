package toutouchien.itemsadderadditions.feature.advancement;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import toutouchien.itemsadderadditions.feature.advancement.trigger.RuntimeTrigger;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AdvancementLoaderPredicateParsingTest {
    private static YamlConfiguration yamlOf(String yaml) {
        var cfg = new YamlConfiguration();
        try {
            cfg.loadFromString(yaml);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return cfg;
    }

    private static List<AdvancementDefinition> load(String yaml) {
        return AdvancementLoader.loadAll("testns", yamlOf(yaml).getConfigurationSection("advancements"));
    }

    private static AdvancementCriterionDefinition loadSingleCriterion(String trigger, String conditionsYaml) {
        String conditions = conditionsYaml.isBlank() ? "" : "        conditions:\n" + conditionsYaml.indent(10);
        var result = load("""
                advancements:
                  adv:
                    parent: root
                    display:
                      title: "Title"
                      description: "Description"
                      icon: minecraft:stone
                    criteria:
                      c1:
                        trigger: %s
                """.formatted(trigger) + conditions);
        assertEquals(1, result.size(), "Expected advancement to load for trigger: " + trigger);
        assertEquals(1, result.getFirst().criteria().size());
        return result.getFirst().criteria().getFirst();
    }

    @Test
    void fallFromHeight_yDistanceRange_parsedIntoCondition() {
        AdvancementCriterionDefinition criterion = loadSingleCriterion("fall_from_height", """
                distance:
                  y:
                    min: 50
                    max: 320
                """);

        assertEquals(RuntimeTrigger.FALL_FROM_HEIGHT, criterion.trigger());
        var condition = assertInstanceOf(AdvancementConditions.FallFromHeight.class, criterion.conditions());
        assertEquals(50.0D, condition.minDistance());
        assertEquals(320.0D, condition.maxDistance());
    }

    @Test
    void fallFromHeight_plainNumericDistance_setsMinimumOnly() {
        AdvancementCriterionDefinition criterion = loadSingleCriterion("fall_from_height", """
                distance: 12.5
                """);

        var condition = assertInstanceOf(AdvancementConditions.FallFromHeight.class, criterion.conditions());
        assertEquals(12.5D, condition.minDistance());
        assertEquals(Double.MAX_VALUE, condition.maxDistance());
    }

    @Test
    void playerPredicate_objectFormat_isStoredOnCriterion() {
        AdvancementCriterionDefinition criterion = loadSingleCriterion("slept_in_bed", """
                player:
                  flags:
                    is_sneaking: true
                """);

        assertInstanceOf(AdvancementConditions.None.class, criterion.conditions());
        assertNotSame(AdvancementPlayerPredicate.ANY, criterion.playerPredicate());
        assertEquals(1, criterion.playerPredicate().predicates().size());
    }

    @Test
    void playerPredicate_listFormat_isStoredOnCriterion() {
        AdvancementCriterionDefinition criterion = loadSingleCriterion("slept_in_bed", """
                player:
                  - condition: minecraft:entity_properties
                    predicate:
                      flags:
                        is_sneaking: true
                  - condition: minecraft:entity_properties
                    predicate:
                      type_specific:
                        type: minecraft:player
                        gamemode: survival
                """);

        assertNotSame(AdvancementPlayerPredicate.ANY, criterion.playerPredicate());
        assertEquals(2, criterion.playerPredicate().predicates().size());
    }

    @Test
    void impossibleTrigger_ignoresPlayerPredicate() {
        AdvancementCriterionDefinition criterion = loadSingleCriterion("impossible", """
                player:
                  flags:
                    is_sneaking: true
                """);

        assertSame(AdvancementPlayerPredicate.ANY, criterion.playerPredicate());
    }
}
