package n4ture27.nature_collapse.item;

import n4ture27.nature_collapse.client.ClientTooltipHandler;
import n4ture27.nature_collapse.utils.InventoryUtil;
import n4ture27.nature_collapse.utils.NaturalizeUtils;
import n4ture27.nature_collapse.utils.RaycastUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.fml.loading.FMLEnvironment;

import javax.annotation.Nullable;
import java.util.List;

public class NaturaLizeSword extends SwordItem {
    public NaturaLizeSword(Item.Properties pProperties) {
        super(Tiers.WOOD,137, -2.4f, pProperties);
    }

    @Override
    public boolean hurtEnemy(ItemStack pStack, LivingEntity pTarget, LivingEntity pAttacker) {
        if (!pTarget.level().isClientSide()) {
            NaturalizeUtils.naturalize(pTarget, pAttacker);
        }
        return super.hurtEnemy(pStack, pTarget, pAttacker);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide()) {
            double range = InventoryUtil.getSwordRange(stack);
            EntityHitResult hit = RaycastUtil.entityRaycast(player, range);
            if (hit != null && hit.getEntity() != null) {
                NaturalizeUtils.naturalize(hit.getEntity(), player);
                return InteractionResultHolder.sidedSuccess(stack, false);
            }
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public void appendHoverText(ItemStack pStack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        double currentRange = InventoryUtil.getSwordRange(pStack);

        tooltip.add(Component.translatable("item.nature_collapse.naturalize_sword.subtext")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.BOLD, ChatFormatting.ITALIC));

        if (FMLEnvironment.dist.isClient() && ClientTooltipHandler.hasShiftDown()) {
            int color = getPulsingNatureColor();
            tooltip.add(Component.translatable("item.nature_collapse.naturalize_sword.desc")
                    .withStyle(style -> style.withColor(color)));
            tooltip.add(Component.literal("Range: " + currentRange).withStyle(ChatFormatting.AQUA));
        } else {
            tooltip.add(Component.translatable("tooltip.nature_collapse.hold_shift").withStyle(ChatFormatting.YELLOW));
            tooltip.add(Component.literal("Press [" + getKeyDisplay() + "] to adjust range").withStyle(ChatFormatting.GOLD));
        }

        super.appendHoverText(pStack, level, tooltip, flag);
    }

    @Override
    public boolean onEntitySwing(ItemStack stack, LivingEntity entity) {
        if (!entity.level().isClientSide() && entity instanceof Player player) {
            double range = InventoryUtil.getSwordRange(stack);
            EntityHitResult hit = RaycastUtil.entityRaycast(player, range);
            if (hit != null && hit.getEntity() != null) {
                NaturalizeUtils.naturalize(hit.getEntity(), player);
            }
        }
        return false;
    }

    @Override
    public boolean isDamageable(ItemStack stack) {
        return false;
    }

    public static int getPulsingNatureColor() {
        long time = System.currentTimeMillis();
        float hue = (float) (0.325f + 0.05f * Math.sin(time / 600.0));
        float brightness = (float) (0.7f + 0.3f * Math.sin(time / 400.0));
        return Mth.hsvToRgb(hue, 0.8f, brightness);
    }

    public static String getKeyDisplay() {
        if (FMLEnvironment.dist.isClient()) {
            return ClientTooltipHandler.getKeyBindingDisplayName();
        }
        return "K";
    }
}
