package toutouchien.itemsadderadditions.feature.action.builtin;

import org.bukkit.configuration.ConfigurationSection;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.common.annotation.Parameter;
import toutouchien.itemsadderadditions.feature.action.ActionContext;
import toutouchien.itemsadderadditions.feature.action.ActionExecutor;
import toutouchien.itemsadderadditions.feature.action.annotation.Action;
import toutouchien.itemsadderadditions.integration.hook.MythicMobsUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Cast a Mythic Mobs skill.
 * <p>
 * Example:
 * <pre>{@code
 * mythic_mobs_skill:
 *   skill: "duplicate"
 *   power: 0.5 # Optional (default: 1.0)
 *   variables:
 *     damage: 5
 *     radius: 3
 * }</pre>
 */
@SuppressWarnings("unused")
@NullMarked
@Action(key = "mythic_mobs_skill")
public final class MythicMobsSkillAction extends ActionExecutor {
    @Parameter(key = "skill", type = String.class, required = true)
    private String skill;

    @Parameter(key = "power", type = Float.class)
    private float power = 1F;

    private Map<String, Object> variables = Map.of();

    @Override
    public boolean configure(@Nullable Object configData, String namespacedID) {
        if (!super.configure(configData, namespacedID)) return false;

        ConfigurationSection section = configData instanceof ConfigurationSection cs ? cs : null;
        if (section == null) {
            variables = Map.of();
            return true;
        }

        ConfigurationSection variablesSection = section.getConfigurationSection("variables");
        if (variablesSection == null) {
            variables = Map.of();
            return true;
        }

        variables = new LinkedHashMap<>(variablesSection.getValues(false));
        return true;
    }

    @Override
    protected void execute(ActionContext context) {
        MythicMobsUtils.castSkill(context.player(), skill, power, variables);
    }
}
