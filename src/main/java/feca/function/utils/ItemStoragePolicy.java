package feca.function.utils;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/// 可入库物品的**数据组件白名单**策略。
///
/// ## 为什么用白名单
/// 库存以「物品 + 数据组件补丁」为键（见 {@link InfinityInventoryStorage.ItemTemplate}），
/// 每个不同的组件取值都会成为一条独立条目。组件取值空间一旦是无限的
/// （容器内容、自定义名称、Lore、自定义数据、属性修饰符、书内容、玩家头颅档案……），
/// 库存、持久化与同步包都会被撑爆。因此只有显式注册过规则的组件才允许入库。
///
/// ## 默认放行的组件
/// - {@link DataComponents#ENCHANTMENTS} / {@link DataComponents#STORED_ENCHANTMENTS}：满级附魔
/// - {@link DataComponents#POTION_CONTENTS}：注册表内的药水，且是最高等级、无自定义效果 / 颜色 / 名称
/// - {@link DataComponents#FIREWORKS}：只带飞行时长，不带爆炸效果
///
/// 其余组件一律显式拒绝，例如 {@link DataComponents#CONTAINER}（容器内容）、
/// {@link DataComponents#CUSTOM_NAME}（命名）、{@link DataComponents#LORE}。
///
/// ## 扩展
/// 通过 {@link #register(DataComponentType, ComponentRule)} 注册组件规则，
/// 与 {@link InfinityInventoryStorage#registerComplexItemPushProcessor} 的
/// 特殊处理注册机制一致：默认规则在类初始化时注册，模组可追加自己的规则。
@SuppressWarnings("unused")
public final class ItemStoragePolicy {
    private ItemStoragePolicy() {}

    /// 单个组件的取值规则。
    /// @param <T> 组件值类型
    @FunctionalInterface
    public interface ComponentRule<T> {
        /// @return {@code null} 表示允许入库；否则返回展示给玩家的拒绝原因
        @Nullable Component rejectReason(@NonNull T value);
    }

    /// 拒绝结果：哪个组件、为什么。
    /// @param component 被拒绝的组件类型
    /// @param reason    拒绝原因（面向玩家的文本）
    public record Rejection(@NonNull DataComponentType<?> component, @NonNull Component reason) {
        /// @return 面向玩家的完整说明文本
        public @NonNull Component describe() {
            return Component.translatable("feca.msg.storage.rejected", componentId(component), reason);
        }
    }

    /// 已注册的组件规则，保持注册顺序
    private static final Map<DataComponentType<?>, ComponentRule<?>> RULES = new LinkedHashMap<>();

    /// 默认规则：满级附魔
    private static final ComponentRule<ItemEnchantments> MAX_LEVEL_ENCHANTMENTS = enchantments -> {
        if (enchantments.isEmpty()) {
            return Component.translatable("feca.msg.storage.component.enchantments.empty");
        }
        for (var enchantment : enchantments.keySet()) {
            int level = enchantments.getLevel(enchantment);
            if (level != enchantment.value().getMaxLevel()) {
                return Component.translatable("feca.msg.storage.component.enchantments.notMaxLevel",
                        Enchantment.getFullname(enchantment, level));
            }
        }
        return null;
    };

    /// 默认规则：烟花火箭（只保留时长）
    private static final ComponentRule<Fireworks> FLIGHT_ONLY_FIREWORKS = fireworks ->
            fireworks.explosions().isEmpty()
                    && fireworks.flightDuration() >= 0
                    && fireworks.flightDuration() <= 3
                    ? null
                    : Component.translatable("feca.msg.storage.component.fireworks.explosions");

    static {
        registerDefaultRules();
    }

    /// 注册（或覆盖）某个组件的入库规则。
    /// @param type 组件类型
    /// @param rule 取值规则；返回 {@code null} 表示允许入库
    public static <T> void register(@NonNull DataComponentType<T> type, @NonNull ComponentRule<T> rule) {
        RULES.put(type, rule);
    }

    /// @return {@code true} 表示该组件类型已注册规则
    public static boolean isRegistered(@NonNull DataComponentType<?> type) {
        return RULES.containsKey(type);
    }

    /// 检查物品堆携带的全部组件是否都允许入库。
    /// @return {@link Optional#empty()} 表示可以入库；否则是第一条被拒绝的组件
    public static @NonNull Optional<Rejection> check(@NonNull ItemStack stack) {
        return check(stack.getComponentsPatch());
    }

