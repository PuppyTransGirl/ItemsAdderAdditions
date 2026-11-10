package toutouchien.itemsadderadditions.patch.impl.ia_5_0_0;

import toutouchien.itemsadderadditions.patch.VersionConstraint;
import toutouchien.itemsadderadditions.patch.VersionSet;
import toutouchien.itemsadderadditions.patch.impl.ia_4_0_17.StonecutterSelectiveBypassPatch_IA_4_0_17;

public final class StonecutterSelectiveBypassPatch_IA_5_0_0
        extends StonecutterSelectiveBypassPatch_IA_4_0_17 {
    @Override
    public VersionConstraint supportedVersions() {
        return VersionSet.ia("5.0.0");
    }

    @Override
    public String targetClass() {
        return "itemsadder/m/afc";
    }

    @Override
    protected String targetCallOwner() {
        return "itemsadder/m/xl";
    }
}
