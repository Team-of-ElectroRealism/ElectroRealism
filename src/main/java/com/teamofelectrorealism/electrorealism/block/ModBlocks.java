package com.teamofelectrorealism.electrorealism.block;

import com.teamofelectrorealism.electrorealism.ElectroRealism;
import com.teamofelectrorealism.electrorealism.block.components.ANDGate.AndGateBlock;
import com.teamofelectrorealism.electrorealism.block.components.CopperWire.CopperWireBlock;
import com.teamofelectrorealism.electrorealism.block.components.Ground.GroundBlock;
import com.teamofelectrorealism.electrorealism.block.components.NOTGate.NotGateBlock;
import com.teamofelectrorealism.electrorealism.block.components.ORGate.OrGateBlock;
import com.teamofelectrorealism.electrorealism.block.components.Resistor.ResistorBlock;
import com.teamofelectrorealism.electrorealism.block.connector.duo.DuoConnectorBlock;
import com.teamofelectrorealism.electrorealism.block.connector.large.LargeConnectorBlock;
import com.teamofelectrorealism.electrorealism.block.custom.ModFlammableRotatedPillarBlock;
import com.teamofelectrorealism.electrorealism.block.custom.Mounting_PlateBlock;
import com.teamofelectrorealism.electrorealism.block.connector.small.SmallConnectorBlock;
import com.teamofelectrorealism.electrorealism.block.machine.user.crusher.ElectricCrusherBlock;
import com.teamofelectrorealism.electrorealism.block.machine.generator.combustion.CombustionGeneratorBlock;
import com.teamofelectrorealism.electrorealism.block.machine.generator.solarpanel.SolarPanelBlock;
import com.teamofelectrorealism.electrorealism.block.machine.generator.test.VoltageSourceBlock;
import com.teamofelectrorealism.electrorealism.block.machine.generator.waterwheel.WaterWheelBlock;
import com.teamofelectrorealism.electrorealism.block.machine.user.arc_furnace.ArcFurnaceBlock;
import com.teamofelectrorealism.electrorealism.item.ModItems;
import com.teamofelectrorealism.electrorealism.worldgen.tree.ModTreeGrowers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ElectroRealism.MODID);

    // Blocks under here

    public static final DeferredBlock<Block> PROGRAMMER_BLOCK = registerBlock("programmer_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .instabreak()
                    .mapColor(MapColor.COLOR_MAGENTA)));

    public static final DeferredBlock<Block> ELECTRIC_CRUSHER = registerBlock("electric_crusher",
            () -> new ElectricCrusherBlock(BlockBehaviour.Properties.of()
                    .instabreak()
                    .mapColor(MapColor.COLOR_GRAY)
                    .noOcclusion()));
    public static final DeferredBlock<Block> ARC_FURNACE = registerBlock("arc_furnace",
            () -> new ArcFurnaceBlock(BlockBehaviour.Properties.of()
                    .instabreak()
                    .mapColor(MapColor.COLOR_RED)
                    .noOcclusion()));

    public static final DeferredBlock<Block> VOLTAGE_SOURCE = registerBlock("voltage_source",
            () -> new VoltageSourceBlock(BlockBehaviour.Properties.of()
                    .instabreak()
                    .mapColor(MapColor.COLOR_GRAY)
                    .noOcclusion()));

    public static final DeferredBlock<Block> SOLAR_PANEL = registerBlock("solar_panel",
            () -> new SolarPanelBlock(BlockBehaviour.Properties.of()
                    .instabreak()
                    .mapColor(MapColor.COLOR_GRAY)
                    .noOcclusion()));

    public static final DeferredBlock<Block> COMBUSTION_GENERATOR = registerBlock("smeltables_generator",
            () -> new CombustionGeneratorBlock(BlockBehaviour.Properties.of()
                    .instabreak()
                    .mapColor(MapColor.COLOR_GRAY)
                    .noOcclusion()));

    public static final DeferredBlock<Block> WATER_WHEEL = registerBlock("water_wheel",
            () -> new WaterWheelBlock(BlockBehaviour.Properties.of()
                    .instabreak()
                    .mapColor(MapColor.COLOR_GRAY)
                    .noOcclusion()));

    public static final DeferredBlock<Block> SMALL_CONNECTOR = registerBlock("small_connector",
            () -> new SmallConnectorBlock(BlockBehaviour.Properties.of()
                    .instabreak()
                    .mapColor(MapColor.COLOR_GRAY)
                    .noOcclusion()));

    public static final DeferredBlock<Block> LARGE_CONNECTOR = registerBlock("large_connector",
            () -> new LargeConnectorBlock(BlockBehaviour.Properties.of()
                    .instabreak()
                    .mapColor(MapColor.COLOR_GRAY)
                    .noOcclusion()));

    public static final DeferredBlock<Block> DUO_CONNECTOR = registerBlock("duo_connector",
            () -> new DuoConnectorBlock(BlockBehaviour.Properties.of()
                    .instabreak()
                    .mapColor(MapColor.COLOR_GRAY)
                    .noOcclusion()));

    public static final DeferredBlock<CopperWireBlock> COPPER_WIRE = BLOCKS.register("copper_wire",
            () -> new CopperWireBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .noCollission()
                    .instabreak()
                    .strength(0.2F)
                    .sound(SoundType.METAL)
            ));

    public static final DeferredBlock<ResistorBlock> RESISTOR = registerBlock("resistor",
            () -> new ResistorBlock(BlockBehaviour.Properties.of()
                    .instabreak()
                    .strength(0.2F)
                    .sound(SoundType.METAL)
            ));

    public static final DeferredBlock<AndGateBlock> AND_GATE = registerBlock("and_gate",
            () -> new AndGateBlock(BlockBehaviour.Properties.of()
                    .instabreak()
                    .strength(0.2F)
                    .sound(SoundType.METAL)
            ));

    public static final DeferredBlock<OrGateBlock> OR_GATE = registerBlock("or_gate",
            () -> new OrGateBlock(BlockBehaviour.Properties.of()
                    .instabreak()
                    .strength(0.2F)
                    .sound(SoundType.METAL)
            ));

    public static final DeferredBlock<NotGateBlock> NOT_GATE = registerBlock("not_gate",
            () -> new NotGateBlock(BlockBehaviour.Properties.of()
                    .instabreak()
                    .strength(0.2F)
                    .sound(SoundType.METAL)
            ));

    public static final DeferredBlock<GroundBlock> GROUND = registerBlock("ground",
            () -> new GroundBlock(BlockBehaviour.Properties.of()
                    .instabreak()
                    .strength(0.2F)
                    .sound(SoundType.METAL)
            ));

    public static final DeferredBlock<Block> STRIPPED_DARK_OAK_FENCE = registerBlock("stripped_dark_oak_fence",
            () -> new FenceBlock(BlockBehaviour.Properties.of()
                    .strength(4f)
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<Block> STONE_WALL = registerBlock("stone_wall",
            () -> new WallBlock(BlockBehaviour.Properties.of()
                    .strength(4f)
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<Block> MOUNTING_PLATE = registerBlock("mounting_plate",
            () -> new Mounting_PlateBlock(BlockBehaviour.Properties.of()
                    .strength(1.0f)
                    .noOcclusion()
                    .isRedstoneConductor((state, getter, pos) -> false)
                    .isSuffocating((state, getter, pos) -> false)
                    .isViewBlocking((state, getter, pos) -> false)));

    public static final DeferredBlock<Block> BAUXITE_ORE = registerBlock("bauxite_ore",
            () -> new DropExperienceBlock(UniformInt.of(2, 5), BlockBehaviour.Properties.of()
                    .strength(4f)
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<Block> DEEPSLATE_BAUXITE_ORE = registerBlock("deepslate_bauxite_ore",
            () -> new DropExperienceBlock(UniformInt.of(3, 6), BlockBehaviour.Properties.of()
                    .strength(5f)
                    .requiresCorrectToolForDrops()));

    public static final DeferredBlock<Block> ALUMINIUM_BLOCK = registerBlock("aluminium_block",
            () -> new Block(BlockBehaviour.Properties.of()
                    .strength(4f)
                    .requiresCorrectToolForDrops()));

    // Rubber tree start
    public static final DeferredBlock<Block> RUBBER_LOG = registerBlock("rubber_log",
            () -> new ModFlammableRotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LOG)));

    public static final DeferredBlock<Block> RUBBER_WOOD = registerBlock("rubber_wood",
            () -> new ModFlammableRotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_WOOD)));

    public static final DeferredBlock<Block> STRIPPED_RUBBER_LOG = registerBlock("stripped_rubber_log",
            () -> new ModFlammableRotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_OAK_LOG)));

    public static final DeferredBlock<Block> STRIPPED_RUBBER_WOOD = registerBlock("stripped_rubber_wood",
            () -> new ModFlammableRotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_OAK_WOOD)));

    public static final DeferredBlock<Block> RUBBER_PLANKS = registerBlock("rubber_planks",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS)) {
                @Override
                public boolean isFlammable(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
                    return true;
                }

                @Override
                public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
                    return 20;
                }

                @Override
                public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
                    return 5;
                }
            });

    public static final DeferredBlock<Block> RUBBER_LEAVES = registerBlock("rubber_leaves",
            () -> new LeavesBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LEAVES)) {
                @Override
                public boolean isFlammable(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
                    return true;
                }

                @Override
                public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
                    return 60;
                }

                @Override
                public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
                    return 30;
                }
            });

    public static final DeferredBlock<Block> RUBBER_SAPLING = registerBlock("rubber_sapling",
            () -> new SaplingBlock(ModTreeGrowers.RUBBER, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SAPLING)));

    // Rubber tree end
    // Stop Blocks

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }

    private static  <T extends Block> DeferredBlock<T> registerBlock(String name, Supplier<T> block) {
        DeferredBlock<T> toReturn = BLOCKS.register(name, block);
        registerBlockItem(name, toReturn);
        return toReturn;
    }

    private static <T extends Block> void registerBlockItem(String name, DeferredBlock<T> block) {
        ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }


}
