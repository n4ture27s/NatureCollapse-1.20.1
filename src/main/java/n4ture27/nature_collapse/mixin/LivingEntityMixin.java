package n4ture27.nature_collapse.mixin;

import java.util.Set;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import n4ture27.nature_collapse.NatureCollapse;
import n4ture27.nature_collapse.utils.InventoryUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity.RemovalReason;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {
    @Shadow @Final
    private static EntityDataAccessor<Float> DATA_HEALTH_ID;

    @Overwrite
	public float getHealth() {
        var self = (LivingEntity) (Object) this;

        if (self instanceof Player player){
            if (InventoryUtil.hasFlower(player)) {
                return player.getMaxHealth();
            }
        }
        return self.getEntityData().get(DATA_HEALTH_ID);
    }
    
    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void onHurt(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        var self = (LivingEntity) (Object) this;
        if (self instanceof Player player && InventoryUtil.hasFlower(player)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "die", at = @At("HEAD"), cancellable = true)
    private void onDie(DamageSource source, CallbackInfo ci) {
        var self = (LivingEntity) (Object) this;
        if (self instanceof Player player && InventoryUtil.hasFlower(player)) {
            ci.cancel();
        }
    }

    @Inject(method = "setHealth", at = @At("HEAD"), cancellable = true)
    private void onSetHealth(float health, CallbackInfo ci) {
        var self = (LivingEntity) (Object) this;
        if (self instanceof Player player && InventoryUtil.hasFlower(player)) {
            // 体力を減らそうとする操作（現在の最大体力未満に設定しようとする場合）をキャンセル
            if (health < player.getMaxHealth()) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "remove", at = @At("HEAD"), cancellable = true)
    private void onRemove(RemovalReason reason, CallbackInfo ci) {
        var self = (LivingEntity) (Object) this;
        if (self instanceof Player player && InventoryUtil.hasFlower(player)) {
            if (reason == RemovalReason.KILLED) {
                ci.cancel();
            }
        }
    }
}
