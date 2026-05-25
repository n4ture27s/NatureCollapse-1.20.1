package n4ture27.nature_collapse.client;

import net.minecraft.network.chat.Component;

import n4ture27.nature_collapse.NatureCollapse;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

// Create a wrapper class to manage registration
public class CreativeModeTabs {
    // 1. Define the DeferredRegister
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, NatureCollapse.ModId);

    // 2. Define the tab, referencing your items/blocks
    public static final RegistryObject<CreativeModeTab> TABS = CREATIVE_MODE_TABS.register("test", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup." + NatureCollapse.ModId))
            .icon(() -> new ItemStack(NatureCollapse.NatureFlower.get()))
            .displayItems((params, output) -> {
                output.accept(NatureCollapse.NaturelizeSword.get());
                output.accept(NatureCollapse.NatureFlower.get());
            })
            .build()
    );

    // 3. Register the DeferredRegister to the mod event bus
    public static void register(IEventBus bus) {
        CREATIVE_MODE_TABS.register(bus);
    }
}
