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

package commoble.databuddy.datagen;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;

import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.data.HashCache;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;

/**
 * Generic data provider that uses DataFixerUpper Codecs to generate jsons from
 * java objects.
 * 
 * @param gson
 *            The gson instance that will be used to write JsonElements to raw
 *            text json files
 * @param generator
 *            The generator instance from the gather data event
 * @param resourceType
 *            Whether to generate data in the assets or data folder
 * @param folder
 *            The root folder of this data type in a given data domain e.g. to
 *            generate data in resources/data/modid/foods/cheeses/, use
 *            DATA for the resource type, and "foods/cheeses" for
 *            the folder name.
 * @param codec
 *            The codec that will be used to convert objects to jsons
 * @param objects
 *            An ID-to-object map that defines the objects to generate jsons
 *            from and where the jsons will be generated.
 */
public record JsonDataProvider<T>(Gson gson, DataGenerator generator, PackType resourceType, String folder, Codec<T> codec, Map<ResourceLocation,T> objects) implements DataProvider
{
    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * Takes all the objects declared by #addObject calls and writes them to json.
     * If this provider has been added to the data generator from gatherDataEvent,
     * this will be automatically called. Alternatively, other data providers can
     * invoke this in their own run methods if they choose to do so.
     */
    @Override
    public void run(HashCache cache) throws IOException
    {
        Path resourcesFolder = this.generator.getOutputFolder();
        this.objects.forEach((id, object) -> {
            Path jsonLocation = resourcesFolder.resolve(String.join("/", this.resourceType.getDirectory(), id.getNamespace(), this.folder, id.getPath() + ".json"));
            this.codec.encodeStart(JsonOps.INSTANCE, object)
                .resultOrPartial(s -> LOGGER.error("{} {} provider failed to encode {}", this.folder, this.resourceType.getDirectory(), jsonLocation, s))
                .ifPresent(jsonElement -> {
                    try
                    {
                        DataProvider.save(this.gson, cache, jsonElement, jsonLocation);
                    }
                    catch (IOException e)
                    {
                        LOGGER.error("{} {} provider failed to save {}", this.folder, this.resourceType.getDirectory(), jsonLocation, e);
                    }
                });

        });
    }

    /**
     * Gets the name of this data provider. Used by the data generator to log its root data providers.
     */
    @Override
    public String getName()
    {
        return String.format("%s %s provider", this.folder, this.resourceType.getDirectory());
    }

}