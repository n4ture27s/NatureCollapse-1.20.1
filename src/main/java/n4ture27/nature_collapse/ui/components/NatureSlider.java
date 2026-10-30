package n4ture27.nature_collapse.ui.components;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;

public class NatureSlider extends AbstractSliderButton {
    private final String prefix;
    private final String suffix;
    private final double minValue;
    private final double maxValue;
    private final double step;
    private final Function<Double, String> valueFormatter;
    private final Consumer<Double> onValueChanged;

    public NatureSlider(int x, int y, int width, int height, String prefix, String suffix,
                        double minValue, double maxValue, double currentValue, double step,
                        Consumer<Double> onValueChanged) {
        this(x, y, width, height, prefix, suffix, minValue, maxValue, currentValue, step, null, onValueChanged);
    }

    public NatureSlider(int x, int y, int width, int height, String prefix, String suffix,
                        double minValue, double maxValue, double currentValue, double step,
                        Function<Double, String> valueFormatter, Consumer<Double> onValueChanged) {
        super(x, y, width, height, Component.empty(),
                Mth.clamp((currentValue - minValue) / Math.max(0.0001, maxValue - minValue), 0.0, 1.0));
        this.prefix = prefix;
        this.suffix = suffix;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.step = step;
        this.valueFormatter = valueFormatter;
        this.onValueChanged = onValueChanged;
        this.updateMessage();
    }

    public double getActualValue() {
        double raw = minValue + this.value * (maxValue - minValue);
        if (step > 0.0) {
            raw = Math.round(raw / step) * step;
        }
        return Mth.clamp(raw, minValue, maxValue);
    }

    public void setActualValue(double newValue) {
        double clamped = Mth.clamp(newValue, minValue, maxValue);
        this.value = Mth.clamp((clamped - minValue) / Math.max(0.0001, maxValue - minValue), 0.0, 1.0);
        this.updateMessage();
    }

    @Override
    protected void updateMessage() {
        double val = getActualValue();
        String formatted;
        if (this.valueFormatter != null) {
            formatted = this.valueFormatter.apply(val);
        } else if (step >= 1.0 && Math.floor(val) == val) {
            formatted = String.valueOf((int) val);
        } else if (step >= 0.1) {
            formatted = String.format(Locale.ROOT, "%.1f", val);
        } else {
            formatted = String.format(Locale.ROOT, "%.2f", val);
        }
        this.setMessage(Component.literal(prefix + formatted + suffix));
    }

    @Override
    protected void applyValue() {
        if (this.onValueChanged != null) {
            this.onValueChanged.accept(getActualValue());
        }
    }

    @Override
    public void renderWidget(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        boolean hovered = this.isHoveredOrFocused();

        int x = this.getX();
        int y = this.getY();
        int w = this.width;
        int h = this.height;

        // トラック枠線と背景
        gui.fill(x - 1, y - 1, x + w + 1, y + h + 1, hovered ? 0xFF00FF88 : 0xFF009944);
        gui.fill(x, y, x + w, y + h, 0xEE06160D);

        // プログレスフィル（ゲージ）
        int progressW = (int) (this.value * (w - 4));
        if (progressW > 0) {
            gui.fillGradient(x + 2, y + 2, x + 2 + progressW, y + h - 2, 0xCC00AA55, 0xEE00DD77);
        }

        // スライダーつまみ（ノブ）
        int knobWidth = 8;
        int knobX = x + (int) (this.value * (w - knobWidth));
        int knobColor = hovered ? 0xFFFFFFFF : 0xFF55FFAA;
        int knobBody = hovered ? 0xFF00FF88 : 0xFF00AA55;

        gui.fill(knobX - 1, y, knobX + knobWidth + 1, y + h, 0x88000000);
        gui.fill(knobX, y + 1, knobX + knobWidth, y + h - 1, knobColor);
        gui.fill(knobX + 1, y + 2, knobX + knobWidth - 1, y + h - 2, knobBody);

        // ラベルテキスト描画
        int textColor = hovered ? 0xFFFFFF : 0xEEFFEE;
        gui.drawCenteredString(minecraft.font, this.getMessage(), x + w / 2, y + (h - 8) / 2, textColor);
    }
}
