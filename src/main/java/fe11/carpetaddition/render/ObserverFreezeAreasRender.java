package fe11.carpetaddition.render;

import fe11.carpetaddition.Feca;
import fe11.carpetaddition.config.ClientConfigs;
import fe11.carpetaddition.config.ServerConfigs;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.util.ARGB;
import net.minecraft.world.phys.shapes.Shapes;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public class ObserverFreezeAreasRender implements WorldRenderEvents.AfterTranslucent {
    @Override
    public void afterTranslucent(@NotNull WorldRenderContext ctx) {
        AtomicBoolean skip = new AtomicBoolean(false);
        if (!ClientConfigs.tryRead(data -> {
            if (!data.observerFreezeAreasHighlight) {
                skip.set(true);
            }
        }) || skip.get()) {
            return;
        }

        ClientConfigs.tryReadSync(data -> {
            if (data.observerFreezeAreas.isEmpty()) return;

            var bufferSource = Objects.requireNonNull(ctx.consumers());
            var poseStack = Objects.requireNonNull(ctx.matrixStack());
            var vertexConsumer = bufferSource.getBuffer(RenderType.lines());
            var cameraPos =  ctx.camera().getPosition();
            data.observerFreezeAreas.forEach(area -> ShapeRenderer.renderShape(
                    poseStack, vertexConsumer,
                    Shapes.create(area),
                    -cameraPos.x, -cameraPos.y, -cameraPos.z,
                    ARGB.color(255, 255, 0, 0)
            ));
        });
    }
}
