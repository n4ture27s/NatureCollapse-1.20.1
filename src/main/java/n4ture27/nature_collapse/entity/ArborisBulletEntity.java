package n4ture27.nature_collapse.entity;

import n4ture27.nature_collapse.NatureCollapse;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

public class ArborisBulletEntity extends Projectile {
    private static final EntityDataAccessor<Integer> BULLET_TYPE = SynchedEntityData.defineId(ArborisBulletEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> BULLET_SCALE = SynchedEntityData.defineId(ArborisBulletEntity.class, EntityDataSerializers.FLOAT);

    private int lifetime = 160;
    private float damage = 250.0f;
    private int erosionGain = 2;
    private boolean homing = false;
    @Nullable
    private UUID targetUUID = null;

    public ArborisBulletEntity(EntityType<? extends ArborisBulletEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public ArborisBulletEntity(Level level, double x, double y, double z, int type, float scale, Vec3 velocity, float damage, int erosion, int lifetime) {
        super(NatureCollapse.ArborisBulletEntityType.get(), level);
        this.setPos(x, y, z);
        this.setDeltaMovement(velocity);
        this.setBulletType(type);
        this.setBulletScale(scale);
        this.damage = damage;
        this.erosionGain = erosion;
        this.lifetime = lifetime;
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(BULLET_TYPE, 0);
        this.entityData.define(BULLET_SCALE, 1.0f);
    }

    public int getBulletType() {
        return this.entityData.get(BULLET_TYPE);
    }

    public void setBulletType(int type) {
        this.entityData.set(BULLET_TYPE, type);
    }

    public float getBulletScale() {
        return this.entityData.get(BULLET_SCALE);
    }

    public void setBulletScale(float scale) {
        this.entityData.set(BULLET_SCALE, scale);
    }

    public void setHoming(boolean homing, @Nullable LivingEntity target) {
        this.homing = homing;
        if (target != null) {
            this.targetUUID = target.getUUID();
        }
    }

    @Override
    public void tick() {
        super.tick();

        // 1. 移動処理
        Vec3 movement = this.getDeltaMovement();

        // 追尾弾（Homing）の場合、ターゲットへ向けて旋回加速
        if (this.homing && !this.level().isClientSide() && this.targetUUID != null && this.tickCount > 10) {
            Entity target = ((ServerLevel) this.level()).getEntity(this.targetUUID);
            if (target instanceof LivingEntity living && living.isAlive()) {
                Vec3 toTarget = living.getEyePosition().subtract(this.position()).normalize();
                double currentSpeed = Math.max(0.6, movement.length());
                Vec3 newVel = movement.normalize().scale(0.85).add(toTarget.scale(0.15)).normalize().scale(currentSpeed * 1.02);
                this.setDeltaMovement(newVel);
                movement = newVel;
            }
        }

        this.setPos(this.getX() + movement.x, this.getY() + movement.y, this.getZ() + movement.z);

        // 2. クライアント側パーティクル軌跡
        if (this.level().isClientSide()) {
            spawnTrailingParticles();
        } else {
            // 3. サーバー側当たり判定
            checkEntityCollisions();

            // 4. 寿命
            if (this.tickCount >= this.lifetime) {
                this.discard();
            }
        }
    }

    private void checkEntityCollisions() {
        float r = 0.6f * this.getBulletScale();
        AABB box = this.getBoundingBox().inflate(r);
        Entity owner = this.getOwner();
        List<LivingEntity> targets = this.level().getEntitiesOfClass(LivingEntity.class, box,
                e -> !(e instanceof ArborisEntity) && e.isAlive() && (owner == null || !e.equals(owner)));

        for (LivingEntity target : targets) {
            // 防護を貫通する原初の魔法ダメージ
            DamageSource dmgSource = this.level().damageSources().indirectMagic(this, owner instanceof LivingEntity le ? le : null);
            target.hurt(dmgSource, this.damage);

            // プレイヤーへの浸食値蓄積（無敵装備でも浸食は回避不能）
            if (target instanceof ServerPlayer player) {
                ArborisEntity.addErosion(player, this.erosionGain, "bullet_hit");
            }

            // 着弾音とパーティクル
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.HOSTILE, 1.5f, 1.2f);

            if (this.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.GLOW, this.getX(), this.getY(), this.getZ(), 8, 0.2, 0.2, 0.2, 0.05);
            }

            this.discard();
            break;
        }
    }

    private void spawnTrailingParticles() {
        int type = this.getBulletType();
        switch (type) {
            case 0 -> { // Emerald Seed
                this.level().addParticle(ParticleTypes.GLOW, this.getX(), this.getY(), this.getZ(), 0, 0, 0);
                this.level().addParticle(ParticleTypes.SPORE_BLOSSOM_AIR, this.getX(), this.getY(), this.getZ(), 0, 0.02, 0);
            }
            case 1 -> { // Cyan Diamond
                this.level().addParticle(ParticleTypes.SOUL_FIRE_FLAME, this.getX(), this.getY(), this.getZ(), 0, 0.01, 0);
                this.level().addParticle(ParticleTypes.END_ROD, this.getX(), this.getY(), this.getZ(), 0, 0, 0);
            }
            case 2 -> { // Solar Gold
                this.level().addParticle(ParticleTypes.FLAME, this.getX(), this.getY(), this.getZ(), 0, 0.02, 0);
                this.level().addParticle(ParticleTypes.GLOW, this.getX(), this.getY(), this.getZ(), 0, 0, 0);
            }
            case 3 -> { // Abyssal Purple
                this.level().addParticle(ParticleTypes.WITCH, this.getX(), this.getY(), this.getZ(), 0, 0.02, 0);
                this.level().addParticle(ParticleTypes.PORTAL, this.getX(), this.getY(), this.getZ(), 0, 0, 0);
            }
            default -> { // Radiant Spear
                this.level().addParticle(ParticleTypes.INSTANT_EFFECT, this.getX(), this.getY(), this.getZ(), 0, 0.05, 0);
                this.level().addParticle(ParticleTypes.END_ROD, this.getX(), this.getY(), this.getZ(), 0, 0, 0);
            }
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distanceSq) {
        return distanceSq < 128.0 * 128.0;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setBulletType(tag.getInt("BulletType"));
        this.setBulletScale(tag.getFloat("BulletScale"));
        this.damage = tag.getFloat("Damage");
        this.erosionGain = tag.getInt("ErosionGain");
        this.lifetime = tag.getInt("Lifetime");
        this.homing = tag.getBoolean("Homing");
        if (tag.hasUUID("TargetUUID")) {
            this.targetUUID = tag.getUUID("TargetUUID");
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("BulletType", this.getBulletType());
        tag.putFloat("BulletScale", this.getBulletScale());
        tag.putFloat("Damage", this.damage);
        tag.putInt("ErosionGain", this.erosionGain);
        tag.putInt("Lifetime", this.lifetime);
        tag.putBoolean("Homing", this.homing);
        if (this.targetUUID != null) {
            tag.putUUID("TargetUUID", this.targetUUID);
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
