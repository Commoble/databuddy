/*

The MIT License (MIT)

Copyright (c) 2022 Joseph Bettendorff a.k.a. "Commoble"

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

import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.data.DataGenerator;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraftforge.common.data.JsonCodecProvider;
import net.minecraftforge.forge.event.lifecycle.GatherDataEvent;

/**
 * Represents a parented model file. The codec can be used for datagen via {@link #addDataProvider(GatherDataEvent, String, DynamicOps, Map)}.
 */
public record SimpleModel(ResourceLocation parent, Map<String, ResourceLocation> textures)
{
	/** codec **/
	public static final Codec<SimpleModel> CODEC = RecordCodecBuilder.create(builder -> builder.group(
			ResourceLocation.CODEC.fieldOf("parent").forGetter(SimpleModel::parent),
			Codec.unboundedMap(Codec.STRING, ResourceLocation.CODEC).optionalFieldOf("textures", Map.of()).forGetter(SimpleModel::textures)
		).apply(builder, SimpleModel::new));
	
	/**
	 * Creates a DataProvider and adds the provided GatherDataEvent's datagenerator, generating in the assets/namespace/models/ folder
	 * @param event GatherDataEvent containing datagen context
	 * @param modid String modid for logging purposes
	 * @param dynamicOps DynamicOps to serialize the data to json with, e.g. JsonOps.INSTANCE
	 * @param entries Map of ResourceLocation ids to SimpleModels to serialize
	 */
	public static void addDataProvider(GatherDataEvent event, String modid, DynamicOps<JsonElement> dynamicOps, Map<ResourceLocation,SimpleModel> entries)
	{
		DataGenerator dataGenerator = event.getGenerator();
		dataGenerator.addProvider(event.includeClient(), new JsonCodecProvider<SimpleModel>(dataGenerator, event.getExistingFileHelper(), modid, dynamicOps, PackType.CLIENT_RESOURCES, "models", CODEC, entries));
	}
	
	/**
	 * @param parent Model id of the parent modek, e.g. "minecraft:block/cube_all"
	 * @return Builder-like model that allows chaining via {@link SimpleModel#addTexture(String, ResourceLocation)}
	 */
	public static SimpleModel create(ResourceLocation parent)
	{
		return new SimpleModel(parent, new HashMap<>());
	}
	
	/**
	 * Chaining method for building a SimpleModel. Call on a SimpleModel created via {@link SimpleModel#create(ResourceLocation)}
	 * @param textureName Texture key in the parent model, e.g. "texture" or "down" or "particle"
	 * @param textureId e.g. "minecraft:block/cobblestone"
	 * @return This SimpleModel, or crashes if this was called on a deserialized SimpleModel as the map will not be mutable
	 */
	public SimpleModel addTexture(String textureName, ResourceLocation textureId)
	{
		this.textures.put(textureName, textureId);
		return this;
	}
}
