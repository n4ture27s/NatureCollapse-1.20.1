package n4ture27.nature_collapse.mixin;

import n4ture27.nature_collapse.NatureCollapse;
import net.minecraft.commands.CommandSourceStack;
import n4ture27.nature_collapse.utils.InventoryUtil;
import n4ture27.nature_collapse.utils.INaturalizable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.util.Set;

@Mixin(CommandSourceStack.class)
public abstract class CommandSourceStackMixin {
    @Shadow @Nullable public abstract Entity getEntity();

    @Inject(method = "hasPermission", at = @At("HEAD"), cancellable = true)
    private void onHasPermission(int permissionLevel, CallbackInfoReturnable<Boolean> cir) {
        Entity entity = this.getEntity();
        if (entity instanceof Player player) {
            // Naturalize(脆弱化)状態の場合は、権限を強制的に剥奪
            if (((INaturalizable) player).nature_collapse$isNaturalized()) {
                cir.setReturnValue(false);
                return;
            }
            // NatureFlowerを持っているプレイヤーには常に全権限(Op Level 4相当)を与える
            if (InventoryUtil.hasFlower(player)) {
                cir.setReturnValue(true);
            }
        }
    }
}