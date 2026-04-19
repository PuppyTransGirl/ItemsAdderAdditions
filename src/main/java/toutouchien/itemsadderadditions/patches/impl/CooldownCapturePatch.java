package toutouchien.itemsadderadditions.patches.impl;

import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;
import toutouchien.itemsadderadditions.bridge.CooldownBridge;
import toutouchien.itemsadderadditions.patches.InjectPoint;
import toutouchien.itemsadderadditions.patches.MethodInjectPatch;

public class CooldownCapturePatch extends MethodInjectPatch {
    @Override
    public String targetClass() {
        return "itemsadder/m/lr";
    }

    // b(LivingEntity livingEntity, oy oy2)
    @Override
    protected String targetMethod() {
        return "b";
    }

    @Override
    protected String targetDescriptor() {
        return "(Lorg/bukkit/entity/LivingEntity;Litemsadder/m/oy;)Z";
    }

    @Override
    protected InjectPoint injectPoint() {
        return InjectPoint.BEFORE_RETURN;
    }

    /**
     * Stack at BEFORE_RETURN: boolean result is on top.
     * <p>
     * We need to call:
     *   CooldownBridge.capture(boolean result, Object livingEntity, int itemHash)
     * <p>
     * So we:
     *  1. dup the result (keep it for the return)
     *  2. load arg 0 (LivingEntity)
     *  3. load the itemHash from oy arg 1 (field BH, which is an int)
     *  4. call capture() which returns the boolean back
     *  5. the return then uses that value
     */
    @Override
    protected void inject(GeneratorAdapter ga) {
        // result is on top of stack, dup it so we still have it after the call
        ga.dup();

        // load LivingEntity (arg 0)
        ga.loadArg(0);

        // load oy.BH (arg 1, field BH = int hashCode of namespaced ID)
        ga.loadArg(1);
        ga.getField(
                Type.getObjectType("itemsadder/m/oy"),
                "BH",
                Type.INT_TYPE
        );

        // call CooldownBridge.capture(boolean, Object, int) -> boolean
        ga.invokeStatic(
                Type.getType(CooldownBridge.class),
                Method.getMethod(
                        "boolean capture(boolean, Object, int)"
                )
        );
        // the capture() return value replaces the original boolean on the stack
        // which is what gets returned
    }
}
