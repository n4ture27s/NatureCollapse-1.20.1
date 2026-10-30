package n4ture27.nature_collapse.mixin;

import n4ture27.nature_collapse.utils.INaturalizable;
import n4ture27.nature_collapse.utils.InventoryUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ServerPlayer.class, priority = Integer.MAX_VALUE)
public abstract class ServerPlayerMixin {

    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void onHurt(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        Player self = (Player) (Object) this;
        if (InventoryUtil.hasInvulnerableFlower(self)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "die", at = @At("HEAD"), cancellable = true)
    private void onDie(DamageSource source, CallbackInfo ci) {
        Player self = (Player) (Object) this;
        if (InventoryUtil.hasInvulnerableFlower(self)) {
            ((INaturalizable) self).nature_collapse$setHealthForced(self.getMaxHealth());
            self.deathTime = 0;
            ci.cancel();
        }
    }
}
