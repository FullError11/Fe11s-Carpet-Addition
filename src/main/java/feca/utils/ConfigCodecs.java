package feca.utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.NonNull;

@SuppressWarnings("unused")
public class ConfigCodecs {
    public static class BlockPosCodec implements ConfigManager.ItemCodec<BlockPos> {
        @Override
        public JsonElement toJson(@NonNull BlockPos value) {
            var json = new JsonObject();
            json.addProperty("x", value.getX());
            json.addProperty("y", value.getY());
            json.addProperty("z", value.getZ());
            return json;
        }

        @Override
        public BlockPos fromJson(@NonNull JsonElement element) {
            var json = element.getAsJsonObject();
            return new BlockPos(
                    json.get("x").getAsInt(),
                    json.get("y").getAsInt(),
                    json.get("z").getAsInt()
            );
        }

        private BlockPosCodec() {}
        public static class Holder {
            public static final BlockPosCodec INSTANCE = new BlockPosCodec();
        }
    }

    public static class Vec3Codec implements ConfigManager.ItemCodec<Vec3> {
        @Override
        public JsonElement toJson(@NonNull Vec3 value) {
            var json = new JsonObject();
            json.addProperty("x", value.x);
            json.addProperty("y", value.y);
            json.addProperty("z", value.z);
            return json;
        }

        @Override
        public Vec3 fromJson(@NonNull JsonElement element) {
            var json = element.getAsJsonObject();
            return new Vec3(
                    json.get("x").getAsDouble(),
                    json.get("y").getAsDouble(),
                    json.get("z").getAsDouble()
            );
        }

        private Vec3Codec() {}
        public static class Holder {
            public static final Vec3Codec INSTANCE = new Vec3Codec();
        }
    }

    public static class AABBCodec implements ConfigManager.ItemCodec<AABB> {
        private final Vec3Codec vec3Codec = Vec3Codec.Holder.INSTANCE;

        @Override
        public JsonElement toJson(@NonNull AABB value) {
            var json = new JsonObject();
            json.add("min", vec3Codec.toJson(value.getMinPosition()));
            json.add("max", vec3Codec.toJson(value.getMaxPosition()));
            return json;
        }

        @Override
        public AABB fromJson(@NonNull JsonElement element) {
            var json = element.getAsJsonObject();
            return new AABB(
                    vec3Codec.fromJson(json.get("min")),
                    vec3Codec.fromJson(json.get("max"))
            );
        }

        private AABBCodec() {}
        public static class Holder {
            public static final AABBCodec INSTANCE = new AABBCodec();
        }
    }
}
