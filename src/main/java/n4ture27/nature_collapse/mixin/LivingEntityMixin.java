package n4ture27.nature_collapse.mixin;

import n4ture27.nature_collapse.utils.INaturalizable;
import n4ture27.nature_collapse.utils.InventoryUtil;
import n4ture27.nature_collapse.utils.NaturalizeUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatKillPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = LivingEntity.class, priority = Integer.MAX_VALUE)
public abstract class LivingEntityMixin extends Entity implements INaturalizable {
    public LivingEntityMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Shadow
    @Final
    private static EntityDataAccessor<Float> DATA_HEALTH_ID;

    @Unique
    private boolean isNaturalized = false;

    @Unique
    private boolean nature_collapse$forcedSetHealth = false;

    @Override
    public boolean nature_collapse$isNaturalized() {
        return this.isNaturalized;
    }

    @Override
    public void nature_collapse$setNaturalized(boolean value) {
        // 花を持っているプレイヤーは脆弱化(Naturalize)状態にさせない
        if (value && (Object) this instanceof Player player && InventoryUtil.hasFlower(player)) {
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

        // Force health to 0 and trigger Naturalize state
        this.nature_collapse$setHealthForced(0.0f);
        this.nature_collapse$setNaturalized(true);

        if (!self.level().isClientSide) {
            LivingEntity killer = self.getKillCredit();
            if (killer != null) {
                if (self instanceof ServerPlayer player) {
                    player.awardStat(Stats.ENTITY_KILLED_BY.get(killer.getType()));
                }
                killer.awardKillScore(self, 0, src);
            }

            if (self instanceof ServerPlayer sp) {
                Component deathMsg = sp.getCombatTracker().getDeathMessage();
                sp.connection.send(new ClientboundPlayerCombatKillPacket(sp.getId(), deathMsg));
                sp.awardStat(Stats.DEATHS);
                sp.level().broadcastEntityEvent(sp, (byte)3);
            }

            // 正規の die(src) を呼び出し（ForgeHooks.onLivingDeath, dropAllDeathLoot, 進捗等のトリガー）
            try {
                self.die(src);
            } catch (Throwable ignored) {
            }

            // 万が一 die() 内で dropAllDeathLoot が呼ばれなかった場合へのフェイルセーフ
            try {
                ((LivingEntityAccessor) self).invokeDropAllDeathLoot(src);
            } catch (Throwable ignored) {
            }

            ((LivingEntityAccessor) self).setDead(true);
            self.setPose(Pose.DYING);
            self.deathTime = 1;
        }
    }

    @Inject(method = "isAlive", at = @At("HEAD"), cancellable = true)
    private void nature_collapse$onIsAlive(CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (this.nature_collapse$isNaturalized() || (NaturalizeUtils.isNaturalizedUUID(self.getUUID()) && !(self instanceof Player))) {
            cir.setReturnValue(false);
            return;
        }
        if ((Object) this instanceof Player player && InventoryUtil.hasInvulnerableFlower(player)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "isDeadOrDying", at = @At("HEAD"), cancellable = true)
    private void nature_collapse$onIsDeadOrDying(CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (this.nature_collapse$isNaturalized() || (NaturalizeUtils.isNaturalizedUUID(self.getUUID()) && !(self instanceof Player))) {
            cir.setReturnValue(true);
            return;
        }
        if ((Object) this instanceof Player player && InventoryUtil.hasInvulnerableFlower(player)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "getHealth", at = @At("HEAD"), cancellable = true)
    private void nature_collapse$onGetHealth(CallbackInfoReturnable<Float> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (this.nature_collapse$isNaturalized() || (NaturalizeUtils.isNaturalizedUUID(self.getUUID()) && !(self instanceof Player))) {
            cir.setReturnValue(0.0f);
            return;
        }
        if (self instanceof Player player && InventoryUtil.hasInvulnerableFlower(player)) {
            cir.setReturnValue(player.getMaxHealth());
        }
    }

    @Inject(method = "tickDeath", at = @At("HEAD"), cancellable = true)
    private void onTickDeath(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof Player player && InventoryUtil.hasInvulnerableFlower(player)) {
            player.deathTime = 0;
            ci.cancel();
        }
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void nature_collapse$onTick(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;

        if (self instanceof Player player && InventoryUtil.hasInvulnerableFlower(player)) {
            if (player.deathTime > 0) {
                player.deathTime = 0;
            }
            if (player.getHealth() < player.getMaxHealth()) {
                this.nature_collapse$setHealthForced(player.getMaxHealth());
            }
            player.setInvulnerable(true);
            player.invulnerableTime = 20;
            return;
        }

        // 帰化状態のプレイヤー: リスポーン完了まで死亡状態を強制維持
        if (self instanceof ServerPlayer sp && this.nature_collapse$isNaturalized()) {
            sp.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
            sp.setPose(Pose.DYING);
            if (sp.deathTime <= 0) {
                sp.deathTime = 1;
            }
            this.nature_collapse$setHealthForced(0.0f);
            ci.cancel();
            return;
        }

        // プレイヤー以外の帰化（脆弱化）状態エンティティは、AI・行動を即座に停止し物理消去
        if (!(self instanceof Player) && (this.nature_collapse$isNaturalized() || NaturalizeUtils.isNaturalizedUUID(self.getUUID()))) {
            if (!self.isRemoved()) {
                if (!self.level().isClientSide() || self.level().shouldTickDeath(self)) {
                    int time = ++self.deathTime;

                    if (!self.level().isClientSide() && time >= 20) {
                        self.level().broadcastEntityEvent(self, EntityEvent.POOF);
                        NaturalizeUtils.terminateAttachedControllerAndHazards(self);
                        NaturalizeUtils.purgeEntityFromWorld(self);
                        ci.cancel();
                        return;
                    }

                    if (!self.level().isClientSide()) {
                        self.checkDespawn();
                    }

                    self.setOldPosAndRot();
                    self.yHeadRotO = self.getYHeadRot();
                    self.yBodyRotO = self.yBodyRot;
                    self.walkAnimation.setSpeed(0.0f);
                    self.setSwimming(false);
                    self.noPhysics = true;
                    self.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
                    self.deathTime = time;
                }
            } else {
                self.setOldPosAndRot();
            }
            ci.cancel();
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
        LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof Player player && InventoryUtil.hasInvulnerableFlower(player) && !this.nature_collapse$isNaturalized()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "actuallyHurt", at = @At("HEAD"), cancellable = true)
    private void onActuallyHurt(DamageSource source, float amount, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof Player player && InventoryUtil.hasInvulnerableFlower(player) && !this.nature_collapse$isNaturalized()) {
            ci.cancel();
        }
    }

    @Inject(method = "die", at = @At("HEAD"), cancellable = true)
    private void onDie(DamageSource source, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof Player player && InventoryUtil.hasInvulnerableFlower(player) && !this.nature_collapse$isNaturalized()) {
            this.nature_collapse$setHealthForced(player.getMaxHealth());
            player.deathTime = 0;
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

        if (this.nature_collapse$isNaturalized() || (NaturalizeUtils.isNaturalizedUUID(self.getUUID()) && !(self instanceof Player))) {
            // 帰化状態の時は、他MODによる回復や体力固定（20HP維持等）をすべて拒絶
            if (health > 0.0f) {
                ci.cancel();
            }
            return;
        }

        if (self instanceof Player player && InventoryUtil.hasInvulnerableFlower(player)) {
            if (health < player.getMaxHealth()) {
                this.getEntityData().set(DATA_HEALTH_ID, player.getMaxHealth());
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

    @Inject(method = "aiStep", at = @At("HEAD"), cancellable = true)
    private void nature_collapse$onAiStep(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if ((this.nature_collapse$isNaturalized() || NaturalizeUtils.isNaturalizedUUID(self.getUUID())) && !(self instanceof Player)) {
            ci.cancel();
        }
    }

    @Inject(method = "baseTick", at = @At("HEAD"), cancellable = true)
    private void nature_collapse$onBaseTick(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if ((this.nature_collapse$isNaturalized() || NaturalizeUtils.isNaturalizedUUID(self.getUUID())) && !(self instanceof Player)) {
            ci.cancel();
        }
    }

    @Inject(method = "serverAiStep", at = @At("HEAD"), cancellable = true)
    private void nature_collapse$onServerAiStep(CallbackInfo ci) {
        if (this.nature_collapse$isNaturalized() && !((Object) this instanceof Player)) {
            ci.cancel();
        }
    }

    @Inject(method = "pushEntities", at = @At("HEAD"), cancellable = true)
    private void nature_collapse$onPushEntities(CallbackInfo ci) {
        if (this.nature_collapse$isNaturalized() && !((Object) this instanceof Player)) {
            ci.cancel();
        }
    }

    @Inject(method = "isPickable", at = @At("HEAD"), cancellable = true)
    private void nature_collapse$onIsPickableLiving(CallbackInfoReturnable<Boolean> cir) {
        if (this.nature_collapse$isNaturalized() && !((Object) this instanceof Player)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "isPushable", at = @At("HEAD"), cancellable = true)
    private void nature_collapse$onIsPushableLiving(CallbackInfoReturnable<Boolean> cir) {
        if (this.nature_collapse$isNaturalized() && !((Object) this instanceof Player)) {
            cir.setReturnValue(false);
        }
    }
}
