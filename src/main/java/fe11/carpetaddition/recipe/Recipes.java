package fe11.carpetaddition.recipe;

import fe11.carpetaddition.FecaCarpetSettings;
import fe11.carpetaddition.third_party.recipe.AmsRecipeBuilder;
import fe11.carpetaddition.third_party.recipe.AmsRecipeManager;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static fe11.carpetaddition.Feca.MOD_ID;

public class Recipes {
    public static void registerCustomRecipes(Map<Identifier, Recipe<?>> map, HolderLookup.Provider wrapperLookup) {
        AmsRecipeManager amsRecipeManager = new AmsRecipeManager(AmsRecipeBuilder.getInstance());
        AmsRecipeManager.clearRecipeListMemory(AmsRecipeBuilder.getInstance());
        FecaCarpetSettings.buildRecipes();
        amsRecipeManager.registerRecipes(map, wrapperLookup);
    }

    private static boolean serverIsRunning(MinecraftServer server) {
        return server != null && server.isRunning();
    }

    public static void onPlayerLoggedIn(MinecraftServer server, ServerPlayer player) {
        if (serverIsRunning(server) && FecaCarpetSettings.hasRecipeRuleActivate()) {
            Collection<RecipeHolder<?>> allRecipes = getServerRecipeManager(server).getRecipes();
            for (RecipeHolder<?> recipe : allRecipes) {
                if (recipe.id().identifier().getNamespace().equals(MOD_ID) && !player.getRecipeBook().contains(recipe.id())) {
                    player.awardRecipes(List.of(recipe));
                }
            }
        }
    }

    public static void onValueChange(MinecraftServer server) {
        if (serverIsRunning(server)) {
            server.execute(() -> {
                AmsRecipeManager.clearRecipeListMemory(AmsRecipeBuilder.getInstance());
                FecaCarpetSettings.buildRecipes();
                reloadServerResources(server);
                Collection<RecipeHolder<?>> allRecipes = getServerRecipeManager(server).getRecipes();
                for (RecipeHolder<?> recipe : allRecipes) {
                    if (recipe.id().identifier().getNamespace().equals(MOD_ID)) {
                        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                            if (!player.getRecipeBook().contains(recipe.id())) {
                                player.awardRecipes(List.of(recipe));
                            }
                        }
                    }
                }
            });
        }
    }

    private static @NotNull RecipeManager getServerRecipeManager(@NotNull MinecraftServer server) {
        return server.getRecipeManager();
    }

    public static void reloadServerResources(@NotNull MinecraftServer server) {
        server.reloadResources(server.getPackRepository().getSelectedIds());
    }
}
