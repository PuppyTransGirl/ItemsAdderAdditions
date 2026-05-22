package toutouchien.itemsadderadditions.feature.recipe;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

class RecipeActionsParserTest {
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
        assertSame(RecipeActions.EMPTY, RecipeActionsParser.parse(null));
    }

    @Test
    void parse_emptySection_returnsEmpty() {
        assertSame(RecipeActions.EMPTY, RecipeActionsParser.parse(new YamlConfiguration()));
    }

    @Test
    void parse_validSound_returnsNonEmpty() {
        YamlConfiguration cfg = yamlOf("""
                sound:
                  name: minecraft:entity.player.levelup
                """);
        assertFalse(RecipeActionsParser.parse(cfg).isEmpty());
    }

    @Test
    void parse_soundWithInvalidSource_returnsEmpty() {
        YamlConfiguration cfg = yamlOf("""
                sound:
                  name: minecraft:entity.player.levelup
                  source: not_a_valid_source
                """);
        assertSame(RecipeActions.EMPTY, RecipeActionsParser.parse(cfg));
    }

    @Test
    void parse_soundMissingName_returnsEmpty() {
        YamlConfiguration cfg = yamlOf("""
                sound:
                  source: master
                """);
        assertSame(RecipeActions.EMPTY, RecipeActionsParser.parse(cfg));
    }

    @Test
    void parse_commandAsConsoleFalseDefault_returnsNonEmpty() {
        YamlConfiguration cfg = yamlOf("""
                commands:
                  notify:
                    command: 'tell {player} done'
                """);
        assertFalse(RecipeActionsParser.parse(cfg).isEmpty());
    }

    @Test
    void parse_commandAsConsoleTrue_returnsNonEmpty() {
        YamlConfiguration cfg = yamlOf("""
                commands:
                  reward:
                    command: 'give {player} diamond 1'
                    as_console: true
                """);
        assertFalse(RecipeActionsParser.parse(cfg).isEmpty());
    }

    @Test
    void parse_commandWithBlankCommand_isSkipped() {
        YamlConfiguration cfg = yamlOf("""
                commands:
                  bad:
                    command: '   '
                """);
        assertSame(RecipeActions.EMPTY, RecipeActionsParser.parse(cfg));
    }

    @Test
    void parse_commandWithMissingCommandField_isSkipped() {
        YamlConfiguration cfg = yamlOf("""
                commands:
                  bad:
                    as_console: true
                """);
        assertSame(RecipeActions.EMPTY, RecipeActionsParser.parse(cfg));
    }

    @Test
    void parse_soundAndCommands_returnsNonEmpty() {
        YamlConfiguration cfg = yamlOf("""
                sound:
                  name: minecraft:entity.player.levelup
                commands:
                  reward:
                    command: 'give {player} diamond 1'
                    as_console: true
                """);
        assertFalse(RecipeActionsParser.parse(cfg).isEmpty());
    }

    @Test
    void parse_multipleCommandEntries_allParsed() {
        YamlConfiguration cfg = yamlOf("""
                commands:
                  cmd1:
                    command: 'give {player} stone 1'
                  cmd2:
                    command: 'say hello'
                """);
        assertFalse(RecipeActionsParser.parse(cfg).isEmpty());
    }
}
