package com.teamofelectrorealism.electrorealism.block;

import net.minecraft.core.Direction;
import net.minecraft.util.datafix.fixes.ChunkPalettedStorageFix;
import net.minecraft.world.level.block.state.BlockState;

public interface IPowerReceiver {
    int getResistance();

    void receiveVoltage(int voltage);

    int getBufferCharge();

    void setBufferCharge(int charge);
}
