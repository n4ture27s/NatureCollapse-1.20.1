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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * プレイヤーの周囲に召喚され、照準を合わせて連続レーザービームを掃射する神聖幾何学魔方陣エンティティ
 */
public class ArborisMagicCircleEntity extends Entity {
    private static final EntityDataAccessor<Float> CIRCLE_RADIUS = SynchedEntityData.defineId(ArborisMagicCircleEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> CHARGE_PROGRESS = SynchedEntityData.defineId(ArborisMagicCircleEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> COLOR_THEME = SynchedEntityData.defineId(ArborisMagicCircleEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> AIM_PITCH = SynchedEntityData.defineId(ArborisMagicCircleEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> AIM_YAW = SynchedEntityData.defineId(ArborisMagicCircleEntity.class, EntityDataSerializers.FLOAT);

    public static final int CHARGE_DURATION = 30; // 1.5秒チャージ
    public static final int FIRE_START = 30;
    public static final int FIRE_END = 85;       // 連射時間
    public static final int MAX_LIFETIME = 95;   // 寿命

    @Nullable
    private UUID targetUUID = null;
    @Nullable
    private UUID ownerUUID = null;
    private Vec3 targetAimPos = Vec3.ZERO;

    public ArborisMagicCircleEntity(EntityType<? extends ArborisMagicCircleEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public ArborisMagicCircleEntity(Level level, double x, double y, double z, float targetRadius, int theme, @Nullable LivingEntity owner, @Nullable LivingEntity target) {
        super(NatureCollapse.ArborisMagicCircleEntityType.get(), level);
        this.setPos(x, y, z);
        this.setCircleRadius(targetRadius);
        this.setColorTheme(theme);
        this.noPhysics = true;
        if (owner != null) this.ownerUUID = owner.getUUID();
        if (target != null) {
            this.targetUUID = target.getUUID();
            this.targetAimPos = target.getEyePosition();
        }
        updateAimAngles();
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(CIRCLE_RADIUS, 2.8f);
        this.entityData.define(CHARGE_PROGRESS, 0.0f);
        this.entityData.define(COLOR_THEME, 0);
        this.entityData.define(AIM_PITCH, 0.0f);
        this.entityData.define(AIM_YAW, 0.0f);
    }

    public float getCircleRadius() {
        return this.entityData.get(CIRCLE_RADIUS);
    }

    public void setCircleRadius(float radius) {
        this.entityData.set(CIRCLE_RADIUS, radius);
    }

    public float getChargeProgress() {
        return this.entityData.get(CHARGE_PROGRESS);
    }

    public void setChargeProgress(float prog) {
        this.entityData.set(CHARGE_PROGRESS, prog);
    }

    public int getColorTheme() {
        return this.entityData.get(COLOR_THEME);
    }

    public void setColorTheme(int theme) {
        this.entityData.set(COLOR_THEME, theme);
    }

    public float getAimPitch() {
        return this.entityData.get(AIM_PITCH);
    }

    public float getAimYaw() {
        return this.entityData.get(AIM_YAW);
    }

    private void updateAimAngles() {
        Vec3 diff = this.targetAimPos.subtract(this.position());
        double distHoriz = Math.sqrt(diff.x * diff.x + diff.z * diff.z);
        float yaw = (float) (Mth.atan2(diff.x, diff.z) * (180.0 / Math.PI));
        float pitch = (float) (Mth.atan2(diff.y, distHoriz) * (180.0 / Math.PI));

        this.entityData.set(AIM_YAW, yaw);
        this.entityData.set(AIM_PITCH, pitch);
    }

    @Override
    public void tick() {
        super.tick();

        Level level = this.level();

        // ターゲットの追尾エイム更新
        if (!level.isClientSide() && this.targetUUID != null && level instanceof ServerLevel sl) {
            Entity target = sl.getEntity(this.targetUUID);
            if (target instanceof LivingEntity living && living.isAlive()) {
                // 照準をスムーズに追従
                Vec3 currentEye = living.getEyePosition();
                this.targetAimPos = this.targetAimPos.lerp(currentEye, 0.25);
                updateAimAngles();
            }
        }

        // チャージ進行度の更新
        if (!level.isClientSide()) {
            if (this.tickCount <= CHARGE_DURATION) {
                this.setChargeProgress((float) this.tickCount / (float) CHARGE_DURATION);
            } else {
                this.setChargeProgress(1.0f);
            }
        }

        // 1. チャージ演出フェーズ
        if (this.tickCount <= FIRE_START) {
            if (level.isClientSide()) {
                // 魔方陣中心へ向けて吸い込まれる収束光粒子
                float rad = this.getCircleRadius() * (float) this.tickCount / FIRE_START;
                for (int i = 0; i < 2; i++) {
                    double angle = this.random.nextDouble() * Math.PI * 2;
                    double px = this.getX() + Math.cos(angle) * rad;
                    double pz = this.getZ() + Math.sin(angle) * rad;
                    double py = this.getY() + (this.random.nextDouble() - 0.5) * 1.5;
                    level.addParticle(ParticleTypes.ENCHANT, px, py, pz, -Math.cos(angle) * 0.3, 0.05, -Math.sin(angle) * 0.3);
                }
            } else if (this.tickCount % 10 == 0) {
                level.playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 1.5f, 0.8f + (this.tickCount / (float) FIRE_START) * 0.8f);
            }
        }
        // 2. ビーム連射フェーズ (FIRE_START .. FIRE_END)
        else if (this.tickCount <= FIRE_END) {
            // 3 tickごとに魔方陣の中心から高エネルギー貫通槍ビームを発射
            if (!level.isClientSide() && this.tickCount % 3 == 0) {
                Vec3 aimDir = this.targetAimPos.subtract(this.position()).normalize();
                // 予測ブレ
                Vec3 shotVel = aimDir.add((this.random.nextDouble() - 0.5) * 0.05,
                                          (this.random.nextDouble() - 0.5) * 0.05,
                                          (this.random.nextDouble() - 0.5) * 0.05).normalize().scale(2.2);

                ArborisBulletEntity beamBullet = new ArborisBulletEntity(
                        level, this.getX(), this.getY(), this.getZ(), 4, 1.6f, shotVel, 380.0f, 3, 70);

                if (this.ownerUUID != null && level instanceof ServerLevel sl) {
                    Entity owner = sl.getEntity(this.ownerUUID);
                    if (owner != null) beamBullet.setOwner(owner);
                }
                level.addFreshEntity(beamBullet);

                // 発射音とマズルエフェクト
                level.playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.TRIDENT_RIPTIDE_2, SoundSource.HOSTILE, 1.8f, 1.6f + (this.random.nextFloat() * 0.3f));
            }
        }
        // 3. 寿命終了による消滅
        else if (!level.isClientSide() && this.tickCount >= MAX_LIFETIME) {
            this.discard();
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distanceSq) {
        return distanceSq < 192.0 * 192.0;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.setCircleRadius(tag.getFloat("CircleRadius"));
        this.setColorTheme(tag.getInt("ColorTheme"));
        this.targetAimPos = new Vec3(tag.getDouble("TargetX"), tag.getDouble("TargetY"), tag.getDouble("TargetZ"));
        if (tag.hasUUID("TargetUUID")) this.targetUUID = tag.getUUID("TargetUUID");
        if (tag.hasUUID("OwnerUUID")) this.ownerUUID = tag.getUUID("OwnerUUID");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putFloat("CircleRadius", this.getCircleRadius());
        tag.putInt("ColorTheme", this.getColorTheme());
        tag.putDouble("TargetX", this.targetAimPos.x);
        tag.putDouble("TargetY", this.targetAimPos.y);
        tag.putDouble("TargetZ", this.targetAimPos.z);
        if (this.targetUUID != null) tag.putUUID("TargetUUID", this.targetUUID);
        if (this.ownerUUID != null) tag.putUUID("OwnerUUID", this.ownerUUID);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
