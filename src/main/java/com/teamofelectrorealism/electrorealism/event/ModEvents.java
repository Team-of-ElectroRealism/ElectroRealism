package com.teamofelectrorealism.electrorealism.event;

import com.mojang.logging.LogUtils;
import com.teamofelectrorealism.electrorealism.network.NetworkManager;
import net.minecraft.world.level.Level;
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
        LevelAccessor level = event.getLevel();

        if (level instanceof Level) {
            if (!NetworkManager.instances.containsKey(level)) {
                NetworkManager networkManager = new NetworkManager(level);
                NetworkManager.instances.put(level, networkManager);
                LOGGER.info("NetworkManager created for level: " + ((Level) level).dimension());
            }
        }
    }
}