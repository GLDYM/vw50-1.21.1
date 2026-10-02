package com.sqzj.vw50.common.envelope;

import com.sqzj.vw50.VW50;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public class RedEnvelopeStyleOptions {

    public static final int ICONS_PER_PAGE = 6;
    public static final int ICON_TEXTURE_SIZE = 16;
    public static final String ICON_DIRECTORY = "textures/gui/red_env_icons";
    public static final ResourceLocation DEFAULT_ICON_IDENTIFIER = VW50.prefix(ICON_DIRECTORY + "/red_envelope.png");

    public static final List<Integer> CARD_COLORS = List.of(
            0xFF9E1503,
            0xFFB82525,
            0xFFEE5050,
            0xFFFFFFFF,
            0xFF2589B8,
            0xFF27B825,
            0xFFFAE900,
            0xFFA617B5);

    public static ResourceLocation normalizeIconIdentifier(ResourceLocation identifier) {
        if (identifier == null || !identifier.getPath().endsWith(".png")) {
            return DEFAULT_ICON_IDENTIFIER;
        }
        return identifier;
    }

}