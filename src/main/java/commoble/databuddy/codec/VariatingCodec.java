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

package commoble.databuddy.codec;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Encoder;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;

/**
 * Like KeyDispatchCodec, but subtly different.
 * KeyDispatchCodec is difficult to work with when working with subtypes of a thing that doesn't store its own key,
 * VariatingCodec explicitly supports such a scheme.
 * In any case, VariatingCodec allows us to make codecs that can decode data of the form
 * <pre>
 * {
 * 	"type": "some:type",
 * 	"some_field_of_subtype": "data",
 * 	"some_other_field": "data"
 * }</pre>
 * where the fields beyond "type" depend on which subtype we are decoding to.
 * This implementation requires that subtype codecs define their own fields;
 * A single-value subtype codec can be represented with e.g.
 * <pre>
 * {@code 
 Codec<Integer> VALUE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.INT.optionalFieldOf("value", 0).forGetter(x -> x)
			).apply(instance, x -> Integer.valueOf(x)))}</pre>
 * If we assigned this codec to a "constant" key, a json with this codec could look like (for example)
 * <pre>
 * "some_variant_type_field":
 * {
 * 	"type": "constant",
 * 	"value": 24
 * }</pre>
 * A singleton-like or fieldless codec can be represented with e.g.
 * <pre>{@code Codec<Integer> THOUSAND_CODEC = Codec.unit(1000);}</pre>
 * A json using this codec could look like
 * <pre>
 * "some_variant_type_field":
 * {
 * 	"type": "always_thousand"
 * }</pre>
 * For singleton codecs, the below form is equivalent to the above:
 * <pre>
 * "some_variant_type_field": "always_thousand"</pre>
 */
public class VariatingCodec<K, V> extends MapCodec<Pair<K,V>>
{	
	/**
	 * Make a variating codec backed by a key-codec map. Helpful for registries-of-codecs.
	 * @param <K> The type of the subtype key, e.g. String or ResourceLocation
	 * @param <V> The type of the thing we are having subtypes of
	 * @param typeKey The string-name of the field we are getting the type key from, e.g. "type"
	 * @param keyCodec The codec for the typekey field, e.g. Codec.STRING or ResourceLocation.CODEC
	 * @param codecMapGetter A getter for a map of codecs
	 * @return a codec that uses the "type" field in an object to get a codec from the given map to determine what to decode the rest of the object as
	 */
	public static <K,V> Codec<Pair<K,V>> makeMapBackedVariatingCodec(String typeKey, final Codec<K> keyCodec, Supplier<Map<K, Codec<V>>> codecMapGetter)	{
		Function<K, ? extends DataResult<? extends Codec<? extends V>>> function = key ->
			Optional.ofNullable(codecMapGetter.get().get(key))
				.map(DataResult::success)
				.orElse(DataResult.error("No subtype registered for key: " + key.toString()));
		
		return makeVariatingCodec(typeKey, keyCodec, function);
	}
	
	/**
	 * Make a variating codec with a general-purpose subtype getter
	 * @param <K> The type of the subtype key, e.g. String or ResourceLocation
	 * @param <V> The type of the thing we are having subtypes of
	 * @param typeKey The string-name of the field we are getting the type key from, e.g. "type"
	 * @param keyCodec The codec for the typekey field, e.g. Codec.STRING or ResourceLocation.CODEC
	 * @param codecFactory A function to convert keys to subtype codecs. May return an error-holding-result if no subtype codec exists for the given key
	 * @return a codec that uses the "type" field in an object to decide what codec to decode the rest of the object as
	 */
	public static <K,V> Codec<Pair<K,V>> makeVariatingCodec(String typeKey, final Codec<K> keyCodec, final Function<K, ? extends DataResult<? extends Codec<? extends V>>> codecFactory)
	{
		Codec<Pair<K,V>> codecForMap = new VariatingCodec<>(typeKey, keyCodec, codecFactory, k -> getCodec(k, codecFactory)).codec();
		return new NoopWrapperCodec<>(typeKey, keyCodec, codecForMap);
	}
	
