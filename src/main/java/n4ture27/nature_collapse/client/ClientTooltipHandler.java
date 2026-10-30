package n4ture27.nature_collapse.client;

import com.mojang.blaze3d.platform.InputConstants;
import n4ture27.nature_collapse.NatureCollapse;
import n4ture27.nature_collapse.compat.CuriosCompat;
import n4ture27.nature_collapse.item.NaturaLizeSword;
import n4ture27.nature_collapse.item.NatureFlower;
import n4ture27.nature_collapse.ui.FlowerEffectSettingsScreen;
import n4ture27.nature_collapse.ui.FlowerHubScreen;
import n4ture27.nature_collapse.ui.RangeAdjustmentScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = NatureCollapse.ModId, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ClientTooltipHandler {

    public static final KeyMapping OPEN_RANGE_GUI = new KeyMapping(
            "key.nature_collapse.open_range_gui",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_K,
            "key.categories.nature_collapse"
    );

    public static boolean hasShiftDown() {
        return Screen.hasShiftDown();
    }

    public static String getKeyBindingDisplayName() {
        return OPEN_RANGE_GUI.getTranslatedKeyMessage().getString();
    }

    @SubscribeEvent
    public static void onTooltipColor(RenderTooltipEvent.Color event) {
        ItemStack stack = event.getItemStack();
        Item item = stack.getItem();
        if (!stack.isEmpty() && (item instanceof NaturaLizeSword || item instanceof NatureFlower)) {
            event.setBackground(0xF0002200);
            event.setBorderStart(0xFF00FF00);
            event.setBorderEnd(0xFF008800);
        }
    }

    @SubscribeEvent
    public static void onScreenKey(ScreenEvent.KeyPressed.Pre ev) {
        if (!(ev.getScreen() instanceof AbstractContainerScreen<?> container)) {
            return;
        }
        if (!OPEN_RANGE_GUI.isActiveAndMatches(InputConstants.getKey(ev.getKeyCode(), ev.getScanCode()))) {
            return;
        }

        Slot slot = container.getSlotUnderMouse();
        if (slot == null || slot.getItem().isEmpty()) {
            return;
        }

        Player localPlayer = Minecraft.getInstance().player;
        if (localPlayer == null) {
            return;
        }

        boolean isCurio = false;
        String identifier = "";
        int slotIndex = slot.getContainerSlot();

        CuriosCompat.CurioSlotInfo curioSlotInfo = CuriosCompat.getSlotInfo(slot);
        if (curioSlotInfo != null) {
            isCurio = true;
            identifier = curioSlotInfo.identifier();
            slotIndex = curioSlotInfo.slotIndex();
        } else if (slot.container != localPlayer.getInventory()) {
            // Ignore slots belonging to external containers (chests, etc.) so we don't overwrite player inventory slots
            return;
        }

        ItemStack stack = slot.getItem();
        if (stack.getItem() instanceof NaturaLizeSword) {
            Minecraft.getInstance().setScreen(new RangeAdjustmentScreen(stack, slotIndex, isCurio, identifier));
            ev.setCanceled(true);
        } else if (stack.getItem() instanceof NatureFlower) {
            Minecraft.getInstance().setScreen(new FlowerHubScreen(stack, slotIndex, isCurio, identifier));
            ev.setCanceled(true);
        }
    }

    @Mod.EventBusSubscriber(modid = NatureCollapse.ModId, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModBusEvents {
        @SubscribeEvent
        public static void onKeyRegister(RegisterKeyMappingsEvent event) {
            event.register(OPEN_RANGE_GUI);
        }
    }
}
