package n4ture27.nature_collapse.mixin;

import n4ture27.nature_collapse.utils.InventoryUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(value = EntitySelector.class, priority = 10000)
public class EntitySelectorMixin {
    @Inject(method = "findEntities", at = @At("RETURN"), cancellable = true)
    private void onFindEntities(CommandSourceStack source, CallbackInfoReturnable<List<? extends Entity>> cir) {
        List<? extends Entity> originalList = cir.getReturnValue();
        if (originalList.isEmpty()) return;

        List<Entity> filteredList = new ArrayList<>(originalList);
        boolean removed = filteredList.removeIf(entity -> {
            if (entity instanceof Player targetPlayer && targetPlayer.getInventory() != null) {
                if (InventoryUtil.hasFlower(targetPlayer)) {
                    return source.getEntity() != targetPlayer;
                }
            }
            return false;
        });

        if (removed) {
            cir.setReturnValue(filteredList);
        }
    }
}
