package toutouchien.itemsadderadditions.patch.impl.ia_4_0_18;

import toutouchien.itemsadderadditions.patch.VersionConstraint;
import toutouchien.itemsadderadditions.patch.VersionSet;
import toutouchien.itemsadderadditions.patch.impl.ia_4_0_17.StonecutterSelectiveBypassPatch_IA_4_0_17;

public final class StonecutterSelectiveBypassPatch_IA_4_0_18
        extends StonecutterSelectiveBypassPatch_IA_4_0_17 {
    @Override
    public VersionConstraint supportedVersions() {
        return VersionSet.ia("4.0.18");
    }

    @Override
    public String targetClass() {
        return "itemsadder/m/vw";
    }

    @Override
    protected String targetCallOwner() {
        return "itemsadder/m/qm";
    }
}
