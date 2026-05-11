package toutouchien.itemsadderadditions.clientcreative;

import dev.lone.itemsadder.api.CustomEntity;
import dev.lone.itemsadder.api.CustomFurniture;
import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public final class CreativeRegistryBuilder {
    private final ItemStackNetworkEncoder itemStackEncoder;

    public CreativeRegistryBuilder(ItemStackNetworkEncoder itemStackEncoder) {
        this.itemStackEncoder = itemStackEncoder;
    }

    public OptimizedCreativeRegistry build() {
        EnumMap<TabBucket, List<String>> tabItems = new EnumMap<>(TabBucket.class);
        for (TabBucket bucket : TabBucket.values()) {
            tabItems.put(bucket, new ArrayList<>());
        }

        Set<String> ids = collectAllKnownNamespacedIds();

        for (String namespacedId : ids) {
            TabBucket bucket = CreativeItemClassifier.classifyItem(namespacedId);
            tabItems.get(bucket).add(namespacedId);
        }

        Map<String, Integer> itemIndexes = new LinkedHashMap<>();
        List<OptimizedCreativeRegistry.ItemEntry> itemEntries = new ArrayList<>();
        List<OptimizedCreativeRegistry.TabEntry> tabEntries = new ArrayList<>();

        for (TabBucket bucket : TabBucket.values()) {
            List<Integer> indexes = new ArrayList<>();

            for (String namespacedId : tabItems.get(bucket)) {
                Integer index = itemIndexes.computeIfAbsent(namespacedId, id -> {
                    byte[] encodedStack = encodeCustomStack(id);
                    if (encodedStack == null || encodedStack.length == 0) {
                        return -1;
                    }

                    int nextIndex = itemEntries.size();
                    itemEntries.add(new OptimizedCreativeRegistry.ItemEntry(id, encodedStack));
                    return nextIndex;
                });

                if (index != null && index >= 0) {
                    indexes.add(index);
                }
            }

            int[] indexArray = indexes.stream().mapToInt(Integer::intValue).toArray();
            int iconIndex = indexArray.length == 0 ? -1 : indexArray[0];

            tabEntries.add(new OptimizedCreativeRegistry.TabEntry(
                    bucket.id,
                    bucket.displayName,
                    iconIndex,
                    indexArray
            ));
        }

        int hash = stableHash(itemEntries, tabEntries);
        return new OptimizedCreativeRegistry(hash, List.copyOf(itemEntries), List.copyOf(tabEntries));
    }

    private Set<String> collectAllKnownNamespacedIds() {
        LinkedHashSet<String> ids = new LinkedHashSet<>();

        addAllSafe(ids, CustomStack.getNamespacedIdsInRegistry());
        addAllSafe(ids, CustomFurniture.getNamespacedIdsInRegistry());
        addAllSafe(ids, CustomEntity.getNamespacedIdsInRegistry());

        return ids;
    }

    private void addAllSafe(Set<String> target, Set<String> incoming) {
        try {
            if (incoming != null) {
                target.addAll(incoming);
            }
        } catch (Throwable ignored) {
        }
    }

    private byte[] encodeCustomStack(String namespacedId) {
        try {
            CustomStack customStack = CustomStack.getInstance(namespacedId);
            if (customStack == null) {
                return null;
            }

            ItemStack bukkitStack = customStack.getItemStack();
            if (bukkitStack == null || bukkitStack.getType().isAir()) {
                return null;
            }

            return itemStackEncoder.encode(bukkitStack);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private int stableHash(
            List<OptimizedCreativeRegistry.ItemEntry> items,
            List<OptimizedCreativeRegistry.TabEntry> tabs
    ) {
        int result = 1;

        for (OptimizedCreativeRegistry.ItemEntry item : items) {
            result = 31 * result + item.namespacedId().hashCode();
            result = 31 * result + Arrays.hashCode(item.encodedStack());
        }

        for (OptimizedCreativeRegistry.TabEntry tab : tabs) {
            result = 31 * result + tab.id().hashCode();
            result = 31 * result + tab.title().hashCode();
            result = 31 * result + tab.iconIndex();
            result = 31 * result + Arrays.hashCode(tab.itemIndexes());
        }

        return result;
    }
}
