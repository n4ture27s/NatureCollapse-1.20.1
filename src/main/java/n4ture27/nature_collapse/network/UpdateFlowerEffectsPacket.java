package n4ture27.nature_collapse.network;

import n4ture27.nature_collapse.item.NatureFlower;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class UpdateFlowerEffectsPacket {
    private final Map<String, Integer> effectLevels;
    private final int slotId;

    public UpdateFlowerEffectsPacket(Map<String, Integer> effectLevels, int slotId) {
        this.effectLevels = effectLevels;
        this.slotId = slotId;
    }

    public UpdateFlowerEffectsPacket(FriendlyByteBuf buf) {
        this.effectLevels = buf.readMap(FriendlyByteBuf::readUtf, FriendlyByteBuf::readInt);
        this.slotId = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeMap(effectLevels, FriendlyByteBuf::writeUtf, FriendlyByteBuf::writeInt);
        buf.writeInt(slotId);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                ItemStack stack = player.getInventory().getItem(slotId);
                if (stack.getItem() instanceof NatureFlower) {
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
        });
        ctx.get().setPacketHandled(true);
    }
}