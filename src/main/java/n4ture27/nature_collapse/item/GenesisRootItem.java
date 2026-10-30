package n4ture27.nature_collapse.item;

import n4ture27.nature_collapse.NatureCollapse;
import n4ture27.nature_collapse.entity.ArborisEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import javax.annotation.Nullable;
import java.util.List;

public class GenesisRootItem extends Item {
    public GenesisRootItem(Properties properties) {
        super(properties.rarity(Rarity.EPIC).fireResistant());
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos clickedPos = context.getClickedPos();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();

        // 1. 現世（Overworld）判定
        if (level.dimension() != Level.OVERWORLD) {
            if (level.isClientSide() && player != null) {
                player.sendSystemMessage(Component.translatable("message.nature_collapse.genesis_root.overworld_only")
                        .withStyle(ChatFormatting.RED));
            }
            return InteractionResult.FAIL;
        }

        // 2. 岩盤判定 (BEDROCK ブロック または 最下層 Y <= -55)
        boolean isBedrock = level.getBlockState(clickedPos).is(Blocks.BEDROCK) || clickedPos.getY() <= -55;
        if (!isBedrock) {
            if (level.isClientSide() && player != null) {
                player.sendSystemMessage(Component.translatable("message.nature_collapse.genesis_root.bedrock_only")
                        .withStyle(ChatFormatting.RED));
            }
            return InteractionResult.FAIL;
        }

        // 3. 召喚シークエンス開始
        if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
            ArborisEntity arboris = NatureCollapse.ArborisEntityType.get().create(serverLevel);
            if (arboris != null) {
                arboris.startAwakening(clickedPos);
                serverLevel.addFreshEntity(arboris);

                // 全体アナウンス
                for (Player p : serverLevel.players()) {
                    p.sendSystemMessage(Component.translatable("message.nature_collapse.genesis_root.awakening_started")
                            .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD));
                }

                // 召喚雷鳴
                serverLevel.playSound(null, clickedPos, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 5.0f, 0.5f);
                serverLevel.playSound(null, clickedPos, SoundEvents.WARDEN_EMERGE, SoundSource.HOSTILE, 5.0f, 0.4f);

                if (player != null && !player.isCreative()) {
                    stack.shrink(1);
                }
                return InteractionResult.SUCCESS;
            }
        }

        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.nature_collapse.genesis_root.desc1")
                .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.ITALIC));
        tooltip.add(Component.translatable("item.nature_collapse.genesis_root.desc2")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.nature_collapse.genesis_root.desc3")
                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
