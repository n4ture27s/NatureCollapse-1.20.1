package n4ture27.nature_collapse.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import n4ture27.nature_collapse.entity.ArborisEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

public class ArborisRenderer extends EntityRenderer<ArborisEntity> {
    private static final ResourceLocation BLANK_TEX = ResourceLocation.fromNamespaceAndPath("nature_collapse", "textures/entity/blank.png");

    public ArborisRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 3.0f;
    }

    @Override
    public ResourceLocation getTextureLocation(ArborisEntity entity) {
        return BLANK_TEX;
    }

    @Override
    public boolean shouldRender(ArborisEntity entity, Frustum frustum, double camX, double camY, double camZ) {
        // 天空を貫く光柱、巨大魔方陣(半径24m)、周囲の公転多面体が見る方向によって見切れたり
        // フラスタムカリングで突然消失する問題を完全に防止する
        return true;
    }

    @Override
    public void render(ArborisEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);

        float animTime = entity.getAnimTick() + partialTicks;
        VertexConsumer glowConsumer = buffer.getBuffer(RenderType.lightning());

        poseStack.pushPose();

        float yawToPlayer = getYawToPlayer(entity, partialTicks);

        if (entity.isAwakening()) {
            renderAwakeningRitual(entity, animTime, yawToPlayer, poseStack, glowConsumer);
        } else {
            renderFullAwakenedBoss(entity, animTime, yawToPlayer, poseStack, glowConsumer);
        }

        poseStack.popPose();
    }

    /**
     * プレイヤーの立ち位置（座標）からボスへの水平相対方位角（Yaw）を計算
     * 視線（マウス移動）を振るだけでは値が一切変化せず、プレイヤーがボスの周囲を実際に歩いた時のみ追従する
     */
    private float getYawToPlayer(ArborisEntity entity, float partialTicks) {
        net.minecraft.world.phys.Vec3 camPos = this.entityRenderDispatcher.camera.getPosition();
        double entityX = Mth.lerp(partialTicks, entity.xOld, entity.getX());
        double entityZ = Mth.lerp(partialTicks, entity.zOld, entity.getZ());
        double dx = camPos.x - entityX;
        double dz = camPos.z - entityZ;
        if (dx * dx + dz * dz < 0.0001) {
            return 0.0f;
        }
        return (float) (Math.atan2(dx, dz) * (180.0 / Math.PI));
    }

    /**
     * 30秒間の出現演出
     * - 0〜5秒 (0〜100 ticks): 岩盤での前兆共鳴（光の収束・小型魔法陣・種核の脈動）
     * - 5〜30秒 (100〜600 ticks): 大爆破とともに地上へ隆起、天空を貫く光柱、超巨大魔方陣の展開、蠢く巨根
     */
    private void renderAwakeningRitual(ArborisEntity entity, float animTime, float yawToPlayer, PoseStack poseStack, VertexConsumer builder) {
        int ticks = entity.getAwakeningTicks();

        // -------------------------------------------------------------------------
        // PHASE 1: 前兆共鳴フェーズ (0〜5秒 = 0〜100 ticks)
        // -------------------------------------------------------------------------
        if (ticks < ArborisEntity.PRELUDE_DURATION) {
            float preludeProg = (float) ticks / (float) ArborisEntity.PRELUDE_DURATION;

            // 1. 岩盤上に広がる前兆の水平収束魔方陣 (半径 3m 〜 7m)
            poseStack.pushPose();
            poseStack.translate(0.0, 0.05, 0.0);
            float circleRadius = 3.5f + preludeProg * 3.5f;
            drawMagicCircle(poseStack, builder, circleRadius, animTime * 0.8f, 0.15f, 0.9f, 0.5f, 0.7f + preludeProg * 0.25f);
            drawSacredGeometryGrid(poseStack, builder, circleRadius * 0.75f, -animTime * 1.2f, 0.8f, 0.3f, 0.95f);
            poseStack.popPose();

            // 2. 横方向から見た際に正面を向く直立前兆魔方陣 (位置基準のY軸追従、視線ブレなし)
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(yawToPlayer));
            poseStack.translate(0.0, 2.5, -0.6);
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0f));
            float vertPreludeRad = 3.0f + preludeProg * 3.0f;
            drawMagicCircle(poseStack, builder, vertPreludeRad, animTime * 0.8f, 0.15f, 0.95f, 0.55f, 0.85f);
            drawSacredGeometryGrid(poseStack, builder, vertPreludeRad * 0.75f, -animTime * 1.2f, 0.8f, 0.3f, 0.95f);
            poseStack.popPose();

            // 3. 中心で脈動する休眠状態の種根核 (Genesis Seed Core)
            poseStack.pushPose();
            float corePulse = 0.8f + preludeProg * 0.4f;
            drawGenesisCore(poseStack, builder, animTime, corePulse);
            poseStack.popPose();

            // 4. 岩盤から立ち昇る前兆の微光柱
            poseStack.pushPose();
            float thinPillar = 0.8f * preludeProg;
            drawEnergyPillar(poseStack, builder, thinPillar, -1.0f, 16.0f * preludeProg, animTime);
            poseStack.popPose();
            return;
        }

        // -------------------------------------------------------------------------
        // PHASE 2: 地上隆起・大覚醒フェーズ (5〜30秒 = 100〜600 ticks)
        // -------------------------------------------------------------------------
        float risingTicks = ticks - ArborisEntity.PRELUDE_DURATION;
        float totalRisingDuration = ArborisEntity.AWAKENING_DURATION - ArborisEntity.PRELUDE_DURATION;
        float progress = Mth.clamp(risingTicks / totalRisingDuration, 0.0f, 1.0f);

        // 1. 岩盤から天空(高度320)へと貫通し続ける極太の原初光柱 (Pillar of Genesis)
        poseStack.pushPose();
        float startRelY = (float) (entity.getStartY() - entity.getY()) - 2.0f;
        float topRelY = 280.0f;
        float pillarRadius = 3.0f + progress * 3.5f;
        float pulse = 0.88f + 0.12f * Mth.sin(animTime * 0.18f);
        drawEnergyPillar(poseStack, builder, pillarRadius * pulse, startRelY, topRelY, animTime);
        poseStack.popPose();

        // 2. 足元の岩盤/地表に広がる水平大魔方陣（上下方向の視点用、水平面維持）
        poseStack.pushPose();
        poseStack.translate(0.0, 0.05, 0.0);
        float circleRadius = 6.0f + progress * 12.0f;
        drawMagicCircle(poseStack, builder, circleRadius, animTime, 0.1f, 0.95f, 0.4f, 0.9f);
        drawSacredGeometryGrid(poseStack, builder, circleRadius * 0.85f, animTime * 0.8f, 0.15f, 0.85f, 0.85f);
        poseStack.popPose();

        // 3. 横方向から見た際に常にプレイヤーの位置を向く直立超巨大魔方陣 (位置基準のY軸追従、視線ブレなし)
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(yawToPlayer));
        poseStack.translate(0.0, 5.0 + progress * 2.0f, -1.2);
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0f));
        float vertCircleRad = 7.0f + progress * 14.0f; // 半径7〜21mの超巨大正面魔方陣
        drawMagicCircle(poseStack, builder, vertCircleRad, animTime, 0.1f, 0.95f, 0.4f, 0.95f);
        drawMagicCircle(poseStack, builder, vertCircleRad * 0.65f, -animTime * 1.5f, 0.7f, 0.2f, 0.95f, 0.9f);
        drawSacredGeometryGrid(poseStack, builder, vertCircleRad * 0.85f, animTime * 0.8f, 0.15f, 0.85f, 0.9f);
        poseStack.popPose();

        // 4. 地中から徐々に地上へ蠢きながら突き出てくる原初の根 (Emerging Roots)
        poseStack.pushPose();
        float rootScale = 0.4f + progress * 0.8f;
        poseStack.scale(rootScale, rootScale, rootScale);
        drawWrithingRoots(poseStack, builder, animTime, progress);
        drawGenesisCore(poseStack, builder, animTime, 1.5f + progress * 1.5f);
        poseStack.popPose();
    }

    /**
     * 完全顕現後の最強の威容（植物の根の巨躯・多重魔方陣・神聖幾何学オービタル）
     */
    private void renderFullAwakenedBoss(ArborisEntity entity, float animTime, float yawToPlayer, PoseStack poseStack, VertexConsumer builder) {
        boolean lastStand = entity.isLastStand();
        float timeScale = lastStand ? animTime * 2.2f : animTime;

        // 浮遊アニメーション
        float hover = Mth.sin(animTime * 0.04f) * (lastStand ? 0.6f : 0.35f);
        poseStack.translate(0.0, 1.5 + hover, 0.0);

        // ラストスタンド時の終焉特異点エフェクト (天を衝く深紅の光柱・4基の崩壊特異点ノード)
        if (lastStand) {
            renderLastStandAura(poseStack, builder, animTime);
        }

        // 1. ボスの足元に広がる水平大魔方陣（上下視点用、水平面維持）
        poseStack.pushPose();
        poseStack.translate(0.0, -1.0, 0.0);
        float bottomRad = lastStand ? 15.0f : 9.0f;
        float bR = lastStand ? 1.0f : 0.1f;
        float bG = lastStand ? 0.15f : 0.9f;
        float bB = lastStand ? 0.3f : 0.45f;
        drawMagicCircle(poseStack, builder, bottomRad, timeScale * 0.6f, bR, bG, bB, 0.9f);
        drawSacredGeometryGrid(poseStack, builder, bottomRad * 0.8f, timeScale * 0.4f, lastStand ? 0.9f : 0.2f, lastStand ? 0.2f : 0.85f, lastStand ? 0.4f : 0.85f);
        poseStack.popPose();

        // 2. ボスの背後に背負う神聖幾何学の後光大魔方陣
        // （プレイヤーの位置ベクトルに基づくY軸追従：視線を振っても一切ブレず、ボスの周囲を回った時のみ正面を向き続ける）
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(yawToPlayer));
        poseStack.translate(0.0, 5.0, -1.2);
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0f)); // 垂直に立てる
        float backRad = lastStand ? 13.5f : 8.5f;
        float backR1 = lastStand ? 1.0f : 0.95f;
        float backG1 = lastStand ? 0.2f : 0.8f;
        float backB1 = lastStand ? 0.25f : 0.2f;
        drawMagicCircle(poseStack, builder, backRad, timeScale * 0.8f, backR1, backG1, backB1, 0.98f);
        drawMagicCircle(poseStack, builder, backRad * 0.68f, -timeScale * 1.3f, lastStand ? 0.9f : 0.6f, lastStand ? 0.1f : 0.25f, lastStand ? 0.85f : 0.95f, 0.92f);
        drawSacredGeometryGrid(poseStack, builder, backRad * 0.85f, -timeScale * 0.6f, lastStand ? 1.0f : 0.2f, lastStand ? 0.35f : 0.95f, lastStand ? 0.2f : 0.8f);
        poseStack.popPose();

        // 3. 螺旋状に絡み合い脈動する「原初の根」本体 (Writhing Root Monolith)
        poseStack.pushPose();
        drawWrithingRoots(poseStack, builder, animTime, 1.0f);
        poseStack.popPose();

        // 4. 原初の根核 (Genesis Core / Root Heart)
        float coreSize = lastStand ? 3.2f : 2.4f;
        drawGenesisCore(poseStack, builder, timeScale, coreSize);

        // 5. ボスの周りを公転する幾何学多面体オービタル (Sacred Geometry Orbits)
        drawGeometricOrbits(poseStack, builder, animTime);

        // 6. 神聖幾何学反射バリア（被弾後2秒間の無敵・反射モード時に発動）
        if (entity.isInvulnerableShieldActive()) {
            drawReflectShield(poseStack, builder, animTime);
        }

        // 7. 半径40ブロックの円形封絶結界壁 (ラストスタンド中のみ展開)
        if (lastStand) {
            poseStack.pushPose();
            drawBarrierWall(poseStack, builder, (float) ArborisEntity.BARRIER_RADIUS, -12.0f, 96.0f, animTime, true);
            poseStack.popPose();
        }
    }

    /**
     * 半径40ブロックの円形封絶結界（天を貫く多重リング & 垂直神聖ルーン柱 & 発光結界壁）
     */
    private void drawBarrierWall(PoseStack poseStack, VertexConsumer builder, float radius, float minY, float maxY, float animTime, boolean lastStand) {
        Matrix4f mat = poseStack.last().pose();
        int sides = 48;

        float r = lastStand ? 1.0f : 0.15f;
        float g = lastStand ? 0.15f : 0.9f;
        float b = lastStand ? 0.25f : 0.65f;
        float wallAlpha = lastStand ? 0.38f : 0.22f;
        float lineAlpha = lastStand ? 0.85f : 0.65f;

        float rot = animTime * (lastStand ? 0.15f : 0.04f);

        // 1. 円筒状の透過バリア壁 (表裏両面)
        for (int i = 0; i < sides; i++) {
            float a1 = rot + (float) (2.0 * Math.PI / sides) * i;
            float a2 = rot + (float) (2.0 * Math.PI / sides) * (i + 1);

            float x1 = Mth.cos(a1) * radius;
            float z1 = Mth.sin(a1) * radius;
            float x2 = Mth.cos(a2) * radius;
            float z2 = Mth.sin(a2) * radius;

            // 表面
            builder.vertex(mat, x1, minY, z1).color(r, g, b, wallAlpha).endVertex();
            builder.vertex(mat, x2, minY, z2).color(r, g, b, wallAlpha).endVertex();
            builder.vertex(mat, x2, maxY, z2).color(r, g, b, wallAlpha * 0.4f).endVertex();
            builder.vertex(mat, x1, maxY, z1).color(r, g, b, wallAlpha * 0.4f).endVertex();

            // 裏面
            builder.vertex(mat, x1, maxY, z1).color(r, g, b, wallAlpha * 0.4f).endVertex();
            builder.vertex(mat, x2, maxY, z2).color(r, g, b, wallAlpha * 0.4f).endVertex();
            builder.vertex(mat, x2, minY, z2).color(r, g, b, wallAlpha).endVertex();
            builder.vertex(mat, x1, minY, z1).color(r, g, b, wallAlpha).endVertex();
        }

        // 2. 結界を補強する上下・中段の多重境界リング (高度 -2m, 20m, 50m)
        float[] ringHeights = new float[]{-2.0f, 20.0f, 50.0f, 85.0f};
        for (float h : ringHeights) {
            poseStack.pushPose();
            poseStack.translate(0.0, h, 0.0);
            Matrix4f rMat = poseStack.last().pose();
            drawRing(builder, rMat, radius, 0.25f, r, g, b, lineAlpha);
            poseStack.popPose();
        }

        // 3. 結界壁に等間隔でそびえ立つ 16本の垂直神聖ルーン光柱
        int pillars = 16;
        for (int p = 0; p < pillars; p++) {
            float pa = rot * 0.5f + (float) (2.0 * Math.PI / pillars) * p;
            float px = Mth.cos(pa) * radius;
            float pz = Mth.sin(pa) * radius;
            drawLine(builder, mat, px, minY, pz, px, maxY, pz, 0.22f, r, g, b, lineAlpha);
        }
    }

    /**
     * ラストスタンド限定：終焉特異点オーラ & 天空を突く終焉深紅光柱 & 4基の崩壊特異点ノード
     */
    private void renderLastStandAura(PoseStack poseStack, VertexConsumer builder, float animTime) {
        // 1. 天空を貫く深紅の終焉光柱 (半径 3.8m、高さ 256m)
        poseStack.pushPose();
        drawEnergyPillar(poseStack, builder, 3.8f, -2.0f, 256.0f, animTime * 1.5f);
        poseStack.popPose();

        // 2. ボス周囲を高速公転する4基の崩壊特異点ノード
        int nodes = 4;
        for (int i = 0; i < nodes; i++) {
            float angle = (float) (2.0 * Math.PI / nodes) * i + animTime * 0.12f;
            float r = 8.5f + 1.2f * Mth.sin(animTime * 0.08f + i);
            float y = 5.0f + 2.5f * Mth.cos(animTime * 0.1f + i);
            float x = Mth.cos(angle) * r;
            float z = Mth.sin(angle) * r;

            poseStack.pushPose();
            poseStack.translate(x, y, z);
            poseStack.mulPose(Axis.YP.rotationDegrees(animTime * 8.0f));
            poseStack.mulPose(Axis.ZP.rotationDegrees(animTime * 5.0f));
            Matrix4f nMat = poseStack.last().pose();

            // 崩壊特異点（赤黒い発光八面体＋外周回転リング）
            drawOctahedron(builder, nMat, 1.2f, 1.0f, 0.15f, 0.2f, 0.95f);
            drawOctahedron(builder, nMat, 0.6f, 1.0f, 0.9f, 0.95f, 1.0f);
            drawRing(builder, nMat, 1.6f, 0.1f, 0.9f, 0.1f, 0.6f, 0.85f);
            poseStack.popPose();
        }
    }

    private void drawReflectShield(PoseStack poseStack, VertexConsumer builder, float animTime) {
        poseStack.pushPose();
        poseStack.translate(0.0, 5.0, 0.0);
        float pulse = 7.5f + 0.3f * Mth.sin(animTime * 0.3f);
        poseStack.mulPose(Axis.YP.rotationDegrees(animTime * 4.0f));
        poseStack.mulPose(Axis.XP.rotationDegrees(animTime * 2.5f));
        Matrix4f mat = poseStack.last().pose();

        // 黄金と真紅の反射結晶シールド
        drawOctahedron(builder, mat, pulse, 1.0f, 0.3f, 0.15f, 0.55f);
        drawOctahedron(builder, mat, pulse * 0.95f, 1.0f, 0.85f, 0.2f, 0.45f);

        // 外周回転バリアリング
        drawRing(builder, mat, pulse * 1.15f, 0.15f, 1.0f, 0.4f, 0.1f, 0.9f);
        poseStack.popPose();
    }

    /**
     * 幾重にも絡み合う8本の巨大な主根のプロシージャル描画（立体螺旋・脈動）
     */
    private void drawWrithingRoots(PoseStack poseStack, VertexConsumer builder, float animTime, float maturity) {
        int rootCount = 8;
        int segments = 24;
        float height = 12.0f * maturity;

        Matrix4f mat = poseStack.last().pose();

        for (int r = 0; r < rootCount; r++) {
            float baseAngle = (float) (2.0 * Math.PI / rootCount) * r;
            float cr = 0.08f + 0.12f * (r % 3 == 0 ? 1.0f : 0.0f);
            float cg = 0.65f + 0.35f * Mth.sin(animTime * 0.05f + r);
            float cb = 0.35f + 0.45f * Mth.cos(animTime * 0.04f + r * 0.5f);

            for (int s = 0; s < segments - 1; s++) {
                float t1 = (float) s / (float) segments;
                float t2 = (float) (s + 1) / (float) segments;

                float y1 = t1 * height;
                float y2 = t2 * height;

                float rad1 = (2.2f + 1.2f * Mth.sin(t1 * Mth.PI * 1.5f)) * (1.0f + 0.1f * Mth.sin(animTime * 0.08f + y1));
                float rad2 = (2.2f + 1.2f * Mth.sin(t2 * Mth.PI * 1.5f)) * (1.0f + 0.1f * Mth.sin(animTime * 0.08f + y2));

                float twist1 = baseAngle + y1 * 0.25f + Mth.sin(animTime * 0.04f + t1 * 3.0f) * 0.3f;
                float twist2 = baseAngle + y2 * 0.25f + Mth.sin(animTime * 0.04f + t2 * 3.0f) * 0.3f;

                float x1 = Mth.cos(twist1) * rad1;
                float z1 = Mth.sin(twist1) * rad1;
                float x2 = Mth.cos(twist2) * rad2;
                float z2 = Mth.sin(twist2) * rad2;

                float width1 = (0.55f * (1.0f - t1 * 0.6f)) * maturity;
                float width2 = (0.55f * (1.0f - t2 * 0.6f)) * maturity;

                drawRibbonSegment(builder, mat, x1, y1, z1, x2, y2, z2, width1, width2, cr, cg, cb, 0.95f);
            }
        }
    }

    private void drawRibbonSegment(VertexConsumer builder, Matrix4f mat,
                                   float x1, float y1, float z1, float x2, float y2, float z2,
                                   float w1, float w2, float r, float g, float b, float a) {
        // 正面
        builder.vertex(mat, x1 - w1, y1, z1).color(r, g, b, a).endVertex();
        builder.vertex(mat, x1 + w1, y1, z1).color(r, g, b, a).endVertex();
        builder.vertex(mat, x2 + w2, y2, z2).color(r, g, b, a).endVertex();
        builder.vertex(mat, x2 - w2, y2, z2).color(r, g, b, a).endVertex();

        // 正面裏
        builder.vertex(mat, x2 - w2, y2, z2).color(r, g, b, a).endVertex();
        builder.vertex(mat, x2 + w2, y2, z2).color(r, g, b, a).endVertex();
        builder.vertex(mat, x1 + w1, y1, z1).color(r, g, b, a).endVertex();
        builder.vertex(mat, x1 - w1, y1, z1).color(r, g, b, a).endVertex();

        // 側面
        builder.vertex(mat, x1, y1, z1 - w1).color(r * 0.8f, g * 0.8f, b * 0.8f, a).endVertex();
        builder.vertex(mat, x1, y1, z1 + w1).color(r * 0.8f, g * 0.8f, b * 0.8f, a).endVertex();
        builder.vertex(mat, x2, y2, z2 + w2).color(r * 0.8f, g * 0.8f, b * 0.8f, a).endVertex();
        builder.vertex(mat, x2, y2, z2 - w2).color(r * 0.8f, g * 0.8f, b * 0.8f, a).endVertex();

        // 側面裏
        builder.vertex(mat, x2, y2, z2 - w2).color(r * 0.8f, g * 0.8f, b * 0.8f, a).endVertex();
        builder.vertex(mat, x2, y2, z2 + w2).color(r * 0.8f, g * 0.8f, b * 0.8f, a).endVertex();
        builder.vertex(mat, x1, y1, z1 + w1).color(r * 0.8f, g * 0.8f, b * 0.8f, a).endVertex();
        builder.vertex(mat, x1, y1, z1 - w1).color(r * 0.8f, g * 0.8f, b * 0.8f, a).endVertex();
    }

    /**
     * 多重同心円と幾何学目盛りによる美麗な魔方陣サークル
     */
    private void drawMagicCircle(PoseStack poseStack, VertexConsumer builder, float radius, float rotDeg, float r, float g, float b, float a) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(rotDeg));

        Matrix4f mat = poseStack.last().pose();
        float ringWidth = radius * 0.045f;

        // 外周二重リング
        drawRing(builder, mat, radius, ringWidth, r, g, b, a);
        drawRing(builder, mat, radius * 0.92f, ringWidth * 0.6f, r, g, b, a * 0.75f);

        // 円周上の12分割放射ライン（アストロラーベ・時計目盛り風）
        int ticks = 12;
        for (int i = 0; i < ticks; i++) {
            float angle = (float) (2.0 * Math.PI / ticks) * i;
            float cos = Mth.cos(angle);
            float sin = Mth.sin(angle);
            float inner = radius * 0.85f;
            float outer = radius * 0.92f;

            drawLine(builder, mat, cos * inner, 0.0f, sin * inner, cos * outer, 0.0f, sin * outer, 0.08f, r, g, b, a);
        }

        // 内周リング
        drawRing(builder, mat, radius * 0.45f, ringWidth * 0.8f, r, g, b, a * 0.85f);

        poseStack.popPose();
    }

    /**
     * 神聖幾何学（八芒星・十二芒星・幾何学トライアングル）のライン描画
     */
    private void drawSacredGeometryGrid(PoseStack poseStack, VertexConsumer builder, float radius, float rotDeg, float r, float g, float b) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(rotDeg));
        Matrix4f mat = poseStack.last().pose();

        // 八芒星 (Octagram) - 2つの正方形の交差
        drawPolygonStar(builder, mat, radius, 4, 0.0f, r, g, b);
        drawPolygonStar(builder, mat, radius, 4, 45.0f, r * 0.9f, g * 0.9f, b * 0.9f);

        // 六芒星 (Hexagram) - 内部の幾何学調和
        float innerRad = radius * 0.65f;
        drawPolygonStar(builder, mat, innerRad, 3, 0.0f, 0.95f, 0.85f, 0.2f);
        drawPolygonStar(builder, mat, innerRad, 3, 60.0f, 0.95f, 0.85f, 0.2f);

        poseStack.popPose();
    }

    private void drawPolygonStar(VertexConsumer builder, Matrix4f mat, float radius, int vertices, float offsetDeg, float r, float g, float b) {
        float step = (float) (2.0 * Math.PI / vertices);
        float offsetRad = (float) Math.toRadians(offsetDeg);

        for (int i = 0; i < vertices; i++) {
            float a1 = offsetRad + step * i;
            float a2 = offsetRad + step * (i + 1);

            float x1 = Mth.cos(a1) * radius;
            float z1 = Mth.sin(a1) * radius;
            float x2 = Mth.cos(a2) * radius;
            float z2 = Mth.sin(a2) * radius;

            drawLine(builder, mat, x1, 0.0f, z1, x2, 0.0f, z2, 0.07f, r, g, b, 0.85f);
        }
    }

    /**
     * 両面クワッドによる発光リング描画（表裏両面から均等に描画）
     */
    private void drawRing(VertexConsumer builder, Matrix4f mat, float radius, float width, float r, float g, float b, float a) {
        int steps = 48;

        for (int i = 0; i < steps; i++) {
            float a1 = (float) (2.0 * Math.PI / steps) * i;
            float a2 = (float) (2.0 * Math.PI / steps) * (i + 1);

            float cos1 = Mth.cos(a1);
            float sin1 = Mth.sin(a1);
            float cos2 = Mth.cos(a2);
            float sin2 = Mth.sin(a2);

            float rIn = radius - width;
            float rOut = radius + width;

            // リング（表面）
            builder.vertex(mat, cos1 * rIn, 0.0f, sin1 * rIn).color(r, g, b, a).endVertex();
            builder.vertex(mat, cos1 * rOut, 0.0f, sin1 * rOut).color(r, g, b, a).endVertex();
            builder.vertex(mat, cos2 * rOut, 0.0f, sin2 * rOut).color(r, g, b, a).endVertex();
            builder.vertex(mat, cos2 * rIn, 0.0f, sin2 * rIn).color(r, g, b, a).endVertex();

            // リング（裏面）
            builder.vertex(mat, cos2 * rIn, 0.0f, sin2 * rIn).color(r, g, b, a).endVertex();
            builder.vertex(mat, cos2 * rOut, 0.0f, sin2 * rOut).color(r, g, b, a).endVertex();
            builder.vertex(mat, cos1 * rOut, 0.0f, sin1 * rOut).color(r, g, b, a).endVertex();
            builder.vertex(mat, cos1 * rIn, 0.0f, sin1 * rIn).color(r, g, b, a).endVertex();
        }
    }

    /**
     * 3次元空間における全方位立体ライン描画（直交する2枚の両面クワッドによるクロスビーム構造）
     * 垂直線・水平線・斜め線を問わず、どの角度・距離から見ても消えず見切れない
     */
    private void drawLine(VertexConsumer builder, Matrix4f mat,
                          float x1, float y1, float z1,
                          float x2, float y2, float z2,
                          float width, float r, float g, float b, float a) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float dz = z2 - z1;
        float len = Mth.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 0.0001f) return;

        float dirX = dx / len;
        float dirY = dy / len;
        float dirZ = dz / len;

        // 方向ベクトルと平行にならない基準ベクトル(up)を選択
        float upX = 0.0f, upY = 1.0f, upZ = 0.0f;
        if (Math.abs(dirY) > 0.85f) {
            upX = 1.0f;
            upY = 0.0f;
            upZ = 0.0f;
        }

        // 第1の直交法線ベクトル (p1 = dir × up)
        float p1x = dirY * upZ - dirZ * upY;
        float p1y = dirZ * upX - dirX * upZ;
        float p1z = dirX * upY - dirY * upX;
        float p1Len = Mth.sqrt(p1x * p1x + p1y * p1y + p1z * p1z);
        if (p1Len < 0.0001f) return;
        p1x = (p1x / p1Len) * width;
        p1y = (p1y / p1Len) * width;
        p1z = (p1z / p1Len) * width;

        // 第2の直交法線ベクトル (p2 = dir × p1)
        float p2x = dirY * p1z - dirZ * p1y;
        float p2y = dirZ * p1x - dirX * p1z;
        float p2z = dirX * p1y - dirY * p1x;
        float p2Len = Mth.sqrt(p2x * p2x + p2y * p2y + p2z * p2z);
        if (p2Len < 0.0001f) return;
        p2x = (p2x / p2Len) * width;
        p2y = (p2y / p2Len) * width;
        p2z = (p2z / p2Len) * width;

        // クワッド 1: p1 方向 (表面・裏面)
        builder.vertex(mat, x1 - p1x, y1 - p1y, z1 - p1z).color(r, g, b, a).endVertex();
        builder.vertex(mat, x1 + p1x, y1 + p1y, z1 + p1z).color(r, g, b, a).endVertex();
        builder.vertex(mat, x2 + p1x, y2 + p1y, z2 + p1z).color(r, g, b, a).endVertex();
        builder.vertex(mat, x2 - p1x, y2 - p1y, z2 - p1z).color(r, g, b, a).endVertex();

        builder.vertex(mat, x2 - p1x, y2 - p1y, z2 - p1z).color(r, g, b, a).endVertex();
        builder.vertex(mat, x2 + p1x, y2 + p1y, z2 + p1z).color(r, g, b, a).endVertex();
        builder.vertex(mat, x1 + p1x, y1 + p1y, z1 + p1z).color(r, g, b, a).endVertex();
        builder.vertex(mat, x1 - p1x, y1 - p1y, z1 - p1z).color(r, g, b, a).endVertex();

        // クワッド 2: p2 方向 (表面・裏面)
        builder.vertex(mat, x1 - p2x, y1 - p2y, z1 - p2z).color(r, g, b, a).endVertex();
        builder.vertex(mat, x1 + p2x, y1 + p2y, z1 + p2z).color(r, g, b, a).endVertex();
        builder.vertex(mat, x2 + p2x, y2 + p2y, z2 + p2z).color(r, g, b, a).endVertex();
        builder.vertex(mat, x2 - p2x, y2 - p2y, z2 - p2z).color(r, g, b, a).endVertex();

        builder.vertex(mat, x2 - p2x, y2 - p2y, z2 - p2z).color(r, g, b, a).endVertex();
        builder.vertex(mat, x2 + p2x, y2 + p2y, z2 + p2z).color(r, g, b, a).endVertex();
        builder.vertex(mat, x1 + p2x, y1 + p2y, z1 + p2z).color(r, g, b, a).endVertex();
        builder.vertex(mat, x1 - p2x, y1 - p2y, z1 - p2z).color(r, g, b, a).endVertex();
    }

    /**
     * 原初の根核 (Genesis Core / Root Heart) - 脈動する巨大なエネルギー結晶
     */
    private void drawGenesisCore(PoseStack poseStack, VertexConsumer builder, float animTime, float size) {
        poseStack.pushPose();
        poseStack.translate(0.0, 4.5, 0.0);
        float pulse = size * (1.0f + 0.12f * Mth.sin(animTime * 0.12f));

        // 3軸自転
        poseStack.mulPose(Axis.YP.rotationDegrees(animTime * 1.8f));
        poseStack.mulPose(Axis.XP.rotationDegrees(animTime * 1.2f));
        poseStack.mulPose(Axis.ZP.rotationDegrees(animTime * 0.9f));

        Matrix4f mat = poseStack.last().pose();

        // 正八面体コア (Octahedron Crystal)
        float cr = 0.2f + 0.3f * Mth.sin(animTime * 0.05f);
        float cg = 0.95f;
        float cb = 0.7f + 0.3f * Mth.cos(animTime * 0.07f);

        drawOctahedron(builder, mat, pulse, cr, cg, cb, 0.9f);
        drawOctahedron(builder, mat, pulse * 0.55f, 1.0f, 1.0f, 1.0f, 0.98f);

        poseStack.popPose();
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
        // 表
        builder.vertex(mat, x1, y1, z1).color(r, g, b, a).endVertex();
        builder.vertex(mat, x2, y2, z2).color(r, g, b, a).endVertex();
        builder.vertex(mat, x3, y3, z3).color(r, g, b, a).endVertex();
        builder.vertex(mat, x1, y1, z1).color(r, g, b, a).endVertex();

        // 裏
        builder.vertex(mat, x3, y3, z3).color(r, g, b, a).endVertex();
        builder.vertex(mat, x2, y2, z2).color(r, g, b, a).endVertex();
        builder.vertex(mat, x1, y1, z1).color(r, g, b, a).endVertex();
        builder.vertex(mat, x3, y3, z3).color(r, g, b, a).endVertex();
    }

    /**
     * 神聖幾何学オービタル - ボスの周りを公転する浮遊幾何学多面体
     */
    private void drawGeometricOrbits(PoseStack poseStack, VertexConsumer builder, float animTime) {
        int orbCount = 4;
        float orbitRadius = 6.8f;

        for (int i = 0; i < orbCount; i++) {
            float angle = (float) (2.0 * Math.PI / orbCount) * i + animTime * 0.03f;
            float ox = Mth.cos(angle) * orbitRadius;
            float oz = Mth.sin(angle) * orbitRadius;
            float oy = 4.0f + Mth.sin(animTime * 0.08f + i * 1.5f) * 1.2f;

            poseStack.pushPose();
            poseStack.translate(ox, oy, oz);
            poseStack.mulPose(Axis.YP.rotationDegrees(animTime * 3.0f + i * 90.0f));
            poseStack.mulPose(Axis.XP.rotationDegrees(animTime * 2.0f));

            Matrix4f mat = poseStack.last().pose();
            drawWireCube(builder, mat, 0.75f, 0.2f, 0.95f, 0.85f);
            drawOctahedron(builder, mat, 0.35f, 0.95f, 0.8f, 0.2f, 0.95f);

            poseStack.popPose();
        }
    }

    private void drawWireCube(VertexConsumer builder, Matrix4f mat, float s, float r, float g, float b) {
        float w = 0.045f;
        // 下面4辺
        drawLine(builder, mat, -s, -s, -s,  s, -s, -s, w, r, g, b, 0.95f);
        drawLine(builder, mat,  s, -s, -s,  s, -s,  s, w, r, g, b, 0.95f);
        drawLine(builder, mat,  s, -s,  s, -s, -s,  s, w, r, g, b, 0.95f);
        drawLine(builder, mat, -s, -s,  s, -s, -s, -s, w, r, g, b, 0.95f);

        // 上面4辺
        drawLine(builder, mat, -s,  s, -s,  s,  s, -s, w, r, g, b, 0.95f);
        drawLine(builder, mat,  s,  s, -s,  s,  s,  s, w, r, g, b, 0.95f);
        drawLine(builder, mat,  s,  s,  s, -s,  s,  s, w, r, g, b, 0.95f);
        drawLine(builder, mat, -s,  s,  s, -s,  s, -s, w, r, g, b, 0.95f);

        // 垂直柱4辺
        drawLine(builder, mat, -s, -s, -s, -s,  s, -s, w, r, g, b, 0.95f);
        drawLine(builder, mat,  s, -s, -s,  s,  s, -s, w, r, g, b, 0.95f);
        drawLine(builder, mat,  s, -s,  s,  s,  s,  s, w, r, g, b, 0.95f);
        drawLine(builder, mat, -s, -s,  s, -s,  s,  s, w, r, g, b, 0.95f);
    }

    /**
     * 出現シークエンス中の天を衝く原初光柱 (Pillar of Genesis)
     */
    private void drawEnergyPillar(PoseStack poseStack, VertexConsumer builder, float radius, float minY, float maxY, float animTime) {
        if (radius <= 0.01f) return;
        Matrix4f mat = poseStack.last().pose();
        int sides = 24;

        // 1. 外層エネルギーシールドオーラ
        float rot = animTime * 0.06f;
        for (int i = 0; i < sides; i++) {
            float a1 = rot + (float) (2.0 * Math.PI / sides) * i;
            float a2 = rot + (float) (2.0 * Math.PI / sides) * (i + 1);

            float x1 = Mth.cos(a1) * radius;
            float z1 = Mth.sin(a1) * radius;
            float x2 = Mth.cos(a2) * radius;
            float z2 = Mth.sin(a2) * radius;

            float cr = 0.15f + 0.15f * Mth.sin(a1 + animTime * 0.1f);
            float cg = 0.95f;
            float cb = 0.5f + 0.4f * Mth.cos(a1);

            builder.vertex(mat, x1, minY, z1).color(cr, cg, cb, 0.65f).endVertex();
            builder.vertex(mat, x2, minY, z2).color(cr, cg, cb, 0.65f).endVertex();
            builder.vertex(mat, x2, maxY, z2).color(cr, cg, cb, 0.65f).endVertex();
            builder.vertex(mat, x1, maxY, z1).color(cr, cg, cb, 0.65f).endVertex();

            builder.vertex(mat, x1, maxY, z1).color(cr, cg, cb, 0.65f).endVertex();
            builder.vertex(mat, x2, maxY, z2).color(cr, cg, cb, 0.65f).endVertex();
            builder.vertex(mat, x2, minY, z2).color(cr, cg, cb, 0.65f).endVertex();
            builder.vertex(mat, x1, minY, z1).color(cr, cg, cb, 0.65f).endVertex();
        }

        // 2. 内層超高輝度コアビーム
        float coreRad = radius * 0.35f;
        float rotCore = -animTime * 0.1f;
        for (int i = 0; i < sides; i++) {
            float a1 = rotCore + (float) (2.0 * Math.PI / sides) * i;
            float a2 = rotCore + (float) (2.0 * Math.PI / sides) * (i + 1);

            float x1 = Mth.cos(a1) * coreRad;
            float z1 = Mth.sin(a1) * coreRad;
            float x2 = Mth.cos(a2) * coreRad;
            float z2 = Mth.sin(a2) * coreRad;

            builder.vertex(mat, x1, minY, z1).color(0.9f, 1.0f, 0.95f, 0.92f).endVertex();
            builder.vertex(mat, x2, minY, z2).color(0.9f, 1.0f, 0.95f, 0.92f).endVertex();
            builder.vertex(mat, x2, maxY, z2).color(0.9f, 1.0f, 0.95f, 0.92f).endVertex();
            builder.vertex(mat, x1, maxY, z1).color(0.9f, 1.0f, 0.95f, 0.92f).endVertex();

            builder.vertex(mat, x1, maxY, z1).color(0.9f, 1.0f, 0.95f, 0.92f).endVertex();
            builder.vertex(mat, x2, maxY, z2).color(0.9f, 1.0f, 0.95f, 0.92f).endVertex();
            builder.vertex(mat, x2, minY, z2).color(0.9f, 1.0f, 0.95f, 0.92f).endVertex();
            builder.vertex(mat, x1, minY, z1).color(0.9f, 1.0f, 0.95f, 0.92f).endVertex();
        }
    }
}
