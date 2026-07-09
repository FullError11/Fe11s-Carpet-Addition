/*
 * This file is part of the Carpet AMS Addition project, licensed under the
 * GNU Lesser General Public License v3.0
 *
 * Copyright (C) 2024 A Minecraft Server and contributors
 *
 * Carpet AMS Addition is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Carpet AMS Addition is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Carpet AMS Addition. If not, see <https://www.gnu.org/licenses/>.
 */

package fe11.carpetaddition.third_party.recipe;

import fe11.carpetaddition.third_party.recipe.template.ShapedRecipeTemplate;
import fe11.carpetaddition.third_party.recipe.template.ShapelessRecipeTemplate;
import fe11.carpetaddition.third_party.recipe.template.SmeltingRecipeTemplate;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import com.mojang.serialization.JsonOps;

import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AmsRecipeManager {
    private final List<ShapelessRecipeTemplate> shapelessRecipes;
    private final List<ShapedRecipeTemplate> shapedRecipes;
    private final List<SmeltingRecipeTemplate> smeltingRecipes;

    public AmsRecipeManager(@NotNull AmsRecipeBuilder builder) {
        this.shapelessRecipes = builder.getShapelessRecipeList();
        this.shapedRecipes = builder.getShapedRecipeList();
        this.smeltingRecipes = builder.getSmeltingRecipeList();
    }

    public void registerRecipes(Map<Identifier, Recipe<?>> map, HolderLookup.Provider wrapperLookup) {
        Map<Identifier, JsonElement> recipeMap = new HashMap<>();
        registerAllRecipes(recipeMap);
        recipeMap.forEach((id, json) -> addRecipe(map, wrapperLookup, id, json));
    }

    private void addRecipe(@NotNull Map<Identifier, Recipe<?>> map, HolderLookup.Provider wrapperLookup, Identifier id, @NotNull JsonElement json) {
        RecipeHolder<?> recipeEntry = this.deserializeRecipe(ResourceKey.create(Registries.RECIPE, id), json.getAsJsonObject(), wrapperLookup);
        map.put(id, recipeEntry.value());
    }

    @Contract("_, _, _ -> new")
    private @NotNull RecipeHolder<?> deserializeRecipe(ResourceKey<@NotNull Recipe<?>> key, JsonObject json, HolderLookup.@NotNull Provider registries) {
        return new RecipeHolder<>(key, Recipe.CODEC.parse(registries.createSerializationContext(JsonOps.INSTANCE), json).getOrThrow(JsonParseException::new));
    }

    private void registerAllRecipes(Map<Identifier, JsonElement> recipeMap) {
        shapelessRecipes.forEach(recipe -> recipe.addToRecipeMap(recipeMap));
        shapedRecipes.forEach(recipe -> recipe.addToRecipeMap(recipeMap));
        smeltingRecipes.forEach(recipe -> recipe.addToRecipeMap(recipeMap));
    }

    public static void clearRecipeListMemory(@NotNull AmsRecipeBuilder amsRecipeBuilder) {
        amsRecipeBuilder.getShapedRecipeList().clear();
        amsRecipeBuilder.getShapelessRecipeList().clear();
        amsRecipeBuilder.getSmeltingRecipeList().clear();
    }
}
