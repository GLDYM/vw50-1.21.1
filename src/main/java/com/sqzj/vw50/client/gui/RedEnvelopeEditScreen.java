package com.sqzj.vw50.client.gui;

import com.mojang.datafixers.util.Pair;
import com.sqzj.vw50.VW50;
import com.sqzj.vw50.client.menu.SendRedEnvelopeMenu;
import com.sqzj.vw50.common.envelope.RedEnvelopeStyleOptions;
import com.sqzj.vw50.client.widget.UniversalCheckbox;
import com.sqzj.vw50.server.network.SendRedEnvelopePayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RedEnvelopeEditScreen extends AbstractContainerScreen<SendRedEnvelopeMenu> {

    private static final int SCREEN_WIDTH = 208;
    private static final int SCREEN_HEIGHT = 231;

    private static final ResourceLocation LOCATION = VW50.prefix("textures/gui/send_red_envelope.png");
    private static final ResourceLocation COLOR_POPUP = VW50.prefix("textures/gui/send_red_envelope_select_color.png");
    private static final ResourceLocation ICON_POPUP = VW50.prefix("textures/gui/send_red_envelope_select_icon.png");
    private static final ResourceLocation CHECKBOX_CHECKED = VW50.prefix("small_checkbox_checked");
    private static final ResourceLocation CHECKBOX_UNCHECKED = VW50.prefix("small_checkbox_unchecked");

    private static final Component TOO_MANY_PLAYERS = Component.translatable("red_envelope.too_many_players").withStyle(ChatFormatting.RED);
    private static final Component PLAYER_MORE_THAN_ITEMS = Component.translatable("red_envelope.player_more_than_items").withStyle(ChatFormatting.RED);
    private static final Component LUCKY_MONEY = Component.translatable("red_envelope.lucky_money").withStyle(ChatFormatting.BOLD);
    private static final Component DESTROY_ON_EXPIRED = Component.translatable("red_envelope.destroy_on_expired").withStyle(ChatFormatting.BOLD);
    private static final Component ICON = Component.translatable("red_envelope.custom.icon").withStyle(ChatFormatting.BOLD);
    private static final Component COLOR = Component.translatable("red_envelope.custom.color").withStyle(ChatFormatting.BOLD);
    private static final Component SEND = Component.translatable("red_envelope.send").withStyle(ChatFormatting.BOLD);
    private static final Component HINT_NAME = Component.translatable("red_envelope.hint.name");
    private static final Component HINT_PASSWORD = Component.translatable("red_envelope.hint.password");

    private static final WidgetSprites SEND_BUTTON_SPRITES = new WidgetSprites(
            VW50.prefix("send_button_default"),
            VW50.prefix("send_button_disabled"),
            VW50.prefix("send_button_hover"));
    private static final WidgetSprites PROPERTY_BUTTON_SPRITES = new WidgetSprites(
            VW50.prefix("property_button_default"),
            VW50.prefix("property_button_disabled"),
            VW50.prefix("property_button_hover"));

    // Main texture coordinates. These are deliberately centralized so the UI can
    // be fine-tuned later without having to chase values through the class.
    private static final int PROPERTY_X = 154;
    private static final int PROPERTY_Y = 34;
    private static final int PROPERTY_W = 27;
    private static final int PROPERTY_H = 14;
    private static final int EXTRA_X = 120;
    private static final int EXTRA_Y = 51;
    private static final int EXTRA_W = 63;
    private static final int EXTRA_H = 16;
    private static final int LUCKY_X = 72;
    private static final int LUCKY_Y = 33;
    private static final int RETURN_X = 72;
    private static final int RETURN_Y = 51;
    private static final int SELECTOR_SIZE = 20;
    private static final int COLOR_SELECTOR_X = 46;
    private static final int COLOR_SELECTOR_Y = 73;
    private static final int ICON_SELECTOR_X = 94;
    private static final int ICON_SELECTOR_Y = 73;
    private static final int SEND_X = 123;
    private static final int SEND_Y = 74;
    private static final int SEND_W = 57; 
    private static final int SEND_H = 19;
    private static final int PREVIEW_X = 10;
    private static final int PREVIEW_Y = 104;
    private static final int PREVIEW_W = 188;
    private static final int PREVIEW_H = 34;

    private static final int COLOR_POPUP_W = 54;
    private static final int COLOR_POPUP_H = 34;
    private static final int ICON_POPUP_W = 94;
    private static final int ICON_POPUP_H = 55;

    private RedEnvelopeStyleCatalog styleCatalog;
    private EditBox titleBox;
    private EditBox numberBox;
    private EditBox nameBox;
    private ImageButton sendButton;
    private PropertyButton propertyButton;
    private boolean isLuckyMoney = true;
    private boolean returnWhenExpired = true;
    private boolean playerTooMany;
    private boolean playerMoreThanItems;
    private boolean colorPopupOpen;
    private boolean iconPopupOpen;
    private Property property = Property.NORMAL;
    private int selectedColorIndex = 1;
    private int selectedIconIndex;
    private int iconPage;

    public RedEnvelopeEditScreen(SendRedEnvelopeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = SCREEN_WIDTH;
        this.imageHeight = SCREEN_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        int x = this.leftPos;
        int y = this.topPos;
        Pair<ResourceLocation, ResourceLocation> sprites = Pair.of(CHECKBOX_CHECKED, CHECKBOX_UNCHECKED);
        this.styleCatalog = new RedEnvelopeStyleCatalog(this.minecraft.getResourceManager());
        this.selectedIconIndex = Math.clamp(this.selectedIconIndex, 0, this.styleCatalog.iconIdentifiers().size() - 1);
        this.titleBox = new EditBox(this.font, x + 49, y + 13, 110, 16, Component.empty());
        this.numberBox = new EditBox(this.font, x + 120, y + 33, 29, 16, Component.empty());
        this.nameBox = new EditBox(this.font, x + 120, y + 51, 61, 15, Component.empty());
        this.titleBox.setHint(Component.translatable("red_envelope.hint.title"));
        this.titleBox.setMaxLength(64);
        this.numberBox.setHint(Component.translatable("red_envelope.hint.number"));
        this.numberBox.setFilter(value -> value.matches("\\d+"));
        this.numberBox.setResponder(this::updateSendButtonState);
        this.nameBox.setFilter(value -> !value.startsWith("/"));
        this.nameBox.setMaxLength(64);
        this.nameBox.setResponder(ignored -> this.updateSendButtonState(this.numberBox.getValue()));
        this.sendButton = new ImageButton(x + SEND_X, y + SEND_Y, SEND_W, SEND_H, SEND_BUTTON_SPRITES, this::sendRedEnvelope);
        this.addRenderableWidget(new UniversalCheckbox(x + LUCKY_X, y + LUCKY_Y, 16, 16,
                sprites, true, (button, value) -> this.isLuckyMoney = value));
        this.addRenderableWidget(new UniversalCheckbox(x + RETURN_X, y + RETURN_Y, 16,
                sprites, (button, value) -> this.returnWhenExpired = value));
        this.propertyButton = new PropertyButton(x + PROPERTY_X, y + PROPERTY_Y);
        this.addRenderableWidget(this.propertyButton);
        List.of(this.titleBox, this.numberBox, this.nameBox, this.sendButton).forEach(this::addRenderableWidget);
        this.updateUIForType();
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        int luckyTextX = LUCKY_X - this.font.width(LUCKY_MONEY) - 3;
        int returnTextX = RETURN_X - this.font.width(DESTROY_ON_EXPIRED) - 3;
        graphics.drawString(this.font, LUCKY_MONEY, luckyTextX, LUCKY_Y + 4, -1);
        graphics.drawString(this.font, DESTROY_ON_EXPIRED, returnTextX, RETURN_Y + 4, -1);
        graphics.drawString(this.font, SEND, SEND_X + (SEND_W - this.font.width(SEND)) / 2, SEND_Y + 5, -1);
        graphics.drawString(this.font, COLOR, 25, 78, -1);
        graphics.drawString(this.font, ICON, 73, 78, -1);
        this.renderSelectors(graphics);
        this.renderPreview(graphics);
        this.renderNameSuggestions(graphics);
        this.renderPopups(graphics);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(LOCATION,
                this.leftPos, this.topPos, 0.0F, 0.0F,
                SCREEN_WIDTH, SCREEN_HEIGHT, 256, 256);
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderTooltip(graphics, mouseX, mouseY);
        if (this.playerTooMany && this.sendButton.isHovered()) {
            graphics.renderTooltip(this.font, TOO_MANY_PLAYERS, mouseX, mouseY);
        } else if (this.playerMoreThanItems && this.sendButton.isHovered()) {
            graphics.renderTooltip(this.font, PLAYER_MORE_THAN_ITEMS, mouseX, mouseY);
        }

        if (this.iconPopupOpen) {
            int hovered = this.hoveredIconIndex(mouseX, mouseY);
            if (hovered >= 0) {
                ResourceLocation identifier = this.styleCatalog.iconIdentifier(hovered);
                graphics.renderTooltip(this.font, Component.literal(identifier.toString()).withStyle(ChatFormatting.DARK_GRAY), mouseX, mouseY);
            }
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.titleBox.keyPressed(keyCode, scanCode, modifiers)
                || this.numberBox.keyPressed(keyCode, scanCode, modifiers)
                || this.nameBox.keyPressed(keyCode, scanCode, modifiers)) return true;
        return keyCode != 256 && (this.titleBox.isFocused() || this.numberBox.isFocused() || this.nameBox.isFocused())
                || super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseXValue, double mouseYValue, int button) {
        int mouseX = (int) mouseXValue;
        int mouseY = (int) mouseYValue;
        if (this.handleColorPopupClick(mouseX, mouseY)) return true;
        if (this.handleIconPopupClick(mouseX, mouseY)) return true;
        if (this.isInsideScreen(mouseX, mouseY, COLOR_SELECTOR_X, COLOR_SELECTOR_Y, SELECTOR_SIZE, SELECTOR_SIZE)) {
            this.colorPopupOpen = !this.colorPopupOpen;
            this.iconPopupOpen = false;
            return true;
        }

        if (this.isInsideScreen(mouseX, mouseY, ICON_SELECTOR_X, ICON_SELECTOR_Y, SELECTOR_SIZE, SELECTOR_SIZE)) {
            this.iconPopupOpen = !this.iconPopupOpen;
            this.colorPopupOpen = false;
            this.iconPage = this.selectedIconIndex / RedEnvelopeStyleOptions.ICONS_PER_PAGE;
            return true;
        }

        if (this.property == Property.EXCLUSIVE) {
            List<String> names = this.matchingOnlineNames();
            int sx = this.leftPos + EXTRA_X;
            int sy = this.topPos + EXTRA_Y + EXTRA_H + 1;
            for (int i = 0; i < names.size(); i++) {
                if (this.isInside(mouseX, mouseY, sx, sy + i * 12, 88, 12)) {
                    this.nameBox.setValue(names.get(i));
                    return true;
                }
            }
        }

        this.colorPopupOpen = false;
        this.iconPopupOpen = false;
        return super.mouseClicked(mouseXValue, mouseYValue, button);
    }

    @Override
    protected void containerTick() {
        this.updateUIForType();
    }

    private void renderSelectors(GuiGraphics graphics) {
        int color = this.selectedCardColor();
        graphics.fill(COLOR_SELECTOR_X + 2, COLOR_SELECTOR_Y + 2,
                COLOR_SELECTOR_X + SELECTOR_SIZE - 2, COLOR_SELECTOR_Y + SELECTOR_SIZE - 2, color);
        if ((color & 0x00FFFFFF) == 0x00FFFFFF) {
            this.drawOutline(graphics, COLOR_SELECTOR_X + 2, COLOR_SELECTOR_Y + 2,
                    COLOR_SELECTOR_X + SELECTOR_SIZE - 2, COLOR_SELECTOR_Y + SELECTOR_SIZE - 2, 0xFF777777);
        }

        this.renderIcon(graphics, this.selectedIconIdentifier(), ICON_SELECTOR_X + 2, ICON_SELECTOR_Y + 2, RedEnvelopeStyleOptions.ICON_TEXTURE_SIZE);
    }

    private void renderPreview(GuiGraphics graphics) {
        int color = this.selectedCardColor();
        graphics.fill(PREVIEW_X, PREVIEW_Y, PREVIEW_X + PREVIEW_W, PREVIEW_Y + PREVIEW_H, color);
        graphics.fill(PREVIEW_X + 2, PREVIEW_Y + 2, PREVIEW_X + PREVIEW_W - 2, PREVIEW_Y + PREVIEW_H - 2, this.darken(color, 36));
        this.renderIcon(graphics, this.selectedIconIdentifier(), PREVIEW_X + 8, PREVIEW_Y + 9,
                RedEnvelopeStyleOptions.ICON_TEXTURE_SIZE);
        String title = this.ellipsize(this.titleBox.getValue().isBlank()
                ? Component.translatable("red_envelope.default_title").getString()
                : this.titleBox.getValue(), 108);
        String detail = switch (this.property) {
            case PASSWORD -> this.ellipsize(Component.translatable("red_envelope.chat.copy_password",
                    this.nameBox.getValue().isBlank() ? "?" : this.nameBox.getValue()).getString(), 142);
            case EXCLUSIVE -> this.ellipsize(Component.translatable("red_envelope.chat.exclusive_value",
                    this.nameBox.getValue().isBlank() ? "?" : this.nameBox.getValue()).getString(), 142);
            case NORMAL -> Component.translatable("red_envelope.chat.click", 0, this.parsePositive(this.numberBox.getValue())).getString();
        };

        int primaryText = this.readableTextColor(color, 0xFFFFD27A);
        int secondaryText = this.readableTextColor(color, 0xFFFFFF88);
        graphics.drawString(this.font, title, PREVIEW_X + 30, PREVIEW_Y + 6, primaryText);
        graphics.drawString(this.font, detail, PREVIEW_X + 30, PREVIEW_Y + 19, secondaryText);
    }

    private void renderNameSuggestions(GuiGraphics graphics) {
        if (this.property != Property.EXCLUSIVE || !this.nameBox.isFocused()) return;
        List<String> names = this.matchingOnlineNames();
        int sx = EXTRA_X;
        int sy = EXTRA_Y + EXTRA_H + 1;
        for (int i = 0; i < names.size(); i++) {
            int y = sy + i * 12;
            graphics.fill(sx, y, sx + 88, y + 12, 0xEE2C1512);
            graphics.drawString(this.font, names.get(i), sx + 3, y + 2, -1);
        }
    }

    private void renderPopups(GuiGraphics graphics) {
        if (!this.colorPopupOpen && !this.iconPopupOpen) return;

        // Popup textures overlap the preview area. Keep the whole popup in a
        // higher GUI layer so text buffers cannot draw over its transparent edges.
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 100.0F);
        if (this.colorPopupOpen) {
            int x = this.colorPopupX();
            int y = this.popupY();
            graphics.blit(COLOR_POPUP,
                    x, y, 0.0F, 0.0F, COLOR_POPUP_W,
                    COLOR_POPUP_H, COLOR_POPUP_W, COLOR_POPUP_H);
            int localIndex = this.selectedColorIndex;
            int col = localIndex % 4;
            int row = localIndex / 4;
            this.drawOutline(graphics, x + 4 + col * 12, y + 8 + row * 12,
                    x + 13 + col * 12, y + 17 + row * 12, 0xFFFFFFFF);
        }

        if (this.iconPopupOpen) {
            int x = this.iconPopupX();
            int y = this.popupY();
            graphics.blit(ICON_POPUP,
                    x, y, 0.0F, 0.0F, ICON_POPUP_W, ICON_POPUP_H, ICON_POPUP_W, ICON_POPUP_H);
            int start = this.iconPage * RedEnvelopeStyleOptions.ICONS_PER_PAGE;
            for (int slot = 0; slot < RedEnvelopeStyleOptions.ICONS_PER_PAGE; slot++) {
                int index = start + slot;
                if (index >= this.styleCatalog.iconIdentifiers().size()) break;
                int slotX = x + 14 + (slot % 3) * 23;
                int slotY = y + 8 + (slot / 3) * 23;
                this.renderIcon(graphics, this.styleCatalog.iconIdentifier(index), slotX + 2, slotY + 2, RedEnvelopeStyleOptions.ICON_TEXTURE_SIZE);
                if (index == this.selectedIconIndex) {
                    this.drawOutline(graphics, slotX, slotY, slotX + 20, slotY + 20, 0xFFFFE08A);
                }
            }
        }
        graphics.pose().popPose();
    }

    private boolean handleColorPopupClick(int mouseX, int mouseY) {
        if (!this.colorPopupOpen) return false;
        int x = this.leftPos + this.colorPopupX();
        int y = this.topPos + this.popupY();
        for (int i = 0; i < this.styleCatalog.cardColors().size(); i++) {
            int col = i % 4;
            int row = i / 4;
            int sx = x + 4 + col * 12;
            int sy = y + 8 + row * 12;
            if (this.isInside(mouseX, mouseY, sx, sy, 10, 10)) {
                this.selectedColorIndex = i;
                this.colorPopupOpen = false;
                return true;
            }
        }

        return this.isInside(mouseX, mouseY, x, y, COLOR_POPUP_W, COLOR_POPUP_H);
    }

    private boolean handleIconPopupClick(int mouseX, int mouseY) {
        if (!this.iconPopupOpen) return false;
        int x = this.leftPos + this.iconPopupX();
        int y = this.topPos + this.popupY();
        if (this.isInside(mouseX, mouseY, x, y + 18, 14, 20)) {
            this.iconPage = Math.floorMod(this.iconPage - 1, this.styleCatalog.iconPageCount());
            return true;
        }

        if (this.isInside(mouseX, mouseY, x + 80, y + 18, 14, 20)) {
            this.iconPage = (this.iconPage + 1) % this.styleCatalog.iconPageCount();
            return true;
        }

        int start = this.iconPage * RedEnvelopeStyleOptions.ICONS_PER_PAGE;
        for (int slot = 0; slot < RedEnvelopeStyleOptions.ICONS_PER_PAGE; slot++) {
            int index = start + slot;
            if (index >= this.styleCatalog.iconIdentifiers().size()) break;
            int sx = x + 14 + (slot % 3) * 23;
            int sy = y + 8 + (slot / 3) * 23;
            if (this.isInside(mouseX, mouseY, sx, sy, 20, 20)) {
                this.selectedIconIndex = index;
                this.iconPopupOpen = false;
                return true;
            }
        }

        return this.isInside(mouseX, mouseY, x, y, ICON_POPUP_W, ICON_POPUP_H);
    }

    private int hoveredIconIndex(int mouseX, int mouseY) {
        int x = this.leftPos + this.iconPopupX();
        int y = this.topPos + this.popupY();
        int start = this.iconPage * RedEnvelopeStyleOptions.ICONS_PER_PAGE;
        for (int slot = 0; slot < RedEnvelopeStyleOptions.ICONS_PER_PAGE; slot++) {
            int index = start + slot;
            if (index >= this.styleCatalog.iconIdentifiers().size()) break;
            int sx = x + 14 + (slot % 3) * 23;
            int sy = y + 8 + (slot / 3) * 23;
            if (this.isInside(mouseX, mouseY, sx, sy, 20, 20)) return index;
        }

        return -1;
    }

    private List<Component> previewTooltip() {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("red_envelope.preview.full"));
        lines.add(Component.literal(this.titleBox.getValue().isBlank()
                ? Component.translatable("red_envelope.default_title").getString()
                : this.titleBox.getValue()).withStyle(ChatFormatting.GOLD));
        if (this.property == Property.PASSWORD) {
            lines.add(Component.translatable("red_envelope.chat.copy_password", this.nameBox.getValue()).withStyle(ChatFormatting.YELLOW));
        }

        if (this.property == Property.EXCLUSIVE) {
            lines.add(Component.translatable("red_envelope.chat.exclusive_value", this.nameBox.getValue()).withStyle(ChatFormatting.YELLOW));
        }

        lines.add(Component.translatable("red_envelope.preview.style", this.selectedIconIdentifier(), this.colorHex(this.selectedCardColor())).withStyle(ChatFormatting.GRAY));
        return lines;
    }

    private List<String> matchingOnlineNames() {
        if (this.minecraft.getConnection() == null) return List.of();
        String filter = this.nameBox.getValue().toLowerCase(Locale.ROOT);
        return this.minecraft.getConnection().getListedOnlinePlayers().stream()
                .map(PlayerInfo::getProfile).map(profile -> profile.getName())
                .filter(name -> filter.isBlank() || name.toLowerCase(Locale.ROOT).contains(filter))
                .limit(5).toList();
    }

    private void updateUIForType() {
        switch (this.property) {
            case NORMAL -> {
                this.numberBox.active = true;
                this.nameBox.visible = false;
            }
            case EXCLUSIVE -> {
                if (!"1".equals(this.numberBox.getValue())) this.numberBox.setValue("1");
                this.numberBox.active = false;
                this.nameBox.visible = true;
                this.nameBox.setHint(HINT_NAME);
            }
            case PASSWORD -> {
                this.numberBox.active = true;
                this.nameBox.visible = true;
                this.nameBox.setHint(HINT_PASSWORD);
            }
        }

        this.updateSendButtonState(this.numberBox.getValue());
    }

    private void updateSendButtonState(String number) {
        if (this.sendButton == null || this.numberBox == null) return;
        this.playerTooMany = false;
        this.playerMoreThanItems = false;
        int playerCount = this.parsePositive(number);
        int itemCount = this.getGiftCount();
        boolean hasProperty = this.property == Property.NORMAL || !this.nameBox.getValue().isBlank();
        boolean numberOk = this.property == Property.EXCLUSIVE || playerCount > 0;
        boolean countOk = itemCount > 0 && playerCount <= itemCount;
        boolean maxOk = playerCount <= 256;
        this.playerMoreThanItems = itemCount > 0 && playerCount > itemCount;
        this.playerTooMany = playerCount > 256;
        this.numberBox.setTextColor((numberOk && countOk && maxOk) ? -1 : -40864);
        this.sendButton.active = itemCount > 0 && numberOk && countOk && maxOk && hasProperty;
    }

    private int parsePositive(String number) {
        if (number == null || number.isBlank()) return 0;
        try {
            return Integer.parseInt(number);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private int getGiftCount() {
        return this.menu.giftSlot.getItem(0).getCount();
    }

    private void updateProperty(Property property) {
        this.nameBox.setValue("");
        if (property == Property.EXCLUSIVE) this.numberBox.setValue("1");
        else if (this.numberBox.getValue().equals("1") && this.property == Property.EXCLUSIVE) this.numberBox.setValue("");
        this.property = property;
        this.updateUIForType();
    }

    private void sendRedEnvelope(Button button) {
        String title = this.titleBox.getValue().trim();
        String propertyValue = this.nameBox.getValue().trim();
        int playerCount = this.property == Property.EXCLUSIVE ? 1 : this.parsePositive(this.numberBox.getValue());
        SendRedEnvelopePayload.PropertyType type = switch (this.property) {
            case NORMAL -> SendRedEnvelopePayload.PropertyType.NORMAL;
            case PASSWORD -> SendRedEnvelopePayload.PropertyType.PASSWORD;
            case EXCLUSIVE -> SendRedEnvelopePayload.PropertyType.EXCLUSIVE;
        };

        PacketDistributor.sendToServer(new SendRedEnvelopePayload(
                title, playerCount, this.isLuckyMoney, this.returnWhenExpired, type,
                propertyValue, this.selectedIconIdentifier(), this.selectedCardColor()));
    }

    private int selectedCardColor() {
        int safeIndex = Math.clamp(this.selectedColorIndex, 0, this.styleCatalog.cardColors().size() - 1);
        return this.styleCatalog.cardColors().get(safeIndex);
    }

    private ResourceLocation selectedIconIdentifier() {
        return this.styleCatalog.iconIdentifier(this.selectedIconIndex);
    }

    private void renderIcon(GuiGraphics graphics, ResourceLocation identifier, int x, int y, int size) {
        graphics.blit(identifier,
                x, y, 0.0F, 0.0F, size, size,
                RedEnvelopeStyleOptions.ICON_TEXTURE_SIZE,
                RedEnvelopeStyleOptions.ICON_TEXTURE_SIZE);
    }

    private int colorPopupX() {
        return COLOR_SELECTOR_X + SELECTOR_SIZE / 2 - COLOR_POPUP_W / 2;
    }

    private int iconPopupX() {
        return ICON_SELECTOR_X + SELECTOR_SIZE / 2 - ICON_POPUP_W / 2;
    }

    private int popupY() {
        return 94;
    }

    private String ellipsize(String text, int width) {
        if (this.font.width(text) <= width) return text;
        String suffix = "...";
        while (!text.isEmpty() && this.font.width(text + suffix) > width) {
            text = text.substring(0, text.length() - 1);
        }
        return text + suffix;
    }

    private int readableTextColor(int background, int fallback) {
        int r = (background >>> 16) & 0xFF;
        int g = (background >>> 8) & 0xFF;
        int b = background & 0xFF;
        return r * 299 + g * 587 + b * 114 > 180000 ? 0xFF4A120E : fallback;
    }

    private String colorHex(int color) {
        return String.format("#%06X", color & 0x00FFFFFF);
    }

    private boolean isInsideScreen(int mouseX, int mouseY, int x, int y, int width, int height) {
        return this.isInside(mouseX, mouseY, this.leftPos + x, this.topPos + y, width, height);
    }

    private boolean isInside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private void drawOutline(GuiGraphics graphics, int left, int top, int right, int bottom, int color) {
        graphics.fill(left, top, right, top + 1, color);
        graphics.fill(left, bottom - 1, right, bottom, color);
        graphics.fill(left, top, left + 1, bottom, color);
        graphics.fill(right - 1, top, right, bottom, color);
    }

    private int darken(int argb, int amount) {
        int a = (argb >>> 24) & 0xFF;
        int r = Math.max(0, ((argb >>> 16) & 0xFF) - amount);
        int g = Math.max(0, ((argb >>> 8) & 0xFF) - amount);
        int b = Math.max(0, (argb & 0xFF) - amount);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private enum Property implements StringRepresentable {
        NORMAL("normal"),
        EXCLUSIVE("exclusive"),
        PASSWORD("password");

        private final String name;

        Property(String name) {
            this.name = name;
        }

        public Component getDescription() {
            return Component.translatable(String.format("red_envelope.property.%s", this.name));
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }

    private final class PropertyButton extends AbstractButton {

        private PropertyButton(int x, int y) {
            super(x, y, PROPERTY_W, PROPERTY_H, Component.empty());
        }

        @Override
        public void onPress() {
            Property[] values = Property.values();
            RedEnvelopeEditScreen.this.updateProperty(values[(RedEnvelopeEditScreen.this.property.ordinal() + 1) % values.length]);
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            ResourceLocation sprite = PROPERTY_BUTTON_SPRITES.get(this.active, this.isHoveredOrFocused());
            graphics.blitSprite(sprite, this.getX(), this.getY(), this.width, this.height);
            graphics.drawCenteredString(RedEnvelopeEditScreen.this.font, RedEnvelopeEditScreen.this.property.getDescription(),
                    this.getX() + this.width / 2, this.getY() + 3, 0xFFFFFFFF);
        }

        @Override
        protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput output) {
            this.defaultButtonNarrationText(output);
        }
    }

}
