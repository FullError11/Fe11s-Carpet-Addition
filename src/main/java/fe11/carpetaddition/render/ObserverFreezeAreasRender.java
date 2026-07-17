package fe11.carpetaddition.render;

import fe11.carpetaddition.Feca;
import fe11.carpetaddition.config.ClientConfigs;
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
                data.observerFreezeAreas.access(level.dimension(), areas -> {
                    if (areas.isEmpty()) return;

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
                Feca.LOGGER.error("[CLIENT] Minecraft.INSTANCE.level is null");
            }
        });
    }
}
