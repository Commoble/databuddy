package commoble.databuddy.examplecontent;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import com.mojang.serialization.Codec;

import commoble.databuddy.codec.ExtraCodecs;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NBTDynamicOps;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkEvent;

public class FlavorTagSyncPacket
{
	private static final Codec<Map<ResourceLocation, Set<ResourceLocation>>> MAPPER =
		Codec.unboundedMap(ResourceLocation.CODEC, ExtraCodecs.makeSetCodec(ResourceLocation.CODEC));
	
	private final Map<ResourceLocation, Set<ResourceLocation>> map;
		
	public FlavorTagSyncPacket(Map<ResourceLocation, Set<ResourceLocation>> map)
	{
		this.map = map;
	}
	
	public void encode(PacketBuffer buffer)
	{
		buffer.writeCompoundTag((CompoundNBT)(MAPPER.encodeStart(NBTDynamicOps.INSTANCE, this.map).result().orElse(new CompoundNBT())));
	}
	
	public static FlavorTagSyncPacket decode(PacketBuffer buffer)
	{
		return new FlavorTagSyncPacket(MAPPER.parse(NBTDynamicOps.INSTANCE, buffer.readCompoundTag()).result().orElse(new HashMap<>()));
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
