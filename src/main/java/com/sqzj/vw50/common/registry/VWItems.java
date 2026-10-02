package com.sqzj.vw50.common.registry;

import com.sqzj.vw50.VW50;
import com.sqzj.vw50.common.items.EmptyRedEnvelope;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Function;

public class VWItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(VW50.MOD_ID);
    public static final DeferredHolder<Item, ? extends Item> EMPTY_RED_PACKET = register("empty_red_envelope", EmptyRedEnvelope::new);
    public static final DeferredHolder<Item, ? extends Item> YUANBAO = register("yuanbao", Item::new);

    public static DeferredHolder<Item, ? extends Item> register(
            String name, Function<Item.Properties, Item> itemFactory) {
        return register(name, itemFactory, Item.Properties::new);
    }

    public static DeferredHolder<Item, ? extends Item> register(
            String name, Function<Item.Properties, Item> itemFactory, java.util.function.Supplier<Item.Properties> properties) {
        return ITEMS.registerItem(name, itemFactory, properties.get());
    }

}
