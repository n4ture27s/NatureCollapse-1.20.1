package n4ture27.nature_collapse.mixin;

import n4ture27.nature_collapse.NatureCollapse;
import n4ture27.nature_collapse.utils.CloverUtil;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.function.Consumer;

@Mixin(LootTable.class)
public abstract class LootTableMixin {

    @ModifyVariable(
            method = "getRandomItemsRaw(Lnet/minecraft/world/level/storage/loot/LootContext;Ljava/util/function/Consumer;)V",
            at = @At("HEAD"),
            argsOnly = true
    )
    private Consumer<ItemStack> nature_collapse$modifyLootOutput(Consumer<ItemStack> output, LootContext context) {
        Player player = null;

        if (context.hasParam(LootContextParams.THIS_ENTITY)) {
            Entity ent = context.getParamOrNull(LootContextParams.THIS_ENTITY);
            if (ent instanceof Player p) {
                player = p;
            }
        }
        if (player == null && context.hasParam(LootContextParams.KILLER_ENTITY)) {
            Entity ent = context.getParamOrNull(LootContextParams.KILLER_ENTITY);
            if (ent instanceof Player p) {
                player = p;
            }
        }
        if (player == null && context.hasParam(LootContextParams.LAST_DAMAGE_PLAYER)) {
            player = context.getParamOrNull(LootContextParams.LAST_DAMAGE_PLAYER);
        }
        if (player == null && context.hasParam(LootContextParams.DIRECT_KILLER_ENTITY)) {
            Entity ent = context.getParamOrNull(LootContextParams.DIRECT_KILLER_ENTITY);
            if (ent instanceof Player p) {
                player = p;
            }
        }
        if (player == null && context.hasParam(LootContextParams.ORIGIN) && context.getLevel() != null) {
            net.minecraft.world.phys.Vec3 origin = context.getParamOrNull(LootContextParams.ORIGIN);
            if (origin != null) {
                player = context.getLevel().getNearestPlayer(origin.x, origin.y, origin.z, 16.0, false);
            }
        }

        // 作物収穫時のクローバードロップ判定 (0.077%)
        if (context.hasParam(LootContextParams.BLOCK_STATE)) {
            BlockState state = context.getParamOrNull(LootContextParams.BLOCK_STATE);
            if (state != null && isCrop(state)) {
                if (context.getRandom().nextFloat() < 0.00077f) {
                    output.accept(new ItemStack(NatureCollapse.Clover.get()));
                }
            }
        }

        final int countMultiplier = player != null ? CloverUtil.getCountMultiplier(player) : 1;

        // 四つ葉所持時: 出力ItemStackの数量を2倍にする
        if (countMultiplier > 1) {
            return stack -> {
                if (!stack.isEmpty()) {
                    stack.setCount(stack.getCount() * countMultiplier);
                    output.accept(stack);
                }
            };
        }

        return output;
    }

    private static boolean isCrop(BlockState state) {
        Block block = state.getBlock();
        if (block instanceof CropBlock crop) {
            return crop.isMaxAge(state);
        }
        if (state.is(BlockTags.CROPS)) {
            return true;
        }
        return false;
    }
}
