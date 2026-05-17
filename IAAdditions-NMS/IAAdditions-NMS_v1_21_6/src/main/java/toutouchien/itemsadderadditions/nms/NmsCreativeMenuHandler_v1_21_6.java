package toutouchien.itemsadderadditions.nms;

import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.plugin.Plugin;
import toutouchien.itemsadderadditions.nms.api.INmsCreativeMenuHandler;
import toutouchien.itemsadderadditions.nms.creative.BytePacketListener_v1_21_6;
import toutouchien.itemsadderadditions.nms.creative.PacketListener_v1_21_6;
import toutouchien.itemsadderadditions.nms.creative.RegistryInjector_v1_21_6;

import java.util.Collection;

final class NmsCreativeMenuHandler_v1_21_6 implements INmsCreativeMenuHandler {
    @Override
    public void injectListeners(Plugin plugin) {
        PacketListener_v1_21_6.inject();
        BytePacketListener_v1_21_6.inject(plugin);
    }

    @Override
    public void updatePaintingCache(Collection<CustomStack> items) {
        PacketListener_v1_21_6.updateCache(items);
    }

    @Override
    public void injectPaintingVariants(Collection<CustomStack> items) {
        RegistryInjector_v1_21_6.injectPaintingVariants(items);
    }
}
