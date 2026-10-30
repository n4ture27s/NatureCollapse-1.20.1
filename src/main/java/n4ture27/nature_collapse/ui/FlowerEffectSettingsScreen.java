package n4ture27.nature_collapse.ui;

import n4ture27.nature_collapse.NatureCollapse;
import n4ture27.nature_collapse.network.UpdateFlowerEffectsPacket;
import n4ture27.nature_collapse.ui.components.EffectScrollList;
import n4ture27.nature_collapse.ui.components.NatureButton;
import n4ture27.nature_collapse.ui.components.NatureSlider;
import n4ture27.nature_collapse.utils.InventoryUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class FlowerEffectSettingsScreen extends NatureBaseScreen {

    public static class EffectData {
        public MobEffect effect;
        public int level;
        public int duration;

        public EffectData(MobEffect effect, int level, int duration) {
            this.effect = effect;
            this.level = level;
            this.duration = duration;
        }
    }

    private final ItemStack flowerStack;
    private final int slotIndex;
    private final boolean isCurio;
    private final String curioIdentifier;

    private EditBox searchBox;
    private EffectScrollList effectScrollList;
    private List<MobEffect> allEffects = new ArrayList<>();
    private final List<EffectData> activeEffects = new ArrayList<>();

    private EffectData editingEffect;
    private NatureSlider levelSlider;
    private NatureSlider durationSlider;

    private double activeListScroll = 0;

    public FlowerEffectSettingsScreen(ItemStack flowerStack, int slotIndex, boolean isCurio, String curioIdentifier) {
        super(Component.translatable("gui.nature_collapse.flower_settings")
                .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
        this.flowerStack = flowerStack;
        this.slotIndex = slotIndex;
        this.isCurio = isCurio;
        this.curioIdentifier = curioIdentifier;

        loadSelectedEffects();
    }

    private EffectData getEffectData(MobEffect effect) {
        for (EffectData data : activeEffects) {
            if (data.effect == effect) {
                return data;
            }
        }
        return null;
    }

    private void addOrSelectEffect(MobEffect effect) {
        EffectData existing = getEffectData(effect);
        if (existing != null) {
            editingEffect = existing;
            updateInspectorWidgets();
            return;
        }

        EffectData data = new EffectData(effect, 1, InventoryUtil.DEFAULT_EFFECT_DURATION);
        activeEffects.add(data);
        editingEffect = data;
        rebuildWidgets();
    }

    private void removeEffect(EffectData data) {
        activeEffects.remove(data);
        if (editingEffect == data) {
            editingEffect = activeEffects.isEmpty() ? null : activeEffects.get(0);
        }
        rebuildWidgets();
    }

    @Override
    protected void init() {
        super.init();

        this.allEffects = ForgeRegistries.MOB_EFFECTS.getValues().stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(e -> e.getDisplayName().getString()))
                .collect(Collectors.toList());

        rebuildWidgets();
    }

    protected void rebuildWidgets() {
        String prevSearch = searchBox != null ? searchBox.getValue() : "";
        clearWidgets();

        int margin = 14;
        int topHeaderH = 40;
        int inspectorH = 88;
        int footerH = 32;

        int contentY = topHeaderH + 4;
        int contentH = this.height - contentY - inspectorH - footerH;
        int colW = (this.width - margin * 3) / 2;

        // --- 左カラム（検索＋候補一覧） ---
        int leftX = margin;
        this.searchBox = new EditBox(
                this.font,
                leftX,
                contentY,
                colW,
                18,
                Component.translatable("gui.nature_collapse.search_effect")
        );
        this.searchBox.setValue(prevSearch);
        this.searchBox.setResponder(text -> {
            if (this.effectScrollList != null) {
                this.effectScrollList.filter(text);
            }
        });
        this.addRenderableWidget(searchBox);

        this.effectScrollList = new EffectScrollList(
                leftX,
                contentY + 22,
                colW,
                contentH - 22,
                effect -> getEffectData(effect) != null,
                this::addOrSelectEffect
        );
        this.effectScrollList.setEffects(allEffects);
        this.effectScrollList.filter(prevSearch);
        this.addRenderableWidget(effectScrollList);

        // --- 右カラム（アクティブカード用ボタン群） ---
        int rightX = leftX + colW + margin;
        int cardH = 26;
        for (int i = 0; i < activeEffects.size(); i++) {
            EffectData data = activeEffects.get(i);
            int cardY = contentY + 22 + i * (cardH + 4) - (int) activeListScroll;
            if (cardY + cardH < contentY + 22 || cardY > contentY + contentH) {
                continue; // 画面外は省略
            }

            // カード選択ボタン
            final EffectData fData = data;
            boolean isSelected = editingEffect == data;
            Component nameComp = Component.literal(data.effect.getDisplayName().getString() + " Lv." + data.level);
            this.addRenderableWidget(new NatureButton(
                    rightX + 24, cardY, colW - 54, cardH,
                    nameComp,
                    btn -> {
                        editingEffect = fData;
                        updateInspectorWidgets();
                    },
                    isSelected
            ));

            // カード削除 [X] ボタン
            this.addRenderableWidget(new NatureButton(
                    rightX + colW - 26, cardY, 24, cardH,
                    Component.literal("X").withStyle(ChatFormatting.RED),
                    btn -> removeEffect(fData)
            ));
        }

        // --- 下部インスペクター（選択エフェクトの編集） ---
        int inspectorY = this.height - inspectorH - footerH + 2;
        if (editingEffect != null) {
            int sliderW = this.width / 2 - 80;

            // Level スライダー ＋ 微調整 [-] [+] ＋ プリセット
            this.levelSlider = new NatureSlider(
                    margin + 70, inspectorY + 18, sliderW, 18,
                    "Level: ", "",
                    1.0, 255.0, editingEffect.level, 1.0,
                    val -> editingEffect.level = (int) Math.round(val)
            );
            this.addRenderableWidget(levelSlider);

            this.addRenderableWidget(new NatureButton(
                    margin + 70 + sliderW + 4, inspectorY + 18, 20, 18,
                    Component.literal("-"),
                    btn -> {
                        editingEffect.level = Math.max(1, editingEffect.level - 1);
                        levelSlider.setActualValue(editingEffect.level);
                    }
            ));
            this.addRenderableWidget(new NatureButton(
                    margin + 70 + sliderW + 26, inspectorY + 18, 20, 18,
                    Component.literal("+"),
                    btn -> {
                        editingEffect.level = Math.min(255, editingEffect.level + 1);
                        levelSlider.setActualValue(editingEffect.level);
                    }
            ));

            // Level クイックプリセット
            int[] levelPresets = {1, 5, 10, 255};
            int lpStartX = margin + 70 + sliderW + 52;
            for (int i = 0; i < levelPresets.length; i++) {
                int lv = levelPresets[i];
                this.addRenderableWidget(new NatureButton(
                        lpStartX + i * 32, inspectorY + 18, 30, 18,
                        Component.literal(lv == 255 ? "MAX" : "Lv." + lv),
                        btn -> {
                            editingEffect.level = lv;
                            levelSlider.setActualValue(lv);
                        }
                ));
            }

            // Duration スライダー ＋ 微調整 [-] [+] ＋ プリセット
            this.durationSlider = new NatureSlider(
                    margin + 70, inspectorY + 44, sliderW, 18,
                    "Duration: ", " ticks",
                    20.0, 12000.0, editingEffect.duration, 20.0,
                    val -> editingEffect.duration = (int) Math.round(val)
            );
            this.addRenderableWidget(durationSlider);

            this.addRenderableWidget(new NatureButton(
                    margin + 70 + sliderW + 4, inspectorY + 44, 20, 18,
                    Component.literal("-"),
                    btn -> {
                        editingEffect.duration = Math.max(20, editingEffect.duration - 100);
                        durationSlider.setActualValue(editingEffect.duration);
                    }
            ));
            this.addRenderableWidget(new NatureButton(
                    margin + 70 + sliderW + 26, inspectorY + 44, 20, 18,
                    Component.literal("+"),
                    btn -> {
                        editingEffect.duration = Math.min(12000, editingEffect.duration + 100);
                        durationSlider.setActualValue(editingEffect.duration);
                    }
            ));

            // Duration クイックプリセット (300t=15s, 600t=30s, 1200t=1m, 6000t=5m)
            int[] durPresets = {300, 600, 1200, 6000};
            String[] durLabels = {"15s", "30s", "1m", "5m"};
            for (int i = 0; i < durPresets.length; i++) {
                int dur = durPresets[i];
                this.addRenderableWidget(new NatureButton(
                        lpStartX + i * 32, inspectorY + 44, 30, 18,
                        Component.literal(durLabels[i]),
                        btn -> {
                            editingEffect.duration = dur;
                            durationSlider.setActualValue(dur);
                        }
                ));
            }
        }

        // --- 最下部フッター（クリア ＆ HUBへ戻る ＆ 完了） ---
        int footerY = this.height - footerH + 4;
        this.addRenderableWidget(new NatureButton(
                margin, footerY, 95, 22,
                Component.literal("Clear All").withStyle(ChatFormatting.RED),
                btn -> {
                    activeEffects.clear();
                    editingEffect = null;
                    rebuildWidgets();
                }
        ));

        this.addRenderableWidget(new NatureButton(
                this.width / 2 - 65, footerY, 130, 22,
                Component.literal("< Back to Hub"),
                btn -> this.returnToHub()
        ));

        this.addRenderableWidget(new NatureButton(
                this.width - margin - 95, footerY, 95, 22,
                CommonComponents.GUI_DONE,
                btn -> this.onClose(),
                true
        ));
    }

    private void returnToHub() {
        saveSelectedEffects();
        Minecraft.getInstance().setScreen(new FlowerHubScreen(flowerStack, slotIndex, isCurio, curioIdentifier));
    }

    private void updateInspectorWidgets() {
        if (editingEffect != null) {
            if (levelSlider != null) {
                levelSlider.setActualValue(editingEffect.level);
            }
            if (durationSlider != null) {
                durationSlider.setActualValue(editingEffect.duration);
            }
        }
    }

    private void loadSelectedEffects() {
        CompoundTag tag = flowerStack.getTag();
        if (tag == null || !tag.contains(InventoryUtil.TAG_SELECTED_EFFECTS, Tag.TAG_LIST)) {
            return;
        }

        ListTag list = tag.getList(InventoryUtil.TAG_SELECTED_EFFECTS, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag effectTag = list.getCompound(i);
            ResourceLocation id = ResourceLocation.tryParse(effectTag.getString(InventoryUtil.TAG_EFFECT_ID));
            if (id == null) continue;

            MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(id);
            if (effect == null) continue;

            int level = Mth.clamp(effectTag.getInt(InventoryUtil.TAG_EFFECT_LEVEL),
                    InventoryUtil.MIN_EFFECT_LEVEL, InventoryUtil.MAX_EFFECT_LEVEL);
            int duration = effectTag.contains(InventoryUtil.TAG_EFFECT_DURATION, Tag.TAG_ANY_NUMERIC)
                    ? Mth.clamp(effectTag.getInt(InventoryUtil.TAG_EFFECT_DURATION),
                    InventoryUtil.MIN_EFFECT_DURATION, InventoryUtil.MAX_EFFECT_DURATION)
                    : InventoryUtil.DEFAULT_EFFECT_DURATION;

            activeEffects.add(new EffectData(effect, level, duration));
        }

        if (!activeEffects.isEmpty()) {
            editingEffect = activeEffects.get(0);
        }
    }

    private void saveSelectedEffects() {
        List<UpdateFlowerEffectsPacket.EffectEntry> packetEntries = new ArrayList<>();
        ListTag list = new ListTag();

        for (EffectData data : activeEffects) {
            ResourceLocation key = ForgeRegistries.MOB_EFFECTS.getKey(data.effect);
            if (key == null) continue;

            CompoundTag effectTag = new CompoundTag();
            effectTag.putString(InventoryUtil.TAG_EFFECT_ID, key.toString());
            effectTag.putInt(InventoryUtil.TAG_EFFECT_LEVEL, data.level);
            effectTag.putInt(InventoryUtil.TAG_EFFECT_DURATION, data.duration);
            list.add(effectTag);

            packetEntries.add(new UpdateFlowerEffectsPacket.EffectEntry(key.toString(), data.level, data.duration));
        }

        if (list.isEmpty()) {
            CompoundTag tag = flowerStack.getTag();
            if (tag != null) {
                tag.remove(InventoryUtil.TAG_SELECTED_EFFECTS);
                if (tag.isEmpty()) {
                    flowerStack.setTag(null);
                }
            }
        } else {
            flowerStack.getOrCreateTag().put(InventoryUtil.TAG_SELECTED_EFFECTS, list);
        }

        NatureCollapse.CHANNEL.sendToServer(
                new UpdateFlowerEffectsPacket(slotIndex, isCurio, curioIdentifier, packetEntries));
    }

    @Override
    public void onClose() {
        saveSelectedEffects();
        Minecraft.getInstance().setScreen(null);
    }

    @Override
    protected void renderContents(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        int margin = 14;
        int topHeaderH = 40;
        int inspectorH = 88;
        int footerH = 32;

        int contentY = topHeaderH + 4;
        int contentH = this.height - contentY - inspectorH - footerH;
        int colW = (this.width - margin * 3) / 2;
        int rightX = margin + colW + margin;

        // 全体外枠フロストガラスパネル（ヘッダーとコンテンツ領域）
        this.renderGlassPanel(gui, margin - 4, 6, this.width - (margin - 4) * 2, this.height - 12);

        // アイテムアイコン（Nature Flower）とヘッダータイトル（横スライド・回転なしで固定配置）
        gui.renderItem(this.flowerStack, margin + 4, 14);
        gui.drawString(this.font, this.title, margin + 24, 18, 0x55FFAA, true);

        // カラム見出しラベル
        gui.drawString(this.font, "Available Effects", margin, contentY - 10, 0x88DDAA, false);
        String activeTitle = "Active Effects (" + activeEffects.size() + ")";
        gui.drawString(this.font, activeTitle, rightX, contentY - 10, 0x55FFAA, false);

        // 右カラムの背景エリア枠線
        gui.fill(rightX - 1, contentY + 21, rightX + colW + 1, contentY + contentH + 1, 0xFF00AA55);
        gui.fill(rightX, contentY + 22, rightX + colW, contentY + contentH, 0xEE031008);

        // 右カラムのアクティブカードアイコン描画
        int cardH = 26;
        for (int i = 0; i < activeEffects.size(); i++) {
            EffectData data = activeEffects.get(i);
            int cardY = contentY + 22 + i * (cardH + 4) - (int) activeListScroll;
            if (cardY + cardH >= contentY + 22 && cardY <= contentY + contentH) {
                renderMobEffectIcon(gui, data.effect, rightX + 4, cardY + 4, 18);
            }
        }

        // 下部インスペクターパネル
        int inspectorY = this.height - inspectorH - footerH + 2;
        gui.fill(margin, inspectorY - 1, this.width - margin, inspectorY + inspectorH + 1, 0xFF008844);
        gui.fill(margin + 1, inspectorY, this.width - margin - 1, inspectorY + inspectorH, 0xEE05160D);

        if (editingEffect != null) {
            renderMobEffectIcon(gui, editingEffect.effect, margin + 10, inspectorY + 16, 36);

            String name = editingEffect.effect.getDisplayName().getString();
            gui.drawString(this.font, "Configuring: " + name, margin + 56, inspectorY + 6, 0x55FFAA, false);

            gui.drawString(this.font, "Level:", margin + 10, inspectorY + 22, 0xCCCCCC, false);
            gui.drawString(this.font, "Duration:", margin + 10, inspectorY + 48, 0xCCCCCC, false);

            float sec = editingEffect.duration / 20.0f;
            gui.drawString(this.font, String.format("(approx. %.1f seconds)", sec), margin + 56, inspectorY + 68, 0x88AAAA, false);
        } else {
            gui.drawCenteredString(this.font, "Select an effect from the list above to customize Level & Duration",
                    this.width / 2, inspectorY + 36, 0x888888);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (this.effectScrollList != null && this.effectScrollList.isHoveredOrFocused()) {
            return this.effectScrollList.mouseScrolled(mouseX, mouseY, delta);
        }

        // 右カラムのスクロール
        int margin = 14;
        int topHeaderH = 40;
        int inspectorH = 88;
        int footerH = 32;
        int contentY = topHeaderH + 4;
        int contentH = this.height - contentY - inspectorH - footerH;
        int colW = (this.width - margin * 3) / 2;
        int rightX = margin + colW + margin;

        if (mouseX >= rightX && mouseX <= rightX + colW && mouseY >= contentY && mouseY <= contentY + contentH) {
            int maxScroll = Math.max(0, activeEffects.size() * 30 - contentH);
            this.activeListScroll = Mth.clamp(this.activeListScroll - delta * 20.0, 0, maxScroll);
            rebuildWidgets();
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.searchBox != null && this.searchBox.isFocused() && this.searchBox.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (this.searchBox != null && this.searchBox.isFocused() && this.searchBox.charTyped(codePoint, modifiers)) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
