package feca.render;

import feca.FECA;
import feca.config.ClientConfigs;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.ARGB;
import net.minecraft.world.phys.shapes.Shapes;
import org.jspecify.annotations.NonNull;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public class ObserverFreezeAreasRender implements WorldRenderEvents.BeforeEntities {
    @Override
    public void beforeEntities(@NonNull WorldRenderContext ctx) {
        AtomicBoolean skip = new AtomicBoolean(false);
        if (!ClientConfigs.tryRead(data -> {
            if (!data.observerFreezeAreasHighlight) {
                skip.set(true);
            }
        }) || skip.get()) {
            return;
        }

        ClientConfigs.tryReadSync(data -> {
            var level = Minecraft.getInstance().level;
            if (level != null) {
                data.observerFreezeAreas.getDimension(level.dimension().identifier()).ifPresent(areas -> {
                    var bufferSource = Objects.requireNonNull(ctx.consumers());
                    var poseStack = ctx.matrices();
                    var vertexConsumer = bufferSource.getBuffer(RenderTypes.lines());
                    var cameraPos =  ctx.gameRenderer().getMainCamera().position();
                    areas.forEach(area -> ShapeRenderer.renderShape(
                            poseStack, vertexConsumer,
                            Shapes.create(area),
                            -cameraPos.x, -cameraPos.y, -cameraPos.z,
                            ARGB.color(255, 255, 0, 0),
                            1.0f // 线宽
                    ));
                });
            } else {
                // 一般情况下，level总是不为null
                FECA.LOGGER.error("[CLIENT] Minecraft.INSTANCE.level is null");
            }
        });
    }
}
