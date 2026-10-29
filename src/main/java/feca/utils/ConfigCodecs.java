package feca.utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

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

        protected BlockPosCodec() {}
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

        protected Vec3Codec() {}
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

        protected AABBCodec() {}
        public static class Holder {
            public static final AABBCodec INSTANCE = new AABBCodec();
        }
    }

    public static class NullableCodec<T> implements ConfigManager.ItemCodec<@NonNull T> {
        private final ConfigManager.ItemCodec<T> itemCodec;

        public NullableCodec(ConfigManager.ItemCodec<T> itemCodec) {
            this.itemCodec = itemCodec;
        }

        @Contract(value = "_ -> new", pure = true)
        public static <T> @NonNull NullableCodec<T> of(ConfigManager.ItemCodec<T> itemCodec) {
            return new NullableCodec<>(itemCodec);
        }

        @Override
        public JsonElement toJson(@Nullable T value) {
            return value == null ? JsonNull.INSTANCE : itemCodec.toJson(value);
        }

        @Override
        public @Nullable T fromJson(@Nullable JsonElement element) {
            return (element == null || element.isJsonNull()) ? null : itemCodec.fromJson(element);
        }
    }

    public static class OptionalCodec<T> implements ConfigManager.ItemCodec<Optional<T>> {
        private final ConfigManager.ItemCodec<T> itemCodec;

        public OptionalCodec(ConfigManager.ItemCodec<T> itemCodec) {
            this.itemCodec = itemCodec;
        }

        @Override
        public JsonElement toJson(@NonNull Optional<T> value) {
            return value.map(itemCodec::toJson).orElse(JsonNull.INSTANCE);
        }

        @Override
        public Optional<T> fromJson(@Nullable JsonElement element) {
            return (element == null || element.isJsonNull())
                    ? Optional.empty()
                    : Optional.of(itemCodec.fromJson(element));
        }
    }
}
