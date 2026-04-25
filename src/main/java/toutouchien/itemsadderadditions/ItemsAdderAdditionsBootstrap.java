package toutouchien.itemsadderadditions;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import toutouchien.itemsadderadditions.commands.IATradeMachineCommand;
import toutouchien.itemsadderadditions.commands.ItemsAdderAdditionsCommand;

import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public class ItemsAdderAdditionsBootstrap implements PluginBootstrap {
    @Override
    public void bootstrap(BootstrapContext ctx) {
        ctx.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
            commands.registrar().register(ItemsAdderAdditionsCommand.get(), List.of("iaadditions", "iaa"));
            commands.registrar().register(IATradeMachineCommand.get());
        });
    }
}
