package toutouchien.itemsadderadditions.integration.hook;

import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.skills.variables.VariableRegistry;
import io.lumine.mythic.core.skills.variables.types.StringVariable;
import io.lumine.mythic.core.utils.MythicUtil;
import net.kyori.adventure.util.TriState;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@NullMarked
public final class MythicMobsUtils {
    private static TriState mythicMobsLoaded = TriState.NOT_SET;

    private MythicMobsUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static void castSkill(Player player, String skill, float power) {
        castSkill(player, skill, power, Map.of());
    }

    public static void castSkill(Player player, String skill, float power, Map<String, ?> variables) {
        if (mythicMobsLoaded == TriState.NOT_SET) {
            mythicMobsLoaded = TriState.byBoolean(
                    Bukkit.getPluginManager().isPluginEnabled("MythicMobs")
            );
        }

        if (mythicMobsLoaded == TriState.FALSE) return;

        Location casterLoc = player.getLocation();
        LivingEntity target = MythicUtil.getTargetedEntity(player);

        List<Entity> targets = target == null
                ? null
                : Collections.singletonList(target);
        List<Location> targetLocations = target == null
                ? null
                : Collections.singletonList(target.getLocation());

        MythicBukkit.inst().getAPIHelper().castSkill(
                player,
                skill,
                player,
                casterLoc,
                targets,
                targetLocations,
                power,
                metadata -> injectVariables(metadata.getVariables(), variables)
        );
    }

    private static void injectVariables(VariableRegistry registry, Map<String, ?> variables) {
        if (variables.isEmpty()) return;

        for (Map.Entry<String, ?> entry : variables.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (key.isEmpty() || value == null) continue;

            registry.put(key, new StringVariable(String.valueOf(value)));
        }
    }
}
