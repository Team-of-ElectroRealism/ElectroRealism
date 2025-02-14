package com.teamofelectrorealism.electrorealism.block;

import com.teamofelectrorealism.electrorealism.ElectroRealism;
import com.teamofelectrorealism.electrorealism.block.crusher.ElectricCrusherBlockEntity;
import com.teamofelectrorealism.electrorealism.block.arc_furnace.ArcFurnaceBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModBlockEntityTypes {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ElectroRealism.MODID);

    public static final Supplier<BlockEntityType<ElectricCrusherBlockEntity>> ELECTRIC_CRUSHER_BE =
            BLOCK_ENTITY_TYPES.register("electric_crusher_be",
                    () -> BlockEntityType.Builder.of(
                            ElectricCrusherBlockEntity::new,
                            ModBlocks.ELECTRIC_CRUSHER.get()
                    )
                    .build(null));
    public static final Supplier<BlockEntityType<ArcFurnaceBlockEntity>> ARC_FURNACE_BE =
            BLOCK_ENTITY_TYPES.register("arc_furnace_entity",
                    () -> BlockEntityType.Builder.of(
                            ArcFurnaceBlockEntity::new,
                            ModBlocks.ARC_FURNACE.get()
                    )
                    .build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITY_TYPES.register(eventBus);
    }
}
