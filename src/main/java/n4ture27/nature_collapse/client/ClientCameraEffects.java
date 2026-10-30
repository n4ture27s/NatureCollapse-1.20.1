package n4ture27.nature_collapse.client;

import n4ture27.nature_collapse.NatureCollapse;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Random;

@Mod.EventBusSubscriber(modid = NatureCollapse.ModId, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ClientCameraEffects {
    private static float currentIntensity = 0.0f;
    private static int remainingDuration = 0;
    private static final Random RANDOM = new Random();

    public static void addShake(float intensity, int duration) {
        if (intensity > currentIntensity || remainingDuration <= 0) {
            currentIntensity = intensity;
        }
        remainingDuration = Math.max(remainingDuration, duration);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            if (remainingDuration > 0) {
                remainingDuration--;
                if (remainingDuration <= 0) {
                    currentIntensity = 0.0f;
                } else {
                    currentIntensity *= 0.96f;
                }
            }
        }
    }

    @SubscribeEvent
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (remainingDuration > 0 && currentIntensity > 0.001f) {
            float pitchOffset = (RANDOM.nextFloat() * 2.0f - 1.0f) * currentIntensity;
            float yawOffset = (RANDOM.nextFloat() * 2.0f - 1.0f) * currentIntensity;
            float rollOffset = (RANDOM.nextFloat() * 2.0f - 1.0f) * currentIntensity * 0.7f;

            event.setPitch(event.getPitch() + pitchOffset);
            event.setYaw(event.getYaw() + yawOffset);
            event.setRoll(event.getRoll() + rollOffset);
        }
    }
}
