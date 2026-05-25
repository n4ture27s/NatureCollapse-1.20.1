package n4ture27.nature_collapse;

import java.util.Set;

import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = NatureCollapse.ModId, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class EventHandler {
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onAttackedEvent(LivingAttackEvent ev) {
		if (ev.getEntity() instanceof Player player) {
			if (!player.level().isClientSide()) {
				if (player.getInventory().hasAnyOf(Set.of(NatureCollapse.NatureFlower.get()))) {
					ev.setCanceled(true);
					NatureCollapse.Logger.debug("Attacked");
				}
			}
		}
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onDamageEvent(LivingDamageEvent ev) {
		if (ev.getEntity() instanceof Player player) {
			if (!player.level().isClientSide()) {
				if (player.getInventory().hasAnyOf(Set.of(NatureCollapse.NatureFlower.get()))) {
					ev.setCanceled(true);
					NatureCollapse.Logger.debug("Damage");
				}
			}
		}
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onHurtEvent(LivingHurtEvent ev) {
		if (ev.getEntity() instanceof Player player) {
			if (!player.level().isClientSide()) {
				if (player.getInventory().hasAnyOf(Set.of(NatureCollapse.NatureFlower.get()))) {
					ev.setCanceled(true);
					NatureCollapse.Logger.debug("Hurt");
				}
			}
		}
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onDeathEvent(LivingDeathEvent ev) {
		if (ev.getEntity() instanceof Player player) {
			if (!player.level().isClientSide()) {
				if (player.getInventory().hasAnyOf(Set.of(NatureCollapse.NatureFlower.get()))) {
					ev.setCanceled(true);
					player.setHealth(player.getMaxHealth());
					player.removeAllEffects();
					player.invulnerableTime = 10;
					player.hurtDuration = 20;
					player.hurtTime = 20;
				}
			}
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void onTick(TickEvent.PlayerTickEvent ev) {
		var player = ev.player;
		if (!player.level().isClientSide()) {
			if (player.getInventory().hasAnyOf(Set.of(NatureCollapse.NatureFlower.get()))) {
				player.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 20, 20));
				player.addEffect(new MobEffectInstance(MobEffects.CONDUIT_POWER, 20, 20));
				var food = player.getFoodData();
				if (food.getFoodLevel() < 20 || food.getSaturationLevel() < 20f) {
					food.setFoodLevel(20);
					food.setSaturation(20f);
				}
			}
		}
	}

	// ここでねいちゃーを殺す(サンキューチャッピー)
	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void onEffectAdded(MobEffectEvent.Added ev) {
		LivingEntity entity = ev.getEntity();
		var effect = ev.getEffectInstance();
		if (entity instanceof Player player) {
			if (player.getInventory().hasAnyOf(Set.of(NatureCollapse.NatureFlower.get()))) {
				if (effect.getEffect().getCategory() == MobEffectCategory.HARMFUL) {
					entity.removeEffect(effect.getEffect());
				}
			}
		}
	}
}
