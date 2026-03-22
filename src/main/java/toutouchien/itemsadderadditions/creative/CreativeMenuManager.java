package toutouchien.itemsadderadditions.creative;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.lone.itemsadder.api.CustomStack;
import dev.lone.itemsadder.api.ItemsAdder;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.ItemsAdderAdditions;
import toutouchien.itemsadderadditions.utils.Log;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * Populates the vanilla creative menu with ItemsAdder custom items.
 *
 * <h2>Mechanism</h2>
 * We exploit the {@code minecraft:painting/variant} component: every variant
 * registered in the server's {@code PaintingVariant} registry appears as a
 * separate entry in the creative Decorations tab. By overriding
 * {@code assets/minecraft/items/painting.json} with a {@code minecraft:select}
 * model that maps each variant key to the item's IA-generated model, holding
 * the painting in-hand shows the correct icon.
 *
 * <h2>Model path strategy</h2>
 * ItemsAdder generates a model for every item at
 * {@code <ns>:item/ia_auto/<id>} in the compiled resource pack. These are
 * the authoritative paths - we reference them directly. For items that declare a
 * {@code graphics.icon} / {@code resource.icon}, IA additionally generates
 * {@code <ns>:item/ia_auto/<id>_icon}, which uses a flat GUI-appropriate
 * representation.
 *
 * <h2>Two-part output</h2>
 * <ol>
 *   <li><b>Resource pack</b> - {@code assets/minecraft/items/painting.json}
 *       written to {@code plugins/ItemsAdderAdditions/resourcepack/} and
 *       merged into ItemsAdder's pack via
 *       {@code merge_other_plugins_resourcepacks_folders}.
 *       Takes effect after {@code /iazip} + resource-pack reload.</li>
 *   <li><b>NMS registry injection</b> - {@link RegistryInjector} injects
 *       custom {@code PaintingVariant} entries at runtime on every
 *       {@code ItemsAdderLoadDataEvent}. Because {@code painting_variant} is a
 *       frozen registry that the client also holds, variants are only visible in
 *       the creative menu after the client reconnects (or the server restarts
 *       with a data pack that pre-registers them).</li>
 * </ol>
 */
