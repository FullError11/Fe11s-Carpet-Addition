package feca.test.component.function;

import feca.function.utils.ObserverFreezeAreasOverlay;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 组件测试: {@link ObserverFreezeAreasOverlay} 的棱几何与形状 id 分组。
 *
 * <p>棱是 12 条独立 {@code LineShape}，几何退化会少一条棱；子形状 id 的前缀少了
 * {@code "/"} 会误删相邻区域。这两类错误在游戏里只表现为「看起来少点什么」，
 * 很难排查，所以在这里钉住。</p>
 */
class OverlayEdgesTest {
    private static final AABB BOX = new AABB(0, 0, 0, 2, 3, 4);

    @Test
    @DisplayName("12 条棱：底面 4 + 顶面 4 + 竖直 4")
    void twelveEdges() {
        assertEquals(12, ObserverFreezeAreasOverlay.edgeSegments(BOX).length);
    }

    @Test
    @DisplayName("每条棱的端点都落在包围盒的角上")
    void endpointsAreCorners() {
        for (Vec3[] edge : ObserverFreezeAreasOverlay.edgeSegments(BOX)) {
            for (Vec3 point : edge) {
                assertTrue(isCorner(point), point + " 不是包围盒的角");
            }
        }
    }

    @Test
    @DisplayName("每条棱平行于某个坐标轴（立方体的边不能是斜线）")
    void edgesAreAxisAligned() {
        for (Vec3[] edge : ObserverFreezeAreasOverlay.edgeSegments(BOX)) {
            final Vec3 d = edge[1].subtract(edge[0]);
            final int nonZero = (d.x != 0 ? 1 : 0) + (d.y != 0 ? 1 : 0) + (d.z != 0 ? 1 : 0);
            assertEquals(1, nonZero, "棱 " + edge[0] + " -> " + edge[1] + " 不是轴平行");
        }
    }

    @Test
    @DisplayName("没有重复的棱（重复会让线宽叠加、颜色变深）")
    void noDuplicateEdges() {
        final Set<String> seen = new HashSet<>();
        for (Vec3[] edge : ObserverFreezeAreasOverlay.edgeSegments(BOX)) {
            // 反向视为同一条棱
            final String a = key(edge[0]) + "|" + key(edge[1]);
            final String b = key(edge[1]) + "|" + key(edge[0]);
            assertTrue(seen.add(a) && !seen.contains(b), "重复的棱: " + a);
        }
    }

    @Test
    @DisplayName("各条棱的长度与包围盒尺寸一致（4 条 x 边、4 条 y 边、4 条 z 边）")
    void edgeLengthsMatchBox() {
        int alongX = 0, alongY = 0, alongZ = 0;
        for (Vec3[] edge : ObserverFreezeAreasOverlay.edgeSegments(BOX)) {
            final Vec3 d = edge[1].subtract(edge[0]);
            if (d.x != 0) { alongX++; assertEquals(2.0, Math.abs(d.x), 1e-9); }
            if (d.y != 0) { alongY++; assertEquals(3.0, Math.abs(d.y), 1e-9); }
            if (d.z != 0) { alongZ++; assertEquals(4.0, Math.abs(d.z), 1e-9); }
        }
        assertEquals(4, alongX);
        assertEquals(4, alongY);
        assertEquals(4, alongZ);
    }

    @Test
    @DisplayName("整组前缀带 / 分隔符：不会误删 id 以本区域 id 开头的其它区域")
    void groupPrefixIsSeparated() {
        final Identifier area = Identifier.fromNamespaceAndPath("feca", "observer_freeze_areas/overworld/ab");
        final Identifier other = Identifier.fromNamespaceAndPath("feca", "observer_freeze_areas/overworld/abc");
        final String prefix = ObserverFreezeAreasOverlay.groupId(area).getPath();
        final Identifier otherChild = Identifier.fromNamespaceAndPath("feca", other.getPath() + "/face");

        assertEquals("observer_freeze_areas/overworld/ab/", prefix);
        assertTrue(Identifier.fromNamespaceAndPath("feca", area.getPath() + "/face")
                .getPath().startsWith(prefix));
        assertFalse(otherChild.getPath().startsWith(prefix), "abc/face 不应被 ab 的前缀命中");
    }

    private static boolean isCorner(Vec3 p) {
        final boolean x = p.x == BOX.minX || p.x == BOX.maxX;
        final boolean y = p.y == BOX.minY || p.y == BOX.maxY;
        final boolean z = p.z == BOX.minZ || p.z == BOX.maxZ;
        return x && y && z;
    }

    private static String key(Vec3 p) {
        return p.x + "," + p.y + "," + p.z;
    }
}