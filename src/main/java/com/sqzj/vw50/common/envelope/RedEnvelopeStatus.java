package com.sqzj.vw50.common.envelope;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

public enum RedEnvelopeStatus implements StringRepresentable {
    ACTIVE("active"),
    DESTROYED("destroyed"),
    EXPIRED("expired"),
    FINISHED("finished");

    public static final Codec<RedEnvelopeStatus> CODEC = StringRepresentable.fromEnum(RedEnvelopeStatus::values);
    public static final StreamCodec<ByteBuf, RedEnvelopeStatus> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    private final String name;

    RedEnvelopeStatus(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }
}
