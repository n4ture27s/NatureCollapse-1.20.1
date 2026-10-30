package n4ture27.nature_collapse.event;

import n4ture27.nature_collapse.NatureCollapse;
import n4ture27.nature_collapse.entity.ArborisEntity;
import n4ture27.nature_collapse.utils.NaturalizeUtils;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 原初の根源樹アルボリスに対する全ダメージ・攻撃イベントの包括的インターセプトハンドラ。
 * 外部Mod等による極大・無限大ダメージ攻撃に対し、
 * ハートストップ（10,000ダメージ上限）および被弾後無敵時間（2秒間のダメージ反射）を最優先で厳格に強制する。
 */
@Mod.EventBusSubscriber(modid = NatureCollapse.ModId, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ArborisDamageHandler {

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingAttack(LivingAttackEvent event) {
        if (event.getEntity() instanceof ArborisEntity arboris) {
            // 出現演出中は完全無敵
            if (arboris.isAwakening()) {
                event.setCanceled(true);
                return;
            }
            // 無敵時間中は即座に攻撃をキャンセルし、ダメージ反射を実施
            if (arboris.getInvulnerableTicks() > 0) {
                event.setCanceled(true);
                arboris.reflectDamage(event.getSource(), 0.0f);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof ArborisEntity arboris) {
            if (arboris.isAwakening()) {
                event.setCanceled(true);
                return;
            }
            // 無敵時間中はダメージ反射
            if (arboris.getInvulnerableTicks() > 0) {
                event.setCanceled(true);
                arboris.reflectDamage(event.getSource(), event.getAmount());
                return;
            }
            // ハートストップ: 10,000を超えるダメージ、NaN、無限大ダメージを10,000にクランプ
            float amount = event.getAmount();
            if (amount > ArborisEntity.HEART_STOP_DAMAGE || Float.isNaN(amount) || Float.isInfinite(amount)) {
                event.setAmount(ArborisEntity.HEART_STOP_DAMAGE);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingDamage(LivingDamageEvent event) {
        if (event.getEntity() instanceof ArborisEntity arboris) {
            if (arboris.isAwakening()) {
                event.setCanceled(true);
                return;
            }
            if (arboris.getInvulnerableTicks() > 0) {
                event.setCanceled(true);
                return;
            }
            float amount = event.getAmount();
            if (amount > ArborisEntity.HEART_STOP_DAMAGE || Float.isNaN(amount) || Float.isInfinite(amount)) {
                event.setAmount(ArborisEntity.HEART_STOP_DAMAGE);
            }
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            NaturalizeUtils.tickPursuits(event.getServer());
        }
    }
}
