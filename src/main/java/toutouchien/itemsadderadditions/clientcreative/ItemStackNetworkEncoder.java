package toutouchien.itemsadderadditions.clientcreative;

import org.bukkit.inventory.ItemStack;

@FunctionalInterface
public interface ItemStackNetworkEncoder {
    /*
     * Encode the Bukkit ItemStack to the exact network format the Fabric client expects.
     *
     * Best option:
     * - Convert Bukkit/Craft stack to NMS stack.
     * - Encode it with the same Minecraft ItemStack packet codec used by the client.
     *
     * Do not manually serialize raw NBT; that is what caused "Invalid tag id: 31".
     */
    byte[] encode(ItemStack itemStack);
}
