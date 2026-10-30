package n4ture27.nature_collapse.ui;

import n4ture27.nature_collapse.Config;
import n4ture27.nature_collapse.NatureCollapse;
import n4ture27.nature_collapse.network.UpdateRangePacket;
import n4ture27.nature_collapse.ui.components.NatureButton;
import n4ture27.nature_collapse.ui.components.NatureSlider;
import n4ture27.nature_collapse.utils.InventoryUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

public class RangeAdjustmentScreen extends NatureBaseScreen {
    private static final int PANEL_WIDTH = 320;
    private static final int PANEL_HEIGHT = 190;

    private final ItemStack stack;
    private final int slotId;
    private final boolean isCurio;
    private final String curiosIdentifier;

    private double range;
    private NatureSlider slider;
    private EditBox numberBox;
    private boolean isUpdatingFromBox = false;

    public RangeAdjustmentScreen(ItemStack stack, int slotId, boolean isCurio, String curiosIdentifier) {
        super(Component.translatable("gui.nature_collapse.range_settings")
                .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
        this.stack = stack;
        this.slotId = slotId;
        this.isCurio = isCurio;
        this.curiosIdentifier = curiosIdentifier;
        this.loadSettings();
    }

    private void loadSettings() {
        this.range = InventoryUtil.getSwordRange(stack);
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int panelTop = this.height / 2 - PANEL_HEIGHT / 2;
        double maxRange = Math.max(1.0, Config.maxSwordRange);

        // スライダー（幅 210）
        this.slider = new NatureSlider(
                centerX - 140,
                panelTop + 56,
                210,
                20,
                "Range: ",
                " blocks",
                1.0,
                maxRange,
                this.range,
                1.0,
                val -> {
                    this.range = val;
                    if (!isUpdatingFromBox && numberBox != null) {
                        numberBox.setValue(String.valueOf((int) Math.round(val)));
                    }
                }
        );
        this.addRenderableWidget(slider);

        // 数値直接入力ボックス（幅 60）
        this.numberBox = new EditBox(
                this.font,
                centerX + 80,
                panelTop + 56,
                60,
                20,
                Component.literal("Value")
        );
        this.numberBox.setValue(String.valueOf((int) Math.round(this.range)));
        this.numberBox.setFilter(str -> str.matches("\\d*"));
        this.numberBox.setResponder(text -> {
            if (!text.isEmpty()) {
                try {
                    double parsed = Double.parseDouble(text);
                    double clamped = Mth.clamp(parsed, 1.0, maxRange);
                    this.range = clamped;
                    this.isUpdatingFromBox = true;
                    this.slider.setActualValue(clamped);
                    this.isUpdatingFromBox = false;
                } catch (NumberFormatException ignored) {
                }
            }
        });
        this.addRenderableWidget(numberBox);

        // クイックプリセットボタン (横並び4つ)
        int presetY = panelTop + 86;
        int btnW = 68;
        int gap = 6;
        int startX = centerX - (btnW * 4 + gap * 3) / 2;

        this.addRenderableWidget(new NatureButton(
                startX, presetY, btnW, 20,
                Component.literal("10m (Def)"),
                btn -> applyPreset(Config.defaultSwordRange)
        ));

        this.addRenderableWidget(new NatureButton(
                startX + (btnW + gap), presetY, btnW, 20,
                Component.literal("25m"),
                btn -> applyPreset(25.0)
        ));

        this.addRenderableWidget(new NatureButton(
                startX + (btnW + gap) * 2, presetY, btnW, 20,
                Component.literal("50m"),
                btn -> applyPreset(50.0)
        ));

        this.addRenderableWidget(new NatureButton(
                startX + (btnW + gap) * 3, presetY, btnW, 20,
                Component.literal("Max (" + (int) maxRange + ")"),
                btn -> applyPreset(maxRange),
                true
        ));

        // 最下部アクションボタン (Reset, Done)
        int actionY = panelTop + PANEL_HEIGHT - 32;
        this.addRenderableWidget(new NatureButton(
                centerX - 130, actionY, 120, 22,
                Component.literal("Reset to Default"),
                btn -> applyPreset(Config.defaultSwordRange)
        ));

        this.addRenderableWidget(new NatureButton(
                centerX + 10, actionY, 120, 22,
                Component.literal("Done"),
                btn -> this.onClose(),
                true
        ));
    }

    private void applyPreset(double val) {
        double maxRange = Math.max(1.0, Config.maxSwordRange);
        this.range = Mth.clamp(val, 1.0, maxRange);
        if (this.slider != null) {
            this.slider.setActualValue(this.range);
        }
        if (this.numberBox != null) {
            this.numberBox.setValue(String.valueOf((int) Math.round(this.range)));
        }
    }

    @Override
    public void onClose() {
        this.saveSettings();
        super.onClose();
    }

    private void saveSettings() {
        InventoryUtil.setSwordRange(stack, range);
        NatureCollapse.CHANNEL.sendToServer(new UpdateRangePacket(range, slotId, isCurio, curiosIdentifier));
    }

    @Override
    protected void renderContents(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        int centerX = this.width / 2;
        int panelLeft = centerX - PANEL_WIDTH / 2;
        int panelTop = this.height / 2 - PANEL_HEIGHT / 2;

        // サイバー・フロストガラスパネル
        this.renderGlassPanel(gui, panelLeft, panelTop, PANEL_WIDTH, PANEL_HEIGHT);

        // タイトルと剣アイコン（横スライド・回転なしでタイトルの横に固定配置）
        int titleWidth = this.font.width(this.title);
        int totalHeaderWidth = 20 + titleWidth;
        int headerStartX = centerX - totalHeaderWidth / 2;
        int headerY = panelTop + 20;

        gui.renderItem(this.stack, headerStartX, headerY - 4);
        gui.drawString(this.font, this.title, headerStartX + 20, headerY, 0x55FFAA, true);

        // 現在の射程ステータスバー表示
        String modeText;
        int modeColor;
        if (this.range <= 10.0) {
            modeText = "Mode: Melee / Short Reach";
            modeColor = 0x88FF88;
        } else if (this.range <= 30.0) {
            modeText = "Mode: Medium Combat Reach";
            modeColor = 0xFFFF55;
        } else {
            modeText = "Mode: Long Range / Dimensional Snipe";
            modeColor = 0x55FFFF;
        }
        gui.drawCenteredString(this.font, modeText, centerX, panelTop + 118, modeColor);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.numberBox != null && this.numberBox.isFocused() && this.numberBox.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (this.numberBox != null && this.numberBox.isFocused() && this.numberBox.charTyped(codePoint, modifiers)) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
