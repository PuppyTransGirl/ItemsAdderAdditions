package toutouchien.itemsadderadditions.nms.api.textdisplay;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public record PacketTextDisplayVisual(
        PacketTextDisplayBillboard billboard,
        PacketTextDisplayAlignment alignment,
        boolean shadow,
        boolean seeThrough,
        int lineWidth,
        @Nullable Integer backgroundArgb,
        byte opacity,
        float scale
) {
}
