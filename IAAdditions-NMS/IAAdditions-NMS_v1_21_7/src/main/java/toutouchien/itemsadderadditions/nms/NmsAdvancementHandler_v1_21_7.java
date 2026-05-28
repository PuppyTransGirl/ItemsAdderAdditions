package toutouchien.itemsadderadditions.nms;

import io.papermc.paper.adventure.PaperAdventure;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minecraft.advancements.*;
import net.minecraft.advancements.critereon.ImpossibleTrigger;
import net.minecraft.core.ClientAsset;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ClientboundUpdateAdvancementsPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.ServerAdvancementManager;
import net.minecraft.world.item.crafting.Recipe;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.nms.api.AdvancementDisplaySpec;
import toutouchien.itemsadderadditions.nms.api.AdvancementSpec;
import toutouchien.itemsadderadditions.nms.api.INmsAdvancementHandler;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.*;

@NullMarked
public final class NmsAdvancementHandler_v1_21_7 implements INmsAdvancementHandler {
    private static final MethodHandle ADVANCEMENT_PROGRESS_CTOR;
    private static final MethodHandle ADVANCEMENTS_GETTER;
    private static final MethodHandle ADVANCEMENTS_SETTER;

    static {
        try {
            MethodHandles.Lookup apLookup = MethodHandles.privateLookupIn(
                    AdvancementProgress.class, MethodHandles.lookup()
            );
            ADVANCEMENT_PROGRESS_CTOR = apLookup.findConstructor(
                    AdvancementProgress.class,
                    MethodType.methodType(void.class, Map.class)
            );

            MethodHandles.Lookup samLookup = MethodHandles.privateLookupIn(
                    ServerAdvancementManager.class, MethodHandles.lookup()
            );
            ADVANCEMENTS_GETTER = samLookup.findGetter(
                    ServerAdvancementManager.class, "advancements", Map.class
            );
            ADVANCEMENTS_SETTER = samLookup.findSetter(
                    ServerAdvancementManager.class, "advancements", Map.class
            );
        } catch (Exception e) {
            throw new RuntimeException("NmsAdvancementHandler_v1_21_7 init failed", e);
        }
    }

    private static ResourceLocation toRL(NamespacedKey key) {
        return ResourceLocation.fromNamespaceAndPath(key.getNamespace(), key.getKey());
    }

    private static AdvancementHolder buildHolder(AdvancementSpec spec) {
        ResourceLocation rl = toRL(spec.key());
        Optional<ResourceLocation> parent = spec.parent() == null
                ? Optional.empty()
                : Optional.of(toRL(spec.parent()));

        DisplayInfo display = buildDisplay(spec.display());
        Map<String, Criterion<?>> criteria = buildCriteria(spec.criteriaNames());
        AdvancementRequirements requirements = buildRequirements(spec.criteriaNames());
        AdvancementRewards rewards = buildRewards(spec);

        Advancement advancement = new Advancement(
                parent,
                Optional.of(display),
                rewards,
                criteria,
                requirements,
                false,
                Optional.empty()
        );
        return new AdvancementHolder(rl, advancement);
    }

    private static DisplayInfo buildDisplay(AdvancementDisplaySpec spec) {
        net.minecraft.world.item.ItemStack icon = CraftItemStack.asNMSCopy(spec.icon());
        net.minecraft.network.chat.Component title =
                PaperAdventure.asVanilla(MiniMessage.miniMessage().deserialize(spec.title()));
        net.minecraft.network.chat.Component desc =
                PaperAdventure.asVanilla(MiniMessage.miniMessage().deserialize(spec.description()));

        AdvancementType type = AdvancementType.valueOf(spec.frame().toUpperCase(Locale.ROOT));
        Optional<ClientAsset> bg = buildBackground(spec.background());

        return new DisplayInfo(icon, title, desc, bg, type,
                spec.showToast(), spec.announceToChat(), spec.hidden());
    }

    private static Optional<ClientAsset> buildBackground(@Nullable String background) {
        if (background == null) return Optional.empty();
        try {
            return Optional.of(new ClientAsset(ResourceLocation.parse(background)));
        } catch (Exception e) {
            toutouchien.itemsadderadditions.common.logging.Log
                    .warn("NmsAdvancementHandler", "Invalid background path '{}', ignoring: {}", background, e.getMessage());
            return Optional.empty();
        }
    }

