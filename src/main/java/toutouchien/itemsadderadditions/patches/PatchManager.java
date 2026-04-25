package toutouchien.itemsadderadditions.patches;

import net.bytebuddy.agent.ByteBuddyAgent;
import toutouchien.itemsadderadditions.patches.impl.*;
import toutouchien.itemsadderadditions.utils.other.Log;

import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class PatchManager {
    private static final List<ClassPatch> PATCHES = List.of(
            new AddEnchantmentPatch(),
            new CooldownCapturePatch(),
            new JqCapturePatch(),
            new StatRequirementsCapturePatch(),
            new StonecutterSelectiveBypassPatch()
    );

    public static void applyAll() {
        Instrumentation inst = null;

        try {
            inst = ByteBuddyAgent.install();
        } catch (Exception ignored) {

        }

        if (inst == null) {
            try {
                inst = ByteBuddyAgent.install(
                        ByteBuddyAgent.AttachmentProvider.DEFAULT
                );
            } catch (Exception e) {
                Log.error("Patcher", "All attach methods failed", e);
                return;
            }
        }

        Map<String, List<ClassPatch>> byClass = PATCHES.stream()
                .collect(Collectors.groupingBy(ClassPatch::targetClass));

        // Register transformer for classes not yet loaded
        inst.addTransformer(new PatchTransformer(PATCHES), true);

        // Retransform classes already loaded in ANY classloader
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

        // Log which targets are still deferred
        for (String internalName : byClass.keySet()) {
            if (!patched.contains(internalName)) {
                Log.info("Patcher",
                        "Deferred (not loaded yet): " + internalName.replace('/', '.'));
            }
        }
    }
}
