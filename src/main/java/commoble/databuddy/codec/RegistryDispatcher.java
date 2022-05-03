package commoble.databuddy.codec;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import com.mojang.serialization.Codec;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryEntry;
import net.minecraftforge.registries.RegistryBuilder;

/**
 * Helper for creating a deferred register and dispatch codec for a custom registry of serializer types.
 * This allows you to register serializers for type-dispatched data, e.g. consider the following json:
 * <pre>
 * {
 *   "type": "yourmod:sometype"
 *   "data": 5
 * }
 * </pre>
 * Here, "yourmod:sometype" refers to a registered codec that defines how to read the rest of the json object.
 * @param <T> Data type -- the things that get parsed from jsons
 * @param <S> Serializer type -- the things that get registered
 * @param dispatcherCodec The codec for the serializer type.
 * @param dispatchedCodec The codec for the data type (this is what you would use for reading/writing json data)
 * @param registry The primary DeferredRegister for the serializer registry. Should only be used by the mod that created the registry.
 */
public record RegistryDispatcher<T, S extends IForgeRegistryEntry<S>>(Codec<S> dispatcherCodec, Codec<T> dispatchedCodec, DeferredRegister<S> registry)
{
	/**
	 * Helper method for creating and registering a DeferredRegister for a registry of serializers.
	 * @param <T> Data type -- the things that get parsed from jsons
	 * @param <S> Serializer type -- the things that get registered
	 * @param modBus mod bus obtained via FMLJavaModLoadingContext.get().getModEventBus()
	 * @param registryClass The class for your serializer. Only one registry can be created for a given class, and this cannot be an interface class.
	 * @param registryId The ID of your registry. Names should be singular to follow mojang's naming convention, e.g. "block", "bird"
	 * @param typeLookup A function to get the registered serializer for a given T (e.g. RuleTest::getType)
	 * @param codecLookup A function to get a sub-codec for a given serializer (e.g. RuleTestType::codec)
	 * @param extraSettings Additional registry configuration if necessary.
	 * RegistryBuilder#setName and #setType are automatically called and do not need to be called here.
	 * @return Dispatch codec and a DeferredRegister for a new custom registry of serializers;
	 * the deferred register will have been subscribed, and a forge registry will be created for it.
	 */
	public static <T, S extends IForgeRegistryEntry<S>> RegistryDispatcher<T,S> makeDispatchForgeRegistry(
		final IEventBus modBus,
		final Class<?> registryClass,
		final ResourceLocation registryId,
		final Function<T,? extends S> typeLookup,
		final Function<S, Codec<? extends T>> codecLookup,
		final Consumer<RegistryBuilder<S>> extraSettings)
	{
		@SuppressWarnings("unchecked")
		Class<S> genargifiedClass = (Class<S>)registryClass;
		DeferredRegister<S> deferredRegister = DeferredRegister.create(registryId, registryId.getNamespace());
		Supplier<RegistryBuilder<S>> builderFactory = () ->
		{
			RegistryBuilder<S> builder = new RegistryBuilder<>();
			extraSettings.accept(builder);
			return builder;
		};
		Supplier<IForgeRegistry<S>> registryGetter = deferredRegister.makeRegistry(genargifiedClass, builderFactory);
		Codec<S> dispatcherCodec = ResourceLocation.CODEC.xmap(
			id -> registryGetter.get().getValue(id),
			S::getRegistryName);
		Codec<T> dispatchedCodec = dispatcherCodec.dispatch(typeLookup, codecLookup);
		deferredRegister.register(modBus);
		
		return new RegistryDispatcher<>(dispatcherCodec, dispatchedCodec, deferredRegister);
	}
}
