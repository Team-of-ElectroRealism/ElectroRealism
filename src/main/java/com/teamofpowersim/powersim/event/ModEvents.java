package com.teamofpowersim.powersim.event;

import com.mojang.logging.LogUtils;
import com.teamofpowersim.powersim.PowerSim;
import com.teamofpowersim.powersim.network.ValidateNetworkCommand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.server.command.ConfigCommand;
import org.slf4j.Logger;

@EventBusSubscriber
public class ModEvents {
    private static final Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent
    public static void onLevelLoad(LevelEvent.Load event) {
        LevelAccessor world = event.getLevel();

        PowerSim.NETWORK_MANAGER.levelLoaded(world);
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        LevelAccessor world = event.getLevel();

        if (!world.isClientSide() && world instanceof ServerLevel) {
            if (PowerSim.NETWORK_MANAGER != null) {
                LOGGER.info("Server Level is unloading. Clearing NetworkManager runtime state.");
                PowerSim.NETWORK_MANAGER.levelUnloaded();
            }
        }
    }

    @SubscribeEvent
    public static void onCommandRegister(RegisterCommandsEvent event) {
        ValidateNetworkCommand.register(event.getDispatcher());

        ConfigCommand.register(event.getDispatcher());
    }
}