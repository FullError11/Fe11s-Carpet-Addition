package feca.test.rule.options;

import feca.rule.options.ActiveMendingOptions;
import net.minecraft.world.entity.EquipmentSlot;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 组件测试: {@link ActiveMendingOptions}（规则 {@code activeMending} 的槽位选项）。
 *
 * <p>契约: 枚举常量对应的槽位集合决定了「经验修补可以作用于哪些槽位」，
 * 这是该规则唯一的可配置语义。</p>
 */
class ActiveMendingOptionsTest {
    @Test
    @DisplayName("关闭时不提供任何槽位")
    void falseHasNoSlots() {
        assertEquals(0, ActiveMendingOptions.FALSE.getMendingSlots().length);
    }

    @Test
    @DisplayName("MAIN_HAND 只包含主手")
    void mainHandOnly() {
        assertArrayEquals(new EquipmentSlot[]{EquipmentSlot.MAINHAND}, ActiveMendingOptions.MAIN_HAND.getMendingSlots());
    }

    @Test
    @DisplayName("HAND 包含主副手")
    void handIsBothHands() {
        assertEquals(Set.of(EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND),
                asSet(ActiveMendingOptions.HAND.getMendingSlots()));
    }

    @Test
    @DisplayName("TRUE 覆盖六个装备槽（不含 BODY：那是生物穿戴槽）")
    void trueCoversPlayerSlots() {
        assertEquals(EnumSet.of(
                        EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND,
                        EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET),
                asSet(ActiveMendingOptions.TRUE.getMendingSlots()));
        assertNull(ActiveMendingOptions.TRUE.getMendingSlots().length == 6 ? null : "unexpected",
                "TRUE 必须且只能包含 6 个玩家装备槽");
    }

    @Test
    @DisplayName("槽位数量随选项单调递增（FALSE < MAIN_HAND < HAND < TRUE）")
    void slotCountsAreMonotonic() {
        assertTrue(ActiveMendingOptions.FALSE.getMendingSlots().length
                < ActiveMendingOptions.MAIN_HAND.getMendingSlots().length);
        assertTrue(ActiveMendingOptions.MAIN_HAND.getMendingSlots().length
                < ActiveMendingOptions.HAND.getMendingSlots().length);
        assertTrue(ActiveMendingOptions.HAND.getMendingSlots().length
                < ActiveMendingOptions.TRUE.getMendingSlots().length);
    }

    @Test
    @DisplayName("每个选项内部没有重复槽位")
    void noDuplicateSlots() {
        for (ActiveMendingOptions option : ActiveMendingOptions.values()) {
            final List<EquipmentSlot> slots = List.of(option.getMendingSlots());
            assertEquals(slots.size(), Set.copyOf(slots).size(), "存在重复槽位: " + option);
        }
    }

    private static Set<EquipmentSlot> asSet(EquipmentSlot @NonNull [] slots) {
        return slots.length == 0 ? Set.of() : EnumSet.copyOf(List.of(slots));
    }
}
