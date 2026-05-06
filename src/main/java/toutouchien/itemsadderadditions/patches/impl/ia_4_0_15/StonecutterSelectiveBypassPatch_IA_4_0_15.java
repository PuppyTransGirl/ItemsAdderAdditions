package toutouchien.itemsadderadditions.patches.impl.ia_4_0_15;

import org.objectweb.asm.commons.GeneratorAdapter;
import toutouchien.itemsadderadditions.bridge.StonecutterPatchBridge;
import toutouchien.itemsadderadditions.patches.*;

public final class StonecutterSelectiveBypassPatch_IA_4_0_15
        extends CallSiteInjectPatch {
    @Override
    public VersionConstraint supportedVersions() {
        return VersionSet.ia("4.0.15");
    }

    @Override
    public String targetClass() {
        return "itemsadder/m/tt";
    }

    @Override
    protected String targetMethod() {
        return "b";
    }

    @Override
    protected String targetDescriptor() {
        return "(Lorg/bukkit/event/inventory/InventoryClickEvent;)V";
    }

    @Override
    protected String targetCallOwner() {
        return "itemsadder/m/ov";
    }

    @Override
    protected String targetCallName() {
        return "isCustomItem";
    }

    @Override
    protected CallSiteInjectPoint callSiteInjectPoint() {
        return CallSiteInjectPoint.AFTER_CALL;
    }

    @Override
    protected void inject(GeneratorAdapter ga) {
        ga.loadArg(0);

        BytecodeHelper.invokeStatic(
                ga,
                StonecutterPatchBridge.class,
                "boolean filterCustomItemCheck(" +
                        "boolean, " +
                        "org.bukkit.event.inventory.InventoryClickEvent" +
                        ")"
        );
    }
}
