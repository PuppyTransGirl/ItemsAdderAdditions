package toutouchien.itemsadderadditions.patches.impl;

import org.objectweb.asm.commons.GeneratorAdapter;
import toutouchien.itemsadderadditions.patches.BytecodeHelper;
import toutouchien.itemsadderadditions.patches.CallSiteInjectPatch;
import toutouchien.itemsadderadditions.patches.CallSiteInjectPoint;
import toutouchien.itemsadderadditions.recipes.stonecutter.StonecutterPatchBridge;

public final class StonecutterSelectiveBypassPatch extends CallSiteInjectPatch {
    @Override
    public String targetClass() {
        return "itemsadder/m/tw";
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
        return "itemsadder/m/oy";
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
        // Stack currently: [boolean originalResult]
        // Push method arg 0 = InventoryClickEvent
        ga.loadArg(0);

        // Call:
        // boolean StonecutterPatchBridge.filterCustomItemCheck(
        //     boolean original,
        //     InventoryClickEvent event
        // )
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
