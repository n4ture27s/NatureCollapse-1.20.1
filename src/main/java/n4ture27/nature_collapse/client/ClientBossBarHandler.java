package n4ture27.nature_collapse.client;

import n4ture27.nature_collapse.NatureCollapse;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 原初の根源樹アルボリス専用のシンプルなカスタムボスバー。
 * 細いライン状のゲージ + 文字アニメーション（タイプライター出現 / 波打ち / 色の流れ）で構成する。
 */
@Mod.EventBusSubscriber(modid = NatureCollapse.ModId, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ClientBossBarHandler {

    private static final int BAR_WIDTH = 220;
    private static final int BAR_HEIGHT = 3;

    // ボスバーが最初に表示された時刻（タイプライター演出用）。名前の「種類」が変わるとリセット
    private static long revealStart = 0L;
    private static String lastKind = "";

    @SubscribeEvent
    public static void onBossBarRender(CustomizeGuiOverlayEvent.BossEventProgress event) {
        LerpingBossEvent bossEvent = event.getBossEvent();
        String raw = ChatFormatting.stripFormatting(bossEvent.getName().getString());
        if (raw == null) return;

        if (!raw.contains("アルボリス") && !raw.contains("Arboris") && !raw.contains("LAST STAND")
                && !raw.contains("共鳴の儀式") && !raw.contains("原初覚醒")) {
            return;
        }
        event.setCanceled(true);

        boolean lastStand = raw.contains("LAST STAND");
        boolean awakening = raw.contains("共鳴の儀式") || raw.contains("原初覚醒");

        String kind = lastStand ? "last" : (awakening ? "awake" : "combat");
        long now = System.currentTimeMillis();
        if (!kind.equals(lastKind)) {
            lastKind = kind;
            revealStart = now;
        }

        render(event.getGuiGraphics(), event.getWindow().getGuiScaledWidth(), event.getY(),
                bossEvent.getProgress(), raw, lastStand, awakening, now);

        event.setIncrement(24);
    }

    private static void render(GuiGraphics g, int screenWidth, int y, float progress, String raw,
                               boolean lastStand, boolean awakening, long now) {
        Font font = Minecraft.getInstance().font;
        float t = (now % 1_000_000L) / 1000.0f; // 秒
        int cx = screenWidth / 2;

        // ---- テキスト構成 ----
        String title;
        String sub;
        if (lastStand) {
            title = "LAST STAND";
            sub = extractBracket(raw); // 例: "27s | 結界耐久: 41"
        } else if (awakening) {
            title = "ARBORIS";
            sub = raw.replace("✦", "").replace("⚡", "").trim();
        } else {
            title = "ARBORIS";
            sub = "原初の根源樹";
        }

        // ---- タイトル: タイプライター出現 + 文字ごとの波打ち + 色の流れ ----
        float elapsed = (now - revealStart) / 1000.0f;
        int visible = Math.min(title.length(), (int) (elapsed * 18.0f) + 1);
        String spaced = spaced(title);
        int totalWidth = font.width(spaced);
        int x = cx - totalWidth / 2;

        int charIndex = 0;
        for (int i = 0; i < spaced.length(); i++) {
            char c = spaced.charAt(i);
            String s = String.valueOf(c);
            if (c != ' ') {
                if (charIndex >= visible) break;
                float wave = lastStand
                        ? Mth.sin(t * 9.0f + charIndex * 0.7f) * 1.2f + (float) (Math.random() - 0.5) * 0.8f
                        : Mth.sin(t * 3.0f + charIndex * 0.55f) * 1.0f;
                int color = titleColor(lastStand, t, charIndex);

                // 出現直後の文字はフラッシュ（白→本来の色）
                float charAge = elapsed - charIndex / 18.0f;
                if (charAge < 0.15f) color = 0xFFFFFFFF;

                g.pose().pushPose();
                g.pose().translate(0, wave, 0);
                g.drawString(font, s, x, y, color, true);
                g.pose().popPose();
                charIndex++;
            }
            x += font.width(s);
        }

        // ---- ゲージ（細いライン） ----
        int barX = cx - BAR_WIDTH / 2;
        int barY = y + 12;
        float p = Mth.clamp(progress, 0.0f, 1.0f);
        int fill = (int) (BAR_WIDTH * p);

        g.fill(barX - 1, barY - 1, barX + BAR_WIDTH + 1, barY + BAR_HEIGHT + 1, 0xA0000000);
        g.fill(barX, barY, barX + BAR_WIDTH, barY + BAR_HEIGHT, 0x60303030);

        int baseColor = lastStand ? 0xFFE0283C : (awakening ? 0xFFE8B83A : 0xFF3EE6A0);
        if (fill > 0) {
            g.fill(barX, barY, barX + fill, barY + BAR_HEIGHT, baseColor);
            // 流れるハイライト
            int sweepW = 24;
            float sweepSpeed = lastStand ? 1.6f : 0.6f;
            int sweepX = (int) ((t * sweepSpeed % 1.0f) * (BAR_WIDTH + sweepW)) - sweepW;
            int sx0 = Math.max(0, sweepX);
            int sx1 = Math.min(fill, sweepX + sweepW);
            if (sx1 > sx0) {
                g.fill(barX + sx0, barY, barX + sx1, barY + BAR_HEIGHT, 0x70FFFFFF);
            }
            // 先端
            g.fill(barX + fill - 1, barY - 1, barX + fill + 1, barY + BAR_HEIGHT + 1, 0xFFFFFFFF);
        }

        // 両端の小さな菱形マーカー
        int markColor = (baseColor & 0x00FFFFFF) | 0xC0000000;
        drawDiamond(g, barX - 5, barY + 1, markColor);
        drawDiamond(g, barX + BAR_WIDTH + 4, barY + 1, markColor);

        // ---- サブテキスト（小さめ・フェードイン） ----
        if (sub != null && !sub.isEmpty()) {
            float subAlpha = Mth.clamp((elapsed - 0.3f) / 0.5f, 0.0f, 1.0f);
            if (lastStand) {
                // 鼓動するように明滅
                subAlpha *= 0.7f + 0.3f * (0.5f + 0.5f * Mth.sin(t * 6.0f));
            }
            int a = (int) (subAlpha * 255) & 0xFF;
            if (a > 8) {
                int subColor = (a << 24) | (lastStand ? 0xFFB0B8 : 0xC8D8D0);
                g.pose().pushPose();
                g.pose().translate(cx, barY + 6, 0);
                g.pose().scale(0.75f, 0.75f, 1.0f);
                g.drawCenteredString(font, sub, 0, 0, subColor);
                g.pose().popPose();
            }
        }
    }

    private static int titleColor(boolean lastStand, float t, int i) {
        float k = 0.5f + 0.5f * Mth.sin(t * (lastStand ? 5.0f : 2.0f) - i * 0.45f);
        int r, gr, b;
        if (lastStand) {
            // 深紅 ⇄ 橙
            r = 255;
            gr = (int) Mth.lerp(k, 40, 150);
            b = (int) Mth.lerp(k, 50, 40);
        } else {
            // エメラルド ⇄ 淡い金
            r = (int) Mth.lerp(k, 90, 245);
            gr = (int) Mth.lerp(k, 235, 220);
            b = (int) Mth.lerp(k, 170, 120);
        }
        return 0xFF000000 | (r << 16) | (gr << 8) | b;
    }

    private static void drawDiamond(GuiGraphics g, int x, int y, int color) {
        g.fill(x, y - 2, x + 1, y + 3, color);
        g.fill(x - 1, y - 1, x + 2, y + 2, color);
        g.fill(x - 2, y, x + 3, y + 1, color);
    }

    private static String spaced(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            if (i > 0) sb.append(' ');
            sb.append(s.charAt(i));
        }
        return sb.toString();
    }

    private static String extractBracket(String raw) {
        int a = raw.lastIndexOf('[');
        int b = raw.lastIndexOf(']');
        if (a >= 0 && b > a) {
            return raw.substring(a + 1, b).trim();
        }
        return "";
    }
}