	private final String typeKey;
	private final Codec<K> keyCodec;
    private final Function<K, ? extends DataResult<? extends Decoder<? extends V>>> decoder;
    private final Function<K, ? extends DataResult<? extends Encoder<V>>> encoder;
    
    protected VariatingCodec(
    	String typeKey,
    	Codec<K> keyCodec,
    	Function<K, ? extends DataResult<? extends Decoder<? extends V>>> decoder,
    	Function<K, ? extends DataResult<? extends Encoder<V>>> encoder)
    {
    	this.typeKey = typeKey;
    	this.keyCodec = keyCodec;
    	this.decoder = decoder;
    	this.encoder = encoder;
    }

	@Override
	public <T> DataResult<Pair<K,V>> decode(DynamicOps<T> ops, MapLike<T> input)
	{		
		// we want to do these three things:
		// A) use the type field from the input to get the actual decoder
		// B) make a fake map without the type field
		// C) ask the decoder to decode the fake map
		final T typeValue = input.get(this.typeKey);
		if (typeValue == null)
		{
			return DataResult.error("Input does not contain a key [" + this.typeKey + "]: " + input);
		}
		
		return this.keyCodec.decode(ops, typeValue).flatMap(pair -> {
			K key = pair.getFirst();
			final DataResult<? extends Decoder<? extends V>> resultWithDecoder = this.decoder.apply(key);
			return resultWithDecoder.flatMap(subtypeDecoder -> {
				T mapWithoutType = ops.createMap(input.entries().filter(field -> ops.getStringValue(field.getFirst()).result().map(s -> !s.equals(this.typeKey)).orElse(true)));
				if (ops.compressMaps())
				{
					DataResult<V> unpairedResult = subtypeDecoder.parse(ops, mapWithoutType).map(Function.identity());
					DataResult<Pair<K,V>> pairedResult = unpairedResult.map(v -> Pair.of(key, v));
					return pairedResult;
				}
				if (subtypeDecoder instanceof MapCodecCodec<?>)
				{
					Optional<MapLike<T>> mapResult = ops.getMap(mapWithoutType).result();
					if (mapResult.isPresent())
					{
						MapLike<T> mapLike = mapResult.get();
						MapCodec<? extends V> mapCodec = ((MapCodecCodec<? extends V>) subtypeDecoder).codec();
						return mapCodec.decode(ops, mapLike).map(v -> Pair.of(key, v));
					}
				}
				return subtypeDecoder.decode(ops, mapWithoutType).map(dataPair -> dataPair.getFirst()).map(v -> Pair.of(key, v));
			});
		});
	}

	@Override
	public <T> RecordBuilder<T> encode(Pair<K,V> input, DynamicOps<T> ops, RecordBuilder<T> prefix)
	{
		// we want to do these three things:
		// A) use the type key in the input to find the encoder
		// B) use the encoder to encode V into a map, which will not have the type field
		// C) encode the type field via keyCodec
		// D) make a fake DynamicOps-element map that consists of V's map and the type field
		final DataResult<? extends Encoder<V>> resultWithEncoder = this.encoder.apply(input.getFirst());
		final Optional<? extends Encoder<V>> optionalSubtypeEncoder = resultWithEncoder.result();
		return optionalSubtypeEncoder
			.map(subtypeEncoder -> this.encodeSubtype(subtypeEncoder, ops, prefix, input.getFirst(), input.getSecond()))
			.orElse(prefix.withErrorsFrom(resultWithEncoder));
	}
	
