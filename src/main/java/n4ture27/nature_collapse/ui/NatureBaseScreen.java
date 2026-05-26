package n4ture27.nature_collapse.ui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public abstract class NatureBaseScreen extends Screen {
    protected long openTime;
    protected static final long ANIM_DURATION = 250; //anime time

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

    protected float getAnimScale() {
        float progress = getAnimProgress();
        if (progress >= 1.0f) return 1.0f;
        return 1.0f - (float) Math.pow(1.0 - progress, 3);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics) {
        float progress = getAnimProgress();
        int alpha = (int) (progress * 0xC0);
        int color = (alpha << 24) | 0x101010;
        guiGraphics.fillGradient(0, 0, this.width, this.height, color, color);
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gui);
        
        float scale = getAnimScale();
        gui.pose().pushPose();
        gui.pose().translate(this.width / 2f, this.height / 2f, 0);
        gui.pose().scale(scale, scale, 1.0f);
        gui.pose().translate(-this.width / 2f, -this.height / 2f, 0);

        this.renderContents(gui, mouseX, mouseY, partialTick);
        super.render(gui, mouseX, mouseY, partialTick);

        gui.pose().popPose();
    }

    protected abstract void renderContents(GuiGraphics gui, int mouseX, int mouseY, float partialTick);

    protected void drawTitle(GuiGraphics gui, int y) {
        gui.drawCenteredString(this.font, this.title, this.width / 2, y, 0xFFFFFF);
    }

    protected void drawNaturePanel(GuiGraphics gui, int centerX, int centerY, int width, int height) {
        int hw = width / 2;
        int hh = height / 2;
        
        gui.fillGradient(centerX - hw - 5, centerY - hh - 5, centerX + hw + 5, centerY + hh + 5, 0xEE004400, 0xEE002200);
        gui.fill(centerX - hw, centerY - hh, centerX + hw, centerY + hh, 0xCC000000);
    }
}