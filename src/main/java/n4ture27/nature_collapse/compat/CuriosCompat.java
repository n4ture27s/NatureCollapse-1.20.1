package n4ture27.nature_collapse.compat;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.ModList;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotTypeMessage;
import top.theillusivec4.curios.common.inventory.CurioSlot;

import javax.annotation.Nullable;
import java.util.function.Consumer;

public final class CuriosCompat {
    public static final String MOD_ID = "curios";

    private CuriosCompat() {
    }

    public static boolean isLoaded() {
        return ModList.get().isLoaded(MOD_ID);
    }

    public static void enqueueImc() {
        if (!isLoaded()) return;
        Internal.enqueueImc();
    }

    public static boolean hasCurio(Player player, Item item) {
        if (!isLoaded()) return false;
        return Internal.hasCurio(player, item);
    }

    public static void forEachCurio(Player player, Item item, Consumer<ItemStack> consumer) {
        if (!isLoaded()) return;
        Internal.forEachCurio(player, item, consumer);
    }

    public static void dropAndClearAllCurios(Player player) {
        if (!isLoaded()) return;
        Internal.dropAndClearAllCurios(player);
    }

    public static ItemStack getCurioStack(Player player, String identifier, int slotIndex) {
        if (!isLoaded()) return ItemStack.EMPTY;
        return Internal.getCurioStack(player, identifier, slotIndex);
    }

    @Nullable
    public static CurioSlotInfo getSlotInfo(Slot slot) {
        if (!isLoaded()) return null;
        return Internal.getSlotInfo(slot);
    }

    public record CurioSlotInfo(String identifier, int slotIndex) {
    }

    private static final class Internal {
        @SuppressWarnings("removal")
        private static void enqueueImc() {
            InterModComms.sendTo(MOD_ID, "register_type", () -> new SlotTypeMessage.Builder("nature_flower")
                    .icon(ResourceLocation.fromNamespaceAndPath(MOD_ID, "slot/empty_charm_slot"))
                    .build());
        }

        private static boolean hasCurio(Player player, Item item) {
            return CuriosApi.getCuriosInventory(player)
                    .map(handler -> handler.findFirstCurio(item).isPresent())
                    .orElse(false);
        }

        private static void forEachCurio(Player player, Item item, Consumer<ItemStack> consumer) {
            CuriosApi.getCuriosInventory(player).ifPresent(handler ->
                    handler.findCurios(item).forEach(result -> consumer.accept(result.stack()))
            );
        }

        private static void dropAndClearAllCurios(Player player) {
            CuriosApi.getCuriosInventory(player).ifPresent(handler ->
                    handler.getCurios().values().forEach(stacksHandler -> {
                        var stacks = stacksHandler.getStacks();
                        for (int i = 0; i < stacks.getSlots(); i++) {
                            ItemStack stack = stacks.getStackInSlot(i);
                            if (!stack.isEmpty()) {
                                player.drop(stack, true);
                                stacks.setStackInSlot(i, ItemStack.EMPTY);
                            }
                        }
                    })
            );
        }

        private static ItemStack getCurioStack(Player player, String identifier, int slotIndex) {
            if (slotIndex < 0 || identifier == null || identifier.isEmpty()) {
                return ItemStack.EMPTY;
            }
            return CuriosApi.getCuriosInventory(player)
                    .resolve()
                    .flatMap(handler -> handler.getStacksHandler(identifier))
                    .map(stacksHandler -> {
                        var stacks = stacksHandler.getStacks();
                        if (slotIndex < stacks.getSlots()) {
                            return stacks.getStackInSlot(slotIndex);
                        }
                        return ItemStack.EMPTY;
                    })
                    .orElse(ItemStack.EMPTY);
        }

        @Nullable
        private static CurioSlotInfo getSlotInfo(Slot slot) {
            if (slot instanceof CurioSlot curioSlot) {
                return new CurioSlotInfo(curioSlot.getIdentifier(), curioSlot.getSlotIndex());
            }
            return null;
        }
    }
}
