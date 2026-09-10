package toutouchien.itemsadderadditions.feature.action.builtin;

import dev.lone.itemsadder.api.CustomBlock;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.common.logging.Log;
import toutouchien.itemsadderadditions.common.namespace.CustomTagType;
import toutouchien.itemsadderadditions.common.namespace.NamespaceUtils;
import toutouchien.itemsadderadditions.common.utils.BlocksShape;
import toutouchien.itemsadderadditions.feature.action.ActionContext;
import toutouchien.itemsadderadditions.feature.action.ActionExecutor;
import toutouchien.itemsadderadditions.feature.action.annotation.Action;
import toutouchien.itemsadderadditions.plugin.ItemsAdderAdditions;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Loaded-chunk, incremental block replacement. See README for supported targets and limits. */
@NullMarked
@Action(key = "iaa_replace_near_blocks")
public final class ReplaceNearBlocksAction extends ActionExecutor {
    static final int MAX_RADIUS = 25;
    static final int MAX_CANDIDATES = 32_768;
    @Nullable Options options;

    @Override
    public boolean configure(@Nullable Object data, String namespacedID) {
        options = null;
        if (!super.configure(data, namespacedID)) return false;
        try {
            if (!(data instanceof ConfigurationSection section)) throw new IllegalArgumentException("config must be a section");
            String namespace = namespacedID.split(":", 2)[0];
            List<?> rawFrom = section.getList("from");
            if (rawFrom == null || rawFrom.isEmpty() || rawFrom.size() > 32)
                throw new IllegalArgumentException("from must contain 1..32 block IDs or block tags");
            List<String> from = new ArrayList<>();
            for (Object raw : rawFrom) {
                String id = NamespaceUtils.normalizeBlockIDOrTag(namespace, text(raw));
                if (!validSource(id)) throw new IllegalArgumentException("unknown block or non-block tag in from: " + id);
                if (!from.contains(id)) from.add(id);
            }
            String to = NamespaceUtils.normalizeBlockID(namespace, text(section.get("to")));
            Material material = NamespaceUtils.isValidNamespacedId(to) ? NamespaceUtils.vanillaMaterial(to) : null;
            if (material == null || !material.isBlock())
                throw new IllegalArgumentException("to must be one exact vanilla block ID; custom targets are unsupported: " + to);
            BlockData targetData = material.createBlockData();
            // No placement context is available for inventories, multipart or support-dependent targets.
            if (!material.isAir() && (!material.isOccluding() || material.hasGravity() || targetData.getAsString().contains("[")))
                throw new IllegalArgumentException("to must be air or a stateless, full solid block: " + to);
            BlocksShape shape = BlocksShape.valueOf(text(section.get("shape", "SPHERE")).toUpperCase(Locale.ROOT));
            if (shape != BlocksShape.CUBOID && shape != BlocksShape.RHOMBUS && shape != BlocksShape.SPHERE && shape != BlocksShape.CYLINDER)
                throw new IllegalArgumentException("shape must be CUBOID, RHOMBUS, SPHERE or CYLINDER");
            ConfigurationSection radius = section.getConfigurationSection("radius");
            if (radius == null) throw new IllegalArgumentException("missing radius section");
            int x = radius(radius, "x"), y = radius(radius, "y"), z = radius(radius, "z");
            if ((2L * x + 1) * (2L * y + 1) * (2L * z + 1) > MAX_CANDIDATES)
                throw new IllegalArgumentException("radius bounding box exceeds " + MAX_CANDIDATES + " candidate coordinates");
            options = new Options(List.copyOf(from), to, targetData, shape, x, y, z, BlockReplacementQueue.generation());
            return true;
        } catch (RuntimeException exception) {
            Log.warn("Actions", "iaa_replace_near_blocks on '{}': {}", namespacedID, exception.getMessage());
            return false;
        }
    }

    private static String text(@Nullable Object raw) {
        if (!(raw instanceof String value) || value.isBlank() || value.length() > 256)
            throw new IllegalArgumentException("block IDs, tags and shape must be nonempty strings of at most 256 characters");
        return value.trim();
    }

    private static int radius(ConfigurationSection section, String axis) {
        Object value = section.get(section.contains("blocks_from_center") ? "blocks_from_center" : axis, 5);
        if (!(value instanceof Integer radius) || radius < 0 || radius > MAX_RADIUS)
            throw new IllegalArgumentException("radius must use integers from 0 to " + MAX_RADIUS);
        return radius;
    }

    private static boolean validSource(String id) {
        String key = NamespaceUtils.stripTagPrefix(id);
        if (!NamespaceUtils.isValidNamespacedId(key)) return false;
        if (id.startsWith("#")) {
            if (NamespaceUtils.isCustomTagReference(id, CustomTagType.BLOCK)) return true;
            if (NamespaceUtils.isCustomTagReference(id)) return false;
            return key.startsWith("minecraft:") && Bukkit.getTag(Tag.REGISTRY_BLOCKS, NamespacedKey.fromString(key), Material.class) != null;
        }
        Material material = NamespaceUtils.vanillaMaterial(id);
        return material != null ? material.isBlock() : CustomBlock.getInstance(id) != null;
    }

    @Override
    protected void execute(ActionContext context) {
        if (options == null) return;
        ItemsAdderAdditions plugin = ItemsAdderAdditions.instance();
        BlockReplacementQueue.submit(plugin, plugin.antiGriefLib(), context.player(), context.runOn().getLocation(), options);
    }

    /** Also invalidates old delayed executors so they cannot restart after a reload. */
    public static void cancelPending() {
        BlockReplacementQueue.cancelAll();
    }

    record Options(List<String> from, String to, BlockData targetData, BlocksShape shape,
                   int radiusX, int radiusY, int radiusZ, long generation) {}
}
