package feca.mixin;

import feca.function.StackableTotemOfUndying;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemInstance.class)
public interface ItemInstanceMixin {

    /**
     * 规则 {@link feca.rule.Rules#stackableTotemOfUndying}：覆盖不死图腾的堆叠上限。
     *
     * <p>
     *     堆叠上限由 {@code ItemInstance.getMaxStackSize()} 这个默认方法读 {@code DataComponents.MAX_STACK_SIZE} 组件得到，
     *     在查询时按规则值改写返回值，不动物品注册表与组件，因此规则可以随时开关。
     * </p>
     */
    @Inject(method = "getMaxStackSize", at = @At("HEAD"), cancellable = true)
    private void stackableTotemOfUndyingMaxStackSize(CallbackInfoReturnable<Integer> cir) {
        // 误报
        // noinspection ConstantValue
        if (((Object)this) instanceof ItemStack stack && StackableTotemOfUndying.appliesTo(stack)) {
            cir.setReturnValue(StackableTotemOfUndying.getMaxStackSize());
        }
    }
}
