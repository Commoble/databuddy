package commoble.databuddy.codec;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Helper class for converting maps to NBT and back using mojang's codec library.
 * 
 * Creates a map codec by combining codecs for keys and values into a codec for a map of the keys and the values.
 * Some examples of getting or creating codecs:
	 * <ul>
	 * <li> Primitives and String are found in Codec: <pre>Codec.STRING</pre>
	 * <li> Some vanilla objects that are frequently de/serialized have their own codecs: <pre>
	 *ResourceLocation.CODEC
	 *BlockState.CODEC</pre>
	 * <li> <a href=https://gist.github.com/Drullkus/1bca3f2d7f048b1fe03be97c28f87910>
	 * See Drullkus's codec primer for information on building your own codecs</a>
	 * 
	 * </ul>
 * A very short summary on what codecs are and how they are used to de/serialize things:
	 * <ul>
	 * <li> To serialize a THING to a DATA and back, you need a {@literal Codec<THING>} and a {@literal DynamicOps<DATA>}
	 * <li> A {@literal Codec<THING>} defines how to break down a complex THING down into primitive values and collections
	 * <li> A {@literal DynamicOps<DATA>} defines how to convert primitive values and collections to DATA and back
	 * <li> Vanilla minecraft comes with two types of DynamicOps:
		 * <ul>
		 * <li>NBTDynamicOps.INSTANCE to serialize to INBT
		 * <li>JsonOps.INSTANCE to serialize to JsonElements
		 * </ul>
	 * <li>An example of serializing a thing to NBT: {@code Optional<INBT> nbt = yourCodec.encodeStart(NBTDynamicOps.INSTANCE, yourThing).resultOrPartial(onError)}
		 * <ul><li>This gives you an optional that either contains your data if the serialization was successful, or an empty if it wasn't, and allows you to log or crash if unsuccessful</ul>
	 * <li>To deserialize a DATA back to a THING, use e.g. {@code Optional<THING> thing = yourCodec.parse(NBTDynamicOps.INSTANCE, someNBT).resultOrPartial(onError)}
	 * <li> .result() can also be used instead of resultOrPartial if errors are ignorable
	 * </ul>
 */
public class MapCodecHelper
{	
	/**
	 * Returns a codec for a map with string keys.
	 * The default map codec seems to require that keys be serialized to strings and back.
	 * String-keyed maps have nicer representation in NBT and json, but aren't always viable.
	 * For keys that aren't strings and can't nicely be converted to strings, see makeEntryListCodec
	 * @param <VALUE> The type of the values in the map's entries
	 * @param valueCodec A codec for the type of the values
	 * @return A codec for a map with keys of type String and values of the given type, serialized as a string-keyed map
	 */
	public static <VALUE> Codec<Map<String, VALUE>> makeStringKeyedCodec(Codec<VALUE> valueCodec)
	{
		return Codec.unboundedMap(Codec.STRING, valueCodec);
	}
	
	/**
	 * <p>Returns a codec for a map with string-representable keys.
	 * The default map codec seems to require that keys be serialized to strings and back.
	 * String-keyed maps have nicer representation in NBT and json, but aren't always viable.
	 * For keys that aren't strings and can't nicely be converted to strings, see makeEntryListCodec.</p>
	 * 
	 * <p>If you already have a key codec that serializes an object to strings, use Codec.unboundedMap(keyCodec, valueCodec).
	 * Keep in mind that this will fail to serialize your map if your key codec does not serialize your keys to strings.
	 * ResourceLocation.CODEC is a good example of something that unboundedMap is safe to use for.
	 * @param <KEY> type of the map's keys
	 * @param <VALUE> type of the map's values
	 * @param toString Function to convert keys to strings
	 * @param fromString Function to convert strings back to keys
	 * @param valueCodec A codec for the type of the values
	 * @return A codec for a map whose keys and values are the given types, serialized as a string-keyed map
	 */
	public static <KEY, VALUE> Codec<Map<KEY, VALUE>> makeStringKeyedCodec(Function<String, KEY> fromString, Function<KEY, String> toString, Codec<VALUE> valueCodec)
	{
		return Codec.unboundedMap(Codec.STRING.xmap(fromString, toString), valueCodec);
	}
	
	/**
	 * Returns a codec for a map that represents the map as a list of key-value pairs.
	 * Keys that cannot or should not be serialized as strings must be serialized in this manner instead.
	 * @param <KEY> The type of the keys in the map
	 * @param <VALUE> The type of the values in the map
	 * @param keyCodec A codec for the type of the map's keys
	 * @param valueCodec A codec for the type of the map's values
	 * @return A codec for a map whose keys and values are the given types, serialized as a list of key-value pairs
	 */
	public static <KEY, VALUE> Codec<Map<KEY, VALUE>> makeEntryListCodec(Codec<KEY> keyCodec, Codec<VALUE> valueCodec)
	{
		// Problem A: unboundedMap always serializes keys to strings, so we can't use that
		// Problem B: Pairs always serialize their left value to strings, so we can't use pairs of keys+values
		// correction to problem B: it's the pair codec that does this
		// solution: make an alternative codec for Pairs
		return MapCodecHelper.getSafePairCodec(keyCodec, valueCodec).listOf().xmap(MapCodecHelper::convertEntriesToMap, MapCodecHelper::convertMapToEntries);
	}
	
	protected static <KEY,VALUE> Codec<Pair<KEY,VALUE>> getSafePairCodec(Codec<KEY> keyCodec, Codec<VALUE> valueCodec)
	{
		return RecordCodecBuilder.create(instance ->
			instance.group(
				keyCodec.fieldOf("k").forGetter(pair -> pair.getFirst()),
				valueCodec.fieldOf("v").forGetter(pair -> pair.getSecond())
				).apply(instance, (key, value) -> Pair.of(key, value))
			);
	}
	
	protected static <KEY, VALUE> Map<KEY, VALUE> convertEntriesToMap(List<Pair<KEY, VALUE>> list)
	{
		return list.stream().collect(Pair.toMap());
	}
	
	protected static <KEY, VALUE> List<Pair<KEY, VALUE>> convertMapToEntries(Map<KEY, VALUE> map)
	{
		return map.entrySet().stream()
			.map(entry -> Pair.of(entry.getKey(), entry.getValue()))
			.collect(Collectors.toList());
	}
}
