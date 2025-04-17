package net.commoble.databuddy.examplecontent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class FlavorTagSyncPacket implements CustomPacketPayload
{
	public static final CustomPacketPayload.Type<FlavorTagSyncPacket> ID = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DataBuddyExampleMod.MODID, "flavor_tags"));
	
	// this used to be a map of id->set but I didn't get any sleep last night and I can't brain stream codecs right now
	// this is just a test class so it doesn't actually have to be useful for anything
	public static final StreamCodec<ByteBuf, FlavorTagSyncPacket> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.map(HashMap::new, ResourceLocation.STREAM_CODEC, ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list())),
		p -> p.map,
		FlavorTagSyncPacket::new);
	public static Map<ResourceLocation, List<ResourceLocation>> SYNCED_DATA = new HashMap<>(); 
	
	private final Map<ResourceLocation, List<ResourceLocation>> map;
		
	public FlavorTagSyncPacket(Map<ResourceLocation, List<ResourceLocation>> map)
	{
		this.map = map;
	}
	
	public void onPacketReceived(IPayloadContext context)
	{
		context.enqueueWork(this::handlePacketOnMainThread);
	}
	
	private void handlePacketOnMainThread()
	{
		SYNCED_DATA = this.map;
	}

	@Override
	public Type<? extends CustomPacketPayload> type()
	{
		return ID;
	}
}
