package com.sqzj.vw50.common.envelope;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sqzj.vw50.server.network.RedEnvelopeSnapshot;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class RedEnvelopeRecord {

    public static final int DEFAULT_EXPIRE_TICKS = 20 * 60 * 8;

    public static final Codec<RedEnvelopeRecord> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Base.CODEC.fieldOf("base").forGetter(RedEnvelopeRecord::base),
            Options.CODEC.fieldOf("options").forGetter(RedEnvelopeRecord::options),
            State.CODEC.fieldOf("state").forGetter(RedEnvelopeRecord::state)
    ).apply(instance, RedEnvelopeRecord::new));

    public final UUID id;
    public final UUID senderUuid;
    public final String senderName;
    public String title;
    public String sign;
    public ItemStack stackPrototype;
    public ResourceLocation iconIdentifier;
    public int cardColor;
    public int totalAmount;
    public int remainingAmount;
    public int playerCount;
    public boolean lucky;
    public boolean returnWhenExpired;
    public boolean usePassword;
    public String password;
    public String exclusiveUser;
    public boolean hidden;
    public List<String> visiblePlayers;
    public RedEnvelopeStatus status;
    public long createdGameTime;
    public long expireGameTime;
    public boolean systemEnvelope;
    public List<ClaimRecord> claims;

    public RedEnvelopeRecord(Base base, Options options, State state) {
        this.id = base.id();
        this.senderUuid = base.senderUuid();
        this.senderName = base.senderName();
        this.title = base.title();
        this.sign = base.sign();
        this.stackPrototype = base.stackPrototype().copyWithCount(1);
        this.iconIdentifier = sanitizeIcon(base.iconIdentifier());
        this.cardColor = base.cardColor();
        this.systemEnvelope = base.systemEnvelope();
        this.playerCount = options.playerCount();
        this.lucky = options.lucky();
        this.returnWhenExpired = options.returnWhenExpired();
        this.usePassword = options.usePassword();
        this.password = options.password().orElse("");
        this.exclusiveUser = options.exclusiveUser().orElse("");
        this.hidden = options.hidden();
        this.visiblePlayers = new ArrayList<>(options.visiblePlayers());
        this.totalAmount = state.totalAmount();
        this.remainingAmount = state.remainingAmount();
        this.status = state.status();
        this.createdGameTime = state.createdGameTime();
        this.expireGameTime = state.expireGameTime();
        this.claims = new ArrayList<>(state.claims());
    }

    public RedEnvelopeRecord(
            UUID senderUuid,
            String senderName,
            String title,
            String sign,
            ItemStack stackPrototype,
            ResourceLocation iconIdentifier,
            int cardColor,
            int totalAmount,
            int playerCount,
            boolean lucky,
            boolean returnWhenExpired,
            boolean usePassword,
            String password,
            String exclusiveUser,
            boolean hidden,
            List<String> visiblePlayers,
            long createdGameTime,
            boolean systemEnvelope) {
        this(new Base(UUID.randomUUID(), senderUuid, senderName, title, sign, stackPrototype, sanitizeIcon(iconIdentifier), cardColor, systemEnvelope),
                new Options(playerCount, lucky, returnWhenExpired, usePassword, emptyToOptional(password), emptyToOptional(exclusiveUser), hidden, visiblePlayers),
                new State(totalAmount, totalAmount, RedEnvelopeStatus.ACTIVE, createdGameTime, createdGameTime + DEFAULT_EXPIRE_TICKS, List.of()));
    }

    private Base base() {
        return new Base(this.id, this.senderUuid, this.senderName, this.title, this.sign, this.stackPrototype, sanitizeIcon(this.iconIdentifier), this.cardColor, this.systemEnvelope);
    }

    private Options options() {
        return new Options(this.playerCount, this.lucky, this.returnWhenExpired, this.usePassword, emptyToOptional(this.password), emptyToOptional(this.exclusiveUser), this.hidden, this.visiblePlayers);
    }

    private State state() {
        return new State(this.totalAmount, this.remainingAmount, this.status, this.createdGameTime, this.expireGameTime, this.claims);
    }

    public boolean isActive(long gameTime) {
        return this.status == RedEnvelopeStatus.ACTIVE && this.remainingAmount > 0 && this.claims.size() < this.playerCount && gameTime < this.expireGameTime;
    }

    public boolean isVisibleTo(String playerName) {
        if (!this.hidden) return true;
        if (this.visiblePlayers.isEmpty()) return true;
        return this.visiblePlayers.stream().anyMatch(name -> name.equalsIgnoreCase(playerName));
    }

    public boolean hasClaimed(UUID playerUuid) {
        return this.claims.stream().anyMatch(claim -> claim.playerUuid().equals(playerUuid));
    }

    public int remainingClaims() {
        return Math.max(0, this.playerCount - this.claims.size());
    }

    public ItemStack copyStackWithAmount(int amount) {
        ItemStack result = this.stackPrototype.copy();
        result.setCount(amount);
        return result;
    }

    public void addClaim(UUID playerUuid, String playerName, int amount, long gameTime) {
        this.claims.add(new ClaimRecord(playerUuid, playerName, amount, gameTime));
        this.remainingAmount = Math.max(0, this.remainingAmount - amount);
        if (this.remainingAmount == 0 || this.claims.size() >= this.playerCount) {
            this.status = RedEnvelopeStatus.FINISHED;
        }
    }

    public List<ClaimRecord> luckiestClaims() {
        int max = this.claims.stream().mapToInt(ClaimRecord::amount).max().orElse(0);
        if (max <= 0) return List.of();
        return this.claims.stream().filter(claim -> claim.amount() == max).toList();
    }

    public static Optional<String> emptyToOptional(String value) {
        return value == null || value.isBlank() ? Optional.empty() : Optional.of(value);
    }

    private static ResourceLocation sanitizeIcon(ResourceLocation iconIdentifier) {
        return RedEnvelopeStyleOptions.normalizeIconIdentifier(iconIdentifier);
    }

    public record Base(UUID id, UUID senderUuid, String senderName, String title, String sign, ItemStack stackPrototype, ResourceLocation iconIdentifier, int cardColor, boolean systemEnvelope) {
        private static final Codec<Base> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                UUIDUtil.CODEC.fieldOf("id").forGetter(Base::id),
                UUIDUtil.CODEC.fieldOf("sender_uuid").forGetter(Base::senderUuid),
                Codec.STRING.fieldOf("sender_name").forGetter(Base::senderName),
                Codec.STRING.fieldOf("title").forGetter(Base::title),
                Codec.STRING.fieldOf("sign").forGetter(Base::sign),
                ItemStack.CODEC.fieldOf("stack_prototype").forGetter(Base::stackPrototype),
                ResourceLocation.CODEC.optionalFieldOf("icon_item_id", RedEnvelopeSnapshot.DEFAULT_ICON_IDENTIFIER).forGetter(Base::iconIdentifier),
                Codec.INT.optionalFieldOf("card_color", RedEnvelopeSnapshot.DEFAULT_CARD_COLOR).forGetter(Base::cardColor),
                Codec.BOOL.fieldOf("system_envelope").forGetter(Base::systemEnvelope)
        ).apply(instance, Base::new));
    }

    public record Options(int playerCount, boolean lucky, boolean returnWhenExpired, boolean usePassword, Optional<String> password, Optional<String> exclusiveUser, boolean hidden, List<String> visiblePlayers) {
        private static final Codec<Options> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("player_count").forGetter(Options::playerCount),
                Codec.BOOL.fieldOf("lucky").forGetter(Options::lucky),
                Codec.BOOL.fieldOf("return_when_expired").forGetter(Options::returnWhenExpired),
                Codec.BOOL.fieldOf("use_password").forGetter(Options::usePassword),
                Codec.STRING.optionalFieldOf("password").forGetter(Options::password),
                Codec.STRING.optionalFieldOf("exclusive_user").forGetter(Options::exclusiveUser),
                Codec.BOOL.fieldOf("hidden").forGetter(Options::hidden),
                Codec.STRING.listOf().fieldOf("visible_players").forGetter(Options::visiblePlayers)
        ).apply(instance, Options::new));
    }

    public record State(int totalAmount, int remainingAmount, RedEnvelopeStatus status, long createdGameTime, long expireGameTime, List<ClaimRecord> claims) {
        private static final Codec<State> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("total_amount").forGetter(State::totalAmount),
                Codec.INT.fieldOf("remaining_amount").forGetter(State::remainingAmount),
                RedEnvelopeStatus.CODEC.fieldOf("status").forGetter(State::status),
                Codec.LONG.fieldOf("created_game_time").forGetter(State::createdGameTime),
                Codec.LONG.fieldOf("expire_game_time").forGetter(State::expireGameTime),
                ClaimRecord.CODEC.listOf().fieldOf("claims").forGetter(State::claims)
        ).apply(instance, State::new));
    }

}