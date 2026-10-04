package com.sqzj.vw50.mixin;

import com.sqzj.vw50.client.E33ChatCompat;
import com.sqzj.vw50.misc.GuiMessageExtraData;
import com.sqzj.vw50.misc.hook.HookChatComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Pseudo
@Mixin(targets = "com.niuqu.chatbubble.render.ChatMessageRenderer", remap = false)
public abstract class MixinE33ChatMessageRenderer {

    @Inject(method = "renderBubble", at = @At("HEAD"), cancellable = true, remap = false)
    private static void renderEnvelope(GuiGraphics graphics, Font font, @Coerce Object message,
                                       int index, int baseY, int mouseX, int mouseY, int panelX, int panelWidth,
                                       int ownBubbleColor, int otherBubbleColor, int ownTextColor, int otherTextColor,
                                       boolean own, int cornerRadius, @Coerce Object colors, ResourceLocation skin,
                                       int searchHighlightIndex, int bubbleMaxWidth, List<int[]> bubbleRects,
                                       List<?> clickableSpans, List<?> textSpans, @Coerce Object selection,
                                       float alpha, boolean drawSelection, CallbackInfo ci) {
        GuiMessageExtraData data = E33ChatCompat.getMessageData(message);
        if (data == null) {
            if (E33ChatCompat.isLegacyEnvelopePlaceholder(message)) ci.cancel();
            return;
        }
        if (!data.isRedEnvelope) return;

        HookChatComponent.OverlayBounds bounds = HookChatComponent.renderEnvelopeBubble(
            graphics, data, panelX, panelWidth, baseY, alpha, false);
        boolean hovered = E33ChatCompat.isMouseOver(graphics, bounds.left(), bounds.top(),
            bounds.width(), bounds.height(), mouseX, mouseY);
        if (hovered) {
            bounds = HookChatComponent.renderEnvelopeBubble(graphics, data, panelX, panelWidth, baseY, alpha, true);
        }
        E33ChatCompat.recordHitBox(graphics, Minecraft.getInstance().screen, bounds.left(), bounds.top(),
            bounds.width(), bounds.height(), data.redEnvelopeId, null, false, mouseX, mouseY);
        ci.cancel();
    }

    @Inject(method = "renderBubble", at = @At("RETURN"), remap = false)
    private static void renderVW50Actions(GuiGraphics graphics, Font font, @Coerce Object message,
                                          int index, int baseY, int mouseX, int mouseY, int panelX, int panelWidth,
                                          int ownBubbleColor, int otherBubbleColor, int ownTextColor, int otherTextColor,
                                          boolean own, int cornerRadius, @Coerce Object colors, ResourceLocation skin,
                                          int searchHighlightIndex, int bubbleMaxWidth, List<int[]> bubbleRects,
                                          List<?> clickableSpans, List<?> textSpans, @Coerce Object selection,
                                          float alpha, boolean drawSelection, CallbackInfo ci) {
        GuiMessageExtraData data = E33ChatCompat.getMessageData(message);
        if (data != null && data.isRedEnvelopeFinishNotice) {
            Component content = E33ChatCompat.getMessageContent(message);
            if (content == null) return;
            List<FormattedCharSequence> lines = font.split(content, Math.max(1, panelWidth - 36));
            int textWidth = lines.stream().mapToInt(font::width).max().orElse(0);
            int left = panelX + (panelWidth - textWidth) / 2;
            int top = baseY + 2;
            boolean hovered = E33ChatCompat.recordHitBox(graphics, Minecraft.getInstance().screen,
                left, top, textWidth, lines.size() * 9, data.redEnvelopeId, null, false, mouseX, mouseY);
            if (hovered) graphics.renderOutline(left - 2, top - 2, textWidth + 4, lines.size() * 9 + 4, 0xFFFFD27A);
            return;
        }

        GuiMessageExtraData repeatData = E33ChatCompat.getRepeatData(message);
        if (repeatData == null || bubbleRects == null) return;
        for (int i = bubbleRects.size() - 1; i >= 0; i--) {
            int[] rect = bubbleRects.get(i);
            if (rect.length < 5 || rect[4] != index) continue;
            int left = rect[0] + rect[2] - 8;
            int top = rect[1] - 4;
            boolean hovered = E33ChatCompat.recordHitBox(graphics, Minecraft.getInstance().screen,
                left, top, 9, 9, null, repeatData.repeatText, false, mouseX, mouseY);
            graphics.blitSprite(HookChatComponent.PLUS_ONE_SPRITES.get(true, hovered), left, top, 9, 9);
            break;
        }
    }
}
