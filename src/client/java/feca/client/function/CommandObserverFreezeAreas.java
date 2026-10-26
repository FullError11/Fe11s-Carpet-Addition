package feca.client.function;

import com.mojang.blaze3d.vertex.*;
import feca.client.renderer.AABBRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;

import static feca.function.CommandObserverFreezeAreas.DIMENSION_MAP;
import static feca.function.CommandObserverFreezeAreas.isEnabled;

public class CommandObserverFreezeAreas {
    static {
        LevelRenderEvents.AFTER_OPAQUE_TERRAIN.register(ctx -> {
            if (!isEnabled()) return;

            var level = Minecraft.getInstance().level;
            if (level == null) return;
            var entry = DIMENSION_MAP.getEntryNullable(level.dimension());
            if (entry == null) return;

            AABBRenderer.renderAll(ctx, entry, 1.f, 0.f, 0.f, 1.f);
        });
    }
}
