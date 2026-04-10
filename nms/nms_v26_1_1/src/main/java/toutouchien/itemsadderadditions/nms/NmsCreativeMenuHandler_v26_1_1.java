package toutouchien.itemsadderadditions.nms;

import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.plugin.Plugin;
import toutouchien.itemsadderadditions.nms.api.INmsCreativeMenuHandler;
import toutouchien.itemsadderadditions.nms.creative.BytePacketListener_v26_1_1;
import toutouchien.itemsadderadditions.nms.creative.PacketListener_v26_1_1;
import toutouchien.itemsadderadditions.nms.creative.RegistryInjector_v26_1_1;

import java.util.Collection;

final class NmsCreativeMenuHandler_v26_1_1 implements INmsCreativeMenuHandler {
    @Override
    public void injectListeners(Plugin plugin) {
        PacketListener_v26_1_1.inject();
        BytePacketListener_v26_1_1.inject(plugin);
    }

    @Override
    public void updatePaintingCache(Collection<CustomStack> items) {
        PacketListener_v26_1_1.updateCache(items);
    }

    @Override
    public void injectPaintingVariants(Collection<CustomStack> items) {
        RegistryInjector_v26_1_1.injectPaintingVariants(items);
    }
}
