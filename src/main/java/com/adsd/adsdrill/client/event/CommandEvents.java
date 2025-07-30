package com.adsd.adsdrill.client.event;
import com.adsd.adsdrill.AdsDrillAddon;
import com.adsd.adsdrill.content.commands.CustomNodeCommand;
import com.adsd.adsdrill.content.commands.FindOreModsCommand;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = AdsDrillAddon.MOD_ID)
public class CommandEvents {
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        FindOreModsCommand.register(event.getDispatcher());
        CustomNodeCommand.register(event.getDispatcher());
    }
}