package toutouchien.itemsadderadditions.integration.worldguard;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.feature.action.ActionContext;
import toutouchien.itemsadderadditions.integration.hook.worldguard.WorldGuardProtection;
import toutouchien.itemsadderadditions.integration.hook.worldguard.WorldGuardSettings;
import toutouchien.itemsadderadditions.plugin.ItemsAdderAdditions;

@NullMarked
public final class WorldGuardProtectionChecks {
    private WorldGuardProtectionChecks() {
    }

    public static boolean canOpenStorage(Player player, Location location) {
        if (!WorldGuardProtection.isAvailable()) return true;
        return WorldGuardProtection.isStorageOpenAllowed(player, location, settings());
    }

    public static boolean canApplyContactDamage(Player player, Location location) {
        if (!WorldGuardProtection.isAvailable()) return true;
        return WorldGuardProtection.isContactDamageAllowed(player, location, settings());
    }

    public static boolean canPlaceStackable(Player player, Location location) {
        if (!WorldGuardProtection.isAvailable()) return true;
        return WorldGuardProtection.isStackablePlaceAllowed(player, location, settings());
    }

    public static boolean canUseBed(Player player, Location location) {
        if (!WorldGuardProtection.isAvailable()) return true;
        return WorldGuardProtection.isBedUseAllowed(player, location, settings());
    }

    public static boolean canPlaceCustomPainting(Player player, Location location) {
        if (!WorldGuardProtection.isAvailable()) return true;
        return WorldGuardProtection.isCustomPaintingPlaceAllowed(player, location, settings());
    }

    public static boolean canRunAction(String actionKey, ActionContext context) {
        if (!WorldGuardProtection.isAvailable()) return true;
        Location location = actionLocation(context);
        return WorldGuardProtection.isActionAllowed(actionKey, context.player(), location, settings());
    }

    private static Location actionLocation(ActionContext context) {
        Entity runOn = context.runOn();
        if (runOn != context.player()) return runOn.getLocation();

        Block block = context.block();
        if (block != null) return block.getLocation();

        Entity target = context.target();
        if (target != null) return target.getLocation();

        Entity furniture = context.complexFurniture();
        if (furniture != null) return furniture.getLocation();

        return context.player().getLocation();
    }

    private static WorldGuardSettings settings() {
        try {
            return ItemsAdderAdditions.instance().settings().worldGuard();
        } catch (IllegalStateException | NullPointerException ignored) {
            return WorldGuardSettings.defaults();
        }
    }
}
