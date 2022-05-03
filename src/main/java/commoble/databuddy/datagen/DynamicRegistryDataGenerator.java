/*
The MIT License (MIT)
Copyright (c) 2022 Joseph Bettendorff aka "Commoble"
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

package commoble.databuddy.datagen;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;

import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.data.BuiltinRegistries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.data.HashCache;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.forge.event.lifecycle.GatherDataEvent;
import net.minecraftforge.registries.DeferredRegister;

/**
 * Helper for datagenerating jsons to be loaded into the dynamic/datapack registries (e.g. biomes and configured features).</br>
 * How to use it:</br>
 * <ol>
 * <li>Use {@link DeferredRegister}s to register objects to {@link BuiltinRegistries}.
 * These can be registered during datagen, objects to be read from json do not need to be registered
 * during normal client/server init. However, any dynamic registry json
 * referenced by the registered objects you generate must also itself be registered
 * (e.g. if a placed feature references a configured feature, in order for the placed feature to be generated,
 * both the placed feature and the configured feature must be registered during the datagen session).</li>
 * <li>Create a DynamicRegistryDataGenerator</li>
 * <li>For each type (registry) of object to be datagenerated, use makeDataProvider to create a datagenerator
 * and add it to the {@link GatherDataEvent}'s datagenerator</li>
 */
public record DynamicRegistryDataGenerator(DataGenerator generator, RegistryOps<JsonElement> ops)
{
	private static final Logger LOGGER = LogManager.getLogger();
	
	/**
	 * Helper for creating the DynamicRegistryDataGenerator in the preferred manner.
	 * @param generator The DataGenerator provided from the {@link GatherDataEvent}
	 * @return A DynamicRegistryDataGenerator with a registryops baked into it.
	 * This is expensive, prefer making only one per datagen session.
	 */
	public static DynamicRegistryDataGenerator create(final DataGenerator generator)
	{
		return new DynamicRegistryDataGenerator(generator, RegistryOps.create(JsonOps.INSTANCE, RegistryAccess.builtinCopy()));
	}
	
	/**
	 * Creates a dataprovider that generates a json file for each specified resource key.<br>
	 * Uses the default codec specified in {@link RegistryAccess} for the given registry.<br>
	 * @param <T> The type of json object to generate, e.g. Biome, ConfiguredFeature, etc
	 * @param modid Used for logging purposes, does not affect the namespace of generated files
	 * @param gson The gson to use to format and output the json
	 * @param registryKey The key to the registry to use. Vanilla builtin registry keys are all constants in {@link Registry}.
	 * @param keys Resource keys identifying the objects to generate jsons for.<br>
	 * Each object identified by these keys must have been previously registered to the {@link BuiltinRegistries}.<br>
	 * Using a deferred register created via {@link DeferredRegister#create(ResourceKey, String)} is the preferred way to do this.
	 * @return A data provider that can be given to {@link DataGenerator#addProvider(DataProvider)} during the {@link GatherDataEvent}.
	 */
	@SuppressWarnings("unchecked")
	@SafeVarargs
	public final <T> DataProvider makeDataProvider(final String modid, final Gson gson, final ResourceKey<Registry<T>> registryKey, final ResourceKey<T>... keys)
	{
		return makeDataProvider(modid, gson, registryKey,
			(Codec<T>)(Objects.requireNonNull(RegistryAccess.REGISTRIES.get(registryKey)).codec()), keys);
	}
	
	/**
	 * Creates a dataprovider that generates a json file for each specified resource key.<br>
	 * @param <T> The type of json object to generate, e.g. Biome, ConfiguredFeature, etc
	 * @param modid Used for logging purposes, does not affect the namespace of generated files
	 * @param gson The gson to use to format and output the json
	 * @param registryKey The key to the registry to use. Vanilla builtin registry keys are all constants in {@link Registry}.
	 * @param codec The codec to use to encode objects to json, e.g. {@link Biome#DIRECT_CODEC}.
	 * @param keys Resource keys identifying the objects to generate jsons for.<br>
	 * Each object identified by these keys must have been previously registered to the {@link BuiltinRegistries}.<br>
	 * Using a deferred register created via {@link DeferredRegister#create(ResourceKey, String)} is the preferred way to do this.
	 * @return A data provider that can be given to {@link DataGenerator#addProvider(DataProvider)} during the {@link GatherDataEvent}.
	 */
	@SafeVarargs
	public final <T> DataProvider makeDataProvider(final String modid, final Gson gson, final ResourceKey<Registry<T>> registryKey, final Codec<T> codec, final ResourceKey<T>... keys)
	{
		return new DataProvider()
		{
			@Override
			public void run(final HashCache cache) throws IOException
			{
				final RegistryOps<JsonElement> ops = DynamicRegistryDataGenerator.this.ops();
				final Registry<T> registry = ops.registry(registryKey).get();
				final Path outputFolder = DynamicRegistryDataGenerator.this.generator().getOutputFolder();
				final String dataFolder = PackType.SERVER_DATA.getDirectory();
				final ResourceLocation registryId = registryKey.location();
				// all minecraft datapack registry folders are in data/json-namespace/registry-name/
				// best practice for mods' registry folders is data/json-namespace/registry-namespace/registry-name/
				// and it is assumed that this practice is followed
				final String registryFolder = registryId.getNamespace().equals("minecraft")
					? registryId.getPath()
					: registryId.getNamespace() + "/" + registryId.getPath();
				for (final ResourceKey<T> key : keys)
				{
					final ResourceLocation id = key.location();
					final T thing = registry.getOrThrow(key);
					final Path path = outputFolder.resolve(String.join("/", dataFolder, id.getNamespace(), registryFolder, id.getPath()+".json"));
					codec.encodeStart(ops, thing)
						.resultOrPartial(msg -> LOGGER.error("Failed to encode {}: {}", path, msg)) // log error on encode failure
						.ifPresent(json -> // output to file on encode success
						{
							try
							{
								DataProvider.save(gson, cache, json, path);
							}
							catch(IOException e) // we're inside this ifpresent so the throws can't deal with this
							{
								e.printStackTrace();
							}
						});
				}
			}

			@Override
			public String getName()
			{
				return String.format("%s generator for %s", registryKey.location().toString(), modid);
			}
		};
	}
}
