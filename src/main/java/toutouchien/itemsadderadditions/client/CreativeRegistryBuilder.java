package toutouchien.itemsadderadditions.client;

import dev.lone.itemsadder.api.CustomFurniture;
import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import toutouchien.itemsadderadditions.utils.other.Log;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.*;

final class CreativeRegistryBuilder {
    private static final String TAG = "ClientCreativeSync";

    private CreativeRegistryBuilder() {
    }

    static CreativeRegistrySnapshot build(Collection<CustomStack> allItems) {
        Map<String, CustomStack> itemsByNamespacedId = new LinkedHashMap<>();
        for (CustomStack item : allItems) {
            if (item == null || shouldSkip(item)) continue;
            itemsByNamespacedId.put(normalizeId(item.getNamespacedID()), item);
            itemsByNamespacedId.put(normalizeId(item.getNamespace() + ":" + item.getId()), item);
        }

        Map<String, MutableCategory> categories = loadCategories(itemsByNamespacedId);
        if (categories.isEmpty()) {
            categories = fallbackCategories(itemsByNamespacedId.values());
        }

        // The client only needs three stable tabs. Keeping the server registry grouped also avoids
        // creating many empty tab slots client-side and makes repeated syncs cheaper to apply.
        Map<String, MutableCategory> groupedCategories = groupIntoClientTabs(categories);

        List<CreativeRegistrySnapshot.Category> encodedCategories = new ArrayList<>();
        int totalItems = 0;

        for (MutableCategory category : groupedCategories.values()) {
            List<CreativeRegistrySnapshot.Entry> entries = new ArrayList<>();
            for (String itemId : category.itemIds) {
                CustomStack customStack = itemsByNamespacedId.get(normalizeId(resolveItemId(itemId, category.namespace)));
                if (customStack == null || shouldSkip(customStack)) {
                    Log.debug(TAG, "Skipping unknown/hidden category item '{}' in '{}'", itemId, category.id);
                    continue;
                }

                byte[] nbt = serializeItem(customStack.getItemStack(), customStack.getNamespacedID());
                if (nbt == null || nbt.length == 0) continue;
                entries.add(new CreativeRegistrySnapshot.Entry(customStack.getNamespacedID(), nbt));
            }

            if (entries.isEmpty()) {
                Log.info(TAG, "Skipping empty client creative tab '{}' ({})", category.id, category.displayName);
                continue;
            }

            byte[] icon = resolveIcon(category, itemsByNamespacedId, entries);
            encodedCategories.add(new CreativeRegistrySnapshot.Category(category.id, category.displayName, icon, List.copyOf(entries)));
            totalItems += entries.size();
            Log.info(TAG, "Client tab '{}' -> {} items", category.displayName, entries.size());
        }

        byte[] encoded = encodeRegistry(encodedCategories);
        Log.info(TAG, "Built creative registry: tabs={}, items={}, totalSerializedSize={} bytes",
                encodedCategories.size(), totalItems, encoded.length);
        return new CreativeRegistrySnapshot(List.copyOf(encodedCategories), encoded, totalItems);
    }

    private static Map<String, MutableCategory> groupIntoClientTabs(Map<String, MutableCategory> sourceCategories) {
        Map<String, MutableCategory> result = new LinkedHashMap<>();
        for (TabBucket bucket : TabBucket.values()) {
            MutableCategory category = new MutableCategory(bucket.id, bucket.id);
            category.displayName = bucket.displayName;
            category.iconId = bucket.iconId;
            result.put(bucket.id, category);
        }

        for (MutableCategory source : sourceCategories.values()) {
            int furniture = 0;
            int blocks = 0;
            int items = 0;

            for (String itemId : source.itemIds) {
                TabBucket bucket = classifyItem(itemId);

                MutableCategory target = result.get(bucket.id);
                if (target == null) {
                    continue;
                }

                target.itemIds.add(itemId);

                switch (bucket) {
                    case FURNITURE -> furniture++;
                    case BLOCK -> blocks++;
                    case ITEM -> items++;
                }
            }

            Log.info(TAG,
                    "Mapped ItemsAdder category '{}' by item type: furniture={}, blocks={}, items={}",
                    source.id, furniture, blocks, items);
        }

        return result;
    }

