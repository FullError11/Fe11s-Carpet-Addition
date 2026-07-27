package feca.third_party.utils;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public class RegexTools {
    //state.getBlock.toString(); | Block{minecraft:bedrock} -> minecraft:bedrock
    @Contract(pure = true)
    public static @NotNull String getBlockRegisterName(@NotNull String sourceName) {
        return sourceName.replaceAll(".*\\{(.*?)}.*", "$1");
    }

    public static @NotNull String getBlockRegisterName(@NotNull BlockState blockState) {
        return blockState.getBlock().toString().replaceAll(".*\\{(.*?)}.*", "$1");
    }

    // itemStack.getItem.toString() | 5 minecraft:bedrock -> minecraft:bedrock
    @Contract(pure = true)
    public static @NotNull String getItemRegisterName(@NotNull String sourceName) {
        return sourceName.replaceAll(".*?(minecraft:[a-z_]+).*", "$1");
    }

    public static @NotNull String getItemRegisterName(@NotNull ItemStack itemStack) {
        return BuiltInRegistries.ITEM.getKey(itemStack.getItem()).toString().replaceAll(".*?(minecraft:[a-z_]+).*", "$1");
    }
}
