package n4ture27.nature_collapse.client;

import n4ture27.nature_collapse.item.NaturaLizeSword;
import n4ture27.nature_collapse.item.NatureFlower;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.client.event.RenderTooltipEvent;

@Mod.EventBusSubscriber(modid = "nature_collapse", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ClientTooltipHandler {

    @SubscribeEvent
    public static void onTooltipColor(RenderTooltipEvent.Color event) {
        Item item = event.getItemStack().getItem();
        if (!event.getItemStack().isEmpty() && (item instanceof NaturaLizeSword || item instanceof NatureFlower)) {
            // 背景色 (ARGB: 濃い緑色)
            event.setBackground(0xF0002200);
            // 枠線の色 (ARGB: 上部を明るい緑、下部を中間的な緑に)
            event.setBorderStart(0xFF00FF00);
            event.setBorderEnd(0xFF008800);
        }
    }
}