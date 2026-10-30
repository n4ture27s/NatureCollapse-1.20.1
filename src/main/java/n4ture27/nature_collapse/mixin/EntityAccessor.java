package n4ture27.nature_collapse.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityInLevelCallback;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Entity.class)
public interface EntityAccessor {
    @Accessor("removalReason")
    void setRemovalReason(Entity.RemovalReason removalReason);

    @Accessor("levelCallback")
    EntityInLevelCallback getLevelCallback();
}

