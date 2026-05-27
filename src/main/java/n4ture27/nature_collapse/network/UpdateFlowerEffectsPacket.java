package n4ture27.nature_collapse.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.network.NetworkEvent;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;
import java.util.function.Supplier;

public class UpdateFlowerEffectsPacket {
    private final int slotIndex;
    private final boolean isCurio;
    private final String curioIdentifier;
    private final ItemStack flowerStack;

    public UpdateFlowerEffectsPacket(int slotIndex, boolean isCurio, String curioIdentifier, ItemStack flowerStack) {
        this.slotIndex = slotIndex;
        this.isCurio = isCurio;
        this.curioIdentifier = curioIdentifier;
        this.flowerStack = flowerStack;
    }

    public UpdateFlowerEffectsPacket(FriendlyByteBuf buf) {
        this.slotIndex = buf.readInt();
        this.isCurio = buf.readBoolean();
        this.curioIdentifier = buf.readUtf();
        this.flowerStack = buf.readItem();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(slotIndex);
        buf.writeBoolean(isCurio);
        buf.writeUtf(curioIdentifier);
        buf.writeItem(flowerStack);
    }

    public static void handle(UpdateFlowerEffectsPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Player player = ctx.get().getSender();
            if (player == null) return;

            ItemStack targetStack = ItemStack.EMPTY;

            if (msg.isCurio) {
                if (ModList.get().isLoaded("curios")) {
                        Optional<ICurioStacksHandler> handler = CuriosApi.getCuriosInventory(player).resolve()
                            .flatMap(h -> h.getStacksHandler(msg.curioIdentifier));
                    if (handler.isPresent() && msg.slotIndex < handler.get().getStacks().getSlots()) {
                        targetStack = handler.get().getStacks().getStackInSlot(msg.slotIndex);
                    }
                }
            } else {
                if (msg.slotIndex >= 0 && msg.slotIndex < player.getInventory().getContainerSize()) {
                    targetStack = player.getInventory().getItem(msg.slotIndex);
                }
            }

            if (!targetStack.isEmpty() && targetStack.getItem() == msg.flowerStack.getItem()) {
                targetStack.setTag(msg.flowerStack.getTag());
            }
        });
        ctx.get().setPacketHandled(true);
    }
}