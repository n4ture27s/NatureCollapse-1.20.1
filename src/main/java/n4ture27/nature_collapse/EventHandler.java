package n4ture27.nature_collapse;

import n4ture27.nature_collapse.utils.CloverUtil;
import n4ture27.nature_collapse.utils.INaturalizable;
import n4ture27.nature_collapse.utils.InventoryUtil;
import n4ture27.nature_collapse.utils.NaturalizeUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.List;

@Mod.EventBusSubscriber(modid = NatureCollapse.ModId, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class EventHandler {

    public static final java.util.UUID FLOWER_SPEED_MODIFIER_UUID = java.util.UUID.fromString("6a782b14-49c7-43cf-bf29-43c3f912e52a");
    public static final String FLOWER_SPEED_MODIFIER_NAME = "nature_collapse:flower_speed";

    @Nullable
    public static InventoryUtil.FlowerConfig getActiveFlowerConfig(Player player) {
        return InventoryUtil.getActiveFlowerConfig(player);
    }

    public static void updatePlayerMobilityDirect(Player player) {
        if (player == null || player.level().isClientSide()) return;
        InventoryUtil.FlowerConfig cfg = getActiveFlowerConfig(player);
        updatePlayerMobility(player, cfg);
    }

    private static void updatePlayerMobility(Player player, @Nullable InventoryUtil.FlowerConfig cfg) {
        // 1. 移動速度 (Speed Boost) の適用 / 解除
        AttributeInstance speedAttr = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttr != null) {
            AttributeModifier existing = speedAttr.getModifier(FLOWER_SPEED_MODIFIER_UUID);
            boolean speedActive = cfg != null && cfg.speedEnabled() && cfg.speedMult() > 1.0f;
            if (speedActive) {
                double desiredAmount = (double) (cfg.speedMult() - 1.0f);
                if (existing == null || Math.abs(existing.getAmount() - desiredAmount) > 0.001) {
                    if (existing != null) {
                        speedAttr.removeModifier(FLOWER_SPEED_MODIFIER_UUID);
                    }
                    speedAttr.addTransientModifier(new AttributeModifier(
                            FLOWER_SPEED_MODIFIER_UUID,
                            FLOWER_SPEED_MODIFIER_NAME,
                            desiredAmount,
                            AttributeModifier.Operation.MULTIPLY_TOTAL
                    ));
                }
            } else if (existing != null) {
                speedAttr.removeModifier(FLOWER_SPEED_MODIFIER_UUID);
            }
        }

        // 2. 飛行アビリティ & 飛行速度 (Flight & Flying Speed) の適用 / 解除
        boolean shouldFly = cfg != null && cfg.flightEnabled() && Config.flowerFlightEnabled;
        float targetSpeed = shouldFly ? cfg.flightSpeedH() : 0.05f;
        boolean needsAbilityUpdate = false;

        if (shouldFly) {
            if (!player.getAbilities().mayfly) {
                player.getAbilities().mayfly = true;
                needsAbilityUpdate = true;
            }
            if (Math.abs(player.getAbilities().getFlyingSpeed() - targetSpeed) > 0.0001f) {
                player.getAbilities().setFlyingSpeed(targetSpeed);
                needsAbilityUpdate = true;
            }
        } else {
            if (!player.isCreative() && !player.isSpectator()) {
                if (player.getAbilities().mayfly) {
                    player.getAbilities().mayfly = false;
                    player.getAbilities().flying = false;
                    needsAbilityUpdate = true;
                }
            }
            if (Math.abs(player.getAbilities().getFlyingSpeed() - 0.05f) > 0.0001f) {
                player.getAbilities().setFlyingSpeed(0.05f);
                needsAbilityUpdate = true;
            }
        }

        if (needsAbilityUpdate) {
            player.onUpdateAbilities();
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerAttackTargetHighest(AttackEntityEvent event) {
        Player player = event.getEntity();
        if (player != null && !player.level().isClientSide()) {
            ItemStack held = player.getMainHandItem();
            if (held.getItem() instanceof n4ture27.nature_collapse.item.NaturaLizeSword) {
                net.minecraft.world.entity.Entity target = event.getTarget();
                if (target != null) {
                    NaturalizeUtils.naturalize(target, player);
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onAttackedEventHighest(LivingAttackEvent ev) {
        if (ev.getEntity() instanceof Player player && InventoryUtil.hasInvulnerableFlower(player)) {
            ev.setCanceled(true);
        }

        DamageSource src = ev.getSource();
        if (src != null && src.getEntity() instanceof Player attacker && !attacker.level().isClientSide()) {
            ItemStack held = attacker.getMainHandItem();
            if (held.getItem() instanceof n4ture27.nature_collapse.item.NaturaLizeSword) {
                LivingEntity target = ev.getEntity();
                if (target != null && target != attacker) {
                    NaturalizeUtils.naturalize(target, attacker);
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onAttackedEventLowest(LivingAttackEvent ev) {
        if (ev.getEntity() instanceof Player player && InventoryUtil.hasInvulnerableFlower(player)) {
            ev.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onDamageEventHighest(LivingDamageEvent ev) {
        if (ev.getEntity() instanceof Player player && InventoryUtil.hasInvulnerableFlower(player)) {
            ev.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDamageEventLowest(LivingDamageEvent ev) {
        if (ev.getEntity() instanceof Player player && InventoryUtil.hasInvulnerableFlower(player)) {
            ev.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onHurtEventHighest(LivingHurtEvent ev) {
        if (ev.getEntity() instanceof Player player && InventoryUtil.hasInvulnerableFlower(player)) {
            ev.setCanceled(true);
        }

        DamageSource src = ev.getSource();
        if (src != null && src.getEntity() instanceof Player attacker && !attacker.level().isClientSide()) {
            ItemStack held = attacker.getMainHandItem();
            if (held.getItem() instanceof n4ture27.nature_collapse.item.NaturaLizeSword) {
                LivingEntity target = ev.getEntity();
                if (target != null && target != attacker) {
                    NaturalizeUtils.naturalize(target, attacker);
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onHurtEventLowest(LivingHurtEvent ev) {
        if (ev.getEntity() instanceof Player player && InventoryUtil.hasInvulnerableFlower(player)) {
            ev.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onDeathEventHighest(LivingDeathEvent ev) {
        if (ev.getEntity() instanceof Player player && InventoryUtil.hasInvulnerableFlower(player)) {
            ev.setCanceled(true);
            player.setHealth(player.getMaxHealth());
            player.removeAllEffects();
            player.deathTime = 0;
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onDeathEventLowest(LivingDeathEvent ev) {
        if (ev.getEntity() instanceof Player player && InventoryUtil.hasInvulnerableFlower(player)) {
            ev.setCanceled(true);
            player.setHealth(player.getMaxHealth());
            player.removeAllEffects();
            player.deathTime = 0;
            return;
        }

        // 帰化状態（Naturalized）のエンティティ（他MODのSoul Protection持ちプレイヤー等含む）への即死処理
        // 他MODが HIGHEST 等でキャンセルした場合でも、最優先でキャンセルを解除して死亡を貫通・確定させる
        LivingEntity living = ev.getEntity();
        if (living != null && ((INaturalizable) living).nature_collapse$isNaturalized()) {
            ev.setCanceled(false);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntityJoinLevel(EntityJoinLevelEvent ev) {
        net.minecraft.world.entity.Entity entity = ev.getEntity();
        if (entity == null || entity instanceof Player) {
            return;
        }

        // 帰化・抹消済みエンティティの再スポーン・再参加（他MODの自動リスポーン処理等）を汎用的に完全阻止
        if (NaturalizeUtils.isNaturalizedUUID(entity.getUUID())
                || (entity instanceof LivingEntity living && ((INaturalizable) living).nature_collapse$isNaturalized())
                || entity.getPersistentData().getBoolean("nature_collapse.is_naturalized")) {
            ev.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onTick(TickEvent.PlayerTickEvent ev) {
        Player player = ev.player;
        if (ev.phase == TickEvent.Phase.START && !player.level().isClientSide()) {
            tickPlayerFlowerDirect(player);
        }
    }

    public static void tickPlayerFlowerDirect(Player player) {
        if (player == null || player.level().isClientSide()) return;

        // 同一 tick 内での重複実行を防止
        int currentTick = player.tickCount;
        if (player.getPersistentData().getInt("nature_collapse.last_flower_tick") == currentTick) {
            return;
        }
        player.getPersistentData().putInt("nature_collapse.last_flower_tick", currentTick);

        InventoryUtil.FlowerConfig cfg = getActiveFlowerConfig(player);
        if (cfg == null) return;

        // 0. 無敵状態の確実な維持
        if (cfg.invulEnabled()) {
            if (player.getHealth() < player.getMaxHealth()) {
                player.setHealth(player.getMaxHealth());
            }
            if (player.deathTime > 0) {
                player.deathTime = 0;
            }
        }

        // 1. エフェクト自動付与
        if (cfg.effectsEnabled()) {
            InventoryUtil.forEachFlower(player, stack -> applyFlowerEffects(player, stack));
        }

        // 2. 飽食・満腹度維持
        if (cfg.saturationEnabled() && Config.flowerSaturationEnabled) {
            var food = player.getFoodData();
            if (food.getFoodLevel() < 20 || food.getSaturationLevel() < 20.0f) {
                food.setFoodLevel(20);
                food.setSaturation(20.0f);
            }
        }

        // 3. 移動能力（速度・飛行）の適用
        updatePlayerMobility(player, cfg);

        // 4. 磁石（ドロップアイテム＆経験値吸引）
        if (cfg.magnetEnabled()) {
            double r = cfg.magnetRange();
            AABB magnetBox = player.getBoundingBox().inflate(r);
            Vec3 pPos = player.position().add(0, 0.5, 0);

            List<ItemEntity> items = player.level().getEntitiesOfClass(ItemEntity.class, magnetBox);
            for (ItemEntity item : items) {
                if (item.isAlive() && !item.hasPickUpDelay()) {
                    Vec3 diff = pPos.subtract(item.position());
                    if (diff.lengthSqr() > 0.05) {
                        item.setDeltaMovement(diff.normalize().scale(0.45));
                    }
                }
            }

            List<ExperienceOrb> orbs = player.level().getEntitiesOfClass(ExperienceOrb.class, magnetBox);
            for (ExperienceOrb orb : orbs) {
                if (orb.isAlive()) {
                    Vec3 diff = pPos.subtract(orb.position());
                    if (diff.lengthSqr() > 0.05) {
                        orb.setDeltaMovement(diff.normalize().scale(0.45));
                    }
                }
            }
        }

        // 5. 帰化オーラ（周囲敵対Mob・ボスの自動消滅）
        if (cfg.auraEnabled()) {
            double r = cfg.auraRange();
            AABB auraBox = player.getBoundingBox().inflate(r);

            // LivingEntity（Mob、Monster、Boss等全般）の自動帰化消滅
            List<LivingEntity> livingEntities = player.level().getEntitiesOfClass(
                    LivingEntity.class,
                    auraBox,
                    target -> target != player
                            && !(target instanceof Player other && InventoryUtil.hasFlower(other))
                            && !(target instanceof net.minecraft.world.entity.OwnableEntity ownable && ownable.getOwner() == player)
                            && !target.isRemoved()
                            && !((INaturalizable) target).nature_collapse$isNaturalized()
            );
            for (LivingEntity target : livingEntities) {
                NaturalizeUtils.naturalize(target, player);
            }

            // 飛来する投射物の消滅
            List<Projectile> projectiles = player.level().getEntitiesOfClass(Projectile.class, auraBox);
            for (Projectile proj : projectiles) {
                if (proj.isAlive() && proj.getOwner() != player) {
                    proj.discard();
                }
            }

            // 環境ハザード・残留攻撃ゾーン（残留効果雲、エヴォーカーの牙、設置型トラップ等）の自動浄化
            List<net.minecraft.world.entity.Entity> hazardEntities = player.level().getEntities(
                    player,
                    auraBox,
                    target -> target != player
                            && !(target instanceof LivingEntity)
                            && !(target instanceof Projectile)
                            && !(target instanceof ItemEntity)
                            && !(target instanceof ExperienceOrb)
                            && !(target instanceof net.minecraft.world.entity.vehicle.AbstractMinecart)
                            && !(target instanceof net.minecraft.world.entity.vehicle.Boat)
                            && !(target instanceof net.minecraft.world.entity.decoration.HangingEntity)
                            && !(target instanceof net.minecraft.world.entity.item.FallingBlockEntity)
                            && !target.isRemoved()
            );
            for (net.minecraft.world.entity.Entity hazard : hazardEntities) {
                if (hazard instanceof net.minecraft.world.entity.TraceableEntity traceable && traceable.getOwner() == player) {
                    continue;
                }
                NaturalizeUtils.purgeEntityFromWorld(hazard);
            }
        }

        // 6. クリア視界（暗視）
        if (cfg.visionEnabled()) {
            MobEffectInstance vision = player.getEffect(MobEffects.NIGHT_VISION);
            if (vision == null || vision.getDuration() <= 220) {
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0, false, false));
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onFlightEnforcement(TickEvent.PlayerTickEvent ev) {
        Player player = ev.player;
        if (ev.phase == TickEvent.Phase.END && !player.level().isClientSide()) {
            InventoryUtil.FlowerConfig cfg = getActiveFlowerConfig(player);
            updatePlayerMobility(player, cfg);
        }
    }

    private static boolean isProtectedByFlower(Player player) {
        return InventoryUtil.hasFlower(player) && !((INaturalizable) player).nature_collapse$isNaturalized();
    }

    private static void applyFlowerEffects(Player player, ItemStack flower) {
        CompoundTag tag = flower.getTag();
        if (tag == null || !tag.contains(InventoryUtil.TAG_SELECTED_EFFECTS, Tag.TAG_LIST)) {
            return;
        }

        ListTag list = tag.getList(InventoryUtil.TAG_SELECTED_EFFECTS, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag effectData = list.getCompound(i);
            ResourceLocation id = ResourceLocation.tryParse(effectData.getString(InventoryUtil.TAG_EFFECT_ID));
            if (id == null) continue;

            MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(id);
            if (effect == null) continue;

            int level = Mth.clamp(effectData.getInt(InventoryUtil.TAG_EFFECT_LEVEL),
                    InventoryUtil.MIN_EFFECT_LEVEL, InventoryUtil.MAX_EFFECT_LEVEL);
            int amplifier = level - 1;

            int configuredDuration = effectData.contains(InventoryUtil.TAG_EFFECT_DURATION, Tag.TAG_ANY_NUMERIC)
                    ? Mth.clamp(effectData.getInt(InventoryUtil.TAG_EFFECT_DURATION),
                    InventoryUtil.MIN_EFFECT_DURATION, InventoryUtil.MAX_EFFECT_DURATION)
                    : InventoryUtil.DEFAULT_EFFECT_DURATION;

            int targetDuration = Math.max(configuredDuration, 300);
            int refreshThreshold = Math.min(220, targetDuration - 40);

            MobEffectInstance existing = player.getEffect(effect);
            if (existing == null || existing.getAmplifier() != amplifier || existing.getDuration() <= refreshThreshold) {
                player.addEffect(new MobEffectInstance(effect, targetDuration, amplifier, false, false));
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onEffectApplicable(MobEffectEvent.Applicable ev) {
        LivingEntity entity = ev.getEntity();
        MobEffectInstance effectInstance = ev.getEffectInstance();
        if (entity instanceof Player player && isProtectedByFlower(player)) {
            InventoryUtil.FlowerConfig cfg = getActiveFlowerConfig(player);
            if (cfg != null && cfg.invulEnabled()) {
                ResourceLocation effectId = ForgeRegistries.MOB_EFFECTS.getKey(effectInstance.getEffect());
                if (effectId != null) {
                    if (effectInstance.getEffect().getCategory() == MobEffectCategory.HARMFUL
                            && !isEffectAllowedByFlower(player, effectId)) {
                        ev.setResult(Event.Result.DENY);
                    }
                }
            }
        }
    }

    private static boolean isEffectAllowedByFlower(Player player, ResourceLocation effectId) {
        String targetId = effectId.toString();
        boolean[] allowed = {false};
        InventoryUtil.forEachFlower(player, stack -> {
            if (allowed[0] || !stack.hasTag()) return;
            CompoundTag tag = stack.getTag();
            if (tag != null && tag.contains(InventoryUtil.TAG_SELECTED_EFFECTS, Tag.TAG_LIST)) {
                ListTag list = tag.getList(InventoryUtil.TAG_SELECTED_EFFECTS, Tag.TAG_COMPOUND);
                for (int j = 0; j < list.size(); j++) {
                    if (targetId.equals(list.getCompound(j).getString(InventoryUtil.TAG_EFFECT_ID))) {
                        allowed[0] = true;
                        break;
                    }
                }
            }
        });
        return allowed[0];
    }

    /**
     * 全ての作物から 0.077% の確率でクローバーをドロップする
     */
    @SubscribeEvent
    public static void onCropBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof ServerLevel sl && !sl.isClientSide()) {
            BlockState state = event.getState();
            if (isMatureCrop(state)) {
                if (sl.random.nextFloat() < 0.00077f) {
                    BlockPos pos = event.getPos();
                    ItemEntity item = new ItemEntity(sl, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                            new ItemStack(NatureCollapse.Clover.get()));
                    item.setDefaultPickUpDelay();
                    sl.addFreshEntity(item);
                }
            }
        }
    }

    private static boolean isMatureCrop(BlockState state) {
        Block block = state.getBlock();
        if (block instanceof CropBlock crop) {
            return crop.isMaxAge(state);
        }
        if (state.is(BlockTags.CROPS)) {
            for (net.minecraft.world.level.block.state.properties.Property<?> prop : state.getProperties()) {
                if (prop.getName().equals("age") && prop instanceof net.minecraft.world.level.block.state.properties.IntegerProperty intProp) {
                    int max = java.util.Collections.max(intProp.getPossibleValues());
                    return state.getValue(intProp) >= max;
                }
            }
            return true;
        }
        if (block instanceof NetherWartBlock) {
            return state.getValue(NetherWartBlock.AGE) >= 3;
        }
        if (block instanceof CocoaBlock) {
            return state.getValue(CocoaBlock.AGE) >= 2;
        }
        return false;
    }

    /**
     * 農民村人の就職判定 & 四つ葉のクローバー（16ブロック以内）による創生の種根取引ブースト
     */
    @SubscribeEvent
    public static void onVillagerTick(LivingEvent.LivingTickEvent event) {
        if (event.getEntity() instanceof Villager villager && !villager.level().isClientSide()
                && villager.level() instanceof ServerLevel sl) {

            // 1秒に1回チェック
            if (villager.tickCount % 20 != 0) return;

            VillagerProfession prof = villager.getVillagerData().getProfession();

            // 1. 農民への新規就職判定
            if (prof == VillagerProfession.FARMER) {
                if (!villager.getPersistentData().getBoolean("nature_collapse.farmer_evaluated")) {
                    villager.getPersistentData().putBoolean("nature_collapse.farmer_evaluated", true);

                    // 16ブロック以内の全プレイヤーをチェック（他プレイヤーが複数持っていても増加は1つのみ適用）
                    AABB searchBox = villager.getBoundingBox().inflate(16.0);
                    List<Player> nearbyPlayers = sl.getEntitiesOfClass(Player.class, searchBox);
                    boolean hasFourLeaf = false;
                    for (Player p : nearbyPlayers) {
                        if (CloverUtil.hasFourLeafClover(p)) {
                            hasFourLeaf = true;
                            break;
                        }
                    }

                    if (hasFourLeaf) {
                        villager.getPersistentData().putBoolean("nature_collapse.has_four_leaf_boost", true);
                        // 就職ブーストの視覚的演出
                        sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, villager.getX(), villager.getY() + 1.2, villager.getZ(),
                                12, 0.4, 0.4, 0.4, 0.1);
                    }
                }

                // 2. 達人（Level 5）到達時の「創生の種根」取引登録
                int level = villager.getVillagerData().getLevel();
                if (level >= 5 && !villager.getPersistentData().getBoolean("nature_collapse.master_trades_added")) {
                    villager.getPersistentData().putBoolean("nature_collapse.master_trades_added", true);

                    boolean boosted = villager.getPersistentData().getBoolean("nature_collapse.has_four_leaf_boost");
                    // 通常確率: 2% (ごく稀)、四つ葉ブースト時: 80% (大幅上昇)
                    float chance = boosted ? 0.80f : 0.02f;

                    if (sl.random.nextFloat() < chance) {
                        // エメラルド2スタック（64 + 64 = 128個）と創生の種根1個の取引
                        MerchantOffer genesisTrade = new MerchantOffer(
                                new ItemStack(Items.EMERALD, 64),
                                new ItemStack(Items.EMERALD, 64),
                                new ItemStack(NatureCollapse.GenesisRoot.get()),
                                1, // 最大取引回数
                                30, // 獲得XP
                                0.05f // 価格変動乗数
                        );
                        villager.getOffers().add(genesisTrade);

                        sl.sendParticles(ParticleTypes.COMPOSTER, villager.getX(), villager.getY() + 1.2, villager.getZ(),
                                15, 0.5, 0.5, 0.5, 0.1);
                    }
                }
            }
        }
    }
}
