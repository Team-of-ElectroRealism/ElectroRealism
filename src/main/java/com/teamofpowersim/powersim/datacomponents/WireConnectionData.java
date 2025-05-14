package com.teamofpowersim.powersim.datacomponents;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;

public record WireConnectionData(BlockPos pos, int node) {
    public static final Codec<WireConnectionData> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    BlockPos.CODEC.fieldOf("pos").forGetter(WireConnectionData::pos),
                    Codec.INT.fieldOf("node").forGetter(WireConnectionData::node)
            ).apply(instance, WireConnectionData::new)
    );
}
