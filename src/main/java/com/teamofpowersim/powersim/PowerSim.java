package com.teamofpowersim.powersim;

import com.teamofpowersim.powersim.block.ModBlockEntityTypes;
import com.teamofpowersim.powersim.block.ModBlocks;
import com.teamofpowersim.powersim.block.connector.ConnectorRenderer;
import com.teamofpowersim.powersim.datacomponents.ModDataComponents;
import com.teamofpowersim.powersim.item.ModCreativeModeTabs;
import com.teamofpowersim.powersim.item.ModItems;
import com.teamofpowersim.powersim.network.NetworkManager;
import com.teamofpowersim.powersim.recipe.ModRecipes;
import com.teamofpowersim.powersim.screen.ModMenuTypes;
import com.teamofpowersim.powersim.screen.arc_furnace.ArcFurnaceScreen;
import com.teamofpowersim.powersim.screen.crusher.ElectricCrusherScreen;
import com.teamofpowersim.powersim.screen.generator.CombustionGeneratorScreen;
import com.teamofpowersim.powersim.simulation.NgSpiceSimulator;
import com.teamofpowersim.powersim.screen.refinery.RefineryScreen;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Mod(PowerSim.MODID)
public class PowerSim {
    public static final String MODID = "powersim";
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final NetworkManager NETWORK_MANAGER = new NetworkManager();

    /** Holds every line ngspice prints via sendCharCallback */
    public static final BlockingQueue<String> SPICE_OUTPUT_QUEUE = new LinkedBlockingQueue<>();

    public PowerSim(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::setupRenderers);
        NeoForge.EVENT_BUS.register(this);

        ModCreativeModeTabs.register(modEventBus);

        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModBlockEntityTypes.register(modEventBus);
        ModMenuTypes.register(modEventBus);
        ModRecipes.register(modEventBus);
        ModDataComponents.register(modEventBus);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        modContainer.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        try {
            NgSpiceSimulator.instance();
            LOGGER.info("ngspice initialized");
        } catch (IOException e) {
            throw new RuntimeException("Could not load ngspice native", e);
        }
    }

    private void setupRenderers(final FMLCommonSetupEvent event) {
        BlockEntityRenderers.register(ModBlockEntityTypes.SMALL_CONNECTOR_BE.get(), ConnectorRenderer::new);
        BlockEntityRenderers.register(ModBlockEntityTypes.LARGE_CONNECTOR_BE.get(), ConnectorRenderer::new);
        BlockEntityRenderers.register(ModBlockEntityTypes.DUO_CONNECTOR_BE.get(), ConnectorRenderer::new);
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("HELLO from server starting");
    }

    @EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            LOGGER.info("HELLO FROM CLIENT SETUP");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
        }

        @SubscribeEvent
        public static void registerScreens(RegisterMenuScreensEvent event) {
            event.register(ModMenuTypes.ELECTRIC_CRUSHER_MENU.get(), ElectricCrusherScreen::new);
            event.register(ModMenuTypes.ARC_FURNACE_MENU.get(), ArcFurnaceScreen::new);
            event.register(ModMenuTypes.COMBUSTION_GENERATOR_MENU.get(), CombustionGeneratorScreen::new);
            event.register(ModMenuTypes.REFINERY_MENU.get(), RefineryScreen::new);
        }
    }
}
