package feca.config.utils;

import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.CheckReturnValue;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;

import java.util.*;
import java.util.function.Function;

public class AreaList {
    /// Identifier _ = ResourceKey<Level>::identifier();
    final Map<Identifier, List<AABB>> areas;

    private AreaList(@NonNull Map<ResourceKey<Level>, List<AABB>> areas) {
        Map<Identifier, List<AABB>> copy = new HashMap<>();
        areas.forEach((key, value) -> copy.put(key.identifier(), new ArrayList<>(value)));
        this.areas = Collections.unmodifiableMap(copy);
    }

    @SuppressWarnings("unused")
    @Contract(value = "_ -> new", pure = true)
    public static @NonNull AreaList newCustom(@NonNull Map<ResourceKey<Level>, List<AABB>> areas) {
        return new AreaList(areas);
    }

    @SuppressWarnings("unused")
    @Contract(" -> new")
    public static @NonNull AreaList newDefault() {
        return new AreaList(Map.of(
                Level.OVERWORLD, new ArrayList<>(),
                Level.NETHER, new ArrayList<>(),
                Level.END, new ArrayList<>()
        ));
    }

    @CheckReturnValue
    public Map<Identifier, List<AABB>> getAreas() {
        return areas;
    }

    @CheckReturnValue
    public Optional<List<AABB>> getDimension(Identifier identifier) {
        return this.access(identifier, aabbs -> aabbs);
    }

    private <T> @NonNull Optional<T> access(Identifier dimension, @NonNull Function<@NonNull List<AABB>, T> accessor) {
        var area = areas.get(dimension);
        if (area == null) return Optional.empty();
        return Optional.ofNullable(accessor.apply(area));
    }

    public enum Result {
        Successful,
        ObjectAlreadyExists,
        ObjectDoesNotExist,
        DimensionNotExist;

        public boolean successful() {
            return this == Successful;
        }
    }

    public Result addIfNotContains(Identifier dimension, AABB aabb) {
        return this.access(dimension, list -> {
            if (list.contains(aabb)) {
                return Result.ObjectAlreadyExists;
            } else {
                list.add(aabb);
                return Result.Successful;
            }
        }).orElse(Result.DimensionNotExist);
    }

    public Result removeIfContains(Identifier dimension, AABB aabb) {
        return this.access(dimension, list -> {
            // remove 本身就会在元素不存在时返回false
            if (list.remove(aabb)) {
                return Result.Successful;
            } else {
                return Result.ObjectDoesNotExist;
            }
        }).orElse(Result.DimensionNotExist);
    }

    public Result clearDimension(Identifier dimension) {
        return this.access(dimension, list -> {
            list.clear();
            return Result.Successful;
        }).orElse(Result.DimensionNotExist);
    }
}
