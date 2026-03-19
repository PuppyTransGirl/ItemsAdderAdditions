package toutouchien.itemsadderadditions.actions.annotations;

import toutouchien.itemsadderadditions.actions.TriggerType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Action {
    /**
     * Config key used to identify this action (e.g. "title", "play_animation").
     */
    String key();

    /**
     * Which trigger types this action may fire on.
     * An empty array means the action is allowed on ALL trigger types.
     */
    TriggerType[] triggers() default {};
}
