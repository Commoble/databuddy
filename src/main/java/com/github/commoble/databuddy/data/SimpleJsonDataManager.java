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

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import net.minecraft.client.resources.JsonReloadListener;
import net.minecraft.profiler.IProfiler;
import net.minecraft.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;

/**
 * Class for loading JSON resources that don't require anything more complicated than a basic GSON parser
 * 
 * This will NOT merge jsons from mods or datapacks that define jsons with the same ID
 * i.e. one json will be "overwritten" by the other according to datapack order, like loot tables or recipes
 * 
 * GSON user guide, reading this first is very recommended
 * https://github.com/google/gson/blob/master/UserGuide.md
 * 
 * Under certain circumstances, we don't have to tell our GSON parser how to parse our jsons
 * If we have a GSON-friendly data class, we can just parse our json with the default GSON
 * and it will just work! All we need to tell it is what type of class the json represents.
 * 
 * The very short version is that if our gson-friendly class only has fields of one of the following types:
 * 	-A json primitive (number, string, boolean)
 * 	-A gson-friendly class
 * 	-An array of json primitives or gson-friendly class objects (a class can even have arrays of its own type!)
 *  -NO GENERIC TYPES
 * And the names of our fields match the keys in the given json object, then we don't have to tell GSON how to parse our json. 
 * 
 * This class is intended to be used as a JsonReloadListener for json structures that represent such classes.
 * 
 * usage example: Let's say we have a json that looks like this

public class ImageData
{
	private String texture;	// the ResourceLocation of a texture file
	private int u;	// the x of the top-left coordinate to read the texture from
	private int v;	// the y of the top-left coordinate to read the texture from
	private int width;	// how much of the texture to draw in width
	private int height;	// how much of the texture to draw in height
}

 * and we have a json at assets/mondobook/mondobookimages/fancy_banner.json
{
	"texture": "mondobook:misc/fancy_banner",
	"width": 100,
	"height":18
}
 * this class will parse this json into the above class, using 0/false/null for unspecified values.
 * 
 * We can register an instance of class to the server data listener in the ServerAboutToStartEvent,
 * or to the client asset listener when our mod is constructed. e.g.
 *
public class AssetManagers
{
	public static final SimpleJsonDataManager<ImageData> IMAGE_DATA = new SimpleJsonDataManager<>("mondobookimages", ImageData.class);
	
	// call from mod constructor if on client
	public static void onClientInit()
	{
		IResourceManager manager = Minecraft.getInstance().getResourceManager();
		if (manager instanceof IReloadableResourceManager)
		{
			IReloadableResourceManager reloader = (IReloadableResourceManager)manager;
			reloader.addReloadListener(IMAGE_DATA);
		}
	}
}

 * And this is literally it, once your data manager is registered as a reload listener, you can call
 * yourDataManager.getData(ResourceLocation) to get the object represented by whatever json was at that location
 * (assuming you set up everything properly and minecraft has loaded resources at least once)
 * Remember that assets are loaded after your mod constructor, but before most client events,
 * so be sure to register asset reload listeners when your mod instance is constructed!
 * 
 * Again, this won't work with data classes that have fields of generic types.
 * You'll either A) define your reload listener (using GSON or otherwise),
 * or B) make a data class that doesn't have generic types and convert it to a "finished" class that does, somehow
 * 	
 **/
public class SimpleJsonDataManager<T> extends JsonReloadListener
{
	private static final Gson GSON = new GsonBuilder().create();

	
	/** The raw data that we parsed from json last time resources were reloaded **/
	protected Map<ResourceLocation, T> data = new HashMap<>();
	
	private final Class<T> dataClass;
	
	/**
	 * @param folder This is the name of the folders that the resource loader looks in, e.g. assets/modid/FOLDER
	 */
	public SimpleJsonDataManager(String folder, Class<T> dataClass)
	{
		super(GSON, folder);
		this.dataClass = dataClass;
	}
	
	/** Get the data object represented by the json at the given resource location **/
	public T getData(ResourceLocation id)
	{
		return this.data.get(id);
	}

	/** Called on resource reload, the jsons have already been found for us and we just need to parse them in here **/
	@Override
	protected void apply(Map<ResourceLocation, JsonObject> jsons, IResourceManager manager, IProfiler profiler)
	{
		this.data = SimpleJsonDataManager.mapValues(jsons, (this::getJsonAsData));
	}

	/** Use a json object (presumably one from an assets/modid/mondobooks folder) to generate a data object **/
	protected T getJsonAsData(JsonObject json)
	{
		return GSON.fromJson(json, this.dataClass);
	}
	
	/** Converts all the values in a map to new values; the new map uses the same keys as the old map **/
	
	public static <Key, In, Out> Map<Key, Out> mapValues(Map<Key,In> inputs, Function<In, Out> mapper)
	{
		Map<Key,Out> newMap = new HashMap<>();
		
		inputs.forEach((key, input) -> newMap.put(key, mapper.apply(input)));
		
		return newMap;
	}

}