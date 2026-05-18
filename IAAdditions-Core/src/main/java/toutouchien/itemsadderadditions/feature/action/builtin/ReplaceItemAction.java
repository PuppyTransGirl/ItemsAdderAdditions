package toutouchien.itemsadderadditions.feature.action.builtin;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.common.annotation.Parameter;
import toutouchien.itemsadderadditions.common.namespace.NamespaceUtils;
import toutouchien.itemsadderadditions.feature.action.ActionContext;
import toutouchien.itemsadderadditions.feature.action.ActionExecutor;
import toutouchien.itemsadderadditions.feature.action.annotation.Action;

import java.util.Map;

/**
 * Replaces the item held by the target player with another item.
 * Optionally copies durability, enchantments, and/or PDC from the original item.
 *
 * <pre>{@code
 * replace_item:
 *   item: "myitems:upgraded_sword"   # required - vanilla or ItemsAdder item
 *   copy_durability: true            # optional, default false
 *   copy_enchantments: true          # optional, default false
 *   copy_pdc: true                   # optional, default false
 * }</pre>
 */
@SuppressWarnings("unused")
@NullMarked
@Action(key = "replace_item")
public final class ReplaceItemAction extends ActionExecutor {
    @Parameter(key = "item", type = String.class, required = true)
    private String item;

    @Parameter(key = "copy_durability", type = Boolean.class)
    private boolean copyDurability = false;

    @Parameter(key = "copy_enchantments", type = Boolean.class)
    private boolean copyEnchantments = false;

    @Parameter(key = "copy_pdc", type = Boolean.class)
    private boolean copyPdc = false;

    @Override
    protected void execute(ActionContext context) {
        Entity runOn = context.runOn();
        if (!(runOn instanceof Player player))
            return;

        boolean isOffHand = context.triggerType().name().endsWith("_OFFHAND");
        PlayerInventory inv = player.getInventory();
        ItemStack source = isOffHand ? inv.getItemInOffHand() : inv.getItemInMainHand();

        ItemStack template = NamespaceUtils.itemByID(item, item);
        if (template == null)
            return;

        ItemStack replacement = template.clone();

        if (copyDurability || copyEnchantments || copyPdc) {
            ItemMeta srcMeta = source.getItemMeta();
            ItemMeta dstMeta = replacement.getItemMeta();
            if (srcMeta != null && dstMeta != null) {
                if (copyDurability
                        && srcMeta instanceof Damageable srcDamageable
                        && dstMeta instanceof Damageable dstDamageable) {
                    dstDamageable.setDamage(srcDamageable.getDamage());
                }
                if (copyEnchantments) {
                    Map<Enchantment, Integer> enchants = srcMeta.getEnchants();
                    for (Map.Entry<Enchantment, Integer> entry : enchants.entrySet()) {
                        dstMeta.addEnchant(entry.getKey(), entry.getValue(), true);
                    }
                }
                if (copyPdc) {
                    srcMeta.getPersistentDataContainer().copyTo(dstMeta.getPersistentDataContainer(), true);
                }
                replacement.setItemMeta(dstMeta);
            }
        }

        if (isOffHand) {
            inv.setItemInOffHand(replacement);
        } else {
            inv.setItemInMainHand(replacement);
        }
    }
}
