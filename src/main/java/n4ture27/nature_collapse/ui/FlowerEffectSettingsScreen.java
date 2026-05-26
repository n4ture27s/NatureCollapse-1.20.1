package n4ture27.nature_collapse.ui;

import n4ture27.nature_collapse.NatureCollapse;
import n4ture27.nature_collapse.network.UpdateFlowerEffectsPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.HashMap;
import java.util.Map;

public class FlowerEffectSettingsScreen extends NatureBaseScreen {
    private final ItemStack stack;
    private final int slotId;
    private final Map<String, Integer> selectedEffects = new HashMap<>();
    private EffectList list;

    public FlowerEffectSettingsScreen(ItemStack stack, int slotId) {
        super(Component.literal("Flower Effect Settings").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
        this.stack = stack;
        this.slotId = slotId;
        this.loadSettings();
    }

    private void loadSettings() {
        if (stack.hasTag() && stack.getTag().contains("SelectedEffects")) {
            ListTag nbtList = stack.getTag().getList("SelectedEffects", Tag.TAG_COMPOUND);
            for (int i = 0; i < nbtList.size(); i++) {
                CompoundTag tag = nbtList.getCompound(i);
                selectedEffects.put(tag.getString("Id"), tag.getInt("Level"));
            }
        }
    }

    @Override
    protected void init() {
        super.init();
        this.list = new EffectList();
        this.addWidget(this.list);

        this.addRenderableWidget(Button.builder(Component.literal("Done"), (btn) -> this.onClose())
                .bounds(this.width / 2 - 100, this.height - 30, 200, 20).build());
    }

    @Override
    protected void renderContents(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        //head&foot
        this.drawNaturePanel(gui, this.width / 2, 16, 280, 25);
        this.drawNaturePanel(gui, this.width / 2, this.height - 20, 210, 25);

        this.list.render(gui, mouseX, mouseY, partialTick);
        this.drawTitle(gui, 12);
    }

    @Override
    public void onClose() {
        this.saveSettings();
        super.onClose();
    }

    private void saveSettings() {
        NatureCollapse.CHANNEL.sendToServer(new UpdateFlowerEffectsPacket(selectedEffects, slotId));
    }

    class EffectList extends ObjectSelectionList<EffectList.Entry> {
        public EffectList() {
            super(FlowerEffectSettingsScreen.this.minecraft, FlowerEffectSettingsScreen.this.width,
                    FlowerEffectSettingsScreen.this.height, 32, FlowerEffectSettingsScreen.this.height - 40, 20);
            ForgeRegistries.MOB_EFFECTS.getEntries().forEach(entry -> {
                this.addEntry(new Entry(entry.getKey().location(), entry.getValue()));
            });
        }

        @Override
        public int getRowWidth() {
            return 260;
        }

        class Entry extends ObjectSelectionList.Entry<Entry> {
            private final ResourceLocation id;
            private final MobEffect effect;

            public Entry(ResourceLocation id, MobEffect effect) {
                this.id = id;
                this.effect = effect;
            }

            @Override
            public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX,
                    int mouseY, boolean isMouseOver, float partialTick) {
                String name = effect.getDisplayName().getString();
                String key = id.toString();
                boolean selected = selectedEffects.containsKey(key);
                int color = selected ? 0x00FF00 : 0xFFFFFF;
                String text = name + " (" + id + ")";
                if (selected) {
                    text += " [Lv." + selectedEffects.get(key) + "]";
                }
                guiGraphics.drawString(FlowerEffectSettingsScreen.this.font, text, left + 5, top + 5, color);
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                String key = id.toString();
                if (button == 0) {
                    if (Screen.hasShiftDown()) {
                        selectedEffects.put(key, 255);
                    } else {
                        int current = selectedEffects.getOrDefault(key, 0);
                        if (current >= 255)
                            selectedEffects.remove(key);
                        else
                            selectedEffects.put(key, current + 1);
                    }
                } else if (button == 1) {
                    if (Screen.hasShiftDown()) {
                        selectedEffects.remove(key);
                    } else {
                        int current = selectedEffects.getOrDefault(key, 0);
                        if (current <= 1)
                            selectedEffects.remove(key);
                        else
                            selectedEffects.put(key, current - 1);
                    }
                }
                return true;
            }

            @Override
            public Component getNarration() {
                return Component.literal(effect.getDisplayName().getString());
            }
        }
    }
}