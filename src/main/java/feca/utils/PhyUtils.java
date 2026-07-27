package feca.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;

@SuppressWarnings("unused")
public class PhyUtils {
    public record BlockPoss(BlockPos min, BlockPos max) {
        @Contract("_ -> new")
        public static @NonNull BlockPoss of(@NonNull AABB aabb) {
            return new BlockPoss(
                    Vec3ToBlockPos(aabb.getMinPosition()),
                    Vec3ToBlockPos(aabb.getMaxPosition().add(-1))
            );
        }
    }

    @Contract(value = "_ -> new", pure = true)
    public static @NonNull BlockPos Vec3ToBlockPos(@NonNull Vec3 vec3) {
        return new BlockPos((int) vec3.x, (int) vec3.y, (int) vec3.z);
    }

    @Contract("_ -> new")
    public static @NonNull BlockPoss getBlockPos(AABB aabb) {
        return BlockPoss.of(aabb);
    }
}