	private <T> RecordBuilder<T> encodeSubtype(@Nonnull final Encoder<V> subtypeEncoder, final DynamicOps<T> ops, final RecordBuilder<T> prefix, K type, V input)
	{		
		final RecordBuilder<T> builder = prefix;
		if (ops.compressMaps())
		{
			// add the type field
			builder.add(this.typeKey, this.keyCodec.encodeStart(ops, type));
			// add all of the fields from the subtype encoder
			// recordbuilder must have its fields added one field at a time
			return subtypeEncoder.encodeStart(ops, input).result()
				.map(element -> this.addSubtypeFields(element, builder, ops))
				.orElse(builder);
		}
		else if (subtypeEncoder instanceof MapCodecCodec<?>)
		{
			return ((MapCodecCodec<V>) subtypeEncoder).codec().encode(input, ops, builder)
				.add(this.typeKey, this.keyCodec.encodeStart(ops, type));
		}
		else
		{
			final T typeString = ops.createString(this.typeKey);
			final DataResult<T> subtypeElementResult = subtypeEncoder.encodeStart(ops, input);
			builder.add(typeString, this.keyCodec.encodeStart(ops, type));
			return subtypeElementResult.result()
				.map(element -> this.addSubtypeFields(element, builder, ops))
				.orElse(builder);
		}
		
	}
	
	private <T> RecordBuilder<T> addSubtypeFields(@Nonnull final T subtypeElement, final RecordBuilder<T> builder, final DynamicOps<T> ops)
	{
		ops.getMap(subtypeElement).result().ifPresent(map ->
			map.entries().forEach(pair ->
				builder.add(pair.getFirst(), pair.getSecond())));
		return builder;
	}

	@Override
	public <T> Stream<T> keys(DynamicOps<T> ops)
	{
		return Stream.of(this.typeKey).map(ops::createString);
	}

    @Override
    public String toString() {
        return "VariatingCodec[" + this.keyCodec.toString() + " " + this.typeKey + " " + this.decoder + "]";
    }
    
    @SuppressWarnings("unchecked")
	private static <K,V> DataResult<? extends Encoder<V>> getCodec(final K key, final Function<? super K, ? extends DataResult<? extends Codec<? extends V>>> codecFactory)
	{
    	return codecFactory.apply(key).map(c -> (Codec<V>)c);
	}

    protected static class NoopWrapperCodec<K,V> implements Codec<Pair<K,V>>
    {
    	private final String typeKey;
    	private final Codec<K> keyCodec;
    	private final Codec<Pair<K,V>> delegate;
    	
    	protected NoopWrapperCodec(final String typeKey, final Codec<K> keyCodec, final Codec<Pair<K,V>> delegate)
    	{
    		this.typeKey = typeKey;
    		this.keyCodec = keyCodec;
    		this.delegate = delegate;
    	}

		@Override
		public <T> DataResult<T> encode(Pair<K, V> input, DynamicOps<T> ops, T prefix)
		{
			// if, after encoding, we end up with a map that only has one field named "type"
			// then we can safely encode using the keycodec instead
			DataResult<T> result = this.delegate.encode(input, ops, prefix);
			DataResult<MapLike<T>> mapResult = result.flatMap(t -> ops.getMap(t));
			Optional<T> singleTypeField = mapResult.result()
				.filter(map -> map.entries().count() == 1)
				.map(map -> map.get(this.typeKey)); // maps to empty if no field named type exists

			// if we only have the type field, return its value
			// otherwise, return the first result
			return singleTypeField.map(t -> DataResult.success(t)).orElse(result);
		}

		@Override
		public <T> DataResult<Pair<Pair<K, V>, T>> decode(DynamicOps<T> ops, T input)
		{
			// first, see if we can decode just the key
			Optional<Pair<K,T>> firstResult = this.keyCodec.decode(ops, input).result();

			// if we can, generate a fake object-with-type-field for the delegate to decode
			return firstResult.map(pair -> this.delegate
				.decode(ops, ops.createMap(
					Stream.of(
						Pair.of(
							ops.createString(this.typeKey),
							input)))))

			// if we can't, let the delegate handle it
				.orElseGet(() -> this.delegate.decode(ops, input));
		}
    	
    }
}
