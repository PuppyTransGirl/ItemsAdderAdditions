package toutouchien.itemsadderadditions.feature.action.loading;

import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.feature.action.TriggerType;

/**
 * Mapping from an ItemsAdder YAML trigger key to the runtime trigger type.
 */
@NullMarked
record TriggerDefinition(TriggerType type, boolean argumentized) {}
