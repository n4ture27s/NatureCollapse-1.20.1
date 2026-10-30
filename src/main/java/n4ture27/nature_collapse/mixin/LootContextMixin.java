package n4ture27.nature_collapse.mixin;

import n4ture27.nature_collapse.utils.CloverUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LootContext.class)
public abstract class LootContextMixin {

    @Inject(method = "getLuck", at = @At("RETURN"), cancellable = true)
    private void nature_collapse$onGetLuck(CallbackInfoReturnable<Float> cir) {
        LootContext self = (LootContext) (Object) this;
        Player player = null;

        // 1. 各種コンテキストパラメータからプレイヤーを特定
        if (self.hasParam(LootContextParams.THIS_ENTITY)) {
            Entity ent = self.getParamOrNull(LootContextParams.THIS_ENTITY);
            if (ent instanceof Player p) {
                player = p;
            }
        }
        if (player == null && self.hasParam(LootContextParams.KILLER_ENTITY)) {
            Entity ent = self.getParamOrNull(LootContextParams.KILLER_ENTITY);
            if (ent instanceof Player p) {
                player = p;
            }
        }
        if (player == null && self.hasParam(LootContextParams.LAST_DAMAGE_PLAYER)) {
            player = self.getParamOrNull(LootContextParams.LAST_DAMAGE_PLAYER);
        }
        if (player == null && self.hasParam(LootContextParams.DIRECT_KILLER_ENTITY)) {
            Entity ent = self.getParamOrNull(LootContextParams.DIRECT_KILLER_ENTITY);
            if (ent instanceof Player p) {
                player = p;
            }
        }

        // 2. 原点付近の最寄りプレイヤーからのフォールバック探索
        if (player == null && self.hasParam(LootContextParams.ORIGIN)) {
            net.minecraft.world.phys.Vec3 origin = self.getParamOrNull(LootContextParams.ORIGIN);
            if (origin != null && self.getLevel() != null) {
                player = self.getLevel().getNearestPlayer(origin.x, origin.y, origin.z, 16.0, false);
            }
        }

        // 3. クローバー所持効果のLuck加算
        if (player != null) {
            float bonus = CloverUtil.getLuckBonus(player);
            if (bonus > 0.0f) {
                cir.setReturnValue(cir.getReturnValue() + bonus);
            }
        }
    }
}
