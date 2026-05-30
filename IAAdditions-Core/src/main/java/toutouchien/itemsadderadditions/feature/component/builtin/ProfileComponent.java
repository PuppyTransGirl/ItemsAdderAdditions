package toutouchien.itemsadderadditions.feature.component.builtin;

import com.destroystokyo.paper.profile.ProfileProperty;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.common.logging.Log;
import toutouchien.itemsadderadditions.feature.component.ComponentExecutor;
import toutouchien.itemsadderadditions.feature.component.annotation.Component;

import java.util.UUID;

/**
 * <table>
 * <tr><th>Property</th><th>Type</th><th>Format</th></tr>
 * <tr><td>(scalar)</td><td>String</td><td>Player name (resolved by client)</td></tr>
 * <tr><td>(section) name</td><td>String</td><td>Player name</td></tr>
 * <tr><td>(section) uuid</td><td>String</td><td>Player UUID</td></tr>
 * <tr><td>(section) texture</td><td>String</td><td>Base64-encoded texture value</td></tr>
 * </table>
 */
@SuppressWarnings({"UnstableApiUsage", "unused"})
@NullMarked
@Component(key = "profile")
public final class ProfileComponent extends ComponentExecutor {
    private @Nullable ResolvableProfile profile;

    @Override
    public boolean configure(@Nullable Object configData, String namespacedID) {
        if (configData instanceof String name) {
            this.profile = ResolvableProfile.resolvableProfile().name(name.trim()).build();
            return true;
        }

        if (!(configData instanceof ConfigurationSection section)) {
            Log.itemWarn("Components", namespacedID, "'profile' must be a player name string or section with name/uuid/texture");
            return false;
        }

        ResolvableProfile.Builder builder = ResolvableProfile.resolvableProfile();

        String name = section.getString("name");
        if (name != null) builder.name(name.trim());

        String rawUuid = section.getString("uuid");
        if (rawUuid != null) {
            try {
                builder.uuid(UUID.fromString(rawUuid.trim()));
            } catch (IllegalArgumentException e) {
                Log.itemWarn("Components", namespacedID, "'profile.uuid' value '{}' is not a valid UUID.", rawUuid);
                return false;
            }
        }

        String texture = section.getString("texture");
        if (texture != null) {
            builder.addProperty(new ProfileProperty("textures", texture.trim()));
        }

        if (name == null && rawUuid == null && texture == null) {
            Log.itemWarn("Components", namespacedID, "'profile' section must have at least 'name', 'uuid', or 'texture'");
            return false;
        }

        this.profile = builder.build();
        return true;
    }

    @Override
    public ItemStack apply(ItemStack itemStack, String namespacedID) {
        if (profile != null) {
            itemStack.setData(DataComponentTypes.PROFILE, profile);
        }
        return itemStack;
    }
}
