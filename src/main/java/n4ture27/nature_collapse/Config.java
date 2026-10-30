package n4ture27.nature_collapse;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = NatureCollapse.ModId, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.DoubleValue DEFAULT_SWORD_RANGE = BUILDER
            .comment("Default attack raycast range for the Naturalize Sword.")
            .defineInRange("defaultSwordRange", 10.0, 1.0, 256.0);

    private static final ForgeConfigSpec.DoubleValue MAX_SWORD_RANGE = BUILDER
            .comment("Maximum configurable attack raycast range for the Naturalize Sword.")
            .defineInRange("maxSwordRange", 100.0, 1.0, 256.0);

    private static final ForgeConfigSpec.BooleanValue FLOWER_FLIGHT_ENABLED = BUILDER
            .comment("Whether holding/equipping the Nature Flower grants creative-style flight.")
            .define("flowerFlightEnabled", true);

    private static final ForgeConfigSpec.BooleanValue FLOWER_SATURATION_ENABLED = BUILDER
            .comment("Whether holding/equipping the Nature Flower keeps food and saturation full.")
            .define("flowerSaturationEnabled", true);

    static final ForgeConfigSpec SPEC = BUILDER.build();

    public static double defaultSwordRange = 10.0;
    public static double maxSwordRange = 100.0;
    public static boolean flowerFlightEnabled = true;
    public static boolean flowerSaturationEnabled = true;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        maxSwordRange = MAX_SWORD_RANGE.get();
        defaultSwordRange = Math.min(DEFAULT_SWORD_RANGE.get(), maxSwordRange);
        flowerFlightEnabled = FLOWER_FLIGHT_ENABLED.get();
        flowerSaturationEnabled = FLOWER_SATURATION_ENABLED.get();
    }
}
