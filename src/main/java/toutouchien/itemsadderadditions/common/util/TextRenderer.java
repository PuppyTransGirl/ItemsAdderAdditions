package toutouchien.itemsadderadditions.common.util;

import dev.lone.itemsadder.api.FontImages.FontImageWrapper;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.integration.hook.PlaceholderAPIUtils;

/**
 * Renders MiniMessage-formatted text into Adventure {@link Component}s.
 *
 * <p>Combines three transformations that every text-based action needs:
 * <ol>
 *   <li>PlaceholderAPI placeholder parsing (player-specific, skipped when
 *       the entity is not a player or PAPI is not installed).</li>
 *   <li>MiniMessage deserialization.</li>
 *   <li>ItemsAdder font-image replacement (e.g. {@code :my_icon:}).</li>
 * </ol>
 *
 * <p>Use this instead of inlining the same three-line pipeline in every action
 * that sends text to players.
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * Component message = TextRenderer.render(context.runOn(), rawText);
 * entity.sendMessage(message);
 * }</pre>
 */
@NullMarked
public final class TextRenderer {
    private static final MiniMessage MM = MiniMessage.miniMessage();

    private TextRenderer() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Renders {@code raw} into a {@link Component} for the given entity.
     *
     * <p>If {@code entity} is a {@link Player}, PlaceholderAPI placeholders are
     * expanded first. MiniMessage tags and font images are always processed.
     *
     * @param entity the entity the text will be shown to (used for placeholder resolution)
     * @param raw    the raw MiniMessage string (may contain {@code <tags>} and {@code :icons:})
     * @return the rendered component, ready to send
     */
    public static Component render(Entity entity, String raw) {
        String input = entity instanceof Player player
                ? PlaceholderAPIUtils.parsePlaceholders(player, raw)
                : raw;
        return FontImageWrapper.replaceFontImages(MM.deserialize(input));
    }

    /**
     * Renders {@code raw} without any PlaceholderAPI expansion.
     *
     * <p>Use when no player context is available or when the text is not
     * player-specific.
     *
     * @param raw the raw MiniMessage string
     * @return the rendered component
     */
    public static Component renderStatic(String raw) {
        return FontImageWrapper.replaceFontImages(MM.deserialize(raw));
    }

    /**
     * Renders {@code raw} for an optional player.
     *
     * <p>When {@code player} is {@code null}, falls back to
     * {@link #renderStatic(String)}.
     *
     * @param player the player, or {@code null}
     * @param raw    the raw MiniMessage string
     * @return the rendered component
     */
    public static Component render(@Nullable Player player, String raw) {
        if (player == null) return renderStatic(raw);
        String input = PlaceholderAPIUtils.parsePlaceholders(player, raw);
        return FontImageWrapper.replaceFontImages(MM.deserialize(input));
    }
}
