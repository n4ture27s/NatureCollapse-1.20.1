package n4ture27.nature_collapse.ui.components;

import n4ture27.nature_collapse.ui.NatureBaseScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class EffectScrollList extends AbstractWidget {
    public static final int ITEM_HEIGHT = 24;

    private final List<MobEffect> allEffects = new ArrayList<>();
    private final List<MobEffect> visibleEffects = new ArrayList<>();
    private final Predicate<MobEffect> isActiveCheck;
    private final Consumer<MobEffect> onSelectEffect;

    private double scrollAmount = 0;
    private boolean isDraggingScrollBar = false;

    public EffectScrollList(int x, int y, int width, int height,
                            Predicate<MobEffect> isActiveCheck,
                            Consumer<MobEffect> onSelectEffect) {
        super(x, y, width, height, Component.empty());
        this.isActiveCheck = isActiveCheck;
        this.onSelectEffect = onSelectEffect;
    }

    public void setEffects(List<MobEffect> effects) {
        this.allEffects.clear();
        this.allEffects.addAll(effects);
        this.visibleEffects.clear();
        this.visibleEffects.addAll(effects);
        this.scrollAmount = Mth.clamp(this.scrollAmount, 0, getMaxScroll());
    }

    public void filter(String query) {
        this.visibleEffects.clear();
        String q = query != null ? query.trim().toLowerCase() : "";
        for (MobEffect effect : allEffects) {
            if (q.isEmpty()
                    || effect.getDisplayName().getString().toLowerCase().contains(q)
                    || effect.getDescriptionId().toLowerCase().contains(q)) {
                this.visibleEffects.add(effect);
            }
        }
        this.scrollAmount = Mth.clamp(this.scrollAmount, 0, getMaxScroll());
    }

    public int getMaxScroll() {
        return Math.max(0, this.visibleEffects.size() * ITEM_HEIGHT - this.height);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (this.isHoveredOrFocused()) {
            this.scrollAmount = Mth.clamp(this.scrollAmount - delta * 20.0, 0, getMaxScroll());
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.visible || !this.isHoveredOrFocused()) return false;

        // スクロールバー領域のクリック判定
        int scrollBarX = this.getX() + this.width - 6;
        if (mouseX >= scrollBarX && mouseX <= this.getX() + this.width) {
            this.isDraggingScrollBar = true;
            updateScrollFromMouse(mouseY);
            return true;
        }

        // リストアイテムのクリック判定
        if (mouseX >= this.getX() && mouseX < scrollBarX) {
            int relativeY = (int) (mouseY - this.getY() + this.scrollAmount);
            int index = relativeY / ITEM_HEIGHT;
            if (index >= 0 && index < this.visibleEffects.size()) {
                MobEffect clicked = this.visibleEffects.get(index);
                if (this.onSelectEffect != null) {
                    this.onSelectEffect.accept(clicked);
                    Minecraft.getInstance().getSoundManager().play(
                            net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                                    net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0f));
                }
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.isDraggingScrollBar = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.isDraggingScrollBar) {
            updateScrollFromMouse(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    private void updateScrollFromMouse(double mouseY) {
        double trackH = this.height;
        double thumbH = Math.max(20, (this.height / (double) Math.max(this.height, this.visibleEffects.size() * ITEM_HEIGHT)) * this.height);
        double scrollableH = trackH - thumbH;
        if (scrollableH > 0) {
            double relY = Mth.clamp(mouseY - this.getY() - thumbH / 2.0, 0, scrollableH);
            this.scrollAmount = (relY / scrollableH) * getMaxScroll();
        }
    }

    @Override
    public void renderWidget(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        int x = this.getX();
        int y = this.getY();
        int w = this.width;
        int h = this.height;

        // リスト背景枠線と半透明パネル
        gui.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xFF00AA55);
        gui.fill(x, y, x + w, y + h, 0xEE041209);

        // クリッピング有効化（はみ出し防止）
        gui.enableScissor(x, y, x + w, y + h);

        int startIndex = Math.max(0, (int) (this.scrollAmount / ITEM_HEIGHT));
        int endIndex = Math.min(this.visibleEffects.size(), (int) ((this.scrollAmount + h) / ITEM_HEIGHT) + 1);

        for (int i = startIndex; i < endIndex; i++) {
            MobEffect effect = this.visibleEffects.get(i);
            int itemY = y + i * ITEM_HEIGHT - (int) this.scrollAmount;
            boolean hovered = mouseX >= x && mouseX < x + w - 7 && mouseY >= itemY && mouseY < itemY + ITEM_HEIGHT;
            boolean active = this.isActiveCheck != null && this.isActiveCheck.test(effect);

            // アイテム背景
            if (active) {
                gui.fill(x + 1, itemY + 1, x + w - 7, itemY + ITEM_HEIGHT - 1, 0x5500FF88);
            } else if (hovered) {
                gui.fill(x + 1, itemY + 1, x + w - 7, itemY + ITEM_HEIGHT - 1, 0x4400AA55);
            }

            // ポーションエフェクトアイコン (18x18)
            NatureBaseScreen.renderMobEffectIcon(gui, effect, x + 4, itemY + 3, 18);

            // エフェクト名
            String name = effect.getDisplayName().getString();
            int nameColor = active ? 0x55FFAA : (hovered ? 0xFFFFFF : 0xDDDDDD);
            gui.drawString(mc.font, name, x + 26, itemY + 8, nameColor, false);

            // アクティブバッジ
            if (active) {
                int badgeW = 14;
                int badgeX = x + w - 24;
                gui.fill(badgeX, itemY + 6, badgeX + badgeW, itemY + 18, 0xFF00AA55);
                gui.drawCenteredString(mc.font, "✓", badgeX + badgeW / 2, itemY + 8, 0xFFFFFF);
            }
        }

        gui.disableScissor();

        // スクロールバー描画
        if (getMaxScroll() > 0) {
            int barX = x + w - 5;
            gui.fill(barX, y, barX + 4, y + h, 0x88000000);

            double thumbH = Math.max(16, (this.height / (double) (this.visibleEffects.size() * ITEM_HEIGHT)) * this.height);
            double thumbY = y + (this.scrollAmount / getMaxScroll()) * (h - thumbH);

            int thumbColor = isDraggingScrollBar ? 0xFFFFFFFF : 0xFF00FF88;
            gui.fill(barX, (int) thumbY, barX + 4, (int) (thumbY + thumbH), thumbColor);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }
}
