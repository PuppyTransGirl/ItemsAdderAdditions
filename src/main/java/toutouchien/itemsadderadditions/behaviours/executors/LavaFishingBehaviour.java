package toutouchien.itemsadderadditions.behaviours.executors;

import org.apache.commons.io.FilenameUtils;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.jspecify.annotations.NullMarked;
import toutouchien.itemsadderadditions.behaviours.BehaviourExecutor;
import toutouchien.itemsadderadditions.behaviours.BehaviourHost;
import toutouchien.itemsadderadditions.behaviours.annotations.Behaviour;
import toutouchien.itemsadderadditions.utils.other.Log;

import java.util.ArrayList;
import java.util.List;

@NullMarked
@Behaviour( key = "lava_fishing" )
public class LavaFishingBehaviour extends BehaviourExecutor implements Listener {
    private BehaviourHost host;

    private final List<String> worlds = new ArrayList<>();

    @Override
    public boolean configure(Object configData, String namespacedID) {
        if (configData instanceof ConfigurationSection section) {
            if (section.contains("worlds")) {
                worlds.clear();

                Object data = section.get("worlds");
                switch (data) {
                    case String str -> worlds.add(str);
                    case List<?> list -> list.forEach(entry -> worlds.add(String.valueOf(entry)));
                    case null, default -> {
                        if (data == null) {
                            Log.warn("lava_fishing", "worlds configuration was null.");
                        } else {
                            Log.warn("lava_fishing", "worlds configuration had unsupported type " + data.getClass().getTypeName());
                        }

                        return false;
                    }
                }
            }
        }

        return true;
    }

    @Override
    protected void onLoad(BehaviourHost host) {
        this.host = host;
        Bukkit.getPluginManager().registerEvents(this, host.plugin());
    }

    @Override
    protected void onUnload(BehaviourHost host) {
        HandlerList.unregisterAll(this);
    }

    public void onProjectileLaunch(ProjectileLaunchEvent event){
        if (!(event.getEntity() instanceof FishHook hook)) return;
        if (!(hook.getShooter() instanceof Player player)) return;


    }
    @EventHandler
    public void onFish(PlayerFishEvent event){

    }
}
