package toutouchien.itemsadderadditions.utils.other;

/**
 * Implemented by any executor type that identifies itself with a YAML config key.
 *
 * <p>Having this common interface lets {@link ExecutorRegistry} be generic over
 * any executor type without knowing about the concrete annotation class.
 */
public interface Keyed {
    /**
     * The YAML key used to reference this executor in item configs
     * (e.g. {@code "title"}, {@code "contact_damage"}).
     *
     * <p>Implementations read this from their class-level annotation
     * and throw {@link IllegalStateException} if the annotation is absent.
     */
    String key();
}
