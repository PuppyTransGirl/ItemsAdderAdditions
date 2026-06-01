package toutouchien.itemsadderadditions.integration.valhalla;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * ValhallaTrinkets PDC data for one item.
 *
 * <p>PDC keys written (namespace: {@code valhallatrinkets}):
 * <ul>
 *   <li>{@code trinket_id}        - INTEGER, the trinket type ID</li>
 *   <li>{@code trinket_unique_id} - INTEGER, the unique instance ID</li>
 *   <li>{@code unique}            - BYTE (1), present only when {@code unique} is {@code true}</li>
 * </ul>
 */
@NullMarked
public record ValhallaTrinketData(
        @Nullable Integer trinketId,
        @Nullable Integer trinketUniqueId,
        @Nullable Boolean unique
) {
    public boolean isEmpty() {
        return trinketId == null && trinketUniqueId == null && unique == null;
    }
}
