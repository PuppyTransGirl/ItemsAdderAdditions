package toutouchien.itemsadderadditions.feature.advancement;

import net.kyori.adventure.sound.Sound;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.common.utils.SoundUtils;
import toutouchien.itemsadderadditions.common.utils.TextRenderer;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@NullMarked
public final class CompletionActionsParser {
    private CompletionActionsParser() {
        throw new IllegalStateException("Utility class");
    }

    public static CompletionActions parse(@Nullable ConfigurationSection section) {
        if (section == null) return CompletionActions.EMPTY;

        List<CompletionAction> actions = new ArrayList<>();

        Sound sound = SoundUtils.parseSound(section.getConfigurationSection("sound"));
        if (sound != null) {
            actions.add(player -> player.playSound(sound));
        }

        actions.addAll(parseCommands(section.getConfigurationSection("commands")));

        CompletionAction title = parseTitle(section.getConfigurationSection("title"));
        if (title != null) actions.add(title);

        CompletionAction actionBar = parseActionBar(section.getConfigurationSection("actionbar"));
        if (actionBar != null) actions.add(actionBar);

        if (actions.isEmpty()) return CompletionActions.EMPTY;
        return new CompletionActions(actions);
    }

    private static List<CompletionAction> parseCommands(@Nullable ConfigurationSection section) {
        if (section == null) return List.of();
        List<CompletionAction> result = new ArrayList<>();
        for (String key : section.getKeys(false)) {
            ConfigurationSection entry = section.getConfigurationSection(key);
            if (entry == null) continue;
            String command = entry.getString("command");
            if (command == null || command.isBlank()) continue;
            boolean asConsole = entry.getBoolean("as_console", false);
            result.add(player -> {
                String resolved = command.replace("{player}", player.getName());
                if (asConsole) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), resolved);
                } else {
                    player.performCommand(resolved);
                }
            });
        }
        return result;
    }

    @Nullable
    private static CompletionAction parseTitle(@Nullable ConfigurationSection section) {
        if (section == null) return null;
        String title = section.getString("title");
        if (title == null || title.isBlank()) return null;
        String subtitle = section.getString("subtitle", "");
        int fadeIn = section.getInt("fade_in", 10);
        int stay = section.getInt("stay", 70);
        int fadeOut = section.getInt("fade_out", 20);
        return player -> player.showTitle(
                net.kyori.adventure.title.Title.title(
                        TextRenderer.render(player, title),
                        TextRenderer.render(player, subtitle),
                        net.kyori.adventure.title.Title.Times.times(
                                Duration.ofMillis(fadeIn * 50L),
                                Duration.ofMillis(stay * 50L),
                                Duration.ofMillis(fadeOut * 50L)
                        )
                )
        );
    }

    @Nullable
    private static CompletionAction parseActionBar(@Nullable ConfigurationSection section) {
        if (section == null) return null;
        String text = section.getString("text");
        if (text == null || text.isBlank()) return null;
        return player -> player.sendActionBar(TextRenderer.render(player, text));
    }
}
