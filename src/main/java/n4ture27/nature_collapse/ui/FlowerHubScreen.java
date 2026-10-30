package n4ture27.nature_collapse.ui;

import n4ture27.nature_collapse.NatureCollapse;
import n4ture27.nature_collapse.network.UpdateFlowerConfigPacket;
import n4ture27.nature_collapse.ui.components.NatureButton;
import n4ture27.nature_collapse.utils.InventoryUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

public class FlowerHubScreen extends NatureBaseScreen {

    public record PetalInfo(
            String id,
            String name,
            String icon,
            double angleDeg,
            FlowerDetailScreen.FeatureType detailType
    ) {
    }

    private static final PetalInfo[] PETALS = new PetalInfo[]{
            new PetalInfo("flight", "Flight", "🪶", 270, FlowerDetailScreen.FeatureType.FLIGHT),        // 上 (12時)
            new PetalInfo("effects", "Effects", "🧪", 315, null),                                         // 右上 (1時半) -> FlowerEffectSettingsScreen
            new PetalInfo("invul", "Invulnerable", "🛡️", 0, FlowerDetailScreen.FeatureType.INVULNERABILITY), // 右 (3時)
            new PetalInfo("saturation", "Saturation", "🍖", 45, FlowerDetailScreen.FeatureType.SATURATION), // 右下 (4時半)
            new PetalInfo("magnet", "Magnet", "🧲", 90, FlowerDetailScreen.FeatureType.MAGNET),          // 下 (6時)
            new PetalInfo("speed", "Speed", "⚡", 135, FlowerDetailScreen.FeatureType.SPEED),             // 左下 (7時半)
            new PetalInfo("aura", "Aura", "🌀", 180, FlowerDetailScreen.FeatureType.AURA),               // 左 (9時)
            new PetalInfo("vision", "Vision", "👁️", 225, FlowerDetailScreen.FeatureType.VISION)          // 左上 (10時半)
    };

    private static final int ORBIT_RADIUS = 100;
    private static final int PETAL_WIDTH = 76;
    private static final int PETAL_HEIGHT = 40;
    private static final int CORE_RADIUS = 36;

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

