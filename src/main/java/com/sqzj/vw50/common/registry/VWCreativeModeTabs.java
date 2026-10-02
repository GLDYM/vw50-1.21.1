package com.sqzj.vw50.common.registry;

import com.sqzj.vw50.VW50;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.stream.Stream;

public class VWCreativeModeTabs {

    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, VW50.MOD_ID);

    static {
        TABS.register("normal_tab", () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup." + VW50.MOD_ID))
                .icon(() -> new ItemStack(VWItems.EMPTY_RED_PACKET)).displayItems((parameters, output) -> {
                    Stream<DeferredHolder<Item, ? extends Item>> stream = VWItems.ITEMS.getEntries().stream();
                    stream.map(Holder::value).map(ItemLike::asItem).forEach(output::accept);
                }).build());
    }

}
