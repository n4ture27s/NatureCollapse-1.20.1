package n4ture27.nature_collapse.utils;

import n4ture27.nature_collapse.Config;
import n4ture27.nature_collapse.NatureCollapse;
import n4ture27.nature_collapse.compat.CuriosCompat;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Set;
import java.util.function.Consumer;

public final class InventoryUtil {
    public static final String TAG_CUSTOM_RANGE = "CustomRange";
    public static final String TAG_SELECTED_EFFECTS = "SelectedEffects";
    public static final String TAG_EFFECT_ID = "Id";
    public static final String TAG_EFFECT_LEVEL = "Level";
    public static final String TAG_EFFECT_DURATION = "Duration";

    public static final String TAG_FLOWER_CONFIG = "FlowerConfig";
    public static final String KEY_FLIGHT_ENABLED = "FlightEnabled";
    public static final String KEY_FLIGHT_SPEED_H = "FlightSpeedH";
    public static final String KEY_FLIGHT_SPEED_V = "FlightSpeedV";
    public static final String KEY_EFFECTS_ENABLED = "EffectsEnabled";
    public static final String KEY_INVUL_ENABLED = "InvulEnabled";
    public static final String KEY_SATURATION_ENABLED = "SaturationEnabled";
    public static final String KEY_MAGNET_ENABLED = "MagnetEnabled";
    public static final String KEY_MAGNET_RANGE = "MagnetRange";
    public static final String KEY_SPEED_ENABLED = "SpeedEnabled";
    public static final String KEY_SPEED_MULT = "SpeedMult";
    public static final String KEY_AURA_ENABLED = "AuraEnabled";
    public static final String KEY_AURA_RANGE = "AuraRange";
    public static final String KEY_VISION_ENABLED = "VisionEnabled";

    public static final int DEFAULT_EFFECT_DURATION = 300;
    public static final int MIN_EFFECT_DURATION = 20;
    public static final int MAX_EFFECT_DURATION = 12000;
    public static final int MIN_EFFECT_LEVEL = 1;
    public static final int MAX_EFFECT_LEVEL = 255;

    public record FlowerConfig(
            boolean flightEnabled,
            float flightSpeedH,
            float flightSpeedV,
            boolean effectsEnabled,
            boolean invulEnabled,
            boolean saturationEnabled,
            boolean magnetEnabled,
            double magnetRange,
            boolean speedEnabled,
            float speedMult,
            boolean auraEnabled,
            double auraRange,
            boolean visionEnabled
    ) {
        public static FlowerConfig createDefault() {
            return new FlowerConfig(
                    true, 0.05f, 0.05f,
                    true,
                    true,
                    true,
                    false, 8.0,
                    false, 1.5f,
                    false, 6.0,
                    false
            );
        }

        public CompoundTag toTag() {
            CompoundTag tag = new CompoundTag();
            tag.putBoolean(KEY_FLIGHT_ENABLED, flightEnabled);
            tag.putFloat(KEY_FLIGHT_SPEED_H, flightSpeedH);
            tag.putFloat(KEY_FLIGHT_SPEED_V, flightSpeedV);
            tag.putBoolean(KEY_EFFECTS_ENABLED, effectsEnabled);
            tag.putBoolean(KEY_INVUL_ENABLED, invulEnabled);
            tag.putBoolean(KEY_SATURATION_ENABLED, saturationEnabled);
            tag.putBoolean(KEY_MAGNET_ENABLED, magnetEnabled);
            tag.putDouble(KEY_MAGNET_RANGE, magnetRange);
            tag.putBoolean(KEY_SPEED_ENABLED, speedEnabled);
            tag.putFloat(KEY_SPEED_MULT, speedMult);
            tag.putBoolean(KEY_AURA_ENABLED, auraEnabled);
            tag.putDouble(KEY_AURA_RANGE, auraRange);
            tag.putBoolean(KEY_VISION_ENABLED, visionEnabled);
            return tag;
        }

        public static FlowerConfig fromTag(CompoundTag tag) {
            if (tag == null) return createDefault();
            float speedH = tag.contains(KEY_FLIGHT_SPEED_H) ? tag.getFloat(KEY_FLIGHT_SPEED_H) : 0.05f;
            float speedV = tag.contains(KEY_FLIGHT_SPEED_V) ? tag.getFloat(KEY_FLIGHT_SPEED_V) : 0.05f;
            double magR = tag.contains(KEY_MAGNET_RANGE) ? tag.getDouble(KEY_MAGNET_RANGE) : 8.0;
            float speedM = tag.contains(KEY_SPEED_MULT) ? tag.getFloat(KEY_SPEED_MULT) : 1.5f;
            double auraR = tag.contains(KEY_AURA_RANGE) ? tag.getDouble(KEY_AURA_RANGE) : 6.0;

            return new FlowerConfig(
                    tag.contains(KEY_FLIGHT_ENABLED) ? tag.getBoolean(KEY_FLIGHT_ENABLED) : true,
                    Float.isFinite(speedH) ? Mth.clamp(speedH, 0.01f, 1.0f) : 0.05f,
                    Float.isFinite(speedV) ? Mth.clamp(speedV, 0.01f, 1.0f) : 0.05f,
                    tag.contains(KEY_EFFECTS_ENABLED) ? tag.getBoolean(KEY_EFFECTS_ENABLED) : true,
                    tag.contains(KEY_INVUL_ENABLED) ? tag.getBoolean(KEY_INVUL_ENABLED) : true,
                    tag.contains(KEY_SATURATION_ENABLED) ? tag.getBoolean(KEY_SATURATION_ENABLED) : true,
                    tag.contains(KEY_MAGNET_ENABLED) ? tag.getBoolean(KEY_MAGNET_ENABLED) : false,
                    Double.isFinite(magR) ? Mth.clamp(magR, 1.0, 64.0) : 8.0,
                    tag.contains(KEY_SPEED_ENABLED) ? tag.getBoolean(KEY_SPEED_ENABLED) : false,
                    Float.isFinite(speedM) ? Mth.clamp(speedM, 1.0f, 10.0f) : 1.5f,
                    tag.contains(KEY_AURA_ENABLED) ? tag.getBoolean(KEY_AURA_ENABLED) : false,
                    Double.isFinite(auraR) ? Mth.clamp(auraR, 1.0, 32.0) : 6.0,
                    tag.contains(KEY_VISION_ENABLED) ? tag.getBoolean(KEY_VISION_ENABLED) : false
            );
        }
    }

