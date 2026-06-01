package toutouchien.itemsadderadditions.nms;

import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.nms.api.component.ComponentValue;
import toutouchien.itemsadderadditions.nms.api.component.GenericComponentResult;
import toutouchien.itemsadderadditions.nms.api.component.INmsItemComponentHandler;

@NullMarked
final class NmsItemComponentHandler_v1_21_7 implements INmsItemComponentHandler {
    @Override
    public boolean isSupported() {
        return true;
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public GenericComponentResult apply(ItemStack bukkitStack, String normalizedKey, ComponentValue value, String itemId) {
        int sep = normalizedKey.indexOf(':');
        if (sep < 0 || sep == normalizedKey.length() - 1) {
            return GenericComponentResult.failure("malformed component key: " + normalizedKey);
        }

        ResourceLocation location = ResourceLocation.fromNamespaceAndPath(
                normalizedKey.substring(0, sep),
                normalizedKey.substring(sep + 1)
        );

        Holder.Reference<DataComponentType<?>> reference = BuiltInRegistries.DATA_COMPONENT_TYPE.get(location).orElse(null);
        if (reference == null) {
            return GenericComponentResult.failure("unknown component type: " + normalizedKey);
        }

        DataComponentType<?> type = reference.value();
        Codec codec = type.codec();
        if (codec == null) {
            return GenericComponentResult.failure("component '" + normalizedKey + "' is transient (not persistable)");
        }

        Tag nbtTag = toTag(value);
        RegistryOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, MinecraftServer.getServer().registryAccess());

        Object parsed;
        try {
            parsed = codec.parse(ops, nbtTag).getOrThrow();
        } catch (Exception e) {
            return GenericComponentResult.failure("codec parse failed for '" + normalizedKey + "': " + e.getMessage());
        }

        net.minecraft.world.item.ItemStack nmsStack = CraftItemStack.asNMSCopy(bukkitStack);
        nmsStack.set((DataComponentType) type, parsed);
        return GenericComponentResult.success(CraftItemStack.asBukkitCopy(nmsStack));
    }

    private static Tag toTag(ComponentValue value) {
        return switch (value) {
            case ComponentValue.ObjectNode(var entries) -> {
                CompoundTag tag = new CompoundTag();
                entries.forEach((k, v) -> tag.put(k, toTag(v)));
                yield tag;
            }

            case ComponentValue.ListNode(var values) -> {
                ListTag list = new ListTag();
                for (ComponentValue elem : values) {
                    list.add(toTag(elem));
                }
                yield list;
            }

            case ComponentValue.StringNode(var s) -> StringTag.valueOf(s);
            case ComponentValue.BooleanNode(var b) -> ByteTag.valueOf(b);
            case ComponentValue.IntNode(var i) -> IntTag.valueOf(i);
            case ComponentValue.LongNode(var l) -> LongTag.valueOf(l);
            case ComponentValue.DoubleNode(var d) -> DoubleTag.valueOf(d);
            case ComponentValue.NullNode() -> new CompoundTag();
        };
    }
}
