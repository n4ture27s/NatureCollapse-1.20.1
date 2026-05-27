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

import n4ture27.nature_collapse.utils.INaturalizable;
import n4ture27.nature_collapse.utils.InventoryUtil;

import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import n4ture27.nature_collapse.NatureCollapse;

@Mixin(value = ServerPlayer.class, priority = 10000)
public abstract class ServerPlayerMixin {
	@Inject(method = "die", at = @At("HEAD"), cancellable = true)
    private void onDie(DamageSource source, CallbackInfo ci) {
        var self = (Player) (Object) this;
        if (InventoryUtil.hasFlower(self) && !((INaturalizable)self).nature_collapse$isNaturalized()) {
            ci.cancel();
        }
    }
}