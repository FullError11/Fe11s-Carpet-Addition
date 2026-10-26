package feca.utils;

import com.mojang.datafixers.util.Pair;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;

@SuppressWarnings("unused")
public class PhysUtils {
    public static @NonNull Vec3 getAABBMinPos(@NonNull AABB aabb) {
        return aabb.getMinPosition();
    }
    public static @NonNull Vec3 getAABBMaxPos(@NonNull AABB aabb) {
        return aabb.getMaxPosition().add(-1);
    }

    /// 将 使用整数坐标的 AABB 转换为构成它的两个 BlockPos (这两个 BlockPos 可以直接构造为原 AABB)
    /// @return Pair<MinPos, MaxPos>
    @Contract("_ -> new")
    public static @NonNull Pair<BlockPos, BlockPos> intAABBToBlockPoss(@NonNull AABB aabb) {
        return Pair.of(intVec3ToBlockPos(getAABBMinPos(aabb)), intVec3ToBlockPos(getAABBMaxPos(aabb)));
    }

    @Contract("_ -> new")
    public static @NonNull Pair<String, String> intAABBToStrings(@NonNull AABB aabb) {
        return Pair.of(intVec3ToString(getAABBMinPos(aabb)), intVec3ToString(getAABBMinPos(aabb)));
    }

    public static @NonNull String blockPosToString(@NonNull BlockPos pos) {
        return String.format("%d %d %d", pos.getX(), pos.getY(), pos.getZ());
    }
    public static @NonNull String blockPosToSignedString(@NonNull BlockPos pos) {
        return String.format("%s %s %s", intToSignedString(pos.getX()), intToSignedString(pos.getY()), intToSignedString(pos.getZ()));
    }
    public static @NonNull Component blockPosToSignedComponent(@NonNull BlockPos pos) {
        return Component.empty()
                .append(intToSignedComponent(pos.getX()))
                .append(" ")
                .append(intToSignedComponent(pos.getY()))
                .append(" ")
                .append(intToSignedComponent(pos.getZ()));
    }
    public static @NonNull Component blockPosToSignedComponent(@NonNull BlockPos pos, TextColor color) {
        return Component.empty()
                .append(intToSignedComponent(pos.getX(), color))
                .append(" ")
                .append(intToSignedComponent(pos.getY(), color))
                .append(" ")
                .append(intToSignedComponent(pos.getZ(), color));
    }

    @Contract(value = "_ -> new", pure = true)
    private static @NonNull BlockPos intVec3ToBlockPos(@NonNull Vec3 vec3) {
        return new BlockPos((int)vec3.x, (int)vec3.y, (int)vec3.z);
    }

    @Contract("_ -> new")
    public static @NonNull String intVec3ToString(@NonNull Vec3 vec3) {
        return String.format("%d %d %d", (int)vec3.x, (int)vec3.y, (int)vec3.z);
    }



    private static String intToSignedString(int num) {
        if (num >= 0) {
            return String.format("+%d", num);
        }
        return String.valueOf(num);
    }
    private static Component intToSignedComponent(int num) {
        if (num >= 0) {
            return Component.literal("+").withStyle(ChatFormatting.GRAY).append(String.valueOf(num));
        }
        return Component.literal(String.valueOf(num));
    }
    private static Component intToSignedComponent(int num, TextColor color) {
        var numComponent = Component.literal(String.valueOf(num)).withColor(color);
        if (num >= 0) {
            return Component.literal("+")
                    .withStyle(ChatFormatting.GRAY)
                    .append(numComponent);
        }
        return numComponent;
    }
}
