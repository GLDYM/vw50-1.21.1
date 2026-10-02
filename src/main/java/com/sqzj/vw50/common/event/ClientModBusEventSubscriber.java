package com.sqzj.vw50.common.event;

import com.sqzj.vw50.VW50;
import com.sqzj.vw50.client.gui.RedEnvelopeEditScreen;
import com.sqzj.vw50.client.particle.ItemIconParticle;
import com.sqzj.vw50.common.registry.VWItems;
import com.sqzj.vw50.common.registry.VWMenus;
import com.sqzj.vw50.common.registry.VWParticleTypes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

@EventBusSubscriber(modid = VW50.MOD_ID, value = Dist.CLIENT)
public class ClientModBusEventSubscriber {

    @SubscribeEvent
    public static void registerMenuScreens(RegisterMenuScreensEvent event) {
        event.register(VWMenus.SEND_RED_ENVELOPE_MENU.get(), RedEnvelopeEditScreen::new);
    }

    @SubscribeEvent
    public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpecial(VWParticleTypes.RED_ENVELOPE.get(),
            new ItemIconParticle.Provider(VWItems.EMPTY_RED_PACKET));
        event.registerSpecial(VWParticleTypes.YUANBAO.get(),
            new ItemIconParticle.Provider(VWItems.YUANBAO));
    }

}
