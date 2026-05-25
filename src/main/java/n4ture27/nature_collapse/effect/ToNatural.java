package n4ture27.nature_collapse.effect;

import n4ture27.nature_collapse.utils.NaturalizeUtils;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

public class ToNatural extends MobEffect {
    public ToNatural() {
        super(MobEffectCategory.BENEFICIAL, 0xFF0000);
    }

    @Override
    public void applyEffectTick(LivingEntity target, int pAmplifier) {
        if (!target.level().isClientSide()) {
            target.setInvulnerable(false);
            target.invulnerableTime = 0;

            if (!target.isDeadOrDying() || target.isAlive() || target.getHealth() > 0) {
                target.kill();
                if (!target.isDeadOrDying() || target.isAlive() || target.getHealth() > 0) {
                    target.die(target.damageSources().genericKill());
                    if (!target.isDeadOrDying() || target.isAlive()) {
                        target.setHealth(0);
                    }
                }
            }
        }
    }

    @Override
    public boolean isDurationEffectTick(int pDuration, int pAmplifier) {
        return true;
    }

}
