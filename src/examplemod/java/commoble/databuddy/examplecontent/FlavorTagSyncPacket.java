package commoble.databuddy.examplecontent;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Codec;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.PlayPayloadContext;

public class FlavorTagSyncPacket implements CustomPacketPayload
{
	public static final ResourceLocation ID = new ResourceLocation(DataBuddyExampleMod.MODID, "flavor_tags");
	
	private static final Codec<Map<ResourceLocation, Set<ResourceLocation>>> MAPPER =
		Codec.unboundedMap(ResourceLocation.CODEC, ResourceLocation.CODEC.listOf().xmap(ImmutableSet::copyOf, ImmutableList::copyOf));
	public static Map<ResourceLocation, Set<ResourceLocation>> SYNCED_DATA = new HashMap<>(); 
	
	private final Map<ResourceLocation, Set<ResourceLocation>> map;
		
	public FlavorTagSyncPacket(Map<ResourceLocation, Set<ResourceLocation>> map)
	{
		this.map = map;
	}
	
	@Override
	public void write(FriendlyByteBuf buffer)
	{
		buffer.writeNbt((CompoundTag)(MAPPER.encodeStart(NbtOps.INSTANCE, this.map).result().orElse(new CompoundTag())));
	}
	
	public static FlavorTagSyncPacket decode(FriendlyByteBuf buffer)
	{
		return new FlavorTagSyncPacket(MAPPER.parse(NbtOps.INSTANCE, buffer.readNbt()).result().orElse(new HashMap<>()));
	}
	
	public void onPacketReceived(PlayPayloadContext context)
	{
		context.workHandler().execute(this::handlePacketOnMainThread);
	}
	
	private void handlePacketOnMainThread()
	{
		SYNCED_DATA = this.map;
	}

	@Override
	public ResourceLocation id()
	{
		return ID;
	}
}
