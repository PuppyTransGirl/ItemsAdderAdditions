package toutouchien.itemsadderadditions.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.json.JSONComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import toutouchien.itemsadderadditions.ItemsAdderAdditions;

import java.util.Locale;
import java.util.UUID;

public final class ToastUtils {
    private ToastUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static void sendToast(Player player, ItemStack itemStack, Component title, String frame) {
        String json = """
                    {
                        "criteria": {
                          "trigger": {
                            "trigger": "minecraft:impossible"
                          }
                        },
                        "display": {
                            "icon": {
                                "id": "%s",
                                "count": "%s",
                                "components": %s
                            },
                            "title": %s,
                            "description": "This isn't shown",
                            "frame": "%s",
                            "show_toast": true,
                            "announce_to_chat": false,
                            "hidden": true
                        },
                        "requirements": [
                          [
                            "trigger"
                          ]
                        ]
                    }
                """.formatted(
                "minecraft:" + itemStack.getType().name().toLowerCase(Locale.ROOT),
                itemStack.getAmount(),
                Bukkit.getUnsafe().serializeItemAsJson(itemStack).getAsJsonObject("components").toString(),
                JSONComponentSerializer.json().serialize(title),
                frame
        );

        NamespacedKey key = new NamespacedKey(ItemsAdderAdditions.instance(), UUID.randomUUID().toString());
        Bukkit.getUnsafe().loadAdvancement(
                key, json
        );

        player.getAdvancementProgress(Bukkit.getAdvancement(key)).awardCriteria("trigger");

        Bukkit.getScheduler().runTaskLater(ItemsAdderAdditions.instance(), () -> {
            player.getAdvancementProgress(Bukkit.getAdvancement(key)).revokeCriteria("trigger");
            Bukkit.getUnsafe().removeAdvancement(key);
        }, 10L);
    }
}
