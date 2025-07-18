package com.yourname.mycreateaddon.client.event;
import com.yourname.mycreateaddon.MyCreateAddon;
import com.yourname.mycreateaddon.content.commands.CustomNodeCommand;
import com.yourname.mycreateaddon.content.commands.FindOreModsCommand;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = MyCreateAddon.MOD_ID)
public class CommandEvents {
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        FindOreModsCommand.register(event.getDispatcher());
        CustomNodeCommand.register(event.getDispatcher());
    }
}