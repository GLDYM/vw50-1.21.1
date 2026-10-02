package com.sqzj.vw50.common.event;

import com.sqzj.vw50.VW50;
import com.sqzj.vw50.client.ClientRedEnvelopeManager;
import com.sqzj.vw50.server.network.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = VW50.MOD_ID)
public class ModBusEventSubscriber {

    @SubscribeEvent
    public static void registerNetworks(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(SendRedEnvelopePayload.TYPE, SendRedEnvelopePayload.STREAM_CODEC, ServerPayloadHandler::handleSendRedEnvelope);
        registrar.playToServer(ClaimRedEnvelopePayload.TYPE, ClaimRedEnvelopePayload.STREAM_CODEC, ServerPayloadHandler::handleClaimRedEnvelope);
        registrar.playToClient(RedEnvelopeSyncPayload.TYPE, RedEnvelopeSyncPayload.STREAM_CODEC, ClientRedEnvelopeManager::handleSync);
        registrar.playToClient(ClaimResultPayload.TYPE, ClaimResultPayload.STREAM_CODEC, ClientRedEnvelopeManager::handleClaimResult);
    }

}
