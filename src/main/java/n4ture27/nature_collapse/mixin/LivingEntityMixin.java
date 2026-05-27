package n4ture27.nature_collapse.mixin;

import java.util.Set;

import net.minecraft.nbt.CompoundTag;
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
import n4ture27.nature_collapse.utils.INaturalizable;

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
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = LivingEntity.class, priority = 10000)
public abstract class LivingEntityMixin extends Entity implements INaturalizable {
    public LivingEntityMixin(net.minecraft.world.entity.EntityType<?> p_19870_, net.minecraft.world.level.Level p_19871_) { super(p_19870_, p_19871_); }

    @Unique
    private boolean isNaturalized = false;

    @Unique
    private boolean nature_collapse$forcedSetHealth = false;

    @Override
    public boolean nature_collapse$isNaturalized() { return this.isNaturalized; }

    @Override
    public void nature_collapse$setNaturalized(boolean value) { 
        this.isNaturalized = value;
    }

    @Override
    public void nature_collapse$setHealthForced(float value) {
        this.nature_collapse$forcedSetHealth = true;
        this.getEntityData().set(LivingEntityAccessor.getDATA_HEALTH_ID(), value);
        this.nature_collapse$forcedSetHealth = false;
    }

    /**
     * @author nature_collapse
     * @reason Protect player health unless naturalized
     */
    @Overwrite
	public float getHealth() {
        var self = (LivingEntity) (Object) this;

        if (self instanceof Player player){
            if (InventoryUtil.hasFlower(player) && !((INaturalizable)player).nature_collapse$isNaturalized()) {
                return player.getMaxHealth();
            }
        }
        return self.getEntityData().get(LivingEntityAccessor.getDATA_HEALTH_ID());
    }
    
    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void onHurt(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        var self = (LivingEntity) (Object) this;
        if (self instanceof Player player && InventoryUtil.hasFlower(player) && !((INaturalizable)player).nature_collapse$isNaturalized()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "die", at = @At("HEAD"), cancellable = true)
    private void onDie(DamageSource source, CallbackInfo ci) {
        var self = (LivingEntity) (Object) this;
        if (self instanceof Player player && InventoryUtil.hasFlower(player) && !((INaturalizable)player).nature_collapse$isNaturalized()) {
            ci.cancel();
        }
    }

    @Inject(method = "setHealth", at = @At("HEAD"), cancellable = true)
    private void onSetHealth(float health, CallbackInfo ci) {
        var self = (LivingEntity) (Object) this;

        if (this.nature_collapse$forcedSetHealth) {
            this.getEntityData().set(LivingEntityAccessor.getDATA_HEALTH_ID(), health);
            ci.cancel();
            return;
        }

        if (this.nature_collapse$isNaturalized()) return;

        if (self instanceof Player player && InventoryUtil.hasFlower(player) && !((INaturalizable)player).nature_collapse$isNaturalized()) {
            if (health < player.getMaxHealth()) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "remove", at = @At("HEAD"), cancellable = true)
    private void onRemove(RemovalReason reason, CallbackInfo ci) {
        var self = (LivingEntity) (Object) this;
        if (self instanceof Player player && InventoryUtil.hasFlower(player) && !((INaturalizable)player).nature_collapse$isNaturalized()) {
            if (reason == RemovalReason.KILLED) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void nature_collapse$addSaveData(CompoundTag nbt, CallbackInfo ci) {
        nbt.putBoolean("nature_collapse.is_naturalized", this.isNaturalized);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("HEAD"))
    private void nature_collapse$readSaveData(CompoundTag nbt, CallbackInfo ci) {
        this.isNaturalized = nbt.getBoolean("nature_collapse.is_naturalized");
    }

    @Inject(method = "readAdditionalSaveData", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;setHealth(F)V"))
    private void nature_collapse$readHealthForced(CompoundTag pCompound, CallbackInfo ci) {
        this.nature_collapse$forcedSetHealth = true;

        this.getEntityData().set(LivingEntityAccessor.getDATA_HEALTH_ID(), pCompound.getFloat("Health"));
        this.nature_collapse$forcedSetHealth = false;
    }
}
