package toutouchien.itemsadderadditions.patch.impl.ia_4_0_16;

import toutouchien.itemsadderadditions.patch.MethodCallReplacePatch;
import toutouchien.itemsadderadditions.patch.VersionConstraint;
import toutouchien.itemsadderadditions.patch.VersionSet;

public class AddEnchantmentPatch_IA_4_0_16 extends MethodCallReplacePatch {
    @Override
    public VersionConstraint supportedVersions() {
        return VersionSet.ia("4.0.16", "4.0.17");
    }

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
