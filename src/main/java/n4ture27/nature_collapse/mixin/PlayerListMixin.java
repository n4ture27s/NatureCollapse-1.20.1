package n4ture27.nature_collapse.mixin;

import n4ture27.nature_collapse.utils.InventoryUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = PlayerList.class, priority = Integer.MAX_VALUE)
public abstract class PlayerListMixin {

    @Inject(method = "respawn", at = @At("HEAD"), cancellable = true)
    private void onRespawn(ServerPlayer player, boolean keepInventory, CallbackInfoReturnable<ServerPlayer> cir) {
        if (!keepInventory && InventoryUtil.hasInvulnerableFlower(player)) {
            player.setHealth(player.getMaxHealth());
            player.deathTime = 0;
            cir.setReturnValue(player);
        }
    }
}
