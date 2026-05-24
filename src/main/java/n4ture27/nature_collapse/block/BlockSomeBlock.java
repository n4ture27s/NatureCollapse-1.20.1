package n4ture27.nature_collapse.block;

import n4ture27.nature_collapse.NatureCollapse;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class BlockSomeBlock extends Block {
  public BlockSomeBlock(BlockBehaviour.Properties properties) {
    super(properties);
  }

  @Override
  public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
    if (pLevel.isClientSide()) super.use(pState, pLevel, pPos, pPlayer, pHand, pHit);

    ItemStack heldItem = pPlayer.getItemInHand(pHand);
    ItemStack duped = heldItem.copy();

    boolean added = pPlayer.addItem(duped);
    if (!added) NatureCollapse.Logger.info("HEYYYYYYY YOUR INVENTORY IS FULL IST ITYOOOOOOOOOOOO");

    NatureCollapse.Logger.info("HEllo??? THis is fro mmmy good use method yoooooooo your item is fucking " + heldItem.getDisplayName());

    return InteractionResult.SUCCESS;
  }
}