package n4ture27.nature_collapse.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public class MiracleFlowerItem extends Item {
    public MiracleFlowerItem(Properties properties) {
        super(properties.rarity(Rarity.EPIC).fireResistant());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.nature_collapse.miracle_flower.desc1")
                .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.ITALIC));
        tooltip.add(Component.translatable("item.nature_collapse.miracle_flower.desc2")
                .withStyle(ChatFormatting.YELLOW));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
