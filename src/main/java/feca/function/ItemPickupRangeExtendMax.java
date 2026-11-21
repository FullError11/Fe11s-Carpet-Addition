package feca.function;

import feca.command.ItemPickupRangeExtend;
import feca.rule.Rules;
import feca.rule.utils.RuleImpl;
import feca.rule.validators.PickupRangeExtendMaxValidator;
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

    public static final float LIMIT_MAX = PickupRangeExtendMaxValidator.LIMIT_MAX;

    public static float getValue() {
        return Rules.itemPickupRangeExtendMax;
    }

    /// @return 当前允许的最大额外水平拾取距离
    public static float limit() {
        return Math.clamp(getValue(), 0.0F, LIMIT_MAX);
    }

    /// 玩家的 拾取范围拓展 设置
    public static @NonNull PickupRangeHolder range(@NonNull ServerPlayer player) {
        return ((PickupRangeHolder.Getter) player).feca$getPickupRange();
    }

    /// @return 该玩家实际的额外水平拾取距离
    public static float extraRangeOf(@NonNull ServerPlayer player) {
        return Math.min(range(player).getExtraRange(), limit());
    }

    /// 扩大拾取查询盒：只加水平方向(xz面)
    public static @NonNull AABB expandForPlayer(@NonNull Player player, @NonNull AABB searchBox) {
        if (!(player instanceof ServerPlayer serverPlayer)) return searchBox;

        final float extra = extraRangeOf(serverPlayer);
        if (extra <= 0.0F) return searchBox;

        return searchBox.inflate(extra, 0.0, extra);
    }
}
