package toutouchien.itemsadderadditions.feature.advancement;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.common.utils.TextRenderer;

import java.util.Locale;

@NullMarked
public final class AdvancementCompletionListener implements Listener {
    private final AdvancementRegistry registry;

    public AdvancementCompletionListener(AdvancementRegistry registry) {
        this.registry = registry;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void formatMessage(PlayerAdvancementDoneEvent event) {
        NamespacedKey key = event.getAdvancement().getKey();
        AdvancementDefinition def = registry.get(key);
        if (def == null) return;

        event.message(def.display().announceToChat()
                ? buildAnnouncementMessage(event, def)
                : null);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void runActions(PlayerAdvancementDoneEvent event) {
        NamespacedKey key = event.getAdvancement().getKey();
        AdvancementDefinition def = registry.get(key);
        if (def == null) return;
        if (def.onComplete().isEmpty()) return;
        def.onComplete().execute(event.getPlayer());
    }

    private static Component buildAnnouncementMessage(
            PlayerAdvancementDoneEvent event,
            AdvancementDefinition def
    ) {
        String frame = normalizeFrame(def.display().frame());
        Component title = TextRenderer.render(event.getPlayer(), def.display().title());
        Component description = TextRenderer.render(event.getPlayer(), def.display().description());

        Component hoverTitle = title.colorIfAbsent(NamedTextColor.GREEN);
        Component hoverDescription = description.colorIfAbsent(NamedTextColor.GREEN);

        Component advancementName = Component.text()
                .append(Component.text("["))
                .append(title)
                .append(Component.text("]"))
                .color(frameColor(frame))
                .hoverEvent(HoverEvent.showText(Component.text()
                        .append(hoverTitle)
                        .append(Component.newline())
                        .append(hoverDescription)
                        .build()))
                .build();

        return Component.translatable(
                "chat.type.advancement." + frame,
                event.getPlayer().displayName(),
                advancementName
        );
    }

    private static String normalizeFrame(String frame) {
        return switch (frame.toLowerCase(Locale.ROOT)) {
            case "goal" -> "goal";
            case "challenge" -> "challenge";
            default -> "task";
        };
    }

    private static TextColor frameColor(String frame) {
        return frame.equals("challenge") ? NamedTextColor.DARK_PURPLE : NamedTextColor.GREEN;
    }
}
