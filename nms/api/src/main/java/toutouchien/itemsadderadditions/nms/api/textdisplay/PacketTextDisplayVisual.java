package toutouchien.itemsadderadditions.nms.api.textdisplay;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Visual properties for a packet-based text display entity.
 * <p>
 * {@code opacity}: use {@code -1} for default full opacity, or {@code 0..254} for a custom alpha
 * value (0 = transparent, 254 = nearly opaque). Note that {@code 255} is equivalent to {@code -1}.
 * <p>
 * {@code brightnessBlock} and {@code brightnessSky}: both must be set together (0..15) to override
 * the world lighting, or both left {@code null} to inherit from the world.
 */
@NullMarked
public record PacketTextDisplayVisual(
        PacketTextDisplayBillboard billboard,
        PacketTextDisplayAlignment alignment,
        boolean shadow,
        boolean seeThrough,
        int lineWidth,
        @Nullable Integer backgroundArgb,
        byte opacity,
        float scaleX,
        float scaleY,
        float scaleZ,
        @Nullable Integer brightnessBlock,
        @Nullable Integer brightnessSky,
        float shadowRadius,
        float shadowStrength
) {
}
