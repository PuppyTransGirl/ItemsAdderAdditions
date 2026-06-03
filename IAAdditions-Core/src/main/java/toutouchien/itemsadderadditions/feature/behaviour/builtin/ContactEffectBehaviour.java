package toutouchien.itemsadderadditions.feature.behaviour.builtin;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.kyori.adventure.key.Key;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.common.annotation.Parameter;
import toutouchien.itemsadderadditions.common.item.ItemCategory;
import toutouchien.itemsadderadditions.common.logging.Log;
import toutouchien.itemsadderadditions.common.utils.Task;
import toutouchien.itemsadderadditions.feature.behaviour.BehaviourExecutor;
import toutouchien.itemsadderadditions.feature.behaviour.BehaviourHost;
import toutouchien.itemsadderadditions.feature.behaviour.annotation.Behaviour;
import toutouchien.itemsadderadditions.feature.behaviour.builtin.contact.ContactDetector;
import toutouchien.itemsadderadditions.plugin.ItemsAdderAdditions;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Applies effects to players while they touch / stand on a custom block, furniture or
 * complex furniture.
 *
 * <p>Supports four independent effect groups, all optional:
 * <ul>
 *   <li>{@code damage} - hurts (or, if the amount is negative, heals) the player on an interval.</li>
 *   <li>{@code heal} - heals (or, if the amount is negative, hurts) the player on an interval.</li>
 *   <li>{@code potion_effects} - a list of potion effects re-applied on per-effect intervals.</li>
 *   <li>{@code attributes} - a list of temporary attribute modifiers applied while in contact.</li>
 * </ul>
 *
 * <p>The scheduler ticks at the lowest interval needed by any interval-based effect.
 * If any attribute is configured, contact is checked every tick so modifiers are removed
 * promptly once the player leaves the block.
 */
@SuppressWarnings("unused")
@NullMarked
@Behaviour(key = "contact_effect")
public final class ContactEffectBehaviour extends BehaviourExecutor implements Listener {
    private static final String SUBSYSTEM = "Behaviours";
    private static final DamageSource HEAL_DAMAGE_SOURCE = DamageSource.builder(DamageType.CACTUS).build();
    private static final @Nullable Attribute MAX_HEALTH = Registry.ATTRIBUTE.get(Key.key("minecraft:max_health"));

    private final EnumSet<BlockFace> activeFaces = EnumSet.noneOf(BlockFace.class);
    private final List<TimedPotion> potions = new ArrayList<>();
    private final List<AttrMod> attributeMods = new ArrayList<>();

    // Per-player effect cooldowns (keyed by current server tick) and contact tracking.
    private final Map<UUID, Integer> lastDamageTick = new HashMap<>();
    private final Map<UUID, Integer> lastHealTick = new HashMap<>();
    private final Map<UUID, int[]> potionLastTick = new HashMap<>();
    private final Set<UUID> contacting = new HashSet<>();

    // Damage group
    @Parameter(key = "amount", path = "damage", type = Double.class) private @Nullable Double damageAmount;
    @Parameter(key = "interval", path = "damage", type = Integer.class, min = 1, max = 200) private Integer damageInterval = 20;
    @Parameter(key = "cause", path = "damage", type = String.class) private @Nullable String damageCause;
    @Parameter(key = "fire_duration", path = "damage", type = Integer.class, min = 0, max = 200) private Integer fireDuration = 0;

    // Heal group
    @Parameter(key = "amount", path = "heal", type = Double.class) private @Nullable Double healAmount;
    @Parameter(key = "interval", path = "heal", type = Integer.class, min = 1, max = 200) private Integer healInterval = 20;

    @Parameter(key = "apply_when_sneaking", type = Boolean.class) private Boolean applyWhenSneaking = true;

    // Contact detection - which block faces count as "touching".
    @Parameter(key = "top", path = "block_faces", type = Boolean.class) private boolean topFaceActive = true;
    @Parameter(key = "north", path = "block_faces", type = Boolean.class) private boolean northFaceActive = true;
    @Parameter(key = "south", path = "block_faces", type = Boolean.class) private boolean southFaceActive = true;
    @Parameter(key = "west", path = "block_faces", type = Boolean.class) private boolean westFaceActive = true;
    @Parameter(key = "east", path = "block_faces", type = Boolean.class) private boolean eastFaceActive = true;

