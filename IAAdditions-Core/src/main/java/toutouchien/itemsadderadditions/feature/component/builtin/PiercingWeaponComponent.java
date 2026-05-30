package toutouchien.itemsadderadditions.feature.component.builtin;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.PiercingWeapon;
import net.kyori.adventure.key.Key;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.common.logging.Log;
import toutouchien.itemsadderadditions.common.version.VersionUtils;
import toutouchien.itemsadderadditions.feature.component.ComponentExecutor;
import toutouchien.itemsadderadditions.feature.component.annotation.Component;

/**
 * <p><strong>Minimum Minecraft Version:</strong> 1.21.5
 *
 * <table>
 * <tr><th>Property</th><th>Type</th><th>Default</th></tr>
 * <tr><td>deals_knockback</td><td>Boolean</td><td>true</td></tr>
 * <tr><td>dismounts</td><td>Boolean</td><td>true</td></tr>
 * <tr><td>sound</td><td>String (namespaced key)</td><td>null</td></tr>
 * <tr><td>hit_sound</td><td>String (namespaced key)</td><td>null</td></tr>
 * </table>
 */
@SuppressWarnings({"UnstableApiUsage", "unused"})
@NullMarked
@Component(key = "piercing_weapon")
public final class PiercingWeaponComponent extends ComponentExecutor {
    private boolean dealsKnockback = true;
    private boolean dismounts = true;
    private @Nullable Key sound;
    private @Nullable Key hitSound;

    @Override
    public @Nullable VersionUtils minimumVersion() {
        return VersionUtils.v1_21_5;
    }

    @Override
    public boolean configure(@Nullable Object configData, String namespacedID) {
        if (!(configData instanceof ConfigurationSection section)) {
            Log.itemWarn("Components", namespacedID, "'piercing_weapon' must be a configuration section");
            return false;
        }

        this.dealsKnockback = section.getBoolean("deals_knockback", true);
        this.dismounts = section.getBoolean("dismounts", true);

        String rawSound = section.getString("sound");
        if (rawSound != null) {
            this.sound = parseKey(rawSound, "piercing_weapon.sound", namespacedID);
            if (this.sound == null) return false;
        }

        String rawHitSound = section.getString("hit_sound");
        if (rawHitSound != null) {
            this.hitSound = parseKey(rawHitSound, "piercing_weapon.hit_sound", namespacedID);
            if (this.hitSound == null) return false;
        }

        return true;
    }

    @Override
    public ItemStack apply(ItemStack itemStack, String namespacedID) {
        PiercingWeapon.Builder builder = PiercingWeapon.piercingWeapon()
                .dealsKnockback(dealsKnockback)
                .dismounts(dismounts)
                .sound(sound)
                .hitSound(hitSound);
        itemStack.setData(DataComponentTypes.PIERCING_WEAPON, builder.build());
        return itemStack;
    }

    private @Nullable Key parseKey(String raw, String field, String namespacedID) {
        String key = raw.contains(":") ? raw : "minecraft:" + raw;
        try {
            return Key.key(key);
        } catch (Exception e) {
            Log.itemWarn("Components", namespacedID, "'{}' value '{}' is not a valid namespaced key.", field, raw);
            return null;
        }
    }
}
