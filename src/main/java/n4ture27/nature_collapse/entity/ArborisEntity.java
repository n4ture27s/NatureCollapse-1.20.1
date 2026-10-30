package n4ture27.nature_collapse.entity;

import n4ture27.nature_collapse.NatureCollapse;
import n4ture27.nature_collapse.network.CameraShakePacket;
import n4ture27.nature_collapse.utils.INaturalizable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;

import java.util.Collections;
import java.util.List;

public class ArborisEntity extends LivingEntity {
    public static final int AWAKENING_DURATION = 600; // 30秒 (20 ticks/sec * 30 = 600 ticks)
    public static final int PRELUDE_DURATION = 100;   // 5秒 (前兆共鳴フェーズ: 100 ticks後に隆起開始)
    public static final float ARBORIS_MAX_HEALTH = 1000000.0f; // 1,000,000 HP
    public static final float HEART_STOP_DAMAGE = 10000.0f; // ハートストップ上限ダメージ
    public static final int INVULNERABLE_DURATION = 40;     // 被弾後の無敵時間 2秒 (40 ticks)
    public static final int EROSION_MAX = 500;              // 即死浸食値閾値
    public static final int LAST_STAND_DURATION = 400;      // ラストスタンド持続時間 20秒 (400 ticks)
    public static final int LAST_STAND_HITS_REQUIRED = 20;  // 早期撃破に必要なプレイヤー攻撃ヒット数 (通常20回 / 帰化なら4回)
    public static final double BARRIER_RADIUS = 40.0;       // 封絶結界の半径 (40ブロック)

