package com.teamofelectrorealism.electrorealism.screen;

import com.teamofelectrorealism.electrorealism.ElectroRealism;
import com.teamofelectrorealism.electrorealism.screen.arc_furnace.ArcFurnaceMenu;
import com.teamofelectrorealism.electrorealism.screen.crusher.ElectricCrusherMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.network.IContainerFactory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, ElectroRealism.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<ElectricCrusherMenu>> ELECTRIC_CRUSHER_MENU =
            registerMenuType("electric_crusher_menu", ElectricCrusherMenu::new);

    public static final DeferredHolder<MenuType<?>, MenuType<ArcFurnaceMenu>> ARC_FURNACE_MENU =
            registerMenuType("arc_furnace_menu", ArcFurnaceMenu::new);

    private static <T extends AbstractContainerMenu> DeferredHolder<MenuType<?>, MenuType<T>> registerMenuType(String name, IContainerFactory<T> factory) {
        return MENUS.register(name, () -> IMenuTypeExtension.create(factory));
    }

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}
