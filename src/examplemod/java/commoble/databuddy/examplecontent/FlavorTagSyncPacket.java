package commoble.databuddy.examplecontent;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Codec;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

public class FlavorTagSyncPacket
{
	private static final Codec<Map<ResourceLocation, Set<ResourceLocation>>> MAPPER =
		Codec.unboundedMap(ResourceLocation.CODEC, ResourceLocation.CODEC.listOf().xmap(ImmutableSet::copyOf, ImmutableList::copyOf));
	
	private final Map<ResourceLocation, Set<ResourceLocation>> map;
		
	public FlavorTagSyncPacket(Map<ResourceLocation, Set<ResourceLocation>> map)
	{
		this.map = map;
	}
	
	public void encode(FriendlyByteBuf buffer)
	{
		buffer.writeNbt((CompoundTag)(MAPPER.encodeStart(NbtOps.INSTANCE, this.map).result().orElse(new CompoundTag())));
	}
	
	public static FlavorTagSyncPacket decode(FriendlyByteBuf buffer)
	{
		return new FlavorTagSyncPacket(MAPPER.parse(NbtOps.INSTANCE, buffer.readNbt()).result().orElse(new HashMap<>()));
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
