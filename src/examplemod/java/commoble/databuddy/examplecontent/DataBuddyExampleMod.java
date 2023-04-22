package commoble.databuddy.examplecontent;

import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import commoble.databuddy.config.ConfigHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent.BreakEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

@Mod(DataBuddyExampleMod.MODID)
public class DataBuddyExampleMod
{
	public static final String MODID = "databuddy";
	private static final Logger LOGGER = LogManager.getLogger();
	
	private final ExampleConfig config;
	private final ExampleConfig firstConfig;
	private final ExampleConfig secondConfig;
	
	private static final String CHANNEL_PROTOCOL = "0";
	
	public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
		new ResourceLocation(MODID, "main"),
			() -> CHANNEL_PROTOCOL,
			CHANNEL_PROTOCOL::equals,
			CHANNEL_PROTOCOL::equals);
		
	
	// constructed by forge during modloading
	public DataBuddyExampleMod()
	{
		// get event busses
		IEventBus forgeBus = MinecraftForge.EVENT_BUS;
		
		// create and subscribe our config instances
		this.config = ConfigHelper.register(
			ModConfig.Type.COMMON, ExampleConfig::create);
		// example of specifying the config name to create subfolders
		this.firstConfig = ConfigHelper.register(
			ModConfig.Type.COMMON, ExampleConfig::create, "databuddy/first");
		this.secondConfig = ConfigHelper.register(
			ModConfig.Type.COMMON, ExampleConfig::create, "databuddy/secondconfig");
		
		this.registerPackets();
		
		// subscribe to events
		forgeBus.addListener(this::onAddReloadListeners);
		forgeBus.addListener(this::testData);
		forgeBus.addListener(this::testConfig);
		FlavorTags.DATA_LOADER.subscribeAsSyncable(CHANNEL, FlavorTagSyncPacket::new);
	}
	
	void registerPackets()
	{
		int id = 0;
		CHANNEL.registerMessage(id++, FlavorTagSyncPacket.class,
			FlavorTagSyncPacket::encode,
			FlavorTagSyncPacket::decode,
			FlavorTagSyncPacket::onPacketReceived);
	}
	
	// register our data loader to the server
	// for loading assets, we can add our reload listener to Minecraft in our mod constructor
	// in 1.16, forge has a dedicated event for registering reload listeners
	// in 1.15, this can be added to the server in FMLServerAboutToStartEvent
	// note that for data that needs to be on the client as well (e.g. recipes)
	// then a syncing packet containing the relevant data should be sent when a player joins or resources are reloaded
	// (you'd need to implement your own packet at the moment)
	void onAddReloadListeners(AddReloadListenerEvent event)
	{
		event.addListener(FlavorTags.DATA_LOADER);
	}
	
	void testData(PlayerInteractEvent.RightClickBlock event)
	{
		// fires on both client and server threads
		LOGGER.info(
			event.getEntity().level.isClientSide
				? FlavorTagSyncPacket.SYNCED_DATA
				: FlavorTags.DATA_LOADER.getData());
	}
	
	void testConfig(BreakEvent event)
	{
		Consumer<ExampleConfig> configLogger = c ->
		{
			LOGGER.info(c.bones().get());
			LOGGER.info(c.bananas().get());
			LOGGER.info(c.testObject().get());
		};
		configLogger.accept(this.config);
		configLogger.accept(this.firstConfig);
		configLogger.accept(this.secondConfig);
		
		this.config.incrementableField().set(this.config.incrementableField().get() + 1);
	}
}
