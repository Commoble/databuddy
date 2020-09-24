/*
The MIT License (MIT)
Copyright (c) 2020 Joseph Bettendorff aka "Commoble"
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

package commoble.databuddy.config;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.apache.commons.lang3.tuple.Pair;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.Builder;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.config.ModConfig.ModConfigEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;


/**
 * Config helper for automatically subscribing forge configs to config reload events as you build them. See DataBuddyExampleMod and ExampleServerConfig in the examplemod for usage examples.
 * <p><a href=https://github.com/Commoble/databuddy/blob/1.16.3/src/examplemod/java/commoble/databuddy/examplecontent/DataBuddyExampleMod.java> Example mod on github </a></p>
 * <p><a href=https://github.com/Commoble/databuddy/blob/1.16.3/src/examplemod/java/commoble/databuddy/examplecontent/ExampleServerConfig.java> Example config class on github </a></p>
 */
public class ConfigHelper
{
	/**
	 * Call this in your @Mod class constructor. This is the same as the other register method, but the contexts are assumed.
	 * @param <T> Your config class
	 * @param configType Forge config type:
	 * <ul>
	 * <li>SERVER configs are defined by the server and synced to clients
	 * <li>COMMON configs are definable by both server and clients and not synced (they may have different values)
	 * <li>CLIENT configs are defined by clients and not used on the server
	 * </ul>
	 * @param configBuilder Typically the constructor for your config class
	 * @return An instance of your config class
	 */
	public static <T> T register(
		final ModConfig.Type configType,
		final BiFunction<Builder, Subscriber, T> configBuilder)
	{
		return register(ModLoadingContext.get(), FMLJavaModLoadingContext.get(), configType, configBuilder);
	}
	
	/**
	 * Call this in your @Mod class constructor.
	 * @param <T> Your config class
	 * @param modContext mod context from ModLoadingContext.get()
	 * @param fmlContext mod context from FMLJavaModLoadingContext.get()
	 * @param configType Forge config type:
	 * <ul>
	 * <li>SERVER configs are defined by the server and synced to clients
	 * <li>COMMON configs are definable by both server and clients and not synced (they may have different values)
	 * <li>CLIENT configs are defined by clients and not used on the server
	 * </ul>
	 * @param configBuilder Typically the constructor for your config class
	 * @return An instance of your config class
	 */
	public static <T> T register(
		final ModLoadingContext modContext,
		final FMLJavaModLoadingContext fmlContext,
		final ModConfig.Type configType,
		final BiFunction<Builder, Subscriber, T> configBuilder)
	{
		final List<ConfigValueListener<?>> subscriptionList = new ArrayList<>();
		final Pair<T, ForgeConfigSpec> entry = new Builder().configure(builder -> configBuilder.apply(builder, getSubscriber(subscriptionList)));
		final T config = entry.getLeft();
		final ForgeConfigSpec spec = entry.getRight();
		
		modContext.registerConfig(configType, spec);
		
		final Consumer<ModConfigEvent> configUpdate = event ->
		{
			if(event.getConfig().getSpec() == spec)
				for(ConfigValueListener<?> value : subscriptionList)
					value.update();
		};
		
		fmlContext.getModEventBus().addListener(configUpdate);
		
		return config;
	}
	
	private static Subscriber getSubscriber(final List<ConfigValueListener<?>> list)
	{
		return new Subscriber(list);
	}
	
	/** Subscriber instances are given to your config class's constructor **/
	public static class Subscriber
	{
		final List<ConfigValueListener<?>> list;
		
		Subscriber(final List<ConfigValueListener<?>> list)
		{
			this.list = list;
		}
		
		/**
		 * Subscribe a config value to the config reload event. Use with a forge config builder to create the config values.
		 * @param <T> The type of the value the config value is configuring
		 * @param value The config value we are subscribing
		 * @return A reload-sensitive wrapper around your config value. Use listener.get() to get the most up-to-date value.
		 */
		public <T> ConfigValueListener<T> subscribe(final ConfigValue<T> value)
		{
			return ConfigValueListener.of(value, this.list);
		}
	}
	
	/** A config-reload-sensitive wrapper around your config value **/
	public static class ConfigValueListener<T> implements Supplier<T>
	{
		private T value = null;
		private final ConfigValue<T> configValue;
		
		private ConfigValueListener(final ConfigValue<T> configValue)
		{
			this.configValue = configValue;
			//this.value = configValue.get();
		}
		
		protected static <T> ConfigValueListener<T> of(final ConfigValue<T> configValue, final List<ConfigValueListener<?>> valueList)
		{
			final ConfigValueListener<T> value = new ConfigValueListener<T>(configValue);
			valueList.add(value);
			return value;
		}
		
		protected void update()
		{
			this.value = this.configValue.get();
		}

		@Override
		public T get()
		{
			if (this.value == null)
				this.update();
			return this.value;
		}
	}

}
