package toutouchien.itemsadderadditions.feature.behaviour.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a {@link toutouchien.itemsadderadditions.feature.behaviour.BehaviourExecutor}
 * subclass and declares its YAML config key.
 *
 * <pre>{@code
 * @Behaviour(key = "painting")
 * public final class PaintingBehaviour extends BehaviourExecutor { ... }
 * }</pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Behaviour {
    /**
     * Config key used to reference this behaviour in item YAML
     * (e.g. {@code "painting"}).
     */
    String key();
}
