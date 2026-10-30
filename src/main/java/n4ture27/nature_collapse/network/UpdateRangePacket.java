package n4ture27.nature_collapse.network;

import n4ture27.nature_collapse.item.NaturaLizeSword;
import n4ture27.nature_collapse.utils.InventoryUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class UpdateRangePacket {
    private final double range;
    private final int slotId;
    private final boolean isCurio;
    private final String curiosIdentifier;

    public UpdateRangePacket(double range, int slotId, boolean isCurio, String curiosIdentifier) {
        this.range = range;
        this.slotId = slotId;
        this.isCurio = isCurio;
        this.curiosIdentifier = curiosIdentifier != null ? curiosIdentifier : "";
    }

    public UpdateRangePacket(FriendlyByteBuf buf) {
        this.range = buf.readDouble();
        this.slotId = buf.readInt();
        this.isCurio = buf.readBoolean();
        this.curiosIdentifier = buf.readUtf(64);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeDouble(range);
        buf.writeInt(slotId);
        buf.writeBoolean(isCurio);
        buf.writeUtf(curiosIdentifier, 64);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            if (Double.isNaN(range) || Double.isInfinite(range) || range <= 0.0) return;
            double validRange = net.minecraft.util.Mth.clamp(range, 1.0, Math.max(1.0, n4ture27.nature_collapse.Config.maxSwordRange));

            ItemStack stack = InventoryUtil.getStackFromSlot(player, slotId, isCurio, curiosIdentifier);
            if (!stack.isEmpty() && stack.getItem() instanceof NaturaLizeSword) {
                InventoryUtil.setSwordRange(stack, validRange);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
