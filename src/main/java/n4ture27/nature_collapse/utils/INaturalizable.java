package n4ture27.nature_collapse.utils;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

public interface INaturalizable {
    boolean nature_collapse$isNaturalized();
    void nature_collapse$setNaturalized(boolean value);
    void nature_collapse$setHealthForced(float value);
    void nature_collapse$instantKill(LivingEntity attacker, DamageSource src);
}