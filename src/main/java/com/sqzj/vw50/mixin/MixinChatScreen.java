package com.sqzj.vw50.mixin;

import com.sqzj.vw50.misc.hook.HookChatComponent;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ChatScreen.class, remap = false)
public class MixinChatScreen extends Screen {

    protected MixinChatScreen(Component title) {
        super(title);
    }

    @Inject(method = "mouseClicked", at = @At(value = "HEAD", remap = false), cancellable = true, remap = false)
    private void mouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (button != 0) return;
        ChatComponent chat = this.minecraft.gui.getChat();
        int screenHeight = this.minecraft.getWindow().getGuiScaledHeight();
        if (HookChatComponent.handleMouseClick(chat, screenHeight, (int) mouseX, (int) mouseY)) {
            cir.setReturnValue(true);
        }
    }
}
