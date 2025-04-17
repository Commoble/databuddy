/*
The MIT License (MIT)
Copyright (c) 2021 Joseph Bettendorff aka "Commoble"
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

package net.commoble.databuddy.datagen;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.PackOutput.PathProvider;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.data.event.GatherDataEvent;

/**
 * Generic data provider that uses DataFixerUpper Codecs to generate jsons from
 * java objects.
 * @param registries
 * 			CompletableFuture providing registries from GatherDataEvent
 * @param packOutput
 * 			PackOutput from DataGenerator#addProvider
 * @param generator
 *			The generator instance from the GatherDataEvent
 * @param target
 *			Whether to generate data in the assets or data folder
 * @param folder
 *			The root folder of this data type in a given data domain e.g. to
 *			generate data in resources/data/modid/foods/cheeses/, use
 *			DATA for the resource type, and "foods/cheeses" for
 *			the folder name.
 * @param codec
 *			The codec that will be used to convert objects to jsons
 * @param objects
 *			An ID-to-object map that defines the objects to generate jsons
 *			from and where the jsons will be generated.
 * @param uniqueName
 * 			String uniquely identifying this dataprovider (local to your mod)
 */
public record JsonDataProvider<T>(CompletableFuture<HolderLookup.Provider> registries, PackOutput packOutput, DataGenerator generator, PackOutput.Target target, String folder, Codec<T> codec, Map<ResourceLocation,T> objects, String uniqueName) implements DataProvider
{
	private static final Logger LOGGER = LogUtils.getLogger();
	
	/**
	 * Registers a JsonDataProvider to the datagenerator using the provided GatherDataEvent, using the folder name as the unique name of this provider.
	 * If you have multiple providers for the same folder, use {@link #addNamedProvider} instead
	 * @param <T> type of things to generate files for
	 * @param event GatherDataEvent
	 * @param target DATA_PACK for server data, RESOURCE_PACK for client assets
	 * @param folder name of the data type folder to write objects to
	 * @param codec Codec to serialize objects with
	 * @param objects Objects to write
	 */
	public static <T> void addProvider(GatherDataEvent event, PackOutput.Target target, String folder, Codec<T> codec, Map<ResourceLocation,T> objects)
	{
		addNamedProvider(event, target, folder, codec, objects, folder);
	}
	
	/**
	 * Registers a JsonDataProvider to the datagenerator using the provided GatherDataEvent
	 * @param <T> type of things to generate files for
	 * @param event GatherDataEvent
	 * @param target DATA_PACK for server data, RESOURCE_PACK for client assets
	 * @param folder name of the data type folder to write objects to
	 * @param codec Codec to serialize objects with
	 * @param objects Objects to write
	 * @param name Unique name (within your mod), required by the datagen system.
	 */
	public static <T> void addNamedProvider(GatherDataEvent event, PackOutput.Target target, String folder, Codec<T> codec, Map<ResourceLocation,T> objects, String name)
	{
		DataGenerator generator = event.getGenerator();
		event.addProvider(new JsonDataProvider<>(event.getLookupProvider(), generator.getPackOutput(), generator, target, folder, codec, objects, name));
	}
	
	@Override
	public CompletableFuture<?> run(CachedOutput cache)
	{
		return registries.thenCompose(registries -> {
			PathProvider pathProvider = packOutput.createPathProvider(target, folder);
			List<CompletableFuture<?>> results = new ArrayList<>();
			for (var entry : this.objects.entrySet())
			{
				var id = entry.getKey();
				this.codec.encodeStart(registries.createSerializationContext(JsonOps.INSTANCE), entry.getValue())
					.resultOrPartial(s -> LOGGER.error("{} failed to encode {}: {}", this.getName(), id, s))
					.ifPresent(json -> {
						results.add(DataProvider.saveStable(cache, json, pathProvider.json(id)));
					});
			}
			return CompletableFuture.allOf(results.toArray(CompletableFuture[]::new));
		});
	}

	/**
	 * Gets the name of this data provider. Used by the data generator to log its root data providers.
	 */
	@Override
	public String getName()
	{
		return this.uniqueName();
	}

}