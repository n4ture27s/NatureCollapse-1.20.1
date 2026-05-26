package n4ture27.nature_collapse.utils;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.player.Player;

public class RaycastUtil {
    
    public static EntityHitResult entityRaycast(Player player, double distance) {
    
        Vec3 start = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = start.add(look.scale(distance));
    
        AABB box = player.getBoundingBox()
                .expandTowards(look.scale(distance))
                .inflate(1.0D);
    
        return ProjectileUtil.getEntityHitResult(
                player.level(),
                player,
                start,
                end,
                box,
                entity -> !entity.isSpectator() && entity.isPickable()
        );
    }
}