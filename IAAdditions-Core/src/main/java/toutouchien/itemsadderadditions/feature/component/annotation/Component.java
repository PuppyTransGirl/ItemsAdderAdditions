package toutouchien.itemsadderadditions.feature.component.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a {@link toutouchien.itemsadderadditions.feature.component.ComponentExecutor}
 * subclass and declares its YAML config key.
 *
 * <pre>{@code
 * @Component(key = "rarity")
 * public final class RarityComponent extends ComponentExecutor { ... }
 * }</pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Component {
    /**
     * Config key used to reference this component in item YAML under {@code components:}
     * (e.g. {@code "rarity"}, {@code "use_cooldown"}).
     */
    String key();
}
