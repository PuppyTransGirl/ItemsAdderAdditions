package toutouchien.itemsadderadditions.nms.api;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public interface INmsToastHandler {
    void sendToast(Player player, ItemStack itemStack, Component title, String frame);
}
