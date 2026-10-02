package com.sqzj.vw50.common.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.sqzj.vw50.common.envelope.RedEnvelopeRecord;
import com.sqzj.vw50.common.envelope.RedEnvelopeService;
import com.sqzj.vw50.common.envelope.RedEnvelopeStyleOptions;
import com.sqzj.vw50.server.network.RedEnvelopeSnapshot;
import com.sqzj.vw50.server.network.SendRedEnvelopePayload;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Comparator;

public class RedEnvelopeCommands {

    private static final SuggestionProvider<CommandSourceStack> ITEM_ID_SUGGESTIONS = (context, builder) -> {
        String remaining = builder.getRemainingLowerCase();
        BuiltInRegistries.ITEM.keySet().stream()
                .map(ResourceLocation::toString)
                .filter(identifier -> identifier.contains(remaining))
                .limit(80).forEach(builder::suggest);
        return builder.buildFuture();
    };

    private static final SuggestionProvider<CommandSourceStack> ICON_IDENTIFIER_SUGGESTIONS = (context, builder) -> {
        builder.suggest(RedEnvelopeStyleOptions.DEFAULT_ICON_IDENTIFIER.toString());
        return builder.buildFuture();
    };

    private static final SuggestionProvider<CommandSourceStack> COLOR_SUGGESTIONS = (context, builder) -> {
        RedEnvelopeStyleOptions.CARD_COLORS.stream()
                .map(RedEnvelopeCommands::colorToHex)
                .forEach(builder::suggest);
        return builder.buildFuture();
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("vw50")
                .then(Commands.literal("hand")
                        .then(Commands.argument("playerCount", IntegerArgumentType.integer(1, 256))
                                .executes(context -> sendHand(context.getSource(),
                                        IntegerArgumentType.getInteger(context, "playerCount"), ""))
                                .then(Commands.argument("label", StringArgumentType.greedyString())
                                        .executes(context -> sendHand(context.getSource(),
                                                IntegerArgumentType.getInteger(context, "playerCount"),
                                                StringArgumentType.getString(context, "label"))))))
                .then(Commands.literal("envelope")
                        .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("label", StringArgumentType.word())
                                .then(Commands.argument("item", StringArgumentType.word()).suggests(ITEM_ID_SUGGESTIONS)
                                        .then(Commands.argument("stackCount", IntegerArgumentType.integer(1, 999999))
                                                .then(Commands.argument("playerCount", IntegerArgumentType.integer(1, 256))
                                                        .executes(context -> sendEnvelopeCommand(context.getSource(),
                                                                StringArgumentType.getString(context, "label"),
                                                                StringArgumentType.getString(context, "item"),
                                                                IntegerArgumentType.getInteger(context, "stackCount"),
                                                                IntegerArgumentType.getInteger(context, "playerCount"),
                                                                RedEnvelopeStyleOptions.DEFAULT_ICON_IDENTIFIER,
                                                                RedEnvelopeSnapshot.DEFAULT_CARD_COLOR))
                                                        .then(Commands.argument("icon", StringArgumentType.word())
                                                                .suggests(ICON_IDENTIFIER_SUGGESTIONS)
                                                                .then(Commands.argument("color", StringArgumentType.word())
                                                                        .suggests(COLOR_SUGGESTIONS)
                                                                        .executes(context -> sendEnvelopeCommand(context.getSource(),
                                                                                StringArgumentType.getString(context, "label"),
                                                                                StringArgumentType.getString(context, "item"),
                                                                                IntegerArgumentType.getInteger(context, "stackCount"),
                                                                                IntegerArgumentType.getInteger(context, "playerCount"),
                                                                                parseIconIdentifier(context.getSource(),
                                                                                        StringArgumentType.getString(context, "icon")),
                                                                                parseColor(StringArgumentType.getString(context, "color")))))))))))
                .then(Commands.literal("history").executes(context -> showHistory(context.getSource())))
                .then(Commands.literal("permission").requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("cooldown")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("seconds", IntegerArgumentType.integer(0, 3600))
                                                .executes(context -> setLimit(context.getSource(),
                                                        EntityArgument.getPlayer(context, "player"),
                                                        IntegerArgumentType.getInteger(context, "seconds"), false)))))
                        .then(Commands.literal("block")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(context -> setLimit(context.getSource(),
                                                EntityArgument.getPlayer(context, "player"), 0, true))))
                        .then(Commands.literal("unblock")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(context -> setLimit(context.getSource(),
                                                EntityArgument.getPlayer(context, "player"), 0, false)))))
                .then(Commands.literal("repeatLimit").requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("maxPerMinute", IntegerArgumentType.integer(0, 120))
                                .executes(context -> setRepeatLimit(context.getSource(),
                                        IntegerArgumentType.getInteger(context, "maxPerMinute"),
                                        RedEnvelopeService.getRepeatMinIntervalMs(context.getSource().getServer())))
                                .then(Commands.argument("minIntervalMs", IntegerArgumentType.integer(0, 60000))
                                        .executes(context -> setRepeatLimit(context.getSource(),
                                                IntegerArgumentType.getInteger(context, "maxPerMinute"),
                                                IntegerArgumentType.getInteger(context, "minIntervalMs")))))));
    }

    private static int sendHand(CommandSourceStack source, int playerCount, String label) {
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception exception) {
            source.sendFailure(Component.translatable("red_envelope.error.player_only"));
            return 0;
        }
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) {
            source.sendFailure(Component.translatable("red_envelope.error.empty_stack"));
            return 0;
        }
        ItemStack gift = stack.copy();
        SendRedEnvelopePayload payload = SendRedEnvelopePayload.basic(label, playerCount, true, true,
                SendRedEnvelopePayload.PropertyType.NORMAL, "");
        RedEnvelopeService.CreateResult result = RedEnvelopeService.create(player, gift, payload, false, false);
        if (result.created() && !player.getAbilities().instabuild) {
            stack.shrink(gift.getCount());
        }
        return result.created() ? 1 : 0;
    }

    private static int sendEnvelopeCommand(CommandSourceStack source, String label, String itemId, int stackCount,
                                           int playerCount, ResourceLocation iconIdentifier, int color) {
        if (iconIdentifier == null) return 0;
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception exception) {
            source.sendFailure(Component.translatable("red_envelope.error.player_only"));
            return 0;
        }
        Item item = resolveItem(itemId);
        if (item == Items.AIR) {
            source.sendFailure(Component.translatable("red_envelope.error.bad_item"));
            return 0;
        }
        ItemStack gift = new ItemStack(item, stackCount);
        SendRedEnvelopePayload payload = new SendRedEnvelopePayload(label, playerCount, true, true,
                SendRedEnvelopePayload.PropertyType.NORMAL, "", iconIdentifier, color);
        RedEnvelopeService.CreateResult result = RedEnvelopeService.create(player, gift, payload, true, false);
        return result.created() ? 1 : 0;
    }

    private static int showHistory(CommandSourceStack source) {
        var data = RedEnvelopeService.getData(source.getServer());
        source.sendSuccess(() -> Component.translatable("red_envelope.history.header", data.envelopes.size())
                .withStyle(ChatFormatting.GOLD), false);
        data.envelopes.stream()
                .sorted(Comparator.comparingLong((RedEnvelopeRecord record) -> record.createdGameTime).reversed())
                .limit(8)
                .forEach(record -> source.sendSuccess(() -> Component.literal("#" + record.id + " " + record.title
                        + " " + record.status.getSerializedName() + " " + record.claims.size() + "/" + record.playerCount), false));
        return data.envelopes.size();
    }

    private static int setLimit(CommandSourceStack source, ServerPlayer target, int cooldownSeconds, boolean blocked) {
        ServerPlayer executor;
        try {
            executor = source.getPlayerOrException();
        } catch (Exception exception) {
            source.sendFailure(Component.translatable("red_envelope.error.player_only"));
            return 0;
        }
        RedEnvelopeService.setPlayerLimit(executor, target, cooldownSeconds, blocked);
        return 1;
    }

    private static int setRepeatLimit(CommandSourceStack source, int maxPerMinute, int minIntervalMs) {
        RedEnvelopeService.setRepeatLimit(source, maxPerMinute, minIntervalMs);
        return 1;
    }

    private static Item resolveItem(String rawId) {
        ResourceLocation identifier = ResourceLocation.tryParse(rawId == null ? "" : rawId);
        if (identifier == null) return Items.AIR;
        return BuiltInRegistries.ITEM.getOptional(identifier).orElse(Items.AIR);
    }

    private static ResourceLocation parseIconIdentifier(CommandSourceStack source, String rawIdentifier) {
        ResourceLocation identifier = ResourceLocation.tryParse(rawIdentifier == null ? "" : rawIdentifier);
        if (identifier == null || !identifier.getPath().endsWith(".png")) {
            source.sendFailure(Component.translatable("red_envelope.error.bad_icon"));
            return null;
        }
        return identifier;
    }

    private static int parseColor(String raw) {
        if (raw == null || raw.isBlank()) return RedEnvelopeSnapshot.DEFAULT_CARD_COLOR;
        String value = raw.trim();
        if (value.startsWith("#")) value = value.substring(1);
        if (value.startsWith("0x") || value.startsWith("0X")) value = value.substring(2);
        try {
            int color = (int)Long.parseLong(value, 16);
            return (color & 0xFF000000) == 0 ? 0xFF000000 | (color & 0x00FFFFFF) : color;
        } catch (NumberFormatException ignored) {
            return RedEnvelopeSnapshot.DEFAULT_CARD_COLOR;
        }
    }

    private static String colorToHex(int color) {
        return String.format("#%06X", color & 0x00FFFFFF);
    }
}
