package com.sqzj.vw50.client.gui;

import com.sqzj.vw50.common.envelope.RedEnvelopeStyleOptions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

import java.util.List;

/**
 * Discovers custom red-envelope icon textures from the active client resource
 * packs. Identifiers are sorted lexicographically, so numeric file-name
 * prefixes provide a stable display order without maintaining a Java list.
 */
public class RedEnvelopeStyleCatalog {

    private final List<ResourceLocation> iconIdentifiers;

    public RedEnvelopeStyleCatalog(ResourceManager resourceManager) {
        this.iconIdentifiers = this.loadIconIdentifiers(resourceManager);
    }

    public List<Integer> cardColors() {
        return RedEnvelopeStyleOptions.CARD_COLORS;
    }

    public List<ResourceLocation> iconIdentifiers() {
        return this.iconIdentifiers;
    }

    public int iconPageCount() {
        return Math.max(1, (this.iconIdentifiers.size() + RedEnvelopeStyleOptions.ICONS_PER_PAGE - 1) / RedEnvelopeStyleOptions.ICONS_PER_PAGE);
    }

    public ResourceLocation iconIdentifier(int index) {
        if (this.iconIdentifiers.isEmpty()) {
            return RedEnvelopeStyleOptions.DEFAULT_ICON_IDENTIFIER;
        }

        int safeIndex = Math.clamp(index, 0, this.iconIdentifiers.size() - 1);
        return this.iconIdentifiers.get(safeIndex);
    }

    private List<ResourceLocation> loadIconIdentifiers(ResourceManager resourceManager) {
        List<ResourceLocation> identifiers = resourceManager
                .listResources(RedEnvelopeStyleOptions.ICON_DIRECTORY,
                        identifier -> identifier.getPath().endsWith(".png"))
                .keySet().stream().sorted().toList();
        return identifiers.isEmpty() ? List.of(RedEnvelopeStyleOptions.DEFAULT_ICON_IDENTIFIER) : identifiers;
    }

}