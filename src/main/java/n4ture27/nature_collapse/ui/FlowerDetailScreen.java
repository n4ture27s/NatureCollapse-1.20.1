package n4ture27.nature_collapse.ui;

import n4ture27.nature_collapse.NatureCollapse;
import n4ture27.nature_collapse.network.UpdateFlowerConfigPacket;
import n4ture27.nature_collapse.ui.components.NatureButton;
import n4ture27.nature_collapse.ui.components.NatureSlider;
import n4ture27.nature_collapse.utils.InventoryUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class FlowerDetailScreen extends NatureBaseScreen {
    public enum FeatureType {
        FLIGHT("gui.nature_collapse.feature.flight"),
        INVULNERABILITY("gui.nature_collapse.feature.invul"),
        SATURATION("gui.nature_collapse.feature.saturation"),
        MAGNET("gui.nature_collapse.feature.magnet"),
        SPEED("gui.nature_collapse.feature.speed"),
        AURA("gui.nature_collapse.feature.aura"),
        VISION("gui.nature_collapse.feature.vision");

        public final String titleKey;

        FeatureType(String titleKey) {
            this.titleKey = titleKey;
        }
    }

    private static final int PANEL_W = 320;
    private static final int PANEL_H = 190;

    private final ItemStack flowerStack;
    private final int slotIndex;
    private final boolean isCurio;
    private final String curioIdentifier;
    private final FeatureType featureType;
    private InventoryUtil.FlowerConfig config;

    public FlowerDetailScreen(ItemStack flowerStack, int slotIndex, boolean isCurio, String curioIdentifier, FeatureType featureType) {
        super(Component.translatable(featureType.titleKey).withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
        this.flowerStack = flowerStack;
        this.slotIndex = slotIndex;
        this.isCurio = isCurio;
        this.curioIdentifier = curioIdentifier;
        this.featureType = featureType;
        this.config = InventoryUtil.getFlowerConfig(flowerStack);
    }

    @Override
    protected void init() {
        super.init();
        int centerX = this.width / 2;
        int panelTop = this.height / 2 - PANEL_H / 2;

        switch (featureType) {
            case FLIGHT -> {
                // 水平飛行速度スライダー (0.02 〜 0.20)
                this.addRenderableWidget(new NatureSlider(
                        centerX - 130, panelTop + 60, 260, 20,
                        "Horizontal Speed: ", "",
                        0.02, 0.20, config.flightSpeedH(), 0.01,
                        val -> config = new InventoryUtil.FlowerConfig(
                                config.flightEnabled(), val.floatValue(), config.flightSpeedV(),
                                config.effectsEnabled(), config.invulEnabled(), config.saturationEnabled(),
                                config.magnetEnabled(), config.magnetRange(), config.speedEnabled(),
                                config.speedMult(), config.auraEnabled(), config.auraRange(), config.visionEnabled()
                        )
                ));

                // 垂直飛行速度スライダー (0.02 〜 0.20)
                this.addRenderableWidget(new NatureSlider(
                        centerX - 130, panelTop + 88, 260, 20,
                        "Vertical Speed: ", "",
                        0.02, 0.20, config.flightSpeedV(), 0.01,
                        val -> config = new InventoryUtil.FlowerConfig(
                                config.flightEnabled(), config.flightSpeedH(), val.floatValue(),
                                config.effectsEnabled(), config.invulEnabled(), config.saturationEnabled(),
                                config.magnetEnabled(), config.magnetRange(), config.speedEnabled(),
                                config.speedMult(), config.auraEnabled(), config.auraRange(), config.visionEnabled()
                        )
                ));
            }
            case MAGNET -> {
                // 吸引範囲スライダー (2m 〜 32m)
                this.addRenderableWidget(new NatureSlider(
                        centerX - 130, panelTop + 72, 260, 20,
                        "Attraction Radius: ", " blocks",
                        2.0, 32.0, config.magnetRange(), 1.0,
                        val -> config = new InventoryUtil.FlowerConfig(
                                config.flightEnabled(), config.flightSpeedH(), config.flightSpeedV(),
                                config.effectsEnabled(), config.invulEnabled(), config.saturationEnabled(),
                                config.magnetEnabled(), val, config.speedEnabled(),
                                config.speedMult(), config.auraEnabled(), config.auraRange(), config.visionEnabled()
                        )
                ));
            }
            case SPEED -> {
                // 移動速度倍率スライダー (1.0x 〜 4.0x)
                this.addRenderableWidget(new NatureSlider(
                        centerX - 130, panelTop + 72, 260, 20,
                        "Ground Speed Boost: ", "x",
                        1.0, 4.0, config.speedMult(), 0.1,
                        val -> config = new InventoryUtil.FlowerConfig(
                                config.flightEnabled(), config.flightSpeedH(), config.flightSpeedV(),
                                config.effectsEnabled(), config.invulEnabled(), config.saturationEnabled(),
                                config.magnetEnabled(), config.magnetRange(), config.speedEnabled(),
                                val.floatValue(), config.auraEnabled(), config.auraRange(), config.visionEnabled()
                        )
                ));
            }
            case AURA -> {
                // オーラ範囲スライダー (2m 〜 16m)
                this.addRenderableWidget(new NatureSlider(
                        centerX - 130, panelTop + 72, 260, 20,
                        "Naturalize Field Radius: ", " blocks",
                        2.0, 16.0, config.auraRange(), 1.0,
                        val -> config = new InventoryUtil.FlowerConfig(
                                config.flightEnabled(), config.flightSpeedH(), config.flightSpeedV(),
                                config.effectsEnabled(), config.invulEnabled(), config.saturationEnabled(),
                                config.magnetEnabled(), config.magnetRange(), config.speedEnabled(),
                                config.speedMult(), config.auraEnabled(), val, config.visionEnabled()
                        )
                ));
            }
            case INVULNERABILITY -> {
                this.addRenderableWidget(new NatureButton(
                        centerX - 100, panelTop + 72, 200, 22,
                        Component.literal("Invulnerability: " + (config.invulEnabled() ? "ENABLED" : "DISABLED")),
                        btn -> {
                            boolean next = !config.invulEnabled();
                            config = new InventoryUtil.FlowerConfig(
                                     config.flightEnabled(), config.flightSpeedH(), config.flightSpeedV(),
                                     config.effectsEnabled(), next, config.saturationEnabled(),
                                     config.magnetEnabled(), config.magnetRange(), config.speedEnabled(),
                                     config.speedMult(), config.auraEnabled(), config.auraRange(), config.visionEnabled()
                            );
                            btn.setMessage(Component.literal("Invulnerability: " + (next ? "ENABLED" : "DISABLED")));
                        },
                        config.invulEnabled()
                ));
            }
            case SATURATION -> {
                this.addRenderableWidget(new NatureButton(
                        centerX - 100, panelTop + 72, 200, 22,
                        Component.literal("Saturation Keep: " + (config.saturationEnabled() ? "ENABLED" : "DISABLED")),
                        btn -> {
                            boolean next = !config.saturationEnabled();
                            config = new InventoryUtil.FlowerConfig(
                                     config.flightEnabled(), config.flightSpeedH(), config.flightSpeedV(),
                                     config.effectsEnabled(), config.invulEnabled(), next,
                                     config.magnetEnabled(), config.magnetRange(), config.speedEnabled(),
                                     config.speedMult(), config.auraEnabled(), config.auraRange(), config.visionEnabled()
                            );
                            btn.setMessage(Component.literal("Saturation Keep: " + (next ? "ENABLED" : "DISABLED")));
                        },
                        config.saturationEnabled()
                ));
            }
            case VISION -> {
                this.addRenderableWidget(new NatureButton(
                        centerX - 100, panelTop + 72, 200, 22,
                        Component.literal("Clear Vision: " + (config.visionEnabled() ? "ENABLED" : "DISABLED")),
                        btn -> {
                            boolean next = !config.visionEnabled();
                            config = new InventoryUtil.FlowerConfig(
                                     config.flightEnabled(), config.flightSpeedH(), config.flightSpeedV(),
                                     config.effectsEnabled(), config.invulEnabled(), config.saturationEnabled(),
                                     config.magnetEnabled(), config.magnetRange(), config.speedEnabled(),
                                     config.speedMult(), config.auraEnabled(), config.auraRange(), next
                            );
                            btn.setMessage(Component.literal("Clear Vision: " + (next ? "ENABLED" : "DISABLED")));
                        },
                        config.visionEnabled()
                ));
            }
        }

        // 下部アクションボタン: "< Back to Hub"
        int actionY = panelTop + PANEL_H - 32;
        this.addRenderableWidget(new NatureButton(
                centerX - 70, actionY, 140, 22,
                Component.literal("< Back to Hub"),
                btn -> this.returnToHub(),
                true
        ));
    }

    private void save() {
        InventoryUtil.setFlowerConfig(flowerStack, config);
        NatureCollapse.CHANNEL.sendToServer(new UpdateFlowerConfigPacket(slotIndex, isCurio, curioIdentifier, config));
    }

    private void returnToHub() {
        save();
        Minecraft.getInstance().setScreen(new FlowerHubScreen(flowerStack, slotIndex, isCurio, curioIdentifier));
    }

    @Override
    public void onClose() {
        save();
        super.onClose();
    }

    @Override
    protected void renderContents(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        int centerX = this.width / 2;
        int panelLeft = centerX - PANEL_W / 2;
        int panelTop = this.height / 2 - PANEL_H / 2;

        this.renderGlassPanel(gui, panelLeft, panelTop, PANEL_W, PANEL_H);

        // タイトル
        String headerTitle = this.title.getString();
        gui.drawCenteredString(this.font, headerTitle, centerX, panelTop + 18, 0x55FFAA);

        // 説明文
        String descText = switch (featureType) {
            case FLIGHT -> "Adjust horizontal and vertical flight mobility speeds.";
            case MAGNET -> "Automatically attracts items and exp orbs into your inventory.";
            case SPEED -> "Boosts ground walking and sprinting velocity.";
            case AURA -> "Instantly neutralizes approaching hostile mobs and projectiles.";
            case INVULNERABILITY -> "Protects you from all damage, death, and harmful status effects.";
            case SATURATION -> "Keeps your hunger and saturation at maximum levels.";
            case VISION -> "Grants permanent clear vision underwater and in absolute darkness.";
        };
        gui.drawCenteredString(this.font, descText, centerX, panelTop + 36, 0x88CCAA);
    }
}
