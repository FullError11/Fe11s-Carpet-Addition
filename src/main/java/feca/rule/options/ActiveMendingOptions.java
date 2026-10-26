package feca.rule.options;

import net.minecraft.world.entity.EquipmentSlot;

import static net.minecraft.world.entity.EquipmentSlot.*;

public enum ActiveMendingOptions {
    FALSE(),                    // 关闭
    MAIN_HAND(MAINHAND),        // 主手
    HAND(MAINHAND, OFFHAND),    // 主副手
    TRUE(MAINHAND, CHEST, OFFHAND, HEAD, LEGS, FEET); // 装备槽

    private final EquipmentSlot[] mendingSlots;

    ActiveMendingOptions(EquipmentSlot... slots) {
        this.mendingSlots = slots;
    }

    public EquipmentSlot[] getMendingSlots() {
        return mendingSlots;
    }
}
