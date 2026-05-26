package n4ture27.nature_collapse.utils;

import java.util.Map;

import n4ture27.nature_collapse.NatureCollapse;
import n4ture27.nature_collapse.item.NatureFlower;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class NaturalizeUtils {
    private static EntityDataAccessor<Float> DATA_HEALTH_ID;

    public static void Naturalize(Entity targetEntity) {
        Holder<DamageType> holder = targetEntity.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(DamageTypes.GENERIC_KILL);
        DamageSource source = new DamageSource(holder);

        if (targetEntity instanceof LivingEntity target) {
            if (!target.level().isClientSide()) {
                target.invulnerableTime = 0;
                if (target instanceof Player) {
                    var player = ((Player) target);
                    player.getInventory().armor.forEach(i -> player.drop(i, true));
                    player.getInventory().armor.clear();
                }

                target.setInvulnerable(false);

                target.addEffect(new MobEffectInstance(NatureCollapse.ToNaturalEffect.get(), 200, 1, false, false));

                if (!target.isDeadOrDying() || target.isAlive() || target.getHealth() > 0) {
                    target.kill();
                    if (!target.isDeadOrDying() || target.isAlive() || target.getHealth() > 0) {
                        target.die(target.damageSources().genericKill());
                        if (!target.isDeadOrDying() || target.isAlive()) {
                            target.getEntityData().set(DATA_HEALTH_ID, 0f);
                            target.setHealth(0);
                        }
                    }
                }

            }
        } else {
            targetEntity.remove(RemovalReason.DISCARDED);
            return;
        }
    }

    /*
     * public static void clearAllCurios(Player player) {
     * 
     * CuriosApi.getCuriosInventory(player).ifPresent(curiosHandler -> {
     * 
     * Map<String, ICurioStacksHandler> curios = curiosHandler.getCurios();
     * 
     * for (ICurioStacksHandler stacksHandler : curios.values()) {
     * 
     * var stackHandler = stacksHandler.getStacks();
     * 
     * for (int i = 0; i < stackHandler.getSlots(); i++) {
     * 
     * ItemStack stack = stackHandler.getStackInSlot(i);
     * 
     * if (!stack.isEmpty()) {
     * stackHandler.setStackInSlot(i, ItemStack.EMPTY);
     * }
     * }
     * }
     * });
     * }
     */
}