        // 閉じるボタン
        this.addRenderableWidget(new NatureButton(
                this.width / 2 - 50,
                this.height - 28,
                100,
                20,
                Component.literal("Close"),
                btn -> this.onClose(),
                true
        ));
    }

    private boolean isFeatureEnabled(String id) {
        return switch (id) {
            case "flight" -> config.flightEnabled();
            case "effects" -> config.effectsEnabled();
            case "invul" -> config.invulEnabled();
            case "saturation" -> config.saturationEnabled();
            case "magnet" -> config.magnetEnabled();
            case "speed" -> config.speedEnabled();
            case "aura" -> config.auraEnabled();
            case "vision" -> config.visionEnabled();
            default -> false;
        };
    }

    private void toggleFeature(String id) {
        config = switch (id) {
            case "flight" -> new InventoryUtil.FlowerConfig(
                    !config.flightEnabled(), config.flightSpeedH(), config.flightSpeedV(),
                    config.effectsEnabled(), config.invulEnabled(), config.saturationEnabled(),
                    config.magnetEnabled(), config.magnetRange(), config.speedEnabled(),
                    config.speedMult(), config.auraEnabled(), config.auraRange(), config.visionEnabled()
            );
            case "effects" -> new InventoryUtil.FlowerConfig(
                    config.flightEnabled(), config.flightSpeedH(), config.flightSpeedV(),
                    !config.effectsEnabled(), config.invulEnabled(), config.saturationEnabled(),
                    config.magnetEnabled(), config.magnetRange(), config.speedEnabled(),
                    config.speedMult(), config.auraEnabled(), config.auraRange(), config.visionEnabled()
            );
            case "invul" -> new InventoryUtil.FlowerConfig(
                    config.flightEnabled(), config.flightSpeedH(), config.flightSpeedV(),
                    config.effectsEnabled(), !config.invulEnabled(), config.saturationEnabled(),
                    config.magnetEnabled(), config.magnetRange(), config.speedEnabled(),
                    config.speedMult(), config.auraEnabled(), config.auraRange(), config.visionEnabled()
            );
            case "saturation" -> new InventoryUtil.FlowerConfig(
                    config.flightEnabled(), config.flightSpeedH(), config.flightSpeedV(),
                    config.effectsEnabled(), config.invulEnabled(), !config.saturationEnabled(),
                    config.magnetEnabled(), config.magnetRange(), config.speedEnabled(),
                    config.speedMult(), config.auraEnabled(), config.auraRange(), config.visionEnabled()
            );
            case "magnet" -> new InventoryUtil.FlowerConfig(
                    config.flightEnabled(), config.flightSpeedH(), config.flightSpeedV(),
                    config.effectsEnabled(), config.invulEnabled(), config.saturationEnabled(),
                    !config.magnetEnabled(), config.magnetRange(), config.speedEnabled(),
                    config.speedMult(), config.auraEnabled(), config.auraRange(), config.visionEnabled()
            );
            case "speed" -> new InventoryUtil.FlowerConfig(
                    config.flightEnabled(), config.flightSpeedH(), config.flightSpeedV(),
                    config.effectsEnabled(), config.invulEnabled(), config.saturationEnabled(),
                    config.magnetEnabled(), config.magnetRange(), !config.speedEnabled(),
                    config.speedMult(), config.auraEnabled(), config.auraRange(), config.visionEnabled()
            );
            case "aura" -> new InventoryUtil.FlowerConfig(
                    config.flightEnabled(), config.flightSpeedH(), config.flightSpeedV(),
                    config.effectsEnabled(), config.invulEnabled(), config.saturationEnabled(),
                    config.magnetEnabled(), config.magnetRange(), config.speedEnabled(),
                    config.speedMult(), !config.auraEnabled(), config.auraRange(), config.visionEnabled()
            );
            case "vision" -> new InventoryUtil.FlowerConfig(
                    config.flightEnabled(), config.flightSpeedH(), config.flightSpeedV(),
                    config.effectsEnabled(), config.invulEnabled(), config.saturationEnabled(),
                    config.magnetEnabled(), config.magnetRange(), config.speedEnabled(),
                    config.speedMult(), config.auraEnabled(), config.auraRange(), !config.visionEnabled()
            );
            default -> config;
        };

        saveConfig();

        Minecraft.getInstance().getSoundManager().play(
                net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.2f)
        );
    }

    private void openDetail(PetalInfo petal) {
        saveConfig();
        if (petal.id().equals("effects")) {
            Minecraft.getInstance().setScreen(new FlowerEffectSettingsScreen(flowerStack, slotIndex, isCurio, curioIdentifier));
        } else if (petal.detailType() != null) {
            Minecraft.getInstance().setScreen(new FlowerDetailScreen(flowerStack, slotIndex, isCurio, curioIdentifier, petal.detailType()));
        }
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
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int centerX = this.width / 2;
        int centerY = this.height / 2 - 10;

        for (PetalInfo petal : PETALS) {
            double rad = Math.toRadians(petal.angleDeg());
            int petalCenterX = centerX + (int) (Math.cos(rad) * ORBIT_RADIUS);
            int petalCenterY = centerY + (int) (Math.sin(rad) * ORBIT_RADIUS);

            int x1 = petalCenterX - PETAL_WIDTH / 2;
            int y1 = petalCenterY - PETAL_HEIGHT / 2;
            int x2 = petalCenterX + PETAL_WIDTH / 2;
            int y2 = petalCenterY + PETAL_HEIGHT / 2;

            if (mouseX >= x1 && mouseX <= x2 && mouseY >= y1 && mouseY <= y2) {
                if (button == 0) {
                    // 左クリック: トグル
                    toggleFeature(petal.id());
                    return true;
                } else if (button == 1) {
                    // 右クリック: 詳細設定へジャンプ
                    openDetail(petal);
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderContents(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        int centerX = this.width / 2;
        int centerY = this.height / 2 - 10;

        // 背景の装飾光彩円
        long time = System.currentTimeMillis();
        float pulse = (float) (0.6f + 0.4f * Math.sin(time / 500.0));
        int orbitGlow = Mth.hsvToRgb(0.38f, 0.8f, pulse);
        gui.fill(centerX - ORBIT_RADIUS - 44, centerY - ORBIT_RADIUS - 24,
                centerX + ORBIT_RADIUS + 44, centerY + ORBIT_RADIUS + 24, 0x1A003318);

        // 8枚の花びらノードを描画
        for (PetalInfo petal : PETALS) {
            boolean enabled = isFeatureEnabled(petal.id());
            double rad = Math.toRadians(petal.angleDeg());

            // ホバー判定
            int basePx = centerX + (int) (Math.cos(rad) * ORBIT_RADIUS);
            int basePy = centerY + (int) (Math.sin(rad) * ORBIT_RADIUS);
            boolean hovered = mouseX >= basePx - PETAL_WIDTH / 2 && mouseX <= basePx + PETAL_WIDTH / 2
                    && mouseY >= basePy - PETAL_HEIGHT / 2 && mouseY <= basePy + PETAL_HEIGHT / 2;

            // ホバー時に外側へ少し広がるアニメーション
            int currentRadius = hovered ? ORBIT_RADIUS + 6 : ORBIT_RADIUS;
            int px = centerX + (int) (Math.cos(rad) * currentRadius);
            int py = centerY + (int) (Math.sin(rad) * currentRadius);

            int x1 = px - PETAL_WIDTH / 2;
            int y1 = py - PETAL_HEIGHT / 2;
            int x2 = px + PETAL_WIDTH / 2;
            int y2 = py + PETAL_HEIGHT / 2;

            // コアから花びらへの接続ステム（茎・エネルギーライン）
            int stemColor = enabled ? 0x9900EE77 : 0x44005522;
            gui.fill(centerX, centerY, px, py, stemColor);

            // 花びら枠線と背景
            int borderColor;
            int bgColorTop;
            int bgColorBot;

            if (enabled) {
                borderColor = hovered ? 0xFFFFFFFF : 0xFF00FF88;
                bgColorTop = hovered ? 0xDD00552B : 0xCC00381C;
                bgColorBot = hovered ? 0xEE00381C : 0xDD002211;
            } else {
                borderColor = hovered ? 0xFF558866 : 0xFF22442E;
                bgColorTop = hovered ? 0xCC112217 : 0xBB0B1710;
                bgColorBot = hovered ? 0xDD0C1A11 : 0xCC07100B;
            }

            // 外周グロー
            gui.fill(x1 - 1, y1 - 1, x2 + 1, y2 + 1, borderColor);
            gui.fillGradient(x1, y1, x2, y2, bgColorTop, bgColorBot);

            // アイコン + 名前
            String label = petal.icon() + " " + petal.name();
            int nameColor = enabled ? (hovered ? 0xFFFFFF : 0x88FFBB) : 0x779988;
            gui.drawCenteredString(this.font, label, px, py - 12, nameColor);

            // ステータスバッジ
            String badge = enabled ? "[ ON ]" : "[ OFF ]";
            int badgeColor = enabled ? 0x55FFAA : 0xAA5555;
            gui.drawCenteredString(this.font, badge, px, py + 2, badgeColor);
        }

        // 中央コア（Receptacle Core）描画
        gui.fill(centerX - CORE_RADIUS - 2, centerY - CORE_RADIUS - 2,
                centerX + CORE_RADIUS + 2, centerY + CORE_RADIUS + 2, (0xFF << 24) | orbitGlow);
        gui.fillGradient(centerX - CORE_RADIUS, centerY - CORE_RADIUS,
                centerX + CORE_RADIUS, centerY + CORE_RADIUS, 0xEE041A0D, 0xF8010A05);

        // 3D花アイコン描画
        gui.renderItem(this.flowerStack, centerX - 8, centerY - 18);

        // コアタイトルとステータス
        gui.drawCenteredString(this.font, "NATURE", centerX, centerY + 4, 0x55FFAA);

        int activeCount = 0;
        for (PetalInfo p : PETALS) {
            if (isFeatureEnabled(p.id())) activeCount++;
        }
        gui.drawCenteredString(this.font, activeCount + "/8 ON", centerX, centerY + 16, 0x88DD99);

        // 画面上部バナー
        gui.drawCenteredString(this.font, Component.literal("NATURE FLOWER HUB").withStyle(ChatFormatting.BOLD, ChatFormatting.GREEN),
                centerX, 16, 0x00FF88);

        // 画面下部操作ガイド
        gui.drawCenteredString(this.font,
                Component.literal("[Left Click] Toggle  •  [Right Click] Settings").withStyle(ChatFormatting.GRAY),
                centerX, this.height - 46, 0x88CCAA);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
