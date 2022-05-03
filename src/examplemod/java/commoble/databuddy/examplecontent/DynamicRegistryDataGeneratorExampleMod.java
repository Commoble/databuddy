package commoble.databuddy.examplecontent;

import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import commoble.databuddy.datagen.DynamicRegistryDataGenerator;
import net.minecraft.core.Registry;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.BlockPileConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.forge.event.lifecycle.GatherDataEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * Example of using DynamicRegistryDataGenerator to datagen worldgen jsons
 */
@EventBusSubscriber(modid=DataBuddyExampleMod.MODID, bus=Bus.MOD)
public class DynamicRegistryDataGeneratorExampleMod
{
	// worldgen jsons do not need to be registered to be loaded during normal server sessions,
	// but if we are datagenerating the jsons, the objects to datagenerate must be registered
	// during the datagen session
	private static final DeferredRegister<ConfiguredFeature<?,?>> CONFIGURED_FEATURES =
		DeferredRegister.create(Registry.CONFIGURED_FEATURE_REGISTRY, DataBuddyExampleMod.MODID);
	private static final DeferredRegister<PlacedFeature> PLACED_FEATURES = 
		DeferredRegister.create(Registry.PLACED_FEATURE_REGISTRY, DataBuddyExampleMod.MODID);
	
	private static final String TNT_PILE_NAME = "tnt_pile";
	private static final ResourceKey<ConfiguredFeature<?,?>> CONFIGURED_TNT_PILE_KEY = ResourceKey.create(Registry.CONFIGURED_FEATURE_REGISTRY, new ResourceLocation(DataBuddyExampleMod.MODID, TNT_PILE_NAME));
	private static final RegistryObject<ConfiguredFeature<?,?>> CONFIGURED_TNT_PILE =
		CONFIGURED_FEATURES.register(TNT_PILE_NAME,
			() -> new ConfiguredFeature<>(Feature.BLOCK_PILE,
				new BlockPileConfiguration(BlockStateProvider.simple(Blocks.TNT))));
	private static final ResourceKey<PlacedFeature> PLACED_TNT_PILE_KEY = ResourceKey.create(Registry.PLACED_FEATURE_REGISTRY, new ResourceLocation(DataBuddyExampleMod.MODID, TNT_PILE_NAME));
	private static final RegistryObject<PlacedFeature> PLACED_TNT_PILE =
		PLACED_FEATURES.register(TNT_PILE_NAME, 
			() -> new PlacedFeature(CONFIGURED_TNT_PILE.getHolder().get(),
				List.of(InSquarePlacement.spread(), PlacementUtils.HEIGHTMAP)));
	
	@SubscribeEvent
	public static void onConstructMod(FMLConstructModEvent event)
	{
		IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
		CONFIGURED_FEATURES.register(modBus);
		PLACED_FEATURES.register(modBus);
	}
	
	@SubscribeEvent
	public static void onGatherData(GatherDataEvent event)
	{
		DataGenerator generator = event.getGenerator();
		DynamicRegistryDataGenerator drdg = DynamicRegistryDataGenerator.create(generator);
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		generator.addProvider(drdg.makeDataProvider(DataBuddyExampleMod.MODID, gson, Registry.CONFIGURED_FEATURE_REGISTRY, CONFIGURED_TNT_PILE_KEY));
		generator.addProvider(drdg.makeDataProvider(DataBuddyExampleMod.MODID, gson, Registry.PLACED_FEATURE_REGISTRY, PLACED_TNT_PILE_KEY));
	}
}
