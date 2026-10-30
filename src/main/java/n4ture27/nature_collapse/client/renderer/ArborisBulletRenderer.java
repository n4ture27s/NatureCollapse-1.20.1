package n4ture27.nature_collapse.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import n4ture27.nature_collapse.entity.ArborisBulletEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public class ArborisBulletRenderer extends EntityRenderer<ArborisBulletEntity> {
    private static final ResourceLocation BLANK_TEX = ResourceLocation.fromNamespaceAndPath("nature_collapse", "textures/entity/blank.png");

    public ArborisBulletRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0f;
    }

    @Override
    public ResourceLocation getTextureLocation(ArborisBulletEntity entity) {
        return BLANK_TEX;
    }

    @Override
    public boolean shouldRender(ArborisBulletEntity entity, Frustum frustum, double camX, double camY, double camZ) {
        return true;
    }

    @Override
    public void render(ArborisBulletEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);

        float time = entity.tickCount + partialTicks;
        float scale = entity.getBulletScale();
        int type = entity.getBulletType();

        VertexConsumer builder = buffer.getBuffer(RenderType.lightning());

        poseStack.pushPose();
        poseStack.scale(scale, scale, scale);

        // 各タイプに応じた神聖幾何学エフェクト
        switch (type) {
            case 0 -> renderEmeraldSeed(poseStack, builder, time);
            case 1 -> renderCyanDiamond(poseStack, builder, time);
            case 2 -> renderSolarGold(poseStack, builder, time);
            case 3 -> renderAbyssalPurple(poseStack, builder, time);
            default -> renderLaserBeam(entity, poseStack, builder, time);
        }

        poseStack.popPose();
    }

    /**
     * Type 0: エメラルドの種核 (Emerald Seed)
     */
    private void renderEmeraldSeed(PoseStack poseStack, VertexConsumer builder, float time) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(time * 6.0f));
        poseStack.mulPose(Axis.XP.rotationDegrees(time * 4.0f));
        Matrix4f mat = poseStack.last().pose();

        // 内部コア結晶（エメラルドグリーン）
        drawOctahedron(builder, mat, 0.45f, 0.1f, 0.95f, 0.45f, 0.95f);
        // 白色内核
        drawOctahedron(builder, mat, 0.22f, 0.9f, 1.0f, 0.9f, 1.0f);
        poseStack.popPose();

        // 外周回転リング
        poseStack.pushPose();
        poseStack.mulPose(Axis.ZP.rotationDegrees(time * -5.0f));
        poseStack.mulPose(Axis.YP.rotationDegrees(time * 3.0f));
        Matrix4f matRing = poseStack.last().pose();
        drawRing(builder, matRing, 0.65f, 0.05f, 0.2f, 1.0f, 0.6f, 0.85f);
        poseStack.popPose();
    }

    /**
     * Type 1: 蒼穹の幾何学ダイヤ (Cyan Diamond)
     */
    private void renderCyanDiamond(PoseStack poseStack, VertexConsumer builder, float time) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-time * 8.0f));
        poseStack.mulPose(Axis.ZP.rotationDegrees(time * 5.0f));
        Matrix4f mat = poseStack.last().pose();

        // シアンダイヤモンド結晶
        drawOctahedron(builder, mat, 0.5f, 0.15f, 0.85f, 1.0f, 0.95f);
        drawOctahedron(builder, mat, 0.25f, 1.0f, 1.0f, 1.0f, 1.0f);
        poseStack.popPose();

        // 回転する外周正方形フレーム
        poseStack.pushPose();
        poseStack.mulPose(Axis.XP.rotationDegrees(time * 6.0f));
        Matrix4f matFrame = poseStack.last().pose();
        drawSquareFrame(builder, matFrame, 0.7f, 0.04f, 0.3f, 0.95f, 1.0f, 0.9f);
        poseStack.popPose();
    }

    /**
     * Type 2: 黄金の太陽ルーン (Solar Gold)
     */
    private void renderSolarGold(PoseStack poseStack, VertexConsumer builder, float time) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(time * 10.0f));
        poseStack.mulPose(Axis.XP.rotationDegrees(time * 7.0f));
        Matrix4f mat = poseStack.last().pose();

        // 黄金コア
        drawOctahedron(builder, mat, 0.55f, 1.0f, 0.85f, 0.15f, 0.95f);
        drawOctahedron(builder, mat, 0.28f, 1.0f, 1.0f, 0.9f, 1.0f);
        poseStack.popPose();

        // 八芒星の太陽リング
        poseStack.pushPose();
        poseStack.mulPose(Axis.ZP.rotationDegrees(-time * 4.0f));
        Matrix4f matSun = poseStack.last().pose();
        drawRing(builder, matSun, 0.75f, 0.06f, 1.0f, 0.75f, 0.1f, 0.9f);
        poseStack.popPose();
    }

    /**
     * Type 3: 虚空のアメジスト星 (Abyssal Purple)
     */
    private void renderAbyssalPurple(PoseStack poseStack, VertexConsumer builder, float time) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(time * 7.0f));
        poseStack.mulPose(Axis.ZP.rotationDegrees(-time * 5.0f));
        Matrix4f mat = poseStack.last().pose();

        // 深紫の虚空結晶
        drawOctahedron(builder, mat, 0.52f, 0.75f, 0.15f, 0.95f, 0.95f);
        drawOctahedron(builder, mat, 0.26f, 0.95f, 0.6f, 1.0f, 1.0f);
        poseStack.popPose();

        // 直交する2重リングオーラ
        poseStack.pushPose();
        poseStack.mulPose(Axis.XP.rotationDegrees(time * 8.0f));
        Matrix4f matR1 = poseStack.last().pose();
        drawRing(builder, matR1, 0.72f, 0.05f, 0.85f, 0.25f, 1.0f, 0.85f);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-time * 8.0f));
        Matrix4f matR2 = poseStack.last().pose();
        drawRing(builder, matR2, 0.72f, 0.05f, 0.55f, 0.1f, 0.9f, 0.85f);
        poseStack.popPose();
    }

    /**
     * Type 4: 単純な直線レーザービーム (Simple Linear Laser Beam)
     * 進行方向に細長く伸びる円柱状の純粋な高密度エネルギー光線
     */
    private void renderLaserBeam(ArborisBulletEntity entity, PoseStack poseStack, VertexConsumer builder, float time) {
        Vec3 vel = entity.getDeltaMovement();
        double speed = vel.length();
        if (speed > 0.001) {
            float yaw = (float) (Mth.atan2(vel.x, vel.z) * (180.0 / Math.PI));
            float pitch = (float) (Mth.atan2(vel.y, Math.sqrt(vel.x * vel.x + vel.z * vel.z)) * (180.0 / Math.PI));

            poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
            poseStack.mulPose(Axis.XP.rotationDegrees(-pitch));
        }

        Matrix4f mat = poseStack.last().pose();

        // ビームの全長と半径
        float beamLength = 4.5f;
        float coreRadius = 0.08f;
        float innerRadius = 0.20f;
        float outerRadius = 0.35f;

        // 1. 中心高エネルギーコア（純白）
        drawBeamCylinder(builder, mat, coreRadius, beamLength, 1.0f, 1.0f, 1.0f, 1.0f);

        // 2. 内層発光エネルギー層（鮮烈な黄金・琥珀色）
        drawBeamCylinder(builder, mat, innerRadius, beamLength * 1.05f, 1.0f, 0.9f, 0.25f, 0.75f);

        // 3. 外層拡散オーラ層（柔らかな光条）
        drawBeamCylinder(builder, mat, outerRadius, beamLength * 1.15f, 1.0f, 0.75f, 0.15f, 0.35f);
    }

    private void drawBeamCylinder(VertexConsumer builder, Matrix4f mat, float radius, float length, float r, float g, float b, float a) {
        int sides = 8;
        float halfLen = length * 0.5f;

        for (int i = 0; i < sides; i++) {
            float a1 = (float) (2.0 * Math.PI / sides) * i;
            float a2 = (float) (2.0 * Math.PI / sides) * (i + 1);

            float x1 = Mth.cos(a1) * radius;
            float y1 = Mth.sin(a1) * radius;
            float x2 = Mth.cos(a2) * radius;
            float y2 = Mth.sin(a2) * radius;

            // 前半 (中心 z=0 から 先端 z=halfLen へフェード)
            builder.vertex(mat, x1, y1, 0).color(r, g, b, a).endVertex();
            builder.vertex(mat, x1, y1, halfLen).color(r, g, b, 0.0f).endVertex();
            builder.vertex(mat, x2, y2, halfLen).color(r, g, b, 0.0f).endVertex();
            builder.vertex(mat, x2, y2, 0).color(r, g, b, a).endVertex();

            // 後半 (中心 z=0 から 末尾 z=-halfLen へフェード)
            builder.vertex(mat, x1, y1, -halfLen).color(r, g, b, 0.0f).endVertex();
            builder.vertex(mat, x1, y1, 0).color(r, g, b, a).endVertex();
            builder.vertex(mat, x2, y2, 0).color(r, g, b, a).endVertex();
            builder.vertex(mat, x2, y2, -halfLen).color(r, g, b, 0.0f).endVertex();
        }
    }

    private void drawOctahedron(VertexConsumer builder, Matrix4f mat, float s, float r, float g, float b, float a) {
        drawTriangle(builder, mat, 0, s, 0, s, 0, 0, 0, 0, s, r, g, b, a);
        drawTriangle(builder, mat, 0, s, 0, 0, 0, s, -s, 0, 0, r, g, b, a);
        drawTriangle(builder, mat, 0, s, 0, -s, 0, 0, 0, 0, -s, r, g, b, a);
        drawTriangle(builder, mat, 0, s, 0, 0, 0, -s, s, 0, 0, r, g, b, a);

        drawTriangle(builder, mat, 0, -s, 0, 0, 0, s, s, 0, 0, r, g, b, a);
        drawTriangle(builder, mat, 0, -s, 0, -s, 0, 0, 0, 0, s, r, g, b, a);
        drawTriangle(builder, mat, 0, -s, 0, 0, 0, -s, -s, 0, 0, r, g, b, a);
        drawTriangle(builder, mat, 0, -s, 0, s, 0, 0, 0, 0, -s, r, g, b, a);
    }

    private void drawTriangle(VertexConsumer builder, Matrix4f mat, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float r, float g, float b, float a) {
        builder.vertex(mat, x1, y1, z1).color(r, g, b, a).endVertex();
        builder.vertex(mat, x2, y2, z2).color(r, g, b, a).endVertex();
        builder.vertex(mat, x3, y3, z3).color(r, g, b, a).endVertex();
        builder.vertex(mat, x1, y1, z1).color(r, g, b, a).endVertex();

        builder.vertex(mat, x3, y3, z3).color(r, g, b, a).endVertex();
        builder.vertex(mat, x2, y2, z2).color(r, g, b, a).endVertex();
        builder.vertex(mat, x1, y1, z1).color(r, g, b, a).endVertex();
        builder.vertex(mat, x3, y3, z3).color(r, g, b, a).endVertex();
    }

    private void drawRing(VertexConsumer builder, Matrix4f mat, float radius, float width, float r, float g, float b, float a) {
        int steps = 24;
        for (int i = 0; i < steps; i++) {
            float a1 = (float) (2.0 * Math.PI / steps) * i;
            float a2 = (float) (2.0 * Math.PI / steps) * (i + 1);

            float c1 = Mth.cos(a1), s1 = Mth.sin(a1);
            float c2 = Mth.cos(a2), s2 = Mth.sin(a2);

            float rIn = radius - width, rOut = radius + width;

            builder.vertex(mat, c1 * rIn, 0, s1 * rIn).color(r, g, b, a).endVertex();
            builder.vertex(mat, c1 * rOut, 0, s1 * rOut).color(r, g, b, a).endVertex();
            builder.vertex(mat, c2 * rOut, 0, s2 * rOut).color(r, g, b, a).endVertex();
            builder.vertex(mat, c2 * rIn, 0, s2 * rIn).color(r, g, b, a).endVertex();

            builder.vertex(mat, c2 * rIn, 0, s2 * rIn).color(r, g, b, a).endVertex();
            builder.vertex(mat, c2 * rOut, 0, s2 * rOut).color(r, g, b, a).endVertex();
            builder.vertex(mat, c1 * rOut, 0, s1 * rOut).color(r, g, b, a).endVertex();
            builder.vertex(mat, c1 * rIn, 0, s1 * rIn).color(r, g, b, a).endVertex();
        }
    }

    private void drawSquareFrame(VertexConsumer builder, Matrix4f mat, float s, float w, float r, float g, float b, float a) {
        drawLine(builder, mat, -s, -s, 0, s, -s, 0, w, r, g, b, a);
        drawLine(builder, mat, s, -s, 0, s, s, 0, w, r, g, b, a);
        drawLine(builder, mat, s, s, 0, -s, s, 0, w, r, g, b, a);
        drawLine(builder, mat, -s, s, 0, -s, -s, 0, w, r, g, b, a);
    }

    private void drawLine(VertexConsumer builder, Matrix4f mat, float x1, float y1, float z1, float x2, float y2, float z2, float width, float r, float g, float b, float a) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float dz = z2 - z1;
        float len = Mth.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 0.0001f) return;

        float nx = -dy / len * width;
        float ny = dx / len * width;

        builder.vertex(mat, x1 - nx, y1 - ny, z1).color(r, g, b, a).endVertex();
        builder.vertex(mat, x1 + nx, y1 + ny, z1).color(r, g, b, a).endVertex();
        builder.vertex(mat, x2 + nx, y2 + ny, z2).color(r, g, b, a).endVertex();
        builder.vertex(mat, x2 - nx, y2 - ny, z2).color(r, g, b, a).endVertex();

        builder.vertex(mat, x2 - nx, y2 - ny, z2).color(r, g, b, a).endVertex();
        builder.vertex(mat, x2 + nx, y2 + ny, z2).color(r, g, b, a).endVertex();
        builder.vertex(mat, x1 + nx, y1 + ny, z1).color(r, g, b, a).endVertex();
        builder.vertex(mat, x1 - nx, y1 - ny, z1).color(r, g, b, a).endVertex();
    }
}
