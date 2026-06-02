package toutouchien.itemsadderadditions.feature.component.parse;

import org.bukkit.configuration.ConfigurationSection;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.nms.api.component.ComponentValue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Converts Bukkit config values (from ConfigurationSection.get()) into the
 * project-owned ComponentValue tree, which is passed to NMS modules for codec application.
 * No NMS classes are used here.
 */
@NullMarked
public final class ComponentTreeParser {
    private ComponentTreeParser() {
    }

    public static ComponentValue parse(@Nullable Object configValue) {
        switch (configValue) {
            case null -> new ComponentValue.NullNode();
            case Boolean b -> new ComponentValue.BooleanNode(b);
            case Integer i -> new ComponentValue.IntNode(i);
            case Long l -> new ComponentValue.LongNode(l);
            case Float f -> new ComponentValue.DoubleNode(f.doubleValue());
            case Double d -> new ComponentValue.DoubleNode(d);

            // Covers Byte, Short, etc.
            case Number n -> new ComponentValue.IntNode(n.intValue());

            case String s -> new ComponentValue.StringNode(s);

            case List<?> list -> {
                List<ComponentValue> values = new ArrayList<>(list.size());
                for (Object entry : list) {
                    values.add(parse(entry));
                }
                return new ComponentValue.ListNode(List.copyOf(values));
            }

            case ConfigurationSection section -> {
                Map<String, ComponentValue> entries = new LinkedHashMap<>();
                for (String key : section.getKeys(false)) {
                    entries.put(key, parse(section.get(key)));
                }
                return new ComponentValue.ObjectNode(Map.copyOf(entries));
            }

            // Fallback: stringify anything else (Map from SnakeYAML, etc.)
            case Map<?, ?> map -> {
                Map<String, ComponentValue> entries = new LinkedHashMap<>();
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    entries.put(String.valueOf(entry.getKey()), parse(entry.getValue()));
                }
                return new ComponentValue.ObjectNode(Map.copyOf(entries));
            }

            default -> {
            }
        }
        return new ComponentValue.StringNode(String.valueOf(configValue));
    }
}
