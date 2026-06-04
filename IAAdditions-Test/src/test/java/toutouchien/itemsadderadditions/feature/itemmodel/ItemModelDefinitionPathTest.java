package toutouchien.itemsadderadditions.feature.itemmodel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemModelDefinitionPathTest {
    @Test
    void defaultPath_usesItemNamespaceAndId() {
        ItemModelDefinitionPath path = ItemModelDefinitionPath.parse(null, "my_pack", "ruby", "my_pack:ruby").orElseThrow();

        assertEquals("my_pack:ruby", path.id());
        assertEquals("assets/my_pack/items/ruby.json", path.resourcePackRelativePath());
    }

    @Test
    void namespacedPath_overridesNamespaceAndStripsJson() {
        ItemModelDefinitionPath path = ItemModelDefinitionPath.parse(
                "other_pack:weapons/ruby_sword.json",
                "my_pack",
                "ruby_sword",
                "my_pack:ruby_sword"
        ).orElseThrow();

        assertEquals("other_pack:weapons/ruby_sword", path.id());
        assertEquals("assets/other_pack/items/weapons/ruby_sword.json", path.resourcePackRelativePath());
    }

    @Test
    void unsafePath_isRejected() {
        assertTrue(ItemModelDefinitionPath.parse("../bad", "my_pack", "ruby", "my_pack:ruby").isEmpty());
        assertTrue(ItemModelDefinitionPath.parse("/absolute", "my_pack", "ruby", "my_pack:ruby").isEmpty());
        assertTrue(ItemModelDefinitionPath.parse("bad\\path", "my_pack", "ruby", "my_pack:ruby").isEmpty());
    }
}
