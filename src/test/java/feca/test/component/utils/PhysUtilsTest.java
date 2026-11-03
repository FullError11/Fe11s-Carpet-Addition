package feca.test.component.utils;

import com.mojang.datafixers.util.Pair;
import feca.utils.PhysUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 组件测试: {@link PhysUtils}（AABB/BlockPos 互转，命令消息与持久化都依赖它）。
 */
class PhysUtilsTest {
    @Test
    @DisplayName("整格 AABB 转换成构成它的两个方块坐标")
    void aabbToBlockPoss() {
        final Pair<BlockPos, BlockPos> single = PhysUtils.intAABBToBlockPoss(new AABB(0, 0, 0, 1, 1, 1));
        assertEquals(new BlockPos(0, 0, 0), single.getFirst());
        assertEquals(new BlockPos(0, 0, 0), single.getSecond());

        final Pair<BlockPos, BlockPos> box = PhysUtils.intAABBToBlockPoss(new AABB(0, 0, 0, 3, 3, 3));
        assertEquals(new BlockPos(0, 0, 0), box.getFirst());
        assertEquals(new BlockPos(2, 2, 2), box.getSecond(), "max 是开区间，需要 -1");
    }

    @Test
    @DisplayName("负坐标同样正确")
    void negativeCoordinates() {
        final Pair<BlockPos, BlockPos> box = PhysUtils.intAABBToBlockPoss(new AABB(-3, -3, -3, -1, -1, -1));
        assertEquals(new BlockPos(-3, -3, -3), box.getFirst());
        assertEquals(new BlockPos(-2, -2, -2), box.getSecond());
    }

    @Test
    @DisplayName("命令参数格式：空格分隔的裸数字")
    void blockPosToString() {
        assertEquals("1 2 3", PhysUtils.blockPosToString(new BlockPos(1, 2, 3)));
        assertEquals("-1 -2 -3", PhysUtils.blockPosToString(new BlockPos(-1, -2, -3)));
    }

    @Test
    @DisplayName("聊天展示格式：正数带 + 号")
    void blockPosToSignedString() {
        assertEquals("+1 +2 +3", PhysUtils.blockPosToSignedString(new BlockPos(1, 2, 3)));
        assertEquals("-1 +0 -3", PhysUtils.blockPosToSignedString(new BlockPos(-1, 0, -3)));
    }

    @Test
    @DisplayName("聊天组件格式与字符串格式一致")
    void signedComponent() {
        assertEquals(PhysUtils.blockPosToSignedString(new BlockPos(5, -6, 7)),
                PhysUtils.blockPosToSignedComponent(new BlockPos(5, -6, 7)).getString());
    }
}
