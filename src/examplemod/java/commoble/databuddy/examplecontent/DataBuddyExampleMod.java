package commoble.databuddy.examplecontent;

import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.commoble.databuddy.config.ConfigHelper;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent.BreakEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@Mod(DataBuddyExampleMod.MODID)
public class DataBuddyExampleMod
{
	public static final String MODID = "databuddy";
	private static final Logger LOGGER = LogManager.getLogger();
	
	private final ExampleConfig config;
	private final ExampleConfig firstConfig;
	private final ExampleConfig secondConfig;

	public DataBuddyExampleMod(IEventBus modBus)
	{
		// get event busses
		IEventBus forgeBus = NeoForge.EVENT_BUS;
		
		// create and subscribe our config instances
		this.config = ConfigHelper.register(
			MODID,
			ModConfig.Type.COMMON, ExampleConfig::create);
		// example of specifying the config name to create subfolders
		this.firstConfig = ConfigHelper.register(
			MODID,
			ModConfig.Type.COMMON, ExampleConfig::create, "databuddy/first");
		this.secondConfig = ConfigHelper.register(
			MODID,
			ModConfig.Type.COMMON, ExampleConfig::create, "databuddy/secondconfig");
		
		// subscribe to events
		modBus.addListener(this::onRegisterPackets);
		forgeBus.addListener(this::onAddReloadListeners);
		forgeBus.addListener(this::testData);
		forgeBus.addListener(this::testConfig);
		FlavorTags.DATA_LOADER.subscribeAsSyncable(FlavorTagSyncPacket::new);
	}
	
	void onRegisterPackets(RegisterPayloadHandlersEvent event)
	{
		event.registrar(MODID)
			.<FlavorTagSyncPacket>playToClient(FlavorTagSyncPacket.ID, FlavorTagSyncPacket.STREAM_CODEC, FlavorTagSyncPacket::onPacketReceived);
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
			event.getEntity().level().isClientSide()
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
