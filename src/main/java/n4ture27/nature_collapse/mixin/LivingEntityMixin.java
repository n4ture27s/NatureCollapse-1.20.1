package n4ture27.nature_collapse.mixin;

import java.util.Set;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import n4ture27.nature_collapse.NatureCollapse;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.player.Player;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {
    @Shadow
    private static final EntityDataAccessor<Float> DATA_HEALTH_ID = SynchedEntityData.defineId(LivingEntity.class, EntityDataSerializers.FLOAT);

    @Overwrite
	public float getHealth() {
        var self = (LivingEntity) (Object) this;

        if (self instanceof Player player){
            if (player.getInventory().hasAnyOf(Set.of(NatureCollapse.NatureFlower.get()))) {
                return player.getMaxHealth();
            }
        }
        return self.getEntityData().get(DATA_HEALTH_ID);
    }
    
    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void onHurt(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        var self = (LivingEntity) (Object) this;
        if (self instanceof Player player && player.getInventory().hasAnyOf(Set.of(NatureCollapse.NatureFlower.get()))) {
            cir.setReturnValue(false); // ダメージ処理そのものを無効化
        }
    }

    @Inject(method = "die", at = @At("HEAD"), cancellable = true)
    private void onDie(DamageSource source, CallbackInfo ci) {
        var self = (LivingEntity) (Object) this;
        if (self instanceof Player player){
        if (player.getInventory().hasAnyOf(Set.of(NatureCollapse.NatureFlower.get()))){
            ci.cancel();
        }}
    }
}
