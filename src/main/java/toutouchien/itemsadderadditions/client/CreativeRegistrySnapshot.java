package toutouchien.itemsadderadditions.client;

import java.util.List;

public record CreativeRegistrySnapshot(List<Category> categories, byte[] encoded, int itemCount) {
    public record Category(String id, String displayName, byte[] iconNbt, List<Entry> items) {
    }

    public record Entry(String id, byte[] itemNbt) {
    }
}
