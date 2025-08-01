package com.adsd.adsdrill.client.event;
import com.adsd.adsdrill.AdsDrillAddon;
import com.adsd.adsdrill.content.commands.CustomNodeCommand;
import com.adsd.adsdrill.content.commands.FindOreModsCommand;
import com.adsd.adsdrill.content.worldgen.OreNodeFeature;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

@EventBusSubscriber(modid = AdsDrillAddon.MOD_ID)
public class CommandEvents {
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        FindOreModsCommand.register(event.getDispatcher());
        CustomNodeCommand.register(event.getDispatcher());
    }


    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        OreNodeFeature.clearCache();
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        OreNodeFeature.clearCache();
    }

    @SubscribeEvent
    public static void onTagsUpdated(TagsUpdatedEvent event) {
        if (event.getUpdateCause() == TagsUpdatedEvent.UpdateCause.SERVER_DATA_LOAD) {
            OreNodeFeature.clearCache();
        }
    }

}