package com.teamofelectrorealism.electrorealism.event;

import com.mojang.logging.LogUtils;
import com.teamofelectrorealism.electrorealism.ElectroRealism;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;
import org.slf4j.Logger;

@EventBusSubscriber
public class ModEvents {
    private static final Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent
    public static void onLevelLoad(LevelEvent.Load event) {
        LevelAccessor world = event.getLevel();

        ElectroRealism.NETWORK_MANAGER.levelLoaded(world);
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        LevelAccessor world = event.getLevel();

        if (!world.isClientSide() && world instanceof ServerLevel) {
            if (ElectroRealism.NETWORK_MANAGER != null) {
                LOGGER.info("Server Level is unloading. Clearing NetworkManager runtime state.");
                ElectroRealism.NETWORK_MANAGER.levelUnloaded();
            }
        }
    }
}