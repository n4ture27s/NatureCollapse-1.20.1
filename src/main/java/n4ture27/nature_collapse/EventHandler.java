package n4ture27.nature_collapse;

import java.util.Set;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.mojang.blaze3d.platform.InputConstants;

import n4ture27.nature_collapse.item.NaturaLizeSword;
import n4ture27.nature_collapse.utils.InventoryUtil;
import n4ture27.nature_collapse.utils.INaturalizable;
import n4ture27.nature_collapse.item.NatureFlower;
import n4ture27.nature_collapse.ui.RangeAdjustmentScreen;
import n4ture27.nature_collapse.ui.FlowerEffectSettingsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.ScreenEvent;
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
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.common.inventory.CurioSlot;

@Mod.EventBusSubscriber(modid = NatureCollapse.ModId, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class EventHandler {
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onAttackedEvent(LivingAttackEvent ev) {
		if (ev.getEntity() instanceof Player player) {
			if (!player.level().isClientSide()) {
				if (InventoryUtil.hasFlower(player)) {
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
				if (InventoryUtil.hasFlower(player)) {
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
				if (InventoryUtil.hasFlower(player)) {
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
				if (InventoryUtil.hasFlower(player)) {
					ev.setCanceled(true);
					player.setHealth(player.getMaxHealth());
					player.removeAllEffects();
					player.invulnerableTime = 10;
					player.hurtDuration = 20;
					player.hurtTime = 20;
				}
                // If the player actually dies (not cancelled by flower protection), reset naturalized state
                if (!ev.isCanceled() && ((INaturalizable) player).nature_collapse$isNaturalized()) {
                    ((INaturalizable) player).nature_collapse$setNaturalized(false);
                }
			}
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void onTick(TickEvent.PlayerTickEvent ev) {
		var player = ev.player;
		if (ev.phase == TickEvent.Phase.START && !player.level().isClientSide()) {
			InventoryUtil.forEachFlower(player, stack -> applyFlowerEffects(player, stack));

			if (InventoryUtil.hasFlower(player)) {
				var food = player.getFoodData();
				if (food.getFoodLevel() < 20 || food.getSaturationLevel() < 20.0f) {
					food.setFoodLevel(20);
					food.setSaturation(20f);
				}

				// 飛行の初期設定
				if (!player.getAbilities().mayfly && !((INaturalizable) player).nature_collapse$isNaturalized()) {
					player.getAbilities().mayfly = true;
					player.onUpdateAbilities();
				}
			}
		}
	}

	// 他のModによる飛行無効化への対策（ティックの最後で再強制）
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onFlightEnforcement(TickEvent.PlayerTickEvent ev) {
		Player player = ev.player;
		if (ev.phase == TickEvent.Phase.END && !player.level().isClientSide()) {
			boolean hasFlower = InventoryUtil.hasFlower(player) && !((INaturalizable) player).nature_collapse$isNaturalized();
			
			if (hasFlower) {
				if (!player.getAbilities().mayfly) {
					player.getAbilities().mayfly = true;
					player.onUpdateAbilities();
				}
			} else if (!player.isCreative() && !player.isSpectator() && player.getAbilities().mayfly) {
				// 花を持っていない、かつサバイバル/アドベンチャーなら飛行を解除
				player.getAbilities().mayfly = false;
				player.getAbilities().flying = false;
				player.onUpdateAbilities();
			}
		}
	}

	private static void applyFlowerEffects(Player player, ItemStack flower) {
		CompoundTag tag = flower.getTag();
		if (tag != null && tag.contains("SelectedEffects")) {
			ListTag list = tag.getList("SelectedEffects", Tag.TAG_COMPOUND);
			for (int i = 0; i < list.size(); i++) {
				CompoundTag effectData = list.getCompound(i);
				MobEffect effect = ForgeRegistries.MOB_EFFECTS
						.getValue(ResourceLocation.tryParse(effectData.getString("Id")));
				if (effect != null) {
					int amplifier = Math.max(0, effectData.getInt("Level") - 1);
					player.addEffect(new MobEffectInstance(effect, 40, amplifier, false, false));
				}
			}
		}
	}

	// ここでねいちゃーを殺す(サンキューチャッピー)
	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void onEffectApplicable(MobEffectEvent.Applicable ev) {
		LivingEntity entity = ev.getEntity();
		var effect = ev.getEffectInstance();
		if (entity instanceof Player player) {
			if (InventoryUtil.hasFlower(player) && !((INaturalizable) player).nature_collapse$isNaturalized()) {
				ResourceLocation effectId = ForgeRegistries.MOB_EFFECTS.getKey(effect.getEffect());
				if (effectId != null) {
					String namespace = effectId.getNamespace();
					if (!namespace.equals("minecraft") && !namespace.equals(NatureCollapse.ModId)) {
						// 有害な効果（HARMFUL）かつ、ハナで許可されていない場合のみ拒否する
						// これにより、他のCuriosアイテムによるバフ（Speed等）が機能するようになります
						if (effect.getEffect().getCategory() == MobEffectCategory.HARMFUL && !isEffectAllowedByFlower(player, effectId)) {
							ev.setResult(net.minecraftforge.eventbus.api.Event.Result.DENY);
						}
					}
				}
			}
		}
	}

	private static boolean isEffectAllowedByFlower(Player player, ResourceLocation effectId) {
		List<Boolean> found = new ArrayList<>();
		InventoryUtil.forEachFlower(player, stack -> {
			if (stack.hasTag()) {
				ListTag list = stack.getTag().getList("SelectedEffects", Tag.TAG_COMPOUND);
				for (int j = 0; j < list.size(); j++) {
					if (list.getCompound(j).getString("Id").equals(effectId.toString()))
						found.add(true);
				}
			}
		});
		return !found.isEmpty();
	}

	@SubscribeEvent
	public static void onScreenKey(ScreenEvent.KeyPressed.Pre ev) {
		if (ev.getScreen() instanceof AbstractContainerScreen<?> container) {
			if (NatureCollapse.ClientModEvents.OPEN_RANGE_GUI
					.isActiveAndMatches(InputConstants.getKey(ev.getKeyCode(), ev.getScanCode()))) {
				Slot slot = container.getSlotUnderMouse();
				if (slot != null && !slot.getItem().isEmpty()) {
					boolean isCurio = false;
					String identifier = "";
					int slotIndex = slot.getContainerSlot();

					// Curiosスロットの判定をAPI経由でより確実に実行
					if (ModList.get().isLoaded("curios")) {
						if (slot instanceof CurioSlot curiosSlot) {
							isCurio = true;
							identifier = curiosSlot.getIdentifier();
							slotIndex = curiosSlot.getSlotIndex();
						}
					}

					// アイテムの型をチェックしてGUIを表示
					ItemStack stack = slot.getItem();
					if (stack.getItem() instanceof NaturaLizeSword) {
						Minecraft.getInstance().setScreen(new RangeAdjustmentScreen(slot.getItem(),
								slotIndex, isCurio, identifier));
						ev.setCanceled(true);
					} else if (stack.getItem() instanceof NatureFlower) {
						Minecraft.getInstance().setScreen(new FlowerEffectSettingsScreen(slot.getItem(),
								slotIndex, isCurio, identifier)); // Updated constructor call
						ev.setCanceled(true);
					}
				}
			}
		}
	}
}
