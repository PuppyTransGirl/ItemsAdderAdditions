package toutouchien.itemsadderadditions.nms;

import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.PaintingVariantTags;
import net.minecraft.world.entity.decoration.PaintingVariant;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.entity.CraftPainting;
import org.bukkit.entity.Painting;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.common.logging.Log;
import toutouchien.itemsadderadditions.nms.api.INmsPaintingHandler;
import toutouchien.itemsadderadditions.nms.api.painting.NmsPaintingVariant;
import toutouchien.itemsadderadditions.nms.painting.RegistryInjector_v1_21_8;

import java.util.*;

@NullMarked
final class NmsPaintingHandler_v1_21_8 implements INmsPaintingHandler {
    private static final String TAG = "CustomPaintings";
    private static final Set<String> KNOWN_MANAGED_VARIANT_IDS = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private static final java.lang.reflect.Field FROZEN_FIELD;

    static {
        try {
            FROZEN_FIELD = MappedRegistry.class.getDeclaredField("frozen");
            FROZEN_FIELD.setAccessible(true);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static int updatePlaceableRegistryTag(Collection<String> managedVariantIds, Collection<String> randomVariantIds) {
        Registry<PaintingVariant> registry = registry();
        if (!(registry instanceof MappedRegistry<PaintingVariant> mappedRegistry)) {
            Log.warn(TAG, "Could not update minecraft:placeable painting tag: registry is not mutable.");
            return 0;
        }

        Set<String> managedIds = new HashSet<>(managedVariantIds);
        managedIds.addAll(KNOWN_MANAGED_VARIANT_IDS);
        Set<String> randomIds = new HashSet<>(randomVariantIds);
        Set<String> before = currentManagedPlaceableIds(registry, managedIds);

        LinkedHashMap<String, Holder<PaintingVariant>> placeableById = new LinkedHashMap<>();
        registry.get(PaintingVariantTags.PLACEABLE).ifPresent(named -> named.stream().forEach(holder -> {
            String id = holderId(holder);
            if (id == null) return;
            if (managedIds.contains(id) && !randomIds.contains(id)) return;

            placeableById.putIfAbsent(id, holder);
        }));

        for (String variantId : randomIds) {
            Optional<Holder.Reference<PaintingVariant>> holder = findHolder(variantId);
            if (holder.isEmpty()) continue;

            placeableById.putIfAbsent(variantId, holder.get());
        }

        Set<String> after = new LinkedHashSet<>();
        for (String variantId : placeableById.keySet()) {
            if (managedIds.contains(variantId)) after.add(variantId);
        }

        if (before.equals(after)) {
            rememberManagedVariantIds(managedVariantIds);
            return 0;
        }

        setRegistryFrozen(mappedRegistry, false);
        try {
            mappedRegistry.bindTag(PaintingVariantTags.PLACEABLE, new ArrayList<>(placeableById.values()));
        } finally {
            setRegistryFrozen(mappedRegistry, true);
        }

        int changed = changedCount(before, after);
        rememberManagedVariantIds(managedVariantIds);
        return changed;
    }

    private static void rememberManagedVariantIds(Collection<String> managedVariantIds) {
        KNOWN_MANAGED_VARIANT_IDS.clear();
        KNOWN_MANAGED_VARIANT_IDS.addAll(managedVariantIds);
    }

    private static Set<String> currentManagedPlaceableIds(Registry<PaintingVariant> registry, Set<String> managedIds) {
        Set<String> current = new LinkedHashSet<>();
        registry.get(PaintingVariantTags.PLACEABLE).ifPresent(named -> named.stream().forEach(holder -> {
            String id = holderId(holder);
            if (id != null && managedIds.contains(id)) current.add(id);
        }));
        return current;
    }

    private static int changedCount(Set<String> before, Set<String> after) {
        int changed = 0;
        for (String id : before) {
            if (!after.contains(id)) changed++;
        }
        for (String id : after) {
            if (!before.contains(id)) changed++;
        }
        return changed;
    }

    private static void setRegistryFrozen(MappedRegistry<?> registry, boolean frozen) {
        try {
            FROZEN_FIELD.set(registry, frozen);
        } catch (IllegalAccessException e) {
            Log.error(TAG, "Failed to toggle painting registry lock", e);
        }
    }

    private static String holderId(Holder<PaintingVariant> holder) {
        return holder.unwrapKey()
                .map(key -> key.location().toString())
                .orElse(null);
    }

    private static Optional<Holder.Reference<PaintingVariant>> findHolder(String variantId) {
        ResourceLocation key = resourceLocation(variantId);
        return registry().get(ResourceKey.create(Registries.PAINTING_VARIANT, key));
    }

    private static Registry<PaintingVariant> registry() {
        return ((CraftServer) Bukkit.getServer())
                .getServer()
                .registryAccess()
                .lookupOrThrow(Registries.PAINTING_VARIANT);
    }

    private static ResourceLocation resourceLocation(String id) {
        int sep = id.indexOf(':');
        return ResourceLocation.fromNamespaceAndPath(
                id.substring(0, sep),
                id.substring(sep + 1)
        );
    }

    @Override
    public void injectPaintingVariants(Collection<NmsPaintingVariant> variants) {
        RegistryInjector_v1_21_8.injectPaintingVariants(variants);
    }

    @Override
    public void updateRandomPlaceableVariants(Collection<String> managedVariantIds, Collection<String> randomVariantIds) {
        int changed = updatePlaceableRegistryTag(managedVariantIds, randomVariantIds);
        if (changed > 0) {
            Log.info(TAG, "Updated vanilla random placement state for {} custom painting(s).", changed);
        }
    }

    @Override
    public boolean applyVariant(Painting painting, String variantId) {
        Optional<Holder.Reference<PaintingVariant>> holder = findHolder(variantId);
        if (holder.isEmpty()) {
            Log.warn(TAG, "Painting variant '{}' is not registered.", variantId);
            return false;
        }

        ((CraftPainting) painting).getHandle().setVariant(holder.get());
        return true;
    }

    @Override
    public boolean isStillValid(Painting painting) {
        return ((CraftPainting) painting).getHandle().survives();
    }
}
