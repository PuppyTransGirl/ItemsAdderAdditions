package toutouchien.itemsadderadditions.clientcreative;

import dev.lone.itemsadder.api.CustomEntity;
import dev.lone.itemsadder.api.CustomFurniture;
import dev.lone.itemsadder.api.CustomStack;

import java.util.Collections;
import java.util.Set;

public final class CreativeItemClassifier {
    private CreativeItemClassifier() {
    }

    public static TabBucket classifyItem(String namespacedId) {
        if (namespacedId == null || namespacedId.isBlank()) {
            return TabBucket.ITEM;
        }

        if (isComplexFurniture(namespacedId)) {
            return TabBucket.COMPLEX_FURNITURE;
        }

        if (isFurniture(namespacedId)) {
            return TabBucket.FURNITURE;
        }

        CustomStack stack = getCustomStack(namespacedId);
        if (stack != null && isBlock(stack)) {
            return TabBucket.BLOCK;
        }

        return TabBucket.ITEM;
    }

    private static boolean isComplexFurniture(String namespacedId) {
        return containsSafe(CustomEntity.getNamespacedIdsInRegistry(), namespacedId);
    }

    private static boolean isFurniture(String namespacedId) {
        if (containsSafe(CustomFurniture.getNamespacedIdsInRegistry(), namespacedId)) {
            return true;
        }

        /*
         * Fallback for IA builds where furniture IDs are not fully exposed by the registry set.
         * Static dispatch resolves this to CustomStack#getInstance, but the runtime object can
         * still be a CustomFurniture instance. If the cast fails, this returns false.
         */
        try {
            CustomStack stack = CustomFurniture.getInstance(namespacedId);
            if (stack instanceof CustomFurniture) {
                return true;
            }

            if (stack != null) {
                try {
                    CustomFurniture ignored = (CustomFurniture) stack;
                    return true;
                } catch (ClassCastException ignored) {
                    return false;
                }
            }
        } catch (Throwable ignored) {
            return false;
        }

        return false;
    }

    private static CustomStack getCustomStack(String namespacedId) {
        try {
            return CustomStack.getInstance(namespacedId);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean isBlock(CustomStack stack) {
        try {
            return stack.isBlock();
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean containsSafe(Set<String> ids, String namespacedId) {
        try {
            return ids != null && ids.contains(namespacedId);
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static Set<String> safeCustomStackIds() {
        try {
            Set<String> ids = CustomStack.getNamespacedIdsInRegistry();
            return ids == null ? Collections.emptySet() : ids;
        } catch (Throwable ignored) {
            return Collections.emptySet();
        }
    }
}
