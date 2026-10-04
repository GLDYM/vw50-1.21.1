package com.sqzj.vw50.mixin;

import com.sqzj.vw50.client.E33ChatCompat;
import com.sqzj.vw50.misc.hook.HookChatComponent;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.niuqu.chatbubble.render.ChatBubbleScreen", remap = false)
public abstract class MixinE33ChatBubbleScreen {

    @Inject(method = "render", at = @At("HEAD"), remap = false)
    private void beginFrame(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        E33ChatCompat.beginFrame(this);
    }

    @Inject(method = "render", at = @At("TAIL"), remap = false)
    private void renderClaimList(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        Screen screen = (Screen) (Object) this;
        HookChatComponent.OverlayBounds closeBounds = HookChatComponent.renderClaimListScreen(
            graphics, screen.width, screen.height, mouseX, mouseY);
        if (closeBounds != null) {
            E33ChatCompat.recordHitBox(graphics, this, closeBounds.left(), closeBounds.top(),
                closeBounds.width(), closeBounds.height(), null, null, true, mouseX, mouseY);
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true, remap = false)
    private void handleVW50Click(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (E33ChatCompat.handleMouseClick(this, mouseX, mouseY, button)) {
            cir.setReturnValue(true);
        }
    }
}
