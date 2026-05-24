package n4ture27.nature_collapse.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;

import java.util.Set;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import n4ture27.nature_collapse.NatureCollapse;

@Mixin(Player.class)
public class PlayerMixin {
	/*@Inject(method = "hurt", at =@At("HEAD"), cancellable = true)
	private void hurt(DamageSource pCause, float pAmount, CallbackInfoReturnable<Boolean> ci) {
		var self = (Player) (Object) this;
		if (self.getInventory().hasAnyOf(Set.of(NatureCollapse.NatureFlower.get()))) {
			self.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100,20));
			self.invulnerableTime = 10;
			self.hurtDuration = 20;
			self.hurtTime = 20;
			ci.setReturnValue(true);
			ci.cancel();
		}
	}*/
	@Inject(method = "die", at = @At("HEAD"), cancellable = true)
    private void onDie(DamageSource source, CallbackInfo ci) {
        var self = (Player) (Object) this;
        if (self.getInventory().hasAnyOf(Set.of(NatureCollapse.NatureFlower.get()))) {
            ci.cancel();
        }
    }
}