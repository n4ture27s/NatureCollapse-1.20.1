package n4ture27.nature_collapse.network;

import n4ture27.nature_collapse.item.NaturaLizeSword;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.fml.ModList;
import top.theillusivec4.curios.api.CuriosApi;

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
        this.curiosIdentifier = curiosIdentifier;
    }

    public UpdateRangePacket(FriendlyByteBuf buf) {
        this.range = buf.readDouble();
        this.slotId = buf.readInt();
        this.isCurio = buf.readBoolean();
        this.curiosIdentifier = buf.readUtf();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeDouble(range);
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

                if (!stack.isEmpty() && stack.getItem() instanceof NaturaLizeSword) {
                    stack.getOrCreateTag().putDouble("CustomRange", range);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}