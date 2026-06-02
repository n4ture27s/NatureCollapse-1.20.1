package n4ture27.nature_collapse.ui;

import n4ture27.nature_collapse.NatureCollapse;
import n4ture27.nature_collapse.network.UpdateFlowerEffectsPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class FlowerEffectSettingsScreen extends Screen {

    private static class EditableSlider
            extends AbstractSliderButton {

        public EditableSlider(
                int x,
                int y,
                int width,
                int height,
                Component message,
                double value) {
            super(
                    x,
                    y,
                    width,
                    height,
                    message,
                    value);
        }

        public void forceValue(
                double newValue) {

            this.value = newValue;

            updateMessage();
        }

        @Override
        protected void updateMessage() {
        }

        @Override
        protected void applyValue() {
        }
    }

    public static class EffectData {

        public MobEffect effect;
        public int level;
        public int duration;

        public EffectData(
                MobEffect effect,
                int level,
                int duration) {
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

    private List<MobEffect> allEffects;
    private List<MobEffect> filteredEffects;

    private List<EffectData> activeEffects;

    private EffectData editingEffect;

    private EditableSlider levelSlider;
    private EditableSlider durationSlider;

    private static final int EFFECTS_PER_PAGE = 8;

    private int currentPage = 0;

    public FlowerEffectSettingsScreen(
            ItemStack flowerStack,
            int slotIndex,
            boolean isCurio,
            String curioIdentifier) {
        super(
                Component.translatable(
                        "gui.nature_collapse.flower_settings"));

        this.flowerStack = flowerStack;
        this.slotIndex = slotIndex;
        this.isCurio = isCurio;
        this.curioIdentifier = curioIdentifier;

        this.activeEffects = new ArrayList<>();

        loadSelectedEffects();
    }

    private EffectData getEffectData(
            MobEffect effect) {

        for (EffectData data : activeEffects) {

            if (data.effect == effect) {
                return data;
            }
        }

        return null;
    }

    private void addEffect(
            MobEffect effect) {

        if (getEffectData(effect) != null) {
            return;
        }

        EffectData data = new EffectData(
                effect,
                1,
                200);

        activeEffects.add(data);

        editingEffect = data;

        syncSliders();
    }

    private void removeEffect(
            EffectData data) {

        activeEffects.remove(data);

        if (editingEffect == data) {

            if (activeEffects.isEmpty()) {

                editingEffect = null;

            } else {

                editingEffect = activeEffects.get(0);
            }
        }

        syncSliders();
    }

    private void syncSliders() {

        if (editingEffect == null
                ||
                levelSlider == null
                ||
                durationSlider == null) {
            return;
        }

        levelSlider.forceValue(
                (editingEffect.level - 1)
                        / 254.0);

        durationSlider.forceValue(
                (editingEffect.duration - 20)
                        / 11980.0);
    }

    @Override
    protected void init() {

        super.init();

        this.allEffects = ForgeRegistries.MOB_EFFECTS
                .getValues()
                .stream()
                .filter(Objects::nonNull)
                .sorted(
                        (e1, e2) -> e1.getDisplayName()
                                .getString()
                                .compareTo(
                                        e2.getDisplayName()
                                                .getString()))
                .collect(Collectors.toList());

        this.searchBox = new EditBox(
                this.font,
                10,
                20,
                180,
                20,
                Component.literal("Search"));

        this.searchBox.setResponder(
                this::onSearchChanged);

        createSliders();

        applyFilter();

        rebuildWidgets();
    }

    private void createSliders() {

        levelSlider = new EditableSlider(
                this.width / 2 - 100,
                this.height - 75,
                200,
                20,
                Component.literal("Level"),
                0) {

            public void setValue(
                    double value) {
                this.value = value;
                updateMessage();
            }

            @Override
            protected void updateMessage() {

                int level = (int) (value * 254) + 1;

                setMessage(
                        Component.literal(
                                "Level : "
                                        + level));
            }

            @Override
            protected void applyValue() {

                if (editingEffect != null) {

                    editingEffect.level = (int) (value * 254)
                            + 1;
                }
            }
        };

        durationSlider = new EditableSlider(
                this.width / 2 - 100,
                this.height - 50,
                200,
                20,
                Component.literal("Duration"),
                0) {

            public void setValue(
                    double value) {
                this.value = value;
                updateMessage();
            }

            @Override
            protected void updateMessage() {

                int duration = (int) (value * 11980)
                        + 20;

                setMessage(
                        Component.literal(
                                "Duration : "
                                        + duration
                                        + " ticks"));
            }

            @Override
            protected void applyValue() {

                if (editingEffect != null) {

                    editingEffect.duration = (int) (value * 11980)
                            + 20;
                }
            }
        };
    }

    protected void rebuildWidgets() {

        clearWidgets();

        addRenderableWidget(searchBox);

        int leftX = 10;
        int leftY = 50;

        int startIndex = currentPage * EFFECTS_PER_PAGE;

        int endIndex = Math.min(
                startIndex
                        + EFFECTS_PER_PAGE,
                filteredEffects.size());

        for (int i = startIndex; i < endIndex; i++) {

            MobEffect effect = filteredEffects.get(i);

            boolean active = getEffectData(effect) != null;

            Component text = effect.getDisplayName()
                    .copy()
                    .append(
                            active
                                    ? " §a[ACTIVE]"
                                    : "");

            addRenderableWidget(
                    Button.builder(
                            text,
                            button -> {

                                addEffect(effect);

                                rebuildWidgets();
                            }).bounds(
                                    leftX,
                                    leftY
                                            + (i - startIndex)
                                                    * 22,
                                    180,
                                    20)
                            .build());
        }

        int rightX = this.width - 220;

        int activeY = 50;

        for (int i = 0; i < activeEffects.size(); i++) {

            EffectData data = activeEffects.get(i);

            int rowY = activeY + i * 22;

            addRenderableWidget(
                    Button.builder(
                            Component.literal(
                                    data.effect
                                            .getDisplayName()
                                            .getString()
                                            + " Lv"
                                            + data.level),
                            button -> {

                                editingEffect = data;

                                rebuildWidgets();
                            }).bounds(
                                    rightX,
                                    rowY,
                                    170,
                                    20)
                            .build());

            addRenderableWidget(
                    Button.builder(
                            Component.literal("X"),
                            button -> {

                                removeEffect(data);

                                rebuildWidgets();
                            }).bounds(
                                    rightX + 175,
                                    rowY,
                                    20,
                                    20)
                            .build());
        }

        int totalPages = (int) Math.ceil(
                (double) filteredEffects.size()
                        / EFFECTS_PER_PAGE);

        if (totalPages > 1) {

            addRenderableWidget(
                    Button.builder(
                            Component.literal("<"),
                            button -> {

                                currentPage = Math.max(
                                        0,
                                        currentPage - 1);

                                rebuildWidgets();
                            }).bounds(
                                    10,
                                    235,
                                    20,
                                    20)
                            .build());

            addRenderableWidget(
                    Button.builder(
                            Component.literal(">"),
                            button -> {

                                currentPage = Math.min(
                                        totalPages - 1,
                                        currentPage + 1);

                                rebuildWidgets();
                            }).bounds(
                                    170,
                                    235,
                                    20,
                                    20)
                            .build());
        }

        if (editingEffect != null) {

            addRenderableWidget(levelSlider);
            addRenderableWidget(durationSlider);

            syncSliders();
        }

        addRenderableWidget(
                Button.builder(
                        CommonComponents.GUI_DONE,
                        button -> {

                            saveSelectedEffects();

                            Minecraft.getInstance()
                                    .setScreen(null);
                        }).bounds(
                                this.width / 2 - 100,
                                this.height - 25,
                                200,
                                20)
                        .build());
    }

    private void onSearchChanged(
            String text) {

        currentPage = 0;

        applyFilter();

        rebuildWidgets();
    }

    private void applyFilter() {

        String search = searchBox.getValue()
                .toLowerCase();

        if (search.isEmpty()) {

            filteredEffects = new ArrayList<>(
                    allEffects);

            return;
        }

        filteredEffects = allEffects.stream()
                .filter(effect ->

                effect
                        .getDisplayName()
                        .getString()
                        .toLowerCase()
                        .contains(search)

                        ||

                        ForgeRegistries.MOB_EFFECTS
                                .getKey(effect)
                                .toString()
                                .toLowerCase()
                                .contains(search)

                )
                .collect(Collectors.toList());
    }

    private void loadSelectedEffects() {

        if (!flowerStack.hasTag()
                ||
                !flowerStack.getTag()
                        .contains(
                                "SelectedEffects")) {
            return;
        }

        ListTag list = flowerStack.getTag()
                .getList(
                        "SelectedEffects",
                        Tag.TAG_COMPOUND);

        for (int i = 0; i < list.size(); i++) {

            CompoundTag effectTag = list.getCompound(i);

            MobEffect effect = ForgeRegistries.MOB_EFFECTS
                    .getValue(
                            ResourceLocation.tryParse(
                                    effectTag.getString("Id")));

            if (effect == null) {
                continue;
            }

            activeEffects.add(
                    new EffectData(
                            effect,
                            effectTag.getInt("Level"),
                            effectTag.getInt("Duration")));
        }

        if (!activeEffects.isEmpty()) {

            editingEffect = activeEffects.get(0);
        }
    }

    private void saveSelectedEffects() {

        CompoundTag tag = flowerStack.getOrCreateTag();

        ListTag list = new ListTag();

        for (EffectData data : activeEffects) {

            CompoundTag effectTag = new CompoundTag();

            effectTag.putString(
                    "Id",
                    Objects.requireNonNull(
                            ForgeRegistries.MOB_EFFECTS
                                    .getKey(
                                            data.effect))
                            .toString());

            effectTag.putInt(
                    "Level",
                    data.level);

            effectTag.putInt(
                    "Duration",
                    data.duration);

            list.add(effectTag);
        }

        tag.put(
                "SelectedEffects",
                list);

        flowerStack.setTag(tag);

        NatureCollapse.CHANNEL.sendToServer(
                new UpdateFlowerEffectsPacket(
                        slotIndex,
                        isCurio,
                        curioIdentifier,
                        flowerStack));
    }

    @Override
    public void render(
            GuiGraphics guiGraphics,
            int mouseX,
            int mouseY,
            float partialTick) {

        renderBackground(guiGraphics);

        super.render(
                guiGraphics,
                mouseX,
                mouseY,
                partialTick);

        guiGraphics.drawCenteredString(
                font,
                title,
                width / 2,
                6,
                0xFFFFFF);

        guiGraphics.drawString(
                font,
                "Available Effects",
                10,
                38,
                0xFFFF55);

        guiGraphics.drawString(
                font,
                "Active Effects",
                width - 220,
                38,
                0x55FF55);

        if (editingEffect != null) {

            guiGraphics.drawCenteredString(
                    font,
                    Component.literal(
                            "Editing : "
                                    + editingEffect.effect
                                            .getDisplayName()
                                            .getString()),
                    width / 2,
                    height - 100,
                    0xFFFFFF);
        }

        int totalPages = (int) Math.ceil(
                (double) filteredEffects.size()
                        / EFFECTS_PER_PAGE);

        guiGraphics.drawString(
                font,
                (currentPage + 1)
                        + "/"
                        + Math.max(
                                totalPages,
                                1),
                90,
                238,
                0xFFFFFF);
    }

    @Override
    public boolean keyPressed(
            int keyCode,
            int scanCode,
            int modifiers) {

        if (searchBox != null
                &&
                searchBox.keyPressed(
                        keyCode,
                        scanCode,
                        modifiers)) {
            return true;
        }

        return super.keyPressed(
                keyCode,
                scanCode,
                modifiers);
    }

    @Override
    public boolean charTyped(
            char codePoint,
            int modifiers) {

        if (searchBox != null
                &&
                searchBox.charTyped(
                        codePoint,
                        modifiers)) {
            return true;
        }

        return super.charTyped(
                codePoint,
                modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}