package n4ture27.nature_collapse.mixin;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Player.class)
public interface PlayerAccessor {
    @Accessor("DATA_PLAYER_ABSORPTION_ID")
    static EntityDataAccessor<Float> getDATA_PLAYER_ABSORPTION_ID() {
        throw new UnsupportedOperationException();
    }
}
