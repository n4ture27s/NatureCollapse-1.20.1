package n4ture27.nature_collapse.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import n4ture27.nature_collapse.entity.ArborisMagicCircleEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

public class ArborisMagicCircleRenderer extends EntityRenderer<ArborisMagicCircleEntity> {
    private static final ResourceLocation BLANK_TEX = ResourceLocation.fromNamespaceAndPath("nature_collapse", "textures/entity/blank.png");

    public ArborisMagicCircleRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0f;
    }

    @Override
    public ResourceLocation getTextureLocation(ArborisMagicCircleEntity entity) {
        return BLANK_TEX;
    }

    @Override
    public boolean shouldRender(ArborisMagicCircleEntity entity, Frustum frustum, double camX, double camY, double camZ) {
        return true;
    }

    @Override
    public void render(ArborisMagicCircleEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);

        float time = entity.tickCount + partialTicks;
        float baseRadius = entity.getCircleRadius();
        float charge = entity.getChargeProgress();

        // 召喚時の展開アニメーション
        float currentRadius = baseRadius * Math.min(1.0f, charge * 1.3f);
        if (currentRadius <= 0.01f) return;

        // ターゲット方向（Yaw, Pitch）への回転
        float yaw = entity.getAimYaw();
        float pitch = entity.getAimPitch();

        VertexConsumer builder = buffer.getBuffer(RenderType.lightning());

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(-pitch));

        // ローカル +Z が発射方向（ターゲット向き）
        // 垂直に立てて +Z 方向を法線にする
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0f));

        // テーマカラーの取得 (0: エメラルド, 1: 黄金, 2: 蒼穹, 3: 深紫)
        float cr = 0.15f, cg = 0.95f, cb = 0.55f;
        int theme = entity.getColorTheme();
        if (theme == 1) { // 黄金
            cr = 1.0f; cg = 0.85f; cb = 0.2f;
        } else if (theme == 2) { // 蒼穹
            cr = 0.2f; cg = 0.85f; cb = 1.0f;
        } else if (theme == 3) { // 深紫
            cr = 0.85f; cg = 0.25f; cb = 0.95f;
        }

        // 1. 神聖幾何学魔方陣（二重外周リング・アストロラーベ目盛り・内周リング）
        drawMagicCircle(poseStack, builder, currentRadius, time * 2.5f, cr, cg, cb, 0.92f);

        // 2. 内部の神聖八芒星・十二芒星グリッド
        drawSacredGeometry(poseStack, builder, currentRadius * 0.75f, -time * 3.5f, cr * 1.1f, cg * 1.1f, cb * 1.1f);

        // 3. 中心で回転する照射エネルギー結晶コア
        drawCenterCore(poseStack, builder, currentRadius * 0.22f * (0.8f + charge * 0.4f), time * 4.0f, cr, cg, cb);

        // 4. チャージ中の細い照準ガイドビーム & 発射中の直線レーザービーム
        if (entity.tickCount < ArborisMagicCircleEntity.FIRE_START) {
            // チャージ中のエイム照準レーザー（細い光線）
            float aimAlpha = 0.3f + charge * 0.5f;
            drawStraightBeam(builder, poseStack.last().pose(), 0.04f, 40.0f, cr, cg, cb, aimAlpha);
        } else if (entity.tickCount <= ArborisMagicCircleEntity.FIRE_END) {
            // 発射中の直線レーザービーム（中心純白コア + テーマカラー外層光柱）
            Matrix4f mat = poseStack.last().pose();
            drawStraightBeam(builder, mat, 0.12f, 48.0f, 1.0f, 1.0f, 1.0f, 0.95f);                 // 中心白コア
            drawStraightBeam(builder, mat, 0.32f, 48.0f, cr, cg, cb, 0.85f);                       // 内層発光ビーム
            drawStraightBeam(builder, mat, 0.58f, 48.0f, cr * 0.85f, cg * 0.85f, cb * 0.85f, 0.38f); // 外層拡散オーラ
        }

        poseStack.popPose();
    }

    private void drawMagicCircle(PoseStack poseStack, VertexConsumer builder, float radius, float rotDeg, float r, float g, float b, float a) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(rotDeg));
        Matrix4f mat = poseStack.last().pose();

        float width = radius * 0.05f;
        // 外周二重リング
        drawRing(builder, mat, radius, width, r, g, b, a);
        drawRing(builder, mat, radius * 0.90f, width * 0.6f, r, g, b, a * 0.75f);

        // 円周上の12分割アストロラーベ目盛り
        int ticks = 12;
        for (int i = 0; i < ticks; i++) {
            float angle = (float) (2.0 * Math.PI / ticks) * i;
            float cos = Mth.cos(angle);
            float sin = Mth.sin(angle);
            float inner = radius * 0.82f;
            float outer = radius * 0.90f;
            drawLine(builder, mat, cos * inner, 0, sin * inner, cos * outer, 0, sin * outer, 0.05f, r, g, b, a);
        }

        // 内周リング
        drawRing(builder, mat, radius * 0.45f, width * 0.8f, r, g, b, a * 0.85f);
        poseStack.popPose();
    }

    private void drawSacredGeometry(PoseStack poseStack, VertexConsumer builder, float radius, float rotDeg, float r, float g, float b) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(rotDeg));
        Matrix4f mat = poseStack.last().pose();

        // 八芒星 (Octagram)
        drawStarPolygon(builder, mat, radius, 4, 0.0f, r, g, b);
        drawStarPolygon(builder, mat, radius, 4, 45.0f, r * 0.9f, g * 0.9f, b * 0.9f);

        // 六芒星 (Hexagram)
        float innerRad = radius * 0.62f;
        drawStarPolygon(builder, mat, innerRad, 3, 0.0f, 1.0f, 0.95f, 0.3f);
        drawStarPolygon(builder, mat, innerRad, 3, 60.0f, 1.0f, 0.95f, 0.3f);
        poseStack.popPose();
    }

    private void drawCenterCore(PoseStack poseStack, VertexConsumer builder, float size, float rot, float r, float g, float b) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(rot));
        poseStack.mulPose(Axis.XP.rotationDegrees(rot * 0.7f));
        Matrix4f mat = poseStack.last().pose();
        drawOctahedron(builder, mat, size, r, g, b, 0.95f);
        drawOctahedron(builder, mat, size * 0.5f, 1.0f, 1.0f, 1.0f, 1.0f);
        poseStack.popPose();
    }

    private void drawStraightBeam(VertexConsumer builder, Matrix4f mat, float radius, float length, float r, float g, float b, float a) {
        int sides = 8;
        for (int i = 0; i < sides; i++) {
            float a1 = (float) (2.0 * Math.PI / sides) * i;
            float a2 = (float) (2.0 * Math.PI / sides) * (i + 1);

            float x1 = Mth.cos(a1) * radius;
            float z1 = Mth.sin(a1) * radius;
            float x2 = Mth.cos(a2) * radius;
            float z2 = Mth.sin(a2) * radius;

            // 表面 (y=0 から y=-length へ直線照射)
            builder.vertex(mat, x1, 0, z1).color(r, g, b, a).endVertex();
            builder.vertex(mat, x1, -length, z1).color(r, g, b, a * 0.6f).endVertex();
            builder.vertex(mat, x2, -length, z2).color(r, g, b, a * 0.6f).endVertex();
            builder.vertex(mat, x2, 0, z2).color(r, g, b, a).endVertex();

            // 裏面
            builder.vertex(mat, x2, 0, z2).color(r, g, b, a).endVertex();
            builder.vertex(mat, x2, -length, z2).color(r, g, b, a * 0.6f).endVertex();
            builder.vertex(mat, x1, -length, z1).color(r, g, b, a * 0.6f).endVertex();
            builder.vertex(mat, x1, 0, z1).color(r, g, b, a).endVertex();
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
        int steps = 36;
        for (int i = 0; i < steps; i++) {
            float a1 = (float) (2.0 * Math.PI / steps) * i;
            float a2 = (float) (2.0 * Math.PI / steps) * (i + 1);

            float c1 = Mth.cos(a1), s1 = Mth.sin(a1);
            float c2 = Mth.cos(a2), s2 = Mth.sin(a2);

            float rIn = radius - width, rOut = radius + width;

            // 表面
            builder.vertex(mat, c1 * rIn, 0, s1 * rIn).color(r, g, b, a).endVertex();
            builder.vertex(mat, c1 * rOut, 0, s1 * rOut).color(r, g, b, a).endVertex();
            builder.vertex(mat, c2 * rOut, 0, s2 * rOut).color(r, g, b, a).endVertex();
            builder.vertex(mat, c2 * rIn, 0, s2 * rIn).color(r, g, b, a).endVertex();

            // 裏面
            builder.vertex(mat, c2 * rIn, 0, s2 * rIn).color(r, g, b, a).endVertex();
            builder.vertex(mat, c2 * rOut, 0, s2 * rOut).color(r, g, b, a).endVertex();
            builder.vertex(mat, c1 * rOut, 0, s1 * rOut).color(r, g, b, a).endVertex();
            builder.vertex(mat, c1 * rIn, 0, s1 * rIn).color(r, g, b, a).endVertex();
        }
    }

    private void drawStarPolygon(VertexConsumer builder, Matrix4f mat, float radius, int vertices, float offsetDeg, float r, float g, float b) {
        float step = (float) (2.0 * Math.PI / vertices);
        float offsetRad = (float) Math.toRadians(offsetDeg);

        for (int i = 0; i < vertices; i++) {
            float a1 = offsetRad + step * i;
            float a2 = offsetRad + step * (i + 1);

            float x1 = Mth.cos(a1) * radius;
            float z1 = Mth.sin(a1) * radius;
            float x2 = Mth.cos(a2) * radius;
            float z2 = Mth.sin(a2) * radius;

            drawLine(builder, mat, x1, 0, z1, x2, 0, z2, 0.045f, r, g, b, 0.9f);
        }
    }

    private void drawLine(VertexConsumer builder, Matrix4f mat, float x1, float y1, float z1, float x2, float y2, float z2, float width, float r, float g, float b, float a) {
        float dx = x2 - x1, dy = y2 - y1, dz = z2 - z1;
        float len = Mth.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 0.0001f) return;

        float nx = -dz / len * width;
        float nz = dx / len * width;

        builder.vertex(mat, x1 - nx, y1, z1 - nz).color(r, g, b, a).endVertex();
        builder.vertex(mat, x1 + nx, y1, z1 + nz).color(r, g, b, a).endVertex();
        builder.vertex(mat, x2 + nx, y2, z2 + nz).color(r, g, b, a).endVertex();
        builder.vertex(mat, x2 - nx, y2, z2 - nz).color(r, g, b, a).endVertex();

        builder.vertex(mat, x2 - nx, y2, z2 - nz).color(r, g, b, a).endVertex();
        builder.vertex(mat, x2 + nx, y2, z2 + nz).color(r, g, b, a).endVertex();
        builder.vertex(mat, x1 + nx, y1, z1 + nz).color(r, g, b, a).endVertex();
        builder.vertex(mat, x1 - nx, y1, z1 - nz).color(r, g, b, a).endVertex();
    }
}
