package toutouchien.itemsadderadditions.nms.creative;

import dev.lone.itemsadder.api.CustomStack;
import io.papermc.paper.adventure.PaperAdventure;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.decoration.PaintingVariant;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.CraftServer;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.utils.other.Log;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@NullMarked
public final class RegistryInjector_v1_21_6 {
    private RegistryInjector_v1_21_6() {
        throw new IllegalStateException("Static class");
    }

    // Reflection objects resolved once at class-init time and reused on every reload.
    private static final java.lang.reflect.Method BIND_METHOD;
    private static final java.lang.reflect.Field FROZEN_FIELD;
    /**
     * Keys already injected into the painting-variant registry.  Lets
     * {@link #injectPaintingVariants} skip the unfreeze/refreeze cycle
     * entirely when no new items have been added since the last reload.
     */
    private static final Set<String> INJECTED_KEYS = ConcurrentHashMap.newKeySet();

    static {
        try {
            BIND_METHOD = Holder.Reference.class.getDeclaredMethod("bindValue", Object.class);
            BIND_METHOD.setAccessible(true);
            FROZEN_FIELD = MappedRegistry.class.getDeclaredField("frozen");
            FROZEN_FIELD.setAccessible(true);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public static void injectPaintingVariants(Collection<CustomStack> items) {
        // Fast path: skip the expensive unfreeze/refreeze cycle if every
        // item's key was already injected on a previous reload.
        List<CustomStack> newItems = new ArrayList<>();
        for (CustomStack item : items) {
            String keyStr = "ia_creative:" + item.getNamespace() + "_" + item.getId();
            if (!INJECTED_KEYS.contains(keyStr)) newItems.add(item);
        }
        if (newItems.isEmpty()) {
            Log.loaded("CreativeMenu", 0, "PaintingVariant(s) injected (all already present)");
            return;
        }

        Registry<PaintingVariant> registry = ((CraftServer) Bukkit.getServer())
                .getServer()
                .registryAccess()
                .lookupOrThrow(Registries.PAINTING_VARIANT);

        if (!(registry instanceof MappedRegistry<PaintingVariant> mappedRegistry))
            return;

        setRegistryFrozen(mappedRegistry, false);

        int injected = 0;
        try {
            for (CustomStack item : newItems) {
                ResourceLocation key = ResourceLocation.fromNamespaceAndPath(
                        "ia_creative",
                        item.getNamespace() + "_" + item.getId()
                );

                if (mappedRegistry.containsKey(key))
                    continue;

                PaintingVariant variant = new PaintingVariant(
                        1, 1, key,
                        Optional.of(PaperAdventure.asVanilla(item.itemName())),
                        Optional.of(Component.literal(item.getNamespacedID()))
                );

                Registry.register(mappedRegistry, key, variant);

                mappedRegistry.get(ResourceKey.create(Registries.PAINTING_VARIANT, key))
                        .ifPresent(holder -> bindHolder(holder, variant));

                INJECTED_KEYS.add("ia_creative:" + item.getNamespace() + "_" + item.getId());
                injected++;
            }
        } finally {
            setRegistryFrozen(mappedRegistry, true);
        }

        Log.loaded("CreativeMenu", injected, "new PaintingVariant(s) injected");
    }

    /**
     * Uses reflection to call the protected {@code bindValue} method on a
     * {@link Holder.Reference}, which is required after manually registering an
     * entry into a {@link MappedRegistry} post-freeze.
     */
    private static void bindHolder(Holder.Reference<PaintingVariant> holder, PaintingVariant value) {
        try {
            BIND_METHOD.invoke(holder, value);
        } catch (Exception e) {
            Log.error("CreativeMenu", "Failed to bind holder value via reflection", e);
        }
    }

    private static void setRegistryFrozen(MappedRegistry<?> registry, boolean frozen) {
        try {
            FROZEN_FIELD.set(registry, frozen);
        } catch (Exception e) {
            Log.error("CreativeMenu", "Failed to toggle registry lock", e);
        }
    }
}
