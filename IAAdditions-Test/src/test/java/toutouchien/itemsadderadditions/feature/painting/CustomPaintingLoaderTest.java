package toutouchien.itemsadderadditions.feature.painting;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import toutouchien.itemsadderadditions.common.loading.ConfigFileCategory;
import toutouchien.itemsadderadditions.common.loading.ConfigFileRegistry;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CustomPaintingLoaderTest {
    @TempDir
    Path tempDir;

    private List<CustomPaintingDefinition> load(String yaml) throws Exception {
        Path file = tempDir.resolve("paintings.yml");
        Files.writeString(file, yaml);
        ConfigFileRegistry registry = ConfigFileRegistry.scan(tempDir.toFile());
        return new CustomPaintingLoader().loadAll(registry.getFiles(ConfigFileCategory.PAINTINGS));
    }

    @Test
    void loadAllValidPainting() throws Exception {
        List<CustomPaintingDefinition> result = load("""
                info:
                  namespace: gallery
                paintings:
                  sunset:
                    width: 2
                    height: 3
                    asset: sunset_asset
                    title: Sunset
                    author: Toutou
                    include_in_random: true
                """);

        assertEquals(1, result.size());
        CustomPaintingDefinition def = result.getFirst();
        assertEquals("gallery:sunset", def.variantId());
        assertEquals(2, def.width());
        assertEquals(3, def.height());
        assertEquals("gallery:sunset_asset", def.assetId());
        assertEquals("Sunset", def.title());
        assertEquals("Toutou", def.author());
        assertTrue(def.includeInRandom());
        assertNull(def.itemId());
    }

    @Test
    void loadAllAlreadyNamespacedAssetIsPreservedAndLowercased() throws Exception {
        List<CustomPaintingDefinition> result = load("""
                info:
                  namespace: gallery
                paintings:
                  one:
                    width: 1
                    height: 1
                    asset: OtherNS:Asset_Path
                """);

        assertEquals("otherns:asset_path", result.getFirst().assetId());
    }

    @Test
    void blankTitleAndAuthorBecomeNull() throws Exception {
        List<CustomPaintingDefinition> result = load("""
                info:
                  namespace: gallery
                paintings:
                  one:
                    width: 1
                    height: 1
                    asset: asset
                    title: "   "
                    author: ""
                """);

        assertNull(result.getFirst().title());
        assertNull(result.getFirst().author());
    }

    @Test
    void disabledPaintingIsSkipped() throws Exception {
        List<CustomPaintingDefinition> result = load("""
                info:
                  namespace: gallery
                paintings:
                  disabled:
                    enabled: false
                    width: 1
                    height: 1
                    asset: asset
                """);

        assertTrue(result.isEmpty());
    }

    @Test
    void missingInfoSectionReturnsEmpty() throws Exception {
        List<CustomPaintingDefinition> result = load("""
                paintings:
                  one:
                    width: 1
                    height: 1
                    asset: asset
                """);

        assertTrue(result.isEmpty());
    }

    @Test
    void blankNamespaceReturnsEmpty() throws Exception {
        List<CustomPaintingDefinition> result = load("""
                info:
                  namespace: "   "
                paintings:
                  one:
                    width: 1
                    height: 1
                    asset: asset
                """);

        assertTrue(result.isEmpty());
    }

    @Test
    void missingPaintingsSectionReturnsEmpty() throws Exception {
        Path file = tempDir.resolve("no_paintings.yml");
        Files.writeString(file, "info:\n  namespace: gallery\nitems:\n  x: {}\n");
        ConfigFileRegistry registry = ConfigFileRegistry.scan(tempDir.toFile());

        List<CustomPaintingDefinition> result = new CustomPaintingLoader().loadAll(registry.getFiles(ConfigFileCategory.PAINTINGS));

        assertTrue(result.isEmpty());
    }

    @Test
    void invalidVariantIdIsSkipped() throws Exception {
        List<CustomPaintingDefinition> result = load("""
                info:
                  namespace: gallery
                paintings:
                  "bad id":
                    width: 1
                    height: 1
                    asset: asset
                """);

        assertTrue(result.isEmpty());
    }

    @Test
    void invalidDimensionsAreSkipped() throws Exception {
        List<CustomPaintingDefinition> result = load("""
                info:
                  namespace: gallery
                paintings:
                  zero_width:
                    width: 0
                    height: 1
                    asset: asset
                  negative_height:
                    width: 1
                    height: -1
                    asset: asset
                """);

        assertTrue(result.isEmpty());
    }

    @Test
    void missingAssetIsSkipped() throws Exception {
        List<CustomPaintingDefinition> result = load("""
                info:
                  namespace: gallery
                paintings:
                  one:
                    width: 1
                    height: 1
                """);

        assertTrue(result.isEmpty());
    }

    @Test
    void invalidAssetIsSkipped() throws Exception {
        List<CustomPaintingDefinition> result = load("""
                info:
                  namespace: gallery
                paintings:
                  one:
                    width: 1
                    height: 1
                    asset: "bad asset"
                """);

        assertTrue(result.isEmpty());
    }

    @Test
    void multiplePaintingsKeepsValidEntriesOnly() throws Exception {
        List<CustomPaintingDefinition> result = load("""
                info:
                  namespace: gallery
                paintings:
                  first:
                    width: 1
                    height: 1
                    asset: asset_a
                  disabled:
                    enabled: false
                    width: 1
                    height: 1
                    asset: asset_b
                  second:
                    width: 4
                    height: 2
                    asset: asset_c
                """);

        assertEquals(List.of("gallery:first", "gallery:second"),
                result.stream().map(CustomPaintingDefinition::variantId).toList());
    }
}
