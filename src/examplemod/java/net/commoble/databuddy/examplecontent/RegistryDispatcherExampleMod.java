package net.commoble.databuddy.examplecontent;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;

import net.commoble.databuddy.codec.RegistryDispatcher;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.neoforge.event.level.BlockEvent.BreakEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

/*
 * Example of using RegistryDispatcher to make serializer registries
 */
@EventBusSubscriber(modid=DataBuddyExampleMod.MODID, bus=Bus.GAME)
public class RegistryDispatcherExampleMod
{
	private static final Logger LOGGER = LogManager.getLogger();
	
	// The RegistryDispatcher contains the codec for your data class and the deferred register for your serializers.
	// The DeferredRegister will be automatically registered and a forge registry will be created and registered.
	public static final RegistryDispatcher<Cheese> CHEESE_DISPATCHER = RegistryDispatcher.makeDispatchForgeRegistry(
		ResourceLocation.fromNamespaceAndPath(DataBuddyExampleMod.MODID, "cheese"),
		cheese -> cheese.getType(), // using a method reference here seems to confuse eclipse
		builder->{});
	
	// RegistryObjects can be created from the dispatcher's deferred registry
	public static final DeferredHolder<MapCodec<? extends Cheese>, MapCodec<Cheddar>> CHEDDAR = CHEESE_DISPATCHER.defreg()
		.register("cheddar", () -> MapCodec.unit(new Cheddar()));
	
	// Base class for your data classes, instances of this could potentially be parsed from jsons or whatever
	public static interface Cheese
	{
		public MapCodec<? extends Cheese> getType();
		public int color();
	}
	
	@SubscribeEvent
	public static void onBlockBreak(BreakEvent event)
	{
		String json = """
			{
				"type": "databuddy:cheddar"
			}
			""";
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		JsonElement jsonElement = gson.fromJson(json, JsonElement.class);
		Cheese cheese = CHEESE_DISPATCHER.dispatchedCodec().parse(JsonOps.INSTANCE, jsonElement)
			.result()
			.get();
		LOGGER.info(CHEESE_DISPATCHER.codecRegistry().getKey(cheese.getType())); // logs "databuddy:cheddar"
	}
	
	// subclass of the data class, the "type" field in Cheese jsons would indicate to use e.g. the databuddy:cheddar serializer
	public static class Cheddar implements Cheese
	{
		@Override
		public MapCodec<? extends Cheese> getType()
		{
			return RegistryDispatcherExampleMod.CHEDDAR.get();
		}

		@Override
		public int color()
		{
			return 0;
		}
	}
}
