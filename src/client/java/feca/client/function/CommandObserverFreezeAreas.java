package feca.client.function;

//import com.mojang.blaze3d.vertex.*;
//import feca.client.renderer.AABBRenderer;
//import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
//import net.minecraft.client.Minecraft;
//
//import static feca.function.CommandObserverFreezeAreas.DIMENSION_MAP;
//import static feca.function.CommandObserverFreezeAreas.isEnabled;

public class CommandObserverFreezeAreas {
    static {

        // FIXME: 在 remove 时遍历 entry 会抛出异常，
        //        该 BUG 预计随 `无头服务端与客户端的数据同步 (用于Overlay显示)` 实现同步修复
//        LevelRenderEvents.AFTER_OPAQUE_TERRAIN.register(ctx -> {
//            if (!isEnabled()) return;
//
//            var level = Minecraft.getInstance().level;
//            if (level == null) return;
//            var entry = DIMENSION_MAP.getEntryNullable(level.dimension());
//            if (entry == null) return;
//
//            for (var nameableAABB : entry) {
//                AABBRenderer.render(ctx, nameableAABB.value(), 1.f, 0.f, 0.f, 1.f);
//            }
//        });
    }
}
