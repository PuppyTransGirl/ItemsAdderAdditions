package toutouchien.itemsadderadditions.patch.impl.ia_4_0_18;

import toutouchien.itemsadderadditions.patch.VersionConstraint;
import toutouchien.itemsadderadditions.patch.VersionSet;
import toutouchien.itemsadderadditions.patch.impl.ia_4_0_17.CooldownCapturePatch_IA_4_0_17;

public final class CooldownCapturePatch_IA_4_0_18 extends CooldownCapturePatch_IA_4_0_17 {
    @Override
    public VersionConstraint supportedVersions() {
        return VersionSet.ia("4.0.18");
    }

    @Override
    public String targetClass() {
        return "itemsadder/m/na";
    }

    @Override
    protected String targetDescriptor() {
        return "(Lorg/bukkit/entity/LivingEntity;Litemsadder/m/qm;)Z";
    }
}
