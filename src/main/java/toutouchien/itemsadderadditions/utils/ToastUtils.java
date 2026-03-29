package toutouchien.itemsadderadditions.utils;

import io.papermc.paper.adventure.PaperAdventure;
import net.kyori.adventure.text.Component;
import net.minecraft.advancements.*;
import net.minecraft.network.protocol.game.ClientboundUpdateAdvancementsPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.time.Instant;
import java.util.*;

public final class ToastUtils {
    private static final Identifier TOAST_ID = Identifier.fromNamespaceAndPath("itemsadderadditions", "toast_notification");
    private static final AdvancementRequirements REQUIREMENTS = new AdvancementRequirements(
            List.of(List.of("trigger"))
    );

    private ToastUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static void sendToast(Player player, ItemStack itemStack, Component title, String frame) {
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

        AdvancementProgress progress = ReflectionUtils.newAdvancementProgress(
                Map.of("trigger", new CriterionProgress(Instant.now()))
        );

        ServerGamePacketListenerImpl connection = ((CraftPlayer) player).getHandle().connection;

        connection.send(new ClientboundUpdateAdvancementsPacket(
                false,
                List.of(holder),
                Collections.emptySet(),
                Map.of(TOAST_ID, progress),
                true
        ));

        connection.send(new ClientboundUpdateAdvancementsPacket(
                false,
                Collections.emptyList(),
                Set.of(TOAST_ID),
                Map.of(),
                false
        ));
    }
}
