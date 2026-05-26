package n4ture27.nature_collapse.network;

import n4ture27.nature_collapse.item.NatureFlower;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.fml.ModList;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class UpdateFlowerEffectsPacket {
    private final Map<String, Integer> effectLevels;
    private final int slotId;
    private final boolean isCurio;
    private final String curiosIdentifier;

    public UpdateFlowerEffectsPacket(Map<String, Integer> effectLevels, int slotId, boolean isCurio, String curiosIdentifier) {
        this.effectLevels = effectLevels;
        this.slotId = slotId;
        this.isCurio = isCurio;
        this.curiosIdentifier = curiosIdentifier;
    }

    public UpdateFlowerEffectsPacket(FriendlyByteBuf buf) {
        this.effectLevels = buf.readMap(FriendlyByteBuf::readUtf, FriendlyByteBuf::readInt);
        this.slotId = buf.readInt();
        this.isCurio = buf.readBoolean();
        this.curiosIdentifier = buf.readUtf();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeMap(effectLevels, FriendlyByteBuf::writeUtf, FriendlyByteBuf::writeInt);
        buf.writeInt(slotId);
        buf.writeBoolean(isCurio);
        buf.writeUtf(curiosIdentifier);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                ItemStack stack = ItemStack.EMPTY;

                if (isCurio && ModList.get().isLoaded("curios")) {
                    final ItemStack[] foundStack = {ItemStack.EMPTY};
                    CuriosApi.getCuriosInventory(player).ifPresent(handler ->
                            handler.getStacksHandler(curiosIdentifier).ifPresent(stacks ->
                                    foundStack[0] = stacks.getStacks().getStackInSlot(slotId)
                            )
                    );
                    stack = foundStack[0];
                } else {
                    stack = player.getInventory().getItem(slotId);
                }

                if (!stack.isEmpty() && stack.getItem() instanceof NatureFlower) {
                    applyToStack(stack);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }

    private void applyToStack(ItemStack stack) {
        ListTag list = new ListTag();
        effectLevels.forEach((id, level) -> {
            CompoundTag compound = new CompoundTag();
            compound.putString("Id", id);
            compound.putInt("Level", level);
            list.add(compound);
        });
        stack.getOrCreateTag().put("SelectedEffects", list);
    }
}