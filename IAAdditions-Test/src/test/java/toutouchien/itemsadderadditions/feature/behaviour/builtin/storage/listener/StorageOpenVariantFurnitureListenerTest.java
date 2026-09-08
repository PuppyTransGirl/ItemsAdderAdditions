package toutouchien.itemsadderadditions.feature.behaviour.builtin.storage.listener;

import dev.lone.itemsadder.api.Events.FurnitureBreakEvent;
import dev.lone.itemsadder.api.Events.FurnitureInteractEvent;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import toutouchien.itemsadderadditions.feature.behaviour.builtin.storage.StorageRuntime;
import toutouchien.itemsadderadditions.feature.behaviour.builtin.storage.inventory.StorageInventoryManager;
import toutouchien.itemsadderadditions.feature.behaviour.builtin.storage.openvariant.OpenVariantPlacement;
import toutouchien.itemsadderadditions.feature.behaviour.builtin.storage.openvariant.OpenVariantTransformer;
import toutouchien.itemsadderadditions.feature.behaviour.builtin.storage.session.StorageSessionManager;

import static org.mockito.Mockito.*;

class StorageOpenVariantFurnitureListenerTest {
    @Test
    void itemDisplayOpenVariantAcceptsUnderlyingStorageFurnitureId() {
        StorageRuntime runtime = runtime();
        StorageSessionManager sessions = runtime.sessionManager();
        OpenVariantTransformer transformer = runtime.openVariantTransformer();
        FurnitureInteractEvent event = mock(FurnitureInteractEvent.class);
        Entity entity = mock(Entity.class);
        Player player = mock(Player.class);
        Location location = mock(Location.class);
        when(event.getBukkitEntity()).thenReturn(entity);
        when(event.getPlayer()).thenReturn(player);
        when(event.getNamespacedID()).thenReturn("pack:barrel");
        when(entity.getLocation()).thenReturn(location);
        when(runtime.matchesOpenVariantId("pack:barrel")).thenReturn(false);
        when(runtime.matchesStorageFurniture("pack:barrel", entity)).thenReturn(true);
        when(transformer.isTransformed(location)).thenReturn(true);

        new StorageOpenVariantFurnitureListener(runtime).onOpenVariantFurnitureInteract(event);

        verify(event).setCancelled(true);
        verify(sessions).openForPlayerAtTransformedLocation(player, location, null, entity);
    }

    @Test
    void breakingItemDisplayOpenVariantCapturesLiveContentsAndDropsOriginal() {
        StorageRuntime runtime = runtime();
        StorageSessionManager sessions = runtime.sessionManager();
        OpenVariantTransformer transformer = runtime.openVariantTransformer();
        FurnitureBreakEvent event = mock(FurnitureBreakEvent.class);
        Entity entity = mock(Entity.class);
        Location location = mock(Location.class);
        ItemStack[] contents = new ItemStack[2];
        when(event.getBukkitEntity()).thenReturn(entity);
        when(event.getNamespacedID()).thenReturn("pack:barrel");
        when(entity.getLocation()).thenReturn(location);
        when(runtime.matchesOpenVariantId("pack:barrel")).thenReturn(false);
        when(runtime.matchesStorageFurniture("pack:barrel", entity)).thenReturn(true);
        when(transformer.isTransformed(location)).thenReturn(true);
        when(sessions.getLiveContentsAt(location)).thenReturn(contents);

        try (MockedStatic<StorageInventoryManager> storage = mockStatic(StorageInventoryManager.class);
             MockedStatic<OpenVariantPlacement> placement = mockStatic(OpenVariantPlacement.class)) {
            new StorageOpenVariantFurnitureListener(runtime).onOpenVariantFurnitureBreak(event);

            verify(sessions).closeSessionsForOpenVariantBreak(location);
            verify(transformer).forgetState(location);
            verify(runtime).handleOpenVariantBreakDrops(location, contents);
            storage.verify(() -> StorageInventoryManager.clearEntity(entity, runtime.contentsKey()));
            placement.verify(() -> OpenVariantPlacement.removeFurnitureEntity(entity));
        }
    }

    private StorageRuntime runtime() {
        StorageRuntime runtime = mock(StorageRuntime.class);
        when(runtime.hasFurnitureOpenVariant()).thenReturn(true);
        when(runtime.namespacedId()).thenReturn("pack:barrel");
        when(runtime.sessionManager()).thenReturn(mock(StorageSessionManager.class));
        when(runtime.openVariantTransformer()).thenReturn(mock(OpenVariantTransformer.class));
        return runtime;
    }
}
