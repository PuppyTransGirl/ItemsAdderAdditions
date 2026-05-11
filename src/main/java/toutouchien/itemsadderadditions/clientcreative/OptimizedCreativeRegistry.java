package toutouchien.itemsadderadditions.clientcreative;

import java.util.List;

public record OptimizedCreativeRegistry(
        int hash,
        List<ItemEntry> items,
        List<TabEntry> tabs
) {
    public record ItemEntry(
            String namespacedId,
            byte[] encodedStack
    ) {
    }

    public record TabEntry(
            String id,
            String title,
            int iconIndex,
            int[] itemIndexes
    ) {
    }
}
