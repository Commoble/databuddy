package commoble.databuddy.data;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.PacketDistributor.PacketTarget;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * @deprecated To be removed in the next version of Minecraft.
 * CodecJsonDataManager will be refactored to be like this class (allowing non-ResourceLocation keys)
 */
@Deprecated(forRemoval=true, since="4.0.0.0")
public class KeyedCodecJsonDataManager<K,V> extends SimpleJsonResourceReloadListener
{
	private static final Gson STANDARD_GSON = new Gson();
	private static final Logger LOGGER = LogManager.getLogger();
	
	/** Function to convert json ResourceLocations to Ks **/
	private final Function<ResourceLocation, DataResult<K>> keyReader;
	/** The codec we use to convert jsonelements to Vs **/
	private final Codec<V> codec;
	
	private final String folderName;
	
	/** The raw data that we parsed from json last time resources were reloaded **/
	protected Map<K, V> data = new HashMap<>();

	public KeyedCodecJsonDataManager(String folderName, Function<ResourceLocation, DataResult<K>> keyReader, Codec<V> valueCodec)
	{
		this(folderName, keyReader, valueCodec, STANDARD_GSON);
	}

	public KeyedCodecJsonDataManager(String folderName, Function<ResourceLocation, DataResult<K>> keyReader, Codec<V> valueCodec, Gson gson)
	{
		super(gson, folderName);
		this.folderName = folderName;
		this.keyReader = keyReader;
		this.codec = valueCodec;
	}
	
	/**
	 * @return The data entries
	 */
	public Map<K, V> getData()
	{
		return this.data;
	}

	@Override
	protected void apply(Map<ResourceLocation, JsonElement> jsons, ResourceManager resourceManager, ProfilerFiller profiler)
	{
		LOGGER.info("Beginning loading of data for data loader: {}", this.folderName);
		Map<K, V> newMap = new HashMap<>();

		for (Entry<ResourceLocation, JsonElement> entry : jsons.entrySet())
		{
			ResourceLocation id = entry.getKey();
			this.keyReader.apply(id)
				.resultOrPartial(errorMsg -> LOGGER.error("Error reading key {} due to: {}", id, errorMsg))
				.ifPresent(key -> {
					JsonElement element = entry.getValue();
					// if we fail to parse json, log an error and continue
					// if we succeeded, add the resulting T to the map
					this.codec.parse(JsonOps.INSTANCE, element)
						.resultOrPartial(errorMsg -> LOGGER.error("Failed to parse data json for {} due to: {}", key, errorMsg))
						.ifPresent(value -> newMap.put(key, value));
				});
		}

		this.data = newMap;
		LOGGER.info("Data loader for {} loaded {} jsons", this.folderName, this.data.size());
	}

	/**
	 * This should be called at most once, during construction of your mod (static init of your main mod class is fine)
	 * Calling this method automatically subscribes a packet-sender to {@link OnDatapackSyncEvent}.
	 * @param <PACKET> the packet type that will be sent on the given channel
	 * @param channel The networking channel of your mod
	 * @param packetFactory  A packet constructor or factory method that converts the given map to a packet object to send on the given channel
	 * @return this manager object
	 */
	public <PACKET> KeyedCodecJsonDataManager<K,V> subscribeMeAsSyncable(final SimpleChannel channel,
		final Function<Map<K, V>, PACKET> packetFactory)
	{
		MinecraftForge.EVENT_BUS.addListener(this.getDatapackSyncListener(channel, packetFactory));
		return this;
	}
	
	/** Generate an event listener function for the on-datapack-sync event **/
	private <PACKET> Consumer<OnDatapackSyncEvent> getDatapackSyncListener(final SimpleChannel channel,
		final Function<Map<K, V>, PACKET> packetFactory)
	{
		return event -> {
			ServerPlayer player = event.getPlayer();
			PACKET packet = packetFactory.apply(this.data);
			PacketTarget target = player == null
				? PacketDistributor.ALL.noArg()
				: PacketDistributor.PLAYER.with(() -> player);
			channel.send(target, packet);
		};
	}

}
