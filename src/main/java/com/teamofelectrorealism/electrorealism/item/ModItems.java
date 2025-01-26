package com.teamofelectrorealism.electrorealism.item;

import com.teamofelectrorealism.electrorealism.ElectroRealism;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ElectroRealism.MODID);

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
