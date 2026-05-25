package toutouchien.itemsadderadditions.settings;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UpdateCheckerSettingsTest {
    @Test
    void defaultsAreEnabledAndNotifyOnJoin() {
        assertTrue(UpdateCheckerSettings.DEFAULT_ENABLED);
        assertTrue(UpdateCheckerSettings.DEFAULT_NOTIFY_ON_JOIN);
    }

    @Test
    void recordStoresFlags() {
        UpdateCheckerSettings settings = new UpdateCheckerSettings(false, true);

        assertFalse(settings.enabled());
        assertTrue(settings.notifyOnJoin());
    }

    @Test
    void equalSettingsHaveSameHashCode() {
        UpdateCheckerSettings first = new UpdateCheckerSettings(true, false);
        UpdateCheckerSettings second = new UpdateCheckerSettings(true, false);

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertTrue(first.toString().contains("enabled=true"));
    }
}
