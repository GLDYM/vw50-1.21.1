package com.sqzj.vw50.server.network;

import com.sqzj.vw50.common.envelope.RedEnvelopeStyleOptions;
import com.sqzj.vw50.common.envelope.RedEnvelopeRecord;
import com.sqzj.vw50.common.envelope.RedEnvelopeStatus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record RedEnvelopeSnapshot(
        UUID id,
        String title,
        String sign,
        String senderName,
        String exclusiveUser,
        boolean usePassword,
        String password,
        boolean hidden,
        boolean viewerClaimed,
        int remainingAmount,
        int totalAmount,
        int playerCount,
        int claimedCount,
        int remainingTicks,
        int elapsedTicks,
        boolean lucky,
        ResourceLocation iconIdentifier,
        int cardColor,
        List<ClaimSnapshot> claims,
        RedEnvelopeStatus status) {

    public static final ResourceLocation DEFAULT_ICON_IDENTIFIER = RedEnvelopeStyleOptions.DEFAULT_ICON_IDENTIFIER;
    public static final int DEFAULT_CARD_COLOR = 0xFFC83F2D;

    public RedEnvelopeSnapshot {
        iconIdentifier = RedEnvelopeStyleOptions.normalizeIconIdentifier(iconIdentifier);
        claims = List.copyOf(claims);
    }

    public static RedEnvelopeSnapshot of(RedEnvelopeRecord record, UUID viewerUuid, long gameTime) {
        int remainingTicks = (int)Math.max(0, record.expireGameTime - gameTime);
        int elapsedTicks = (int)Math.max(0, Math.min(gameTime, record.expireGameTime) - record.createdGameTime);
        List<ClaimSnapshot> claims = record.claims.stream().map(ClaimSnapshot::of).toList();
        return new RedEnvelopeSnapshot(record.id, record.title, record.sign, record.senderName, record.exclusiveUser,
                record.usePassword, record.password, record.hidden, record.hasClaimed(viewerUuid), record.remainingAmount,
                record.totalAmount, record.playerCount, record.claims.size(), remainingTicks, elapsedTicks, record.lucky,
                record.iconIdentifier, record.cardColor, claims, record.status);
    }

    public static RedEnvelopeSnapshot decode(RegistryFriendlyByteBuf buf) {
        UUID id = buf.readUUID();
        String title = buf.readUtf(64);
        String sign = buf.readUtf(64);
        String senderName = buf.readUtf(32);
        String exclusiveUser = buf.readUtf(32);
        boolean usePassword = buf.readBoolean();
        String password = buf.readUtf(64);
        boolean hidden = buf.readBoolean();
        boolean viewerClaimed = buf.readBoolean();
        int remainingAmount = buf.readVarInt();
        int totalAmount = buf.readVarInt();
        int playerCount = buf.readVarInt();
        int claimedCount = buf.readVarInt();
        int remainingTicks = buf.readVarInt();
        int elapsedTicks = buf.readVarInt();
        boolean lucky = buf.readBoolean();
        ResourceLocation iconIdentifier = ResourceLocation.STREAM_CODEC.decode(buf);
        int cardColor = buf.readInt();
        int claimCount = buf.readVarInt();
        List<ClaimSnapshot> claims = new ArrayList<>(claimCount);
        for (int i = 0; i < claimCount; i++) {
            claims.add(ClaimSnapshot.decode(buf));
        }
        RedEnvelopeStatus status = RedEnvelopeStatus.STREAM_CODEC.decode(buf);
        return new RedEnvelopeSnapshot(id, title, sign, senderName, exclusiveUser, usePassword, password, hidden,
                viewerClaimed, remainingAmount, totalAmount, playerCount, claimedCount, remainingTicks, elapsedTicks,
                lucky, iconIdentifier, cardColor, claims, status);
    }

    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeUUID(this.id);
        buf.writeUtf(this.title, 64);
        buf.writeUtf(this.sign, 64);
        buf.writeUtf(this.senderName, 32);
        buf.writeUtf(this.exclusiveUser, 32);
        buf.writeBoolean(this.usePassword);
        buf.writeUtf(this.password, 64);
        buf.writeBoolean(this.hidden);
        buf.writeBoolean(this.viewerClaimed);
        buf.writeVarInt(this.remainingAmount);
        buf.writeVarInt(this.totalAmount);
        buf.writeVarInt(this.playerCount);
        buf.writeVarInt(this.claimedCount);
        buf.writeVarInt(this.remainingTicks);
        buf.writeVarInt(this.elapsedTicks);
        buf.writeBoolean(this.lucky);
        ResourceLocation.STREAM_CODEC.encode(buf, this.iconIdentifier);
        buf.writeInt(this.cardColor);
        buf.writeVarInt(this.claims.size());
        for (ClaimSnapshot claim : this.claims) {
            claim.encode(buf);
        }
        RedEnvelopeStatus.STREAM_CODEC.encode(buf, this.status);
    }
}
