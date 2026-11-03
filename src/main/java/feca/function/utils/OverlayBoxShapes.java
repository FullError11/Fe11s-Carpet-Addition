package feca.function.utils;

import carpet.script.utils.ShapeDispatcher;
import carpet.script.value.ListValue;
import carpet.script.value.NumericValue;
import carpet.script.value.StringValue;
import carpet.script.value.Value;
import feca.FeCA;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.*;

/// 把一个区域编码成 carpet 的 scarpet {@code box} 形状并下发。
final class OverlayBoxShapes {
    private OverlayBoxShapes() {}

    /// 下发一组区域
    static void send(@NonNull MinecraftServer server, @NonNull ServerPlayer player,
                     @NonNull ResourceKey<Level> dimension, @NonNull Collection<AABB> boxes, int durationTicks) {
        if (boxes.isEmpty()) return;

        final List<ShapeDispatcher.ShapeWithConfig> shapes = new ArrayList<>(boxes.size());
        for (AABB box : boxes) {
            final var shape = create(server, box, dimension, durationTicks);
            if (shape != null) shapes.add(shape);
        }
        ShapeDispatcher.sendShape(List.of(player), shapes, server.registryAccess());
    }

    private static ShapeDispatcher.@Nullable ShapeWithConfig create(
            @NonNull MinecraftServer server, @NonNull AABB box,
            @NonNull ResourceKey<Level> dimension, int durationTicks) {
        final Map<String, Value> params = new HashMap<>(8);
        params.put("from", ListValue.ofNums(box.minX, box.minY, box.minZ));
        params.put("to", ListValue.ofNums(box.maxX, box.maxY, box.maxZ));
        params.put("dim", new StringValue(dimension.identifier().toString()));
        params.put("duration", new NumericValue(durationTicks));
        params.put("color", new NumericValue(0xFF3355FFL));
        params.put("line", new NumericValue(2.0));

        try {
            // create 会把 "shape" 写进 params，并把校验后的值放回同一个 map
            return new ShapeDispatcher.ShapeWithConfig(ShapeDispatcher.create(server, "box", params), params);
        } catch (RuntimeException e) {
            FeCA.LOGGER.warn("<ObserverFreezeAreasOverlay> failed to encode box shape for {} in {}",
                    box, dimension.identifier(), e);
            return null;
        }
    }
}
