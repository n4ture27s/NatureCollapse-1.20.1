package n4ture27.nature_collapse.item;

import n4ture27.nature_collapse.client.ClientTooltipHandler;
import n4ture27.nature_collapse.utils.InventoryUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.List;

public class NatureFlower extends Item {
    public NatureFlower(Item.Properties pProperties) {
        super(pProperties);
    }

    @Override
    public void appendHoverText(ItemStack pStack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.nature_collapse.nature_flower.subtext")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.BOLD, ChatFormatting.ITALIC));

        if (FMLEnvironment.dist.isClient() && ClientTooltipHandler.hasShiftDown()) {
            int color = NaturaLizeSword.getPulsingNatureColor();
            tooltip.add(Component.translatable("item.nature_collapse.nature_flower.desc")
                    .withStyle(style -> style.withColor(color)));

            CompoundTag tag = pStack.getTag();
            if (tag != null && tag.contains(InventoryUtil.TAG_SELECTED_EFFECTS, Tag.TAG_LIST)) {
                ListTag list = tag.getList(InventoryUtil.TAG_SELECTED_EFFECTS, Tag.TAG_COMPOUND);
                if (!list.isEmpty()) {
                    tooltip.add(Component.literal("Active Effects:").withStyle(ChatFormatting.DARK_GREEN));
                    for (int i = 0; i < list.size(); i++) {
                        CompoundTag effectData = list.getCompound(i);
                        ResourceLocation effectLocation = ResourceLocation.tryParse(effectData.getString(InventoryUtil.TAG_EFFECT_ID));
                        MobEffect effect = effectLocation != null ? ForgeRegistries.MOB_EFFECTS.getValue(effectLocation) : null;
                        if (effect != null) {
                            int effectLevel = Math.max(1, effectData.getInt(InventoryUtil.TAG_EFFECT_LEVEL));
                            int durationTicks = effectData.contains(InventoryUtil.TAG_EFFECT_DURATION, Tag.TAG_ANY_NUMERIC)
                                    ? effectData.getInt(InventoryUtil.TAG_EFFECT_DURATION)
                                    : InventoryUtil.DEFAULT_EFFECT_DURATION;
                            tooltip.add(Component.literal(" - ")
                                    .append(effect.getDisplayName())
                                    .append(String.format(" Lv.%d (%ds)", effectLevel, durationTicks / 20))
                                    .withStyle(ChatFormatting.AQUA));
                        }
                    }
                }
            }
        } else {
            tooltip.add(Component.translatable("tooltip.nature_collapse.hold_shift").withStyle(ChatFormatting.YELLOW));
            tooltip.add(Component.literal("Press [" + NaturaLizeSword.getKeyDisplay() + "] to open Flower Hub").withStyle(ChatFormatting.GOLD));
        }

        super.appendHoverText(pStack, level, tooltip, flag);
    }
}
