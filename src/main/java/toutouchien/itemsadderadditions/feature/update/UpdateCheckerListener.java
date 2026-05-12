package toutouchien.itemsadderadditions.feature.update;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import toutouchien.itemsadderadditions.plugin.ItemsAdderAdditions;

import java.util.Locale;

public class UpdateCheckerListener implements Listener {
    private final ItemsAdderAdditions plugin;
    private final String currentVersion;
    private final String latestVersion;

    public UpdateCheckerListener(ItemsAdderAdditions plugin, String currentVersion, String latestVersion) {
        this.plugin = plugin;
        this.currentVersion = currentVersion;
        this.latestVersion = latestVersion;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!this.plugin.settings().updateChecker().notifyOnJoin())
            return;

        Player player = event.getPlayer();
        String pluginName = this.plugin.getName().toLowerCase(Locale.ROOT);
        if (!player.hasPermission(pluginName + ".update-checker"))
            return;

        player.sendRichMessage("""
                <gradient:#AC52D4:#6C3484>ItemsAdderAdditions</gradient><#999999>)</#999999> <#B0AEC1>There is a new version: <#F27474>%s</#F27474> → <#7AF291>%s</#7AF291></#B0AEC1>
                <#B0AEC1>Download link: <#AC52D4><click:open_url:"https://modrinth.com/plugin/itemsadderadditions">https://modrinth.com/plugin/itemsadderadditions</click></#AC52D4></#B0AEC1>
                """.formatted(this.currentVersion, this.latestVersion));
    }
}
