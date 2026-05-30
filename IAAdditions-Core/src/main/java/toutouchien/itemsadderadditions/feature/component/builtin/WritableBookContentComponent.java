package toutouchien.itemsadderadditions.feature.component.builtin;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.WritableBookContent;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import toutouchien.itemsadderadditions.common.logging.Log;
import toutouchien.itemsadderadditions.feature.component.ComponentExecutor;
import toutouchien.itemsadderadditions.feature.component.annotation.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * <table>
 * <tr><th>Property</th><th>Type</th><th>Format</th></tr>
 * <tr><td>(value)</td><td>List&lt;String&gt;</td><td>List of page contents</td></tr>
 * </table>
 */
@SuppressWarnings({"UnstableApiUsage", "unused"})
@NullMarked
@Component(key = "writable_book_content")
public final class WritableBookContentComponent extends ComponentExecutor {
    private List<String> pages = List.of();

    @Override
    public boolean configure(@Nullable Object configData, String namespacedID) {
        if (!(configData instanceof List<?> rawList)) {
            Log.itemWarn("Components", namespacedID, "'writable_book_content' must be a list of strings");
            return false;
        }
        List<String> parsed = new ArrayList<>();
        for (Object entry : rawList) {
            if (entry instanceof String s) {
                parsed.add(s);
            } else {
                parsed.add(entry == null ? "" : entry.toString());
            }
        }
        this.pages = List.copyOf(parsed);
        return !this.pages.isEmpty();
    }

    @Override
    public ItemStack apply(ItemStack itemStack, String namespacedID) {
        itemStack.setData(DataComponentTypes.WRITABLE_BOOK_CONTENT,
                WritableBookContent.writeableBookContent().addPages(pages).build());
        return itemStack;
    }
}
