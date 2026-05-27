package n4ture27.nature_collapse.mixin;

import java.util.Set;

import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Overwrite;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatKillPacket;
import net.minecraft.stats.Stats;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import n4ture27.nature_collapse.NatureCollapse;
import n4ture27.nature_collapse.utils.InventoryUtil;
import n4ture27.nature_collapse.utils.INaturalizable;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.Entity.RemovalReason;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = LivingEntity.class, priority = 10000)
public abstract class LivingEntityMixin extends Entity implements INaturalizable {
    public LivingEntityMixin(net.minecraft.world.entity.EntityType<?> p_19870_, net.minecraft.world.level.Level p_19871_) { super(p_19870_, p_19871_); }

    @Shadow @Final
    private static EntityDataAccessor<Float> DATA_HEALTH_ID;

    @Unique
    private boolean isNaturalized = false;

    @Unique
    private boolean nature_collapse$forcedSetHealth = false;

    @Override
    public boolean nature_collapse$isNaturalized() { return this.isNaturalized; }

    @Override
    public void nature_collapse$setNaturalized(boolean value) { 
        // 花を持っているプレイヤーは脆弱化(Naturalize)状態にさせない
        if (value && (Object)this instanceof Player player && InventoryUtil.hasFlower(player)) {
            return;
        }
        this.isNaturalized = value;
    }

    @Override
    public void nature_collapse$setHealthForced(float value) {
        this.nature_collapse$forcedSetHealth = true;
        this.getEntityData().set(DATA_HEALTH_ID, value);
        this.nature_collapse$forcedSetHealth = false;
    }

    @Override
    public void nature_collapse$instantKill(LivingEntity attacker, DamageSource src) {
        LivingEntity self = (LivingEntity) (Object) this;

        // Attribute the kill correctly for XP and loot
        self.getCombatTracker().recordDamage(src, Float.POSITIVE_INFINITY);
        if (attacker instanceof Player player) {
            self.setLastHurtByPlayer(player);
        } else if (attacker != null) {
            self.setLastHurtByMob(attacker);
        }

        // Visual state change
        self.setPose(Pose.DYING);

        // Force health to 0 and trigger Naturalize state
        this.nature_collapse$setHealthForced(0.0f);
        this.nature_collapse$setNaturalized(true);
        this.nature_collapse$forcedie(src);
    }

    @Unique
    private void nature_collapse$forcedie(DamageSource source) {
        LivingEntity self = (LivingEntity) (Object) this;
        ((LivingEntityAccessor) self).setDead(true);
        self.deathTime = 1;

        if (!self.level().isClientSide) {
            if (self instanceof ServerPlayer sp) {
                Component deathMsg = sp.getCombatTracker().getDeathMessage();
                // Ensure the combat packet is sent for proper UI feedback
                if (self.isDeadOrDying() && !self.isAlive() && self.getHealth() <= 0.f) {
                    sp.connection.send(new ClientboundPlayerCombatKillPacket(sp.getId(), deathMsg));
                }
                sp.die(source);
            }

            LivingEntity killer = self.getKillCredit();
            if (killer != null) {
                if (self instanceof ServerPlayer player)
                    player.awardStat(Stats.ENTITY_KILLED_BY.get(killer.getType()));
                killer.awardKillScore(self, 0, source);
            }
            if (!self.isAlive() && self.isDeadOrDying()) {
                self.setPose(Pose.DYING);
                ((LivingEntityAccessor) self).invokeDropAllDeathLoot(source);
            }
        }
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
        return self.getEntityData().get(DATA_HEALTH_ID);
    }
    
    @Inject(method = "tick", at = @At("HEAD"))
    private void nature_collapse$tickFlowerProtection(CallbackInfo ci) {
        var self = (LivingEntity) (Object) this;
        if (self instanceof Player player && InventoryUtil.hasFlower(player) && !((INaturalizable)player).nature_collapse$isNaturalized()) {
            if (player.deathTime > 0) player.deathTime = 0;
        }
    }

    @Inject(method = "handleEntityEvent", at = @At("HEAD"), cancellable = true)
    private void nature_collapse$handleEntityEvent(byte id, CallbackInfo ci) {
        if (id == 3 && this.nature_collapse$isNaturalized()) {
            ci.cancel();
        }
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
            // 死亡処理をキャンセルし、体力を即座に全快させて「動く死体」化を防ぐ
            this.nature_collapse$setHealthForced(player.getMaxHealth());
            ci.cancel();
        }
    }

    @Inject(method = "setHealth", at = @At("HEAD"), cancellable = true)
    private void onSetHealth(float health, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;

        if (this.nature_collapse$forcedSetHealth) {
            this.getEntityData().set(DATA_HEALTH_ID, health);
            ci.cancel();
            return;
        }

        if (this.nature_collapse$isNaturalized()) return;

        if (self instanceof Player player && InventoryUtil.hasFlower(player) && !((INaturalizable)player).nature_collapse$isNaturalized()) {
            // 0以下（死亡）になる設定のみをブロックするように緩和。
            // これにより他のModによる回復や体力増減の「更新」が通るようになります。
            if (health <= 0) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "remove", at = @At("HEAD"), cancellable = true)
    private void onRemove(RemovalReason reason, CallbackInfo ci) {
        var self = (LivingEntity) (Object) this;
        if (self instanceof Player player && InventoryUtil.hasFlower(player) && !((INaturalizable)player).nature_collapse$isNaturalized()) {
            if (reason == RemovalReason.KILLED) {
                // 消去処理をキャンセルし、体力を保持する
                this.nature_collapse$setHealthForced(player.getMaxHealth());
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

        this.getEntityData().set(DATA_HEALTH_ID, pCompound.getFloat("Health"));
        this.nature_collapse$forcedSetHealth = false;
    }
}