    private static TabBucket classifyItem(String namespacedId) {
        if (isFurniture(namespacedId)) {
            return TabBucket.FURNITURE;
        }

        CustomStack stack = CustomStack.getInstance(namespacedId);

        if (stack != null && stack.isBlock()) {
            return TabBucket.BLOCK;
        }

        return TabBucket.ITEM;
    }

    private static boolean isFurniture(String namespacedId) {
        return CustomFurniture.getNamespacedIdsInRegistry().contains(namespacedId);
    }

    private static Map<String, MutableCategory> loadCategories(Map<String, CustomStack> itemsByNamespacedId) {
        Map<String, MutableCategory> categories = new LinkedHashMap<>();
        File contents = itemsAdderContentsFolder();
        if (!contents.isDirectory()) {
            Log.warn(TAG, "ItemsAdder contents folder not found: {}", contents.getAbsolutePath());
            return categories;
        }

        List<File> files = new ArrayList<>();
        collectYamlFiles(contents, files);
        for (File file : files) {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
            ConfigurationSection categoriesSection = yaml.getConfigurationSection("categories");
            if (categoriesSection == null) continue;

            String namespace = readNamespace(yaml, file, contents);
            for (String categoryId : categoriesSection.getKeys(false)) {
                ConfigurationSection section = categoriesSection.getConfigurationSection(categoryId);
                if (section == null) continue;
                if (!section.getBoolean("enabled", true)) {
                    Log.debug(TAG, "Skipping disabled ItemsAdder category '{}'", categoryId);
                    continue;
                }

                MutableCategory category = categories.computeIfAbsent(categoryId, key -> new MutableCategory(key, namespace));
                category.namespace = namespace;
                String displayName = firstNonBlank(
                        section.getString("name"),
                        section.getString("display_name"),
                        section.getString("display-name"),
                        section.getString("title")
                );
                if (displayName != null) category.displayName = displayName;

                String icon = firstNonBlank(section.getString("icon"), section.getString("item"));
                if (icon != null) category.iconId = resolveItemId(icon, namespace);

                for (String itemId : section.getStringList("items")) {
                    if (itemId == null || itemId.isBlank()) continue;
                    category.itemIds.add(resolveItemId(itemId, namespace));
                }

                // Some older packs use a section/map under items instead of a string list.
                ConfigurationSection itemSection = section.getConfigurationSection("items");
                if (itemSection != null) {
                    for (String itemKey : itemSection.getKeys(false)) {
                        category.itemIds.add(resolveItemId(itemKey, namespace));
                    }
                }
            }
        }

        // Drop item ids that do not resolve, but keep this validation before serializing for clearer logs.
        for (MutableCategory category : categories.values()) {
            category.itemIds.removeIf(itemId -> {
                boolean missing = !itemsByNamespacedId.containsKey(normalizeId(itemId));
                if (missing) Log.debug(TAG, "Category '{}' references unknown item '{}'", category.id, itemId);
                return missing;
            });
        }
        return categories;
    }

    private static Map<String, MutableCategory> fallbackCategories(Collection<CustomStack> items) {
        Map<String, MutableCategory> categories = new LinkedHashMap<>();
        for (CustomStack item : items) {
            MutableCategory category = categories.computeIfAbsent(item.getNamespace(), key -> {
                MutableCategory created = new MutableCategory(key, key);
                created.displayName = key;
                return created;
            });
            category.itemIds.add(item.getNamespacedID());
        }
        if (!categories.isEmpty()) {
            Log.warn(TAG, "No ItemsAdder categories were found; falling back to one client tab per namespace.");
        }
        return categories;
    }

    private static byte[] resolveIcon(
            MutableCategory category,
            Map<String, CustomStack> itemsByNamespacedId,
            List<CreativeRegistrySnapshot.Entry> entries
    ) {
        if (category.iconId != null) {
            CustomStack customIcon = itemsByNamespacedId.get(normalizeId(category.iconId));
            if (customIcon != null) {
                byte[] serialized = serializeItem(customIcon.getItemStack(), customIcon.getNamespacedID() + " icon");
                if (serialized != null) return serialized;
            }

            String materialName = category.iconId.contains(":")
                    ? category.iconId.substring(category.iconId.indexOf(':') + 1)
                    : category.iconId;
            Material material = Material.matchMaterial(materialName.toUpperCase(Locale.ROOT));
            if (material != null && material.isItem()) {
                byte[] serialized = serializeItem(new ItemStack(material), category.iconId + " icon");
                if (serialized != null) return serialized;
            }
        }

        if (!entries.isEmpty()) {
            return entries.get(0).itemNbt();
        }
        return serializeItem(new ItemStack(Material.BOOK), "fallback icon");
    }

