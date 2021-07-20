/**
 * Substantial portions of this class are heavily inspired by mezz's plugin loader for the Just Enough Items forge mod.
 * Just Enough Items is Copyright (c) 2014-2015 mezz and licensed under the MIT license
 * https://github.com/mezz/JustEnoughItems

The MIT License (MIT)

Just Enough Items is Copyright (c) 2014-2015 mezz (databuddy is not endorsed by mezz)
Databuddy is Copyright (c) 2020 Joseph Bettendorff aka "Commoble"

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.

 * 
 */

package commoble.databuddy.plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import org.apache.logging.log4j.Logger;
import org.objectweb.asm.Type;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;

import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.ModFileScanData.AnnotationData;

public class PluginLoader
{
	/**
	 * <p>Gathers all classes that implement the given interface and are annotated with the given annotation,
	 * attempts to make one instance of each of these classes, and registers them to some plugin-registering object.</p>
	 * 
	 * <p>In this manner, a mod that uses the plugin loader can grant API access to other mods if the API mod is present.</p>
	 * 
	 * <p>Classes that implement the given PLUGIN interface and are annotated with the given annotation must have an argless
	 * constructor, or that plugin will fail to load. Plugins that fail to load will be caught and logged.</p>
	 * 
	 * @param <REGISTRATOR> Interface that plugins can consume
	 * @param <PLUGIN> Plugin interface type. Must be able to consume REGISTRATORS.
	 * @param <REGISTRY> Implementation type of REGISTRATOR to register plugins to
	 * @param annotationClass Annotation class that the class scanner will look for
	 * @param pluginInterface Registrator-consuming interface class
	 * @param registrator The object to give to plugins so they can register things to it
	 * @param logger A logger to log plugin-loading information with
	 * @param loggerName A name used by the logger, can be your modid
	 * @return The registrator instance that was given, after 
	 * @deprecated TODO will be removed in 1.18.x; use {@link PluginLoader#loadPlugins(Class, Class, Object)} instead
	 */
	@Deprecated
	public static <REGISTRATOR, PLUGIN extends Consumer<REGISTRATOR>, REGISTRY extends REGISTRATOR> REGISTRY loadPlugins(
		final Class<?> annotationClass,
		final Class<PLUGIN> pluginInterface,
		final REGISTRY registrator,
		final Logger logger,
		final String loggerName)
	{
		logger.info("Loading {} plugins", loggerName);
		
		List<Pair<String,Exception>> failures = loadPlugins(annotationClass, pluginInterface, registrator);
		
		for (Pair<String,Exception> errorEntry : failures)
		{
			logger.error("Plugin loader for {} failed to load a plugin {} due to exception:", loggerName, errorEntry.getFirst());
			errorEntry.getSecond().printStackTrace();
		}
		
		logger.info("Completed loading of {} plugins", loggerName);

		return registrator;
	}

	/**
	 * <p>Gathers all classes that implement the given interface and are annotated with the given annotation,
	 * attempts to make one instance of each of these classes, and registers them to some plugin-registering object.</p>
	 * 
	 * <p>In this manner, a mod that uses the plugin loader can grant API access to other mods if the API mod is present.</p>
	 * 
	 * <p>Classes that implement the given PLUGIN interface and are annotated with the given annotation must have an argless
	 * constructor, or that plugin will fail to load. Plugins that fail to load will be caught and logged.</p>
	 * 
	 * @param <REGISTRATOR> Interface that plugins can consume
	 * @param <PLUGIN> Plugin interface type. Must be able to consume REGISTRATORS.
	 * @param <REGISTRY> Implementation type of REGISTRATOR to register plugins to
	 * @param annotationClass Annotation class that the class scanner will look for
	 * @param pluginInterface Registrator-consuming interface class
	 * @param registrator The object to give to plugins so they can register things to it
	 * @return A list of failure results; each entry contains the class name of a plugin class that could not be instantiated,
	 * and an exception that occurred when trying to instantiate it
	 */
	public static <REGISTRATOR, PLUGIN extends Consumer<REGISTRATOR>, REGISTRY extends REGISTRATOR> List<Pair<String,Exception>> loadPlugins(
		final Class<?> annotationClass,
		final Class<PLUGIN> pluginInterface,
		final REGISTRY registrator)
	{
		final Type annotationType = Type.getType(annotationClass);
		final List<Pair<String,Exception>> failures = new ArrayList<>();

		// get the names of all classes annotated with the plugin annotation
		ModList.get().getAllScanData().stream().flatMap(modData -> modData.getAnnotations().stream())
			.filter(annotationData -> Objects.equals(annotationData.getAnnotationType(), annotationType))
			.map(AnnotationData::getMemberName)
			
			// try to create instances of these classes
			.map(className -> PluginLoader.createPluginInstance(pluginInterface, className))
			// and allow them to register behaviors if they were instantiated successfully
			.forEach(either ->
			{
				either.left().ifPresent(plugin -> plugin.accept(registrator));
				either.right().ifPresent(failures::add);
			});
		
		return failures;
	}
	
	/**
	 * Attempts to create a plugin instance, given the name of the class to instantiate.
	 * @param pluginInterface The interface that plugin classes have implemented
	 * @param className The fully-qualified class name of the plugin implementation class
	 * @return An Either containing either the newly created plugin instance if successful, or a pair of the className and a thrown exception if unsuccessful 
	 */
	private static <PLUGIN> Either<PLUGIN, Pair<String,Exception>> createPluginInstance(Class<PLUGIN> pluginInterface, String className)
	{
		try
		{
			return Either.left(
				Class.forName(className) // get the exact class by name
				.asSubclass(pluginInterface) // as a subclass of Plugin
				.newInstance()); // and try to instantiate it via its argless constructor
		}
		catch (Exception e)
		{
			return Either.right(Pair.of(className,e));
		}
	}
}
