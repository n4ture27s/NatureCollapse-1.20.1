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

public class FourLeafCloverItem extends Item {
    public FourLeafCloverItem(Properties properties) {
        super(properties.rarity(Rarity.RARE));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.nature_collapse.four_leaf_clover.desc1")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        tooltip.add(Component.translatable("item.nature_collapse.four_leaf_clover.desc2")
                .withStyle(ChatFormatting.GREEN));
        tooltip.add(Component.translatable("item.nature_collapse.four_leaf_clover.desc3")
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("item.nature_collapse.four_leaf_clover.desc4")
                .withStyle(ChatFormatting.YELLOW));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
