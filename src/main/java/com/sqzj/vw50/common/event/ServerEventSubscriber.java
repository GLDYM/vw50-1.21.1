package com.sqzj.vw50.common.event;

import com.sqzj.vw50.VW50;
import com.sqzj.vw50.common.command.RedEnvelopeCommands;
import com.sqzj.vw50.common.envelope.RedEnvelopeService;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = VW50.MOD_ID)
public class ServerEventSubscriber {

    private static final Map<UUID, Deque<Long>> REPEAT_TIMES = new HashMap<>();
    private static final Map<UUID, Long> LAST_REPEAT_TIME = new HashMap<>();
    private static String lastPlayerMessage = "";

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        RedEnvelopeCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onServerChatSubmitted(ServerChatEvent event) {
        // Password messages should remain normal public chat messages.  This lets
        // other players see and repeat the password, while still queueing the claim.
        RedEnvelopeService.tryPasswordClaim(event.getPlayer(), event.getRawText());
        if (event.getRawText().equals(lastPlayerMessage)) {
            long now = System.currentTimeMillis();
            int maxPerMinute = RedEnvelopeService.getRepeatMaxPerMinute(event.getPlayer().server);
            int minIntervalMs = RedEnvelopeService.getRepeatMinIntervalMs(event.getPlayer().server);
            if (maxPerMinute > 0 || minIntervalMs > 0) {
                Deque<Long> times = REPEAT_TIMES.computeIfAbsent(event.getPlayer().getUUID(), uuid -> new ArrayDeque<>());
                while (!times.isEmpty() && now - times.peekFirst() > 60_000L) times.removeFirst();
                long last = LAST_REPEAT_TIME.getOrDefault(event.getPlayer().getUUID(), Long.MIN_VALUE / 2L);
                boolean tooMany = maxPerMinute > 0 && times.size() >= maxPerMinute;
                boolean tooFast = minIntervalMs > 0 && now - last < minIntervalMs;
                if (tooMany || tooFast) {
                    event.getPlayer().sendSystemMessage(Component.translatable("repeat.too_fast").withStyle(ChatFormatting.RED), true);
                    event.setCanceled(true);
                    return;
                }

                times.addLast(now);
                LAST_REPEAT_TIME.put(event.getPlayer().getUUID(), now);
            }
        } else {
            lastPlayerMessage = event.getRawText();
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        RedEnvelopeService.tick(event.getServer());
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
            RedEnvelopeService.deliverPendingReturns(player);
            RedEnvelopeService.syncActiveTo(player);
        }
    }

}
