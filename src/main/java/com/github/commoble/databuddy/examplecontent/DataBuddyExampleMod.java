package com.github.commoble.databuddy.examplecontent;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.server.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(DataBuddyExampleMod.MODID)
public class DataBuddyExampleMod
{
	public static final String MODID = "databuddy";
		
	
	// constructed by forge during modloading
	public DataBuddyExampleMod()
	{
		IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
		IEventBus forgeBus = MinecraftForge.EVENT_BUS;
		
		forgeBus.addListener(this::onServerAboutToStart);
		forgeBus.addListener(this::testData);
	}
	
	// register our data loader to the server
	// for loading assets, we can add our reload listener to Minecraft in our mod constructor
	// in 1.16, forge has a dedicated event for registering reload listeners
	// note that for data that needs to be on the client as well (e.g. recipes)
	// then a syncing packet containing the relevant data should be sent when a player joins or resources are reloaded
	void onServerAboutToStart(FMLServerAboutToStartEvent event)
	{
		event.getServer().getResourceManager().addReloadListener(FlavorTags.DATA_LOADER);
	}
	
	void testData(BlockEvent.BreakEvent event)
	{
		System.out.println(FlavorTags.DATA_LOADER.data);
	}
}
