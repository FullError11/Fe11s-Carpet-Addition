package fe11.carpetaddition.render;

import fe11.carpetaddition.config.ClientConfigs;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
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
            if (data.observerFreezeAreas.isEmpty()) return;

            var bufferSource = Objects.requireNonNull(ctx.consumers());
            var poseStack = ctx.matrices();
            var vertexConsumer = bufferSource.getBuffer(RenderTypes.lines());
            var cameraPos =  ctx.gameRenderer().getMainCamera().position();
            data.observerFreezeAreas.forEach(area -> ShapeRenderer.renderShape(
                    poseStack, vertexConsumer,
                    Shapes.create(area),
                    -cameraPos.x, -cameraPos.y, -cameraPos.z,
                    ARGB.color(255, 255, 0, 0),
                    1.0f // 线宽(?)
            ));
        });
    }
}
