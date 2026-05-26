package n4ture27.nature_collapse.mixin;

import n4ture27.nature_collapse.NatureCollapse;
import net.minecraft.world.damagesource.DamageSource;
import n4ture27.nature_collapse.utils.INaturalizable;
import n4ture27.nature_collapse.utils.InventoryUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Inject(method = "isInvulnerableTo", at = @At("HEAD"), cancellable = true)
    private void onIsInvulnerableTo(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;

        if (self instanceof LivingEntity living) {
            if (((INaturalizable) living).nature_collapse$isNaturalized()) {
                cir.setReturnValue(false);
                return;
            }
        }

        if (self instanceof Player player) {
            if (InventoryUtil.hasFlower(player)) {
                cir.setReturnValue(true);
            }
        }
    }
}