package toutouchien.itemsadderadditions.feature.advancement.trigger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RuntimeTriggerTest {

    @ParameterizedTest
    @EnumSource(RuntimeTrigger.class)
    void fromYaml_allEnumValues_uppercase_roundtrip(RuntimeTrigger trigger) {
        assertEquals(trigger, RuntimeTrigger.fromYaml(trigger.name()));
    }

    @ParameterizedTest
    @EnumSource(RuntimeTrigger.class)
    void fromYaml_allEnumValues_lowercase_roundtrip(RuntimeTrigger trigger) {
        assertEquals(trigger, RuntimeTrigger.fromYaml(trigger.name().toLowerCase()));
    }

    @Test
    void fromYaml_unknown_returnsNull() {
        assertNull(RuntimeTrigger.fromYaml("not_a_trigger"));
    }

    @Test
    void fromYaml_empty_returnsNull() {
        assertNull(RuntimeTrigger.fromYaml(""));
    }

    @Test
    void fromYaml_mixedCase_matches() {
        assertEquals(RuntimeTrigger.OBTAIN_ITEM, RuntimeTrigger.fromYaml("Obtain_Item"));
    }
}
