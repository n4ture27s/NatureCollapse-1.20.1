package n4ture27.nature_collapse.mixin;

import n4ture27.nature_collapse.entity.ArborisEntity;
import n4ture27.nature_collapse.utils.INaturalizable;
import n4ture27.nature_collapse.utils.InventoryUtil;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Entity.class, priority = Integer.MAX_VALUE)
public abstract class EntityMixin {

    @Inject(method = "isInvulnerableTo", at = @At("HEAD"), cancellable = true)
    private void onIsInvulnerableTo(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;

        if (self instanceof LivingEntity living && ((INaturalizable) living).nature_collapse$isNaturalized()) {
            cir.setReturnValue(false);
            return;
        }

        if (self instanceof Player player && InventoryUtil.hasInvulnerableFlower(player)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "isInvulnerable", at = @At("HEAD"), cancellable = true)
    private void onIsInvulnerable(CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;

        if (self instanceof LivingEntity living && ((INaturalizable) living).nature_collapse$isNaturalized()) {
            cir.setReturnValue(false);
            return;
        }

        if (self instanceof Player player && InventoryUtil.hasInvulnerableFlower(player)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "kill", at = @At("HEAD"), cancellable = true)
    private void onKill(CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (self instanceof Player player && InventoryUtil.hasInvulnerableFlower(player)) {
            ci.cancel();
            return;
        }
        if (self instanceof ArborisEntity arboris && !arboris.isTrulyDefeated()) {
            ci.cancel();
        }
    }

    @Inject(method = "remove", at = @At("HEAD"), cancellable = true)
    private void onRemove(Entity.RemovalReason reason, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (self instanceof Player player && InventoryUtil.hasInvulnerableFlower(player)) {
            if (reason == Entity.RemovalReason.KILLED || reason == Entity.RemovalReason.DISCARDED) {
                ci.cancel();
                return;
            }
        }
        if (self instanceof ArborisEntity arboris && !arboris.isTrulyDefeated()) {
            if (reason.shouldDestroy()) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "setRemoved", at = @At("HEAD"), cancellable = true)
    private void onSetRemoved(Entity.RemovalReason reason, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (self instanceof Player player && InventoryUtil.hasInvulnerableFlower(player)) {
            if (reason == Entity.RemovalReason.KILLED || reason == Entity.RemovalReason.DISCARDED) {
                ci.cancel();
                return;
            }
        }
        if (self instanceof ArborisEntity arboris && !arboris.isTrulyDefeated()) {
            if (reason.shouldDestroy()) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "isRemoved", at = @At("HEAD"), cancellable = true)
    private void onIsRemoved(CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;
        if (self instanceof LivingEntity living && ((INaturalizable) living).nature_collapse$isNaturalized() && !(living instanceof Player)) {
            cir.setReturnValue(true);
            return;
        }
        if (n4ture27.nature_collapse.utils.NaturalizeUtils.isNaturalizedUUID(self.getUUID()) && !(self instanceof Player)) {
            cir.setReturnValue(true);
            return;
        }
        if (self instanceof Player player && InventoryUtil.hasInvulnerableFlower(player)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "getRemovalReason", at = @At("HEAD"), cancellable = true)
    private void onGetRemovalReason(CallbackInfoReturnable<Entity.RemovalReason> cir) {
        Entity self = (Entity) (Object) this;
        if (self instanceof LivingEntity living && ((INaturalizable) living).nature_collapse$isNaturalized() && !(living instanceof Player)) {
            cir.setReturnValue(Entity.RemovalReason.KILLED);
            return;
        }
        if (n4ture27.nature_collapse.utils.NaturalizeUtils.isNaturalizedUUID(self.getUUID()) && !(self instanceof Player)) {
            cir.setReturnValue(Entity.RemovalReason.KILLED);
            return;
        }
        if (self instanceof Player player && InventoryUtil.hasInvulnerableFlower(player)) {
            cir.setReturnValue(null);
        }
    }

    @Inject(method = "isPickable", at = @At("HEAD"), cancellable = true)
    private void nature_collapse$onIsPickable(CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;
        if (self.isRemoved()) {
            cir.setReturnValue(false);
            return;
        }
        if (self instanceof LivingEntity living && ((INaturalizable) living).nature_collapse$isNaturalized() && !(living instanceof Player)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "isPushable", at = @At("HEAD"), cancellable = true)
    private void nature_collapse$onIsPushable(CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;
        if (self.isRemoved()) {
            cir.setReturnValue(false);
            return;
        }
        if (self instanceof LivingEntity living && ((INaturalizable) living).nature_collapse$isNaturalized() && !(living instanceof Player)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "canCollideWith", at = @At("HEAD"), cancellable = true)
    private void nature_collapse$onCanCollideWith(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;
        if (self.isRemoved()) {
            cir.setReturnValue(false);
            return;
        }
        if (self instanceof LivingEntity living && ((INaturalizable) living).nature_collapse$isNaturalized() && !(living instanceof Player)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "canBeCollidedWith", at = @At("HEAD"), cancellable = true)
    private void nature_collapse$onCanBeCollidedWith(CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;
        if (self.isRemoved()) {
            cir.setReturnValue(false);
            return;
        }
        if (self instanceof LivingEntity living && ((INaturalizable) living).nature_collapse$isNaturalized() && !(living instanceof Player)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "isAttackable", at = @At("HEAD"), cancellable = true)
    private void nature_collapse$onIsAttackable(CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;
        if (self.isRemoved()) {
            cir.setReturnValue(false);
            return;
        }
        if (self instanceof LivingEntity living && ((INaturalizable) living).nature_collapse$isNaturalized() && !(living instanceof Player)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "getBoundingBox", at = @At("HEAD"), cancellable = true)
    private void nature_collapse$onGetBoundingBox(CallbackInfoReturnable<AABB> cir) {
        Entity self = (Entity) (Object) this;
        if (self.isRemoved() || (self instanceof LivingEntity living && ((INaturalizable) living).nature_collapse$isNaturalized() && !(living instanceof Player))) {
            cir.setReturnValue(new AABB(self.getX(), self.getY(), self.getZ(), self.getX(), self.getY(), self.getZ()));
        }
    }

    @Inject(method = "playSound(Lnet/minecraft/sounds/SoundEvent;FF)V", at = @At("HEAD"), cancellable = true)
    private void nature_collapse$onPlaySound(SoundEvent sound, float volume, float pitch, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (self.isRemoved() || (self instanceof LivingEntity living && ((INaturalizable) living).nature_collapse$isNaturalized() && !(living instanceof Player))) {
            ci.cancel();
        }
    }
}
