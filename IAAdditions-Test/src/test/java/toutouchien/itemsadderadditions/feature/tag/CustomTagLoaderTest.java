package toutouchien.itemsadderadditions.feature.tag;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import toutouchien.itemsadderadditions.common.loading.ConfigFileCategory;
import toutouchien.itemsadderadditions.common.loading.ConfigFileRegistry;
import toutouchien.itemsadderadditions.common.namespace.CustomTagRegistry;
import toutouchien.itemsadderadditions.common.namespace.CustomTagType;
import toutouchien.itemsadderadditions.common.namespace.NamespaceUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomTagLoaderTest {
    @TempDir
    Path tempDir;

    @AfterEach
    void clearRegistry() {
        NamespaceUtils.clearCustomTagRegistry();
    }

    @Test
    void loadsItemTagsWithDefaultTypeAndNormalizesValues() throws Exception {
        write("tags.yml", """
                info:
                  namespace: my_pack

                tags:
                  ruby_items:
                    values:
                      - ruby
                      - minecraft:diamond
                      - mmoitems:sword:ruby_sword
                """);

        CustomTagRegistry tags = load();

        assertEquals(1, tags.tagCount());
        assertEquals(List.of(
                "my_pack:ruby",
                "minecraft:diamond",
                "mmoitems:sword:ruby_sword"
        ), tags.values("my_pack:ruby_items", CustomTagType.ITEM));
    }

    @Test
    void loadsBlockFurnitureAndRecipeTags() throws Exception {
        write("tags.yml", """
                info:
                  namespace: my_pack

                tags:
                  ruby_blocks:
                    type: blocks
                    values:
                      - ruby_ore
                      - minecraft:diamond_block
                  ruby_furnitures:
                    type: furniture
                    values:
                      - ruby_chair
                  sword_recipes:
                    type: recipes
                    values:
                      - ruby_sword_recipe
                      - minecraft:diamond_sword
                """);

        CustomTagRegistry tags = load();

        assertEquals(List.of("my_pack:ruby_ore", "minecraft:diamond_block"),
                tags.values("my_pack:ruby_blocks", CustomTagType.BLOCK));
        assertEquals(List.of("my_pack:ruby_chair"),
                tags.values("my_pack:ruby_furnitures", CustomTagType.FURNITURE));
        assertEquals(List.of("my_pack:ruby_sword_recipe", "minecraft:diamond_sword"),
                tags.values("my_pack:sword_recipes", CustomTagType.RECIPE));
    }

    @Test
    void resolvesLocalNestedAndCrossNamespaceTagsWithDeduplication() throws Exception {
        write("my.yml", """
                info:
                  namespace: my_pack

                tags:
                  ruby_armor:
                    values:
                      - ruby_helmet
                      - ruby_chestplate
                  ruby_items:
                    values:
                      - "#ruby_armor"
                      - "#other_pack:gems"
                      - ruby_helmet
                      - minecraft:diamond
                """);
        write("other.yml", """
                info:
                  namespace: other_pack

                tags:
                  gems:
                    values:
                      - ruby
                      - sapphire
                """);

        CustomTagRegistry tags = load();

        assertEquals(List.of(
                "my_pack:ruby_helmet",
                "my_pack:ruby_chestplate",
                "other_pack:ruby",
                "other_pack:sapphire",
                "minecraft:diamond"
        ), tags.values("my_pack:ruby_items", CustomTagType.ITEM));
    }

    @Test
    void circularReferencesDoNotCrashAndResolveToEmptyValues() throws Exception {
        write("tags.yml", """
                info:
                  namespace: my_pack

                tags:
                  a:
                    values:
                      - "#b"
                  b:
                    values:
                      - "#a"
                """);

        CustomTagRegistry tags = load();

        assertEquals(List.of(), tags.values("my_pack:a", CustomTagType.ITEM));
        assertEquals(List.of(), tags.values("my_pack:b", CustomTagType.ITEM));
        assertTrue(tags.invalidCount() > 0);
    }

    @Test
    void typeMismatchInNestedTagIsSkipped() throws Exception {
        write("tags.yml", """
                info:
                  namespace: my_pack

                tags:
                  chairs:
                    type: furniture
                    values:
                      - ruby_chair
                  bad_items:
                    values:
                      - "#chairs"
                      - ruby
                """);

        CustomTagRegistry tags = load();

        assertEquals(List.of("my_pack:ruby"), tags.values("my_pack:bad_items", CustomTagType.ITEM));
        assertTrue(tags.invalidCount() > 0);
    }

    @Test
    void registryScanCategorizesTagFiles() throws Exception {
        write("tags.yml", """
                info:
                  namespace: my_pack
                tags:
                  ruby_items:
                    values:
                      - ruby
                """);

        ConfigFileRegistry registry = ConfigFileRegistry.scan(tempDir.toFile());

        assertEquals(1, registry.fileCount(ConfigFileCategory.TAGS));
    }

    private CustomTagRegistry load() {
        ConfigFileRegistry registry = ConfigFileRegistry.scan(tempDir.toFile());
        CustomTagRegistry tags = new CustomTagLoader().loadAll(registry);
        NamespaceUtils.setCustomTagRegistry(tags);
        return tags;
    }

    private void write(String name, String content) throws Exception {
        Files.writeString(tempDir.resolve(name), content);
    }
}
