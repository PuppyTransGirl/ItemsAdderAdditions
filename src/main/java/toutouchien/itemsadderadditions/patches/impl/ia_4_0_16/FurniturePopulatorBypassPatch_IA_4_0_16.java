package toutouchien.itemsadderadditions.patches.impl.ia_4_0_16;

import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;
import toutouchien.itemsadderadditions.bridge.FurniturePopulatorBypassBridge;
import toutouchien.itemsadderadditions.patches.InjectPoint;
import toutouchien.itemsadderadditions.patches.MethodInjectPatch;
import toutouchien.itemsadderadditions.patches.VersionConstraint;
import toutouchien.itemsadderadditions.patches.VersionSet;

/**
 * Injects into {@code WorldGeneratorConfig.i(auo)} at
 * {@link InjectPoint#ENTRY} so that furniture-only {@code blocks_populators}
 * entries are silently removed from IA's view before its loop starts.
 *
 * <h3>Why ENTRY and not BEFORE_RETURN</h3>
 * <p>{@code WorldGeneratorConfig.i()} is {@code static}; the key list it
 * iterates is a local variable built inside the method, not a field we can
 * reach afterwards. Injecting at entry lets us mutate the in-memory
 * {@code YamlConfiguration} that the {@code auo} wraps, which
 * is what IA's own {@code eS()} / {@code fj()} calls read throughout the loop.
 *
 * <h3>What the injection does</h3>
 * <pre>
 *   // injected at method entry - before any IA code runs:
 *   FurniturePopulatorBypassBridge.stripFurnitureKeys(itemDefinitionFile);
 * </pre>
 *
 * <p>The bridge receives the {@code auo} typed as {@link Object}
 * to avoid any compile-time dependency on the obfuscated class. It uses
 * reflection to locate the underlying {@link org.bukkit.configuration.file.YamlConfiguration}
 * and deletes furniture-keyed nodes from it, so IA never sees them.
 *
 * <h3>Error safety</h3>
 * <p>All exceptions inside {@link FurniturePopulatorBypassBridge#stripFurnitureKeys}
 * are caught internally; a failure degrades gracefully to IA logging its own
 * "unknown block" warnings for furniture entries, with no crash.
 */
public final class FurniturePopulatorBypassPatch_IA_4_0_16 extends MethodInjectPatch {
    @Override
    public VersionConstraint supportedVersions() {
        return VersionSet.ia("4.0.16", "4.0.17");
    }

    /**
     * {@code itemsadder.m.uv} - confirmed from decompiled sources.
     */
    @Override
    public String targetClass() {
        return "itemsadder/m/uv";
    }

    /**
     * {@code public static void i(auo)} - IA's naming convention
     * for the static loader method on each config class.
     */
    @Override
    protected String targetMethod() {
        return "i";
    }

    /**
     * Descriptor for {@code i(auo itemDefinitionFile)}.
     * {@code auo} is {@code itemsadder.m.auo}.
     */
    @Override
    protected String targetDescriptor() {
        return "(Litemsadder/m/auo;)V";
    }

    @Override
    protected InjectPoint injectPoint() {
        return InjectPoint.ENTRY;
    }

    /**
     * Stack is empty at entry. We push arg 0 (the {@code auo})
     * and invoke the bridge.
     *
     * <pre>
     *   ALOAD 0   // auo itemDefinitionFile
     *   INVOKESTATIC FurniturePopulatorBypassBridge.stripFurnitureKeys(Object)V
     * </pre>
     */
    @Override
    protected void inject(GeneratorAdapter ga) {
        // arg 0 = auo - typed as Object in the bridge signature
        // to avoid a compile-time dependency on the obfuscated type
        ga.loadArg(0);
        ga.invokeStatic(
                Type.getType(FurniturePopulatorBypassBridge.class),
                Method.getMethod("void stripFurnitureKeys(Object)")
        );
    }
}
