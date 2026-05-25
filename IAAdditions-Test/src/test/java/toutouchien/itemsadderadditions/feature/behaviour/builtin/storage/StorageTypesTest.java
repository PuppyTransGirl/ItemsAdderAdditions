package toutouchien.itemsadderadditions.feature.behaviour.builtin.storage;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StorageTypesTest {
    @Test
    void resolveStorageLowercase() {
        assertEquals(StorageType.STORAGE, StorageTypes.resolve("storage", "ns:chest"));
    }

    @Test
    void resolveShulkerMixedCase() {
        assertEquals(StorageType.SHULKER, StorageTypes.resolve("ShUlKeR", "ns:box"));
    }

    @Test
    void resolveDisposalUppercase() {
        assertEquals(StorageType.DISPOSAL, StorageTypes.resolve("DISPOSAL", "ns:trash"));
    }

    @Test
    void resolveUnknownDefaultsToStorage() {
        assertEquals(StorageType.STORAGE, StorageTypes.resolve("unknown", "ns:chest"));
    }

    @Test
    void resolveBlankDefaultsToStorage() {
        assertEquals(StorageType.STORAGE, StorageTypes.resolve("", "ns:chest"));
    }
}
