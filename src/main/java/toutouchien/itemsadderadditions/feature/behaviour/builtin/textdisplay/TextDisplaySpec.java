package toutouchien.itemsadderadditions.feature.behaviour.builtin.textdisplay;

import org.bukkit.util.Vector;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.nms.api.textdisplay.PacketTextDisplayVisual;

import java.util.List;

/**
 * Parsed configuration for a single text display attached to a block or furniture item.
 * <p>
 * Multiple specs can exist per item when using the {@code displays} section in config.
 */
@NullMarked
public record TextDisplaySpec(
        String id,
        List<String> textLines,
        Vector offset,
        PacketTextDisplayVisual visual,
        double viewRange,
        int refreshInterval
) {
    public TextDisplaySpec {
        textLines = List.copyOf(textLines);
        offset = offset.clone();
    }

    /**
     * Returns the text lines joined by newlines, ready to pass to MiniMessage.
     */
    public String rawText() {
        return String.join("\n", textLines);
    }
}
