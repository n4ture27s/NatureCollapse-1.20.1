package n4ture27.nature_collapse.network;

import n4ture27.nature_collapse.item.NatureFlower;
import n4ture27.nature_collapse.utils.InventoryUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class UpdateFlowerEffectsPacket {
    private static final int MAX_EFFECT_ENTRIES = 128;

    public record EffectEntry(String id, int level, int duration) {
    }

    private final int slotIndex;
    private final boolean isCurio;
    private final String curioIdentifier;
    private final List<EffectEntry> effects;

    public UpdateFlowerEffectsPacket(int slotIndex, boolean isCurio, String curioIdentifier, List<EffectEntry> effects) {
        this.slotIndex = slotIndex;
        this.isCurio = isCurio;
        this.curioIdentifier = curioIdentifier != null ? curioIdentifier : "";
        this.effects = effects != null ? List.copyOf(effects) : List.of();
    }

    public UpdateFlowerEffectsPacket(int slotIndex, boolean isCurio, String curioIdentifier, ItemStack flowerStack) {
        this(slotIndex, isCurio, curioIdentifier, extractEntriesFromStack(flowerStack));
    }

    public UpdateFlowerEffectsPacket(FriendlyByteBuf buf) {
        this.slotIndex = buf.readInt();
        this.isCurio = buf.readBoolean();
        this.curioIdentifier = buf.readUtf(64);
        int size = Mth.clamp(buf.readVarInt(), 0, MAX_EFFECT_ENTRIES);
        List<EffectEntry> parsed = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            String id = buf.readUtf(128);
            int level = buf.readVarInt();
            int duration = buf.readVarInt();
            parsed.add(new EffectEntry(id, level, duration));
        }
        this.effects = parsed;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(slotIndex);
        buf.writeBoolean(isCurio);
        buf.writeUtf(curioIdentifier, 64);
        int size = Math.min(effects.size(), MAX_EFFECT_ENTRIES);
        buf.writeVarInt(size);
        for (int i = 0; i < size; i++) {
            EffectEntry entry = effects.get(i);
            buf.writeUtf(entry.id(), 128);
            buf.writeVarInt(entry.level());
            buf.writeVarInt(entry.duration());
        }
    }

    private static List<EffectEntry> extractEntriesFromStack(ItemStack stack) {
        List<EffectEntry> entries = new ArrayList<>();
        if (stack == null || stack.isEmpty() || !stack.hasTag()) {
            return entries;
        }
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(InventoryUtil.TAG_SELECTED_EFFECTS, Tag.TAG_LIST)) {
            return entries;
        }
        ListTag list = tag.getList(InventoryUtil.TAG_SELECTED_EFFECTS, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag effectTag = list.getCompound(i);
            entries.add(new EffectEntry(
                    effectTag.getString(InventoryUtil.TAG_EFFECT_ID),
                    effectTag.getInt(InventoryUtil.TAG_EFFECT_LEVEL),
                    effectTag.getInt(InventoryUtil.TAG_EFFECT_DURATION)
            ));
        }
        return entries;
    }

    public static void handle(UpdateFlowerEffectsPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            ItemStack targetStack = InventoryUtil.getStackFromSlot(player, msg.slotIndex, msg.isCurio, msg.curioIdentifier);
            if (targetStack.isEmpty() || !(targetStack.getItem() instanceof NatureFlower)) {
                return;
            }

            Map<ResourceLocation, CompoundTag> validatedEffects = new LinkedHashMap<>();
            for (EffectEntry entry : msg.effects) {
                ResourceLocation effectId = ResourceLocation.tryParse(entry.id());
                if (effectId == null) continue;

                MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(effectId);
                if (effect == null) continue;

                CompoundTag effectTag = new CompoundTag();
                effectTag.putString(InventoryUtil.TAG_EFFECT_ID, effectId.toString());
                effectTag.putInt(InventoryUtil.TAG_EFFECT_LEVEL,
                        Mth.clamp(entry.level(), InventoryUtil.MIN_EFFECT_LEVEL, InventoryUtil.MAX_EFFECT_LEVEL));
                effectTag.putInt(InventoryUtil.TAG_EFFECT_DURATION,
                        Mth.clamp(entry.duration(), InventoryUtil.MIN_EFFECT_DURATION, InventoryUtil.MAX_EFFECT_DURATION));
                validatedEffects.put(effectId, effectTag);
            }

            if (validatedEffects.isEmpty()) {
                CompoundTag tag = targetStack.getTag();
                if (tag != null) {
                    tag.remove(InventoryUtil.TAG_SELECTED_EFFECTS);
                    if (tag.isEmpty()) {
                        targetStack.setTag(null);
                    }
                }
            } else {
                ListTag listTag = new ListTag();
                listTag.addAll(validatedEffects.values());
                targetStack.getOrCreateTag().put(InventoryUtil.TAG_SELECTED_EFFECTS, listTag);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
