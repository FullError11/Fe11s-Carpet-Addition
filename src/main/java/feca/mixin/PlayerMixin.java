package feca.mixin;

import feca.function.ItemPickupRangeExtendMax;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

@Mixin(Player.class)
public class PlayerMixin {

    /// 规则 {@link feca.rule.Rules#itemPickupRangeExtendMax}：扩大玩家的水平拾取范围。
    @Redirect(
            method = "aiStep",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getEntities(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;")
    )
    private @NonNull List<Entity> widePickupQuery(net.minecraft.world.level.@NonNull Level level, Entity entity, AABB searchBox) {
        return level.getEntities(entity, ItemPickupRangeExtendMax.expandForPlayer((Player)(Object)this, searchBox));
    }
}
