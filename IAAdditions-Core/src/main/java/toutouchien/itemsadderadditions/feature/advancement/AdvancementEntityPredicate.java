package toutouchien.itemsadderadditions.feature.advancement;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

import static toutouchien.itemsadderadditions.feature.advancement.AdvancementPredicateSupport.*;

record EntityPredicate(
        List<String> types,
        @Nullable DistancePredicate distance,
        Map<String, EffectPredicate> effects,
        Map<String, ItemPredicate> equipment,
        Flags flags,
        @Nullable LocationPredicate location,
        @Nullable String nbt,
        @Nullable EntityPredicate passenger,
        @Nullable SlotsPredicate slots,
        @Nullable LocationPredicate steppingOn,
        @Nullable LocationPredicate movementAffectedBy,
        @Nullable String team,
        @Nullable EntityPredicate targetedEntity,
        @Nullable EntityPredicate vehicle,
        @Nullable MovementPredicate movement,
        @Nullable Integer periodicTick,
        @Nullable TypeSpecificPredicate typeSpecific,
        boolean unsupportedComponents,
        boolean unsupportedPredicates
) {
    public static EntityPredicate parse(String namespace, @Nullable Object raw) {
        if (raw == null) return any();

        Object typeSpecificRaw = section(raw, "type_specific");
        TypeSpecificPredicate typeSpecific = TypeSpecificPredicate.parse(namespace, typeSpecificRaw, raw);

        return new EntityPredicate(
                readStringList(raw, "type").stream()
                        .map(AdvancementPredicateSupport::normalizeMinecraftIdOrTag)
                        .toList(),
                DistancePredicate.parse(section(raw, "distance")),
                parseEffects(section(raw, "effects")),
                parseEquipment(namespace, section(raw, "equipment")),
                Flags.parse(section(raw, "flags")),
                LocationPredicate.parse(namespace, section(raw, "location")),
                emptyToNull(string(value(raw, "nbt"))),
                parseNullableEntityPredicate(namespace, section(raw, "passenger")),
                SlotsPredicate.parse(namespace, section(raw, "slots")),
                LocationPredicate.parse(namespace, section(raw, "stepping_on")),
                LocationPredicate.parse(namespace, section(raw, "movement_affected_by")),
                emptyToNull(string(value(raw, "team"))),
                parseNullableEntityPredicate(namespace, section(raw, "targeted_entity")),
                parseNullableEntityPredicate(namespace, section(raw, "vehicle")),
                MovementPredicate.parse(section(raw, "movement")),
                intObject(value(raw, "periodic_tick")),
                typeSpecific,
                section(raw, "components") != null,
                section(raw, "predicates") != null
        );
    }

    public static EntityPredicate any() {
        return new EntityPredicate(
                List.of(), null, Map.of(), Map.of(), Flags.ANY, null, null,
                null, null, null, null, null, null, null, null, null, null,
                false, false
        );
    }

    public boolean matches(Entity entity, Location origin, Player contextPlayer) {
        if (!matchesType(entity)) return false;
        if (distance != null && !distance.matches(origin, entity.getLocation())) return false;
        if (!matchesEffects(entity)) return false;
        if (!matchesEquipment(entity)) return false;
        if (!flags.matches(entity)) return false;
        if (location != null && !location.matches(entity.getLocation())) return false;
        if (nbt != null && !matchesLimitedNbt(entity, nbt)) return false;
        if (passenger != null && entity.getPassengers().stream().noneMatch(passengerEntity -> passenger.matches(passengerEntity, origin, contextPlayer)))
            return false;
        if (slots != null) {
            if (!(entity instanceof Player player) || !slots.matches(player)) return false;
        }
        if (steppingOn != null && !steppingOn.matches(blockRelativeLocation(entity.getLocation(), 0.01D))) return false;
        if (movementAffectedBy != null && !movementAffectedBy.matches(blockRelativeLocation(entity.getLocation(), 0.5D)))
            return false;
        if (team != null && !team.equals(entityTeam(entity))) return false;
        if (targetedEntity != null) {
            if (!(entity instanceof Mob mob) || mob.getTarget() == null || !targetedEntity.matches(mob.getTarget(), origin, contextPlayer))
                return false;
        }
        if (vehicle != null) {
            Entity actualVehicle = entity.getVehicle();
            if (actualVehicle == null || !vehicle.matches(actualVehicle, origin, contextPlayer)) return false;
        }
        if (movement != null && !movement.matches(entity)) return false;
        if (periodicTick != null && periodicTick > 0 && entity.getTicksLived() % periodicTick != 0) return false;
        if (typeSpecific != null && !typeSpecific.matches(entity, origin, contextPlayer)) return false;
        if (unsupportedComponents || unsupportedPredicates) return false;
        return true;
    }

    private boolean matchesType(Entity entity) {
        if (types.isEmpty()) return true;
        String actual = entity.getType().getKey().toString();
        for (String type : types) {
            if (type.startsWith("#")) continue; // Bukkit does not expose entity-type tag membership reliably here.
            if (type.equals(actual)) return true;
        }
        return false;
    }

    private boolean matchesEffects(Entity entity) {
        if (effects.isEmpty()) return true;
        if (!(entity instanceof LivingEntity living)) return false;

        for (Map.Entry<String, EffectPredicate> entry : effects.entrySet()) {
            PotionEffect effect = null;
            for (PotionEffect active : living.getActivePotionEffects()) {
                if (active.getType().getKey().toString().equals(entry.getKey())) {
                    effect = active;
                    break;
                }
            }
            if (effect == null || !entry.getValue().matches(effect)) return false;
        }
        return true;
    }

    private boolean matchesEquipment(Entity entity) {
        if (equipment.isEmpty()) return true;
        if (!(entity instanceof LivingEntity living)) return false;

        EntityEquipment entityEquipment = living.getEquipment();
        if (entityEquipment == null) return false;

        for (Map.Entry<String, ItemPredicate> entry : equipment.entrySet()) {
            ItemStack stack = switch (entry.getKey()) {
                case "mainhand" -> entityEquipment.getItemInMainHand();
                case "offhand" -> entityEquipment.getItemInOffHand();
                case "head" -> entityEquipment.getHelmet();
                case "chest" -> entityEquipment.getChestplate();
                case "legs" -> entityEquipment.getLeggings();
                case "feet" -> entityEquipment.getBoots();
                case "body" -> itemFromMethod(entityEquipment, "getBody");
                default -> null;
            };
            if (!entry.getValue().matches(stack)) return false;
        }
        return true;
    }
}