    private static Map<String, Criterion<?>> buildCriteria(List<String> names) {
        Map<String, Criterion<?>> map = new LinkedHashMap<>();
        for (String name : names) {
            map.put(name, CriteriaTriggers.IMPOSSIBLE.createCriterion(
                    new ImpossibleTrigger.TriggerInstance()
            ));
        }
        return map;
    }

    private static AdvancementRequirements buildRequirements(List<String> names) {
        List<List<String>> reqs = names.stream().map(List::of).toList();
        return new AdvancementRequirements(reqs);
    }

    private static AdvancementRewards buildRewards(AdvancementSpec spec) {
        if (spec.rewardExperience() == 0 && spec.rewardRecipes().isEmpty()) {
            return AdvancementRewards.EMPTY;
        }
        List<ResourceKey<Recipe<?>>> recipes = spec.rewardRecipes().stream()
                .map(k -> ResourceKey.create(Registries.RECIPE,
                        ResourceLocation.fromNamespaceAndPath(k.getNamespace(), k.getKey())))
                .toList();
        return new AdvancementRewards(spec.rewardExperience(), List.of(), recipes, Optional.empty());
    }

    private static AdvancementProgress newProgress(Map<String, CriterionProgress> criteria) {
        try {
            return (AdvancementProgress) ADVANCEMENT_PROGRESS_CTOR.invokeExact(criteria);
        } catch (Throwable e) {
            throw new RuntimeException("Cannot create AdvancementProgress", e);
        }
    }

    private static void grantCriteria(Player player, AdvancementHolder holder, List<String> names) {
        PlayerAdvancements pa = ((CraftPlayer) player).getHandle().getAdvancements();
        for (String name : names) {
            pa.award(holder, name);
        }
    }

    private static void arrangeAffectedRoots(ServerAdvancementManager sam, Collection<AdvancementHolder> holders) {
        Set<AdvancementNode> roots = Collections.newSetFromMap(new IdentityHashMap<>());
        for (AdvancementHolder holder : holders) {
            AdvancementNode node = sam.tree().get(holder.id());
            if (node == null) continue;

            AdvancementNode root = node;
            while (root.parent() != null) {
                root = root.parent();
            }
            roots.add(root);
        }

        for (AdvancementNode root : roots) {
            TreeNodePosition.run(root);
        }
    }

    private static Map<ResourceLocation, AdvancementProgress> buildInitialProgress(List<AdvancementHolder> holders) {
        Map<ResourceLocation, AdvancementProgress> progress = new HashMap<>();
        for (AdvancementHolder holder : holders) {
            Map<String, CriterionProgress> criteria = new HashMap<>();
            for (String name : holder.value().criteria().keySet()) {
                criteria.put(name, new CriterionProgress());
            }
            progress.put(holder.id(), newProgress(criteria));
        }
        return progress;
    }

    private static void broadcastUpdate(List<AdvancementHolder> added, Set<ResourceLocation> removed) {
        if (added.isEmpty() && removed.isEmpty()) return;

        ClientboundUpdateAdvancementsPacket packet = new ClientboundUpdateAdvancementsPacket(
                false, added, removed, buildInitialProgress(added), false
        );
        for (Player player : Bukkit.getOnlinePlayers()) {
            ((CraftPlayer) player).getHandle().connection.send(packet);
        }
    }

    @Override
    public void registerAll(List<AdvancementSpec> specs) {
        if (specs.isEmpty()) return;
        replaceAll(Collections.emptySet(), specs);
    }

    @Override
    public void unregisterAll(Collection<NamespacedKey> keys) {
        if (keys.isEmpty()) return;
        replaceAll(keys, Collections.emptyList());
    }

