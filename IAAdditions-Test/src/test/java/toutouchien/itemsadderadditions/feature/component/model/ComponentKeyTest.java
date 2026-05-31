package toutouchien.itemsadderadditions.feature.component.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ComponentKeyTest {
    @Test
    void shortKeyGetsMinecraftNamespace() {
        assertEquals("minecraft:custom_data", ComponentKey.from("custom_data").normalized());
    }

    @Test
    void shortKeyWithUppercaseIsLowercased() {
        assertEquals("minecraft:custom_data", ComponentKey.from("Custom_Data").normalized());
    }

    @Test
    void alreadyNamespacedKeyIsPreserved() {
        assertEquals("minecraft:custom_data", ComponentKey.from("minecraft:custom_data").normalized());
    }

    @Test
    void customNamespacePreserved() {
        assertEquals("mymod:foo", ComponentKey.from("mymod:foo").normalized());
    }

    @Test
    void whitespaceTrimmed() {
        assertEquals("minecraft:rarity", ComponentKey.from("  rarity  ").normalized());
    }

    @Test
    void validKeyPassesIsValid() {
        assertTrue(ComponentKey.from("custom_data").isValid());
        assertTrue(ComponentKey.from("minecraft:custom_data").isValid());
        assertTrue(ComponentKey.from("mymod:something").isValid());
    }

    @Test
    void emptyPathIsInvalid() {
        ComponentKey key = new ComponentKey("minecraft:");
        assertFalse(key.isValid());
    }

    @Test
    void emptyNamespaceIsInvalid() {
        ComponentKey key = new ComponentKey(":custom_data");
        assertFalse(key.isValid());
    }

    @Test
    void colonOnlyIsInvalid() {
        ComponentKey key = new ComponentKey(":");
        assertFalse(key.isValid());
    }

    @Test
    void toStringReturnsNormalized() {
        assertEquals("minecraft:rarity", ComponentKey.from("rarity").toString());
    }
}
