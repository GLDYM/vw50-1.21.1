package com.sqzj.vw50;

import com.sqzj.vw50.common.registry.VWAttachmentTypes;
import com.sqzj.vw50.common.registry.VWCreativeModeTabs;
import com.sqzj.vw50.common.registry.VWItems;
import com.sqzj.vw50.common.registry.VWMenus;
import com.sqzj.vw50.common.registry.VWParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

import java.util.Locale;

@Mod(VW50.MOD_ID)
public class VW50 {

    public static final String MOD_ID = "vw50";

    public VW50(IEventBus modEventBus) {
        VWAttachmentTypes.ATTACHMENT_TYPES.register(modEventBus);
        VWCreativeModeTabs.TABS.register(modEventBus);
        VWItems.ITEMS.register(modEventBus);
        VWMenus.MENUS.register(modEventBus);
        VWParticleTypes.PARTICLE_TYPES.register(modEventBus);
    }

    public static ResourceLocation prefix(String name) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, name.toLowerCase(Locale.ROOT));
    }

}