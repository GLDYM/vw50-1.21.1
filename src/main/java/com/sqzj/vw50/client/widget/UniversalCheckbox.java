package com.sqzj.vw50.client.widget;

import com.mojang.datafixers.util.Pair;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class UniversalCheckbox extends AbstractButton {

    private boolean selected;
    private final Pair<ResourceLocation, ResourceLocation> sprite;
    private final OnValueChange onValueChange;

    public UniversalCheckbox(int x, int y, int size, Pair<ResourceLocation, ResourceLocation> sprite, OnValueChange onValueChange) {
        this(x, y, size, size, sprite, false, onValueChange);
    }

    public UniversalCheckbox(
            int x, int y, int width, int height, Pair<ResourceLocation, ResourceLocation> sprite,
            boolean selected, OnValueChange onValueChange) {
        super(x, y, width, height, Component.empty());
        this.sprite = sprite;
        this.selected = selected;
        this.onValueChange = onValueChange;
    }

    @Override
    public void onPress() {
        this.selected = !this.selected;
        this.onValueChange.onValueChange(this, this.selected);
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        ResourceLocation sprite = this.selected ? this.sprite.getFirst() : this.sprite.getSecond();
        graphics.blitSprite(sprite, this.getX(), this.getY(), this.width, this.height);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {

    }

    public interface OnValueChange {

        OnValueChange NOP = (checkbox, value) -> {};

        void onValueChange(UniversalCheckbox checkbox, boolean value);

    }

}