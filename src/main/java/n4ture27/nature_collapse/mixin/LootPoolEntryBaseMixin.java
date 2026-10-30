package n4ture27.nature_collapse.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer$EntryBase")
public abstract class LootPoolEntryBaseMixin {

    @Inject(method = "getWeight(F)I", at = @At("RETURN"), cancellable = true)
    private void nature_collapse$onGetWeight(float luck, CallbackInfoReturnable<Integer> cir) {
        int original = cir.getReturnValue();
        if (original <= 0) return;

        // luck の値に基づいて所持効果を適用
        // Clover: luck +1, quality +1, rate x1.5
        // FourLeafClover: luck +3, quality +3, rate x3.0
        if (luck >= 3.0f) {
            int boosted = (int) (original * 3.0f + 3.0f * luck);
            cir.setReturnValue(Math.max(boosted, original));
        } else if (luck >= 1.0f) {
            int boosted = (int) (original * 1.5f + 1.0f * luck);
            cir.setReturnValue(Math.max(boosted, original));
        }
    }
}
