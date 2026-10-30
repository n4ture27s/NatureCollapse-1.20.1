package n4ture27.nature_collapse.client;

import n4ture27.nature_collapse.NatureCollapse;
import n4ture27.nature_collapse.client.renderer.ArborisBulletRenderer;
import n4ture27.nature_collapse.client.renderer.ArborisMagicCircleRenderer;
import n4ture27.nature_collapse.client.renderer.ArborisRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = NatureCollapse.ModId, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModClientEvents {
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(NatureCollapse.ArborisEntityType.get(), ArborisRenderer::new);
        event.registerEntityRenderer(NatureCollapse.ArborisBulletEntityType.get(), ArborisBulletRenderer::new);
        event.registerEntityRenderer(NatureCollapse.ArborisMagicCircleEntityType.get(), ArborisMagicCircleRenderer::new);
    }
}
