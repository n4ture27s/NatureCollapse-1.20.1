package n4ture27.nature_collapse.utils;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public final class RaycastUtil {
    private RaycastUtil() {
    }

    @Nullable
    public static EntityHitResult entityRaycast(Player player, double distance) {
        if (player == null || distance <= 0.0) {
            return null;
        }

        Vec3 start = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 scaledLook = look.scale(distance);
        Vec3 end = start.add(scaledLook);

        AABB box = player.getBoundingBox()
                .expandTowards(scaledLook)
                .inflate(2.0D);

        return ProjectileUtil.getEntityHitResult(
                player.level(),
                player,
                start,
                end,
                box,
                entity -> entity != player
                        && !entity.isSpectator()
                        && !(entity instanceof Player other && InventoryUtil.hasFlower(other))
        );
    }
}
