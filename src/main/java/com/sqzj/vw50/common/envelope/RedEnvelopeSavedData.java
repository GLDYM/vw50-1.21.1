package com.sqzj.vw50.common.envelope;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sqzj.vw50.VW50;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class RedEnvelopeSavedData extends SavedData {

    public static final int DEFAULT_REPEAT_MAX_PER_MINUTE = 6;
    public static final int DEFAULT_REPEAT_MIN_INTERVAL_MS = 1200;

    public static final Codec<RedEnvelopeSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    RedEnvelopeRecord.CODEC.listOf().fieldOf("envelopes").forGetter(data -> data.envelopes),
                    PendingReturnRecord.CODEC.listOf().fieldOf("pending_returns").forGetter(data -> data.pendingReturns),
                    SendLimitRecord.CODEC.listOf().fieldOf("send_limits").forGetter(data -> data.sendLimits),
                    com.mojang.serialization.Codec.INT.optionalFieldOf("repeat_max_per_minute", DEFAULT_REPEAT_MAX_PER_MINUTE).forGetter(data -> data.repeatMaxPerMinute),
                    com.mojang.serialization.Codec.INT.optionalFieldOf("repeat_min_interval_ms", DEFAULT_REPEAT_MIN_INTERVAL_MS).forGetter(data -> data.repeatMinIntervalMs)
            ).apply(instance, RedEnvelopeSavedData::new));

    public static final SavedData.Factory<RedEnvelopeSavedData> TYPE = new SavedData.Factory<>(
            RedEnvelopeSavedData::new,
            (tag, registries) -> CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE), tag)
                    .result().orElseGet(RedEnvelopeSavedData::new));

    public final List<RedEnvelopeRecord> envelopes = new ArrayList<>();
    public final List<PendingReturnRecord> pendingReturns = new ArrayList<>();
    public final List<SendLimitRecord> sendLimits = new ArrayList<>();
    public int repeatMaxPerMinute = DEFAULT_REPEAT_MAX_PER_MINUTE;
    public int repeatMinIntervalMs = DEFAULT_REPEAT_MIN_INTERVAL_MS;

    public RedEnvelopeSavedData() {}

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), this)
                .result().ifPresent(encoded -> tag.merge((CompoundTag) encoded));
        return tag;
    }

    public RedEnvelopeSavedData(List<RedEnvelopeRecord> envelopes, List<PendingReturnRecord> pendingReturns, List<SendLimitRecord> sendLimits, int repeatMaxPerMinute, int repeatMinIntervalMs) {
        this.envelopes.addAll(envelopes);
        this.pendingReturns.addAll(pendingReturns);
        this.sendLimits.addAll(sendLimits);
        this.repeatMaxPerMinute = Math.max(0, repeatMaxPerMinute);
        this.repeatMinIntervalMs = Math.max(0, repeatMinIntervalMs);
    }

    public Optional<RedEnvelopeRecord> getEnvelope(UUID id) {
        return this.envelopes.stream().filter(envelope -> envelope.id.equals(id)).findFirst();
    }

    public void addEnvelope(RedEnvelopeRecord record) {
        this.envelopes.add(record);
        this.setDirty();
    }

    public void addPendingReturn(PendingReturnRecord record) {
        this.pendingReturns.add(record);
        this.setDirty();
    }

    public Optional<SendLimitRecord> getLimit(UUID playerUuid) {
        return this.sendLimits.stream().filter(limit -> limit.playerUuid().equals(playerUuid)).findFirst();
    }

    public void setLimit(SendLimitRecord newLimit) {
        this.sendLimits.removeIf(limit -> limit.playerUuid().equals(newLimit.playerUuid()));
        this.sendLimits.add(newLimit);
        this.setDirty();
    }

    public void setRepeatLimit(int maxPerMinute, int minIntervalMs) {
        this.repeatMaxPerMinute = Math.max(0, maxPerMinute);
        this.repeatMinIntervalMs = Math.max(0, minIntervalMs);
        this.setDirty();
    }

    public List<PendingReturnRecord> removePendingReturns(UUID playerUuid) {
        List<PendingReturnRecord> removed = new ArrayList<>();
        Iterator<PendingReturnRecord> iterator = this.pendingReturns.iterator();
        while (iterator.hasNext()) {
            PendingReturnRecord record = iterator.next();
            if (record.playerUuid().equals(playerUuid)) {
                removed.add(record);
                iterator.remove();
            }
        }

        if (!removed.isEmpty()) {
            this.setDirty();
        }

        return removed;
    }

    public void pruneOldDestroyed(long gameTime) {
        long cutoff = gameTime - 20L * 60L * 60L * 8L;
        if (this.envelopes.removeIf(envelope -> envelope.status != RedEnvelopeStatus.ACTIVE && envelope.expireGameTime < cutoff)) {
            this.setDirty();
        }
    }

}