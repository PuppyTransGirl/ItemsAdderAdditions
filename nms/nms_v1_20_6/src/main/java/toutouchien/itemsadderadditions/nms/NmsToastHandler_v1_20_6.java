package toutouchien.itemsadderadditions.nms;

import io.papermc.paper.adventure.PaperAdventure;
import net.kyori.adventure.text.Component;
import net.minecraft.advancements.*;
import net.minecraft.network.protocol.game.ClientboundUpdateAdvancementsPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import toutouchien.itemsadderadditions.nms.api.INmsToastHandler;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.time.Instant;
import java.util.*;

public final class NmsToastHandler_v1_20_6 implements INmsToastHandler {
    private static final ResourceLocation TOAST_ID = ResourceLocation.of("iaadditions:toast_notification", ':');
    private static final AdvancementRequirements REQUIREMENTS = new AdvancementRequirements(List.of(List.of("trigger")));
    private static final MethodHandle ADVANCEMENT_PROGRESS_CTOR;

    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(
                    AdvancementProgress.class,
                    MethodHandles.lookup()
            );
            ADVANCEMENT_PROGRESS_CTOR = lookup.findConstructor(
                    AdvancementProgress.class,
                    MethodType.methodType(void.class, Map.class)
            );
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to initialize ADVANCEMENT_PROGRESS_CTOR", e
            );
        }
    }

    private static AdvancementProgress newAdvancementProgress(Map<String, CriterionProgress> criteria) {
        try {
            return (AdvancementProgress) ADVANCEMENT_PROGRESS_CTOR.invokeExact(criteria);
        } catch (Throwable e) {
            throw new RuntimeException(
                    "Failed to invoke AdvancementProgress constructor", e
            );
        }
    }

    @Override
    public void sendToast(Player player, ItemStack itemStack, Component title, String frame) {
        AdvancementType type = AdvancementType.valueOf(frame.toUpperCase(Locale.ROOT));

        DisplayInfo displayInfo = new DisplayInfo(
                CraftItemStack.asNMSCopy(itemStack),
                PaperAdventure.asVanilla(title),
                net.minecraft.network.chat.Component.empty(),
                Optional.empty(),
                type,
                true,
                false,
                true
        );

        Advancement advancement = new Advancement(
                Optional.empty(),
                Optional.of(displayInfo),
                AdvancementRewards.EMPTY,
                Collections.emptyMap(),
                REQUIREMENTS,
                false,
                Optional.empty()
        );

        AdvancementHolder holder = new AdvancementHolder(TOAST_ID, advancement);

        AdvancementProgress progress = newAdvancementProgress(
                Map.of("trigger", new CriterionProgress(Instant.now()))
        );

        ServerGamePacketListenerImpl connection =
                ((CraftPlayer) player).getHandle().connection;

        connection.send(new ClientboundUpdateAdvancementsPacket(
                false,
                List.of(holder),
                Collections.emptySet(),
                Map.of(TOAST_ID, progress)
        ));

        connection.send(new ClientboundUpdateAdvancementsPacket(
                false,
                Collections.emptyList(),
                Set.of(TOAST_ID),
                Map.of()
        ));
    }
}
