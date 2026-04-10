package toutouchien.itemsadderadditions.nms.creative;

import dev.lone.itemsadder.api.CustomStack;
import io.papermc.paper.adventure.PaperAdventure;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.decoration.painting.PaintingVariant;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.CraftServer;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.utils.other.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Optional;

@NullMarked
public final class RegistryInjector_v26_1_1 {
    private RegistryInjector_v26_1_1() {
        throw new IllegalStateException("Static class");
    }

    public static void injectPaintingVariants(Collection<CustomStack> items) {
        Registry<PaintingVariant> registry = ((CraftServer) Bukkit.getServer())
                .getServer()
                .registryAccess()
                .lookupOrThrow(Registries.PAINTING_VARIANT);

        if (!(registry instanceof MappedRegistry<PaintingVariant> mappedRegistry))
            return;

        setRegistryFrozen(mappedRegistry, false);

        int injected = 0;
        try {
            for (CustomStack item : items) {
                Identifier key = Identifier.fromNamespaceAndPath(
                        "ia_creative",
                        item.getNamespace() + "_" + item.getId()
                );

                if (mappedRegistry.containsKey(key))
                    continue;

                PaintingVariant variant = new PaintingVariant(
                        1, 1, key,
                        Optional.of(PaperAdventure.asVanilla(item.itemName())),
                        Optional.of(Component.literal(item.getNamespace()))
                );

                Registry.register(mappedRegistry, key, variant);

                mappedRegistry.get(ResourceKey.create(Registries.PAINTING_VARIANT, key))
                        .ifPresent(holder -> bindHolder(holder, variant));

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
            Method bindMethod = Holder.Reference.class.getDeclaredMethod("bindValue", Object.class);
            bindMethod.setAccessible(true);
            bindMethod.invoke(holder, value);
        } catch (Exception e) {
            Log.error("CreativeMenu", "Failed to bind holder value via reflection", e);
        }
    }

    private static void setRegistryFrozen(MappedRegistry<?> registry, boolean frozen) {
        try {
            Field frozenField = MappedRegistry.class.getDeclaredField("frozen");
            frozenField.setAccessible(true);
            frozenField.set(registry, frozen);
        } catch (Exception e) {
            Log.error("CreativeMenu", "Failed to toggle registry lock", e);
        }
    }
}
