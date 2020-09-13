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

import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.apache.logging.log4j.Logger;
import org.objectweb.asm.Type;

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
	 */
	public static <REGISTRATOR, PLUGIN extends Consumer<REGISTRATOR>, REGISTRY extends REGISTRATOR> REGISTRY loadPlugins(
		final Class<?> annotationClass,
		final Class<PLUGIN> pluginInterface,
		final REGISTRY registrator,
		final Logger logger,
		final String loggerName)
	{
		logger.info("Loading {} plugins", loggerName);

		final Type annotationType = Type.getType(annotationClass);

		// get the names of all classes annotated with the plugin annotation
		ModList.get().getAllScanData().stream().flatMap(modData -> modData.getAnnotations().stream())
			.filter(annotationData -> Objects.equals(annotationData.getAnnotationType(), annotationType))
			.map(AnnotationData::getMemberName)
			
			// try to create instances of these classes
			.flatMap(className -> PluginLoader.createPluginInstance(pluginInterface, className, logger, loggerName))
			// and allow them to register circuit behaviors if they were instantiated
			// successfully
			.forEach(plugin -> plugin.accept(registrator));

		logger.info("Completed loading of {} plugins", loggerName);

		return registrator;
	}
	
	/**
	 * Attempts to create a plugin instance, given the name of the class to instantiate.
	 * We use a Stream instead of Optional so the mod scan stream can flatmap it.
	 * @param pluginInterface The interface that plugin classes have implemented
	 * @param className The fully-qualified class name of the plugin implementation class
	 * @param logger the logger
	 * @param loggerName the name of the logger
	 * @return A Stream containing the instance if successful, or an empty stream otherwise.
	 */
	private static <PLUGIN> Stream<PLUGIN> createPluginInstance(Class<PLUGIN> pluginInterface, String className, Logger logger, String loggerName)
	{
		try
		{
			return Stream.of(
				Class.forName(className) // get the exact class by name
				.asSubclass(pluginInterface) // as a subclass of Plugin
				.newInstance()); // and try to instantiate it via its argless constructor
		}
		catch (Exception e)
		{
			logger.error("Failed to load {} Plugin: {}", loggerName, className, e);
			return Stream.empty();
		}
	}
}
