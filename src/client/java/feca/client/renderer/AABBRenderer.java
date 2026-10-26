package feca.client.renderer;

import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import feca.FeCA;
import feca.client.registry.GameRendererCloseCallbackRegistry;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.StagedVertexBuffer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelTerrainRenderContext;
import org.jspecify.annotations.NonNull;

/**
 * 通用 AABB 线框渲染器。
 *
 * <p>在 {@code AFTER_OPAQUE_TERRAIN} 等提供 {@link LevelTerrainRenderContext} 的事件里，
 * 直接调用 {@link #render(LevelTerrainRenderContext, AABB)} 即可。
 *
 * <p>{@link LevelTerrainRenderContext} 不直接暴露 {@link PoseStack}，本类在内部自行构造一个
 * 并应用相机平移；顶点坐标一律传入世界坐标，渲染器会自动换算到相机相对坐标。
 */
@SuppressWarnings("unused")
public final class AABBRenderer {
    static {
        GameRendererCloseCallbackRegistry.register(AABBRenderer::close);
    }

    private static final RenderPipeline LINES_THROUGH_WALLS = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
                    .withLocation(FeCA.id("pipeline/aabb_lines_through_walls"))
                    .withDepthStencilState(Optional.empty())
                    .build()
    );

    private static final Vector4f COLOR_MODULATOR = new Vector4f(1f, 1f, 1f, 1f);
    private static final Vector3f MODEL_OFFSET = new Vector3f();
    private static final Matrix4f TEXTURE_MATRIX = new Matrix4f();
    private static final StagedVertexBuffer STAGED_BUFFER =
            new StagedVertexBuffer(() -> "AABB Outline Buffer", RenderType.SMALL_BUFFER_SIZE);

    private AABBRenderer() { }

    /** 默认红色（不透明）绘制单个 AABB 线框。 */
    public static void render(LevelTerrainRenderContext context, AABB box) {
        render(context, box, 1f, 0f, 0f, 1f);
    }

    /** 以指定颜色绘制单个 AABB 线框。 */
    public static void render(LevelTerrainRenderContext context, AABB box,
                              float r, float g, float b, float a) {
        flush(context, List.of(box), r, g, b, a);
    }

    /**
     * 一次绘制多个 AABB（同一次 upload / 同一次 draw call，效率最高）。
     * 若有大量盒子，优先用这个而不是循环调 {@link #render}。
     */
    public static void renderAll(LevelTerrainRenderContext context, Iterable<AABB> boxes,
                                 float r, float g, float b, float a) {
        flush(context, boxes, r, g, b, a);
    }

    /** 释放底层 GPU 资源。可在模组关闭/重载时调用。 */
    public static void close() {
        STAGED_BUFFER.close();
    }




    private static void flush(LevelTerrainRenderContext context, Iterable<AABB> boxes,
                              float r, float g, float b, float a) {
        RenderPipeline pipeline = LINES_THROUGH_WALLS;
        VertexFormat formatBinding = pipeline.getVertexFormatBinding(0);
        if (formatBinding == null) return;

        PrimitiveTopology primitive = pipeline.getPrimitiveTopology();
        StagedVertexBuffer.Draw draw = STAGED_BUFFER.appendDraw(
                formatBinding, primitive,
                primitive == PrimitiveTopology.QUADS ? RenderSystem.getProjectionType().vertexSorting() : null);

        // LevelTerrainRenderContext 没有 poseStack，手动构造并应用相机平移
        PoseStack poseStack = new PoseStack();
        Vec3 cam = context.levelState().cameraRenderState.pos;
        poseStack.translate(-cam.x, -cam.y, -cam.z);
        Matrix4fc mat = poseStack.last().pose();

        VertexConsumer buf = STAGED_BUFFER.getVertexBuilder(draw);
        for (AABB box : boxes) {
            drawLineBox(mat, buf, box, r, g, b, a);
        }

        STAGED_BUFFER.upload();
        StagedVertexBuffer.ExecuteInfo info = STAGED_BUFFER.getExecuteInfo(draw);
        if (info != null) {
            execute(Minecraft.getInstance(), info, pipeline);
        }
        STAGED_BUFFER.endFrame();
    }

    private static void drawLineBox(Matrix4fc pose, VertexConsumer buf, @NonNull AABB box,
                                    float r, float g, float b, float a) {
        float x1 = (float) box.minX, y1 = (float) box.minY, z1 = (float) box.minZ;
        float x2 = (float) box.maxX, y2 = (float) box.maxY, z2 = (float) box.maxZ;

        // 底面
        line(pose, buf, x1, y1, z1, x2, y1, z1, r, g, b, a);
        line(pose, buf, x2, y1, z1, x2, y1, z2, r, g, b, a);
        line(pose, buf, x2, y1, z2, x1, y1, z2, r, g, b, a);
        line(pose, buf, x1, y1, z2, x1, y1, z1, r, g, b, a);
        // 顶面
        line(pose, buf, x1, y2, z1, x2, y2, z1, r, g, b, a);
        line(pose, buf, x2, y2, z1, x2, y2, z2, r, g, b, a);
        line(pose, buf, x2, y2, z2, x1, y2, z2, r, g, b, a);
        line(pose, buf, x1, y2, z2, x1, y2, z1, r, g, b, a);
        // 竖边
        line(pose, buf, x1, y1, z1, x1, y2, z1, r, g, b, a);
        line(pose, buf, x2, y1, z1, x2, y2, z1, r, g, b, a);
        line(pose, buf, x2, y1, z2, x2, y2, z2, r, g, b, a);
        line(pose, buf, x1, y1, z2, x1, y2, z2, r, g, b, a);
    }

    private static void line(Matrix4fc pose, VertexConsumer buf,
                             float x1, float y1, float z1,
                             float x2, float y2, float z2,
                             float r, float g, float b, float a) {
        // 1. 计算线段的方向向量（法线）
        float dx = x2 - x1;
        float dy = y2 - y1;
        float dz = z2 - z1;
        float distanceInv = 1.0f / (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        dx *= distanceInv;
        dy *= distanceInv;
        dz *= distanceInv;

        // 2. 写入起点，补上 setNormal 和 setLineWidth
        buf.addVertex(pose, x1, y1, z1)
                .setColor(r, g, b, a)
                .setNormal(dx, dy, dz) // 或者用 pose 的 setNormal 重载
                .setLineWidth(2.0f);   // 如果管线需要线宽

        // 3. 写入终点，同样补上
        buf.addVertex(pose, x2, y2, z2)
                .setColor(r, g, b, a)
                .setNormal(dx, dy, dz)
                .setLineWidth(2.0f);
    }

    private static void execute(@NonNull Minecraft client, StagedVertexBuffer.ExecuteInfo info, RenderPipeline pipeline) {
        GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms()
                .writeTransform(RenderSystem.getModelViewMatrixCopy(), COLOR_MODULATOR, MODEL_OFFSET, TEXTURE_MATRIX);

        RenderTarget mainTarget = client.gameRenderer.mainRenderTarget();
        GpuTextureView colorTexture = mainTarget.getColorTextureView();
        if (colorTexture == null) return;

        try (RenderPass renderPass = RenderSystem.getDevice().createCommandEncoder()
                .createRenderPass(() -> "AABB outline", colorTexture, Optional.empty(),
                        mainTarget.getDepthTextureView(), OptionalDouble.empty())) {
            renderPass.setPipeline(pipeline);
            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.setUniform("DynamicTransforms", dynamicTransforms);
            // noinspection resource
            renderPass.setVertexBuffer(0, info.vertexBuffer().slice());
            renderPass.setIndexBuffer(info.indexBuffer(), info.indexType());
            renderPass.drawIndexed(info.indexCount(), 1, info.firstIndex(), info.baseVertex(), 0);
        }
    }
}