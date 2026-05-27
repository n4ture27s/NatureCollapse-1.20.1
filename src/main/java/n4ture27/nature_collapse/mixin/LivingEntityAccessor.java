package n4ture27.nature_collapse.mixin;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LivingEntity.class)
public interface LivingEntityAccessor {
    @Accessor("DATA_HEALTH_ID")
    static EntityDataAccessor<Float> getDATA_HEALTH_ID() {
        throw new UnsupportedOperationException(); // Mixin will implement this
    }

    @Accessor("dead")
    void setDead(boolean dead);

    @Invoker("dropAllDeathLoot")
    void invokeDropAllDeathLoot(DamageSource source);
}