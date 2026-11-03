package feca.test.component.utils;

import com.google.gson.JsonElement;
import feca.utils.ConfigCodecs;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 组件测试: {@link ConfigCodecs}（世界配置 {@code FeCAConfig.json} 的 JSON 编解码）。
 *
 * <p>契约: 所有 codec 必须是可往返的 —— 配置文件的持久化格式一旦失配，
 * 重启后玩家的数据（冻结区域等）就会丢失。</p>
 */
class ConfigCodecsTest {
    @Test
    @DisplayName("BlockPos 往返")
    void blockPosRoundTrip() {
        final var codec = ConfigCodecs.BlockPosCodec.Holder.INSTANCE;
        final BlockPos value = new BlockPos(-12, 64, 300);
        assertEquals(value, codec.fromJson(codec.toJson(value)));
    }

    @Test
    @DisplayName("Vec3 往返（保留小数）")
    void vec3RoundTrip() {
        final var codec = ConfigCodecs.Vec3Codec.Holder.INSTANCE;
        final Vec3 value = new Vec3(-1.5, 64.25, 0.0);
        assertEquals(value, codec.fromJson(codec.toJson(value)));
    }

    @Test
    @DisplayName("AABB 往返")
    void aabbRoundTrip() {
        final var codec = ConfigCodecs.AABBCodec.Holder.INSTANCE;
        final AABB value = new AABB(-3.5, 60.0, 7.25, 8.5, 71.0, 19.75);
        assertEquals(value, codec.fromJson(codec.toJson(value)));
    }

    @Test
    @DisplayName("NullableCodec：null 与值都能往返")
    void nullableRoundTrip() {
        final var codec = ConfigCodecs.NullableCodec.of(ConfigCodecs.BlockPosCodec.Holder.INSTANCE);

        assertEquals(null, codec.fromJson(codec.toJson(null)), "null 必须序列化为 JsonNull 再还原为 null");

        final BlockPos value = new BlockPos(1, 2, 3);
        assertEquals(value, codec.fromJson(codec.toJson(value)));
    }

    @Test
    @DisplayName("OptionalCodec：空与非空都能往返")
    void optionalRoundTrip() {
        final var codec = new ConfigCodecs.OptionalCodec<>(ConfigCodecs.Vec3Codec.Holder.INSTANCE);

        assertEquals(java.util.Optional.empty(), codec.fromJson(codec.toJson(java.util.Optional.empty())));
        assertEquals(java.util.Optional.of(new Vec3(1, 2, 3)),
                codec.fromJson(codec.toJson(java.util.Optional.of(new Vec3(1, 2, 3)))));
    }

    @Test
    @DisplayName("序列化结果是 JSON 对象（便于人工查看/编辑）")
    void serializesAsObject() {
        final JsonElement json = ConfigCodecs.AABBCodec.Holder.INSTANCE.toJson(new AABB(0, 0, 0, 1, 1, 1));
        assertEquals(true, json.isJsonObject(), "AABB 应序列化为对象而不是字符串");
    }
}
