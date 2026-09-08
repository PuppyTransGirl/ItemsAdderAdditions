package toutouchien.itemsadderadditions.patch.impl.ia_4_0_18;

import toutouchien.itemsadderadditions.patch.VersionConstraint;
import toutouchien.itemsadderadditions.patch.VersionSet;
import toutouchien.itemsadderadditions.patch.impl.ia_4_0_17.StatRequirementsCapturePatch_IA_4_0_17;

public final class StatRequirementsCapturePatch_IA_4_0_18 extends StatRequirementsCapturePatch_IA_4_0_17 {
    @Override
    public VersionConstraint supportedVersions() {
        return VersionSet.ia("4.0.18");
    }

    @Override
    public String targetClass() {
        return "itemsadder/m/mv";
    }

    @Override
    protected String targetDescriptor() {
        return "(Lorg/bukkit/entity/Entity;Litemsadder/m/jj;Litemsadder/m/oz;)V";
    }
}
