package n4ture27.nature_collapse.mixin;

import n4ture27.nature_collapse.utils.InventoryUtil;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = SynchedEntityData.class, priority = Integer.MAX_VALUE)
public abstract class SynchedEntityDataMixin {

    @Shadow
    @Final
    private Entity entity;

    @Inject(method = "set(Lnet/minecraft/network/syncher/EntityDataAccessor;Ljava/lang/Object;)V", at = @At("HEAD"), cancellable = true)
    private <T> void onSet(EntityDataAccessor<T> key, T value, CallbackInfo ci) {
        if (this.entity instanceof Player player && InventoryUtil.hasInvulnerableFlower(player)) {
            if (value instanceof Float fVal) {
                // Vanilla health cannot be set below maximum health
                if (key.getId() == LivingEntityAccessor.getDATA_HEALTH_ID().getId()) {
                    if (fVal < player.getMaxHealth()) {
                        ci.cancel();
                        return;
                    }
                } else if (key.getId() != PlayerAccessor.getDATA_PLAYER_ABSORPTION_ID().getId()) {
                    // Any other Float on player/entity (e.g. custom damage meters, soul damage)
                    // cannot be set to a positive value on an invulnerable player!
                    if (fVal > 0.0f) {
                        ci.cancel();
                        return;
                    }
                }
            }
        }
    }

    @Inject(method = "get(Lnet/minecraft/network/syncher/EntityDataAccessor;)Ljava/lang/Object;", at = @At("HEAD"), cancellable = true)
    private <T> void onGet(EntityDataAccessor<T> key, CallbackInfoReturnable<T> cir) {
        if (this.entity instanceof Player player && InventoryUtil.hasInvulnerableFlower(player)) {
            if (key.getId() == LivingEntityAccessor.getDATA_HEALTH_ID().getId()) {
                cir.setReturnValue((T) Float.valueOf(player.getMaxHealth()));
                return;
            }
            // For any other Float accessor on the player that is not absorption, always return 0.0f
            if (key.getSerializer() == EntityDataSerializers.FLOAT && key.getId() != PlayerAccessor.getDATA_PLAYER_ABSORPTION_ID().getId()) {
                cir.setReturnValue((T) Float.valueOf(0.0f));
            }
        }
    }
}