    /// @return {@code true} 表示该物品堆可以存入库存
    public static boolean isStorable(@NonNull ItemStack stack) {
        return check(stack).isEmpty();
    }

    /// 检查组件补丁是否全部允许入库。
    /// @return {@link Optional#empty()} 表示可以入库；否则是第一条被拒绝的组件
    public static @NonNull Optional<Rejection> check(@NonNull DataComponentPatch patch) {
        for (var entry : patch.entrySet()) {
            var type = entry.getKey();
            Optional<?> value = entry.getValue();
            // 移除默认组件同样会产生新变体，且不受白名单约束，直接拒绝
            if (value.isEmpty()) {
                return Optional.of(new Rejection(type,
                        Component.translatable("feca.msg.storage.component.removed")));
            }

            var rule = RULES.get(type);
            if (rule == null) {
                return Optional.of(new Rejection(type,
                        Component.translatable("feca.msg.storage.component.notWhitelisted")));
            }

            // RULES 的键与值总是成对注册，这里的类型转换由注册时的泛型保证
            @SuppressWarnings("unchecked")
            var reason = ((ComponentRule<Object>) rule).rejectReason(value.get());
            if (reason != null) {
                return Optional.of(new Rejection(type, reason));
            }
        }
        return Optional.empty();
    }

    /// 默认规则：满级附魔、最高等级药水、只有时长的烟花火箭。
    private static void registerDefaultRules() {
        // 附魔：等级必须等于该附魔注册定义的最高等级。
        // 低于最高等级的附魔与命令生成的超限等级都会额外产生变体。
        register(DataComponents.ENCHANTMENTS, MAX_LEVEL_ENCHANTMENTS);
        register(DataComponents.STORED_ENCHANTMENTS, MAX_LEVEL_ENCHANTMENTS);

        // 药水：注册表内的药水本身取值有限，但自定义效果 / 颜色 / 名称是无限的
        register(DataComponents.POTION_CONTENTS, ItemStoragePolicy::potionRejectReason);

        // 烟花火箭：只看飞行时长，任何爆炸效果都带来无限组合
        register(DataComponents.FIREWORKS, FLIGHT_ONLY_FIREWORKS);
    }

    /// 药水规则：必须指向注册表内的药水，且没有任何自定义数据，并且是最高等级。
    private static @Nullable Component potionRejectReason(@NonNull PotionContents contents) {
        if (contents.potion().isEmpty()) {
            return Component.translatable("feca.msg.storage.component.potion.noPotion");
        }
        if (!contents.customEffects().isEmpty()
                || contents.customColor().isPresent()
                || contents.customName().isPresent()) {
            return Component.translatable("feca.msg.storage.component.potion.custom");
        }
        if (!isHighestPotionLevel(contents.potion().get())) {
            return Component.translatable("feca.msg.storage.component.potion.notHighestLevel");
        }
        return null;
    }

    /// 是否为「最高等级」药水：它的每个效果强度都等于该效果在全部已注册药水中出现过的最高强度。
    ///
    /// 例：抗火只有一种强度（0），算最高等级；力量存在 II，因此力量 I 不算最高等级。
    private static boolean isHighestPotionLevel(@NonNull Holder<Potion> potion) {
        var effects = potion.value().getEffects();
        // 无效果药水（水瓶、浓稠、笨拙……）
        if (effects.isEmpty()) return true;

        Map<MobEffect, Integer> maxima = highestAmplifiers();
        for (var effect : effects) {
            Integer max = maxima.get(effect.getEffect().value());
            if (max == null || effect.getAmplifier() < max) return false;
        }
        return true;
    }

    /// @return 每种效果在全部已注册药水中出现过的最高强度（等级 - 1）
    private static @NonNull Map<MobEffect, Integer> highestAmplifiers() {
        Map<MobEffect, Integer> result = new HashMap<>();
        // 药水由数据包驱动，因此不缓存，按注册表当前内容现算
        BuiltInRegistries.POTION.stream().forEach(potion -> {
            for (var effect : potion.getEffects()) {
                result.merge(effect.getEffect().value(), effect.getAmplifier(), Math::max);
            }
        });
        return result;
    }

    /// 组件 ID 文本，供拒绝提示使用；未注册的组件回退到类型字符串
    private static @NonNull Component componentId(@NonNull DataComponentType<?> type) {
        var key = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(type);
        return Component.literal(key == null ? String.valueOf(type) : key.toString());
    }
}
