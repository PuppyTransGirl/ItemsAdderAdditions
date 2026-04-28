package toutouchien.itemsadderadditions.patches.impl.ia_4_0_15;

import toutouchien.itemsadderadditions.patches.MethodCallReplacePatch;
import toutouchien.itemsadderadditions.patches.VersionConstraint;
import toutouchien.itemsadderadditions.patches.VersionSet;

public class AddEnchantmentPatch_IA_4_0_15 extends MethodCallReplacePatch {
    @Override
    public VersionConstraint supportedVersions() {
        return VersionSet.ia("4.0.15");
    }

    @Override
    public String targetClass() {
        return "itemsadder/m/jp";
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
