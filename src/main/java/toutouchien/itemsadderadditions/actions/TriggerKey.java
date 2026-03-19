package toutouchien.itemsadderadditions.actions;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Compound key used by {@link toutouchien.itemsadderadditions.actions.loading.ActionBindings}
 * to store and retrieve action executors.
 *
 * <p>Some trigger types accept an <em>event argument</em> that further qualifies the
 * interaction (e.g. {@code interact} with argument {@code "right"} vs {@code "left"}).
 * When no argument applies, {@code argument} is {@code null}.
 *
 * <h3>Known argument values for interact triggers</h3>
 * <ul>
 *   <li>{@code "right"}       - right-click (not sneaking)</li>
 *   <li>{@code "left"}        - left-click  (not sneaking)</li>
 *   <li>{@code "right_shift"} - right-click while sneaking</li>
 *   <li>{@code "left_shift"}  - left-click  while sneaking</li>
 *   <li>{@code "entity"}      - right-click directly on an entity</li>
 * </ul>
 */
@NullMarked
public record TriggerKey(TriggerType type, @Nullable String argument) {
    /**
     * Key without an argument - used by every non-argumentized trigger.
     */
    public static TriggerKey of(TriggerType type) {
        return new TriggerKey(type, null);
    }

    /**
     * Key with a specific argument - used by argumentized triggers (e.g. interact).
     */
    public static TriggerKey of(TriggerType type, @Nullable String argument) {
        return new TriggerKey(type, argument);
    }

    /**
     * Whether this key has an argument qualifier.
     */
    public boolean hasArgument() {
        return argument != null;
    }
}