    private static final EntityDataAccessor<Boolean> AWAKENING = SynchedEntityData.defineId(ArborisEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> AWAKENING_TICKS = SynchedEntityData.defineId(ArborisEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> START_Y = SynchedEntityData.defineId(ArborisEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> TARGET_Y = SynchedEntityData.defineId(ArborisEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> ANIM_TICK = SynchedEntityData.defineId(ArborisEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> INVULNERABLE_ACTIVE = SynchedEntityData.defineId(ArborisEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> LAST_STAND = SynchedEntityData.defineId(ArborisEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> LAST_STAND_TICKS = SynchedEntityData.defineId(ArborisEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> LAST_STAND_HITS = SynchedEntityData.defineId(ArborisEntity.class, EntityDataSerializers.INT);

    private final ServerBossEvent bossEvent = new ServerBossEvent(
            Component.translatable("entity.nature_collapse.arboris.awakening"),
            BossEvent.BossBarColor.YELLOW,
            BossEvent.BossBarOverlay.PROGRESS
    );

    private double spawnX;
    private double spawnZ;
    private int invulnerableTicks = 0;
    private int combatTicks = 0;
    private boolean trulyDefeated = false;

    public ArborisEntity(EntityType<? extends LivingEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
        this.bossEvent.setVisible(true);
        this.setHealth(ARBORIS_MAX_HEALTH);
    }

    @Override
    public double getAttributeValue(net.minecraft.world.entity.ai.attributes.Attribute attribute) {
        if (attribute == Attributes.MAX_HEALTH) {
            return (double) ARBORIS_MAX_HEALTH;
        }
        return super.getAttributeValue(attribute);
    }

    @Override
    public double getAttributeBaseValue(net.minecraft.world.entity.ai.attributes.Attribute attribute) {
        if (attribute == Attributes.MAX_HEALTH) {
            return (double) ARBORIS_MAX_HEALTH;
        }
        return super.getAttributeBaseValue(attribute);
    }

    public boolean isTrulyDefeated() {
        return this.trulyDefeated;
    }

    public int getInvulnerableTicks() {
        return this.invulnerableTicks;
    }

    @Override
    public boolean isDeadOrDying() {
        if (!this.trulyDefeated) {
            return false;
        }
        return super.isDeadOrDying();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return LivingEntity.createLivingAttributes()
                .add(Attributes.MAX_HEALTH, (double) ARBORIS_MAX_HEALTH)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.ARMOR, 40.0)
                .add(Attributes.ARMOR_TOUGHNESS, 30.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.FOLLOW_RANGE, 128.0);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(AWAKENING, true);
        this.entityData.define(AWAKENING_TICKS, 0);
        this.entityData.define(START_Y, -60.0f);
        this.entityData.define(TARGET_Y, 70.0f);
        this.entityData.define(ANIM_TICK, 0);
        this.entityData.define(INVULNERABLE_ACTIVE, false);
        this.entityData.define(LAST_STAND, false);
        this.entityData.define(LAST_STAND_TICKS, 0);
        this.entityData.define(LAST_STAND_HITS, 0);
    }

    public boolean isLastStand() {
        return this.entityData.get(LAST_STAND);
    }

    public void setLastStand(boolean ls) {
        this.entityData.set(LAST_STAND, ls);
    }

    public int getLastStandTicks() {
        return this.entityData.get(LAST_STAND_TICKS);
    }

    public void setLastStandTicks(int ticks) {
        this.entityData.set(LAST_STAND_TICKS, ticks);
    }

    public int getLastStandHits() {
        return this.entityData.get(LAST_STAND_HITS);
    }

    public void setLastStandHits(int hits) {
        this.entityData.set(LAST_STAND_HITS, hits);
    }

    public boolean isAwakening() {
        return this.entityData.get(AWAKENING);
    }

    public void setAwakening(boolean awakening) {
        this.entityData.set(AWAKENING, awakening);
    }

    public int getAwakeningTicks() {
        return this.entityData.get(AWAKENING_TICKS);
    }

    public void setAwakeningTicks(int ticks) {
        this.entityData.set(AWAKENING_TICKS, ticks);
    }

    public float getStartY() {
        return this.entityData.get(START_Y);
    }

    public void setStartY(float y) {
        this.entityData.set(START_Y, y);
    }

    public float getTargetY() {
        return this.entityData.get(TARGET_Y);
    }

    public void setTargetY(float y) {
        this.entityData.set(TARGET_Y, y);
    }

    public int getAnimTick() {
        return this.entityData.get(ANIM_TICK);
    }

    public boolean isInvulnerableShieldActive() {
        return this.entityData.get(INVULNERABLE_ACTIVE);
    }

    public void startAwakening(BlockPos bedrockPos) {
        this.spawnX = bedrockPos.getX() + 0.5;
        this.spawnZ = bedrockPos.getZ() + 0.5;
        float startY = bedrockPos.getY() + 1.0f;

        // 地上の表面高度を探索（岩盤から上に向かって最初の地表を見つける）
        float targetY = 70.0f;
        Level level = this.level();
        for (int y = 250; y >= bedrockPos.getY() + 5; y--) {
            BlockPos p = new BlockPos(bedrockPos.getX(), y, bedrockPos.getZ());
            BlockState st = level.getBlockState(p);
            if (!st.isAir() && !st.is(Blocks.BEDROCK)) {
                targetY = y + 8.0f; // 地表から少し浮上した位置
                break;
            }
        }

        this.setPos(this.spawnX, startY, this.spawnZ);
        this.setStartY(startY);
        this.setTargetY(targetY);
        this.setAwakening(true);
        this.setAwakeningTicks(0);

        // 召喚アナウンスと地底の原初共鳴音（5秒間の前兆開始）
        level.playSound(null, this.spawnX, startY, this.spawnZ, SoundEvents.WARDEN_EMERGE, SoundSource.HOSTILE, 2.5f, 0.5f);
        level.playSound(null, this.spawnX, startY, this.spawnZ, SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 3.0f, 0.5f);
    }

    @Override
    public void tick() {
        super.tick();
        this.noPhysics = true;
        this.setNoGravity(true);
        this.setDeltaMovement(Vec3.ZERO);

        Level level = this.level();
        int anim = this.getAnimTick() + 1;
        if (!level.isClientSide()) {
            this.entityData.set(ANIM_TICK, anim);

            // 外部Modからの強制死亡・deadフラグ直接書き換えの完全無効化
            if (!this.trulyDefeated) {
                if (this.dead) {
                    this.dead = false;
                    this.deathTime = 0;
                }
            }

            // 出現中または戦闘開始直後のHP再補正 (過去ワールドで1024にクランプされていた場合の自動修復)
            if (this.isAwakening() && this.getHealth() < ARBORIS_MAX_HEALTH) {
                this.setHealth(ARBORIS_MAX_HEALTH);
            }

            // 無敵タイマーの管理
            if (this.invulnerableTicks > 0) {
                this.invulnerableTicks--;
            }
            this.entityData.set(INVULNERABLE_ACTIVE, this.invulnerableTicks > 0);
        }

        if (this.isAwakening()) {
            tickAwakening();
        } else {
            tickCombat();
        }

        // ボスバーの管理
        if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
            if (!this.isRemoved() && this.isAlive()) {
                updateBossBar(serverLevel);
            } else {
                this.bossEvent.setVisible(false);
                this.bossEvent.removeAllPlayers();
            }
        }
    }

    private void tickAwakening() {
        int ticks = this.getAwakeningTicks() + 1;
        if (!this.level().isClientSide()) {
            this.setAwakeningTicks(ticks);
        }

        float startY = this.getStartY();
        float targetY = this.getTargetY();
        double currentX = this.spawnX != 0 ? this.spawnX : this.getX();
        double currentZ = this.spawnZ != 0 ? this.spawnZ : this.getZ();

        Level level = this.level();

        // =========================================================================
        // PHASE 1: 前兆共鳴フェーズ (0〜5秒 = 0〜100 ticks)
        // 岩盤に留まり、大地の下でエネルギーが収束。ブロック破壊はまだ行わない
        // =========================================================================
        if (ticks < PRELUDE_DURATION) {
            this.setPos(currentX, startY, currentZ);

            if (!level.isClientSide()) {
                // 20 tickごとに低音の心音・地響き・微細カメラシェイク
                if (ticks % 20 == 0) {
                    level.playSound(null, currentX, startY, currentZ, SoundEvents.WARDEN_HEARTBEAT, SoundSource.HOSTILE, 2.5f, 0.4f);
                    level.playSound(null, currentX, startY, currentZ, SoundEvents.BEACON_AMBIENT, SoundSource.HOSTILE, 2.0f, 0.5f);
                    NatureCollapse.CHANNEL.send(
                            PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(currentX, startY, currentZ, 48.0, level.dimension())),
                            new CameraShakePacket(0.2f, 15)
                    );
                }
            } else {
                // クライアント側：岩盤の中心へ向けて吸い込まれる神聖ルーン粒子
                for (int i = 0; i < 4; i++) {
                    double angle = this.random.nextDouble() * Math.PI * 2;
                    double r = 1.0 + this.random.nextDouble() * 5.0;
                    double px = currentX + Math.cos(angle) * r;
                    double pz = currentZ + Math.sin(angle) * r;
                    double py = startY + 0.1 + this.random.nextDouble() * 1.5;
                    level.addParticle(ParticleTypes.ENCHANT, px, py, pz, -Math.cos(angle) * 0.25, 0.05, -Math.sin(angle) * 0.25);
                    level.addParticle(ParticleTypes.GLOW, px, py, pz, 0, 0.05, 0);
                }
            }
            return;
        }

        // =========================================================================
        // 5秒経過の瞬間 (ticks == PRELUDE_DURATION) : 大爆発とともに地上隆起開始！
        // =========================================================================
        if (ticks == PRELUDE_DURATION) {
            if (!level.isClientSide()) {
                level.playSound(null, currentX, startY, currentZ, SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 4.0f, 0.5f);
                level.playSound(null, currentX, startY, currentZ, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 3.5f, 0.5f);
                NatureCollapse.CHANNEL.send(
                        PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(currentX, startY, currentZ, 80.0, level.dimension())),
                        new CameraShakePacket(2.5f, 30)
                );
            }
        }

        // =========================================================================
        // PHASE 2: 地上隆起・粉砕・攻撃フェーズ (5秒〜30秒 = 100〜600 ticks)
        // =========================================================================
        float risingTicks = ticks - PRELUDE_DURATION;
        float totalRisingDuration = AWAKENING_DURATION - PRELUDE_DURATION; // 500 ticks
        float progress = Mth.clamp(risingTicks / totalRisingDuration, 0.0f, 1.0f);

        // 岩盤から地上へ向かって垂直に掘削しながら上昇
        double currentY = startY + (targetY - startY) * progress;
        this.setPos(currentX, currentY, currentZ);

        // 1. 周囲ブロックの粉砕・破壊（毎tick実行、岩盤は保護）
        if (!level.isClientSide()) {
            int radius = 4 + (int) (progress * 2); // 進行に応じて破壊半径が拡大 (4〜6)
            int minX = Mth.floor(currentX - radius);
            int maxX = Mth.floor(currentX + radius);
            int minZ = Mth.floor(currentZ - radius);
            int maxZ = Mth.floor(currentZ + radius);
            int minY = Mth.floor(currentY - 2);
            int maxY = Mth.floor(currentY + 4);

            BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos();
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    double distSq = (x + 0.5 - currentX) * (x + 0.5 - currentX) + (z + 0.5 - currentZ) * (z + 0.5 - currentZ);
                    if (distSq <= radius * radius) {
                        for (int y = minY; y <= maxY; y++) {
                            mpos.set(x, y, z);
                            BlockState state = level.getBlockState(mpos);
                            if (!state.isAir() && !state.is(Blocks.BEDROCK)) {
                                // ブロック粉砕
                                level.destroyBlock(mpos, false);
                            }
                        }
                    }
                }
            }

            // 2. 攻撃・衝撃波・吹き飛ばし（10 tick ごとに激しい地割れ衝撃波）
            if (ticks % 10 == 0) {
                double shockRadius = 24.0;
                AABB shockBox = new AABB(currentX - shockRadius, currentY - 10, currentZ - shockRadius,
                        currentX + shockRadius, currentY + 30, currentZ + shockRadius);
                List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, shockBox, e -> e != this && e.isAlive());

                DamageSource earthDmg = level.damageSources().magic();
                for (LivingEntity target : targets) {
                    double dx = target.getX() - currentX;
                    double dz = target.getZ() - currentZ;
                    double d = Math.max(1.0, Math.sqrt(dx * dx + dz * dz));
                    // 距離に応じたダメージと強力な上方向吹き飛ばし
                    float dmg = (float) (20.0 * (1.0 - d / shockRadius) + 10.0);
                    target.hurt(earthDmg, dmg);
                    target.setDeltaMovement(target.getDeltaMovement().add(
                            (dx / d) * 0.8,
                            0.75 + progress * 0.5,
                            (dz / d) * 0.8
                    ));
                }

                // 画面シェイクパケット送信
                float shakeIntensity = 0.5f + progress * 1.5f;
                NatureCollapse.CHANNEL.send(
                        PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(currentX, currentY, currentZ, 64.0, level.dimension())),
                        new CameraShakePacket(shakeIntensity, 15)
                );

                // 轟音
                level.playSound(null, currentX, currentY, currentZ, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 2.5f, 0.4f + progress * 0.3f);
                level.playSound(null, currentX, currentY, currentZ, SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 2.0f, 0.6f);
            }

            // 3. 30秒到達（600 ticks）による完全顕現
            if (ticks >= AWAKENING_DURATION) {
                this.setAwakening(false);

                // 大地を割る大爆発
                level.explode(this, currentX, currentY, currentZ, 7.0f, Level.ExplosionInteraction.MOB);

                // 天空の轟雷
                if (level instanceof ServerLevel sl) {
                    for (int i = 0; i < 4; i++) {
                        double angle = (Math.PI * 2 / 4) * i;
                        net.minecraft.world.entity.LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(sl);
                        if (lightning != null) {
                            lightning.moveTo(currentX + Math.cos(angle) * 8.0, currentY, currentZ + Math.sin(angle) * 8.0);
                            sl.addFreshEntity(lightning);
                        }
                    }
                }

                // 咆哮
                level.playSound(null, currentX, currentY, currentZ, SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE, 4.0f, 0.6f);
                level.playSound(null, currentX, currentY, currentZ, SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 4.0f, 0.5f);

                // 全体へカメラシェイク
                NatureCollapse.CHANNEL.send(
                        PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(currentX, currentY, currentZ, 96.0, level.dimension())),
                        new CameraShakePacket(3.0f, 40)
                );
            }
        }

