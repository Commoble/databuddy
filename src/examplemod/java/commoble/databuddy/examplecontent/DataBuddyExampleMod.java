package commoble.databuddy.examplecontent;

import commoble.databuddy.config.ConfigHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent.BreakEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;

@Mod(DataBuddyExampleMod.MODID)
public class DataBuddyExampleMod
{
	public static final String MODID = "databuddy";
	
	public static ExampleServerConfig config;
	
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
		IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
		IEventBus forgeBus = MinecraftForge.EVENT_BUS;
		
		// create and subscribe our config instance
		DataBuddyExampleMod.config = ConfigHelper.register(
			ModLoadingContext.get(), FMLJavaModLoadingContext.get(),
			ModConfig.Type.SERVER, ExampleServerConfig::new);
		
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
		System.out.println(FlavorTags.DATA_LOADER.data);
	}
	
	void testConfig(BreakEvent event)
	{
		System.out.println(config.bones.get());
		System.out.println(config.bananas.get());
		System.out.println(config.testObject.get());
	}
}
