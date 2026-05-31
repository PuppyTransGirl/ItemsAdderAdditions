package toutouchien.itemsadderadditions.nms.api.component;

import java.util.List;
import java.util.Map;

/**
 * Immutable tree model for a component's YAML-sourced value.
 * Passed from Core (where Bukkit config is parsed) to NMS modules (where codec application happens).
 * All NMS types are excluded from this class.
 */
public sealed interface ComponentValue {
    record ObjectNode(Map<String, ComponentValue> entries) implements ComponentValue {}

    record ListNode(List<ComponentValue> values) implements ComponentValue {}

    record StringNode(String value) implements ComponentValue {}

    record BooleanNode(boolean value) implements ComponentValue {}

    record IntNode(int value) implements ComponentValue {}

    record LongNode(long value) implements ComponentValue {}

    record DoubleNode(double value) implements ComponentValue {}

    record NullNode() implements ComponentValue {}
}
