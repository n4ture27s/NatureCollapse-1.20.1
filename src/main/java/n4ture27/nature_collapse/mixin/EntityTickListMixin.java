package n4ture27.nature_collapse.mixin;

import n4ture27.nature_collapse.utils.INaturalizable;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.entity.EntityTickList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.Consumer;

@Mixin(EntityTickList.class)
public abstract class EntityTickListMixin {
    @Redirect(method = "forEach", at = @At(value = "INVOKE", target = "Ljava/util/function/Consumer;accept(Ljava/lang/Object;)V"))
    private <T> void forEachNaturalize(Consumer<T> instance, T t) {
        if (!(t instanceof Entity entity)) {
            instance.accept(t);
            return;
        }

        // プレイヤー以外のLivingEntityで、かつ脆弱化状態のエンティティを対象にする
        if (entity instanceof LivingEntity living && !(living instanceof Player) && ((INaturalizable)living).nature_collapse$isNaturalized()) {
            if (!living.isRemoved()) {
                // クライアント側、もしくは死亡ティックが必要な場合のみ処理
                if (!living.level().isClientSide() || living.level().shouldTickDeath(living)) {
                    int time = ++living.deathTime;

                    // 20ティック（1秒）経過したら、煙（POOF）を出して消去
                    if (!entity.level().isClientSide() && time >= 20) {
                        living.level().broadcastEntityEvent(living, EntityEvent.POOF);
                        living.remove(Entity.RemovalReason.KILLED);
                        return;
                    }

                    // デスポーンチェック
                    if (!living.level().isClientSide()) {
                        living.checkDespawn();
                    }

                    // ティックを止める代わりに、見た目の位置やアニメーションを固定
                    living.setOldPosAndRot();
                    living.yHeadRotO = living.getYHeadRot();
                    living.yBodyRotO = living.yBodyRot;
                    living.walkAnimation.setSpeed(0.0f);
                    living.setSwimming(false);
                    living.deathTime = time;
                }
            } else {
                living.setOldPosAndRot();
            }
        } else {
            // 通常のエンティティ、またはプレイヤーはそのままティックを実行
            instance.accept(t);
        }
    }
}