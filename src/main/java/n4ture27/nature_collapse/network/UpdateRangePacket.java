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
    private final boolean targetMonsters;
    private final boolean targetPassive;
    private final boolean targetPlayers;
    private final boolean targetItemsAndXP;
    private final boolean targetNonLiving;

    public UpdateRangePacket(double range, int slotId, boolean isCurio, String curiosIdentifier,
                             boolean targetMonsters, boolean targetPassive, boolean targetPlayers,
                             boolean targetItemsAndXP, boolean targetNonLiving) {
        this.range = range;
        this.slotId = slotId;
        this.isCurio = isCurio;
        this.curiosIdentifier = curiosIdentifier != null ? curiosIdentifier : "";
        this.targetMonsters = targetMonsters;
        this.targetPassive = targetPassive;
        this.targetPlayers = targetPlayers;
        this.targetItemsAndXP = targetItemsAndXP;
        this.targetNonLiving = targetNonLiving;
    }

    public UpdateRangePacket(FriendlyByteBuf buf) {
        this.range = buf.readDouble();
        this.slotId = buf.readInt();
        this.isCurio = buf.readBoolean();
        this.curiosIdentifier = buf.readUtf(64);
        this.targetMonsters = buf.readBoolean();
        this.targetPassive = buf.readBoolean();
        this.targetPlayers = buf.readBoolean();
        this.targetItemsAndXP = buf.readBoolean();
        this.targetNonLiving = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeDouble(range);
        buf.writeInt(slotId);
        buf.writeBoolean(isCurio);
        buf.writeUtf(curiosIdentifier, 64);
        buf.writeBoolean(targetMonsters);
        buf.writeBoolean(targetPassive);
        buf.writeBoolean(targetPlayers);
        buf.writeBoolean(targetItemsAndXP);
        buf.writeBoolean(targetNonLiving);
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
                InventoryUtil.setSwordTargetFilter(stack, new InventoryUtil.SwordTargetFilter(
                        targetMonsters, targetPassive, targetPlayers, targetItemsAndXP, targetNonLiving
                ));
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
