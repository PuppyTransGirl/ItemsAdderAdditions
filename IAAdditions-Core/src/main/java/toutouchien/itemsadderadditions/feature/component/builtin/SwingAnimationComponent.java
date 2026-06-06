package toutouchien.itemsadderadditions.feature.component.builtin;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.SwingAnimation;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.common.logging.Log;
import toutouchien.itemsadderadditions.common.version.VersionUtils;
import toutouchien.itemsadderadditions.feature.component.ComponentExecutor;
import toutouchien.itemsadderadditions.feature.component.annotation.Component;

import java.util.Locale;

/**
 * <p><strong>Minimum Minecraft Version:</strong> 1.21.11
 *
 * <table>
 * <tr><th>Property</th><th>Type</th><th>Values</th></tr>
 * <tr><td>type</td><td>String</td><td>NONE, WHACK, STAB</td></tr>
 * <tr><td>duration</td><td>Integer</td><td>&gt;= 1</td></tr>
 * </table>
 */
@SuppressWarnings({"UnstableApiUsage", "unused"})
@NullMarked
@Component(key = "swing_animation")
public final class SwingAnimationComponent extends ComponentExecutor {
    private SwingAnimation.@Nullable Animation type;
    private int duration = 6;

    @Override
    public @Nullable VersionUtils minimumVersion() {
        return VersionUtils.v1_21_11;
    }

    @Override
    public boolean configure(@Nullable Object configData, String namespacedID) {
        if (!(configData instanceof ConfigurationSection section)) {
            Log.itemWarn("Components", namespacedID, "'swing_animation' must be a configuration section");
            return false;
        }

        String rawType = section.getString("type", "WHACK");
        try {
            this.type = SwingAnimation.Animation.valueOf(rawType.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            Log.itemWarn("Components", namespacedID,
                    "'swing_animation.type' value '{}' is not valid. Use NONE, WHACK, or STAB.", rawType);
            return false;
        }

        int d = section.getInt("duration", 6);
        if (d < 1) {
            Log.itemWarn("Components", namespacedID, "'swing_animation.duration' must be >= 1");
            return false;
        }

        this.duration = d;
        return true;
    }

    @Override
    public ItemStack apply(ItemStack itemStack, String namespacedID) {
        SwingAnimation.Animation animation = type != null ? type : SwingAnimation.Animation.WHACK;
        itemStack.setData(DataComponentTypes.SWING_ANIMATION,
                SwingAnimation.swingAnimation().type(animation).duration(duration).build());
        return itemStack;
    }
}
