package com.github.commoble.databuddy.examplecontent;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.server.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;

@Mod(DataBuddyExampleMod.MODID)
public class DataBuddyExampleMod
{
	public static final String MODID = "databuddy";
	
	private static final String CHANNEL_PROTOCOL = "0";
	
	public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
		new ResourceLocation(MODID, "main"),
			() -> CHANNEL_PROTOCOL,
			CHANNEL_PROTOCOL::equals,
			CHANNEL_PROTOCOL::equals);
		
	
	// constructed by forge during modloading
	public DataBuddyExampleMod()
	{
		IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
		IEventBus forgeBus = MinecraftForge.EVENT_BUS;
		
		this.registerPackets();
		
		forgeBus.addListener(this::onServerAboutToStart);
		forgeBus.addListener(this::testData);
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
	// note that for data that needs to be on the client as well (e.g. recipes)
	// then a syncing packet containing the relevant data should be sent when a player joins or resources are reloaded
	// (you'd need to implement your own packet at the moment)
	void onServerAboutToStart(FMLServerAboutToStartEvent event)
	{
		event.getServer().getResourceManager().addReloadListener(FlavorTags.DATA_LOADER);
	}
	
	void testData(PlayerInteractEvent.RightClickBlock event)
	{
		// fires on both client and server threads
		System.out.println(FlavorTags.DATA_LOADER.data);
	}
}
