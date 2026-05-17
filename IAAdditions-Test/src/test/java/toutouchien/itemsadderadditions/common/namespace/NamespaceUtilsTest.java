package toutouchien.itemsadderadditions.common.namespace;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NamespaceUtilsTest {
    @Test
    void stripNorthSuffix() {
        assertEquals("myns:block", NamespaceUtils.stripRotationSuffix("myns:block_north"));
    }

    @Test
    void stripSouthSuffix() {
        assertEquals("myns:block", NamespaceUtils.stripRotationSuffix("myns:block_south"));
    }

    @Test
    void stripEastSuffix() {
        assertEquals("myns:block", NamespaceUtils.stripRotationSuffix("myns:block_east"));
    }

    @Test
    void stripWestSuffix() {
        assertEquals("myns:block", NamespaceUtils.stripRotationSuffix("myns:block_west"));
    }

    @Test
    void stripUpSuffix() {
        assertEquals("myns:block", NamespaceUtils.stripRotationSuffix("myns:block_up"));
    }

    @Test
    void stripDownSuffix() {
        assertEquals("myns:block", NamespaceUtils.stripRotationSuffix("myns:block_down"));
    }

    @Test
    void noSuffixReturnedUnchanged() {
        assertEquals("myns:block", NamespaceUtils.stripRotationSuffix("myns:block"));
    }

    @Test
    void stripBarePathNorthSuffix() {
        assertEquals("block", NamespaceUtils.stripRotationSuffix("block_north"));
    }

    @Test
    void stripBarePathWestSuffix() {
        assertEquals("block", NamespaceUtils.stripRotationSuffix("block_west"));
    }

    @Test
    void barePathNoSuffixReturnedUnchanged() {
        assertEquals("block", NamespaceUtils.stripRotationSuffix("block"));
    }

    @Test
    void exactSuffixAloneNotStripped() {
        // "_north" alone has nothing before it, so it stays
        assertEquals("_north", NamespaceUtils.stripRotationSuffix("_north"));
    }

    @Test
    void namespacePreservedAfterStrip() {
        assertEquals("customns:chair", NamespaceUtils.stripRotationSuffix("customns:chair_south"));
    }


    @Test
    void normalizeAddsNamespace() {
        assertEquals("myns:item", NamespaceUtils.normalizeID("myns", "item"));
    }

    @Test
    void normalizeAlreadyNamespacedPassedThrough() {
        assertEquals("myns:item", NamespaceUtils.normalizeID("other", "myns:item"));
    }

    @Test
    void normalizeLowercasesId() {
        assertEquals("myns:item", NamespaceUtils.normalizeID("myns", "ITEM"));
    }

    @Test
    void normalizeNullNamespaceBareId() {
        assertEquals("item", NamespaceUtils.normalizeID(null, "item"));
    }

    @Test
    void normalizeLowercasesNamespace() {
        assertEquals("myns:item", NamespaceUtils.normalizeID("MYNS", "item"));
    }


    @Test
    void matchesExactId() {
        assertTrue(NamespaceUtils.matchesWithRotation("myns:block", "myns:block"));
    }

    @Test
    void matchesWithNorthSuffix() {
        assertTrue(NamespaceUtils.matchesWithRotation("myns:block_north", "myns:block"));
    }

    @Test
    void matchesWithSouthSuffix() {
        assertTrue(NamespaceUtils.matchesWithRotation("myns:block_south", "myns:block"));
    }

    @Test
    void doesNotMatchDifferentBase() {
        assertFalse(NamespaceUtils.matchesWithRotation("myns:chair_north", "myns:table"));
    }


    @Test
    void extractNamespace() {
        assertEquals("myns", NamespaceUtils.namespace("myns:item"));
    }

    @Test
    void extractId() {
        assertEquals("item", NamespaceUtils.id("myns:item"));
    }
}
