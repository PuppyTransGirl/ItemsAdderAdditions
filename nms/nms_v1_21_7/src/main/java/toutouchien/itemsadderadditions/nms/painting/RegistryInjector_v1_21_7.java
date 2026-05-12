package toutouchien.itemsadderadditions.nms.painting;

import io.papermc.paper.adventure.PaperAdventure;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import net.kyori.adventure.text.minimessage.MiniMessage;
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
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.common.logging.Log;
import toutouchien.itemsadderadditions.nms.api.painting.NmsPaintingVariant;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@NullMarked
public final class RegistryInjector_v1_21_7 {
    private static final String TAG = "CustomPaintings";
    private static final Field FROZEN_FIELD;
    private static final Field BY_VALUE_FIELD;
    private static final Field TO_ID_FIELD;
    private static final Method BIND_VALUE_METHOD;
    private static final ConcurrentMap<String, String> INJECTED_SIGNATURES = new ConcurrentHashMap<>();

    static {
        try {
            FROZEN_FIELD = MappedRegistry.class.getDeclaredField("frozen");
            FROZEN_FIELD.setAccessible(true);

            BY_VALUE_FIELD = MappedRegistry.class.getDeclaredField("byValue");
            BY_VALUE_FIELD.setAccessible(true);

            TO_ID_FIELD = MappedRegistry.class.getDeclaredField("toId");
            TO_ID_FIELD.setAccessible(true);

            BIND_VALUE_METHOD = Holder.Reference.class.getDeclaredMethod("bindValue", Object.class);
            BIND_VALUE_METHOD.setAccessible(true);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private RegistryInjector_v1_21_7() {
        throw new IllegalStateException("Static class");
    }

    public static void injectPaintingVariants(Collection<NmsPaintingVariant> variants) {
        Registry<PaintingVariant> registry = ((CraftServer) Bukkit.getServer())
                .getServer()
                .registryAccess()
                .lookupOrThrow(Registries.PAINTING_VARIANT);

        if (!(registry instanceof MappedRegistry<PaintingVariant> mappedRegistry))
            return;

        int injected = 0;
        int updated = 0;
        int unchanged = 0;

        setRegistryFrozen(mappedRegistry, false);
        try {
            for (NmsPaintingVariant definition : variants) {
                String signature = signature(definition);
                String previousSignature = INJECTED_SIGNATURES.get(definition.variantId());

                if (signature.equals(previousSignature)) {
                    unchanged++;
                    continue;
                }

                ResourceLocation key = resourceLocation(definition.variantId());
                PaintingVariant variant = createVariant(definition);

                ResourceKey<PaintingVariant> resourceKey = ResourceKey.create(Registries.PAINTING_VARIANT, key);
                if (mappedRegistry.containsKey(key)) {
                    if (previousSignature == null && holderIsBound(mappedRegistry, resourceKey)) {
                        INJECTED_SIGNATURES.put(definition.variantId(), signature);
                        unchanged++;
                        continue;
                    }

                    if (replaceVariant(mappedRegistry, resourceKey, variant)) {
                        INJECTED_SIGNATURES.put(definition.variantId(), signature);
                        updated++;
                    }
                    continue;
                }

                Registry.register(mappedRegistry, key, variant);
                if (replaceVariant(mappedRegistry, resourceKey, variant)) {
                    INJECTED_SIGNATURES.put(definition.variantId(), signature);
                    injected++;
                }
            }
        } finally {
            setRegistryFrozen(mappedRegistry, true);
        }

        if (updated > 0) {
            Log.info(TAG, "Loaded {} new PaintingVariant(s) injected, {} updated, {} unchanged.", injected, updated, unchanged);
            return;
        }

        if (injected == 0) {
            Log.loaded(TAG, 0, "PaintingVariant(s) injected (all already present)");
            return;
        }

        Log.loaded(TAG, injected, "new PaintingVariant(s) injected");
    }

    private static PaintingVariant createVariant(NmsPaintingVariant definition) {
        return new PaintingVariant(
                definition.width(),
                definition.height(),
                resourceLocation(definition.assetId()),
                optionalComponent(definition.title()),
                optionalComponent(definition.author())
        );
    }

    private static boolean holderIsBound(MappedRegistry<PaintingVariant> registry, ResourceKey<PaintingVariant> key) {
        Optional<Holder.Reference<PaintingVariant>> holderOptional = registry.get(key);
        if (holderOptional.isEmpty()) return false;

        try {
            holderOptional.get().value();
            return true;
        } catch (IllegalStateException ignored) {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private static boolean replaceVariant(
            MappedRegistry<PaintingVariant> registry,
            ResourceKey<PaintingVariant> key,
            PaintingVariant newVariant
    ) {
        Optional<Holder.Reference<PaintingVariant>> holderOptional = registry.get(key);
        if (holderOptional.isEmpty()) return false;

        try {
            Holder.Reference<PaintingVariant> holder = holderOptional.get();
            PaintingVariant oldVariant = null;
            int id = -1;

            try {
                oldVariant = holder.value();
                id = registry.getId(oldVariant);
            } catch (IllegalStateException ignored) {
                // The holder exists but was never bound to a value. This is exactly
                // the state that crashes registry synchronization during login.
            }

            BIND_VALUE_METHOD.invoke(holder, newVariant);

            Map<PaintingVariant, Holder.Reference<PaintingVariant>> byValue =
                    (Map<PaintingVariant, Holder.Reference<PaintingVariant>>) BY_VALUE_FIELD.get(registry);
            if (oldVariant != null) {
                byValue.remove(oldVariant);
            }
            byValue.put(newVariant, holder);

            Reference2IntMap<PaintingVariant> toId = (Reference2IntMap<PaintingVariant>) TO_ID_FIELD.get(registry);
            if (id >= 0 && oldVariant != null) {
                toId.removeInt(oldVariant);
                toId.put(newVariant, id);
            }

            return true;
        } catch (ReflectiveOperationException | RuntimeException e) {
            Log.error(TAG, "Failed to bind custom painting variant '" + key.location() + "'", e);
            return false;
        }
    }

    private static String signature(NmsPaintingVariant definition) {
        return definition.variantId()
                + '|'
                + definition.width()
                + '|'
                + definition.height()
                + '|'
                + definition.assetId()
                + '|'
                + nullToEmpty(definition.title())
                + '|'
                + nullToEmpty(definition.author());
    }

    private static String nullToEmpty(@Nullable String value) {
        return value == null ? "" : value;
    }

    private static Optional<Component> optionalComponent(@Nullable String value) {
        if (value == null || value.isBlank()) return Optional.empty();
        try {
            return Optional.of(PaperAdventure.asVanilla(MiniMessage.miniMessage().deserialize(value)));
        } catch (Exception e) {
            return Optional.of(Component.literal(value));
        }
    }

    private static ResourceLocation resourceLocation(String id) {
        int sep = id.indexOf(':');
        return ResourceLocation.fromNamespaceAndPath(
                id.substring(0, sep),
                id.substring(sep + 1)
        );
    }

    private static void setRegistryFrozen(MappedRegistry<?> registry, boolean frozen) {
        try {
            FROZEN_FIELD.set(registry, frozen);
        } catch (Exception e) {
            Log.error(TAG, "Failed to toggle registry lock", e);
        }
    }
}