    private InventoryUtil() {
    }

    public static FlowerConfig getFlowerConfig(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return FlowerConfig.createDefault();
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(TAG_FLOWER_CONFIG, Tag.TAG_COMPOUND)) {
            return FlowerConfig.fromTag(tag.getCompound(TAG_FLOWER_CONFIG));
        }
        return FlowerConfig.createDefault();
    }

    public static void setFlowerConfig(ItemStack stack, FlowerConfig config) {
        if (stack == null || stack.isEmpty() || config == null) return;
        stack.getOrCreateTag().put(TAG_FLOWER_CONFIG, config.toTag());
    }

    public static boolean hasFlower(Player player) {
        if (player == null || player.getInventory() == null) return false;

        if (player.getInventory().hasAnyOf(Set.of(NatureCollapse.NatureFlower.get()))) {
            return true;
        }

        return CuriosCompat.hasCurio(player, NatureCollapse.NatureFlower.get());
    }

    public static FlowerConfig getActiveFlowerConfig(Player player) {
        if (player == null) return null;
        if (player instanceof INaturalizable nat && nat.nature_collapse$isNaturalized()) return null;
        if (!hasFlower(player)) return null;

        final FlowerConfig[] found = {null};
        forEachFlower(player, stack -> {
            if (found[0] == null) {
                found[0] = getFlowerConfig(stack);
            }
        });
        return found[0] != null ? found[0] : FlowerConfig.createDefault();
    }

    public static boolean hasInvulnerableFlower(Player player) {
        FlowerConfig config = getActiveFlowerConfig(player);
        return config != null && config.invulEnabled();
    }

    public static void forEachFlower(Player player, Consumer<ItemStack> consumer) {
        if (player == null || player.getInventory() == null) return;

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(NatureCollapse.NatureFlower.get())) {
                consumer.accept(stack);
            }
        }

        CuriosCompat.forEachCurio(player, NatureCollapse.NatureFlower.get(), consumer);
    }

    public static double getSwordRange(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return Config.defaultSwordRange;
        }
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(TAG_CUSTOM_RANGE, Tag.TAG_ANY_NUMERIC)) {
            double saved = tag.getDouble(TAG_CUSTOM_RANGE);
            if (Double.isFinite(saved) && saved > 0.0) {
                return Mth.clamp(saved, 1.0, Config.maxSwordRange);
            }
        }
        return Config.defaultSwordRange;
    }

    public static void setSwordRange(ItemStack stack, double range) {
        if (stack == null || stack.isEmpty()) return;
        double clamped = Double.isFinite(range)
                ? Mth.clamp(range, 1.0, Config.maxSwordRange)
                : Config.defaultSwordRange;
        stack.getOrCreateTag().putDouble(TAG_CUSTOM_RANGE, clamped);
    }

    public static ItemStack getStackFromSlot(Player player, int slotIndex, boolean isCurio, String curioIdentifier) {
        if (player == null) return ItemStack.EMPTY;
        if (isCurio) {
            return CuriosCompat.getCurioStack(player, curioIdentifier, slotIndex);
        }
        if (slotIndex >= 0 && slotIndex < player.getInventory().getContainerSize()) {
            return player.getInventory().getItem(slotIndex);
        }
        return ItemStack.EMPTY;
    }
}
