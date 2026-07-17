package fe11.carpetaddition.commands.utils;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import fe11.carpetaddition.Feca;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class AreaList {
    // Identifier _ = ResourceKey<Level>::identifier();
    final Map<Identifier, List<AABB>> areas;

    private AreaList(@NonNull Map<ResourceKey<Level>, List<AABB>> areas) {
        this.areas = Collections.unmodifiableMap(areas.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> e.getKey().identifier(),
                        Map.Entry::getValue,
                        (a, b) -> a,
                        HashMap::new
                ))
        );
    }

    @Contract(value = "_ -> new", pure = true)
    public static @NonNull AreaList newCustom(@NonNull Map<ResourceKey<Level>, List<AABB>> areas) {
        return new AreaList(areas);
    }

    @Contract(" -> new")
    public static @NonNull AreaList newDefault() {
        return new AreaList(Map.of(
                Level.OVERWORLD, new ArrayList<>(),
                Level.NETHER, new ArrayList<>(),
                Level.END, new ArrayList<>()
        ));
    }

    public void accessAll(@NonNull Consumer<Map<Identifier, List<AABB>>> consumer) {
        consumer.accept(this.areas);
    }

    public <T> @Nullable T access(Identifier dimension, Function<@NonNull List<AABB>, T> accessor) {
        try {
            return accessor.apply(areas.get(dimension));
        } catch (Exception ignored) {
            Feca.LOGGER.error("Invalid dimension: {}", dimension.toString());
            return null;
        }
    }

    public void access(Identifier dimension, Consumer<@NonNull List<AABB>> consumer) {
        this.access(dimension, list -> {
            consumer.accept(list);
            return null;
        });
    }

    public void access(@NonNull ResourceKey<Level> dimension, Consumer<@NonNull List<AABB>> consumer) {
        this.access(dimension.identifier(), consumer);
    }

    public boolean add(Identifier dimension, AABB aabb) {
        return Boolean.TRUE.equals(access(dimension, list -> {
            if (list.contains(aabb)) {
                return false;
            } else {
                return list.add(aabb);
            }
        }));
    }

    public boolean add(Identifier dimension, BlockPos pos1, BlockPos pos2) {
        return this.add(dimension, AABB.encapsulatingFullBlocks(pos1, pos2));
    }

    public boolean remove(Identifier dimension, AABB aabb) {
        return Boolean.TRUE.equals(access(dimension, list -> {
            return list.remove(aabb);
        }));
    }

    public boolean clearDimension(Identifier dimension) {
        var dimensionAreas = this.areas.get(dimension);
        if (dimensionAreas == null) return false;
        dimensionAreas.clear();
        return true;
    }

    public static class ArgumentUtil {
        String firstPosID, secondPosID;
        String dimensionID = "dimension";

        public ArgumentUtil(String firstID, String secondID) {
            this.firstPosID = firstID;
            this.secondPosID = secondID;
        }

        public RequiredArgumentBuilder<CommandSourceStack, Coordinates> require(Command<CommandSourceStack> execute) {
            return Commands.argument(firstPosID, BlockPosArgument.blockPos())
                    .then(Commands.argument(secondPosID, BlockPosArgument.blockPos())
                            .executes(execute)
                            .then(Commands.argument(dimensionID, DimensionArgument.dimension())
                                    .executes(execute)
                            )
                    );
        }

        private static Optional<BlockPos> getPos(String id, CommandContext<CommandSourceStack> ctx) {
            try {
                return Optional.of(BlockPosArgument.getBlockPos(ctx, id));
            } catch (IllegalArgumentException e) {
                return Optional.empty();
            }
        }

        public Optional<BlockPos> getFirstPos(CommandContext<CommandSourceStack> ctx) {
            return getPos(firstPosID, ctx);
        }
        public Optional<BlockPos> getSecondPos(CommandContext<CommandSourceStack> ctx) {
            return getPos(secondPosID, ctx);
        }
        public record PosPair(BlockPos first, BlockPos second) {
            public @NonNull AABB asAABB() {
                return AABB.encapsulatingFullBlocks(first, second);
            }
        }
        public Optional<PosPair> getPosSafely(CommandContext<CommandSourceStack> ctx) {
            var first = getFirstPos(ctx);
            var second = getSecondPos(ctx);
            return first.map(blockPos -> new PosPair(blockPos, second.orElse(blockPos)));
        }
        public ResourceKey<Level> getDimension(CommandContext<CommandSourceStack> ctx) {
            try {
                return DimensionArgument.getDimension(ctx, dimensionID).dimension();
            } catch (Exception ignored) {
                return ctx.getSource().getLevel().dimension();
            }
        }
    }
}
