package n4ture27.nature_collapse.effect;

import n4ture27.nature_collapse.utils.NaturalizeUtils;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

public class ToNatural extends MobEffect{
    public ToNatural() {
        super(MobEffectCategory.BENEFICIAL,0xFF0000);
    }

    @Override
    public void applyEffectTick(LivingEntity target, int pAmplifier) {
        NaturalizeUtils.Naturalize(target);
    }
    @Override
    public boolean isDurationEffectTick(int pDuration, int pAmplifier) {
        return true;
    }


}
