package com.github.commoble.databuddy.examplecontent;

import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.github.commoble.databuddy.nbt.NBTListCodec;
import com.github.commoble.databuddy.nbt.NBTMapCodec;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkEvent;

public class FlavorTagSyncPacket
{
	private static final NBTListCodec<ResourceLocation, String> LISTER = new NBTListCodec<>("values",
		NBTListCodec.ListNBTType.STRING,
		ResourceLocation::toString,
		ResourceLocation::new);
	
	private static final NBTMapCodec<ResourceLocation, Set<ResourceLocation>> MAPPER = new NBTMapCodec<>("flavor_tags",
		(nbt, key) -> nbt.putString("id", key.toString()),
		nbt -> new ResourceLocation(nbt.getString("id")),
		(nbt, set) -> LISTER.write(set.stream().collect(Collectors.toList()), nbt),
		nbt -> LISTER.read(nbt).stream().collect(Collectors.toSet())
		);
	
	private final Map<ResourceLocation, Set<ResourceLocation>> map;
		
	public FlavorTagSyncPacket(Map<ResourceLocation, Set<ResourceLocation>> map)
	{
		this.map = map;
	}
	
	public void encode(PacketBuffer buffer)
	{
		buffer.writeCompoundTag(MAPPER.write(this.map, new CompoundNBT()));
	}
	
	public static FlavorTagSyncPacket decode(PacketBuffer buffer)
	{
		return new FlavorTagSyncPacket(MAPPER.read(buffer.readCompoundTag()));
	}
	
	public void onPacketReceived(Supplier<NetworkEvent.Context> contextGetter)
	{
		NetworkEvent.Context context = contextGetter.get();
		context.enqueueWork(this::handlePacketOnMainThread);
		context.setPacketHandled(true);
	}
	
	private void handlePacketOnMainThread()
	{
		FlavorTags.DATA_LOADER.data = this.map;
	}
}
