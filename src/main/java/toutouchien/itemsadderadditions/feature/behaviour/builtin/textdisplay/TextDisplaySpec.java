package toutouchien.itemsadderadditions.feature.behaviour.builtin.textdisplay;

import org.bukkit.util.Vector;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.nms.api.textdisplay.PacketTextDisplayAlignment;
import toutouchien.itemsadderadditions.nms.api.textdisplay.PacketTextDisplayBillboard;
import toutouchien.itemsadderadditions.nms.api.textdisplay.PacketTextDisplayVisual;

import java.util.List;

@NullMarked
public record TextDisplaySpec(
        String id,
        List<String> textLines,
        Vector offset,
        PacketTextDisplayBillboard billboard,
        PacketTextDisplayAlignment alignment,
        boolean shadow,
        boolean seeThrough,
        int lineWidth,
        @Nullable Integer backgroundArgb,
        byte opacity,
        float scale,
        double viewRange,
        int refreshInterval
) {
    public TextDisplaySpec {
        textLines = List.copyOf(textLines);
        offset = offset.clone();
    }

    public String rawText() {
        return String.join("\n", textLines);
    }

    public PacketTextDisplayVisual visual() {
        return new PacketTextDisplayVisual(
                billboard,
                alignment,
                shadow,
                seeThrough,
                lineWidth,
                backgroundArgb,
                opacity,
                scale
        );
    }
}
