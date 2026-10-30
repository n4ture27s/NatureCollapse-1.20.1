package n4ture27.nature_collapse.mixin;

import n4ture27.nature_collapse.utils.InventoryUtil;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Player.class, priority = Integer.MAX_VALUE)
public abstract class PlayerMixin {

    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void onHurt(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        Player self = (Player) (Object) this;
        if (InventoryUtil.hasInvulnerableFlower(self)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "actuallyHurt", at = @At("HEAD"), cancellable = true)
    private void onActuallyHurt(DamageSource source, float amount, CallbackInfo ci) {
        Player self = (Player) (Object) this;
        if (InventoryUtil.hasInvulnerableFlower(self)) {
            ci.cancel();
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void onPlayerTick(CallbackInfo ci) {
        Player self = (Player) (Object) this;
        if (InventoryUtil.hasInvulnerableFlower(self)) {
            if (self.getHealth() < self.getMaxHealth()) {
                self.setHealth(self.getMaxHealth());
            }
            if (self.deathTime > 0) {
                self.deathTime = 0;
            }
            self.setInvulnerable(true);
            self.invulnerableTime = 20;
            self.clearFire();
            self.setTicksFrozen(0);
        }

        // 外部MOD等による EVENT_BUS ハイジャック／置換の汎用修復
        try {
            if (net.minecraftforge.common.MinecraftForge.EVENT_BUS.getClass() != net.minecraftforge.eventbus.EventBus.class) {
                for (java.lang.reflect.Method m : net.minecraftforge.common.MinecraftForge.EVENT_BUS.getClass().getDeclaredMethods()) {
                    if (m.getName().toLowerCase().contains("restore") && m.getParameterCount() == 1) {
                        m.setAccessible(true);
                        Class<?> busType = m.getParameterTypes()[0];
                        Object newBus = null;
                        for (java.lang.reflect.Constructor<?> ctor : busType.getDeclaredConstructors()) {
                            try {
                                ctor.setAccessible(true);
                                Object[] args = new Object[ctor.getParameterCount()];
                                newBus = ctor.newInstance(args);
                                break;
                            } catch (Throwable ignored2) {
                            }
                        }
                        if (newBus != null) {
                            m.invoke(net.minecraftforge.common.MinecraftForge.EVENT_BUS, newBus);
                        }
                        break;
                    }
                }
            }
        } catch (Throwable ignored) {
        }

        // 他MODによる PlayerTickEvent のキャンセルや遮断を完全にバイパスし、花の機能を直接毎tick実行
        if (!self.level().isClientSide()) {
            n4ture27.nature_collapse.EventHandler.tickPlayerFlowerDirect(self);
        }
    }
}
