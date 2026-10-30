package n4ture27.nature_collapse.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class CameraShakePacket {
    private final float intensity;
    private final int duration;

    public CameraShakePacket(float intensity, int duration) {
        this.intensity = intensity;
        this.duration = duration;
    }

    public CameraShakePacket(FriendlyByteBuf buf) {
        this.intensity = buf.readFloat();
        this.duration = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeFloat(this.intensity);
        buf.writeInt(this.duration);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                n4ture27.nature_collapse.client.ClientCameraEffects.addShake(this.intensity, this.duration);
            });
        });
        ctx.get().setPacketHandled(true);
    }
}
