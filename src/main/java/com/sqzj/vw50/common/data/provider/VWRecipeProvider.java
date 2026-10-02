package com.sqzj.vw50.common.data.provider;

import com.sqzj.vw50.common.registry.VWItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.world.item.Items;

import java.util.concurrent.CompletableFuture;

public class VWRecipeProvider extends RecipeProvider {

    public VWRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput output) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, VWItems.EMPTY_RED_PACKET.get(), 4)
                .requires(Items.PAPER, 2).requires(Items.RED_DYE)
                .unlockedBy(getHasName(Items.PAPER), this.has(Items.PAPER))
                .save(output);
    }

}