    private boolean damageEnabled;
    private boolean healEnabled;
    private DamageSource damageSource = DamageSource.builder(DamageType.CACTUS).build();
    private int contactCheckInterval = 1;

    private @Nullable ScheduledTask task;
    private @Nullable ContactDetector detector;
    private @Nullable ItemCategory category;

    @Override
    public boolean configure(Object configData, String namespacedID) {
        if (!(configData instanceof ConfigurationSection section))
            return false;

        if (!super.configure(configData, namespacedID))
            return false;

        if (topFaceActive) activeFaces.add(BlockFace.UP);
        if (northFaceActive) activeFaces.add(BlockFace.NORTH);
        if (southFaceActive) activeFaces.add(BlockFace.SOUTH);
        if (westFaceActive) activeFaces.add(BlockFace.WEST);
        if (eastFaceActive) activeFaces.add(BlockFace.EAST);

        damageEnabled = damageAmount != null && damageAmount != 0.0;
        healEnabled = healAmount != null && healAmount != 0.0;
        damageSource = resolveDamageSource(damageCause, namespacedID);

        parsePotions(section, namespacedID);
        parseAttributes(section, namespacedID);

        if (!damageEnabled && !healEnabled && potions.isEmpty() && attributeMods.isEmpty()) {
            Log.itemSkip(SUBSYSTEM, namespacedID,
                    "'contact_effect' has no valid effects (damage, heal, potion_effects or attributes)");
            return false;
        }

        contactCheckInterval = computeContactInterval();
        return true;
    }

    @Override
    protected void onLoad(BehaviourHost host) {
        this.category = host.category();
        this.detector = new ContactDetector(host.namespacedID(), activeFaces, topFaceActive);
        Bukkit.getPluginManager().registerEvents(this, host.plugin());
        long periodMs = (long) contactCheckInterval * 50L;
        task = Task.syncRepeat(t -> tick(), host.plugin(), periodMs, periodMs, TimeUnit.MILLISECONDS);
    }

    @Override
    protected void onUnload(BehaviourHost host) {
        if (task != null) {
            task.cancel();
            task = null;
        }

        if (!attributeMods.isEmpty()) {
            for (UUID id : contacting) {
                Player player = Bukkit.getPlayer(id);
                if (player != null) removeModifiers(player);
            }
        }

        HandlerList.unregisterAll(this);
        detector = null;
        category = null;
        contacting.clear();
        lastDamageTick.clear();
        lastHealTick.clear();
        potionLastTick.clear();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        boolean wasContacting = contacting.remove(id);
        if (wasContacting && !attributeMods.isEmpty()) removeModifiers(event.getPlayer());
        lastDamageTick.remove(id);
        lastHealTick.remove(id);
        potionLastTick.remove(id);
    }

    private void tick() {
        if (detector == null || category == null)
            return;

        int now = Bukkit.getCurrentTick();
        Set<UUID> nowSet = new HashSet<>();

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!isTouching(player) || (player.isSneaking() && !applyWhenSneaking))
                continue;

            UUID id = player.getUniqueId();
            nowSet.add(id);
            boolean entering = !contacting.contains(id);

            if (!attributeMods.isEmpty() && entering)
                applyModifiers(player);

            if (damageEnabled && due(lastDamageTick, id, now, damageInterval))
                applyDamage(player);

            if (healEnabled && due(lastHealTick, id, now, healInterval))
                applyHeal(player, healAmount);

