package n4ture27.nature_collapse.ui;

import n4ture27.nature_collapse.NatureCollapse;
import n4ture27.nature_collapse.network.UpdateFlowerConfigPacket;
import n4ture27.nature_collapse.ui.components.NatureButton;
import n4ture27.nature_collapse.utils.InventoryUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class FlowerHubScreen extends NatureBaseScreen {

    private static final int PANEL_W = 340;
    private static final int PANEL_H = 224;

    private final ItemStack flowerStack;
    private final int slotIndex;
    private final boolean isCurio;
    private final String curioIdentifier;

    private InventoryUtil.FlowerConfig config;

    public FlowerHubScreen(ItemStack flowerStack, int slotIndex, boolean isCurio, String curioIdentifier) {
        super(Component.translatable("item.nature_collapse.nature_flower")
                .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
        this.flowerStack = flowerStack;
        this.slotIndex = slotIndex;
        this.isCurio = isCurio;
        this.curioIdentifier = curioIdentifier;
        this.config = InventoryUtil.getFlowerConfig(flowerStack);
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int panelLeft = centerX - PANEL_W / 2;
        int panelTop = this.height / 2 - PANEL_H / 2;

        int col1X = panelLeft + 16;
        int col2X = centerX + 6;
        int colW = 148;
        int rowH = 24;
        int rowGap = 6;
        int startY = panelTop + 52;

        // ==========================================
        // 列 1
        // ==========================================

        // 1. Flight (トグル + 詳細)
        int y0 = startY;
        this.addRenderableWidget(new NatureButton(
                col1X, y0, colW - 30, rowH,
                getToggleText("Flight", config.flightEnabled()),
                btn -> {
                    config = new InventoryUtil.FlowerConfig(
                            !config.flightEnabled(), config.flightSpeedH(), config.flightSpeedV(),
                            config.effectsEnabled(), config.invulEnabled(), config.saturationEnabled(),
                            config.magnetEnabled(), config.magnetRange(), config.speedEnabled(),
                            config.speedMult(), config.auraEnabled(), config.auraRange(), config.visionEnabled()
                    );
                    saveConfig();
                    btn.setMessage(getToggleText("Flight", config.flightEnabled()));
                },
                config.flightEnabled()
        ));
        this.addRenderableWidget(new NatureButton(
                col1X + colW - 26, y0, 26, rowH,
                Component.literal(">"),
                btn -> openDetail(FlowerDetailScreen.FeatureType.FLIGHT)
        ));

        // 2. Invulnerability (トグルのみ)
        int y1 = startY + (rowH + rowGap);
        this.addRenderableWidget(new NatureButton(
                col1X, y1, colW, rowH,
                getToggleText("Invulnerable", config.invulEnabled()),
                btn -> {
                    config = new InventoryUtil.FlowerConfig(
                            config.flightEnabled(), config.flightSpeedH(), config.flightSpeedV(),
                            config.effectsEnabled(), !config.invulEnabled(), config.saturationEnabled(),
                            config.magnetEnabled(), config.magnetRange(), config.speedEnabled(),
                            config.speedMult(), config.auraEnabled(), config.auraRange(), config.visionEnabled()
                    );
                    saveConfig();
                    btn.setMessage(getToggleText("Invulnerable", config.invulEnabled()));
                },
                config.invulEnabled()
        ));

        // 3. Saturation (トグルのみ)
        int y2 = startY + (rowH + rowGap) * 2;
        this.addRenderableWidget(new NatureButton(
                col1X, y2, colW, rowH,
                getToggleText("Saturation", config.saturationEnabled()),
                btn -> {
                    config = new InventoryUtil.FlowerConfig(
                            config.flightEnabled(), config.flightSpeedH(), config.flightSpeedV(),
                            config.effectsEnabled(), config.invulEnabled(), !config.saturationEnabled(),
                            config.magnetEnabled(), config.magnetRange(), config.speedEnabled(),
                            config.speedMult(), config.auraEnabled(), config.auraRange(), config.visionEnabled()
                    );
                    saveConfig();
                    btn.setMessage(getToggleText("Saturation", config.saturationEnabled()));
                },
                config.saturationEnabled()
        ));

        // 4. Vision (トグルのみ)
        int y3 = startY + (rowH + rowGap) * 3;
        this.addRenderableWidget(new NatureButton(
                col1X, y3, colW, rowH,
                getToggleText("Clear Vision", config.visionEnabled()),
                btn -> {
                    config = new InventoryUtil.FlowerConfig(
                            config.flightEnabled(), config.flightSpeedH(), config.flightSpeedV(),
                            config.effectsEnabled(), config.invulEnabled(), config.saturationEnabled(),
                            config.magnetEnabled(), config.magnetRange(), config.speedEnabled(),
                            config.speedMult(), config.auraEnabled(), config.auraRange(), !config.visionEnabled()
                    );
                    saveConfig();
                    btn.setMessage(getToggleText("Clear Vision", config.visionEnabled()));
                },
                config.visionEnabled()
        ));

        // ==========================================
        // 列 2
        // ==========================================

        // 5. Magnet (トグル + 詳細)
        this.addRenderableWidget(new NatureButton(
                col2X, y0, colW - 30, rowH,
                getToggleText("Item Magnet", config.magnetEnabled()),
                btn -> {
                    config = new InventoryUtil.FlowerConfig(
                            config.flightEnabled(), config.flightSpeedH(), config.flightSpeedV(),
                            config.effectsEnabled(), config.invulEnabled(), config.saturationEnabled(),
                            !config.magnetEnabled(), config.magnetRange(), config.speedEnabled(),
                            config.speedMult(), config.auraEnabled(), config.auraRange(), config.visionEnabled()
                    );
                    saveConfig();
                    btn.setMessage(getToggleText("Item Magnet", config.magnetEnabled()));
                },
                config.magnetEnabled()
        ));
        this.addRenderableWidget(new NatureButton(
                col2X + colW - 26, y0, 26, rowH,
                Component.literal(">"),
                btn -> openDetail(FlowerDetailScreen.FeatureType.MAGNET)
        ));

        // 6. Speed (トグル + 詳細)
        this.addRenderableWidget(new NatureButton(
                col2X, y1, colW - 30, rowH,
                getToggleText("Speed Boost", config.speedEnabled()),
                btn -> {
                    config = new InventoryUtil.FlowerConfig(
                            config.flightEnabled(), config.flightSpeedH(), config.flightSpeedV(),
                            config.effectsEnabled(), config.invulEnabled(), config.saturationEnabled(),
                            config.magnetEnabled(), config.magnetRange(), !config.speedEnabled(),
                            config.speedMult(), config.auraEnabled(), config.auraRange(), config.visionEnabled()
                    );
                    saveConfig();
                    btn.setMessage(getToggleText("Speed Boost", config.speedEnabled()));
                },
                config.speedEnabled()
        ));
        this.addRenderableWidget(new NatureButton(
                col2X + colW - 26, y1, 26, rowH,
                Component.literal(">"),
                btn -> openDetail(FlowerDetailScreen.FeatureType.SPEED)
        ));

        // 7. Aura (トグル + 詳細)
        this.addRenderableWidget(new NatureButton(
                col2X, y2, colW - 30, rowH,
                getToggleText("Field Aura", config.auraEnabled()),
                btn -> {
                    config = new InventoryUtil.FlowerConfig(
                            config.flightEnabled(), config.flightSpeedH(), config.flightSpeedV(),
                            config.effectsEnabled(), config.invulEnabled(), config.saturationEnabled(),
                            config.magnetEnabled(), config.magnetRange(), config.speedEnabled(),
                            config.speedMult(), !config.auraEnabled(), config.auraRange(), config.visionEnabled()
                    );
                    saveConfig();
                    btn.setMessage(getToggleText("Field Aura", config.auraEnabled()));
                },
                config.auraEnabled()
        ));
        this.addRenderableWidget(new NatureButton(
                col2X + colW - 26, y2, 26, rowH,
                Component.literal(">"),
                btn -> openDetail(FlowerDetailScreen.FeatureType.AURA)
        ));

        // 8. Potion Effects (設定画面へ)
        this.addRenderableWidget(new NatureButton(
                col2X, y3, colW, rowH,
                Component.literal("Effects  [Configure >]"),
                btn -> openEffectsScreen(),
                config.effectsEnabled()
        ));

        // ==========================================
        // フッター
        // ==========================================
        this.addRenderableWidget(new NatureButton(
                centerX - 60,
                panelTop + PANEL_H - 30,
                120,
                20,
                Component.literal("Close"),
                btn -> this.onClose(),
                true
        ));
    }

    private Component getToggleText(String name, boolean enabled) {
        if (enabled) {
            return Component.literal(name + "  ").append(Component.literal("[ON]").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
        } else {
            return Component.literal(name + "  ").append(Component.literal("[OFF]").withStyle(ChatFormatting.GRAY));
        }
    }

    private void openDetail(FlowerDetailScreen.FeatureType type) {
        saveConfig();
        Minecraft.getInstance().setScreen(new FlowerDetailScreen(flowerStack, slotIndex, isCurio, curioIdentifier, type));
    }

    private void openEffectsScreen() {
        saveConfig();
        Minecraft.getInstance().setScreen(new FlowerEffectSettingsScreen(flowerStack, slotIndex, isCurio, curioIdentifier));
    }

    private void saveConfig() {
        InventoryUtil.setFlowerConfig(flowerStack, config);
        NatureCollapse.CHANNEL.sendToServer(new UpdateFlowerConfigPacket(slotIndex, isCurio, curioIdentifier, config));
    }

    @Override
    public void onClose() {
        saveConfig();
        super.onClose();
    }

    @Override
    protected void renderContents(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        int centerX = this.width / 2;
        int panelLeft = centerX - PANEL_W / 2;
        int panelTop = this.height / 2 - PANEL_H / 2;

        // クリーンなフロストガラスパネル
        this.renderGlassPanel(gui, panelLeft, panelTop, PANEL_W, PANEL_H);

        // ヘッダー: アイテムアイコン ＋ タイトル
        int headerY = panelTop + 14;
        gui.renderItem(this.flowerStack, centerX - 80, headerY - 4);
        gui.drawString(this.font, Component.literal("Nature Flower - Control Hub").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD),
                centerX - 58, headerY, 0x55FFAA, true);

        // サブガイドテキスト
        gui.drawCenteredString(this.font,
                Component.literal("Click to toggle | [>] to configure parameters").withStyle(ChatFormatting.DARK_GRAY),
                centerX, panelTop + 36, 0x88AAAA);
    }
}
