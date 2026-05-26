package toutouchien.itemsadderadditions.feature.advancement;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static toutouchien.itemsadderadditions.feature.advancement.AdvancementPredicateSupport.unwrapPredicateCondition;

@NullMarked
public record AdvancementPlayerPredicate(List<EntityPredicate> predicates) {
    public static final AdvancementPlayerPredicate ANY = new AdvancementPlayerPredicate(List.of());

    public boolean matches(Player player) {
        if (predicates.isEmpty()) return true;
        Location origin = player.getLocation();
        for (EntityPredicate predicate : predicates) {
            if (!predicate.matches(player, origin, player)) return false;
        }
        return true;
    }

    public static AdvancementPlayerPredicate parse(String namespace, @Nullable ConfigurationSection sec) {
        return parse(namespace, (Object) sec);
    }

    public static AdvancementPlayerPredicate parse(String namespace, @Nullable Object raw) {
        if (raw == null) return ANY;

        Object normalized = unwrapPredicateCondition(raw);
        if (normalized instanceof List<?> list) {
            List<EntityPredicate> predicates = new ArrayList<>();
            for (Object entry : list) {
                Object predicateRaw = unwrapPredicateCondition(entry);
                if (predicateRaw != null) predicates.add(EntityPredicate.parse(namespace, predicateRaw));
            }
            return predicates.isEmpty() ? ANY : new AdvancementPlayerPredicate(List.copyOf(predicates));
        }

        return new AdvancementPlayerPredicate(List.of(EntityPredicate.parse(namespace, normalized)));
    }
}
