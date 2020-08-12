/*

The MIT License (MIT)

Copyright (c) 2020 Joseph Bettendorff a.k.a. "Commoble"

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

 */

package com.github.commoble.databuddy.data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import net.minecraft.client.resources.ReloadListener;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.profiler.IProfiler;
import net.minecraft.resources.IResource;
import net.minecraft.resources.IResourceManager;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.fml.network.simple.SimpleChannel;

public class MergeableJsonDataManager<RAW, FINE> extends ReloadListener<Map<ResourceLocation, FINE>>
{
	protected static final String JSON_EXTENSION = ".json";
	protected static final int JSON_EXTENSION_LENGTH = JSON_EXTENSION.length();
	
	@Nonnull
	public Map<ResourceLocation, FINE> data = new HashMap<>();
	
	private final String folderName;
	private final Type type; // type of the raw data
	private final Gson gson;
	private final Logger logger;
	private final Function<Stream<RAW>, FINE> merger; // the function that merges objects from multiple datapacks into one object
	private Optional<Runnable> syncOnReloadCallback = Optional.empty();
	
	/**
	 * Initialize a data manager with the given data folder name and gson codec
	 * 
	 * @param folderName 
	 * The name of the data folders we are reading from,
	 * can include subfolders, e.g. "folder/subfolder"
	 * a json at src/main/resources/data/modid/foldername/jsonid.json has the id "modid:jsonid"
	 * 
	 * @param type
	 * The type of java object we are converting jsons into;
	 * This is for the RAW data type
	 * use TypeToken.get(YourThing.class).getType()
	 * or e.g. new TypeToken<Map<Thing>>(){}.getType() for collections
	 * 
	 * @param gson
	 * The gson instance used to deserialize jsons to our java objects
	 * The standard gson is pretty good at doing this, a specialized instance may be faster
	 * or overcome limitations, see here for gson usage details/examples
	 * https://github.com/google/gson/blob/master/UserGuide.md
	 * 
	 * @param logger
	 * A logger to notify users when resource errors occur
	 * 
	 * @param merger
	 * A function that converts a list of objects from multiple datapacks into a single object
	 */
	public MergeableJsonDataManager(final String folderName, Type type, final Gson gson, final Logger logger, final Function<Stream<RAW>, FINE> merger)
	{
		this.folderName = folderName;
		this.type = type;
		this.gson = gson;
		this.logger = logger;
		this.merger = merger;
	}
	
	/**
	 * Subscribes to additional events to handle the syncing of the data from server to client
	 * when players join the server or the server reloads datapacks.
	 * Call this when you instantiate the data manager (don't call more than once per data manager instance!)
	 * Also, this will probably cause crashes if this is invoked in a data loader that loads client assets,
	 * so don't do that either
	 * 
	 * @param <PACKET> A packet type. Does not need to be registered to your channel when you call withSyncingPacket,
	 * 	but does need to be registered to your channel by the time the server starts
	 * @param channel A channel that the given packet type is registered to
	 * @param packetFactory A function that produces a packet instance from the data map
	 * @return this
	 */
	public <PACKET> MergeableJsonDataManager<RAW, FINE> withSyncingPacket(SimpleChannel channel,
		Function<Map<ResourceLocation, FINE>, PACKET> packetFactory)
	{
		
		MinecraftForge.EVENT_BUS.addListener(this.getLoginListener(channel, packetFactory));
		this.syncOnReloadCallback = Optional.of(() -> channel.send(PacketDistributor.ALL.noArg(), packetFactory.apply(this.data)));
		
		return this;
	}
	
	<PACKET> Consumer<PlayerEvent.PlayerLoggedInEvent> getLoginListener(SimpleChannel channel,
		Function<Map<ResourceLocation, FINE>, PACKET> packetFactory)
	{
		return event -> {
			PlayerEntity player = event.getPlayer();
			if (player instanceof ServerPlayerEntity)
			{
				channel.send(PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity)player), packetFactory.apply(this.data));
			}
		};
	}

	/** Off-thread calculations (can include reading files) **/
	@Override
	protected Map<ResourceLocation, FINE> prepare(final IResourceManager resourceManager, final IProfiler profiler)
	{
		final Map<ResourceLocation, List<JsonObject>> map = Maps.newHashMap();
		
		for (ResourceLocation resourceLocation : resourceManager.getAllResourceLocations(this.folderName, MergeableJsonDataManager::isStringJsonFile))
		{
			final String namespace = resourceLocation.getNamespace();
			final String filePath = resourceLocation.getPath();
			final String dataPath = filePath.substring(this.folderName.length() + 1, filePath.length() - JSON_EXTENSION_LENGTH);
			
			// this is a json with identifier "somemodid:somedata"
			final ResourceLocation jsonIdentifier = new ResourceLocation(namespace, dataPath);
			// this is the list of all json objects with the given resource location (i.e. in multiple datapacks)
			final List<JsonObject> list = new ArrayList<>();
			// it's entirely possible that there are multiple jsons with this identifier,
			// we can query the resource manager for these
			try
			{
				for (IResource resource : resourceManager.getAllResources(resourceLocation))
				{
					try // with resources
					(
						final InputStream inputStream = resource.getInputStream();
						final Reader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
					)
					{
						// read the json file and save the parsed object for later
						final JsonObject jsonObject = JSONUtils.fromJson(this.gson, reader, JsonObject.class);
						list.add(jsonObject);
					}
					catch(RuntimeException | IOException exception)
					{
						this.logger.error("Data loader for {} could not read data {} from file {} in data pack {}", this.folderName, jsonIdentifier, resourceLocation, resource.getPackName(), exception); 
					}
					finally
					{
						IOUtils.closeQuietly(resource);
					}
				}
			}
			catch (IOException exception)
			{
				this.logger.error("Data loader for {} could not read data {} from file {}", this.folderName, jsonIdentifier, resourceLocation, exception);
			}
			
			
			map.put(jsonIdentifier, list);
		}

		return MergeableJsonDataManager.mapValues(map, this::processData);
	}
	
	protected FINE processData(final List<JsonObject> data)
	{
		return this.merger.apply(data.stream().map(this::getJsonAsData));
	}

	/** Use a json object  to generate a data object **/
	protected RAW getJsonAsData(final JsonObject json)
	{
		return this.gson.fromJson(json, this.type);
	}

	/** Main thread -- the processedData argument is the data generated and returned by prepare off-thread **/
	@Override
	protected void apply(final Map<ResourceLocation, FINE> processedData, final IResourceManager resourceManager, final IProfiler profiler)
	{
		this.data = processedData;
		
		this.syncOnReloadCallback.ifPresent(Runnable::run);
	}

	public static boolean isStringJsonFile(final String filename)
	{
		return filename.endsWith(JSON_EXTENSION);
	}
	
	public static <Key, In, Out> Map<Key, Out> mapValues(final Map<Key,In> inputs, final Function<In, Out> mapper)
	{
		final Map<Key,Out> newMap = new HashMap<>();
		
		inputs.forEach((key, input) -> newMap.put(key, mapper.apply(input)));
		
		return newMap;
	}
}
