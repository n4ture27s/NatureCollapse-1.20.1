package n4ture27.nature_collapse;

import com.mojang.logging.LogUtils;

import n4ture27.nature_collapse.network.UpdateRangePacket;
import n4ture27.nature_collapse.network.UpdateFlowerEffectsPacket;
import n4ture27.nature_collapse.effect.ToNatural;
import n4ture27.nature_collapse.item.NaturaLizeSword;
import n4ture27.nature_collapse.item.NatureFlower;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.Set;

import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(NatureCollapse.ModId)
public class NatureCollapse {
	// Define mod id in a common place for everything to reference
	public static final String ModId = "nature_collapse";
	// Directly reference a slf4j logger
	public static final Logger Logger = LogUtils.getLogger();
	// Create a Deferred Register to hold Blocks which will all be registered under
	// the "examplemod" namespace
	public static final DeferredRegister<Block> Blocks = DeferredRegister.create(ForgeRegistries.BLOCKS, ModId);
	// Create a Deferred Register to hold Items which will all be registered under
	// the "examplemod" namespace
	public static final DeferredRegister<Item> Items = DeferredRegister.create(ForgeRegistries.ITEMS, ModId);
	// Create a Deferred Register to hold CreativeModeTabs which will all be
	// registered under the "examplemod" namespace
	public static final DeferredRegister<CreativeModeTab> CreativeTabRegister = DeferredRegister
			.create(Registries.CREATIVE_MODE_TAB, ModId);
	public static final DeferredRegister<MobEffect> EffectRegister = DeferredRegister
			.create(ForgeRegistries.MOB_EFFECTS, ModId);

	public static final RegistryObject<NaturaLizeSword> NaturelizeSword = Items.register("naturalize_sword",
			() -> new NaturaLizeSword(new Item.Properties().fireResistant()));
	public static final RegistryObject<NatureFlower> NatureFlower = Items.register("nature_flower",
			() -> new NatureFlower(new Item.Properties().fireResistant()));
	public static final RegistryObject<MobEffect> ToNaturalEffect = EffectRegister.register("to_natural",
			() -> new ToNatural());
	public static final RegistryObject<CreativeModeTab> CreativeTabs = CreativeTabRegister.register("example",
			() -> CreativeModeTab.builder()
					.title(Component.translatable("item_group." + ModId))
					.icon(() -> new ItemStack(NatureFlower.get()))
					.displayItems((params, output) -> {
						output.accept(NaturelizeSword.get());
						output.accept(NatureFlower.get());
					})
					.build());

	public static MinecraftServer Server;
	
	private static final String PROTOCOL_VERSION = "1";
	public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
			new ResourceLocation(ModId, "main"),
			() -> PROTOCOL_VERSION,
			PROTOCOL_VERSION::equals,
			PROTOCOL_VERSION::equals
	);

	public NatureCollapse(FMLJavaModLoadingContext context) {
		IEventBus modEventBus = context.getModEventBus();

		// Register the commonSetup method for modloading
		modEventBus.addListener(this::commonSetup);

		// Register the Deferred Register to the mod event bus so blocks get registered
		Blocks.register(modEventBus);
		// Register the Deferred Register to the mod event bus so items get registered
		Items.register(modEventBus);
		// Register the Deferred Register to the mod event bus so tabs get registered
		CreativeTabRegister.register(modEventBus);
		EffectRegister.register(modEventBus);

		int id = 0;
		CHANNEL.registerMessage(id++, UpdateRangePacket.class, UpdateRangePacket::encode, UpdateRangePacket::new, UpdateRangePacket::handle);
		CHANNEL.registerMessage(id++, UpdateFlowerEffectsPacket.class, UpdateFlowerEffectsPacket::encode, UpdateFlowerEffectsPacket::new, UpdateFlowerEffectsPacket::handle);

		// Register ourselves for server and other game events we are interested in
		MinecraftForge.EVENT_BUS.register(this);

		// Register our mod's ForgeConfigSpec so that Forge can create and load the
		// config file for us
		context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
	}

	private void commonSetup(final FMLCommonSetupEvent event) {
		Logger.info("HELLO FROM COMMON SETUP");

		// if (Config.logDirtBlock)
		// Logger.info("DIRT BLOCK >> {}", ForgeRegistries.BLOCKS.getKey(Blocks.DIRT));

		Logger.info(Config.magicNumberIntroduction + Config.magicNumber);

		Config.items.forEach((item) -> Logger.info("ITEM >> {}", item.toString()));
	}

	// You can use SubscribeEvent and let the Event Bus discover methods to call
	@SubscribeEvent
	public void onServerStarting(ServerStartingEvent event) {
		// Do something when the server starts
		Logger.info("HELLO from server starting");

		Server = event.getServer();
	}

	// You can use EventBusSubscriber to automatically register all static methods
	// in the class annotated with @SubscribeEvent
	@Mod.EventBusSubscriber(modid = ModId, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
	public static class ClientModEvents {
		public static final KeyMapping OPEN_RANGE_GUI = new KeyMapping("key.nature_collapse.open_range_gui", InputConstants.Type.KEYSYM, InputConstants.KEY_K, "key.categories.nature_collapse");

		@SubscribeEvent
		public static void onClientSetup(FMLClientSetupEvent event) {
			// Some client setup code
			Logger.info("HELLO FROM CLIENT SETUP");
			Logger.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
		}

		@SubscribeEvent
		public static void onKeyRegister(RegisterKeyMappingsEvent event) {
			event.register(OPEN_RANGE_GUI);
		}
	}
}
