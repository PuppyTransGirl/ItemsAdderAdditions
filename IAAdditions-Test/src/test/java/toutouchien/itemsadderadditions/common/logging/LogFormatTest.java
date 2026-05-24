package toutouchien.itemsadderadditions.common.logging;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LogFormatTest {
    @Test
    void noPlaceholdersReturnedAsIs() {
        assertEquals("hello world", Log.format("hello world"));
    }

    @Test
    void singlePlaceholderReplaced() {
        assertEquals("hello foo", Log.format("hello {}", "foo"));
    }

    @Test
    void multiplePlaceholdersReplaced() {
        assertEquals("a 1 b 2", Log.format("a {} b {}", 1, 2));
    }

    @Test
    void nullArgumentRenderedAsNullString() {
        assertEquals("value is null", Log.format("value is {}", (Object) null));
    }

    @Test
    void extraArgsIgnored() {
        assertEquals("only one", Log.format("only {}", "one", "two", "three"));
    }

    @Test
    void fewerArgsThanPlaceholdersLeavesRemainingLiteral() {
        assertEquals("a b {}", Log.format("a {} {}", "b"));
    }

    @Test
    void noArgsArray() {
        assertEquals("no args", Log.format("no args"));
    }

    @Test
    void numericArgumentFormatted() {
        assertEquals("count: 42", Log.format("count: {}", 42));
    }

    @Test
    void adjacentPlaceholders() {
        assertEquals("ab", Log.format("{}{}", "a", "b"));
    }
}
