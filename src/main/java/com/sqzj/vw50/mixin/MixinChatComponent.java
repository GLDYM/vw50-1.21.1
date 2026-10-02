package com.sqzj.vw50.mixin;

import com.sqzj.vw50.misc.GuiMessageAttachment;
import com.sqzj.vw50.misc.GuiMessageExtraData;
import com.sqzj.vw50.misc.hook.HookChatComponent;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ChatComponent.class, remap = false)
public abstract class MixinChatComponent {

    @Inject(method = "clearMessages", at = @At(value = "HEAD", remap = false), remap = false)
    private void clearMessages(boolean clearHistory, CallbackInfo ci) {
        GuiMessageAttachment.clear();
    }

    @Inject(method = "addMessageToDisplayQueue", at = @At(value = "TAIL", remap = false), remap = false)
    private void addMessageToDisplayQueue(GuiMessage message, CallbackInfo ci) {
        HookChatComponent.onMessageDisplayed((ChatComponent) (Object) this, message);
    }

    @Inject(method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/ChatComponent;addMessageToDisplayQueue(Lnet/minecraft/client/GuiMessage;)V", remap = false), remap = false)
    private void beforeDisplayMessage(Component contents, MessageSignature signature, GuiMessageTag tag, CallbackInfo ci, @Local GuiMessage message) {
        ChatComponent chat = (ChatComponent) (Object) this;
        if (tag == null) {
            HookChatComponent.addMessage_Inject(chat.allMessages, message);
        }
    }

    @Inject(method = "render", at = @At(value = "TAIL", remap = false), remap = false)
    private void render(GuiGraphics graphics, int tick, int mouseX, int mouseY, boolean focused, CallbackInfo ci) {
        ChatComponent chat = (ChatComponent) (Object) this;
        HookChatComponent.renderChatOverlay(chat, graphics, tick, mouseX, mouseY, graphics.guiHeight(), focused);
    }

    @WrapOperation(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;III)I",
                    ordinal = 0,
                    remap = false),
            remap = false)
    private int suppressRedEnvelopePlaceholder(
            GuiGraphics graphics, Font font, FormattedCharSequence text, int x, int y, int color,
            Operation<Integer> original, @Local GuiMessage.Line line) {
        GuiMessageExtraData extraData = GuiMessageAttachment.get(line);
        if (extraData != null && extraData.isRedEnvelope) {
            return 0;
        }
        return original.call(graphics, font, text, x, y, color);
    }
}
