package feca.function;

import feca.command.ItemPickupRangeExtend;
import feca.rule.Rules;
import feca.rule.utils.RuleImpl;
import feca.utils.ClassForceLoader;
import feca.utils.mixin.PickupRangeHolder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.NonNull;

public class ItemPickupRangeExtendMax implements RuleImpl {
    static {
        ClassForceLoader.load(ItemPickupRangeExtend.class);
    }

    public static final int LIMIT_MAX = 64;

    public static int getValue() {
        return Rules.itemPickupRangeExtendMax;
    }

    /// @return 当前允许的最大额外水平拾取距离
    public static int limit() {
        return Math.clamp(getValue(), 0, LIMIT_MAX);
    }

    /// 玩家的 拾取范围拓展 设置
    public static @NonNull PickupRangeHolder range(@NonNull ServerPlayer player) {
        return ((PickupRangeHolder.Getter) player).feca$getPickupRange();
    }

    /// @return 该玩家实际的额外水平拾取距离
    public static int extraRangeOf(@NonNull ServerPlayer player) {
        if (limit() <= 0) return 0;
        return Math.min(range(player).getExtraRange(), limit());
    }

    /// 扩大拾取查询盒：只加水平方向(xz面)
    public static @NonNull AABB expandForPlayer(@NonNull Player player, @NonNull AABB searchBox) {
        if (!(player instanceof ServerPlayer serverPlayer)) return searchBox;

        final int extra = extraRangeOf(serverPlayer);
        if (extra <= 0) return searchBox;

        return searchBox.inflate(extra, 0.0, extra);
    }
}
