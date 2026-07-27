package feca.network.payload;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;

public class AABBCodec {
    public static final StreamCodec<RegistryFriendlyByteBuf, AABB> CODEC = new StreamCodec<>() {
        @Override
        public void encode(@NotNull RegistryFriendlyByteBuf buf, @NotNull AABB aabb) {
            buf.writeDouble(aabb.minX);
            buf.writeDouble(aabb.minY);
            buf.writeDouble(aabb.minZ);
            buf.writeDouble(aabb.maxX);
            buf.writeDouble(aabb.maxY);
            buf.writeDouble(aabb.maxZ);
        }

        @Override
        public @NotNull AABB decode(@NotNull RegistryFriendlyByteBuf buf) {
            double minX = buf.readDouble();
            double minY = buf.readDouble();
            double minZ = buf.readDouble();
            double maxX = buf.readDouble();
            double maxY = buf.readDouble();
            double maxZ = buf.readDouble();
            return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
        }
    };
}