package n4ture27.nature_collapse;

import com.mojang.logging.LogUtils;
import n4ture27.nature_collapse.compat.CuriosCompat;
import n4ture27.nature_collapse.effect.ToNatural;
import n4ture27.nature_collapse.entity.ArborisBulletEntity;
import n4ture27.nature_collapse.entity.ArborisEntity;
import n4ture27.nature_collapse.entity.ArborisMagicCircleEntity;
import n4ture27.nature_collapse.item.CloverItem;
import n4ture27.nature_collapse.item.FourLeafCloverItem;
import n4ture27.nature_collapse.item.GenesisRootItem;
import n4ture27.nature_collapse.item.MiracleFlowerItem;
import n4ture27.nature_collapse.item.MiracleRootItem;
import n4ture27.nature_collapse.item.NaturaLizeSword;
import n4ture27.nature_collapse.item.NatureFlower;
import n4ture27.nature_collapse.network.CameraShakePacket;
import n4ture27.nature_collapse.network.UpdateFlowerConfigPacket;
import n4ture27.nature_collapse.network.UpdateFlowerEffectsPacket;
import n4ture27.nature_collapse.network.UpdateRangePacket;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

import java.lang.reflect.Field;

@Mod(NatureCollapse.ModId)
public class NatureCollapse {
    public static final String ModId = "nature_collapse";
    public static final Logger Logger = LogUtils.getLogger();

    public static final DeferredRegister<Item> Items = DeferredRegister.create(ForgeRegistries.ITEMS, ModId);
    public static final DeferredRegister<EntityType<?>> Entities = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, ModId);
    public static final DeferredRegister<CreativeModeTab> CreativeTabRegister = DeferredRegister
            .create(Registries.CREATIVE_MODE_TAB, ModId);
    public static final DeferredRegister<MobEffect> EffectRegister = DeferredRegister
            .create(ForgeRegistries.MOB_EFFECTS, ModId);

    public static final RegistryObject<NaturaLizeSword> NaturelizeSword = Items.register("naturalize_sword",
            () -> new NaturaLizeSword(new Item.Properties().fireResistant()));
    public static final RegistryObject<NatureFlower> NatureFlower = Items.register("nature_flower",
            () -> new NatureFlower(new Item.Properties().fireResistant()));
    public static final RegistryObject<GenesisRootItem> GenesisRoot = Items.register("genesis_root",
            () -> new GenesisRootItem(new Item.Properties()));
    public static final RegistryObject<CloverItem> Clover = Items.register("clover",
            () -> new CloverItem(new Item.Properties()));
    public static final RegistryObject<FourLeafCloverItem> FourLeafClover = Items.register("four_leaf_clover",
            () -> new FourLeafCloverItem(new Item.Properties()));
    public static final RegistryObject<MiracleFlowerItem> MiracleFlower = Items.register("miracle_flower",
            () -> new MiracleFlowerItem(new Item.Properties()));
    public static final RegistryObject<MiracleRootItem> MiracleRoot = Items.register("miracle_root",
            () -> new MiracleRootItem(new Item.Properties()));

    public static final RegistryObject<EntityType<ArborisEntity>> ArborisEntityType = Entities.register("arboris",
            () -> EntityType.Builder.of(ArborisEntity::new, MobCategory.MONSTER)
                    .sized(5.0f, 12.0f)
                    .clientTrackingRange(128)
                    .updateInterval(1)
                    .fireImmune()
                    .build("arboris"));

    public static final RegistryObject<EntityType<ArborisBulletEntity>> ArborisBulletEntityType = Entities.register("arboris_bullet",
            () -> EntityType.Builder.<ArborisBulletEntity>of(ArborisBulletEntity::new, MobCategory.MISC)
                    .sized(0.8f, 0.8f)
                    .clientTrackingRange(128)
                    .updateInterval(1)
                    .fireImmune()
                    .build("arboris_bullet"));

    public static final RegistryObject<EntityType<ArborisMagicCircleEntity>> ArborisMagicCircleEntityType = Entities.register("arboris_magic_circle",
            () -> EntityType.Builder.<ArborisMagicCircleEntity>of(ArborisMagicCircleEntity::new, MobCategory.MISC)
                    .sized(3.0f, 3.0f)
                    .clientTrackingRange(128)
                    .updateInterval(1)
                    .fireImmune()
                    .build("arboris_magic_circle"));

    public static final RegistryObject<MobEffect> ToNaturalEffect = EffectRegister.register("to_natural",
            ToNatural::new);

    public static final RegistryObject<CreativeModeTab> CreativeTabs = CreativeTabRegister.register("example",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("item_group." + ModId))
                    .icon(() -> new ItemStack(NatureFlower.get()))
                    .displayItems((params, output) -> {
                        output.accept(NaturelizeSword.get());
                        output.accept(NatureFlower.get());
                        output.accept(GenesisRoot.get());
                        output.accept(Clover.get());
                        output.accept(FourLeafClover.get());
                        output.accept(MiracleFlower.get());
                        output.accept(MiracleRoot.get());
                    })
                    .build());

    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(ModId, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public NatureCollapse(FMLJavaModLoadingContext context) {
        uncapMaxHealthAttribute();

        IEventBus modEventBus = context.getModEventBus();

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::enqueueIMC);
        modEventBus.addListener(this::registerAttributes);

        Items.register(modEventBus);
        Entities.register(modEventBus);
        CreativeTabRegister.register(modEventBus);
        EffectRegister.register(modEventBus);

        int id = 0;
        CHANNEL.registerMessage(id++, UpdateRangePacket.class, UpdateRangePacket::encode, UpdateRangePacket::new, UpdateRangePacket::handle);
        CHANNEL.registerMessage(id++, UpdateFlowerEffectsPacket.class, UpdateFlowerEffectsPacket::encode, UpdateFlowerEffectsPacket::new, UpdateFlowerEffectsPacket::handle);
        CHANNEL.registerMessage(id++, UpdateFlowerConfigPacket.class, UpdateFlowerConfigPacket::encode, UpdateFlowerConfigPacket::new, UpdateFlowerConfigPacket::handle);
        CHANNEL.registerMessage(id++, CameraShakePacket.class, CameraShakePacket::encode, CameraShakePacket::new, CameraShakePacket::handle);

        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ArborisEntityType.get(), ArborisEntity.createAttributes().build());
    }

    private void enqueueIMC(final InterModEnqueueEvent event) {
        CuriosCompat.enqueueImc();
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        uncapMaxHealthAttribute();
        Logger.info("{} initialized.", ModId);
    }

    public static void uncapMaxHealthAttribute() {
        try {
            Field unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            sun.misc.Unsafe unsafe = (sun.misc.Unsafe) unsafeField.get(null);
            for (Field f : RangedAttribute.class.getDeclaredFields()) {
                if (f.getType() == double.class) {
                    long offset = unsafe.objectFieldOffset(f);
                    double val = unsafe.getDouble(Attributes.MAX_HEALTH, offset);
                    if (Math.abs(val - 1024.0D) < 0.1) {
                        unsafe.putDouble(Attributes.MAX_HEALTH, offset, 1_000_000_000.0D);
                        Logger.info("Successfully uncapped Attributes.MAX_HEALTH max value to 1,000,000,000.0!");
                        break;
                    }
                }
            }
        } catch (Throwable t) {
            Logger.error("Failed to uncap Attributes.MAX_HEALTH", t);
        }
    }
}
