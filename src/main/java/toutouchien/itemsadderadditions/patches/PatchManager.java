package toutouchien.itemsadderadditions.patches;

import net.bytebuddy.agent.ByteBuddyAgent;
import toutouchien.itemsadderadditions.patches.impl.ia_4_0_16.*;
import toutouchien.itemsadderadditions.utils.other.Log;

import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Entry point for the bytecode-patching system.
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * // Resolve the runtime version however your plugin does it:
 * Version version = Version.of("1.21.1", "3.4.0");
 * PatchManager.applyAll(version);
 * }</pre>
 *
 * <h3>Adding a new patch</h3>
 * <ol>
 *   <li>Create a class extending one of the patch base classes
 *       ({@link MethodInjectPatch}, {@link MethodCallReplacePatch}, etc.).</li>
 *   <li>Override {@link ClassPatch#supportedVersions()} to declare version compatibility.</li>
 *   <li>Register it in {@link #ALL_PATCHES} below.</li>
 * </ol>
 */
public final class PatchManager {

    private static final List<ClassPatch> ALL_PATCHES = List.of(
            new AddEnchantmentPatch_IA_4_0_16(),
            new CooldownCapturePatch_IA_4_0_16(),
            new CraftingRecipeBypassPatch_IA_4_0_16(),
            new TradeMachineCapturePatch_IA_4_0_16(),
            new StatRequirementsCapturePatch_IA_4_0_16(),
            new StonecutterSelectiveBypassPatch_IA_4_0_16()
    );

    private PatchManager() {
    }

    /**
     * Filters {@link #ALL_PATCHES} by {@code version}, attaches the Java agent,
     * and (re)transforms any already-loaded target classes.
     *
     * @param version the resolved Minecraft + ItemsAdder version at runtime
     */
    public static void applyAll(Version version) {
        List<ClassPatch> active = filterPatches(ALL_PATCHES, version);

        if (active.isEmpty()) {
            Log.info("Patcher", "No patches are compatible with " + version + " - nothing to do.");
            return;
        }

        Log.info("Patcher", "Applying " + active.size() + "/" + ALL_PATCHES.size()
                + " patches for " + version);

        Instrumentation inst = attachAgent();
        if (inst == null) return;

        Map<String, List<ClassPatch>> byClass = active.stream()
                .collect(Collectors.groupingBy(ClassPatch::targetClass));

        inst.addTransformer(new PatchTransformer(active), true);

        Set<String> patched = retransformLoadedClasses(inst, byClass);

        // Report deferred patches (target class not yet loaded)
        for (String internalName : byClass.keySet()) {
            if (!patched.contains(internalName)) {
                Log.info("Patcher", "Deferred (not loaded yet): "
                        + internalName.replace('/', '.'));
            }
        }
    }

    /**
     * Returns only the patches whose {@link ClassPatch#supportedVersions()} accepts {@code version}.
     */
    private static List<ClassPatch> filterPatches(List<ClassPatch> patches, Version version) {
        List<ClassPatch> active = new ArrayList<>();
        for (ClassPatch patch : patches) {
            if (patch.supportedVersions().test(version)) {
                active.add(patch);
            } else {
                Log.info("Patcher", "Skipped (incompatible version): "
                        + patch.getClass().getSimpleName()
                        + " - requires " + patch.supportedVersions()
                        + ", got " + version);
            }
        }
        return active;
    }

    /**
     * Attempts to attach the ByteBuddy agent, falling back to the default
     * attachment provider if the first attempt fails.
     *
     * @return the {@link Instrumentation} instance, or {@code null} if all attempts fail
     */
    private static Instrumentation attachAgent() {
        try {
            return ByteBuddyAgent.install();
        } catch (Exception ignored) {
            // First attempt failed; try with the explicit default provider
        }
        try {
            return ByteBuddyAgent.install(ByteBuddyAgent.AttachmentProvider.DEFAULT);
        } catch (Exception e) {
            Log.error("Patcher", "All agent-attach methods failed", e);
            return null;
        }
    }

    /**
     * Retransforms every already-loaded class that has a registered patch.
     *
     * @return the set of internal class names that were successfully retransformed
     */
    private static Set<String> retransformLoadedClasses(
            Instrumentation inst,
            Map<String, List<ClassPatch>> byClass
    ) {
        Set<String> patched = new HashSet<>();

        for (Class<?> clazz : inst.getAllLoadedClasses()) {
            String internalName = clazz.getName().replace('.', '/');
            if (!byClass.containsKey(internalName)) continue;

            if (!inst.isModifiableClass(clazz)) {
                Log.error("Patcher", "Not modifiable, skipping: " + clazz.getName());
                continue;
            }
            try {
                inst.retransformClasses(clazz);
                Log.info("Patcher", "Patched (already loaded): " + clazz.getName());
                patched.add(internalName);
            } catch (UnmodifiableClassException e) {
                Log.error("Patcher", "Cannot retransform: " + clazz.getName(), e);
            }
        }

        return patched;
    }
}
