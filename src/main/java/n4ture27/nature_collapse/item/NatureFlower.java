package n4ture27.nature_collapse.item;

import java.util.List;

import n4ture27.nature_collapse.NatureCollapse;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

public class NatureFlower extends Item {
  public NatureFlower(Item.Properties pProperties) {
    super(pProperties);
  }

  @Override
  public void appendHoverText(ItemStack pStack, Level level, List<Component> tooltip, TooltipFlag flag) {
    tooltip.add(Component.translatable("item.nature_collapse.nature_flower.subtext").withStyle(ChatFormatting.GRAY,
        ChatFormatting.BOLD, ChatFormatting.ITALIC));

    // Shiftキーが押されている場合のみ詳細説明を表示
    if (Screen.hasShiftDown()) {
      long time = System.currentTimeMillis();
      float hue = (float) (0.325f + 0.05f * Math.sin(time / 600.0)); // 黄緑から深緑の間でわずかに変化
      float brightness = (float) (0.7f + 0.3f * Math.sin(time / 400.0)); // 明るさが脈動する
      int color = java.awt.Color.HSBtoRGB(hue, 0.8f, brightness);

      tooltip.add(Component.translatable("item.nature_collapse.nature_flower.desc").withStyle(style -> style.withColor(color)));

      CompoundTag tag = pStack.getTag();
      if (tag != null && tag.contains("SelectedEffects")) {
        ListTag list = tag.getList("SelectedEffects", Tag.TAG_COMPOUND);
        if (!list.isEmpty()) {
          tooltip.add(Component.literal("Active Effects:").withStyle(ChatFormatting.DARK_GREEN));
          for (int i = 0; i < list.size(); i++) {
            CompoundTag effectData = list.getCompound(i);
            MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation(effectData.getString("Id")));
            if (effect != null) {
              int effectLevel = effectData.getInt("Level");
              tooltip.add(Component.literal(" - ").append(effect.getDisplayName()).append(" Lv." + effectLevel)
                  .withStyle(ChatFormatting.AQUA));
            }
          }
        }
      }

    } else {
      // Shiftを押していない時のヒント
      tooltip.add(Component.translatable("tooltip.nature_collapse.hold_shift").withStyle(ChatFormatting.YELLOW));
      tooltip.add(Component.literal("Press [K] to set effects").withStyle(ChatFormatting.GOLD));
    }

    super.appendHoverText(pStack, level, tooltip, flag);
  }
}