@NullMarked
public final class CreativeMenuManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final String IA_MERGE_PATH = "ItemsAdderAdditions/resourcepack";
    private static final String MERGE_SETTING_ITEMSADDER = "resource-pack.zip.merge_other_plugins_resourcepacks_folders";

    public void setup() {
        configureItemsAdder();
        writeBlankPaintingTexture();
    }

    public void reload() {
        Collection<CustomStack> items = ItemsAdder.getAllItems();
        int count = generatePaintingJson(items);

        Log.success("CreativeMenu", "Generated {} entries - run /iazip to apply resource pack changes.", count);
    }

    /**
     * Builds {@code assets/minecraft/items/painting.json} and writes it to our
     * resource pack folder.
     *
     * @return the number of items written into the select cases
     */
    private int generatePaintingJson(Collection<CustomStack> items) {
        JsonObject root = new JsonObject();
        JsonObject selectModel = new JsonObject();
        selectModel.addProperty("type", "minecraft:select");
        selectModel.addProperty("property", "minecraft:component");
        selectModel.addProperty("component", "minecraft:painting/variant");

        JsonArray cases = new JsonArray();
        int count = 0;

        for (CustomStack item : items) {
            if (shouldSkip(item))
                continue;

            String modelKey = resolveModel(item);

            JsonObject caseObj = new JsonObject();
            caseObj.addProperty("when", variantKey(item));

            JsonObject caseModel = new JsonObject();
            caseModel.addProperty("type", "minecraft:model");
            caseModel.addProperty("model", modelKey);
            caseObj.add("model", caseModel);

            cases.add(caseObj);
            count++;
        }

        selectModel.add("cases", cases);

        // Vanilla painting as fallback so unmodified paintings still render
        JsonObject fallback = new JsonObject();
        fallback.addProperty("type", "minecraft:model");
        fallback.addProperty("model", "minecraft:item/painting");
        selectModel.add("fallback", fallback);

        root.add("model", selectModel);
        writeJson(resourcePackFile("assets/minecraft/items/painting.json"), root);
        return count;
    }

    /**
     * Returns the model key to use for {@code item} in {@code painting.json}.
     *
     * <p>Resolution order:
     * <ol>
     *   <li>{@code graphics.icon} / {@code resource.icon} present ->
     *       {@code <ns>:item/ia_auto/<id>_icon}<br>
     *       IA generates this flat GUI-oriented model for every item that
     *       declares an icon.</li>
     *   <li>{@code graphics.model} present ->
     *       the declared model path (physical file in the content pack).</li>
     *   <li>{@code resource.model_path} with {@code generate: false} ->
     *       the declared model path (physical file in the content pack).</li>
     *   <li>Everything else ->
     *       {@code <ns>:item/ia_auto/<id>}<br>
     *       IA generates this model for ALL items - it is the correct path
     *       for items using {@code graphics.texture}, {@code graphics.parent +
     *       graphics.textures}, {@code resource.generate: true}, and all
     *       category defaults.</li>
     * </ol>
     */
    private static String resolveModel(CustomStack item) {
        FileConfiguration config = item.getConfig();
        String base = "items." + item.getId();
        String namespace = item.getNamespace();
        String id = item.getId();

        if (hasIcon(config, base))
            return namespace + ":item/ia_auto/" + id + "_icon";

        String graphicsModel = config.getString(base + ".graphics.model");
        if (graphicsModel != null && !graphicsModel.isBlank())
            return normalizeModelPath(graphicsModel, namespace);

        String resourceModel = config.getString(base + ".resource.model_path");
        boolean generate = config.getBoolean(base + ".resource.generate", true);
        if (resourceModel != null && !resourceModel.isBlank() && !generate)
            return normalizeModelPath(resourceModel, namespace);

        return namespace + ":item/ia_auto/" + id;
    }

    /**
     * Returns {@code true} if {@code base} declares {@code graphics.icon} /
     * {@code resource.icon}, or if it is a {@code variant_of} a template that does.
     * IA generates a {@code ia_auto/<id>_icon} model in either case.
     */
    private static boolean hasIcon(FileConfiguration config, String base) {
        String icon = config.getString(base + ".graphics.icon",
                config.getString(base + ".resource.icon"));
        if (icon != null && !icon.isBlank())
            return true;

        // Follow one level of variant_of to pick up inherited icon.
        String templateId = config.getString(base + ".variant_of");
        if (templateId == null || templateId.isBlank())
            return false;

        String templateBase = "items." + templateId;
        String templateIcon = config.getString(templateBase + ".graphics.icon",
                config.getString(templateBase + ".resource.icon"));
        return templateIcon != null && !templateIcon.isBlank();
    }

    /**
     * Returns {@code true} for items that should not appear in the creative menu:
     * internal IA items (namespaces starting with {@code _}) and template items.
     */
    private static boolean shouldSkip(CustomStack item) {
        FileConfiguration config = item.getConfig();
        return config.getBoolean("items." + item.getId() + ".template", false);
    }

    /**
     * Strips any trailing {@code .json} or {@code .png} extension from {@code path}.
     */
    private static String stripExtension(String path) {
        if (path.endsWith(".json"))
            return path.substring(0, path.length() - 5);

        if (path.endsWith(".png"))
            return path.substring(0, path.length() - 4);

        return path;
    }

    /**
     * Strips any extension and prepends {@code namespace:} when the path is
     * not already fully qualified (does not contain {@code :}).
     */
    private static String normalizeModelPath(String path, String namespace) {
        path = stripExtension(path);
        return path.contains(":") ? path : namespace + ":" + path;
    }

    private void writeBlankPaintingTexture() {
        File out = resourcePackFile("assets/itemsadder_additions/textures/painting/placeholder.png");
        if (out.exists())
            return;

        try {
            out.getParentFile().mkdirs();
            BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
            image.setRGB(0, 0, 0x00000000);

            if (!ImageIO.write(image, "PNG", out))
                throw new IOException("No PNG writer available");
        } catch (IOException e) {
            Log.error("CreativeMenu", "Failed to write placeholder painting texture", e);
        }
    }

    private void configureItemsAdder() {
        File iaConfig = new File(
                ItemsAdderAdditions.instance().getDataFolder().getParentFile(),
                "ItemsAdder/config.yml"
        );

        if (!iaConfig.exists()) {
            Log.warn("CreativeMenu", "Could not locate ItemsAdder/config.yml - add '{}' to merge_other_plugins_resourcepacks_folders manually", IA_MERGE_PATH);
            return;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(iaConfig);
        List<String> mergeFolders = config.getStringList(MERGE_SETTING_ITEMSADDER);

        if (mergeFolders.contains(IA_MERGE_PATH))
            return;

        mergeFolders.add(IA_MERGE_PATH);
        config.set(MERGE_SETTING_ITEMSADDER, mergeFolders);

        try {
            config.save(iaConfig);
            Log.success("CreativeMenu", "Registered '{}' in ItemsAdder's merge list.", IA_MERGE_PATH);
        } catch (IOException e) {
            Log.error("CreativeMenu", "Failed to save ItemsAdder/config.yml", e);
        }
    }

    /**
     * Returns the painting variant key for {@code item}.
     * <strong>Must match the {@code ResourceLocation} used in {@link RegistryInjector}.</strong>
     */
    private static String variantKey(CustomStack item) {
        return "ia_creative:" + item.getNamespace() + "_" + item.getId();
    }

    private static File resourcePackFile(String relativePath) {
        return new File(ItemsAdderAdditions.instance().getDataFolder(),
                "resourcepack/" + relativePath);
    }

    private boolean writeJson(File file, JsonObject json) {
        String content = GSON.toJson(json);
        if (file.exists()) {
            try {
                if (Files.readString(file.toPath()).equals(content))
                    return false;
            } catch (IOException ignored) {
                // we don't care
            }
        }

        try {
            file.getParentFile().mkdirs();
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(content);
            }
            return true;
        } catch (IOException e) {
            Log.error("CreativeMenu", "Failed to write " + file.getPath(), e);
            return false;
        }
    }
}
