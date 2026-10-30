package n4ture27.nature_collapse.network;

import n4ture27.nature_collapse.item.NatureFlower;
import n4ture27.nature_collapse.utils.InventoryUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class UpdateFlowerConfigPacket {
    private final int slotIndex;
    private final boolean isCurio;
    private final String curioIdentifier;
    private final InventoryUtil.FlowerConfig config;

    public UpdateFlowerConfigPacket(int slotIndex, boolean isCurio, String curioIdentifier, InventoryUtil.FlowerConfig config) {
        this.slotIndex = slotIndex;
        this.isCurio = isCurio;
        this.curioIdentifier = curioIdentifier != null ? curioIdentifier : "";
        this.config = config != null ? config : InventoryUtil.FlowerConfig.createDefault();
    }

    public UpdateFlowerConfigPacket(FriendlyByteBuf buf) {
        this.slotIndex = buf.readInt();
        this.isCurio = buf.readBoolean();
        this.curioIdentifier = buf.readUtf(64);
        CompoundTag tag = buf.readNbt();
        this.config = InventoryUtil.FlowerConfig.fromTag(tag);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(slotIndex);
        buf.writeBoolean(isCurio);
        buf.writeUtf(curioIdentifier, 64);
        buf.writeNbt(config.toTag());
    }

    public static void handle(UpdateFlowerConfigPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            ItemStack targetStack = InventoryUtil.getStackFromSlot(player, msg.slotIndex, msg.isCurio, msg.curioIdentifier);
            if (!targetStack.isEmpty() && targetStack.getItem() instanceof NatureFlower) {
                InventoryUtil.setFlowerConfig(targetStack, msg.config);
                n4ture27.nature_collapse.EventHandler.updatePlayerMobilityDirect(player);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
