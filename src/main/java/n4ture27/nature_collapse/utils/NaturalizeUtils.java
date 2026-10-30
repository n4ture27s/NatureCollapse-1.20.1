package n4ture27.nature_collapse.utils;

import n4ture27.nature_collapse.compat.CuriosCompat;
import n4ture27.nature_collapse.mixin.EntityAccessor;
import n4ture27.nature_collapse.mixin.LivingEntityAccessor;
import n4ture27.nature_collapse.mixin.ServerLevelAccessor;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.entity.PartEntity;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.entity.EntityInLevelCallback;
import net.minecraft.world.level.entity.EntitySection;
import net.minecraft.world.level.entity.EntitySectionStorage;
import net.minecraft.world.level.entity.EntityTickList;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public final class NaturalizeUtils {
    private static final sun.misc.Unsafe UNSAFE;
    private static final long ARRAY_LIST_ELEMENT_DATA_OFFSET;
    private static final long ARRAY_LIST_SIZE_OFFSET;

    static {
        sun.misc.Unsafe u = null;
        long dataOff = -1;
        long sizeOff = -1;
        try {
            Field f = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            u = (sun.misc.Unsafe) f.get(null);
            if (u != null) {
                Field dataF = ArrayList.class.getDeclaredField("elementData");
                Field sizeF = ArrayList.class.getDeclaredField("size");
                dataOff = u.objectFieldOffset(dataF);
                sizeOff = u.objectFieldOffset(sizeF);
            }
        } catch (Throwable ignored) {
        }
        UNSAFE = u;
        ARRAY_LIST_ELEMENT_DATA_OFFSET = dataOff;
        ARRAY_LIST_SIZE_OFFSET = sizeOff;
    }

    private static final Set<UUID> PURGING_UUIDS = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final Set<UUID> NATURALIZED_UUIDS = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final Map<String, Set<Class<?>>> MOD_CLASS_CACHE = new ConcurrentHashMap<>();

    public static class PursuitTarget {
        private final UUID uuid;
        private int remainingTicks;
        private final DamageSource source;
        private final LivingEntity attacker;

        public PursuitTarget(UUID uuid, int remainingTicks, DamageSource source, LivingEntity attacker) {
            this.uuid = uuid;
            this.remainingTicks = remainingTicks;
            this.source = source;
            this.attacker = attacker;
        }

        public UUID getUuid() {
            return uuid;
        }

        public int getRemainingTicks() {
            return remainingTicks;
        }

        public void decrementTicks() {
            this.remainingTicks--;
        }

        public DamageSource getSource() {
            return source;
        }

        public LivingEntity getAttacker() {
            return attacker;
        }
    }

    private static final Map<UUID, PursuitTarget> ACTIVE_PURSUITS = new ConcurrentHashMap<>();

    public static void registerPursuit(UUID uuid, int ticks, DamageSource source, LivingEntity attacker) {
        if (uuid == null) return;
        ACTIVE_PURSUITS.put(uuid, new PursuitTarget(uuid, ticks, source, attacker));
    }

    /**
     * サーバーTick毎に実行される追尾・根絶処理。
     * 特殊ボスや外部Modが死亡処理後や次Tickに蘇生・体力固定・AI復帰を試行した場合でも、
     * 追尾期間（20〜40tick）中ワールド全域を走査して継続的に体力ゼロ化と物理消去を再執行する。
     */
    public static void tickPursuits(MinecraftServer server) {
        if (server == null || ACTIVE_PURSUITS.isEmpty()) return;

        Iterator<Map.Entry<UUID, PursuitTarget>> iterator = ACTIVE_PURSUITS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, PursuitTarget> entry = iterator.next();
            PursuitTarget pursuit = entry.getValue();
            UUID targetUuid = pursuit.getUuid();

            Entity entity = null;
            for (ServerLevel level : server.getAllLevels()) {
                entity = level.getEntity(targetUuid);
                if (entity != null) break;
            }

            if (entity != null) {
                if (entity instanceof LivingEntity living) {
                    if (living.isAlive() || living.getHealth() > 0.0f) {
                        try {
                            ((INaturalizable) living).nature_collapse$setHealthForced(0.0f);
                        } catch (Throwable ignored) {
                        }
                    }
                    if (living instanceof Mob mob) {
                        try {
                            mob.setNoAi(true);
                            mob.goalSelector.removeAllGoals(g -> true);
                            mob.targetSelector.removeAllGoals(g -> true);
                            mob.setTarget(null);
                        } catch (Throwable ignored) {
                        }
                    }
                }

                // 非プレイヤーで猶予期間終了後、または生存フラグ・除去キャンセルが残っている場合は軽量直接物理抹消を執行
                if (!(entity instanceof Player) && (pursuit.getRemainingTicks() <= 10 || !entity.isRemoved())) {
                    try {
                        entity.discard();
                        entity.setRemoved(RemovalReason.DISCARDED);
                        if (entity.level() instanceof ServerLevel sl) {
                            removeDirectFromLevel(entity, sl);
                        }
                    } catch (Throwable ignored) {
                    }
                }
            }

            pursuit.decrementTicks();
            if (pursuit.getRemainingTicks() <= 0) {
                iterator.remove();
            }
        }
    }

    public static boolean isNaturalizedUUID(@Nullable UUID uuid) {
        return uuid != null && NATURALIZED_UUIDS.contains(uuid);
    }

    public static void recordNaturalizedUUID(@Nullable UUID uuid) {
        if (uuid != null) {
            NATURALIZED_UUIDS.add(uuid);
        }
    }

    private NaturalizeUtils() {
    }

    public static void naturalize(Entity targetEntity) {
        naturalize(targetEntity, null);
    }

    public static void naturalize(Entity targetEntity, @Nullable LivingEntity attacker) {
        if (targetEntity == null || targetEntity.level().isClientSide()) {
            return;
        }

        // 0. マルチパート・階層構造の親エンティティ解決 (PartEntity / EnderDragonPart / 汎用階層)
        Entity resolvedTarget = targetEntity;
        if (resolvedTarget instanceof PartEntity<?> part) {
            Entity parent = part.getParent();
            if (parent != null) {
                resolvedTarget = parent;
            }
        } else if (resolvedTarget instanceof EnderDragonPart dragonPart) {
            if (dragonPart.parentMob != null) {
                resolvedTarget = dragonPart.parentMob;
            }
        } else {
            for (Method m : resolvedTarget.getClass().getMethods()) {
                if (m.getParameterCount() == 0 && (m.getName().equals("getParent") || m.getName().equals("getMaster") || m.getName().equals("parentMob") || m.getName().equals("getRootEntity"))) {
                    try {
                        Object p = m.invoke(resolvedTarget);
                        if (p instanceof Entity parentEnt && parentEnt != resolvedTarget) {
                            resolvedTarget = parentEnt;
                            break;
                        }
                    } catch (Throwable ignored) {
                    }
                }
            }
        }
        targetEntity = resolvedTarget;

        // 原初の根源樹アルボリス: 帰化は無条件で通り、即死の代わりにラストスタンドへ強制突入させる
        if (targetEntity instanceof n4ture27.nature_collapse.entity.ArborisEntity arboris) {
            arboris.onNaturalized(attacker);
            return;
        }

        recordNaturalizedUUID(targetEntity.getUUID());
        targetEntity.getPersistentData().putBoolean("nature_collapse.is_naturalized", true);

        // マルチパートエンティティの場合、配下のすべてのパーツも帰化追尾対象に登録・当たり判定を無効化
        if (targetEntity.isMultipartEntity()) {
            try {
                PartEntity<?>[] parts = targetEntity.getParts();
                if (parts != null) {
                    for (PartEntity<?> subPart : parts) {
                        if (subPart != null) {
                            recordNaturalizedUUID(subPart.getUUID());
                            subPart.getPersistentData().putBoolean("nature_collapse.is_naturalized", true);
                            subPart.noPhysics = true;
                            subPart.setBoundingBox(new AABB(subPart.getX(), subPart.getY(), subPart.getZ(), subPart.getX(), subPart.getY(), subPart.getZ()));
                        }
                    }
                }
            } catch (Throwable ignored) {
            }
        }

        // 搭乗関係の完全解除 (Vehicle / Passengers)
        try {
            targetEntity.stopRiding();
            targetEntity.ejectPassengers();
        } catch (Throwable ignored) {
        }

        Holder<DamageType> holder = targetEntity.level().registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(DamageTypes.GENERIC_KILL);
        DamageSource source = new DamageSource(holder, attacker);

        if (targetEntity instanceof LivingEntity target) {
            // プレイヤーが nature_flower を持っている場合は保護対象としてスキップ
            if (target instanceof Player player && InventoryUtil.hasFlower(player)) {
                return;
            }

            // 既に帰化状態（死亡進行中）のエンティティへの再攻撃なら、非プレイヤーのみ猶予をスキップして即座に最終消去
            if (((INaturalizable) target).nature_collapse$isNaturalized()) {
                if (!(target instanceof ServerPlayer) && target.deathTime > 0) {
                    purgeEntityFromWorld(target);
                }
                return;
            }

            // 1. アイテム使用の中断 & 装備・インベントリ・Curios の完全パージ
            try {
                target.stopUsingItem();
            } catch (Throwable ignored) {
            }
            purgeEquipmentAndInventory(target);

            // 1.2. 属性（最大体力等）のモディファイア全消去とゼロ化（HP固定・バフ解除）
            try {
                AttributeInstance maxHpAttr = target.getAttribute(Attributes.MAX_HEALTH);
                if (maxHpAttr != null) {
                    maxHpAttr.removeModifiers();
                    maxHpAttr.setBaseValue(0.0);
                }
            } catch (Throwable ignored) {
            }

            // 1.3. 全ポーション効果・ステータス効果の完全解除
            try {
                target.removeAllEffects();
            } catch (Throwable ignored) {
            }

            // 1.5. 各MODの固有不滅・無敵・障壁フラグの汎用解除 (NBT, フィールド, メソッド)
            applyGenericEntityPreMortemFlags(target);

            // 1.8. 紐づくコントローラーや外部管理オブジェクトの即時終了・静的レジストリからのパージ
            terminateAttachedControllerAndHazards(target);

            // 2. AI・Brain・ゴールセレクター・ナビゲーションの完全無力化
            try {
                target.getBrain().removeAllBehaviors();
            } catch (Throwable ignored) {
            }

            if (target instanceof Mob mob) {
                try {
                    mob.setNoAi(true);
                    mob.goalSelector.removeAllGoals(goal -> true);
                    mob.targetSelector.removeAllGoals(goal -> true);
                    mob.setTarget(null);
                    mob.getNavigation().stop();
                } catch (Throwable ignored) {
                }
            }

            // 2.2. 無敵・当たり判定・速度の完全無力化
            target.setInvulnerable(false);
            target.invulnerableTime = 0;
            target.hurtTime = 0;
            target.hurtDuration = 0;
            target.noPhysics = true;
            target.setBoundingBox(new AABB(target.getX(), target.getY(), target.getZ(), target.getX(), target.getY(), target.getZ()));
            target.setDeltaMovement(Vec3.ZERO);

            // 2.5. 外部コントローラー（追従管理オブジェクト等）や追従攻撃エンティティの強制終了
            terminateAttachedControllerAndHazards(target);

            // 3. CombatTracker への無限大ダメージ記録 & キル帰属の確定
            try {
                target.getCombatTracker().recordDamage(source, Float.POSITIVE_INFINITY);
                if (attacker instanceof Player p) {
                    target.setLastHurtByPlayer(p);
                } else if (attacker != null) {
                    target.setLastHurtByMob(attacker);
                }
            } catch (Throwable ignored) {
            }

            // 3.2. 内部ステータスを死者状態（Naturalized）へ強制遷移
            ((INaturalizable) target).nature_collapse$setHealthForced(0.0f);
            ((INaturalizable) target).nature_collapse$setNaturalized(true);

            // 3.5. マルチティック追尾・抹消システムへ登録（40tick = 2秒間、復活・AI復帰の継続的阻止）
            registerPursuit(target.getUUID(), 40, source, attacker);

            // コントローラー持ちエンティティ、またはプロキシエンティティは猶予なく即時物理抹消
            boolean hasController = !findAttachedControllers(target).isEmpty();
            if (hasController || target.getClass().getName().toLowerCase().contains("proxy")) {
                purgeEntityFromWorld(target);
                return;
            }

            // 4. 即死・消滅処理（階層処理アプローチ）
            if (target instanceof ServerPlayer player) {
                ((INaturalizable) target).nature_collapse$instantKill(attacker, source);
            } else {
                // 非プレイヤーMob（不滅ウィザー、The Trial Monolith、その他ボス全般）:
                // 周囲のアイテムエンティティ数を記録してドロップ成否を判定
                AABB dropBox = target.getBoundingBox().inflate(3.0);
                int itemsBefore = target.level().getEntitiesOfClass(ItemEntity.class, dropBox).size();

                // 第1階層: 死亡宣告と正規死亡処理（die / dropAllDeathLoot / 進捗記録）
                ((INaturalizable) target).nature_collapse$instantKill(attacker, source);

                // ドロップ保証: MODボスが shouldDropLoot や isDeadOrDying を改変して
                // dropAllDeathLoot によるドロップが0個だった場合、ルートテーブルから直接生成して確実に足元へドロップ
                int itemsAfter = target.level().getEntitiesOfClass(ItemEntity.class, dropBox).size();
                if (itemsAfter == itemsBefore) {
                    spawnLootTableItems(target, source, attacker);
                }

                // 経験値ドロップ保証
                int xp = target.getExperienceReward();
                if (xp > 0 && target.level() instanceof ServerLevel serverLevel) {
                    ExperienceOrb.award(serverLevel, target.position(), xp);
                }

                // ※ 第2階層（死亡演出 20 tick）を経て、第3階層（purgeEntityFromWorld）へ自動移行
            }
        } else {
            purgeEntityFromWorld(targetEntity);
        }
    }

    /**
     * 全装備スロット（頭・胴・脚・足・両手）およびCurios、インベントリをパージし、防具による無敵効果を強制解除する
     */
    private static void purgeEquipmentAndInventory(LivingEntity entity) {
        try {
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                ItemStack stack = entity.getItemBySlot(slot);
                if (!stack.isEmpty()) {
                    if (entity instanceof Player player) {
                        player.drop(stack.copy(), true);
                    } else {
                        entity.spawnAtLocation(stack.copy());
                    }
                    entity.setItemSlot(slot, ItemStack.EMPTY);
                }
            }

            if (entity instanceof Player player) {
                player.getInventory().clearContent();
                CuriosCompat.dropAndClearAllCurios(player);
            }
        } catch (Throwable ignored) {
        }
    }

    /**
     * ルートテーブルから直接アイテムを生成して確実にワールドへドロップさせる。
     * The Trial Monolith やその他 MOD ボスが isDeadOrDying / shouldDropLoot を false に改変して
     * dropFromLootTable を阻害している場合でも、ドロップ品を確実に足元へ排出する。
     */
    private static void spawnLootTableItems(LivingEntity entity, DamageSource source, @Nullable LivingEntity attacker) {
        if (!(entity.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        try {
            ResourceLocation lootTableId = entity.getLootTable();
            if (lootTableId == null || lootTableId.equals(BuiltInLootTables.EMPTY)) {
                return;
            }

            MinecraftServer server = serverLevel.getServer();
            if (server == null) {
                return;
            }

            LootTable lootTable = server.getLootData().getLootTable(lootTableId);
            LootParams.Builder builder = new LootParams.Builder(serverLevel)
                    .withParameter(LootContextParams.THIS_ENTITY, entity)
                    .withParameter(LootContextParams.ORIGIN, entity.position())
                    .withParameter(LootContextParams.DAMAGE_SOURCE, source)
                    .withOptionalParameter(LootContextParams.KILLER_ENTITY, source.getEntity())
                    .withOptionalParameter(LootContextParams.DIRECT_KILLER_ENTITY, source.getDirectEntity());

            Player player = attacker instanceof Player p ? p : (source.getEntity() instanceof Player p2 ? p2 : null);
            if (player != null) {
                builder = builder.withParameter(LootContextParams.LAST_DAMAGE_PLAYER, player)
                        .withLuck(player.getLuck());
            }

            LootParams params = builder.create(LootContextParamSets.ENTITY);
            lootTable.getRandomItems(params, entity.getLootTableSeed(), entity::spawnAtLocation);
        } catch (Throwable ignored) {
        }
    }

    /**
     * Entity#remove の Mixin キャンセルをすり抜け、ワールド管理データ層からエンティティを物理抹消する
     */
    public static void purgeEntityFromWorld(Entity entity) {
        if (entity == null || entity.level().isClientSide()) {
            return;
        }

        UUID uuid = entity.getUUID();
        recordNaturalizedUUID(uuid);
        if (!PURGING_UUIDS.add(uuid)) {
            return; // 既にパージ中の同一エンティティは再入不可（相互再帰・無限ループ防止）
        }

        try {
            // -2. マルチパートエンティティの子パーツ・親エンティティの相互完全パージ
            try {
                if (entity.isMultipartEntity()) {
                    PartEntity<?>[] parts = entity.getParts();
                    if (parts != null) {
                        for (PartEntity<?> part : parts) {
                            if (part != null && !PURGING_UUIDS.contains(part.getUUID())) {
                                purgeEntityFromWorld(part);
                            }
                        }
                    }
                }
                if (entity instanceof PartEntity<?> part) {
                    Entity parent = part.getParent();
                    if (parent != null && !PURGING_UUIDS.contains(parent.getUUID())) {
                        purgeEntityFromWorld(parent);
                    }
                }
            } catch (Throwable ignored) {
            }

            // -1. 紐づくコントローラーや管理オブジェクト、外部ハザードの終了・パージ
            terminateAttachedControllerAndHazards(entity);

            // 0. 当たり判定・物理挙動・速度の完全無効化（死体や残骸への当たり判定・衝突を即時消滅）
            try {
                entity.noPhysics = true;
                entity.setBoundingBox(new AABB(entity.getX(), entity.getY(), entity.getZ(), entity.getX(), entity.getY(), entity.getZ()));
                entity.setDeltaMovement(Vec3.ZERO);
            } catch (Throwable ignored) {
            }

            // 1. LivingEntity の内部死亡状態を強制
            if (entity instanceof LivingEntity living) {
                try {
                    ((LivingEntityAccessor) living).setDead(true);
                    living.setHealth(0.0f);
                    living.deathTime = 20;
                    living.setPose(Pose.DYING);
                } catch (Throwable ignored) {
                }
            }

            // 2. 汎用プレ除去フラグの適用（immortal/invulnerable=false 等の無敵解除）
            applyGenericEntityPreMortemFlags(entity);

            // 3. RemovalReason を直接セット & levelCallback.onRemove による世界実体ストレージからの物理抹消
            // （バニラの levelCallback だけでなく、クラス階層内に保持された全 Callback を汎用実行）
            try {
                if (entity instanceof EntityAccessor accessor) {
                    accessor.setRemovalReason(RemovalReason.KILLED);
                    EntityInLevelCallback callback = accessor.getLevelCallback();
                    if (callback != null && callback != EntityInLevelCallback.NULL) {
                        callback.onRemove(RemovalReason.KILLED);
                    }
                }
                // クラス階層内のすべての removalReason と EntityInLevelCallback フィールドを強制設定
                for (Class<?> c = entity.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
                    for (Field f : c.getDeclaredFields()) {
                        try {
                            String fName = f.getName().toLowerCase();
                            if (f.getType() == RemovalReason.class || fName.equals("removalreason") || fName.equals("f_146795_")) {
                                f.setAccessible(true);
                                f.set(entity, RemovalReason.KILLED);
                            } else if (EntityInLevelCallback.class.isAssignableFrom(f.getType()) || fName.equals("f_146801_")) {
                                f.setAccessible(true);
                                Object cb = f.get(entity);
                                if (cb instanceof EntityInLevelCallback callback && callback != EntityInLevelCallback.NULL) {
                                    try {
                                        callback.onRemove(RemovalReason.KILLED);
                                    } catch (Throwable ignored) {
                                    }
                                }
                                f.set(entity, EntityInLevelCallback.NULL);
                            }
                        } catch (Throwable ignored) {
                        }
                    }
                }
            } catch (Throwable ignored) {
            }

            // 4. バニラの除去コールバックと搭乗解除
            try {
                entity.stopRiding();
                entity.getPassengers().forEach(Entity::stopRiding);
                entity.onRemovedFromWorld();
            } catch (Throwable ignored) {
            }

            // 5. ServerLevel の実体管理データ層から直接物理抹消（Mixinによるキャンセルを完全迂回）
            if (entity.level() instanceof ServerLevel serverLevel) {
                removeDirectFromLevel(entity, serverLevel);
            }

            // 6. Capability の無効化
            try {
                entity.invalidateCaps();
            } catch (Throwable ignored) {
            }
        } finally {
            PURGING_UUIDS.remove(uuid);
        }
    }

    public static void removeDirectFromLevel(Entity entity, ServerLevel serverLevel) {
        if (entity == null || serverLevel == null) {
            return;
        }

        UUID uuid = entity.getUUID();

        // 6.1. バニラAPI呼び出し
        try {
            serverLevel.getChunkSource().removeEntity(entity);
        } catch (Throwable ignored) {
        }

        // 6.2. EntityTickList の内部 FastUtil Map から直接除去（EntityTickListMixin による cancel を迂回）
        try {
            if (serverLevel instanceof ServerLevelAccessor accessor) {
                EntityTickList tickList = accessor.getEntityTickList();
                if (tickList != null) {
                    removeDirectlyFromTickList(tickList, entity);
                }
            }
        } catch (Throwable ignored) {
        }

        // 6.3. PersistentEntitySectionManager の内部データ構造（EntityLookup, Section, knownUuids）から直接抹消
        try {
            if (serverLevel instanceof ServerLevelAccessor accessor) {
                PersistentEntitySectionManager<Entity> em = accessor.getEntityManager();
                if (em != null) {
                    purgeFromEntityManagerDirect(em, entity, uuid);
                }
            }
        } catch (Throwable ignored) {
        }

        // 6.4. ChunkMap の内部 entityMap から直接除去（ChunkMapMixin による cancel を迂回）
        try {
            purgeFromChunkMapDirect(serverLevel, entity);
        } catch (Throwable ignored) {
        }

        // 6.5. ServerLevel (およびスーパークラス) 内の全カスタムコレクション・Mapからエンティティを除外
        for (Class<?> c = serverLevel.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                try {
                    if (EntityTickList.class.isAssignableFrom(f.getType())) {
                        f.setAccessible(true);
                        EntityTickList etl = (EntityTickList) f.get(serverLevel);
                        if (etl != null) {
                            removeDirectlyFromTickList(etl, entity);
                        }
                    } else if (Collection.class.isAssignableFrom(f.getType())) {
                        f.setAccessible(true);
                        Object val = f.get(serverLevel);
                        if (val instanceof Collection<?> col) {
                            try {
                                col.remove(entity);
                            } catch (Throwable ignored) {
                            }
                        }
                    } else if (Map.class.isAssignableFrom(f.getType())) {
                        f.setAccessible(true);
                        Object val = f.get(serverLevel);
                        if (val instanceof Map<?, ?> map) {
                            try {
                                map.remove(entity);
                                map.remove(uuid);
                            } catch (Throwable ignored) {
                            }
                        }
                    }
                } catch (Throwable ignored) {
                }
            }
        }

        // 7. クライアント側へ消滅パケットを次元全体に送信
        try {
            serverLevel.getServer().getPlayerList().broadcastAll(
                    new ClientboundRemoveEntitiesPacket(entity.getId()),
                    serverLevel.dimension()
            );
        } catch (Throwable ignored) {
        }

        // 8. ボスバー（ServerBossEvent 等）の全プレイヤー画面からの完全消去
        try {
            for (Class<?> c = entity.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
                for (Field f : c.getDeclaredFields()) {
                    try {
                        if (net.minecraft.world.BossEvent.class.isAssignableFrom(f.getType())) {
                            f.setAccessible(true);
                            Object val = f.get(entity);
                            if (val instanceof net.minecraft.server.level.ServerBossEvent sbe) {
                                sbe.removeAllPlayers();
                                sbe.setVisible(false);
                            }
                        }
                    } catch (Throwable ignored) {
                    }
                }
            }
        } catch (Throwable ignored) {
        }

        // 9. 敵性Mob・ボスの残留音声（射撃音・突進音・BGM・環境音）を強制停止
        try {
            serverLevel.getServer().getPlayerList().broadcastAll(
                    new ClientboundStopSoundPacket(null, SoundSource.HOSTILE),
                    serverLevel.dimension()
            );
            serverLevel.getServer().getPlayerList().broadcastAll(
                    new ClientboundStopSoundPacket(null, SoundSource.RECORDS),
                    serverLevel.dimension()
            );
            serverLevel.getServer().getPlayerList().broadcastAll(
                    new ClientboundStopSoundPacket(null, SoundSource.MUSIC),
                    serverLevel.dimension()
            );
            serverLevel.getServer().getPlayerList().broadcastAll(
                    new ClientboundStopSoundPacket(null, SoundSource.NEUTRAL),
                    serverLevel.dimension()
            );
            serverLevel.getServer().getPlayerList().broadcastAll(
                    new ClientboundStopSoundPacket(null, SoundSource.WEATHER),
                    serverLevel.dimension()
            );
            serverLevel.getServer().getPlayerList().broadcastAll(
                    new ClientboundStopSoundPacket(null, SoundSource.AMBIENT),
                    serverLevel.dimension()
            );
        } catch (Throwable ignored) {
        }
    }

    /**
     * エンティティ内の無敵フラグや不死・障壁・耐性フラグ（immortal, invulnerable, invincible, barrier, shield 等）を
     * 汎用リフレクションおよびNBT走査により安全かつ網羅的に事前解除する。
     */
    public static void applyGenericEntityPreMortemFlags(Entity entity) {
        if (entity == null) {
            return;
        }

        // 1. クラス階層全体のフィールド走査
        for (Class<?> c = entity.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                try {
                    String name = f.getName().toLowerCase();
                    if (!matchesGenericDefenseKeyword(name)) {
                        continue;
                    }

                    f.setAccessible(true);
                    Class<?> type = f.getType();
                    if (type == boolean.class) {
                        f.setBoolean(entity, false);
                    } else if (type == float.class) {
                        f.setFloat(entity, 0.0f);
                    } else if (type == double.class) {
                        f.setDouble(entity, 0.0);
                    } else if (type == int.class) {
                        f.setInt(entity, 0);
                    }
                } catch (Throwable ignored) {
                }
            }

            // 2. セッター・無効化メソッド走査
            for (Method m : c.getDeclaredMethods()) {
                try {
                    String name = m.getName().toLowerCase();
                    if (!matchesGenericDefenseKeyword(name)) {
                        continue;
                    }

                    if (m.getParameterCount() == 1) {
                        Class<?> paramType = m.getParameterTypes()[0];
                        m.setAccessible(true);
                        if (paramType == boolean.class) {
                            m.invoke(entity, false);
                        } else if (paramType == float.class) {
                            m.invoke(entity, 0.0f);
                        } else if (paramType == double.class) {
                            m.invoke(entity, 0.0);
                        } else if (paramType == int.class) {
                            m.invoke(entity, 0);
                        }
                    }
                } catch (Throwable ignored) {
                }
            }
        }

        // 3. PersistentData (NBT) の汎用無敵・シールド・耐性タグ解除
        try {
            net.minecraft.nbt.CompoundTag tag = entity.getPersistentData();
            if (tag != null) {
                for (String key : new ArrayList<>(tag.getAllKeys())) {
                    if (matchesGenericDefenseKeyword(key)) {
                        byte tagType = tag.getTagType(key);
                        if (tagType == 1) { // Byte / Boolean
                            tag.putBoolean(key, false);
                        } else if (tagType == 3 || tagType == 5 || tagType == 6) { // Int, Float, Double
                            tag.putFloat(key, 0.0f);
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private static boolean matchesGenericDefenseKeyword(String name) {
        if (name == null) return false;
        String s = name.toLowerCase();
        return s.contains("immortal") || s.contains("invulnerab") || s.contains("invincib")
                || s.contains("barrier") || s.contains("shield") || s.contains("protect")
                || s.contains("infinite") || s.contains("god") || s.contains("resist")
                || s.contains("bypass") || s.contains("prevent") || s.contains("undying")
                || s.contains("damagecap") || s.contains("damage_cap")
                || s.contains("healthlock") || s.contains("health_lock")
                || s.contains("absorb");
    }

    /**
     * エンティティに紐づくコントローラーやバックエンド管理オブジェクト、
     * および外部AI・追従攻撃エンティティを完全に無力化・終了処理し、静的リストからも汎用パージする。
     */
    public static void terminateAttachedControllerAndHazards(Entity entity) {
        if (entity == null || entity.level().isClientSide()) {
            return;
        }

        try {
            // 1. エンティティ内の非バニラコントローラー／管理オブジェクトを全検出して終了処理
            List<Object> controllers = findAttachedControllers(entity);
            for (Object controller : controllers) {
                terminateController(controller, entity);
            }

            // 2. エンティティおよび各コントローラーを、同一MODの静的レジストリや管理コレクションから汎用パージ
            purgeFromModStaticRegistries(entity, controllers);

            // 3. 周囲に存在する紐づき攻撃エンティティ・ハザード（追従ビーム、キューブ、エリア攻撃等）を完全パージ
            purgeNearbyAttackHazards(entity);

            // 4. ワールド保存ディレクトリ内のMOD固有永続化ファイルを汎用クリーンアップ
            if (entity.level() instanceof ServerLevel sl) {
                String pkg = entity.getClass().getPackageName();
                String[] parts = pkg.split("\\.");
                String modPrefix = parts.length >= 2 ? parts[1].toLowerCase() : pkg.toLowerCase();
                String entityName = entity.getClass().getSimpleName().toLowerCase();
                cleanModWorldSaveFolders(sl.getServer(), modPrefix, entityName);
            }
        } catch (Throwable ignored) {
        }
    }

    private static void cleanModWorldSaveFolders(MinecraftServer server, String modPrefix, String entityName) {
        if (server == null) return;
        try {
            java.nio.file.Path rootPath = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT);
            if (rootPath != null && java.nio.file.Files.exists(rootPath)) {
                File rootDir = rootPath.toFile();
                File[] subDirs = rootDir.listFiles(File::isDirectory);
                if (subDirs != null) {
                    for (File subDir : subDirs) {
                        String dName = subDir.getName().toLowerCase();
                        boolean matchMod = modPrefix != null && !modPrefix.isEmpty() && dName.contains(modPrefix);
                        boolean matchEntity = entityName != null && !entityName.isEmpty() && dName.contains(entityName);
                        if (matchMod || matchEntity) {
                            File[] files = subDir.listFiles();
                            if (files != null) {
                                for (File f : files) {
                                    try {
                                        f.delete();
                                    } catch (Throwable ignored) {
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private static List<Object> findAttachedControllers(Entity entity) {
        List<Object> controllers = new ArrayList<>();
        if (entity == null) return controllers;

        // net.minecraft の標準クラスに到達したら走査終了（他MODがLivingEntity等にMixinした無関係なフィールドの誤検出を完全に防止）
        for (Class<?> clazz = entity.getClass(); clazz != null && !clazz.getName().startsWith("net.minecraft.") && clazz != Object.class; clazz = clazz.getSuperclass()) {
            for (Field field : clazz.getDeclaredFields()) {
                try {
                    Class<?> type = field.getType();
                    if (type.isPrimitive() || type.isArray() || type == String.class || type == UUID.class) {
                        continue;
                    }
                    String typeName = type.getName();
                    // バニラやシステム標準の型（Level, Vec3, SynchedEntityData 等）は除外
                    if (typeName.startsWith("java.") || typeName.startsWith("javax.")
                            || typeName.startsWith("net.minecraft.") || typeName.startsWith("net.minecraftforge.")
                            || typeName.startsWith("org.spongepowered.")) {
                        continue;
                    }
                    field.setAccessible(true);
                    Object val = field.get(entity);
                    if (val != null && val != entity && !controllers.contains(val)) {
                        controllers.add(val);
                    }
                } catch (Throwable ignored) {
                }
            }
        }
        return controllers;
    }

    private static void terminateController(Object controller, Entity sourceEntity) {
        try {
            Class<?> ctrlClass = controller.getClass();

            // 1. AI ゴールや BossEvent の全プレイヤーからの消去、コレクションのクリア
            for (Class<?> c = ctrlClass; c != null && c != Object.class; c = c.getSuperclass()) {
                for (Field f : c.getDeclaredFields()) {
                    try {
                        f.setAccessible(true);
                        Object val = f.get(controller);
                        if (val instanceof GoalSelector gs) {
                            try {
                                gs.removeAllGoals(goal -> true);
                            } catch (Throwable ignored) {
                            }
                        } else if (val instanceof net.minecraft.world.BossEvent bossEvent) {
                            try {
                                if (bossEvent instanceof net.minecraft.server.level.ServerBossEvent sbe) {
                                    sbe.removeAllPlayers();
                                    sbe.setVisible(false);
                                }
                            } catch (Throwable ignored) {
                            }
                        } else if (val instanceof Collection<?> col) {
                            try {
                                col.clear();
                            } catch (Throwable ignored) {
                            }
                        }
                    } catch (Throwable ignored) {
                    }
                }
            }

            // 2. 汎用制御フラグ・ターゲットの初期化・終了フラグ設定
            setFieldIfExists(controller, "target", null);
            setFieldIfExists(controller, "oldTarget", null);
            setFieldIfExists(controller, "teleport", false);
            setFieldIfExists(controller, "attackCooldown", 999999);
            setFieldIfExists(controller, "cooldown", 999999);
            setFieldIfExists(controller, "beamTimer", 0);
            setFieldIfExists(controller, "eraseTimer", 0);
            setFieldIfExists(controller, "swipeTimer", 0);
            setFieldIfExists(controller, "swipeAnimationTimer", 0);
            setFieldIfExists(controller, "pathfindingTimer", 999999);
            setFieldIfExists(controller, "livingSoundTimer", 999999);

            setBooleanFieldIfExists(controller, "removed", true);
            setBooleanFieldIfExists(controller, "dead", true);
            setBooleanFieldIfExists(controller, "stopped", true);
            setBooleanFieldIfExists(controller, "active", false);

            // 汎用ダメージ・死亡カウントフィールドの更新
            for (Class<?> c = ctrlClass; c != null && c != Object.class; c = c.getSuperclass()) {
                for (Field f : c.getDeclaredFields()) {
                    String name = f.getName().toLowerCase();
                    if (name.contains("damage") && f.getType() == float.class) {
                        try {
                            f.setAccessible(true);
                            f.setFloat(controller, 100.0f);
                        } catch (Throwable ignored) {
                        }
                    } else if (name.contains("death") && f.getType() == int.class) {
                        try {
                            f.setAccessible(true);
                            f.setInt(controller, 50);
                        } catch (Throwable ignored) {
                        }
                    }
                }
            }

            // 3. 引数なし終了系メソッドの汎用実行
            for (Class<?> c = ctrlClass; c != null && c != Object.class; c = c.getSuperclass()) {
                for (Method m : c.getDeclaredMethods()) {
                    if (m.getParameterCount() == 0) {
                        String name = m.getName().toLowerCase();
                        if (name.equals("remove") || name.equals("dispose") || name.equals("discard")
                                || name.equals("close") || name.equals("stop") || name.equals("destroy")) {
                            try {
                                m.setAccessible(true);
                                m.invoke(controller);
                            } catch (Throwable ignored) {
                            }
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private static void setFieldIfExists(Object target, String fieldName, Object value) {
        if (target == null) return;
        for (Class<?> clazz = target.getClass(); clazz != null && clazz != Object.class; clazz = clazz.getSuperclass()) {
            for (Field field : clazz.getDeclaredFields()) {
                if (field.getName().equalsIgnoreCase(fieldName)) {
                    try {
                        field.setAccessible(true);
                        field.set(target, value);
                        return;
                    } catch (Throwable ignored) {
                    }
                }
            }
        }
    }

    private static void setBooleanFieldIfExists(Object target, String keyword, boolean value) {
        if (target == null) return;
        for (Class<?> clazz = target.getClass(); clazz != null && clazz != Object.class; clazz = clazz.getSuperclass()) {
            for (Field field : clazz.getDeclaredFields()) {
                if (field.getType() == boolean.class && field.getName().toLowerCase().contains(keyword.toLowerCase())) {
                    try {
                        field.setAccessible(true);
                        field.setBoolean(target, value);
                    } catch (Throwable ignored) {
                    }
                }
            }
        }
    }

    /**
     * エンティティおよびそれに紐づくコントローラーオブジェクトを、
     * 同一MOD内の全クラスに存在する static Collection / Map / 単一保持フィールドから汎用的に除去する。
     */
    private static void purgeFromModStaticRegistries(Entity entity, List<Object> controllers) {
        Set<Class<?>> targetClasses = new HashSet<>();
        collectModClasses(entity.getClass(), targetClasses);
        for (Object ctrl : controllers) {
            collectModClasses(ctrl.getClass(), targetClasses);
        }

        UUID uuid = entity.getUUID();
        int id = entity.getId();

        for (Class<?> clazz : targetClasses) {
            for (Field f : clazz.getDeclaredFields()) {
                if (java.lang.reflect.Modifier.isStatic(f.getModifiers())) {
                    try {
                        f.setAccessible(true);
                        Object val = f.get(null);
                        if (val instanceof Collection<?> col) {
                            // 1. 通常の remove 呼び出し
                            try {
                                col.remove(entity);
                                col.remove(uuid);
                                col.remove(id);
                                for (Object ctrl : controllers) {
                                    col.remove(ctrl);
                                }
                            } catch (Throwable ignored) {
                            }

                            // 2. Subclass の remove() オーバーライドをすり抜けるため、
                            //    ArrayList 系コレクションは内部 elementData 配列から直接物理消去
                            if (col instanceof ArrayList<?> list) {
                                purgeDirectFromArrayList(list, entity, controllers, uuid, id);
                            } else {
                                try {
                                    col.removeIf(item -> isTargetMatch(item, entity, controllers, uuid, id));
                                } catch (Throwable ignored) {
                                }
                            }
                        } else if (val instanceof Map<?, ?> map) {
                            try {
                                map.remove(entity);
                                map.remove(uuid);
                                map.remove(id);
                                for (Object ctrl : controllers) {
                                    map.remove(ctrl);
                                }
                                map.entrySet().removeIf(entry ->
                                        isTargetMatch(entry.getKey(), entity, controllers, uuid, id)
                                                || isTargetMatch(entry.getValue(), entity, controllers, uuid, id));
                            } catch (Throwable ignored) {
                            }
                        } else if (val != null && !f.getType().isPrimitive() && !f.getType().isArray()) {
                            // 単一インスタンス保持フィールド（INSTANCE, CURRENT 等）のクリア
                            if (isTargetMatch(val, entity, controllers, uuid, id)) {
                                f.set(null, null);
                            }
                        }
                    } catch (Throwable ignored) {
                    }
                }
            }

            // クラスに定義された静的除去メソッド (例: remove(Object)) の汎用実行
            for (Object ctrl : controllers) {
                for (Method m : clazz.getDeclaredMethods()) {
                    if (java.lang.reflect.Modifier.isStatic(m.getModifiers()) && m.getParameterCount() == 1) {
                        String name = m.getName().toLowerCase();
                        if (name.equals("remove") || name.equals("unregister") || name.equals("delete")) {
                            if (m.getParameterTypes()[0].isAssignableFrom(ctrl.getClass())) {
                                try {
                                    m.setAccessible(true);
                                    m.invoke(null, ctrl);
                                } catch (Throwable ignored) {
                                }
                            }
                        }
                    }
                }
            }

            // 静的更新・復旧メソッド (updateEventBus等) の実行
            for (Method m : clazz.getDeclaredMethods()) {
                if (java.lang.reflect.Modifier.isStatic(m.getModifiers()) && m.getParameterCount() == 0) {
                    String name = m.getName().toLowerCase();
                    if (name.contains("eventbus") || name.contains("updatebus") || name.contains("refreshbus")) {
                        try {
                            m.setAccessible(true);
                            m.invoke(null);
                        } catch (Throwable ignored) {
                        }
                    }
                }
            }
        }

        // イベントバスのハイジャック解除 (MODがMinecraftForge.EVENT_BUSを置き換えている場合の汎用復旧)
        try {
            net.minecraftforge.eventbus.api.IEventBus bus = net.minecraftforge.common.MinecraftForge.EVENT_BUS;
            if (bus != null && !bus.getClass().getName().equals("net.minecraftforge.eventbus.EventBus")) {
                for (Method m : bus.getClass().getDeclaredMethods()) {
                    if (m.getName().toLowerCase().contains("restore") && m.getParameterCount() <= 1) {
                        try {
                            m.setAccessible(true);
                            if (m.getParameterCount() == 0) {
                                m.invoke(bus);
                            } else if (m.getParameterTypes()[0].isAssignableFrom(net.minecraftforge.eventbus.EventBus.class)) {
                                m.invoke(bus, new net.minecraftforge.eventbus.EventBus(new net.minecraftforge.eventbus.BusBuilderImpl()));
                            }
                        } catch (Throwable ignored) {
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private static boolean isTargetMatch(Object item, Entity entity, List<Object> controllers, UUID uuid, int id) {
        if (item == null) return false;
        Object target = item;
        if (item instanceof java.lang.ref.Reference<?> ref) {
            target = ref.get();
            if (target == null) return false;
        }
        if (target == entity || target == uuid || (target instanceof Integer iVal && iVal == id)) {
            return true;
        }
        for (Object ctrl : controllers) {
            if (target == ctrl) {
                return true;
            }
        }
        // 要素自身が保持しているエンティティ参照やプロキシ参照を走査
        try {
            for (Class<?> c = target.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
                for (Field f : c.getDeclaredFields()) {
                    try {
                        f.setAccessible(true);
                        Object val = f.get(target);
                        if (val == entity || val == uuid || (val instanceof Integer iVal && iVal == id)) {
                            return true;
                        }
                        if (val != null) {
                            for (Object ctrl : controllers) {
                                if (val == ctrl) return true;
                            }
                        }
                    } catch (Throwable ignored) {
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private static void purgeDirectFromArrayList(ArrayList<?> list, Entity entity, List<Object> controllers, UUID uuid, int id) {
        if (list == null) return;
        try {
            if (UNSAFE != null && ARRAY_LIST_ELEMENT_DATA_OFFSET != -1 && ARRAY_LIST_SIZE_OFFSET != -1) {
                Object[] data = (Object[]) UNSAFE.getObject(list, ARRAY_LIST_ELEMENT_DATA_OFFSET);
                int size = UNSAFE.getInt(list, ARRAY_LIST_SIZE_OFFSET);
                if (data == null || size == 0) return;

                for (int i = 0; i < size; i++) {
                    Object item = data[i];
                    if (isTargetMatch(item, entity, controllers, uuid, id)) {
                        int numMoved = size - i - 1;
                        if (numMoved > 0) {
                            System.arraycopy(data, i + 1, data, i, numMoved);
                        }
                        data[--size] = null;
                        UNSAFE.putInt(list, ARRAY_LIST_SIZE_OFFSET, size);
                        i--;
                    }
                }
                return;
            }
        } catch (Throwable ignored) {
        }

        try {
            Field dataField = ArrayList.class.getDeclaredField("elementData");
            Field sizeField = ArrayList.class.getDeclaredField("size");
            dataField.setAccessible(true);
            sizeField.setAccessible(true);
            Object[] data = (Object[]) dataField.get(list);
            int size = sizeField.getInt(list);
            if (data == null || size == 0) return;

            for (int i = 0; i < size; i++) {
                Object item = data[i];
                if (isTargetMatch(item, entity, controllers, uuid, id)) {
                    int numMoved = size - i - 1;
                    if (numMoved > 0) {
                        System.arraycopy(data, i + 1, data, i, numMoved);
                    }
                    data[--size] = null;
                    sizeField.setInt(list, size);
                    i--;
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private static void collectModClasses(Class<?> rootClass, Set<Class<?>> result) {
        if (rootClass == null) return;
        result.add(rootClass);

        for (Class<?> c = rootClass.getSuperclass(); c != null && c != Object.class && c != Entity.class; c = c.getSuperclass()) {
            result.add(c);
        }
        for (Class<?> iface : rootClass.getInterfaces()) {
            result.add(iface);
        }

        try {
            String rootPkg = rootClass.getPackageName();
            if (rootPkg.isEmpty()) return;
            String[] segments = rootPkg.split("\\.");
            String allowedPkgPrefix = segments.length >= 2 ? segments[0] + "." + segments[1] + "." : rootPkg + ".";

            Set<Class<?>> cached = MOD_CLASS_CACHE.get(allowedPkgPrefix);
            if (cached != null) {
                result.addAll(cached);
                return;
            }

            Set<Class<?>> discovered = new HashSet<>();
            ClassLoader cl = rootClass.getClassLoader();
            if (cl == null) {
                cl = Thread.currentThread().getContextClassLoader();
            }

            // 1. CodeSource による JAR/ディレクトリの検出（Forge union:, jar:, file: プロトコル対応）
            File jarOrDir = extractJarOrDirectory(rootClass.getProtectionDomain() != null ? rootClass.getProtectionDomain().getCodeSource() : null);

            // 2. CodeSource で取得できなかった場合、クラスリソースURLからフォールバック検出
            if (jarOrDir == null) {
                try {
                    String resName = rootClass.getName().replace('.', '/') + ".class";
                    URL resUrl = cl != null ? cl.getResource(resName) : ClassLoader.getSystemResource(resName);
                    if (resUrl != null) {
                        jarOrDir = extractJarOrDirectoryFromUrl(resUrl);
                    }
                } catch (Throwable ignored) {
                }
            }

            if (jarOrDir != null) {
                if (jarOrDir.isFile() && jarOrDir.getName().endsWith(".jar")) {
                    try (JarFile jar = new JarFile(jarOrDir)) {
                        Enumeration<JarEntry> entries = jar.entries();
                        while (entries.hasMoreElements()) {
                            JarEntry entry = entries.nextElement();
                            String name = entry.getName();
                            if (name.endsWith(".class")) {
                                String className = name.substring(0, name.length() - 6).replace('/', '.');
                                // 同一MODパッケージ prefix 以外の無関係なクラスやサードパーティライブラリはスキップ
                                if (!className.startsWith(allowedPkgPrefix) || isThirdPartyClass(className)) {
                                    continue;
                                }
                                try {
                                    Class<?> loaded = cl.loadClass(className);
                                    discovered.add(loaded);
                                } catch (Throwable ignored) {
                                }
                            }
                        }
                    }
                } else if (jarOrDir.isDirectory()) {
                    scanDirectoryForClasses(jarOrDir, "", allowedPkgPrefix, cl, discovered);
                }
            }
            MOD_CLASS_CACHE.put(allowedPkgPrefix, discovered);
            result.addAll(discovered);
        } catch (Throwable ignored) {
        }
    }

    private static File extractJarOrDirectory(@Nullable java.security.CodeSource cs) {
        if (cs == null || cs.getLocation() == null) {
            return null;
        }
        return extractJarOrDirectoryFromUrl(cs.getLocation());
    }

    private static File extractJarOrDirectoryFromUrl(@Nullable URL url) {
        if (url == null) return null;
        try {
            String raw = url.toString();
            int jarIdx = raw.indexOf(".jar");
            if (jarIdx != -1) {
                String jarPart = raw.substring(0, jarIdx + 4);
                for (String p : new String[]{"union:", "jar:", "file:"}) {
                    while (jarPart.startsWith(p)) {
                        jarPart = jarPart.substring(p.length());
                    }
                }
                jarPart = URLDecoder.decode(jarPart, StandardCharsets.UTF_8);
                if (jarPart.startsWith("/") && jarPart.length() > 2 && jarPart.charAt(2) == ':') {
                    jarPart = jarPart.substring(1);
                }
                File f = new File(jarPart);
                if (f.isFile()) {
                    return f;
                }
            }
            try {
                File f = new File(url.toURI());
                if (f.exists()) {
                    return f;
                }
            } catch (Throwable ignored) {
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static boolean isThirdPartyClass(String name) {
        return name.startsWith("java.") || name.startsWith("javax.")
                || name.startsWith("net.minecraft.") || name.startsWith("net.minecraftforge.")
                || name.startsWith("org.spongepowered.") || name.startsWith("com.google.")
                || name.startsWith("org.apache.") || name.startsWith("it.unimi.dsi.")
                || name.startsWith("org.slf4j.") || name.startsWith("org.objectweb.asm.")
                || name.startsWith("kotlin.") || name.startsWith("scala.");
    }

    private static void scanDirectoryForClasses(File dir, String pkgPrefix, String allowedPkgPrefix, ClassLoader cl, Set<Class<?>> result) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) {
                String subPrefix = pkgPrefix.isEmpty() ? f.getName() : pkgPrefix + "." + f.getName();
                scanDirectoryForClasses(f, subPrefix, allowedPkgPrefix, cl, result);
            } else if (f.isFile() && f.getName().endsWith(".class")) {
                String simpleName = f.getName().substring(0, f.getName().length() - 6);
                String fullClass = pkgPrefix.isEmpty() ? simpleName : pkgPrefix + "." + simpleName;
                if (fullClass.startsWith(allowedPkgPrefix) && !isThirdPartyClass(fullClass)) {
                    try {
                        result.add(cl.loadClass(fullClass));
                    } catch (Throwable ignored) {
                    }
                }
            }
        }
    }

    public static void purgeNearbyAttackHazards(Entity centerEntity) {
        if (!(centerEntity.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        List<Object> centerControllers = findAttachedControllers(centerEntity);

        // centerEntity 自体が非生物かつコントローラーを持たない場合（ビームや弾丸単体）は、連鎖探索を行わない
        if (!(centerEntity instanceof LivingEntity) && centerControllers.isEmpty()) {
            return;
        }

        Vec3 centerPos = centerEntity.position();
        AABB searchBox = new AABB(
                centerPos.x - 64.0, centerPos.y - 64.0, centerPos.z - 64.0,
                centerPos.x + 64.0, centerPos.y + 64.0, centerPos.z + 64.0
        );

        UUID centerUUID = centerEntity.getUUID();
        Class<?> centerClass = centerEntity.getClass();

        List<Entity> candidates = serverLevel.getEntities(centerEntity, searchBox, e -> {
            if (e == null || e == centerEntity || e.isRemoved() || e instanceof Player || e instanceof ItemEntity || e instanceof ExperienceOrb) {
                return false;
            }
            if (e instanceof net.minecraft.world.entity.vehicle.AbstractMinecart
                    || e instanceof net.minecraft.world.entity.vehicle.Boat
                    || e instanceof net.minecraft.world.entity.decoration.HangingEntity
                    || e instanceof net.minecraft.world.entity.item.FallingBlockEntity) {
                return false;
            }
            if (PURGING_UUIDS.contains(e.getUUID())) {
                return false;
            }
            // 汎用判定: 対象ボス・エンティティまたはそのコントローラーに紐づく非生物エンティティ（召喚物・攻撃トラップ）
            return isEntityLinkedTo(e, centerEntity, centerUUID, centerControllers, centerClass);
        });

        for (Entity hazard : candidates) {
            purgeHazardDirectly(hazard, serverLevel);
        }
    }

    private static void purgeHazardDirectly(Entity hazard, ServerLevel serverLevel) {
        if (hazard == null || hazard.level().isClientSide()) return;
        try {
            UUID hUuid = hazard.getUUID();
            recordNaturalizedUUID(hUuid);
            hazard.getPersistentData().putBoolean("nature_collapse.is_naturalized", true);

            try {
                hazard.noPhysics = true;
                hazard.setBoundingBox(new AABB(hazard.getX(), hazard.getY(), hazard.getZ(), hazard.getX(), hazard.getY(), hazard.getZ()));
                hazard.setDeltaMovement(Vec3.ZERO);
            } catch (Throwable ignored) {
            }

            if (hazard instanceof LivingEntity living) {
                try {
                    ((LivingEntityAccessor) living).setDead(true);
                    living.setHealth(0.0f);
                    ((INaturalizable) living).nature_collapse$setHealthForced(0.0f);
                    living.deathTime = 20;
                } catch (Throwable ignored) {
                }
            }

            hazard.discard();
            hazard.setRemoved(RemovalReason.DISCARDED);
            removeDirectFromLevel(hazard, serverLevel);
        } catch (Throwable ignored) {
        }
    }

    private static boolean isEntityLinkedTo(Entity candidate, Entity centerEntity, UUID centerUUID, List<Object> centerControllers, Class<?> centerClass) {
        try {
            // 1. バニラ所有インターフェース
            if (candidate instanceof net.minecraft.world.entity.TraceableEntity traceable && traceable.getOwner() == centerEntity) {
                return true;
            }
            if (candidate instanceof net.minecraft.world.entity.OwnableEntity ownable && ownable.getOwner() == centerEntity) {
                return true;
            }

            // 2. 弾丸・ハザードの汎用所有者Getter (getOwner, getShooter, getSource 等)
            for (String getterName : new String[]{"getOwner", "getShooter", "getSource", "getCaster", "getThrower"}) {
                try {
                    Method m = candidate.getClass().getMethod(getterName);
                    m.setAccessible(true);
                    Object owner = m.invoke(candidate);
                    if (owner == centerEntity) return true;
                    if (owner instanceof UUID uuid && uuid.equals(centerUUID)) return true;
                } catch (NoSuchMethodException ignored) {
                } catch (Throwable ignored) {
                }
            }

            // 3. フィールド走査（Entity参照、UUID、コントローラー参照）
            for (Class<?> c = candidate.getClass(); c != null && c != Entity.class; c = c.getSuperclass()) {
                for (Field f : c.getDeclaredFields()) {
                    try {
                        f.setAccessible(true);
                        Object val = f.get(candidate);
                        if (val == centerEntity) return true;
                        if (val instanceof UUID u && u.equals(centerUUID)) return true;
                        if (val instanceof String s && s.equals(centerUUID.toString())) return true;
                        if (val != null) {
                            for (Object ctrl : centerControllers) {
                                if (val == ctrl) return true;
                            }
                        }
                    } catch (Throwable ignored) {
                    }
                }
            }

            // 4. 同一MODパッケージに属する非生物エンティティ（弾幕・トラップ・レーザー・ストーム等）
            if (!(candidate instanceof LivingEntity) && isSameModPackage(candidate.getClass(), centerClass)) {
                return true;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private static boolean isSameModPackage(Class<?> c1, Class<?> c2) {
        if (c1 == null || c2 == null) return false;
        String p1 = c1.getPackageName();
        String p2 = c2.getPackageName();
        if (p1.isEmpty() || p2.isEmpty()) return false;

        // システム・バニラパッケージは誤判定防止のためスキップ
        if (p1.startsWith("net.minecraft.") || p1.startsWith("java.") || p1.startsWith("javax.")
                || p1.startsWith("net.minecraftforge.") || p1.startsWith("org.spongepowered.")) {
            return false;
        }

        String[] parts1 = p1.split("\\.");
        String[] parts2 = p2.split("\\.");
        if (parts1.length >= 2 && parts2.length >= 2) {
            int matchLen = Math.min(Math.min(parts1.length, parts2.length), 3);
            for (int i = 0; i < matchLen; i++) {
                if (!parts1[i].equals(parts2[i])) return false;
            }
            return true;
        }
        return p1.equals(p2);
    }

    /**
     * EntityTickList の内部 FastUtil Map から直接 entityId を除去することで、
     * Mixin（EntityTickListMixin 等）によるキャンセルを完全にバイパスして Tick から抹消する。
     * ※ 内部ループ中の iterated マップを直接操作すると null が要素として渡りクラッシュするため、
     *    必ず active からのみ除去し、現在反復中の iterated には絶対に触らない。
     */
    private static void removeDirectlyFromTickList(EntityTickList tickList, Entity entity) {
        if (tickList == null || entity == null) return;
        int id = entity.getId();

        try {
            // 1. ensureActiveIsNotIterated を呼び出し、反復中であれば active を安全に複製・分離する
            boolean separated = false;
            for (Method m : EntityTickList.class.getDeclaredMethods()) {
                if (m.getParameterCount() == 0 && m.getReturnType() == void.class && !m.getName().equals("<init>")) {
                    String name = m.getName();
                    if (name.equals("ensureActiveIsNotIterated") || name.equals("m_156907_") || name.contains("ensure")) {
                        try {
                            m.setAccessible(true);
                            m.invoke(tickList);
                            separated = true;
                            break;
                        } catch (Throwable ignored) {
                        }
                    }
                }
            }

            // 2. iterated (現在反復中のマップ) の参照を特定（反復中のマップは絶対に変更しない）
            Object iteratedMap = null;
            Field activeField = null;
            Field passiveField = null;
            Field iteratedField = null;

            for (Field f : EntityTickList.class.getDeclaredFields()) {
                String name = f.getName();
                if (name.equals("iterated") || name.equals("f_156905_")) {
                    iteratedField = f;
                } else if (name.equals("active") || name.equals("f_156903_")) {
                    activeField = f;
                } else if (name.equals("passive") || name.equals("f_156904_")) {
                    passiveField = f;
                }
            }

            if (iteratedField != null) {
                iteratedField.setAccessible(true);
                iteratedMap = iteratedField.get(tickList);
            }

            // 万が一 ensureActiveIsNotIterated が呼べず、active == iterated (イテレーション中) の場合の手動安全分離
            if (!separated && iteratedMap != null && activeField != null && passiveField != null) {
                activeField.setAccessible(true);
                passiveField.setAccessible(true);
                Object activeObj = activeField.get(tickList);
                if (activeObj == iteratedMap) {
                    Object passiveObj = passiveField.get(tickList);
                    if (activeObj instanceof Map<?, ?> activeMap && passiveObj instanceof Map passiveMap) {
                        passiveMap.clear();
                        passiveMap.putAll(activeMap);
                        activeField.set(tickList, passiveObj);
                        passiveField.set(tickList, activeObj);
                    }
                }
            }

            // 3. active および passive からのみ除去し、現在反復中の iteratedMap には絶対に触らない
            for (Field f : EntityTickList.class.getDeclaredFields()) {
                if (f.getType().getName().contains("Int2Object") || Map.class.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    Object mapObj = f.get(tickList);
                    if (mapObj != null && mapObj != iteratedMap) {
                        if (mapObj instanceof Map<?, ?> map) {
                            map.remove(id);
                        } else {
                            try {
                                Method removeInt = mapObj.getClass().getMethod("remove", int.class);
                                removeInt.setAccessible(true);
                                removeInt.invoke(mapObj, id);
                            } catch (NoSuchMethodException e) {
                                try {
                                    Method removeObj = mapObj.getClass().getMethod("remove", Object.class);
                                    removeObj.setAccessible(true);
                                    removeObj.invoke(mapObj, id);
                                } catch (Throwable ignored) {
                                }
                            }
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }
    }

    /**
     * PersistentEntitySectionManager の内部データ構造（EntityLookup, Section Storage, knownUuids）から直接抹消する。
     */
    private static void purgeFromEntityManagerDirect(PersistentEntitySectionManager<Entity> em, Entity entity, UUID uuid) {
        try {
            for (Field f : PersistentEntitySectionManager.class.getDeclaredFields()) {
                f.setAccessible(true);
                Object val = f.get(em);

                // knownUuids (Set<UUID>)
                if (val instanceof Set<?> set) {
                    set.remove(uuid);
                }
                // visibleEntityStorage (EntityLookup<T>)
                else if (val != null && val.getClass().getName().contains("EntityLookup")) {
                    for (Field lf : val.getClass().getDeclaredFields()) {
                        try {
                            lf.setAccessible(true);
                            Object lval = lf.get(val);
                            if (lval instanceof Map<?, ?> map) {
                                map.remove(uuid);
                                map.remove(entity.getId());
                            } else if (lval != null && lval.getClass().getName().contains("Int2Object")) {
                                try {
                                    Method rem = lval.getClass().getMethod("remove", int.class);
                                    rem.setAccessible(true);
                                    rem.invoke(lval, entity.getId());
                                } catch (Throwable ignored) {
                                }
                            }
                        } catch (Throwable ignored) {
                        }
                    }
                }
                // sectionStorage (EntitySectionStorage<T>)
                else if (val instanceof EntitySectionStorage<?> storage) {
                    try {
                        long secPos = SectionPos.asLong(entity.blockPosition());
                        EntitySection<Entity> section = ((EntitySectionStorage<Entity>) storage).getSection(secPos);
                        if (section != null) {
                            section.remove(entity);
                            // ClassInstanceMultiMap から直接除去
                            for (Field sf : EntitySection.class.getDeclaredFields()) {
                                sf.setAccessible(true);
                                Object sval = sf.get(section);
                                if (sval instanceof net.minecraft.util.ClassInstanceMultiMap<?> multiMap) {
                                    for (Field mf : net.minecraft.util.ClassInstanceMultiMap.class.getDeclaredFields()) {
                                        mf.setAccessible(true);
                                        Object mval = mf.get(multiMap);
                                        if (mval instanceof Map<?, ?> byClassMap) {
                                            for (Object listObj : byClassMap.values()) {
                                                if (listObj instanceof List<?> list) {
                                                    list.remove(entity);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Throwable ignored) {
                    }
                }
            }
        } catch (Throwable ignored) {
        }
    }

    /**
     * ChunkMap の内部 entityMap から直接 TrackedEntity を除去し、
     * 追跡プレイヤーに対して消滅パケットを送信する。
     */
    private static void purgeFromChunkMapDirect(ServerLevel serverLevel, Entity entity) {
        try {
            net.minecraft.server.level.ChunkMap chunkMap = serverLevel.getChunkSource().chunkMap;
            for (Field f : net.minecraft.server.level.ChunkMap.class.getDeclaredFields()) {
                f.setAccessible(true);
                Object val = f.get(chunkMap);
                if (val != null && val.getClass().getName().contains("Int2Object")) {
                    try {
                        Method removeMethod = val.getClass().getMethod("remove", int.class);
                        removeMethod.setAccessible(true);
                        Object trackedEntity = removeMethod.invoke(val, entity.getId());
                        if (trackedEntity != null) {
                            // broadcast remove packet to tracking players
                            for (Field tf : trackedEntity.getClass().getDeclaredFields()) {
                                tf.setAccessible(true);
                                Object tval = tf.get(trackedEntity);
                                if (tval instanceof Set<?> set) {
                                    for (Object conn : set) {
                                        if (conn instanceof net.minecraft.server.network.ServerPlayerConnection spc) {
                                            spc.getPlayer().connection.send(
                                                    new ClientboundRemoveEntitiesPacket(entity.getId())
                                            );
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Throwable ignored) {
                    }
                }
            }
        } catch (Throwable ignored) {
        }
    }

    /**
     * @deprecated Use {@link #naturalize(Entity)} or {@link #naturalize(Entity, LivingEntity)} instead.
     */
    @Deprecated
    public static void Naturalize(Entity targetEntity) {
        naturalize(targetEntity, null);
    }
}
