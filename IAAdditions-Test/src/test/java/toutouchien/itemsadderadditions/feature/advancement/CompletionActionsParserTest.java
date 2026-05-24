package toutouchien.itemsadderadditions.feature.advancement;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

class CompletionActionsParserTest {
    private static YamlConfiguration yamlOf(String yaml) {
        YamlConfiguration cfg = new YamlConfiguration();
        try {
            cfg.loadFromString(yaml);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return cfg;
    }

    @Test
    void parse_null_returnsEmpty() {
        assertSame(CompletionActions.EMPTY, CompletionActionsParser.parse(null));
    }

    @Test
    void parse_emptySection_returnsEmpty() {
        assertSame(CompletionActions.EMPTY, CompletionActionsParser.parse(new YamlConfiguration()));
    }

    @Test
    void parse_validSound_returnsNonEmpty() {
        var cfg = yamlOf("sound:\n  name: minecraft:entity.player.levelup\n");
        assertFalse(CompletionActionsParser.parse(cfg).isEmpty());
    }

    @Test
    void parse_soundInvalidSource_returnsEmpty() {
        var cfg = yamlOf("sound:\n  name: minecraft:entity.player.levelup\n  source: not_valid\n");
        assertSame(CompletionActions.EMPTY, CompletionActionsParser.parse(cfg));
    }

    @Test
    void parse_validCommand_returnsNonEmpty() {
        var cfg = yamlOf("commands:\n  r:\n    command: 'give {player} diamond 1'\n");
        assertFalse(CompletionActionsParser.parse(cfg).isEmpty());
    }

    @Test
    void parse_blankCommand_returnsEmpty() {
        var cfg = yamlOf("commands:\n  r:\n    command: '   '\n");
        assertSame(CompletionActions.EMPTY, CompletionActionsParser.parse(cfg));
    }

    @Test
    void parse_validTitle_returnsNonEmpty() {
        var cfg = yamlOf("title:\n  title: '<gold>Done!'\n");
        assertFalse(CompletionActionsParser.parse(cfg).isEmpty());
    }

    @Test
    void parse_titleMissingTitle_returnsEmpty() {
        var cfg = yamlOf("title:\n  subtitle: '<gray>only sub'\n");
        assertSame(CompletionActions.EMPTY, CompletionActionsParser.parse(cfg));
    }

    @Test
    void parse_validActionBar_returnsNonEmpty() {
        var cfg = yamlOf("actionbar:\n  text: '<green>done!'\n");
        assertFalse(CompletionActionsParser.parse(cfg).isEmpty());
    }

    @Test
    void parse_actionBarMissingText_returnsEmpty() {
        var cfg = yamlOf("actionbar:\n  foo: bar\n");
        assertSame(CompletionActions.EMPTY, CompletionActionsParser.parse(cfg));
    }

    @Test
    void parse_allFour_returnsNonEmpty() {
        var cfg = yamlOf("""
                sound:
                  name: minecraft:entity.player.levelup
                commands:
                  r:
                    command: 'give {player} diamond 1'
                title:
                  title: '<gold>Done!'
                actionbar:
                  text: '<green>complete'
                """);
        assertFalse(CompletionActionsParser.parse(cfg).isEmpty());
    }
}