    @Override
    public void replaceAll(Collection<NamespacedKey> oldKeys, List<AdvancementSpec> specs) {
        if (oldKeys.isEmpty() && specs.isEmpty()) return;

        ServerAdvancementManager sam = MinecraftServer.getServer().getAdvancements();
        Map<ResourceLocation, AdvancementHolder> map = mutableMap(sam);

        Set<ResourceLocation> removed = new LinkedHashSet<>();
        for (NamespacedKey key : oldKeys) {
            ResourceLocation id = toRL(key);
            if (map.remove(id) != null) {
                removed.add(id);
            }
        }
        if (!removed.isEmpty()) {
            sam.tree().remove(removed);
        }

        List<AdvancementHolder> added = new ArrayList<>(specs.size());
        Map<NamespacedKey, AdvancementHolder> addedByKey = new HashMap<>(specs.size());
        for (AdvancementSpec spec : specs) {
            AdvancementHolder holder = buildHolder(spec);
            ResourceLocation id = toRL(spec.key());
            map.put(id, holder);
            added.add(holder);
            addedByKey.put(spec.key(), holder);
        }

        if (!added.isEmpty()) {
            sam.tree().addAll(added);
            arrangeAffectedRoots(sam, added);
        }

        broadcastUpdate(added, removed);

        List<AdvancementHolder> hiddenAdded = added.stream()
                .filter(h -> h.value().display().map(DisplayInfo::isHidden).orElse(false))
                .toList();
        if (!hiddenAdded.isEmpty()) {
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                PlayerAdvancements pa = ((CraftPlayer) onlinePlayer).getHandle().getAdvancements();
                Set<ResourceLocation> toHide = new LinkedHashSet<>();
                for (AdvancementHolder h : hiddenAdded) {
                    if (!pa.getOrStartProgress(h).isDone()) toHide.add(h.id());
                }
                if (toHide.isEmpty()) continue;
                ((CraftPlayer) onlinePlayer).getHandle().connection.send(
                        new ClientboundUpdateAdvancementsPacket(false, Collections.emptyList(), toHide, Collections.emptyMap(), false)
                );
            }
        }

        if (added.isEmpty()) return;

        Collection<? extends Player> players = Bukkit.getOnlinePlayers();
        for (AdvancementSpec spec : specs) {
            if (!spec.autoGrantRoot()) continue;
            AdvancementHolder holder = addedByKey.get(spec.key());
            if (holder == null) continue;
            for (Player player : players) {
                grantCriteria(player, holder, spec.criteriaNames());
            }
        }
    }

    @Override
    public boolean award(Player player, NamespacedKey key, String criterionName) {
        ServerAdvancementManager sam = MinecraftServer.getServer().getAdvancements();
        AdvancementHolder holder = sam.get(toRL(key));
        if (holder == null) return false;
        if (holder.value().display().map(DisplayInfo::isHidden).orElse(false)) {
            ((CraftPlayer) player).getHandle().connection.send(
                    new ClientboundUpdateAdvancementsPacket(false, List.of(holder), Collections.emptySet(), buildInitialProgress(List.of(holder)), false)
            );
        }
        PlayerAdvancements pa = ((CraftPlayer) player).getHandle().getAdvancements();
        return pa.award(holder, criterionName);
    }

    @Override
    public void onPlayerJoin(Player player, Collection<NamespacedKey> rootKeys) {
        ServerAdvancementManager sam = MinecraftServer.getServer().getAdvancements();
        for (NamespacedKey key : rootKeys) {
            AdvancementHolder holder = sam.get(toRL(key));
            if (holder == null) continue;
            List<String> names = new ArrayList<>(holder.value().criteria().keySet());
            grantCriteria(player, holder, names);
        }
    }

    @Override
    public void removeIncompleteHiddenAdvancements(Player player, Collection<NamespacedKey> hiddenKeys) {
        if (hiddenKeys.isEmpty()) return;
        ServerAdvancementManager sam = MinecraftServer.getServer().getAdvancements();
        PlayerAdvancements pa = ((CraftPlayer) player).getHandle().getAdvancements();
        Set<ResourceLocation> toRemove = new LinkedHashSet<>();
        for (NamespacedKey key : hiddenKeys) {
            ResourceLocation id = toRL(key);
            AdvancementHolder holder = sam.get(id);
            if (holder == null) continue;
            if (!pa.getOrStartProgress(holder).isDone()) toRemove.add(id);
        }
        if (toRemove.isEmpty()) return;
        ((CraftPlayer) player).getHandle().connection.send(
                new ClientboundUpdateAdvancementsPacket(false, Collections.emptyList(), toRemove, Collections.emptyMap(), false)
        );
    }

    @SuppressWarnings("unchecked")
    private Map<ResourceLocation, AdvancementHolder> mutableMap(ServerAdvancementManager sam) {
        try {
            Map<ResourceLocation, AdvancementHolder> current =
                    (Map<ResourceLocation, AdvancementHolder>) ADVANCEMENTS_GETTER.invoke(sam);
            if (current instanceof LinkedHashMap) return current;
            LinkedHashMap<ResourceLocation, AdvancementHolder> mutable = new LinkedHashMap<>(current);
            ADVANCEMENTS_SETTER.invoke(sam, mutable);
            return mutable;
        } catch (Throwable e) {
            throw new RuntimeException("Cannot access ServerAdvancementManager.advancements", e);
        }
    }
}
