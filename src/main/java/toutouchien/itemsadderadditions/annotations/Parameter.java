package toutouchien.itemsadderadditions.annotations;

import toutouchien.itemsadderadditions.utils.other.ConfigUtils;
import toutouchien.itemsadderadditions.utils.other.ParameterInjector;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field as a YAML-injected parameter.
 *
 * <p>When present on a field inside an {@link toutouchien.itemsadderadditions.actions.ActionExecutor},
 * {@link toutouchien.itemsadderadditions.behaviours.BehaviourExecutor}, or
 * {@link toutouchien.itemsadderadditions.components.ComponentProperty},
 * {@link ParameterInjector} will read the
 * corresponding YAML key and inject the value into the field.
 *
 * <h3>Nested keys via {@code path}</h3>
 * Set {@link #path} to read from a sub-section without having to manually call
 * {@link org.bukkit.configuration.ConfigurationSection#getConfigurationSection}.
 * The path is resolved relative to the injector's root section using dot notation.
 *
 * <pre>
 * // Reads section.block_faces.top
 * {@literal @}Parameter(key = "top", path = "block_faces", type = Boolean.class)
 * private boolean topFaceActive = true;
 *
 * // Reads section.sound.name
 * {@literal @}Parameter(key = "name", path = "sound", type = String.class)
 * private String soundName;
 * </pre>
 *
 * <h3>Min / max constraints</h3>
 * {@link #min} and {@link #max} are only enforced when they differ from each other
 * (i.e. when {@code min != max}).  Leaving both at their default value of {@code 0.0}
 * disables range checking - that is the "no constraint" sentinel.
 *
 * <p>To apply a one-sided constraint, use {@link Double#NEGATIVE_INFINITY} or
 * {@link Double#POSITIVE_INFINITY} for the unconstrained end.
 *
 * <p>Examples:
 * <pre>
 * // Must be between 1 and 1024 (inclusive)
 * {@literal @}Parameter(key = "max_blocks", type = Integer.class, required = true, min = 1, max = 1024)
 * private int maxBlocks;
 *
 * // No range check (default)
 * {@literal @}Parameter(key = "text", type = String.class, required = true)
 * private String text;
 *
 * // Only a lower bound
 * {@literal @}Parameter(key = "damage", type = Double.class, min = 0.0, max = Double.POSITIVE_INFINITY)
 * private double damage = 1.0;
 * </pre>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Parameter {
    /** Key in the YAML config section (or sub-section if {@link #path} is set). */
    String key();

    /**
     * Optional dot-separated path to a sub-section to read {@link #key} from.
     * Empty string (default) means read directly from the root section.
     *
     * <p>Example: {@code path = "sound"} with {@code key = "name"} reads
     * {@code section.sound.name}.
     */
    String path() default "";

    /**
     * Expected Java type. Number types are auto-converted by {@link ConfigUtils}.
     */
    Class<?> type();

    /**
     * If {@code true} and the value is missing or the wrong type,
     * the entire action / behaviour is rejected and skipped.
     */
    boolean required() default false;

    /**
     * Inclusive lower bound for numeric parameters.
     * Ignored when {@code min == max} (the "no constraint" sentinel).
     */
    double min() default 0.0;

    /**
     * Inclusive upper bound for numeric parameters.
     * Ignored when {@code min == max} (the "no constraint" sentinel).
     */
    double max() default 0.0;
}
