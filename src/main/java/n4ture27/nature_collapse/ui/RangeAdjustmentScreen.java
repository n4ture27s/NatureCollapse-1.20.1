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
    private static final int PANEL_WIDTH = 340;
    private static final int PANEL_HEIGHT = 245;

    private final ItemStack stack;
    private final int slotId;
    private final boolean isCurio;
    private final String curiosIdentifier;

    private double range;
    private NatureSlider slider;
    private EditBox numberBox;
    private boolean isUpdatingFromBox = false;

    private boolean targetMonsters;
    private boolean targetPassive;
    private boolean targetPlayers;
    private boolean targetItemsAndXP;
    private boolean targetNonLiving;

    private NatureButton btnMonsters;
    private NatureButton btnPassive;
    private NatureButton btnPlayers;
    private NatureButton btnItemsXP;
    private NatureButton btnNonLiving;

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
        InventoryUtil.SwordTargetFilter filter = InventoryUtil.getSwordTargetFilter(stack);
        this.targetMonsters = filter.targetMonsters();
        this.targetPassive = filter.targetPassive();
        this.targetPlayers = filter.targetPlayers();
        this.targetItemsAndXP = filter.targetItemsAndXP();
        this.targetNonLiving = filter.targetNonLiving();
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int panelTop = this.height / 2 - PANEL_HEIGHT / 2;
        double maxRange = Math.max(1.0, Config.maxSwordRange);

        // 1. スライダー (幅 210)
        this.slider = new NatureSlider(
                centerX - 150,
                panelTop + 40,
                220,
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

        // 2. 数値直接入力ボックス (幅 60)
        this.numberBox = new EditBox(
                this.font,
                centerX + 85,
                panelTop + 40,
                65,
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

        // 3. クイックプリセットボタン (横並び4つ)
        int presetY = panelTop + 66;
        int btnW = 70;
        int gap = 6;
        int startX = centerX - (btnW * 4 + gap * 3) / 2;

        this.addRenderableWidget(new NatureButton(
                startX, presetY, btnW, 18,
                Component.literal("10m (Def)"),
                btn -> applyPreset(Config.defaultSwordRange)
        ));

        this.addRenderableWidget(new NatureButton(
                startX + (btnW + gap), presetY, btnW, 18,
                Component.literal("25m"),
                btn -> applyPreset(25.0)
        ));

        this.addRenderableWidget(new NatureButton(
                startX + (btnW + gap) * 2, presetY, btnW, 18,
                Component.literal("50m"),
                btn -> applyPreset(50.0)
        ));

        this.addRenderableWidget(new NatureButton(
                startX + (btnW + gap) * 3, presetY, btnW, 18,
                Component.literal("Max (" + (int) maxRange + ")"),
                btn -> applyPreset(maxRange),
                true
        ));

        // 4. ターゲットフィルターボタン (Row 1: 3ボタン)
        int filterRow1Y = panelTop + 108;
        int fBtnW1 = 96;
        int fGap = 6;
        int fStartX1 = centerX - (fBtnW1 * 3 + fGap * 2) / 2;

        this.btnMonsters = new NatureButton(
                fStartX1, filterRow1Y, fBtnW1, 20,
                getFilterComponent("Monsters", targetMonsters),
                btn -> {
                    targetMonsters = !targetMonsters;
                    updateFilterButtonLabels();
                }
        );
        this.addRenderableWidget(btnMonsters);

        this.btnPassive = new NatureButton(
                fStartX1 + (fBtnW1 + fGap), filterRow1Y, fBtnW1, 20,
                getFilterComponent("Passive", targetPassive),
                btn -> {
                    targetPassive = !targetPassive;
                    updateFilterButtonLabels();
                }
        );
        this.addRenderableWidget(btnPassive);

        this.btnPlayers = new NatureButton(
                fStartX1 + (fBtnW1 + fGap) * 2, filterRow1Y, fBtnW1, 20,
                getFilterComponent("Players", targetPlayers),
                btn -> {
                    targetPlayers = !targetPlayers;
                    updateFilterButtonLabels();
                }
        );
        this.addRenderableWidget(btnPlayers);

        // 5. ターゲットフィルターボタン (Row 2: 2ボタン)
        int filterRow2Y = panelTop + 132;
        int fBtnW2 = 147;
        int fStartX2 = centerX - (fBtnW2 * 2 + fGap) / 2;

        this.btnItemsXP = new NatureButton(
                fStartX2, filterRow2Y, fBtnW2, 20,
                getFilterComponent("Items & XP", targetItemsAndXP),
                btn -> {
                    targetItemsAndXP = !targetItemsAndXP;
                    updateFilterButtonLabels();
                }
        );
        this.addRenderableWidget(btnItemsXP);

        this.btnNonLiving = new NatureButton(
                fStartX2 + fBtnW2 + fGap, filterRow2Y, fBtnW2, 20,
                getFilterComponent("Misc Objects", targetNonLiving),
                btn -> {
                    targetNonLiving = !targetNonLiving;
                    updateFilterButtonLabels();
                }
        );
        this.addRenderableWidget(btnNonLiving);

        updateFilterButtonLabels();

        // 6. 最下部アクションボタン (Reset, Done)
        int actionY = panelTop + PANEL_HEIGHT - 32;
        this.addRenderableWidget(new NatureButton(
                centerX - 130, actionY, 120, 22,
                Component.literal("Reset to Default"),
                btn -> resetToDefault()
        ));

        this.addRenderableWidget(new NatureButton(
                centerX + 10, actionY, 120, 22,
                Component.literal("Done"),
                btn -> this.onClose(),
                true
        ));
    }

    private void updateFilterButtonLabels() {
        if (btnMonsters != null) btnMonsters.setMessage(getFilterComponent("Monsters", targetMonsters));
        if (btnPassive != null) btnPassive.setMessage(getFilterComponent("Passive", targetPassive));
        if (btnPlayers != null) btnPlayers.setMessage(getFilterComponent("Players", targetPlayers));
        if (btnItemsXP != null) btnItemsXP.setMessage(getFilterComponent("Items & XP", targetItemsAndXP));
        if (btnNonLiving != null) btnNonLiving.setMessage(getFilterComponent("Misc Objects", targetNonLiving));
    }

    private Component getFilterComponent(String name, boolean enabled) {
        return Component.literal(name + ": ")
                .withStyle(ChatFormatting.WHITE)
                .append(Component.literal(enabled ? "ON" : "OFF")
                        .withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.RED));
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

    private void resetToDefault() {
        applyPreset(Config.defaultSwordRange);
        this.targetMonsters = InventoryUtil.SwordTargetFilter.DEFAULT.targetMonsters();
        this.targetPassive = InventoryUtil.SwordTargetFilter.DEFAULT.targetPassive();
        this.targetPlayers = InventoryUtil.SwordTargetFilter.DEFAULT.targetPlayers();
        this.targetItemsAndXP = InventoryUtil.SwordTargetFilter.DEFAULT.targetItemsAndXP();
        this.targetNonLiving = InventoryUtil.SwordTargetFilter.DEFAULT.targetNonLiving();
        updateFilterButtonLabels();
    }

    @Override
    public void onClose() {
        this.saveSettings();
        super.onClose();
    }

    private void saveSettings() {
        InventoryUtil.setSwordRange(stack, range);
        InventoryUtil.setSwordTargetFilter(stack, new InventoryUtil.SwordTargetFilter(
                targetMonsters, targetPassive, targetPlayers, targetItemsAndXP, targetNonLiving
        ));
        NatureCollapse.CHANNEL.sendToServer(new UpdateRangePacket(
                range, slotId, isCurio, curiosIdentifier,
                targetMonsters, targetPassive, targetPlayers, targetItemsAndXP, targetNonLiving
        ));
    }

    @Override
    protected void renderContents(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        int centerX = this.width / 2;
        int panelLeft = centerX - PANEL_WIDTH / 2;
        int panelTop = this.height / 2 - PANEL_HEIGHT / 2;

        // サイバー・フロストガラスパネル
        this.renderGlassPanel(gui, panelLeft, panelTop, PANEL_WIDTH, PANEL_HEIGHT);

        // タイトルと剣アイコン
        int titleWidth = this.font.width(this.title);
        int totalHeaderWidth = 20 + titleWidth;
        int headerStartX = centerX - totalHeaderWidth / 2;
        int headerY = panelTop + 16;

        gui.renderItem(this.stack, headerStartX, headerY - 4);
        gui.drawString(this.font, this.title, headerStartX + 20, headerY, 0x55FFAA, true);

        // ターゲットフィルターのセクションラベル
        gui.drawCenteredString(this.font, Component.translatable("gui.nature_collapse.target_filtering").withStyle(ChatFormatting.AQUA), centerX, panelTop + 94, 0x55FFFF);

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
        gui.drawCenteredString(this.font, modeText, centerX, panelTop + 164, modeColor);
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
