package toutouchien.itemsadderadditions.utils.hook;

import io.lumine.mythic.api.MythicProvider;
import io.lumine.mythic.api.adapters.AbstractPlayer;
import io.lumine.mythic.api.skills.SkillCaster;
import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.core.skills.SkillMetadataImpl;
import io.lumine.mythic.core.skills.SkillTriggers;
import net.kyori.adventure.util.TriState;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class MythicMobsUtils {
    private static TriState mythicMobsLoaded = TriState.NOT_SET;

    private MythicMobsUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static void castSkill(Player player, String skill, float power) {
        if (mythicMobsLoaded == TriState.NOT_SET) {
            mythicMobsLoaded = TriState.byBoolean(
                    Bukkit.getPluginManager().isPluginEnabled("MythicMobs")
            );
        }

        if (mythicMobsLoaded == TriState.FALSE)
            return;

        try {
            var skillManager = MythicProvider.get().getSkillManager();
            AbstractPlayer abstractPlayer = BukkitAdapter.adapt(player);
            SkillCaster caster = skillManager.getCaster(abstractPlayer);

            var skillOpt = skillManager.getSkill(skill);
            if (caster != null && skillOpt.isPresent()) {
                var mythicSkill = skillOpt.get();
                var meta = new SkillMetadataImpl(SkillTriggers.API, caster, abstractPlayer, power);
                mythicSkill.execute(meta);
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning("[MythicMobsUtils] Failed to cast skill " + skill + ": " + e.getMessage());
        }
    }
}
