package com.sqzj.vw50.client;

import com.sqzj.vw50.common.envelope.RedEnvelopeStatus;
import com.sqzj.vw50.misc.GuiMessageAttachment;
import com.sqzj.vw50.misc.GuiMessageExtraData;
import com.sqzj.vw50.misc.hook.HookChatComponent;
import com.sqzj.vw50.server.network.ClaimResultPayload;
import com.sqzj.vw50.server.network.ClaimSnapshot;
import com.sqzj.vw50.server.network.RedEnvelopeSnapshot;
import com.sqzj.vw50.server.network.RedEnvelopeSyncPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.*;
import java.util.stream.Collectors;

public final class ClientRedEnvelopeManager {

    private static final Map<UUID, GuiMessage> CHAT_MESSAGES = new HashMap<>();
    private static final Map<UUID, RedEnvelopeSnapshot> SNAPSHOTS = new HashMap<>();
    private static final Set<UUID> FINISH_NOTICES = new HashSet<>();
    private static UUID selectedClaimListId = null;

    public static void handleSync(RedEnvelopeSyncPayload payload, IPayloadContext context) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        if (payload.clearOld()) {
            CHAT_MESSAGES.clear();
            SNAPSHOTS.clear();
            FINISH_NOTICES.clear();
            selectedClaimListId = null;
        }

        for (RedEnvelopeSnapshot snapshot : payload.envelopes()) {
            RedEnvelopeSnapshot old = SNAPSHOTS.put(snapshot.id(), snapshot);
            addOrUpdateChatMessage(minecraft, snapshot);
            if (snapshot.lucky() && snapshot.status() == RedEnvelopeStatus.FINISHED && (old == null || old.status() == RedEnvelopeStatus.ACTIVE)) {
                addFinishNotice(minecraft, snapshot);
            }
        }
    }

    public static void handleClaimResult(ClaimResultPayload payload, IPayloadContext context) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        MutableComponent message = Component.translatable(payload.message(), payload.amount());
        minecraft.player.displayClientMessage(message.withStyle(payload.success() ? ChatFormatting.GOLD : ChatFormatting.RED), true);
    }

    public static RedEnvelopeSnapshot getSnapshot(UUID id) {
        return SNAPSHOTS.get(id);
    }

    public static Optional<RedEnvelopeSnapshot> getSelectedClaimListSnapshot() {
        return selectedClaimListId == null ? Optional.empty() : Optional.ofNullable(SNAPSHOTS.get(selectedClaimListId));
    }

    public static void toggleClaimList(UUID id) {
        selectedClaimListId = Objects.equals(selectedClaimListId, id) ? null : id;
    }

    public static void closeClaimList() {
        selectedClaimListId = null;
    }

    public static List<ClaimSnapshot> getLuckiestClaims(RedEnvelopeSnapshot snapshot) {
        int max = snapshot.claims().stream().mapToInt(ClaimSnapshot::amount).max().orElse(0);
        if (max <= 0) return List.of();
        return snapshot.claims().stream().filter(claim -> claim.amount() == max).toList();
    }

    private static Component makeChatContent(HookChatComponent.RedEnvelopeLayout layout) {
        StringBuilder builder = new StringBuilder();
        int lines = Math.max(1, layout.placeholderLines());
        for (int i = 0; i < lines; i++) {
            if (i > 0) builder.append('\n');
            builder.append(' ');
        }

        return Component.literal(builder.toString());
    }

    private static void applyLayout(GuiMessageExtraData data, HookChatComponent.RedEnvelopeLayout layout) {
        data.redEnvelopeWrapped = layout.wrapped();
        data.redEnvelopeCardWidth = layout.cardWidth();
        data.redEnvelopeCardHeight = layout.cardHeight();
        data.redEnvelopeTotalHeight = layout.totalHeight();
        data.redEnvelopePlaceholderLines = layout.placeholderLines();
    }

    private static void addOrUpdateChatMessage(Minecraft minecraft, RedEnvelopeSnapshot snapshot) {
        HookChatComponent.RedEnvelopeLayout layout = HookChatComponent.computeLayout(minecraft, snapshot);
        GuiMessage existing = CHAT_MESSAGES.get(snapshot.id());
        if (existing != null) {
            GuiMessageExtraData data = GuiMessageAttachment.get(existing);
            if (data != null) {
                data.redEnvelopeSnapshot = snapshot;
                data.isRedEnvelope = true;
                applyLayout(data, layout);
            }

            return;
        }

        ChatComponent chat = minecraft.gui.getChat();
        GuiMessage message = new GuiMessage(minecraft.gui.getGuiTicks(), makeChatContent(layout), null, GuiMessageTag.systemSinglePlayer());
        GuiMessageExtraData data = GuiMessageExtraData.redEnvelope(snapshot);
        applyLayout(data, layout);
        GuiMessageAttachment.put(message, data);
        CHAT_MESSAGES.put(snapshot.id(), message);
        chat.logChatMessage(message);
        chat.addMessageToDisplayQueue(message);
        chat.addMessageToQueue(message);
    }

    private static void addFinishNotice(Minecraft minecraft, RedEnvelopeSnapshot snapshot) {
        if (!FINISH_NOTICES.add(snapshot.id())) return;
        ChatComponent chat = minecraft.gui.getChat();
        String luckiest = getLuckiestClaims(snapshot).stream().map(ClaimSnapshot::playerName).collect(Collectors.joining("、"));
        Component content = Component.translatable("red_envelope.luck_king.notice", snapshot.title(), formatDuration(snapshot.elapsedTicks()), luckiest);
        GuiMessage message = new GuiMessage(minecraft.gui.getGuiTicks(), content, null, GuiMessageTag.systemSinglePlayer());
        GuiMessageExtraData data = GuiMessageExtraData.finishNotice(snapshot);
        GuiMessageAttachment.put(message, data);
        chat.logChatMessage(message);
        chat.addMessageToDisplayQueue(message);
        chat.addMessageToQueue(message);
    }

    private static String formatDuration(int ticks) {
        int seconds = Math.max(0, (ticks + 19) / 20);
        int minutes = seconds / 60;
        int remain = seconds % 60;
        if (minutes <= 0) return Component.translatable("red_envelope.duration.seconds", remain).getString();
        return Component.translatable("red_envelope.duration.minutes_seconds", minutes, remain).getString();
    }

}
