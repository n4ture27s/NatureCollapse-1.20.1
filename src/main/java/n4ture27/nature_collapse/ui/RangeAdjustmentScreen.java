package n4ture27.nature_collapse.ui;

import n4ture27.nature_collapse.NatureCollapse;
import n4ture27.nature_collapse.network.UpdateRangePacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.ChatFormatting;

public class RangeAdjustmentScreen extends NatureBaseScreen {
    private final ItemStack stack;
    private final int slotId;
    private final boolean isCurio;
    private final String curiosIdentifier;
    private double range;

    public RangeAdjustmentScreen(ItemStack stack, int slotId, boolean isCurio, String curiosIdentifier) {
        super(Component.translatable("gui.nature_collapse.range_settings").withStyle(ChatFormatting.GREEN,
                ChatFormatting.BOLD));
        this.stack = stack;
        this.slotId = slotId;
        this.isCurio = isCurio;
        this.curiosIdentifier = curiosIdentifier;
        this.loadSettings();
    }

    private void loadSettings() {
        double saved = stack.getOrCreateTag().getDouble("CustomRange");
        this.range = saved > 0 ? saved : 10.0;
    }

    @Override
    protected void init() {
        super.init();
        // slider
        this.addRenderableWidget(new AbstractSliderButton(this.width / 2 - 100, this.height / 2 - 30, 200, 20,
                Component.literal("Range: " + (int) range), range / 100.0) {
            @Override
            protected void updateMessage() {
                this.setMessage(Component.literal("Range: ").append(
                        Component.literal(String.valueOf((int) (this.value * 100.0))).withStyle(ChatFormatting.AQUA)));
            }

            @Override
            protected void applyValue() {
                range = Math.floor(this.value * 100.0);
            }
        });

        // reset
        this.addRenderableWidget(Button.builder(Component.literal("Reset to Default"), (btn) -> {
            this.range = 10.0;
            this.clearWidgets();
            this.init();
        }).bounds(this.width / 2 - 100, this.height / 2 + 0, 95, 20).build());

        // complate
        this.addRenderableWidget(Button.builder(Component.literal("Done"), (btn) -> {
            this.onClose();
        }).bounds(this.width / 2 + 5, this.height / 2 + 0, 95, 20).build());
    }

    @Override
    public void onClose() {
        this.saveSettings();
        super.onClose();
    }

    private void saveSettings() {
        NatureCollapse.CHANNEL.sendToServer(new UpdateRangePacket(range, slotId, isCurio, curiosIdentifier));
    }

    @Override
    protected void renderContents(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        this.drawNaturePanel(gui, this.width / 2, this.height / 2 - 10, 220, 80);
        this.drawTitle(gui, this.height / 2 - 45);
    }
}