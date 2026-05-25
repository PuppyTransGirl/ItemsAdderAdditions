package toutouchien.itemsadderadditions.feature.behaviour.builtin.textdisplay;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import toutouchien.itemsadderadditions.nms.api.textdisplay.PacketTextDisplayAlignment;
import toutouchien.itemsadderadditions.nms.api.textdisplay.PacketTextDisplayBillboard;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TextDisplayConfigLoaderTest {
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
    void load_missingTextAndDisplays_returnsEmpty() {
        var cfg = yamlOf("other: value");
        assertTrue(TextDisplayConfigLoader.load(cfg, "ns:item").isEmpty());
    }

    @Test
    void load_singleDisplay_textAsString_returnsOneSpec() {
        var cfg = yamlOf("text: 'Hello'");
        var specs = TextDisplayConfigLoader.load(cfg, "ns:item");
        assertEquals(1, specs.size());
        assertEquals(List.of("Hello"), specs.getFirst().textLines());
    }

    @Test
    void load_singleDisplay_textAsList_returnsOneSpec() {
        var cfg = yamlOf("text:\n  - 'Line1'\n  - 'Line2'\n");
        var specs = TextDisplayConfigLoader.load(cfg, "ns:item");
        assertEquals(1, specs.size());
        assertEquals(List.of("Line1", "Line2"), specs.getFirst().textLines());
    }

    @Test
    void load_singleDisplay_blankText_returnsEmpty() {
        var cfg = yamlOf("text: '   '");
        assertTrue(TextDisplayConfigLoader.load(cfg, "ns:item").isEmpty());
    }

    @Test
    void load_multiDisplay_eachSpecLoaded() {
        var cfg = yamlOf("""
                displays:
                  d1:
                    text: 'A'
                  d2:
                    text: 'B'
                """);
        var specs = TextDisplayConfigLoader.load(cfg, "ns:item");
        assertEquals(2, specs.size());
    }

    @Test
    void load_multiDisplay_invalidChildSection_skipped() {
        var cfg = yamlOf("""
                displays:
                  d1: 42
                  d2:
                    text: 'Valid'
                """);
        var specs = TextDisplayConfigLoader.load(cfg, "ns:item");
        assertEquals(1, specs.size());
    }

    @Test
    void load_multiDisplay_missingText_skipped() {
        var cfg = yamlOf("""
                displays:
                  d1:
                    other: value
                """);
        assertTrue(TextDisplayConfigLoader.load(cfg, "ns:item").isEmpty());
    }

    @Test
    void load_textAsUnsupportedType_returnsEmpty() {
        var cfg = yamlOf("text: 42");
        assertTrue(TextDisplayConfigLoader.load(cfg, "ns:item").isEmpty());
    }

    @Test
    void load_billboard_null_defaultsToVertical() {
        var cfg = yamlOf("text: 'T'");
        var spec = TextDisplayConfigLoader.load(cfg, "ns:item").getFirst();
        assertEquals(PacketTextDisplayBillboard.VERTICAL, spec.visual().billboard());
    }

    @Test
    void load_billboard_fixed() {
        var cfg = yamlOf("text: 'T'\nbillboard: fixed");
        var spec = TextDisplayConfigLoader.load(cfg, "ns:item").getFirst();
        assertEquals(PacketTextDisplayBillboard.FIXED, spec.visual().billboard());
    }

    @Test
    void load_billboard_center() {
        var cfg = yamlOf("text: 'T'\nbillboard: CENTER");
        var spec = TextDisplayConfigLoader.load(cfg, "ns:item").getFirst();
        assertEquals(PacketTextDisplayBillboard.CENTER, spec.visual().billboard());
    }

    @Test
    void load_billboard_invalid_defaultsToVertical() {
        var cfg = yamlOf("text: 'T'\nbillboard: invalid_value");
        var spec = TextDisplayConfigLoader.load(cfg, "ns:item").getFirst();
        assertEquals(PacketTextDisplayBillboard.VERTICAL, spec.visual().billboard());
    }

    @Test
    void load_alignment_null_defaultsToCenter() {
        var cfg = yamlOf("text: 'T'");
        var spec = TextDisplayConfigLoader.load(cfg, "ns:item").getFirst();
        assertEquals(PacketTextDisplayAlignment.CENTER, spec.visual().alignment());
    }

    @Test
    void load_alignment_left() {
        var cfg = yamlOf("text: 'T'\nalignment: left");
        var spec = TextDisplayConfigLoader.load(cfg, "ns:item").getFirst();
        assertEquals(PacketTextDisplayAlignment.LEFT, spec.visual().alignment());
    }

    @Test
    void load_alignment_right() {
        var cfg = yamlOf("text: 'T'\nalignment: RIGHT");
        var spec = TextDisplayConfigLoader.load(cfg, "ns:item").getFirst();
        assertEquals(PacketTextDisplayAlignment.RIGHT, spec.visual().alignment());
    }

    @Test
    void load_alignment_invalid_defaultsToCenter() {
        var cfg = yamlOf("text: 'T'\nalignment: nowhere");
        var spec = TextDisplayConfigLoader.load(cfg, "ns:item").getFirst();
        assertEquals(PacketTextDisplayAlignment.CENTER, spec.visual().alignment());
    }

    @Test
    void load_offset_default() {
        var cfg = yamlOf("text: 'T'");
        var spec = TextDisplayConfigLoader.load(cfg, "ns:item").getFirst();
        assertEquals(0.0, spec.offset().getX(), 0.001);
        assertEquals(0.25, spec.offset().getY(), 0.001);
        assertEquals(0.0, spec.offset().getZ(), 0.001);
    }

    @Test
    void load_offset_asString() {
        var cfg = yamlOf("text: 'T'\noffset: '1.0,2.0,3.0'");
        var spec = TextDisplayConfigLoader.load(cfg, "ns:item").getFirst();
        assertEquals(1.0, spec.offset().getX(), 0.001);
        assertEquals(2.0, spec.offset().getY(), 0.001);
        assertEquals(3.0, spec.offset().getZ(), 0.001);
    }

    @Test
    void load_offset_asList() {
        var cfg = yamlOf("text: 'T'\noffset: [0.5, 1.0, 1.5]");
        var spec = TextDisplayConfigLoader.load(cfg, "ns:item").getFirst();
        assertEquals(0.5, spec.offset().getX(), 0.001);
        assertEquals(1.0, spec.offset().getY(), 0.001);
        assertEquals(1.5, spec.offset().getZ(), 0.001);
    }

    @Test
    void load_offset_invalidString_usesDefault() {
        var cfg = yamlOf("text: 'T'\noffset: 'bad'");
        var spec = TextDisplayConfigLoader.load(cfg, "ns:item").getFirst();
        assertEquals(0.25, spec.offset().getY(), 0.001);
    }

    @Test
    void load_scale_null_defaultsToOne() {
        var cfg = yamlOf("text: 'T'");
        var spec = TextDisplayConfigLoader.load(cfg, "ns:item").getFirst();
        assertEquals(1.0f, spec.visual().scaleX(), 0.001f);
        assertEquals(1.0f, spec.visual().scaleY(), 0.001f);
        assertEquals(1.0f, spec.visual().scaleZ(), 0.001f);
    }

    @Test
    void load_scale_asNumber() {
        var cfg = yamlOf("text: 'T'\nscale: 2.0");
        var spec = TextDisplayConfigLoader.load(cfg, "ns:item").getFirst();
        assertEquals(2.0f, spec.visual().scaleX(), 0.001f);
        assertEquals(2.0f, spec.visual().scaleY(), 0.001f);
        assertEquals(2.0f, spec.visual().scaleZ(), 0.001f);
    }

    @Test
    void load_scale_negativeNumber_defaultsToOne() {
        var cfg = yamlOf("text: 'T'\nscale: -1.0");
        var spec = TextDisplayConfigLoader.load(cfg, "ns:item").getFirst();
        assertEquals(1.0f, spec.visual().scaleX(), 0.001f);
    }

    @Test
    void load_scale_asString() {
        var cfg = yamlOf("text: 'T'\nscale: '2.0,3.0,4.0'");
        var spec = TextDisplayConfigLoader.load(cfg, "ns:item").getFirst();
        assertEquals(2.0f, spec.visual().scaleX(), 0.001f);
        assertEquals(3.0f, spec.visual().scaleY(), 0.001f);
        assertEquals(4.0f, spec.visual().scaleZ(), 0.001f);
    }

    @Test
    void load_scale_asList3() {
        var cfg = yamlOf("text: 'T'\nscale: [2.0, 3.0, 4.0]");
        var spec = TextDisplayConfigLoader.load(cfg, "ns:item").getFirst();
        assertEquals(2.0f, spec.visual().scaleX(), 0.001f);
        assertEquals(3.0f, spec.visual().scaleY(), 0.001f);
        assertEquals(4.0f, spec.visual().scaleZ(), 0.001f);
    }

    @Test
    void load_scale_asList1_usedForAll() {
        var cfg = yamlOf("text: 'T'\nscale: [3.0]");
        var spec = TextDisplayConfigLoader.load(cfg, "ns:item").getFirst();
        assertEquals(3.0f, spec.visual().scaleX(), 0.001f);
        assertEquals(3.0f, spec.visual().scaleY(), 0.001f);
        assertEquals(3.0f, spec.visual().scaleZ(), 0.001f);
    }

    @Test
    void load_scale_invalidString_defaultsToOne() {
        var cfg = yamlOf("text: 'T'\nscale: 'bad_scale'");
        var spec = TextDisplayConfigLoader.load(cfg, "ns:item").getFirst();
        assertEquals(1.0f, spec.visual().scaleX(), 0.001f);
    }

    @Test
    void load_brightness_absent_returnsNull() {
        var cfg = yamlOf("text: 'T'");
        var spec = TextDisplayConfigLoader.load(cfg, "ns:item").getFirst();
        assertNull(spec.visual().brightnessBlock());
        assertNull(spec.visual().brightnessSky());
    }

    @Test
    void load_brightness_asNumber() {
        var cfg = yamlOf("text: 'T'\nbrightness: 10");
        var spec = TextDisplayConfigLoader.load(cfg, "ns:item").getFirst();
        assertEquals(10, spec.visual().brightnessBlock());
        assertEquals(10, spec.visual().brightnessSky());
    }

    @Test
    void load_brightness_asList() {
        var cfg = yamlOf("text: 'T'\nbrightness: [5, 12]");
        var spec = TextDisplayConfigLoader.load(cfg, "ns:item").getFirst();
        assertEquals(5, spec.visual().brightnessBlock());
        assertEquals(12, spec.visual().brightnessSky());
    }

    @Test
    void load_brightness_asSection() {
        var cfg = yamlOf("text: 'T'\nbrightness:\n  block: 3\n  sky: 8\n");
        var spec = TextDisplayConfigLoader.load(cfg, "ns:item").getFirst();
        assertEquals(3, spec.visual().brightnessBlock());
        assertEquals(8, spec.visual().brightnessSky());
    }

    @Test
    void load_brightness_booleanFalse_returnsNull() {
        var cfg = yamlOf("text: 'T'\nbrightness: false");
        var spec = TextDisplayConfigLoader.load(cfg, "ns:item").getFirst();
        assertNull(spec.visual().brightnessBlock());
    }

    @Test
    void load_brightness_booleanTrue_warnsAndReturnsNull() {
        var cfg = yamlOf("text: 'T'\nbrightness: true");
        var spec = TextDisplayConfigLoader.load(cfg, "ns:item").getFirst();
        assertNull(spec.visual().brightnessBlock());
    }

    @Test
    void load_brightness_invalidList_returnsNull() {
        var cfg = yamlOf("text: 'T'\nbrightness: [bad]");
        var spec = TextDisplayConfigLoader.load(cfg, "ns:item").getFirst();
        assertNull(spec.visual().brightnessBlock());
    }

    @Test
    void load_brightness_clamped() {
        var cfg = yamlOf("text: 'T'\nbrightness: 99");
        var spec = TextDisplayConfigLoader.load(cfg, "ns:item").getFirst();
        assertEquals(15, spec.visual().brightnessBlock());
    }

    @Test
    void load_textOpacity_absent_returnsMinusOne() {
        var cfg = yamlOf("text: 'T'");
        var spec = TextDisplayConfigLoader.load(cfg, "ns:item").getFirst();
        assertEquals((byte) -1, spec.visual().opacity());
    }

    @Test
    void load_textOpacity_oneOrAbove_returnsMinusOne() {
        var cfg = yamlOf("text: 'T'\ntext_opacity: 1.0");
        var spec = TextDisplayConfigLoader.load(cfg, "ns:item").getFirst();
        assertEquals((byte) -1, spec.visual().opacity());
    }

    @Test
    void load_textOpacity_half_returnsMappedByte() {
        var cfg = yamlOf("text: 'T'\ntext_opacity: 0.5");
        var spec = TextDisplayConfigLoader.load(cfg, "ns:item").getFirst();
        assertEquals((byte) Math.round(0.5f * 254f), spec.visual().opacity());
    }

    @Test
    void load_textOpacity_asString() {
        var cfg = yamlOf("text: 'T'\ntext_opacity: '0.5'");
        var spec = TextDisplayConfigLoader.load(cfg, "ns:item").getFirst();
        assertEquals((byte) Math.round(0.5f * 254f), spec.visual().opacity());
    }

    @Test
    void load_textOpacity_invalidString_returnsMinusOne() {
        var cfg = yamlOf("text: 'T'\ntext_opacity: 'bad'");
        var spec = TextDisplayConfigLoader.load(cfg, "ns:item").getFirst();
        assertEquals((byte) -1, spec.visual().opacity());
    }

    @Test
    void load_textOpacity_unsupportedType_returnsMinusOne() {
        var cfg = yamlOf("text: 'T'\ntext_opacity:\n  nested: value");
        var spec = TextDisplayConfigLoader.load(cfg, "ns:item").getFirst();
        assertEquals((byte) -1, spec.visual().opacity());
    }

    @Test
    void load_textShadow_default_isTrue() {
        var cfg = yamlOf("text: 'T'");
        var spec = TextDisplayConfigLoader.load(cfg, "ns:item").getFirst();
        assertTrue(spec.visual().shadow());
    }

    @Test
    void load_textShadow_false() {
        var cfg = yamlOf("text: 'T'\ntext_shadow: false");
        var spec = TextDisplayConfigLoader.load(cfg, "ns:item").getFirst();
        assertFalse(spec.visual().shadow());
    }

    @Test
    void load_seeThrough_default_isFalse() {
        var cfg = yamlOf("text: 'T'");
        var spec = TextDisplayConfigLoader.load(cfg, "ns:item").getFirst();
        assertFalse(spec.visual().seeThrough());
    }

    @Test
    void load_seeThrough_true() {
        var cfg = yamlOf("text: 'T'\nsee_through: true");
        var spec = TextDisplayConfigLoader.load(cfg, "ns:item").getFirst();
        assertTrue(spec.visual().seeThrough());
    }

    @Test
    void load_lineWidth_default() {
        var cfg = yamlOf("text: 'T'");
        var spec = TextDisplayConfigLoader.load(cfg, "ns:item").getFirst();
        assertEquals(1403, spec.visual().lineWidth());
    }

    @Test
    void load_lineWidth_custom() {
        var cfg = yamlOf("text: 'T'\nline_width: 200");
        var spec = TextDisplayConfigLoader.load(cfg, "ns:item").getFirst();
        assertEquals(200, spec.visual().lineWidth());
    }

    @Test
    void load_lineWidth_zero_usesDefault() {
        var cfg = yamlOf("text: 'T'\nline_width: 0");
        var spec = TextDisplayConfigLoader.load(cfg, "ns:item").getFirst();
        assertEquals(1403, spec.visual().lineWidth());
    }

    @Test
    void load_viewRange_default() {
        var cfg = yamlOf("text: 'T'");
        var spec = TextDisplayConfigLoader.load(cfg, "ns:item").getFirst();
        assertEquals(16.0, spec.viewRange(), 0.001);
    }

    @Test
    void load_viewRange_custom() {
        var cfg = yamlOf("text: 'T'\nview_range: 32.0");
        var spec = TextDisplayConfigLoader.load(cfg, "ns:item").getFirst();
        assertEquals(32.0, spec.viewRange(), 0.001);
    }

    @Test
    void load_viewRange_zero_usesDefault() {
        var cfg = yamlOf("text: 'T'\nview_range: 0");
        var spec = TextDisplayConfigLoader.load(cfg, "ns:item").getFirst();
        assertEquals(16.0, spec.viewRange(), 0.001);
    }

    @Test
    void load_refreshInterval_default() {
        var cfg = yamlOf("text: 'T'");
        var spec = TextDisplayConfigLoader.load(cfg, "ns:item").getFirst();
        assertEquals(0, spec.refreshInterval());
    }

    @Test
    void load_refreshInterval_custom() {
        var cfg = yamlOf("text: 'T'\nrefresh_interval: 20");
        var spec = TextDisplayConfigLoader.load(cfg, "ns:item").getFirst();
        assertEquals(20, spec.refreshInterval());
    }

    @Test
    void load_refreshInterval_negative_usesDefault() {
        var cfg = yamlOf("text: 'T'\nrefresh_interval: -1");
        var spec = TextDisplayConfigLoader.load(cfg, "ns:item").getFirst();
        assertEquals(0, spec.refreshInterval());
    }

    @Test
    void load_yawAndPitchOffset() {
        var cfg = yamlOf("text: 'T'\nyaw: 45.0\npitch: 30.0");
        var spec = TextDisplayConfigLoader.load(cfg, "ns:item").getFirst();
        assertEquals(45.0f, spec.yawOffset(), 0.001f);
        assertEquals(30.0f, spec.pitchOffset(), 0.001f);
    }

    @Test
    void rawText_singleLine() {
        var cfg = yamlOf("text: 'Hello World'");
        var spec = TextDisplayConfigLoader.load(cfg, "ns:item").getFirst();
        assertEquals("Hello World", spec.rawText());
    }

    @Test
    void rawText_multiLine_joinedWithNewline() {
        var cfg = yamlOf("text:\n  - 'Line1'\n  - 'Line2'\n  - 'Line3'\n");
        var spec = TextDisplayConfigLoader.load(cfg, "ns:item").getFirst();
        assertEquals("Line1\nLine2\nLine3", spec.rawText());
    }

    @Test
    void textLines_immutable() {
        var cfg = yamlOf("text:\n  - 'A'\n  - 'B'\n");
        var spec = TextDisplayConfigLoader.load(cfg, "ns:item").getFirst();
        assertThrows(UnsupportedOperationException.class, () -> spec.textLines().add("C"));
    }
}
