package n4ture27.nature_collapse.network;

import n4ture27.nature_collapse.item.NaturaLizeSword;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class UpdateRangePacket {
    private final double range;
    private final int slotId;

    public UpdateRangePacket(double range, int slotId) {
        this.range = range;
        this.slotId = slotId;
    }

    public UpdateRangePacket(FriendlyByteBuf buf) {
        this.range = buf.readDouble();
        this.slotId = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeDouble(range);
        buf.writeInt(slotId);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                ItemStack stack = player.getInventory().getItem(slotId);
                if (stack.getItem() instanceof NaturaLizeSword) {
                    stack.getOrCreateTag().putDouble("CustomRange", range);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}