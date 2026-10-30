package n4ture27.nature_collapse.ui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

public abstract class NatureBaseScreen extends Screen {
    protected long openTime;
    protected static final long ANIM_DURATION = 260; // ms

    protected NatureBaseScreen(Component title) {
        super(title);
    }

    @Override
    protected void init() {
        this.openTime = System.currentTimeMillis();
    }

    protected float getAnimProgress() {
        long elapsed = System.currentTimeMillis() - openTime;
        return Math.min(1.0f, (float) elapsed / ANIM_DURATION);
    }

    /**
     * スムースなCubic Outイージング（揺れのないスムーズな開閉）
     */
    protected float getAnimScale() {
        float progress = getAnimProgress();
        if (progress >= 1.0f) return 1.0f;
        return 1.0f - (float) Math.pow(1.0 - progress, 3);
    }

    protected float getAnimAlpha() {
        return Math.min(1.0f, getAnimProgress() * 1.2f);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics) {
        float alpha = getAnimAlpha();
        int darkAlpha = (int) (alpha * 0xD0);
        int color = (darkAlpha << 24) | 0x050A07;
        guiGraphics.fillGradient(0, 0, this.width, this.height, color, color | 0x020503);
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gui);

        float scale = getAnimScale();
        gui.pose().pushPose();
        gui.pose().translate(this.width / 2.0f, this.height / 2.0f, 0);
        gui.pose().scale(scale, scale, 1.0f);
        gui.pose().translate(-this.width / 2.0f, -this.height / 2.0f, 0);

        this.renderContents(gui, mouseX, mouseY, partialTick);
        super.render(gui, mouseX, mouseY, partialTick);

        gui.pose().popPose();
    }

    protected abstract void renderContents(GuiGraphics gui, int mouseX, int mouseY, float partialTick);

    /**
     * サイバー・ファンタジー調フロストガラスパネルの描画
     */
    protected void renderGlassPanel(GuiGraphics gui, int x, int y, int width, int height) {
        // 脈動グローカラーの計算（ディープエメラルド〜シアン）
        long time = System.currentTimeMillis();
        float pulse = (float) (0.6f + 0.4f * Math.sin(time / 450.0));
        int glowColor = Mth.hsvToRgb(0.38f + 0.04f * pulse, 0.85f, 0.65f + 0.35f * pulse);

        // 外周グローフレーム (外側の薄いグロー)
        gui.fill(x - 3, y - 3, x + width + 3, y + height + 3, 0x3300FF88);
        gui.fill(x - 2, y - 2, x + width + 2, y + height + 2, 0x6600AA55);

        // メイン枠線（エメラルドグラデーション）
        int topBorder = 0xFF00EE88;
        int botBorder = (0xFF << 24) | (glowColor & 0x00FFFFFF);
        gui.fillGradient(x - 1, y - 1, x + width + 1, y, topBorder, topBorder);
        gui.fillGradient(x - 1, y + height, x + width + 1, y + height + 1, botBorder, botBorder);
        gui.fillGradient(x - 1, y, x, y + height, topBorder, botBorder);
        gui.fillGradient(x + width, y, x + width + 1, y + height, topBorder, botBorder);

        // コーナーアクセントライン（サイバー調）
        int cornerLen = Math.min(10, Math.min(width, height) / 4);
        int cornerColor = 0xFF55FFAA;
        gui.fill(x - 2, y - 2, x + cornerLen, y, cornerColor);
        gui.fill(x - 2, y - 2, x, y + cornerLen, cornerColor);
        gui.fill(x + width - cornerLen, y - 2, x + width + 2, y, cornerColor);
        gui.fill(x + width, y - 2, x + width + 2, y + cornerLen, cornerColor);
        gui.fill(x - 2, y + height, x + cornerLen, y + height + 2, cornerColor);
        gui.fill(x - 2, y + height - cornerLen, x, y + height + 2, cornerColor);
        gui.fill(x + width - cornerLen, y + height, x + width + 2, y + height + 2, cornerColor);
        gui.fill(x + width, y + height - cornerLen, x + width + 2, y + height + 2, cornerColor);

        // 内部半透明フロストガラス（ディープフォレストグリーン〜ダークブラック）
        gui.fillGradient(x, y, x + width, y + height, 0xEB04170C, 0xF4010A05);

        // 上部かすかなハイライト光
        gui.fillGradient(x, y, x + width, y + Math.min(24, height / 2), 0x2800FF99, 0x00000000);
    }

    /**
     * 3Dアイテムアイコンの拡大・回転描画
     */
    protected void renderItemIcon3D(GuiGraphics gui, ItemStack stack, int centerX, int centerY, float scale, boolean rotating) {
        if (stack.isEmpty()) return;

        gui.pose().pushPose();
        gui.pose().translate(centerX, centerY, 150.0f);
        gui.pose().scale(scale, scale, 1.0f);

        if (rotating) {
            float rot = (System.currentTimeMillis() % 6000L) / 6000.0f * 360.0f;
            gui.pose().mulPose(com.mojang.math.Axis.YP.rotationDegrees(rot));
        }

        gui.renderItem(stack, -8, -8);
        gui.pose().popPose();
    }

    /**
     * ポーションエフェクトアイコンのテクスチャスプライト描画
     */
    public static void renderMobEffectIcon(GuiGraphics gui, MobEffect effect, int x, int y, int size) {
        if (effect == null) return;
        TextureAtlasSprite sprite = Minecraft.getInstance().getMobEffectTextures().get(effect);
        if (sprite != null) {
            RenderSystem.setShaderTexture(0, sprite.atlasLocation());
            gui.blit(x, y, 0, size, size, sprite);
        } else {
            // アイコンが見つからない場合のフォールバック（エフェクトカラーの四角）
            int color = (0xFF << 24) | (effect.getColor() & 0x00FFFFFF);
            gui.fill(x, y, x + size, y + size, color);
        }
    }
}
