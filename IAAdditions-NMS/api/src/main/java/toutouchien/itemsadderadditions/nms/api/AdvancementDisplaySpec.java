package toutouchien.itemsadderadditions.nms.api;

import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public record AdvancementDisplaySpec(
        ItemStack icon,
        String title,
        String description,
        String frame,
        @Nullable String background,
        boolean showToast,
        boolean announceToChat,
        boolean hidden
) {}
