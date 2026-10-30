package n4ture27.nature_collapse.ui.components;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public class NatureButton extends Button {
    private final boolean isAccent;

    public NatureButton(int x, int y, int width, int height, Component message, OnPress onPress) {
        this(x, y, width, height, message, onPress, false);
    }

    public NatureButton(int x, int y, int width, int height, Component message, OnPress onPress, boolean isAccent) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        this.isAccent = isAccent;
    }

    @Override
    public void renderWidget(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        boolean hovered = this.isHoveredOrFocused();

        // 背景色の計算
        int bgColorTop;
        int bgColorBottom;
        int borderColor;

        if (!this.active) {
            bgColorTop = 0xAA111A13;
            bgColorBottom = 0xAA0A100B;
            borderColor = 0x5533553E;
        } else if (hovered) {
            long time = System.currentTimeMillis();
            float pulse = (float) (0.7f + 0.3f * Math.sin(time / 250.0));
            int glow = Mth.hsvToRgb(0.38f, 0.9f, pulse);
            bgColorTop = isAccent ? 0xDD00552B : 0xDD0C2E1A;
            bgColorBottom = isAccent ? 0xEE00381C : 0xEE071E11;
            borderColor = (0xFF << 24) | (glow & 0x00FFFFFF);
        } else {
            bgColorTop = isAccent ? 0xCC004020 : 0xBB082012;
            bgColorBottom = isAccent ? 0xDD002A15 : 0xCC04140B;
            borderColor = isAccent ? 0xFF00DD77 : 0xFF00AA55;
        }

        // 外周枠線
        gui.fill(this.getX() - 1, this.getY() - 1, this.getX() + this.width + 1, this.getY() + this.height + 1, borderColor);
        // 背景グラデーション
        gui.fillGradient(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, bgColorTop, bgColorBottom);

        // ホバー時のかすかな上部ハイライト
        if (hovered && this.active) {
            gui.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + 1, 0xAAFFFFFF);
        }

        // テキスト色
        int textColor = !this.active ? 0x777777 : (hovered ? 0xFFFFFF : (isAccent ? 0x55FF99 : 0xCCFFDD));
        int textX = this.getX() + this.width / 2;
        int textY = this.getY() + (this.height - 8) / 2;

        gui.drawCenteredString(minecraft.font, this.getMessage(), textX, textY, textColor);
    }
}