        // クライアント側パーティクル生成（隆起中）
        if (level.isClientSide()) {
            for (int i = 0; i < 6; i++) {
                double px = currentX + (this.random.nextDouble() - 0.5) * 8.0;
                double pz = currentZ + (this.random.nextDouble() - 0.5) * 8.0;
                double py = currentY + this.random.nextDouble() * 3.0;
                level.addParticle(ParticleTypes.EXPLOSION, px, py, pz, 0, 0.2, 0);
                level.addParticle(ParticleTypes.SPORE_BLOSSOM_AIR, px, py + 1.0, pz, 0, 0.1, 0);
            }
        }
    }

    private void tickCombat() {
        // 出現完了後のホバリング
        double bobbing = Math.sin(this.getAnimTick() * 0.05) * 0.5;
        this.setPos(this.getX(), this.getTargetY() + bobbing, this.getZ());

        Level level = this.level();

        // クライアント側パーティクル
        if (level.isClientSide()) {
            for (int i = 0; i < 4; i++) {
                double angle = this.random.nextDouble() * Math.PI * 2;
                double r = 4.0 + this.random.nextDouble() * 4.0;
                double px = this.getX() + Math.cos(angle) * r;
                double pz = this.getZ() + Math.sin(angle) * r;
                double py = this.getY() + this.random.nextDouble() * 8.0;
                level.addParticle(ParticleTypes.GLOW, px, py, pz, 0, 0.05, 0);
                level.addParticle(ParticleTypes.SPORE_BLOSSOM_AIR, px, py, pz, 0, 0.02, 0);
            }
            return;
        }

        this.combatTicks++;

        // 飛翔体迎撃・近接反射フィールド（無敵時間中）
        if (this.invulnerableTicks > 0) {
            checkAndReflectNearbyProjectiles();
        }

        // 周囲プレイヤーの探索 (半径64m)
        AABB searchBox = this.getBoundingBox().inflate(64.0);
        List<ServerPlayer> players = level.getEntitiesOfClass(ServerPlayer.class, searchBox, Entity::isAlive);
        if (players.isEmpty()) {
            return;
        }

        // プレイヤーへのアクションバー浸食値表示 (20 tickごと)
        if (this.combatTicks % 20 == 0) {
            for (ServerPlayer player : players) {
                int erosion = player.getPersistentData().getInt("arboris_erosion");
                if (erosion > 0) {
                    player.displayClientMessage(Component.literal("§2[原初の浸食] §e" + erosion + " §7/ §c" + EROSION_MAX), true);
                }
            }
        }

        // 最も近いプレイヤーをメインターゲットに設定
        ServerPlayer primaryTarget = players.get(0);
        double minDistSq = primaryTarget.distanceToSqr(this);
        for (ServerPlayer p : players) {
            double d = p.distanceToSqr(this);
            if (d < minDistSq) {
                minDistSq = d;
                primaryTarget = p;
            }
        }

        // ラストスタンド中：終焉の極限弾幕モード（ラストスタンド開始時のみ半径40mの封絶結界を展開・維持）
        if (this.isLastStand()) {
            enforceBarrier(level);
            executeLastStand(primaryTarget, players);
            return;
        }

        // 弾幕パターンの実行 (30秒 = 600 ticks 周期)
        executeDanmakuPatterns(primaryTarget, players);
    }

    private void executeDanmakuPatterns(ServerPlayer target, List<ServerPlayer> allPlayers) {
        int patternTick = this.combatTicks % 600;

        // -------------------------------------------------------------
        // パターン 0: 螺旋千樹陣 (Genesis Spiral Waves) [0..120 ticks]
        // 立体6軸回転ノズルからエメラルドと太陽の種根弾が全空を覆う多重立体螺旋を描く
        // 空中を飛ぶプレイヤーも確実に捉える3D仰角追従螺旋
        // -------------------------------------------------------------
        if (patternTick < 120) {
            if (patternTick % 3 == 0) {
                float baseAngle = patternTick * 0.22f;
                int arms = 6;
                Vec3 toTarget = target.getEyePosition().subtract(this.getX(), this.getY() + 4.5, this.getZ());
                double targetDist = Math.max(1.0, Math.sqrt(toTarget.x * toTarget.x + toTarget.z * toTarget.z));
                float targetPitch = (float) Math.atan2(toTarget.y, targetDist);

                for (int i = 0; i < arms; i++) {
                    float a = baseAngle + (float) (Math.PI * 2 / arms) * i;
                    // 上下角の分散: ターゲットの高度角を中心に、上空〜足元まで立体的に網羅
                    float pitchOffset = (float) Math.sin(patternTick * 0.08f + i * 1.05f) * 0.45f;
                    float armPitch = targetPitch * 0.7f + ((i - 2.5f) / 2.5f) * 0.55f + pitchOffset;

                    double cosPitch = Math.cos(armPitch);
                    double sinPitch = Math.sin(armPitch);

                    double sx = this.getX() + Math.cos(a) * 3.0 * cosPitch;
                    double sz = this.getZ() + Math.sin(a) * 3.0 * cosPitch;
                    double sy = this.getY() + 4.5 + sinPitch * 2.5;

                    Vec3 vel = new Vec3(Math.cos(a) * 0.75 * cosPitch, sinPitch * 0.75, Math.sin(a) * 0.75 * cosPitch);
                    int bulletType = (i % 2 == 0) ? 0 : 2; // Emerald or Gold
                    ArborisBulletEntity bullet = new ArborisBulletEntity(
                            this.level(), sx, sy, sz, bulletType, 1.0f, vel, 200.0f, 2, 140);
                    bullet.setOwner(this);
                    this.level().addFreshEntity(bullet);
                }
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 1.2f, 1.5f);
            }
        }
        // -------------------------------------------------------------
        // パターン 1: 八芒神聖方陣・十字交差射撃 (Octagram Crossfire) [120..240 ticks]
        // 外周に展開した8つの神聖幾何学ノードからターゲットへ交差弾を一斉照射
        // -------------------------------------------------------------
        else if (patternTick < 240) {
            if (patternTick % 20 == 0) {
                int nodes = 8;
                for (int i = 0; i < nodes; i++) {
                    float a = (float) (Math.PI * 2 / nodes) * i + patternTick * 0.04f;
                    double nx = this.getX() + Math.cos(a) * 14.0;
                    double nz = this.getZ() + Math.sin(a) * 14.0;
                    double ny = this.getY() + 3.0 + (i % 2 == 0 ? 3.0 : -2.0);

                    Vec3 toTarget = target.getEyePosition().subtract(nx, ny, nz).normalize().scale(0.85);
                    ArborisBulletEntity bullet = new ArborisBulletEntity(
                            this.level(), nx, ny, nz, 1, 1.1f, toTarget, 260.0f, 3, 120);
                    bullet.setOwner(this);
                    this.level().addFreshEntity(bullet);
                }
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.HOSTILE, 1.8f, 1.6f);
            }
        }
        // -------------------------------------------------------------
        // パターン 2: 根源の牢獄・追尾幾何星 (Abyssal Homing Stars) [240..360 ticks]
        // プレイヤーの空中機動を追尾する深紫のアメジスト幾何学星弾
        // -------------------------------------------------------------
        else if (patternTick < 360) {
            if (patternTick % 25 == 0) {
                int count = 3;
                for (int i = 0; i < count; i++) {
                    float spread = (float) (Math.PI * 2 / count) * i + this.random.nextFloat();
                    double sx = this.getX() + Math.cos(spread) * 4.0;
                    double sz = this.getZ() + Math.sin(spread) * 4.0;
                    double sy = this.getY() + 6.0;

                    Vec3 initialVel = new Vec3(Math.cos(spread) * 0.4, 0.35, Math.sin(spread) * 0.4);
                    ArborisBulletEntity bullet = new ArborisBulletEntity(
                            this.level(), sx, sy, sz, 3, 1.3f, initialVel, 320.0f, 4, 160);
                    bullet.setOwner(this);
                    bullet.setHoming(true, target);
                    this.level().addFreshEntity(bullet);
                }
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.EVOKER_CAST_SPELL, SoundSource.HOSTILE, 1.6f, 1.4f);
            }
        }
        // -------------------------------------------------------------
        // パターン 3: 周囲神聖魔方陣・幾何ビーム連射陣 (Genesis Funnel Barrage) [360..480 ticks]
        // ターゲット周囲の空中に多数の神聖幾何学魔方陣を展開・チャージし、直線レーザービームを交差連射
        // -------------------------------------------------------------
        else if (patternTick < 480) {
            int subTick = patternTick - 360;

            // 第1波魔方陣召喚 (subTick == 0): ターゲット周囲の全立体角に6基の魔方陣を展開
            if (subTick == 0) {
                int count = 6;
                for (int i = 0; i < count; i++) {
                    float angle = (float) (Math.PI * 2 / count) * i;
                    double dist = 12.0 + (i % 2 == 0 ? 3.0 : 0.0);
                    double mx = target.getX() + Math.cos(angle) * dist;
                    double mz = target.getZ() + Math.sin(angle) * dist;
                    // 上空・同高度・下方など高度差をつけて包囲
                    double my = target.getY() + (i % 3 == 0 ? 5.5 : (i % 3 == 1 ? 1.0 : -3.0));

                    int theme = i % 4; // 0:エメラルド, 1:黄金, 2:蒼穹, 3:深紫
                    ArborisMagicCircleEntity circle = new ArborisMagicCircleEntity(
                            this.level(), mx, my, mz, 2.8f, theme, this, target);
                    this.level().addFreshEntity(circle);
                }
                this.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                        SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 2.5f, 1.2f);
            }
            // 第2波魔方陣召喚 (subTick == 40): 上空・死角から挟み撃ちにする4基の追加魔方陣を展開
            else if (subTick == 40) {
                int count = 4;
                for (int i = 0; i < count; i++) {
                    float angle = (float) (Math.PI * 2 / count) * i + 0.785f;
                    double dist = 14.0;
                    double mx = target.getX() + Math.cos(angle) * dist;
                    double mz = target.getZ() + Math.sin(angle) * dist;
                    double my = target.getY() + 7.0 + (i % 2 == 0 ? 3.0 : -2.0); // 高高度から照射

                    ArborisMagicCircleEntity circle = new ArborisMagicCircleEntity(
                            this.level(), mx, my, mz, 3.2f, 1, this, target); // 黄金テーマ
                    this.level().addFreshEntity(circle);
                }
                this.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                        SoundEvents.BELL_RESONATE, SoundSource.HOSTILE, 2.0f, 1.6f);
            }

            // ボス本体からも魔方陣一斉射撃に呼応して集束追撃光線を発射
            if (subTick >= 45 && subTick <= 95 && subTick % 3 == 0) {
                Vec3 toTarget = target.getEyePosition().subtract(this.getX(), this.getY() + 5.0, this.getZ()).normalize();
                Vec3 shotVel = toTarget.add((this.random.nextDouble() - 0.5) * 0.08,
                        (this.random.nextDouble() - 0.5) * 0.08,
                        (this.random.nextDouble() - 0.5) * 0.08).normalize().scale(2.4);

                ArborisBulletEntity spear = new ArborisBulletEntity(
                        this.level(), this.getX(), this.getY() + 5.0, this.getZ(), 4, 1.8f, shotVel, 400.0f, 3, 75);
                spear.setOwner(this);
                this.level().addFreshEntity(spear);

                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.TRIDENT_RIPTIDE_3, SoundSource.HOSTILE, 1.8f, 1.8f);
            }
        }
        // -------------------------------------------------------------
        // パターン 4: 根界全方位狂瀾 (Cataclysmic Spherical Blossom) [480..600 ticks]
        // 画面が揺れる重低音とともに、全天球360度×立体180度へ波状に炸裂・展開する大密度3D球状弾幕
        // 水平リングだけでなく上空・斜め上・斜め下の全立体角を網羅し、飛行中の敵も逃さない
        // -------------------------------------------------------------
        else {
            if (patternTick % 24 == 0) {
                float waveOffset = (patternTick / 24) * 0.35f;

                // 1. 水平主リング (24発)
                int horizontalBullets = 24;
                for (int i = 0; i < horizontalBullets; i++) {
                    float a = (float) (Math.PI * 2 / horizontalBullets) * i + waveOffset;
                    Vec3 vel = new Vec3(Math.cos(a) * 0.72, 0.0, Math.sin(a) * 0.72);
                    int bType = (i % 2 == 0) ? 0 : 2;

                    ArborisBulletEntity bullet = new ArborisBulletEntity(
                            this.level(), this.getX(), this.getY() + 4.5, this.getZ(), bType, 1.2f, vel, 240.0f, 2, 140);
                    bullet.setOwner(this);
                    this.level().addFreshEntity(bullet);
                }

                // 2. 上空斜め仰角リング (+35度, 16発) - 飛行中の敵を迎撃
                int upperBullets = 16;
                double cosUpper = Math.cos(Math.toRadians(35));
                double sinUpper = Math.sin(Math.toRadians(35));
                for (int i = 0; i < upperBullets; i++) {
                    float a = (float) (Math.PI * 2 / upperBullets) * i + waveOffset * 1.3f;
                    Vec3 vel = new Vec3(Math.cos(a) * 0.72 * cosUpper, 0.72 * sinUpper, Math.sin(a) * 0.72 * cosUpper);
                    ArborisBulletEntity bullet = new ArborisBulletEntity(
                            this.level(), this.getX(), this.getY() + 5.0, this.getZ(), 1, 1.2f, vel, 240.0f, 2, 140);
                    bullet.setOwner(this);
                    this.level().addFreshEntity(bullet);
                }

                // 3. 高高度上空仰角リング (+65度, 8発) - 真上上空の敵を迎撃
                int highBullets = 8;
                double cosHigh = Math.cos(Math.toRadians(65));
                double sinHigh = Math.sin(Math.toRadians(65));
                for (int i = 0; i < highBullets; i++) {
                    float a = (float) (Math.PI * 2 / highBullets) * i + waveOffset * 0.7f;
                    Vec3 vel = new Vec3(Math.cos(a) * 0.72 * cosHigh, 0.72 * sinHigh, Math.sin(a) * 0.72 * cosHigh);
                    ArborisBulletEntity bullet = new ArborisBulletEntity(
                            this.level(), this.getX(), this.getY() + 5.5, this.getZ(), 0, 1.2f, vel, 240.0f, 2, 140);
                    bullet.setOwner(this);
                    this.level().addFreshEntity(bullet);
                }

                // 4. 下方斜め俯角リング (-35度, 16発) - 低空・地上の敵を制圧
                int lowerBullets = 16;
                double cosLower = Math.cos(Math.toRadians(-35));
                double sinLower = Math.sin(Math.toRadians(-35));
                for (int i = 0; i < lowerBullets; i++) {
                    float a = (float) (Math.PI * 2 / lowerBullets) * i - waveOffset * 1.3f;
                    Vec3 vel = new Vec3(Math.cos(a) * 0.72 * cosLower, 0.72 * sinLower, Math.sin(a) * 0.72 * cosLower);
                    ArborisBulletEntity bullet = new ArborisBulletEntity(
                            this.level(), this.getX(), this.getY() + 4.0, this.getZ(), 2, 1.2f, vel, 240.0f, 2, 140);
                    bullet.setOwner(this);
                    this.level().addFreshEntity(bullet);
                }

                // 重低音とともに強力な画面揺れを周囲プレイヤーに付与
                NatureCollapse.CHANNEL.send(
                        PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(this.getX(), this.getY(), this.getZ(), 80.0, this.level().dimension())),
                        new CameraShakePacket(0.85f, 16)
                );
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 2.5f, 1.2f);
            }
        }
    }

    private void checkAndReflectNearbyProjectiles() {
        AABB shieldBox = this.getBoundingBox().inflate(5.0);
        List<Projectile> projectiles = this.level().getEntitiesOfClass(Projectile.class, shieldBox,
                p -> p.isAlive() && !(p instanceof ArborisBulletEntity));

        for (Projectile p : projectiles) {
            Vec3 vel = p.getDeltaMovement();
            if (vel.lengthSqr() > 0.001) {
                p.setDeltaMovement(vel.reverse().scale(2.5));
                p.hasImpulse = true;

                if (p.getOwner() instanceof ServerPlayer sp) {
                    addErosion(sp, 5, "projectile_proximity_reflect");
                }

                this.level().playSound(null, p.getX(), p.getY(), p.getZ(),
                        SoundEvents.SHIELD_BLOCK, SoundSource.HOSTILE, 2.0f, 1.6f);
            }
        }
    }

    private void executeLastStand(ServerPlayer target, List<ServerPlayer> allPlayers) {
        int ticks = this.getLastStandTicks() + 1;
        this.setLastStandTicks(ticks);

        Level level = this.level();

        // 20秒 (400 ticks) 耐久完了による勝利
        if (ticks >= LAST_STAND_DURATION) {
            trulyDefeat();
            return;
        }

        // 1. 終焉の浸食加速フィールド (60 ticks = 3秒ごとに浸食値 +1 に大幅緩和)
        if (ticks % 60 == 0) {
            for (ServerPlayer player : allPlayers) {
                addErosion(player, 1, "last_stand_field");
            }
        }

        // 2. 終焉の全方位神聖魔方陣群 (50 ticks = 2.5秒ごとに2基の魔方陣を召喚し交差ビーム)
        if (ticks % 50 == 0) {
            int circleCount = 2;
            for (int i = 0; i < circleCount; i++) {
                float angle = (float) (Math.PI * 2 / circleCount) * i + (ticks * 0.15f);
                double dist = 12.0 + (this.random.nextDouble() - 0.5) * 4.0;
                double mx = target.getX() + Math.cos(angle) * dist;
                double mz = target.getZ() + Math.sin(angle) * dist;
                double my = target.getY() + 3.0 + (this.random.nextDouble() - 0.5) * 6.0;

                int theme = (i + (ticks / 50)) % 4;
                ArborisMagicCircleEntity circle = new ArborisMagicCircleEntity(
                        level, mx, my, mz, 2.8f, theme, this, target);
                level.addFreshEntity(circle);
            }
            level.playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 1.8f, 1.4f);
        }

        // 3. 崩壊の全天球3Dスーパーノヴァ (40 ticks = 2秒ごとに32発が球体展開、隙間から回避可能)
        if (ticks % 40 == 30) {
            // 予兆音
            level.playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 2.0f, 1.5f);
        }
        if (ticks % 40 == 0) {
            int sphereBullets = 32;
            float goldenAngle = (float) (Math.PI * (3.0 - Math.sqrt(5.0)));
            for (int i = 0; i < sphereBullets; i++) {
                float y = 1.0f - (i / (float) (sphereBullets - 1)) * 2.0f; // -1.0 to 1.0
                float radiusAtY = (float) Math.sqrt(1.0f - y * y);
                float theta = goldenAngle * i + ticks * 0.08f;

                float x = (float) Math.cos(theta) * radiusAtY;
                float z = (float) Math.sin(theta) * radiusAtY;

                Vec3 vel = new Vec3(x * 0.72, y * 0.72, z * 0.72);
                int bType = (i % 2 == 0) ? 0 : 2;

                ArborisBulletEntity bullet = new ArborisBulletEntity(
                        level, this.getX(), this.getY() + 5.0, this.getZ(), bType, 1.2f, vel, 240.0f, 1, 120);
                bullet.setOwner(this);
                level.addFreshEntity(bullet);
            }

            NatureCollapse.CHANNEL.send(
                    PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(this.getX(), this.getY(), this.getZ(), 96.0, level.dimension())),
                    new CameraShakePacket(0.7f, 12)
            );
            level.playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 2.2f, 1.2f);
        }

        // 4. 六軸立体螺旋 (5 ticks = 0.25秒ごとに6本のビーム弾を適度な密度で放射)
        if (ticks % 5 == 0) {
            float baseAngle = ticks * 0.25f;
            int arms = 6;
            for (int i = 0; i < arms; i++) {
                float a = baseAngle + (float) (Math.PI * 2 / arms) * i;
                float pitch = (float) Math.sin(ticks * 0.08f + i * 1.05f) * 0.75f;
                double cosP = Math.cos(pitch);
                double sinP = Math.sin(pitch);

                Vec3 vel = new Vec3(Math.cos(a) * 0.7 * cosP, sinP * 0.7, Math.sin(a) * 0.7 * cosP);
                ArborisBulletEntity bullet = new ArborisBulletEntity(
                        level, this.getX(), this.getY() + 5.0, this.getZ(), 1, 1.1f, vel, 200.0f, 1, 100);
                bullet.setOwner(this);
                level.addFreshEntity(bullet);
            }
        }

        // 5. 追尾虚空星 (40 ticksごとに1発、速度をマイルドにして機動で振り切れるように)
        if (ticks % 40 == 0) {
            double sx = this.getX() + (this.random.nextDouble() - 0.5) * 6.0;
            double sz = this.getZ() + (this.random.nextDouble() - 0.5) * 6.0;
            double sy = this.getY() + 9.0;

            Vec3 initVel = new Vec3((this.random.nextDouble() - 0.5) * 0.4, 0.35, (this.random.nextDouble() - 0.5) * 0.4);
            ArborisBulletEntity star = new ArborisBulletEntity(
                    level, sx, sy, sz, 3, 1.3f, initVel, 280.0f, 1, 140);
            star.setOwner(this);
            star.setHoming(true, target);
            level.addFreshEntity(star);
        }
    }

    /**
     * 半径40ブロックの円形結界を強制し、プレイヤーが外に出られないように封絶
     */
    private void enforceBarrier(Level level) {
        double centerX = this.spawnX != 0 ? this.spawnX : this.getX();
        double centerZ = this.spawnZ != 0 ? this.spawnZ : this.getZ();
        double maxRadius = BARRIER_RADIUS;

        AABB barrierBox = new AABB(centerX - maxRadius - 16.0, this.getY() - 32.0, centerZ - maxRadius - 16.0,
                                   centerX + maxRadius + 16.0, this.getY() + 128.0, centerZ + maxRadius + 16.0);
        List<Player> nearbyPlayers = level.getEntitiesOfClass(Player.class, barrierBox);

        for (Player player : nearbyPlayers) {
            double dx = player.getX() - centerX;
            double dz = player.getZ() - centerZ;
            double distSq = dx * dx + dz * dz;

            if (distSq > (maxRadius - 0.5) * (maxRadius - 0.5)) {
                double dist = Math.sqrt(distSq);
                if (dist < 0.0001) continue;

                // 境界を超えた、または境界に接触した場合：内側へ強制引き戻し
                double targetX = centerX + (dx / dist) * (maxRadius - 0.8);
                double targetZ = centerZ + (dz / dist) * (maxRadius - 0.8);

                // 速度の外向き成分を打ち消し、内向きのベクトルを与える
                Vec3 vel = player.getDeltaMovement();
                double outwardDot = (vel.x * (dx / dist) + vel.z * (dz / dist));
                if (outwardDot > 0) {
                    player.setDeltaMovement(vel.x - outwardDot * (dx / dist) * 1.5 - (dx / dist) * 0.25,
                                           vel.y,
                                           vel.z - outwardDot * (dz / dist) * 1.5 - (dz / dist) * 0.25);
                    player.hasImpulse = true;
                }

                player.teleportTo(targetX, player.getY(), targetZ);

                // 境界接触エフェクト & 音
                if (player.tickCount % 6 == 0) {
                    level.playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.2f, 1.8f);
                    level.playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.8f, 1.5f);

                    if (player instanceof ServerPlayer sp) {
                        sp.displayClientMessage(Component.literal("§c§l【原初の結界】§e 空間が封絶されている！ 結界の外へは出られない！"), true);
                    }
                }

                if (level instanceof ServerLevel sl) {
                    for (int i = 0; i < 4; i++) {
                        sl.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                                player.getX() + (this.random.nextDouble() - 0.5),
                                player.getY() + this.random.nextDouble() * 2.0,
                                player.getZ() + (this.random.nextDouble() - 0.5),
                                2, 0.1, 0.1, 0.1, 0.1);
                    }
                }
            }
        }
    }

    /**
     * 帰化 (Naturalize) を受けた際の処理。
     * 出現演出中・無敵時間中・HP残量に関係なく無条件で受理し、即死の代わりにラストスタンドへ突入する。
     * ラストスタンド中に受けた場合は結界耐久を1削る（同tick内の多重適用は無敵時間で抑制）。
     */
    public void onNaturalized(@javax.annotation.Nullable LivingEntity attacker) {
        if (this.level().isClientSide() || this.isRemoved()) return;

        // 帰化フラグが残っていると各種Mixinに死亡扱いされるため常にクリア
        this.getPersistentData().remove("nature_collapse.is_naturalized");
        try {
            ((INaturalizable) this).nature_collapse$setNaturalized(false);
        } catch (Throwable ignored) {
        }
        // 継続帰化エフェクトによる毎tick多重発動を防止
        try {
            this.removeEffect(NatureCollapse.ToNaturalEffect.get());
        } catch (Throwable ignored) {
        }

        if (this.isLastStand()) {
            if (this.invulnerableTicks > 0) return;
            // 帰化による大ダメージ: 結界耐久を一気に5ヒット分ブレイク！
            int hits = this.getLastStandHits() + 5;
            this.setLastStandHits(hits);
            this.invulnerableTicks = 10; // 0.5秒の無敵
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 2.0f, 1.8f);
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.ITEM_BREAK, SoundSource.HOSTILE, 2.0f, 1.2f);
            if (hits >= LAST_STAND_HITS_REQUIRED) {
                trulyDefeat();
            }
            return;
        }

        // 出現演出中だった場合は強制的に地上へ顕現させる
        if (this.isAwakening()) {
            this.setAwakening(false);
            double cx = this.spawnX != 0 ? this.spawnX : this.getX();
            double cz = this.spawnZ != 0 ? this.spawnZ : this.getZ();
            this.setPos(cx, this.getTargetY(), cz);
        }

        // 無敵時間を無視して即座にラストスタンドへ
        this.invulnerableTicks = 0;
        startLastStand();
    }

    public void startLastStand() {
        if (this.isLastStand()) return;
        this.setLastStand(true);
        this.setLastStandTicks(0);
        this.setLastStandHits(0);
        super.setHealth(1.0f);
        this.invulnerableTicks = 40; // 突入時2秒間の無敵硬直

        Level level = this.level();
        if (!level.isClientSide()) {
            // ラストスタンド突入時：プレイヤーの蓄積浸食値を安全圏(60)まで浄化・リセット
            for (Player player : level.players()) {
                if (player instanceof ServerPlayer sp) {
                    int erosion = sp.getPersistentData().getInt("arboris_erosion");
                    if (erosion > 60) {
                        sp.getPersistentData().putInt("arboris_erosion", 60);
                    }
                }
            }
            // 空間震撼・カメラシェイク
            NatureCollapse.CHANNEL.send(
                    PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(this.getX(), this.getY(), this.getZ(), 128.0, level.dimension())),
                    new CameraShakePacket(2.5f, 40)
            );

            // 終焉の雷鳴・轟音
            level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 4.0f, 0.5f);
            level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 3.5f, 0.4f);
            level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.HOSTILE, 3.0f, 0.7f);

            // 天空の終焉稲妻
            if (level instanceof ServerLevel sl) {
                for (int i = 0; i < 6; i++) {
                    double angle = (Math.PI * 2 / 6) * i;
                    net.minecraft.world.entity.LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(sl);
                    if (bolt != null) {
                        bolt.moveTo(this.getX() + Math.cos(angle) * 12.0, this.getY(), this.getZ() + Math.sin(angle) * 12.0);
                        sl.addFreshEntity(bolt);
                    }
                }
            }
        }
    }

    public void trulyDefeat() {
        if (this.trulyDefeated) return;
        this.trulyDefeated = true;
        Level level = this.level();
        if (!level.isClientSide()) {
            Component msg = Component.literal("§a§l【崩壊阻止】§2 原初の根源樹アルボリスは完全に鎮圧され、世界の崩壊は防がれた！ 創生の光となって昇華する...");
            for (Player player : level.players()) {
                player.sendSystemMessage(msg);
                player.getPersistentData().putInt("arboris_erosion", 0); // 浸食値の完全浄化
            }

            // 浄化の鐘・歓喜の音
            level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.HOSTILE, 3.0f, 1.0f);
            level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.BELL_RESONATE, SoundSource.HOSTILE, 3.0f, 0.8f);

            if (level instanceof ServerLevel sl) {
                // 超巨大浄化パーティクル爆轟
                sl.sendParticles(ParticleTypes.FLASH, this.getX(), this.getY() + 6.0, this.getZ(), 10, 2.0, 2.0, 2.0, 0.0);
                sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, this.getX(), this.getY() + 6.0, this.getZ(), 5, 3.0, 3.0, 3.0, 0.1);
                sl.sendParticles(ParticleTypes.END_ROD, this.getX(), this.getY() + 6.0, this.getZ(), 200, 4.0, 8.0, 4.0, 0.4);

                // アイテムドロップ (奇跡の花、奇跡の根)
                this.spawnAtLocation(new ItemStack(NatureCollapse.MiracleFlower.get()));
                this.spawnAtLocation(new ItemStack(NatureCollapse.MiracleRoot.get()));
            }

            this.bossEvent.setVisible(false);
            this.bossEvent.removeAllPlayers();
            this.discard();
        }
    }

    public void reflectDamage(DamageSource source, float amount) {
        Entity direct = source.getDirectEntity();
        Entity shooter = source.getEntity();

        // 飛翔体攻撃の場合: 速度加速反転 + 浸食値+5
        if (direct instanceof Projectile projectile) {
            Vec3 vel = projectile.getDeltaMovement();
            projectile.setDeltaMovement(vel.reverse().scale(2.5));
            projectile.hasImpulse = true;

            if (shooter instanceof ServerPlayer sp) {
                addErosion(sp, 5, "projectile_reflect");
            }

            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.SHIELD_BLOCK, SoundSource.HOSTILE, 2.5f, 1.6f);
            return;
        }

        // 近接または直接攻撃の場合: ダメージ反射
        if (shooter instanceof LivingEntity attacker && attacker != this) {
            float reflectDamage = Math.max(amount, HEART_STOP_DAMAGE);
            DamageSource reflectSrc = this.damageSources().thorns(this);
            attacker.hurt(reflectSrc, reflectDamage);

            if (attacker instanceof ServerPlayer sp) {
                addErosion(sp, 5, "melee_reflect");
            }

            this.level().playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(),
                    SoundEvents.THORNS_HIT, SoundSource.HOSTILE, 2.5f, 0.8f);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // 出現中は完全無敵（岩盤から突き破り中の儀式状態）
        if (this.isAwakening()) {
            return false;
        }

        // ラストスタンド中の処理
        if (this.isLastStand()) {
            // 無敵時間中はダメージ反射
            if (this.invulnerableTicks > 0) {
                reflectDamage(source, amount);
                return false;
            }

            // プレイヤーからの攻撃を受けた場合、ヒット数をカウントしてゲージを削る
            Entity direct = source.getDirectEntity();
            Entity shooter = source.getEntity();
            if (direct instanceof Player || shooter instanceof Player) {
                int hits = this.getLastStandHits() + 1;
                this.setLastStandHits(hits);
                this.invulnerableTicks = 6; // 0.3秒の短縮無敵でテンポよく攻撃可能に

                if (hits >= LAST_STAND_HITS_REQUIRED) {
                    trulyDefeat();
                    return true;
                }

                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 1.5f, 1.8f);
            }
            return false;
        }

        // 1. 無敵時間中 (2秒 = 40 ticks) の反射処理
        if (this.invulnerableTicks > 0) {
            reflectDamage(source, amount);
            return false;
        }

        // 2. ハートストップ (10000ダメージ上限、それ以上のダメージは10000固定)
        float cappedAmount = Math.min(amount, HEART_STOP_DAMAGE);

        // 3. 致死ダメージ判定: HPが削り切られる場合はラストスタンドへ突入
        if (this.getHealth() - cappedAmount <= 1.0f) {
            startLastStand();
            return true;
        }

        // 4. ダメージ適用
        boolean hurt = super.hurt(source, cappedAmount);
        if (hurt) {
            // 被弾後、2秒間 (40 ticks) の無敵反射モード突入
            this.invulnerableTicks = INVULNERABLE_DURATION;
            this.entityData.set(INVULNERABLE_ACTIVE, true);
        }

        return hurt;
    }

    @Override
    public void setHealth(float health) {
        if (this.trulyDefeated) {
            super.setHealth(health);
            return;
        }
        if (this.isAwakening()) {
            super.setHealth(ARBORIS_MAX_HEALTH);
            return;
        }
        if (this.isLastStand()) {
            super.setHealth(1.0f);
            return;
        }

        // 回復・HPリセット（現在値以上への変更）は上限1,000,000まで正常適用
        if (health >= this.getHealth()) {
            super.setHealth(Math.min(health, ARBORIS_MAX_HEALTH));
            return;
        }

        // 無敵時間中 (2秒 = 40 ticks) は外部からのHP強制減少を一切無効化
        if (this.invulnerableTicks > 0) {
            return;
        }

        // ハートストップ: 1回の攻撃でのHP減少量は最大でも HEART_STOP_DAMAGE (10,000) に制限
        float damage = this.getHealth() - health;
        if (damage > HEART_STOP_DAMAGE || Float.isNaN(damage) || Float.isInfinite(damage)) {
            damage = HEART_STOP_DAMAGE;
        }

        float newHealth = this.getHealth() - damage;

        // 致死判定: HPが1.0以下になる場合はラストスタンドへ突入
        if (newHealth <= 1.0f) {
            super.setHealth(1.0f);
            startLastStand();
            return;
        }

        // 通常ダメージ適用後、直ちに2秒間 (40 ticks) の無敵反射モードへ突入
        super.setHealth(newHealth);
        this.invulnerableTicks = INVULNERABLE_DURATION;
        this.entityData.set(INVULNERABLE_ACTIVE, true);
    }

    public static void addErosion(ServerPlayer player, int amount, String reason) {
        int current = player.getPersistentData().getInt("arboris_erosion");
        int next = current + amount;
        player.getPersistentData().putInt("arboris_erosion", next);

        int oldTier = current / 100;
        int newTier = next / 100;

        // 浸食値100ごとにチャットメッセージ表示
        if (newTier > oldTier && next < EROSION_MAX) {
            int tierNum = newTier * 100;
            int percent = newTier * 20;
            player.sendSystemMessage(Component.literal("§4§l【原初の浸食】 §c浸食値が " + tierNum + " に到達した... (侵食深度 " + percent + "%)"));
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.HOSTILE, 1.8f, 0.6f);
        }

        // 浸食値500で即死（無敵・防具貫通）
        if (next >= EROSION_MAX) {
            player.sendSystemMessage(Component.literal("§4§l【原初の断罪】 §c浸食値が臨界点 (" + EROSION_MAX + ") に達した！ 魂が原初の根源樹に呑み込まれ、完全消滅する..."));
            player.getPersistentData().putInt("arboris_erosion", 0);
            executeErosionInstantDeath(player);
        }
    }

    private static void executeErosionInstantDeath(ServerPlayer player) {
        Level level = player.level();
        if (level instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.SCULK_SOUL, player.getX(), player.getY() + 1.0, player.getZ(), 30, 0.5, 0.8, 0.5, 0.08);
            sl.sendParticles(ParticleTypes.FLASH, player.getX(), player.getY() + 1.0, player.getZ(), 3, 0.1, 0.1, 0.1, 0.0);
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WITHER_DEATH, SoundSource.HOSTILE, 3.0f, 0.5f);

        // あらゆる防具・無敵効果を貫通する即死処理
        try {
            ((INaturalizable) player).nature_collapse$instantKill(null, player.damageSources().fellOutOfWorld());
        } catch (Throwable ignored) {
        }

        // 生存していた場合の強制解除・キルフォールバック
        if (player.isAlive()) {
            try {
                player.setInvulnerable(false);
                player.invulnerableTime = 0;
                player.setHealth(0.0f);
                player.die(player.damageSources().fellOutOfWorld());
            } catch (Throwable ignored) {
            }
        }
    }

    private void updateBossBar(ServerLevel level) {
        if (this.isRemoved() || !this.isAlive()) {
            this.bossEvent.setVisible(false);
            this.bossEvent.removeAllPlayers();
            return;
        }

        if (this.isAwakening()) {
            int ticks = this.getAwakeningTicks();
            if (ticks < PRELUDE_DURATION) {
                // 前兆共鳴中 (5秒カウントダウン)
                int secToRise = Math.max(1, (PRELUDE_DURATION - ticks + 19) / 20);
                float prog = (float) ticks / (float) PRELUDE_DURATION;
                this.bossEvent.setName(Component.literal(String.format("共鳴の儀式  地上隆起まで %d秒", secToRise)));
                this.bossEvent.setProgress(Mth.clamp(prog, 0.0f, 1.0f));
                this.bossEvent.setColor(BossEvent.BossBarColor.YELLOW);
                this.bossEvent.setOverlay(BossEvent.BossBarOverlay.NOTCHED_6);
            } else {
                // 地上隆起中 (25秒カウントダウン)
                int remainingSec = Math.max(0, (AWAKENING_DURATION - ticks + 19) / 20);
                float prog = (float) (ticks - PRELUDE_DURATION) / (float) (AWAKENING_DURATION - PRELUDE_DURATION);
                this.bossEvent.setName(Component.literal(String.format("原初覚醒  完全顕現まで %d秒", remainingSec)));
                this.bossEvent.setProgress(Mth.clamp(prog, 0.0f, 1.0f));
                this.bossEvent.setColor(BossEvent.BossBarColor.RED);
                this.bossEvent.setOverlay(BossEvent.BossBarOverlay.NOTCHED_10);
            }
        } else if (this.isLastStand()) {
            int ticks = this.getLastStandTicks();
            int remainingSec = Math.max(0, (LAST_STAND_DURATION - ticks + 19) / 20);
            int remainingHits = Math.max(0, LAST_STAND_HITS_REQUIRED - this.getLastStandHits());
            float progTime = (float) (LAST_STAND_DURATION - ticks) / (float) LAST_STAND_DURATION;
            float progHits = (float) remainingHits / (float) LAST_STAND_HITS_REQUIRED;
            float finalProg = Math.min(progTime, progHits);

            this.bossEvent.setName(Component.literal(String.format("LAST STAND [ %02ds  |  結界耐久 %d ]", remainingSec, remainingHits)));
            this.bossEvent.setProgress(Mth.clamp(finalProg, 0.0f, 1.0f));
            this.bossEvent.setColor(BossEvent.BossBarColor.RED);
            this.bossEvent.setOverlay(BossEvent.BossBarOverlay.NOTCHED_20);
        } else {
            float healthRatio = this.getHealth() / this.getMaxHealth();
            this.bossEvent.setName(Component.literal("原初の根源樹 アルボリス"));
            this.bossEvent.setProgress(Mth.clamp(healthRatio, 0.0f, 1.0f));
            this.bossEvent.setColor(BossEvent.BossBarColor.GREEN);
            this.bossEvent.setOverlay(BossEvent.BossBarOverlay.PROGRESS);
        }

        // 周囲プレイヤーの更新
        AABB box = this.getBoundingBox().inflate(96.0);
        List<ServerPlayer> nearbyPlayers = level.getEntitiesOfClass(ServerPlayer.class, box);
        for (ServerPlayer player : level.players()) {
            if (nearbyPlayers.contains(player)) {
                this.bossEvent.addPlayer(player);
            } else {
                this.bossEvent.removePlayer(player);
            }
        }
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        // 天空を貫く光柱や超巨大魔方陣(半径24m)、周囲の公転多面体が見る方向によってカリングされないよう広大な描画範囲を確保
        return this.getBoundingBox().inflate(128.0, 384.0, 128.0);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distanceSq) {
        // 遠距離(最大256ブロック)からでも神聖幾何学演出や光柱が途切れないよう保証
        return distanceSq < 256.0 * 256.0;
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        if (!this.isRemoved() && this.isAlive()) {
            this.bossEvent.addPlayer(player);
        }
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        if (!this.trulyDefeated && (reason == RemovalReason.KILLED || reason == RemovalReason.DISCARDED)) {
            if (!this.isLastStand()) {
                startLastStand();
            }
            return;
        }
        super.remove(reason);
        this.bossEvent.setVisible(false);
        this.bossEvent.removeAllPlayers();
    }

    @Override
    public void onRemovedFromWorld() {
        super.onRemovedFromWorld();
        if (this.trulyDefeated) {
            this.bossEvent.setVisible(false);
            this.bossEvent.removeAllPlayers();
        }
    }

    @Override
    public void die(DamageSource cause) {
        if (!this.trulyDefeated) {
            this.dead = false;
            this.deathTime = 0;
            return;
        }
        super.die(cause);
        this.bossEvent.setVisible(false);
        this.bossEvent.removeAllPlayers();
    }



    @Override
    public boolean isPickable() {
        return !this.isRemoved();
    }

    @Override
    public Iterable<ItemStack> getArmorSlots() {
        return Collections.emptyList();
    }

    @Override
    public ItemStack getItemBySlot(net.minecraft.world.entity.EquipmentSlot slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public void setItemSlot(net.minecraft.world.entity.EquipmentSlot slot, ItemStack stack) {
    }

    @Override
    public net.minecraft.world.entity.HumanoidArm getMainArm() {
        return net.minecraft.world.entity.HumanoidArm.RIGHT;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Awakening", this.isAwakening());
        tag.putInt("AwakeningTicks", this.getAwakeningTicks());
        tag.putFloat("StartY", this.getStartY());
        tag.putFloat("TargetY", this.getTargetY());
        tag.putDouble("SpawnX", this.spawnX);
        tag.putDouble("SpawnZ", this.spawnZ);
        tag.putBoolean("LastStand", this.isLastStand());
        tag.putInt("LastStandTicks", this.getLastStandTicks());
        tag.putInt("LastStandHits", this.getLastStandHits());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setAwakening(tag.getBoolean("Awakening"));
        this.setAwakeningTicks(tag.getInt("AwakeningTicks"));
        this.setStartY(tag.getFloat("StartY"));
        this.setTargetY(tag.getFloat("TargetY"));
        this.spawnX = tag.getDouble("SpawnX");
        this.spawnZ = tag.getDouble("SpawnZ");
        this.setLastStand(tag.getBoolean("LastStand"));
        this.setLastStandTicks(tag.getInt("LastStandTicks"));
        this.setLastStandHits(tag.getInt("LastStandHits"));
        if (this.getHealth() <= 1024.0f && !this.isLastStand()) {
            this.setHealth(ARBORIS_MAX_HEALTH);
        }
    }
}
