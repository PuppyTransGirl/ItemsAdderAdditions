package toutouchien.itemsadderadditions.patches.impl;

import toutouchien.itemsadderadditions.patches.MethodCallReplacePatch;

public class AddEnchantmentPatch extends MethodCallReplacePatch {
    @Override
    public String targetClass() {
        return "itemsadder/m/jq";
    }

    // ItemStack c(ItemStack source, ItemStack target)
    @Override
    protected String targetMethod() {
        return "c";
    }

    @Override
    protected String targetDescriptor() {
        return "(Lorg/bukkit/inventory/ItemStack;Lorg/bukkit/inventory/ItemStack;)"
                + "Lorg/bukkit/inventory/ItemStack;";
    }

    // The call we want to intercept
    @Override
    protected String targetCallOwner() {
        return "org/bukkit/inventory/ItemStack";
    }

    @Override
    protected String targetCallName() {
        return "addEnchantment";
    }

    @Override
    protected String replacementCallName() {
        return "addUnsafeEnchantment";
    }
}
