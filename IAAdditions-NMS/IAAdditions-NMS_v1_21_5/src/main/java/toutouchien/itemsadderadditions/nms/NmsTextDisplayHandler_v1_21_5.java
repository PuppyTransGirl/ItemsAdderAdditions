package toutouchien.itemsadderadditions.nms;

import com.mojang.math.Transformation;
import io.papermc.paper.adventure.PaperAdventure;
import net.kyori.adventure.text.Component;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.util.Brightness;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.nms.api.INmsTextDisplayHandler;
import toutouchien.itemsadderadditions.nms.api.textdisplay.PacketTextDisplay;
import toutouchien.itemsadderadditions.nms.api.textdisplay.PacketTextDisplayBillboard;
import toutouchien.itemsadderadditions.nms.api.textdisplay.PacketTextDisplayHandle;
import toutouchien.itemsadderadditions.nms.api.textdisplay.PacketTextDisplayVisual;

import java.util.List;

@NullMarked
public final class NmsTextDisplayHandler_v1_21_5 implements INmsTextDisplayHandler {

    private static Display.TextDisplay createTextDisplay(Location location) {
        World world = location.getWorld();
        if (world == null) {
            throw new IllegalArgumentException("TextDisplay location has no world");
        }

        ServerLevel level = ((CraftWorld) world).getHandle();
        Display.TextDisplay entity = new Display.TextDisplay(EntityType.TEXT_DISPLAY, level);
        entity.setPos(location.getX(), location.getY(), location.getZ());
        entity.setYRot(location.getYaw());
        entity.setXRot(location.getPitch());
        return entity;
    }

    private static void applyVisual(Display.TextDisplay entity, Component text, PacketTextDisplayVisual visual) {
        entity.setText(PaperAdventure.asVanilla(text));
        entity.setBillboardConstraints(toNmsBillboard(visual.billboard()));
        entity.getEntityData().set(Display.TextDisplay.DATA_LINE_WIDTH_ID, visual.lineWidth());
        entity.getEntityData().set(Display.TextDisplay.DATA_BACKGROUND_COLOR_ID, visual.backgroundArgb() == null ? 0 : visual.backgroundArgb());
        entity.setTextOpacity(visual.opacity());
        entity.setFlags(textFlags(visual));
        entity.setTransformation(new Transformation(
                new Vector3f(0.0F, 0.0F, 0.0F),
                new Quaternionf(),
                new Vector3f(visual.scaleX(), visual.scaleY(), visual.scaleZ()),
                new Quaternionf()
        ));
        if (visual.brightnessBlock() != null && visual.brightnessSky() != null) {
            entity.setBrightnessOverride(new Brightness(visual.brightnessBlock(), visual.brightnessSky()));
        }
        entity.setShadowRadius(visual.shadowRadius());
        entity.setShadowStrength(visual.shadowStrength());
    }

    private static byte textFlags(PacketTextDisplayVisual visual) {
        byte flags = 0;
        if (visual.shadow()) flags |= Display.TextDisplay.FLAG_SHADOW;
        if (visual.seeThrough()) flags |= Display.TextDisplay.FLAG_SEE_THROUGH;

        switch (visual.alignment()) {
            case LEFT -> flags |= Display.TextDisplay.FLAG_ALIGN_LEFT;
            case RIGHT -> flags |= Display.TextDisplay.FLAG_ALIGN_RIGHT;
            case CENTER -> {
            }
        }

        return flags;
    }

    private static ClientboundSetEntityDataPacket entityDataPacket(int entityId, Display.TextDisplay entity) {
        List<SynchedEntityData.DataValue<?>> values = entity.getEntityData().getNonDefaultValues();
        return new ClientboundSetEntityDataPacket(entityId, values == null ? List.of() : values);
    }

    private static ServerGamePacketListenerImpl connection(Player viewer) {
        return ((CraftPlayer) viewer).getHandle().connection;
    }

    private static Display.BillboardConstraints toNmsBillboard(PacketTextDisplayBillboard billboard) {
        return switch (billboard) {
            case FIXED -> Display.BillboardConstraints.FIXED;
            case VERTICAL -> Display.BillboardConstraints.VERTICAL;
            case HORIZONTAL -> Display.BillboardConstraints.HORIZONTAL;
            case CENTER -> Display.BillboardConstraints.CENTER;
        };
    }

    @Override
    public PacketTextDisplayHandle spawn(Player viewer, PacketTextDisplay display) {
        try {
            Display.TextDisplay entity = createTextDisplay(display.location());
            applyVisual(entity, display.text(), display.visual());

            ServerGamePacketListenerImpl connection = connection(viewer);
            connection.send(new ClientboundAddEntityPacket(
                    entity.getId(),
                    entity.getUUID(),
                    entity.getX(),
                    entity.getY(),
                    entity.getZ(),
                    entity.getXRot(),
                    entity.getYRot(),
                    entity.getType(),
                    0,
                    entity.getDeltaMovement(),
                    entity.getYHeadRot()
            ));
            connection.send(entityDataPacket(entity.getId(), entity));

            return new PacketTextDisplayHandle(entity.getId(), entity.getUUID());
        } catch (RuntimeException ex) {
            throw new IllegalStateException("Failed to spawn packet TextDisplay", ex);
        }
    }

    @Override
    public void updateMetadata(Player viewer, PacketTextDisplayHandle handle, Component text, PacketTextDisplayVisual visual) {
        try {
            Display.TextDisplay entity = createTextDisplay(viewer.getLocation());
            applyVisual(entity, text, visual);
            connection(viewer).send(entityDataPacket(handle.entityId(), entity));
        } catch (RuntimeException ex) {
            throw new IllegalStateException("Failed to update packet TextDisplay metadata", ex);
        }
    }

    @Override
    public void destroy(Player viewer, PacketTextDisplayHandle handle) {
        try {
            connection(viewer).send(new ClientboundRemoveEntitiesPacket(handle.entityId()));
        } catch (RuntimeException ex) {
            throw new IllegalStateException("Failed to destroy packet TextDisplay", ex);
        }
    }
}
