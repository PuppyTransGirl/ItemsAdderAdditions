package toutouchien.itemsadderadditions.patch.impl.ia_4_0_17;

import toutouchien.itemsadderadditions.patch.MethodCallReplacePatch;
import toutouchien.itemsadderadditions.patch.VersionConstraint;
import toutouchien.itemsadderadditions.patch.VersionSet;

public final class AddEnchantmentPatch_IA_4_0_17 extends MethodCallReplacePatch {
    @Override
    public VersionConstraint supportedVersions() {
        return VersionSet.ia("4.0.17");
    }

    @Override
    public String targetClass() {
        return "itemsadder/m/jq";
    }

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
