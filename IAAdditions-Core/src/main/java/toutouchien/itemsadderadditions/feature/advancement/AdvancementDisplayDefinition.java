package toutouchien.itemsadderadditions.feature.advancement;

import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public record AdvancementDisplayDefinition(
        ItemStack icon,
        String title,
        String description,
        String frame,
        @Nullable String background,
        boolean showToast,
        boolean announceToChat,
        boolean hidden
) {}
