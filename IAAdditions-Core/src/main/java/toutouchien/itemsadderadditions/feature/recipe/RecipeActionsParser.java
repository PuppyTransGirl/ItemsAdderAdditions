package toutouchien.itemsadderadditions.feature.recipe;

import net.kyori.adventure.sound.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.common.utils.SoundUtils;

import java.util.ArrayList;
import java.util.List;

@NullMarked
public final class RecipeActionsParser {
    private RecipeActionsParser() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Parses the {@code on_complete} section of a recipe entry.
     *
     * @param section the {@code on_complete} config section, or {@code null}
     * @return parsed actions, or {@link RecipeActions#EMPTY} when nothing is configured
     */
    public static RecipeActions parse(@Nullable ConfigurationSection section) {
        if (section == null) return RecipeActions.EMPTY;

        Sound sound = SoundUtils.parseSound(section.getConfigurationSection("sound"));
        List<RecipeCommand> commands = parseCommands(section.getConfigurationSection("commands"));

        if (sound == null && commands.isEmpty()) return RecipeActions.EMPTY;
        return new RecipeActions(sound, commands);
    }

    private static List<RecipeCommand> parseCommands(@Nullable ConfigurationSection section) {
        if (section == null) return List.of();

        List<RecipeCommand> result = new ArrayList<>();
        for (String key : section.getKeys(false)) {
            ConfigurationSection entry = section.getConfigurationSection(key);
            if (entry == null) continue;
            String command = entry.getString("command");
            if (command == null || command.isBlank()) continue;
            boolean asConsole = entry.getBoolean("as_console", false);
            result.add(new RecipeCommand(command, asConsole));
        }
        return result;
    }
}
