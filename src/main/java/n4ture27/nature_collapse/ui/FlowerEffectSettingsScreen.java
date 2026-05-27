package n4ture27.nature_collapse.ui;

import com.mojang.blaze3d.systems.RenderSystem;
import n4ture27.nature_collapse.NatureCollapse;
import n4ture27.nature_collapse.network.UpdateFlowerEffectsPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class FlowerEffectSettingsScreen extends Screen {

    private final ItemStack flowerStack;
    private final int slotIndex;
    private final boolean isCurio;
    private final String curioIdentifier;

    private EditBox searchBox;
    private List<MobEffect> allEffects;
    private List<MobEffect> filteredEffects;
    private List<MobEffect> selectedEffects;

    private static final int EFFECTS_PER_PAGE = 8;
    private int currentPage = 0;

    public FlowerEffectSettingsScreen(ItemStack flowerStack, int slotIndex, boolean isCurio, String curioIdentifier) {
        super(Component.translatable("gui.nature_collapse.flower_settings"));
        this.flowerStack = flowerStack;
        this.slotIndex = slotIndex;
        this.isCurio = isCurio;
        this.curioIdentifier = curioIdentifier;
        this.selectedEffects = new ArrayList<>();
        loadSelectedEffects();
    }

    @Override
    protected void init() {
        super.init();
        this.allEffects = ForgeRegistries.MOB_EFFECTS.getValues().stream()
                .filter(Objects::nonNull)
                .sorted((e1, e2) -> e1.getDisplayName().getString().compareTo(e2.getDisplayName().getString()))
                .collect(Collectors.toList());
        
        this.searchBox = new EditBox(this.font, this.width / 2 - 100, 20, 200, 20, Component.translatable("gui.nature_collapse.search_effect"));
        this.searchBox.setMaxLength(50);
        this.searchBox.setResponder(this::onSearchBoxChanged);
        this.addRenderableWidget(this.searchBox);

        this.applyFilter();
        this.addButtons();
    }

    private void addButtons() {
        this.clearWidgets(); // Clear existing buttons before re-adding
        this.addRenderableWidget(this.searchBox); // Re-add search box

        int startX = this.width / 2 - 100;
        int startY = 50;
        int buttonHeight = 20;
        int buttonSpacing = 2;

        int startIndex = currentPage * EFFECTS_PER_PAGE;
        int endIndex = Math.min(startIndex + EFFECTS_PER_PAGE, filteredEffects.size());

        for (int i = startIndex; i < endIndex; i++) {
            MobEffect effect = filteredEffects.get(i);
            boolean isSelected = selectedEffects.contains(effect);
            Component buttonText = effect.getDisplayName().copy().append(isSelected ? " (§a選択中§r)" : "");
            
            this.addRenderableWidget(Button.builder(buttonText, (button) -> {
                toggleEffect(effect);
                this.applyFilter(); // Reapply filter to update button text
                this.addButtons(); // Re-add buttons to update state
            }).bounds(startX, startY + (i - startIndex) * (buttonHeight + buttonSpacing), 200, buttonHeight).build());
        }

        // Pagination buttons
        int totalPages = (int) Math.ceil((double) filteredEffects.size() / EFFECTS_PER_PAGE);
        if (totalPages > 1) {
            this.addRenderableWidget(Button.builder(Component.literal("<"), (button) -> {
                currentPage = Math.max(0, currentPage - 1);
                this.addButtons();
            }).bounds(startX - 30, startY + EFFECTS_PER_PAGE * (buttonHeight + buttonSpacing), 20, buttonHeight).build());

            this.addRenderableWidget(Button.builder(Component.literal(">"), (button) -> {
                currentPage = Math.min(totalPages - 1, currentPage + 1);
                this.addButtons();
            }).bounds(startX + 210, startY + EFFECTS_PER_PAGE * (buttonHeight + buttonSpacing), 20, buttonHeight).build());
        }

        // Done button
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, (button) -> {
            saveSelectedEffects();
            Minecraft.getInstance().setScreen(null);
        }).bounds(this.width / 2 - 100, this.height - 30, 200, 20).build());
    }

    private void onSearchBoxChanged(String text) {
        this.currentPage = 0; // Reset page on search
        this.applyFilter();
        this.addButtons();
    }

    private void applyFilter() {
        String searchText = searchBox.getValue().toLowerCase();
        if (searchText.isEmpty()) {
            filteredEffects = new ArrayList<>(allEffects);
        } else {
            filteredEffects = allEffects.stream()
                    .filter(effect -> ForgeRegistries.MOB_EFFECTS.getKey(effect).toString().toLowerCase().contains(searchText) ||
                                     effect.getDisplayName().getString().toLowerCase().contains(searchText))
                    .collect(Collectors.toList());
        }
    }

    private void toggleEffect(MobEffect effect) {
        if (selectedEffects.contains(effect)) {
            selectedEffects.remove(effect);
        } else {
            selectedEffects.add(effect);
        }
    }

    private void loadSelectedEffects() {
        if (flowerStack.hasTag() && flowerStack.getTag().contains("SelectedEffects")) {
            ListTag list = flowerStack.getTag().getList("SelectedEffects", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag effectData = list.getCompound(i);
                MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(ResourceLocation.tryParse(effectData.getString("Id")));
                if (effect != null) {
                    selectedEffects.add(effect);
                }
            }
        }
    }

    private void saveSelectedEffects() {
        CompoundTag tag = flowerStack.getOrCreateTag();
        ListTag list = new ListTag();
        for (MobEffect effect : selectedEffects) {
            CompoundTag effectData = new CompoundTag();
            effectData.putString("Id", Objects.requireNonNull(ForgeRegistries.MOB_EFFECTS.getKey(effect)).toString());
            // Amplifier and Duration are not configurable in this GUI, so use defaults or existing values
            // For simplicity, we'll just save the ID. If you need to save level/duration, you'd need more complex UI.
            effectData.putInt("Level", 1); // Default level
            effectData.putInt("Duration", 200); // Default duration (10 seconds)
            list.add(effectData);
        }
        tag.put("SelectedEffects", list);
        flowerStack.setTag(tag);

        // Send packet to server to update the item's NBT
        NatureCollapse.CHANNEL.sendToServer(new UpdateFlowerEffectsPacket(slotIndex, isCurio, curioIdentifier, flowerStack));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, pMouseX, pMouseY, pPartialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 8, 0xFFFFFF);

        // Draw current page info
        int totalPages = (int) Math.ceil((double) filteredEffects.size() / EFFECTS_PER_PAGE);
        if (totalPages > 1) {
            guiGraphics.drawCenteredString(this.font, Component.literal((currentPage + 1) + "/" + totalPages), this.width / 2, this.height - 50, 0xFFFFFF);
        }
    }

    @Override
    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        if (this.searchBox.keyPressed(pKeyCode, pScanCode, pModifiers)) {
            return true;
        }
        return super.keyPressed(pKeyCode, pScanCode, pModifiers);
    }

    @Override
    public boolean charTyped(char pCodePoint, int pModifiers) {
        if (this.searchBox.charTyped(pCodePoint, pModifiers)) {
            return true;
        }
        return super.charTyped(pCodePoint, pModifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}