            applyPotions(player, id, now);
        }

        // Handle players who left contact since last tick.
        for (UUID id : contacting) {
            if (nowSet.contains(id))
                continue;

            Player player = Bukkit.getPlayer(id);
            if (player == null)
                continue;

            if (!attributeMods.isEmpty())
                removeModifiers(player);

            if (damageEnabled && damageInterval < 20)
                player.setMaximumNoDamageTicks(20);
        }

        contacting.clear();
        contacting.addAll(nowSet);
        lastDamageTick.keySet().retainAll(nowSet);
        lastHealTick.keySet().retainAll(nowSet);
        potionLastTick.keySet().retainAll(nowSet);
    }

    private void applyDamage(Player player) {
        double amount = damageAmount == null ? 0.0 : damageAmount;
        if (amount > 0.0) {
            if (damageInterval < 20)
                player.setMaximumNoDamageTicks(damageInterval);

            player.damage(amount, damageSource);

            if (fireDuration > 0)
                player.setFireTicks(fireDuration);
        } else if (amount < 0.0) {
            heal(player, -amount);
        }
    }

    private void applyHeal(Player player, @Nullable Double configured) {
        double amount = configured == null ? 0.0 : configured;
        if (amount > 0.0)
            heal(player, amount);
        else if (amount < 0.0)
            player.damage(-amount, HEAL_DAMAGE_SOURCE);
    }

    private void heal(Player player, double amount) {
        if (player.isDead() || player.getHealth() <= 0.0)
            return;

        AttributeInstance maxAttr = MAX_HEALTH == null ? null : player.getAttribute(MAX_HEALTH);
        double max = maxAttr == null ? 20.0 : maxAttr.getValue();
        player.setHealth(Math.clamp(player.getHealth() + amount, 0.0, max));
    }

    private void applyPotions(Player player, UUID id, int now) {
        if (potions.isEmpty())
            return;

        int[] last = potionLastTick.computeIfAbsent(id, k -> {
            int[] arr = new int[potions.size()];
            Arrays.fill(arr, Integer.MIN_VALUE);
            return arr;
        });

        for (int i = 0; i < potions.size(); i++) {
            TimedPotion timed = potions.get(i);
            if (last[i] != Integer.MIN_VALUE && now - last[i] < timed.interval())
                continue;

            player.addPotionEffect(timed.effect());
            last[i] = now;
        }
    }

    private void applyModifiers(Player player) {
        for (AttrMod mod : attributeMods) {
            AttributeInstance instance = player.getAttribute(mod.attribute());
            if (instance == null)
                continue;

            // Remove any stale copy first so we never stack duplicates.
            try {
                instance.removeModifier(mod.modifier().getKey());
            } catch (RuntimeException ignored) {
                // No existing modifier with this key - fine.
            }
            instance.addModifier(mod.modifier());
        }
    }

    private void removeModifiers(Player player) {
        for (AttrMod mod : attributeMods) {
            AttributeInstance instance = player.getAttribute(mod.attribute());
            if (instance == null)
                continue;

            try {
                instance.removeModifier(mod.modifier().getKey());
            } catch (RuntimeException ignored) {
                // Already absent.
            }
        }
    }

    private boolean due(Map<UUID, Integer> map, UUID id, int now, int interval) {
        Integer last = map.get(id);
        if (last != null && now - last < interval)
            return false;

        map.put(id, now);
        return true;
    }

    private int computeContactInterval() {
        if (!attributeMods.isEmpty())
            return 1; // attributes must be removed the tick the player leaves

        int min = Integer.MAX_VALUE;
        if (damageEnabled) min = Math.min(min, damageInterval);
        if (healEnabled) min = Math.min(min, healInterval);
        for (TimedPotion potion : potions) min = Math.min(min, potion.interval());

        return min == Integer.MAX_VALUE ? 1 : min;
    }

    private DamageSource resolveDamageSource(@Nullable String name, String itemId) {
        if (name == null || name.isBlank())
            return DamageSource.builder(DamageType.CACTUS).build();

        String key = name.trim().toLowerCase(Locale.ROOT);
        if (!key.contains(":")) key = "minecraft:" + key;

        DamageType type = Registry.DAMAGE_TYPE.get(Key.key(key));
        if (type == null) {
            Log.itemWarn(SUBSYSTEM, itemId,
                    "'contact_effect.damage.cause' value '{}' is not a known damage type - using cactus", name);
            return DamageSource.builder(DamageType.CACTUS).build();
        }

        return DamageSource.builder(type).build();
    }

    private void parsePotions(ConfigurationSection section, String itemId) {
        for (Map<?, ?> raw : section.getMapList("potion_effects")) {
            Object typeObj = raw.get("type");
            if (typeObj == null) {
                Log.itemWarn(SUBSYSTEM, itemId, "'contact_effect' potion effect entry is missing 'type' - skipping");
                continue;
            }

            String type = typeObj.toString().trim().toLowerCase(Locale.ROOT);
            PotionEffectType effectType = Registry.POTION_EFFECT_TYPE.get(Key.key("minecraft", type));
            if (effectType == null) {
                Log.itemWarn(SUBSYSTEM, itemId, "'contact_effect' potion effect type '{}' is unknown - skipping", typeObj);
                continue;
            }

            int duration = intValue(raw.get("duration"), 40);
            int amplifier = intValue(raw.get("amplifier"), 0);
            boolean ambient = boolValue(raw.get("ambient"), false);
            boolean particles = boolValue(raw.get("particles"), true);
            boolean icon = boolValue(raw.get("icon"), true);
            int interval = Math.max(1, intValue(raw.get("interval"), 20));

            potions.add(new TimedPotion(
                    new PotionEffect(effectType, duration, amplifier, ambient, particles, icon),
                    interval
            ));
        }
    }

    private void parseAttributes(ConfigurationSection section, String itemId) {
        for (Map<?, ?> raw : section.getMapList("attributes")) {
            Object attrObj = raw.get("attribute");
            if (attrObj == null) {
                Log.itemWarn(SUBSYSTEM, itemId, "'contact_effect' attribute entry is missing 'attribute' - skipping");
                continue;
            }

            Attribute attribute = resolveAttribute(attrObj.toString());
            if (attribute == null) {
                Log.itemWarn(SUBSYSTEM, itemId, "'contact_effect' attribute '{}' is unknown - skipping", attrObj);
                continue;
            }

            Object amountObj = raw.get("amount");
            if (!(amountObj instanceof Number amountNum)) {
                Log.itemWarn(SUBSYSTEM, itemId, "'contact_effect' attribute '{}' is missing a numeric 'amount' - skipping", attrObj);
                continue;
            }

            AttributeModifier.Operation operation = AttributeModifier.Operation.ADD_NUMBER;
            Object opObj = raw.get("operation");
            if (opObj != null) {
                try {
                    operation = AttributeModifier.Operation.valueOf(opObj.toString().trim().toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException e) {
                    Log.itemWarn(SUBSYSTEM, itemId,
                            "'contact_effect' attribute '{}' has unknown operation '{}' - using ADD_NUMBER", attrObj, opObj);
                }
            }

            NamespacedKey key = new NamespacedKey(
                    ItemsAdderAdditions.instance(),
                    "contact_effect." + itemId.replace(':', '.') + "." + attribute.getKey().value()
            );
            attributeMods.add(new AttrMod(attribute, new AttributeModifier(key, amountNum.doubleValue(), operation)));
        }
    }

    @Nullable
    private Attribute resolveAttribute(String raw) {
        String norm = raw.trim().toLowerCase(Locale.ROOT);
        // Modern attribute registry keys dropped the old category prefixes.
        if (norm.startsWith("generic_")) norm = norm.substring("generic_".length());
        else if (norm.startsWith("player_")) norm = norm.substring("player_".length());
        else if (norm.startsWith("horse_")) norm = norm.substring("horse_".length());
        else if (norm.startsWith("zombie_")) norm = norm.substring("zombie_".length());

        if (!norm.contains(":")) norm = "minecraft:" + norm;
        return Registry.ATTRIBUTE.get(Key.key(norm));
    }

    private boolean isTouching(Player player) {
        if (detector == null || category == null)
            return false;

        return switch (category) {
            case BLOCK, ITEM, FURNITURE -> detector.isTouchingBlock(player);
            case COMPLEX_FURNITURE -> detector.isTouchingComplexFurniture(player);
        };
    }

    private static int intValue(@Nullable Object value, int fallback) {
        return value instanceof Number number ? number.intValue() : fallback;
    }

    private static boolean boolValue(@Nullable Object value, boolean fallback) {
        return value instanceof Boolean bool ? bool : fallback;
    }

    private record TimedPotion(PotionEffect effect, int interval) {
    }

    private record AttrMod(Attribute attribute, AttributeModifier modifier) {
    }
}