    private static byte[] serializeItem(ItemStack stack, String debugName) {
        try {
            return stack.serializeAsBytes();
        } catch (RuntimeException ex) {
            Log.warn(TAG, "Failed to serialize ItemStack for '{}'", debugName, ex);
            return null;
        }
    }

    private static byte[] encodeRegistry(List<CreativeRegistrySnapshot.Category> categories) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);
            out.writeInt(CreativeSyncProtocol.REGISTRY_MAGIC);
            out.writeInt(CreativeSyncProtocol.REGISTRY_FORMAT_VERSION);
            out.writeInt(categories.size());
            for (CreativeRegistrySnapshot.Category category : categories) {
                WireIO.writeString(out, category.id());
                WireIO.writeString(out, category.displayName());
                writeBytes(out, category.iconNbt());
                out.writeInt(category.items().size());
                for (CreativeRegistrySnapshot.Entry entry : category.items()) {
                    WireIO.writeString(out, entry.id());
                    // This length is only the protocol envelope. The following bytes are the raw
                    // Paper ItemStack#serializeAsBytes() NBT payload, with no fake leading int.
                    writeBytes(out, entry.itemNbt());
                }
            }
            out.flush();
            return bytes.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to encode creative registry", ex);
        }
    }

    private static void writeBytes(DataOutputStream out, byte[] bytes) throws IOException {
        out.writeInt(bytes == null ? 0 : bytes.length);
        if (bytes != null && bytes.length > 0) out.write(bytes);
    }

    private static boolean shouldSkip(CustomStack item) {
        FileConfiguration config = item.getConfig();
        String base = "items." + item.getId();
        return config.getBoolean(base + ".template", false)
                || config.getBoolean(base + ".hide_from_inventory", false)
                || !config.getBoolean(base + ".enabled", true);
    }

    private static void collectYamlFiles(File folder, List<File> out) {
        File[] children = folder.listFiles();
        if (children == null) return;
        for (File child : children) {
            if (child.isDirectory()) {
                collectYamlFiles(child, out);
            } else {
                String name = child.getName().toLowerCase(Locale.ROOT);
                if (name.endsWith(".yml") || name.endsWith(".yaml")) out.add(child);
            }
        }
    }

    private static String readNamespace(YamlConfiguration yaml, File file, File contentsRoot) {
        String namespace = yaml.getString("info.namespace");
        if (namespace != null && !namespace.isBlank()) return namespace;

        File current = file.getParentFile();
        if (Objects.equals(current, contentsRoot)) {
            return "itemsadder";
        }
        while (current != null && !Objects.equals(current.getParentFile(), contentsRoot)) {
            current = current.getParentFile();
        }
        if (current != null && !Objects.equals(current, contentsRoot)) {
            return current.getName();
        }
        return "itemsadder";
    }

    private static File itemsAdderContentsFolder() {
        Plugin itemsAdder = Bukkit.getPluginManager().getPlugin("ItemsAdder");
        if (itemsAdder == null) return new File("plugins/ItemsAdder/contents");
        return new File(itemsAdder.getDataFolder(), "contents");
    }

    private static String resolveItemId(String raw, String namespace) {
        if (raw == null) return "";
        String normalized = raw.trim();
        if (normalized.isEmpty()) return normalized;
        if (normalized.contains(":")) return normalized;
        return namespace + ":" + normalized;
    }

    private static String normalizeId(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) return value;
        }
        return null;
    }

    private enum TabBucket {
        FURNITURE("furniture", "Furniture", "minecraft:oak_stairs"),
        BLOCK("block", "Block", "minecraft:grass_block"),
        ITEM("item", "Item", "minecraft:book");

        final String id;
        final String displayName;
        final String iconId;

        TabBucket(String id, String displayName, String iconId) {
            this.id = id;
            this.displayName = displayName;
            this.iconId = iconId;
        }
    }

    private static final class MutableCategory {
        final String id;
        final LinkedHashSet<String> itemIds = new LinkedHashSet<>();
        String namespace;
        String displayName;
        String iconId;

        MutableCategory(String id, String namespace) {
            this.id = id;
            this.namespace = namespace;
            this.displayName = id;
        }
    }
}
