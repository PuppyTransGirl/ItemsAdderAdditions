package toutouchien.itemsadderadditions.feature.component.builtin;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.KineticWeapon;
import io.papermc.paper.datacomponent.item.KineticWeapon.Condition;
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
 * <tr><td>contact_cooldown_ticks</td><td>Integer</td><td>0</td></tr>
 * <tr><td>delay_ticks</td><td>Integer</td><td>0</td></tr>
 * <tr><td>forward_movement</td><td>Float</td><td>1.0</td></tr>
 * <tr><td>damage_multiplier</td><td>Float</td><td>1.0</td></tr>
 * <tr><td>sound</td><td>String (namespaced key)</td><td>null</td></tr>
 * <tr><td>hit_sound</td><td>String (namespaced key)</td><td>null</td></tr>
 * <tr><td>dismount_conditions</td><td>Section (max_duration_ticks, min_speed, min_relative_speed)</td><td>null</td></tr>
 * <tr><td>knockback_conditions</td><td>Section (max_duration_ticks, min_speed, min_relative_speed)</td><td>null</td></tr>
 * <tr><td>damage_conditions</td><td>Section (max_duration_ticks, min_speed, min_relative_speed)</td><td>null</td></tr>
 * </table>
 */
@SuppressWarnings({"UnstableApiUsage", "unused"})
@NullMarked
@Component(key = "kinetic_weapon")
public final class KineticWeaponComponent extends ComponentExecutor {
    private int contactCooldownTicks;
    private int delayTicks;
    private float forwardMovement = 1f;
    private float damageMultiplier = 1f;
    private @Nullable Key sound;
    private @Nullable Key hitSound;
    private @Nullable Condition dismountConditions;
    private @Nullable Condition knockbackConditions;
    private @Nullable Condition damageConditions;

    @Override
    public @Nullable VersionUtils minimumVersion() {
        return VersionUtils.v1_21_5;
    }

    @Override
    public boolean configure(@Nullable Object configData, String namespacedID) {
        if (!(configData instanceof ConfigurationSection section)) {
            Log.itemWarn("Components", namespacedID, "'kinetic_weapon' must be a configuration section");
            return false;
        }

        this.contactCooldownTicks = section.getInt("contact_cooldown_ticks", 0);
        this.delayTicks = section.getInt("delay_ticks", 0);
        this.forwardMovement = readFloat(section, "forward_movement", 1f);
        this.damageMultiplier = readFloat(section, "damage_multiplier", 1f);

        String rawSound = section.getString("sound");
        if (rawSound != null) {
            this.sound = parseKey(rawSound, "kinetic_weapon.sound", namespacedID);
            if (this.sound == null) return false;
        }

        String rawHitSound = section.getString("hit_sound");
        if (rawHitSound != null) {
            this.hitSound = parseKey(rawHitSound, "kinetic_weapon.hit_sound", namespacedID);
            if (this.hitSound == null) return false;
        }

        this.dismountConditions = parseCondition(section.getConfigurationSection("dismount_conditions"),
                "dismount_conditions", namespacedID);
        this.knockbackConditions = parseCondition(section.getConfigurationSection("knockback_conditions"),
                "knockback_conditions", namespacedID);
        this.damageConditions = parseCondition(section.getConfigurationSection("damage_conditions"),
                "damage_conditions", namespacedID);

        return true;
    }

    @Override
    public ItemStack apply(ItemStack itemStack, String namespacedID) {
        KineticWeapon weapon = KineticWeapon.kineticWeapon()
                .contactCooldownTicks(contactCooldownTicks)
                .delayTicks(delayTicks)
                .forwardMovement(forwardMovement)
                .damageMultiplier(damageMultiplier)
                .sound(sound)
                .hitSound(hitSound)
                .dismountConditions(dismountConditions)
                .knockbackConditions(knockbackConditions)
                .damageConditions(damageConditions)
                .build();
        itemStack.setData(DataComponentTypes.KINETIC_WEAPON, weapon);
        return itemStack;
    }

    private @Nullable Condition parseCondition(@Nullable ConfigurationSection section, String field, String namespacedID) {
        if (section == null) return null;
        int maxDuration = section.getInt("max_duration_ticks", 0);
        float minSpeed = readFloat(section, "min_speed", 0f);
        float minRelativeSpeed = readFloat(section, "min_relative_speed", 0f);
        return KineticWeapon.condition(maxDuration, minSpeed, minRelativeSpeed);
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

    private static float readFloat(ConfigurationSection section, String key, float def) {
        Object raw = section.get(key);
        if (raw instanceof Number n) return n.floatValue();
        return def;
    }
}
