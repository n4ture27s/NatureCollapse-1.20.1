package n4ture27.nature_collapse.item;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import n4ture27.nature_collapse.NatureCollapse;
import n4ture27.nature_collapse.utils.NaturalizeUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.network.chat.Component;
import java.util.List;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

public class NaturaLizeSword extends SwordItem {
  public NaturaLizeSword(Item.Properties pProperties) {
    // Tier, 攻撃ダメージ, 攻撃速度, Properties
    super(Tiers.DIAMOND, 9999, -2.4f, pProperties);
  }

  @Override
  public boolean hurtEnemy(ItemStack pStack, LivingEntity pTarget, LivingEntity pAttacker) {
    if (pTarget.level().isClientSide()) return super.hurtEnemy(pStack, pTarget, pAttacker);

    ServerLevel targetLevel = (ServerLevel) pTarget.level();
    MinecraftServer server = targetLevel.getServer();

    if (!pTarget.isAlive()) return super.hurtEnemy(pStack, pTarget, pAttacker);
    NaturalizeUtils.Naturalize(pTarget);
    pTarget.addEffect(new MobEffectInstance(NatureCollapse.ToNaturalEffect.get(), 100/*ticks*/));
    return super.hurtEnemy(pStack, pTarget, pAttacker);
  }

  @Override
  public void appendHoverText(ItemStack pStack, Level level, List<Component> tooltip, TooltipFlag flag) {
    tooltip.add(Component.translatable("item.nature_collapse.naturalize_sword.subtext").withStyle(ChatFormatting.GRAY, ChatFormatting.BOLD, ChatFormatting.ITALIC));
    // Shiftキーが押されている場合のみ詳細説明を表示
    if (Screen.hasShiftDown()) {
      // 緑色の濃淡や色合いが滑らかに変化するアニメーション
      long time = System.currentTimeMillis();
      float hue = (float) (0.325f + 0.05f * Math.sin(time / 600.0)); // 黄緑から深緑の間でわずかに変化
      float brightness = (float) (0.7f + 0.3f * Math.sin(time / 400.0)); // 明るさが脈動する
      int color = java.awt.Color.HSBtoRGB(hue, 0.8f, brightness);

      tooltip.add(Component.translatable("item.nature_collapse.naturalize_sword.desc").withStyle(style -> style.withColor(color)));
    } else {
      // Shiftを押していない時のヒント
      tooltip.add(Component.translatable("tooltip.nature_collapse.hold_shift").withStyle(ChatFormatting.YELLOW));
    }
    
    super.appendHoverText(pStack, level, tooltip, flag);
  }

  @Override
  public boolean onEntitySwing(ItemStack stack, LivingEntity entity) {
    var hit = ProjectileUtil.getEntityHitResult(entity, entity.getEyePosition(), entity.getEyePosition().add(entity.getLookAngle().scale(10)), entity.getBoundingBox().expandTowards(entity.getLookAngle().scale(10)).inflate(1), (entt) -> true, 100);
    if (hit != null) {
      var hitEntity = hit.getEntity();
      NaturalizeUtils.Naturalize(hitEntity);
    }

    return true;
  }

  @Override
  public boolean isDamageable(ItemStack stack) {
      return false;
  }
}