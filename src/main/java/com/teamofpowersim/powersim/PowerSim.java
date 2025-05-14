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
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.Blocks;
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
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
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

        modEventBus.addListener(this::addCreative);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        try {
            NgSpiceSimulator.instance();
            LOGGER.info("ngspice initialized");
        } catch (IOException e) {
            throw new RuntimeException("Could not load ngspice native", e);
        }

        LOGGER.info("HELLO FROM COMMON SETUP");

        if (Config.logDirtBlock)
            LOGGER.info("DIRT BLOCK >> {}", BuiltInRegistries.BLOCK.getKey(Blocks.DIRT));

        LOGGER.info(Config.magicNumberIntroduction + Config.magicNumber);

        Config.items.forEach(item -> LOGGER.info("ITEM >> {}", item.toString()));
    }

    private void setupRenderers(final FMLCommonSetupEvent event) {
        BlockEntityRenderers.register(ModBlockEntityTypes.SMALL_CONNECTOR_BE.get(), ConnectorRenderer::new);
        BlockEntityRenderers.register(ModBlockEntityTypes.LARGE_CONNECTOR_BE.get(), ConnectorRenderer::new);
        BlockEntityRenderers.register(ModBlockEntityTypes.DUO_CONNECTOR_BE.get(), ConnectorRenderer::new);
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(ModBlocks.PROGRAMMER_BLOCK);
            event.accept(ModBlocks.COPPER_WIRE);
            event.accept(ModBlocks.ELECTRIC_CRUSHER);
            event.accept(ModBlocks.ARC_FURNACE);
            event.accept(ModBlocks.MOUNTING_PLATE);
        }
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
