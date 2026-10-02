package com.sqzj.vw50.misc.hook;

import com.sqzj.vw50.VW50;
import com.sqzj.vw50.client.ClientRedEnvelopeManager;
import com.sqzj.vw50.common.envelope.RedEnvelopeStatus;
import com.sqzj.vw50.misc.GuiMessageAttachment;
import com.sqzj.vw50.misc.GuiMessageExtraData;
import com.sqzj.vw50.server.network.ClaimRedEnvelopePayload;
import com.sqzj.vw50.server.network.ClaimSnapshot;
import com.sqzj.vw50.server.network.RedEnvelopeSnapshot;
import net.minecraft.ChatFormatting;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector2f;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class HookChatComponent {

    public static final WidgetSprites PLUS_ONE_SPRITES = new WidgetSprites(VW50.prefix("plus_one_default"), VW50.prefix("plus_one"));

    private static final int RED_ENV_LEFT = 4;
    private static final int RED_ENV_MIN_WIDTH = 118;
    private static final int RED_ENV_DEFAULT_WIDTH = 158;
    private static final int RED_ENV_MAX_WIDTH = 220;
    private static final int RED_ENV_MIN_HEIGHT = 32;
    private static final int RED_ENV_INLINE_GAP = 4;
    private static final int VANILLA_MESSAGE_HEIGHT = 9;
    private static final int RED_ENV_HEADER_GAP = 2;
    private static final int CARD_TEXT_LINE_HEIGHT = 10;
    private static final int CARD_VERTICAL_PADDING = 6;
    private static final int CARD_DETAIL_GAP = 2;
    private static final int CARD_TEXT_LEFT_OFFSET = 30;
    private static final int CARD_RIGHT_PADDING = 8;
    private static final int CLAIM_PANEL_WIDTH = 232;
    private static final int CLAIM_PANEL_ROW_HEIGHT = 14;
    private static final int EXCLUSIVE_FACE_SIZE = 8;
    private static final int EXCLUSIVE_FACE_GAP = 12;
    private static final Pattern VANILLA_PLAYER_MESSAGE = Pattern.compile("^<[^>]+>\\s*(.*)$");

    private HookChatComponent() {
    }

    public static void onMessageDisplayed(ChatComponent chat, GuiMessage message) {
        GuiMessageExtraData extraData = GuiMessageAttachment.get(message);
        if (extraData == null) return;

        int width = Mth.floor((double) chat.getWidth() / chat.getScale());
        int lineCount = Minecraft.getInstance().font.split(message.content(), Math.max(1, width)).size();
        List<GuiMessage.Line> lines = new ArrayList<>();
        for (int i = 0; i < Math.min(lineCount, chat.trimmedMessages.size()); i++) {
            lines.add(chat.trimmedMessages.get(i));
        }
        GuiMessageAttachment.putLines(message, lines, extraData);
    }

    public static RedEnvelopeLayout computeLayout(Minecraft minecraft, RedEnvelopeSnapshot snapshot) {
        ChatComponent chat = minecraft.gui.getChat();
        int chatWidth = Math.max(RED_ENV_MIN_WIDTH, chat.getWidth());
        int senderWidth = minecraft.font.width(makeSenderText(snapshot));
        int desiredWidth = getDesiredCardWidth(minecraft, snapshot);
        int inlineAvailable = chatWidth - RED_ENV_LEFT - senderWidth - RED_ENV_INLINE_GAP;
        boolean wrapped = inlineAvailable < RED_ENV_MIN_WIDTH || senderWidth + RED_ENV_INLINE_GAP + desiredWidth > chatWidth - RED_ENV_LEFT;
        int maxAvailable = wrapped ? chatWidth - RED_ENV_LEFT : inlineAvailable;
        int cardWidth = clamp(desiredWidth, clamp(maxAvailable, 60, RED_ENV_MIN_WIDTH), clamp(maxAvailable, 60, RED_ENV_MAX_WIDTH));
        int cardHeight = getCardHeight(minecraft, snapshot, cardWidth);
        int totalHeight = wrapped ? VANILLA_MESSAGE_HEIGHT + RED_ENV_HEADER_GAP + cardHeight : cardHeight;
        int entryHeight = Math.max(1, chat.getLineHeight());
        int placeholderLines = Math.max(1, (int) Math.ceil(Math.max(0, totalHeight - VANILLA_MESSAGE_HEIGHT) / (double) entryHeight) + 1);
        return new RedEnvelopeLayout(wrapped, cardWidth, cardHeight, totalHeight, placeholderLines);
    }

    public record RedEnvelopeLayout(boolean wrapped, int cardWidth, int cardHeight, int totalHeight, int placeholderLines) {
    }

    private record Bounds(int left, int top, int right, int bottom) {
        int width() { return this.right - this.left; }
        int height() { return this.bottom - this.top; }
        boolean contains(float x, float y) { return x >= this.left && x < this.right && y >= this.top && y < this.bottom; }
    }

    private record RedEnvelopeRenderLayout(Bounds cardBounds, int senderTop) {
    }

    private record ClaimPanelLayout(int left, int top, int width, int height, Bounds closeBounds) {
    }

    private sealed interface InteractionTarget permits CloseClaimPanelTarget, RedEnvelopeTarget, RepeatTarget {
    }

    private enum CloseClaimPanelTarget implements InteractionTarget { INSTANCE }
    private record RedEnvelopeTarget(UUID id) implements InteractionTarget { }
    private record RepeatTarget(String text) implements InteractionTarget { }

    private static RedEnvelopeLayout layoutFromExtra(GuiMessageExtraData extraData) {
        return new RedEnvelopeLayout(extraData.redEnvelopeWrapped,
            Math.max(60, extraData.redEnvelopeCardWidth),
            Math.max(RED_ENV_MIN_HEIGHT, extraData.redEnvelopeCardHeight),
            Math.max(RED_ENV_MIN_HEIGHT, extraData.redEnvelopeTotalHeight),
            Math.max(1, extraData.redEnvelopePlaceholderLines));
    }

    private static RedEnvelopeRenderLayout getRedEnvelopeRenderLayout(GuiMessageExtraData extraData, int row, int chatBottom, double lineSpacing) {
        ChatComponent chat = Minecraft.getInstance().gui.getChat();
        int textTop = getTextTop(chatBottom, row, chat.getLineHeight(), lineSpacing);
        RedEnvelopeLayout layout = layoutFromExtra(extraData);
        int messageBottom = textTop + VANILLA_MESSAGE_HEIGHT;
        int renderTop = messageBottom - layout.totalHeight();
        int cardTop = layout.wrapped() ? renderTop + VANILLA_MESSAGE_HEIGHT : renderTop;
        int cardLeft = layout.wrapped() ? RED_ENV_LEFT : getInlineCardLeft(extraData);
        int senderTop = layout.wrapped() ? renderTop : cardTop + Math.max(0, (layout.cardHeight() - VANILLA_MESSAGE_HEIGHT) / 2);
        return new RedEnvelopeRenderLayout(new Bounds(cardLeft, cardTop, cardLeft + layout.cardWidth(), cardTop + layout.cardHeight()), senderTop);
    }

    private static Bounds getFinishNoticeBounds(ChatComponent chat, GuiMessage.Line line, int textTop) {
        int width = Math.min(Minecraft.getInstance().font.width(line.content()), chat.getWidth());
        return new Bounds(RED_ENV_LEFT, textTop, RED_ENV_LEFT + Math.max(40, width), textTop + VANILLA_MESSAGE_HEIGHT);
    }

    private static Bounds getRepeatButtonBounds(ChatComponent chat, GuiMessage.Line line, int textTop) {
        int iconLeft = chat.getTagIconLeft(line);
        return new Bounds(iconLeft, textTop, iconLeft + 9, textTop + VANILLA_MESSAGE_HEIGHT);
    }

    private static ClaimPanelLayout getClaimPanelLayout(ChatComponent chat, RedEnvelopeSnapshot snapshot, int chatBottom) {
        int rowCount = Math.max(1, snapshot.claims().size());
        int width = clamp(chat.getWidth(), 140, CLAIM_PANEL_WIDTH);
        int height = 25 + rowCount * CLAIM_PANEL_ROW_HEIGHT + 8;
        int left = RED_ENV_LEFT;
        int top = Math.max(8, chatBottom - height - 92);
        int closeX = left + width - 16;
        int closeY = top + 6;
        return new ClaimPanelLayout(left, top, width, height, new Bounds(closeX - 2, closeY - 2, closeX + 10, closeY + 10));
    }

    private static Vector2f getChatLocalMouse(ChatComponent chat, int mouseX, int mouseY) {
        float scale = (float) chat.getScale();
        return new Vector2f(mouseX / scale - 4.0F, mouseY / scale);
    }

    public static boolean handleMouseClick(ChatComponent chat, int screenHeight, int mouseX, int mouseY) {
        InteractionTarget target = findInteractionTarget(chat, screenHeight, mouseX, mouseY);
        if (target == null) return false;

        Minecraft minecraft = Minecraft.getInstance();
        if (target == CloseClaimPanelTarget.INSTANCE) {
            ClientRedEnvelopeManager.closeClaimList();
            return true;
        }

        if (target instanceof RedEnvelopeTarget redEnvelope) {
            UUID id = redEnvelope.id();
            RedEnvelopeSnapshot snapshot = ClientRedEnvelopeManager.getSnapshot(id);
            if (snapshot != null && (snapshot.status() != RedEnvelopeStatus.ACTIVE || snapshot.viewerClaimed())) {
                ClientRedEnvelopeManager.toggleClaimList(id);
            } else if (snapshot != null && snapshot.usePassword()) {
                if (!snapshot.password().isBlank()) {
                    minecraft.keyboardHandler.setClipboard(snapshot.password());
                    if (minecraft.player != null) minecraft.player.displayClientMessage(Component.translatable("red_envelope.chat.password_copied").withStyle(ChatFormatting.GOLD), true);
                }
            } else {
                PacketDistributor.sendToServer(new ClaimRedEnvelopePayload(id));
            }
            return true;
        }

        if (target instanceof RepeatTarget repeat) {
            String text = repeat.text().trim();
            if (minecraft.player != null && !text.isBlank()) minecraft.player.connection.sendChat(text);
            return true;
        }
        return false;
    }

    private static InteractionTarget findInteractionTarget(ChatComponent chat, int screenHeight, int mouseX, int mouseY) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!chat.isChatFocused() || chat.isChatHidden() || minecraft.options.hideGui) return null;
        Vector2f mouse = getChatLocalMouse(chat, mouseX, mouseY);
        int chatBottom = Mth.floor((screenHeight - ChatComponent.BOTTOM_MARGIN) / (float) chat.getScale());

        RedEnvelopeSnapshot selected = ClientRedEnvelopeManager.getSelectedClaimListSnapshot().orElse(null);
        if (selected != null && getClaimPanelLayout(chat, selected, chatBottom).closeBounds().contains(mouse.x, mouse.y)) {
            return CloseClaimPanelTarget.INSTANCE;
        }

        double lineSpacing = minecraft.options.chatLineSpacing().get();
        int scroll = chat.chatScrollbarPos;
        for (int row = 0; row < chat.getLinesPerPage(); row++) {
            int index = row + scroll;
            if (index >= chat.trimmedMessages.size()) break;
            GuiMessage.Line line = chat.trimmedMessages.get(index);
            GuiMessageExtraData extraData = GuiMessageAttachment.get(line);
            if (extraData == null || !line.endOfEntry()) continue;
            int textTop = getTextTop(chatBottom, row, chat.getLineHeight(), lineSpacing);

            if (extraData.isRedEnvelope && extraData.redEnvelopeId != null) {
                Bounds card = getRedEnvelopeRenderLayout(extraData, row, chatBottom, lineSpacing).cardBounds();
                if (card.contains(mouse.x, mouse.y)) return new RedEnvelopeTarget(extraData.redEnvelopeId);
            } else if (extraData.isRedEnvelopeFinishNotice && extraData.redEnvelopeId != null) {
                if (getFinishNoticeBounds(chat, line, textTop).contains(mouse.x, mouse.y)) return new RedEnvelopeTarget(extraData.redEnvelopeId);
            } else if (extraData.canPlusOne && getRepeatButtonBounds(chat, line, textTop).contains(mouse.x, mouse.y)) {
                return new RepeatTarget(extraData.repeatText);
            }
        }
        return null;
    }

    private static int getTextTop(int chatBottom, int row, int entryHeight, double lineSpacing) {
        int bottomOffset = (int) Math.round(8.0 * (lineSpacing + 1.0) - 4.0 * lineSpacing);
        return chatBottom - row * entryHeight - bottomOffset;
    }

    private static int getInlineCardLeft(GuiMessageExtraData extraData) {
        return Minecraft.getInstance().font.width(makeSenderText(extraData.redEnvelopeSnapshot));
    }

    public static void renderChatOverlay(ChatComponent chat, GuiGraphics graphics, int tick, int mouseX, int mouseY, int screenHeight, boolean focused) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui || chat.isChatHidden()) return;
        float scale = (float) chat.getScale();
        int chatBottom = Mth.floor((screenHeight - ChatComponent.BOTTOM_MARGIN) / scale);
        double lineSpacing = minecraft.options.chatLineSpacing().get();
        Vector2f mouse = getChatLocalMouse(chat, mouseX, mouseY);
        float textOpacity = (float) (focused ? 1.0 : minecraft.options.chatOpacity().get() * 0.9 + 0.1);

        graphics.pose().pushPose();
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.pose().translate(4.0F, 0.0F, 0.0F);
        for (int row = 0; row < chat.getLinesPerPage(); row++) {
            int index = row + chat.chatScrollbarPos;
            if (index >= chat.trimmedMessages.size()) break;
            GuiMessage.Line line = chat.trimmedMessages.get(index);
            GuiMessageExtraData extraData = GuiMessageAttachment.get(line);
            if (extraData == null) continue;
            int age = tick - line.addedTime();
            if (age >= 200 && !focused) continue;
            float opacity = textOpacity * (float) (focused ? 1.0 : ChatComponent.getTimeFactor(age));
            int textTop = getTextTop(chatBottom, row, chat.getLineHeight(), lineSpacing);

            if (extraData.isRedEnvelope && line.endOfEntry()) {
                RedEnvelopeRenderLayout layout = getRedEnvelopeRenderLayout(extraData, row, chatBottom, lineSpacing);
                Bounds card = layout.cardBounds();
                boolean hovered = extraData.redEnvelopeId != null && card.contains(mouse.x, mouse.y);
                renderSenderPrefix(graphics, extraData, 0, layout.senderTop(), opacity);
                renderRedEnvelope(graphics, extraData, card.left(), card.top(), card.width(), card.height(), hovered, opacity);
            } else if (extraData.isRedEnvelopeFinishNotice && line.endOfEntry()) {
                Bounds bounds = getFinishNoticeBounds(chat, line, textTop);
                if (focused && bounds.contains(mouse.x, mouse.y)) graphics.renderOutline(bounds.left(), bounds.top(), bounds.width(), bounds.height(), applyOpacity(0xFFFFD27A, opacity));
            } else if (focused && extraData.canPlusOne && line.endOfEntry()) {
                Bounds bounds = getRepeatButtonBounds(chat, line, textTop);
                graphics.blitSprite(PLUS_ONE_SPRITES.get(true, bounds.contains(mouse.x, mouse.y)), bounds.left(), bounds.top(), bounds.width(), bounds.height());
            }
        }

        if (focused) {
            ClientRedEnvelopeManager.getSelectedClaimListSnapshot().ifPresent(snapshot -> {
                ClaimPanelLayout panel = getClaimPanelLayout(chat, snapshot, chatBottom);
                renderClaimListPanel(graphics, panel, snapshot, textOpacity, mouse.x, mouse.y);
            });
        }
        graphics.pose().popPose();
    }

    private static void renderSenderPrefix(GuiGraphics graphics, GuiMessageExtraData extraData, int left, int top, float opacity) {
        graphics.drawString(Minecraft.getInstance().font, makeSenderText(extraData.redEnvelopeSnapshot), left, top, applyOpacity(0xFFFFFFFF, opacity), true);
    }

    private static void renderRedEnvelope(GuiGraphics graphics, GuiMessageExtraData extraData, int left, int top, int width, int height, boolean hovered, float opacity) {
        RedEnvelopeSnapshot snapshot = extraData.redEnvelopeSnapshot;
        boolean inactive = snapshot != null && snapshot.status() != RedEnvelopeStatus.ACTIVE;
        int baseColor = snapshot == null ? RedEnvelopeSnapshot.DEFAULT_CARD_COLOR : snapshot.cardColor();
        int border = applyOpacity(hovered ? 0xFFFFD27A : lighten(baseColor, 46), opacity);
        int body = applyOpacity(inactive ? desaturate(baseColor) : baseColor, opacity);
        int inner = applyOpacity(inactive ? darken(desaturate(baseColor), 42) : darken(baseColor, 40), opacity);
        graphics.fill(left, top, left + width, top + height, body);
        graphics.fill(left + 2, top + 2, left + width - 2, top + height - 2, inner);
        graphics.fill(left, top, left + width, top + 1, border);
        graphics.fill(left, top + height - 1, left + width, top + height, border);

        int iconY = top + Math.max(8, (height - 16) / 2);
        ResourceLocation icon = snapshot == null ? RedEnvelopeSnapshot.DEFAULT_ICON_IDENTIFIER : snapshot.iconIdentifier();
        graphics.setColor(1.0F, 1.0F, 1.0F, opacity);
        graphics.blit(icon, left + 8, iconY, 16, 16, 0.0F, 0.0F, 16, 16, 16, 16);
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);

        int textLeft = left + CARD_TEXT_LEFT_OFFSET;
        int textWidth = Math.max(20, width - CARD_TEXT_LEFT_OFFSET - CARD_RIGHT_PADDING);
        int y = top + CARD_VERTICAL_PADDING;
        String title = makeTitleText(snapshot);
        drawWrapped(graphics, textLeft, y, textWidth, title, 0xFFFFD27A, opacity);
        y += wrapText(title, textWidth).size() * CARD_TEXT_LINE_HEIGHT + CARD_DETAIL_GAP;
        renderCardDetail(graphics, snapshot, textLeft, y, textWidth, opacity);
    }

    private static void renderCardDetail(GuiGraphics graphics, RedEnvelopeSnapshot snapshot, int x, int y, int width, float opacity) {
        if (snapshot == null) {
            drawWrapped(graphics, x, y, width, Component.translatable("red_envelope.chat.click").getString(), 0xFFFFFF55, opacity);
        } else if (snapshot.status() != RedEnvelopeStatus.ACTIVE) {
            drawWrapped(graphics, x, y, width, Component.translatable("red_envelope.chat.finished").getString(), 0xFFAAAAAA, opacity);
        } else if (snapshot.viewerClaimed()) {
            drawWrapped(graphics, x, y, width, Component.translatable("red_envelope.chat.claimed", snapshot.claimedCount(), snapshot.playerCount()).getString(), 0xFFAAAAAA, opacity);
        } else if (snapshot.usePassword()) {
            String password = snapshot.password().isBlank() ? "?" : snapshot.password();
            drawWrapped(graphics, x, y, width, Component.translatable("red_envelope.chat.copy_password", password).getString(), 0xFFFFFF55, opacity);
        } else if (!snapshot.exclusiveUser().isBlank()) {
            graphics.fill(x - 1, y - 1, x + EXCLUSIVE_FACE_SIZE + 1, y + EXCLUSIVE_FACE_SIZE + 1, applyOpacity(0xFFFFD27A, opacity));
            renderPlayerHead(graphics, snapshot.exclusiveUser(), x, y, opacity);
            drawWrapped(graphics, x + EXCLUSIVE_FACE_GAP, y, Math.max(20, width - EXCLUSIVE_FACE_GAP),
                Component.translatable("red_envelope.chat.exclusive_value", snapshot.exclusiveUser()).getString(), 0xFFFFFF55, opacity);
        } else {
            drawWrapped(graphics, x, y, width, Component.translatable("red_envelope.chat.click", snapshot.claimedCount(), snapshot.playerCount()).getString(), 0xFFFFFF55, opacity);
        }
    }

    private static void renderClaimListPanel(GuiGraphics graphics, ClaimPanelLayout panel, RedEnvelopeSnapshot snapshot, float opacity, float mouseX, float mouseY) {
        Minecraft minecraft = Minecraft.getInstance();
        int left = panel.left();
        int top = panel.top();
        int width = panel.width();
        int height = panel.height();
        graphics.fill(left, top, left + width, top + height, applyOpacity(0xFFD24635, opacity));
        graphics.fill(left + 2, top + 2, left + width - 2, top + height - 2, applyOpacity(0xFF8E211A, opacity));
        graphics.renderOutline(left, top, width, height, applyOpacity(0xFFFFD27A, opacity));
        graphics.drawString(minecraft.font, Component.translatable("red_envelope.claim_list.title", snapshot.title()), left + 8, top + 7, applyOpacity(0xFFFFD27A, opacity), true);
        Bounds close = panel.closeBounds();
        boolean closeHover = close.contains(mouseX, mouseY);
        graphics.drawString(minecraft.font, "x", close.left() + 2, close.top() + 2, applyOpacity(closeHover ? -1 : 0xFFAAAAAA, opacity), true);

        int y = top + 22;
        for (ClaimSnapshot claim : snapshot.claims()) {
            renderPlayerHead(graphics, claim.playerName(), left + 8, y + 2, opacity);
            graphics.drawString(minecraft.font, claim.playerName(), left + 22, y + 2, applyOpacity(0xFFFFFFFF, opacity), true);
            String amount = Component.translatable("red_envelope.claim_list.amount", claim.amount()).getString();
            graphics.drawString(minecraft.font, amount, left + width - 8 - minecraft.font.width(amount), y + 2, applyOpacity(0xFFFFFF55, opacity), true);
            y += CLAIM_PANEL_ROW_HEIGHT;
        }

        if (snapshot.claims().isEmpty()) {
            graphics.drawString(minecraft.font, Component.translatable("red_envelope.claim_list.empty"), left + 8, y + 2, applyOpacity(0xFFAAAAAA, opacity), true);
        }
    }

    private static void drawWrapped(GuiGraphics graphics, int x, int y, int width, String text, int color, float opacity) {
        int currentY = y;
        for (String line : wrapText(text, width)) {
            graphics.drawString(Minecraft.getInstance().font, line, x, currentY, applyOpacity(color, opacity), true);
            currentY += CARD_TEXT_LINE_HEIGHT;
        }
    }

    private static int getDesiredCardWidth(Minecraft minecraft, RedEnvelopeSnapshot snapshot) {
        int titleWidth = minecraft.font.width(makeTitleText(snapshot));
        int detailWidth = minecraft.font.width(makeDetailText(snapshot));
        if (snapshot != null && !snapshot.exclusiveUser().isBlank()) detailWidth += EXCLUSIVE_FACE_GAP;
        return Mth.clamp(CARD_TEXT_LEFT_OFFSET + Math.max(titleWidth, detailWidth) + CARD_RIGHT_PADDING, RED_ENV_DEFAULT_WIDTH, RED_ENV_MAX_WIDTH);
    }

    private static int getCardHeight(Minecraft minecraft, RedEnvelopeSnapshot snapshot, int cardWidth) {
        int textWidth = Math.max(20, cardWidth - CARD_TEXT_LEFT_OFFSET - CARD_RIGHT_PADDING);
        int titleLines = wrapText(makeTitleText(snapshot), textWidth).size();
        int detailWidth = snapshot != null && !snapshot.exclusiveUser().isBlank() ? Math.max(20, textWidth - EXCLUSIVE_FACE_GAP) : textWidth;
        int detailLines = wrapText(makeDetailText(snapshot), detailWidth).size();
        int detailHeight = Math.max(CARD_TEXT_LINE_HEIGHT, detailLines * CARD_TEXT_LINE_HEIGHT);
        return Math.max(RED_ENV_MIN_HEIGHT, CARD_VERTICAL_PADDING * 2 + titleLines * CARD_TEXT_LINE_HEIGHT + CARD_DETAIL_GAP + detailHeight);
    }

    private static List<String> wrapText(String text, int maxWidth) {
        Minecraft minecraft = Minecraft.getInstance();
        List<String> result = new ArrayList<>();
        if (text == null || text.isBlank()) return List.of("");
        int safeWidth = Math.max(1, maxWidth);
        StringBuilder current = new StringBuilder();
        text.codePoints().forEach(codePoint -> {
            String part = new String(Character.toChars(codePoint));
            if ("\n".equals(part)) {
                result.add(current.toString());
                current.setLength(0);
            } else {
                if (!current.isEmpty() && minecraft.font.width(current + part) > safeWidth) {
                    result.add(current.toString());
                    current.setLength(0);
                }
                current.append(part);
            }
        });
        if (!current.isEmpty() || result.isEmpty()) result.add(current.toString());
        return result;
    }

    private static int clamp(int value, int min, int max) {
        return max < min ? max : Mth.clamp(value, min, max);
    }

    private static String makeTitleText(RedEnvelopeSnapshot snapshot) {
        if (snapshot == null || snapshot.title().isBlank()) return Component.translatable("red_envelope.chat.card_title").getString();
        return snapshot.title();
    }

    private static String makeDetailText(RedEnvelopeSnapshot snapshot) {
        if (snapshot == null) return Component.translatable("red_envelope.chat.click").getString();
        if (snapshot.status() != RedEnvelopeStatus.ACTIVE) return Component.translatable("red_envelope.chat.finished").getString();
        if (snapshot.viewerClaimed()) return Component.translatable("red_envelope.chat.claimed", snapshot.claimedCount(), snapshot.playerCount()).getString();
        if (snapshot.usePassword()) return Component.translatable("red_envelope.chat.copy_password", snapshot.password().isBlank() ? "?" : snapshot.password()).getString();
        if (!snapshot.exclusiveUser().isBlank()) return Component.translatable("red_envelope.chat.exclusive_value", snapshot.exclusiveUser()).getString();
        return Component.translatable("red_envelope.chat.click", snapshot.claimedCount(), snapshot.playerCount()).getString();
    }

    private static int darken(int argb, int amount) {
        int a = argb >>> 24 & 0xFF;
        int r = Math.max(0, (argb >>> 16 & 0xFF) - amount);
        int g = Math.max(0, (argb >>> 8 & 0xFF) - amount);
        int b = Math.max(0, (argb & 0xFF) - amount);
        return a << 24 | r << 16 | g << 8 | b;
    }

    private static int lighten(int argb, int amount) {
        int a = argb >>> 24 & 0xFF;
        int r = Math.min(255, (argb >>> 16 & 0xFF) + amount);
        int g = Math.min(255, (argb >>> 8 & 0xFF) + amount);
        int b = Math.min(255, (argb & 0xFF) + amount);
        return a << 24 | r << 16 | g << 8 | b;
    }

    private static int desaturate(int argb) {
        int a = argb >>> 24 & 0xFF;
        int r = argb >>> 16 & 0xFF;
        int g = argb >>> 8 & 0xFF;
        int b = argb & 0xFF;
        int gray = (r + g + b) / 3;
        return a << 24 | (r + gray) / 2 << 16 | (g + gray) / 2 << 8 | (b + gray) / 2;
    }

    private static void renderPlayerHead(GuiGraphics graphics, String playerName, int x, int y, float opacity) {
        ResourceLocation skin = getPlayerSkin(playerName);
        graphics.setColor(1.0F, 1.0F, 1.0F, opacity);
        graphics.blit(skin, x, y, 8, 8, 8.0F, 8.0F, 8, 8, 64, 64);
        graphics.blit(skin, x, y, 8, 8, 40.0F, 8.0F, 8, 8, 64, 64);
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static ResourceLocation getPlayerSkin(String playerName) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getConnection() != null) {
            PlayerInfo info = minecraft.getConnection().getPlayerInfo(playerName);
            if (info != null) return info.getSkin().texture();
        }
        UUID fallback = UUID.nameUUIDFromBytes(("OfflinePlayer:" + playerName).getBytes(StandardCharsets.UTF_8));
        PlayerSkin skin = DefaultPlayerSkin.get(fallback);
        return skin.texture();
    }

    private static int applyOpacity(int argb, float opacity) {
        int alpha = Math.round((argb >>> 24 & 0xFF) * opacity);
        return argb & 0x00FFFFFF | alpha << 24;
    }

    private static String makeSenderText(RedEnvelopeSnapshot snapshot) {
        String sender = snapshot == null || snapshot.senderName().isBlank() ? "Server" : snapshot.senderName();
        return "<" + sender + "> ";
    }

    public static void addMessage_Inject(List<GuiMessage> allMessages, GuiMessage message) {
        List<GuiMessage> playerMessages = allMessages.stream().filter(existing -> existing.tag() == null).toList();
        if (playerMessages.isEmpty()) return;
        GuiMessage previous = playerMessages.getFirst();
        String previousText = extractRepeatBody(previous.content());
        String currentText = extractRepeatBody(message.content());
        if (!currentText.isBlank() && Objects.equals(currentText, previousText)) {
            GuiMessageExtraData extraData = new GuiMessageExtraData(Boolean.TRUE, Boolean.FALSE);
            extraData.repeatText = currentText;
            GuiMessageAttachment.put(message, extraData);
            GuiMessageAttachment.remove(previous);
        } else {
            GuiMessageAttachment.clearRepeatMarks();
        }
    }

    public static String extractRepeatBody(Component component) {
        if (component.getContents() instanceof TranslatableContents contents && "chat.type.text".equals(contents.getKey())) {
            Object[] args = contents.getArgs();
            if (args.length >= 2) {
                Object message = args[1];
                if (message instanceof Component messageComponent) return messageComponent.getString().trim();
                return String.valueOf(message).trim();
            }
        }
        String raw = component.getString().trim();
        Matcher matcher = VANILLA_PLAYER_MESSAGE.matcher(raw);
        return matcher.matches() ? matcher.group(1).trim() : raw;
    }
}
