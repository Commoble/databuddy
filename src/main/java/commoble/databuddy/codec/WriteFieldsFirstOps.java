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

package commoble.databuddy.codec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.Sets;
import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.RecordBuilder;

import net.minecraft.resources.DelegatingOps;

/**
 * Delegating DynamicOps that, when serializing data, serializes specified fields in a given order
 * before serializing any other fields.<br>
 * This may be helpful when writing human-readable data, such as json files.<br>
 */
public class WriteFieldsFirstOps<T> extends DelegatingOps<T>
{
	/**
	 * JsonOps that writes the "type" field first when serializing records.<br>
	 * Helpful when serializing type-dispatched json objects.<br>
	 * (type field goes at the top instead of the bottom, making the json easier to read)<br>
	 */
	public static final WriteFieldsFirstOps<JsonElement> TYPE_FIRST_JSON_OPS = new WriteFieldsFirstOps<>(JsonOps.INSTANCE, "type");
	
	private static final Logger LOGGER = LogManager.getLogger();
	private final String[] priorityFields;
	private final Set<String> priorityFieldSet;
	
	/**
	 * 
	 * @param <T> Base class of serialized objects, e.g. nbt Tag or JsonElement
	 * @param delegate The actual DynamicOps to delegate to
	 * @param priorityFields Sequence of (optional) fields to write first when writing maplike objects 
	 * @return DynamicOps that, when serializing maplike objects, serializes the specified fields first (when present)
	 * before serializing other fields in the object. Any fields in the object present in the priorityFields
	 * sequence will be written first in the specified order, followed by any fields not-present-in-priorityFields
	 * in the usual order. 
	 */
	public static <T> WriteFieldsFirstOps<T> of(final DynamicOps<T> delegate, final String... priorityFields)
	{
		return new WriteFieldsFirstOps<>(delegate, priorityFields);
	}
	
	protected WriteFieldsFirstOps(final DynamicOps<T> delegate, final String... priorityFields)
	{
		super(delegate);
		this.priorityFields = priorityFields;
		this.priorityFieldSet = Sets.newHashSet(priorityFields);
	}

	@Override
	public T createMap(final Stream<Pair<T, T>> fields)
	{
		// take the fields and reorder them such that
		// 1) all fields named in priorityFields come first, in the same order as in priorityFields
		// 2) all fields not in priorityFields come later, in the order given
		final Map<String,Pair<T,T>> foundFields = new HashMap<>();
		final List<Pair<T,T>> laterFields = new ArrayList<>();
		fields.forEach(pair ->
		{
			this.delegate.getStringValue(pair.getFirst())
				.resultOrPartial(errorString -> LOGGER.error("Expected value to be a field name but could not parse as a string: {}", errorString))
				.ifPresent(fieldName ->
				{
					if (this.priorityFieldSet.contains(fieldName))
					{
						foundFields.put(fieldName, pair);
					}
					else
					{
						laterFields.add(pair);
					}
				});
		});
		final Stream.Builder<Pair<T,T>> output = Stream.builder();
		for (String fieldName : this.priorityFields)
		{
			@Nullable var pair = foundFields.get(fieldName);
			if (pair != null)
			{
				output.accept(pair);
			}
		}
		laterFields.forEach(output::accept);
		
		return super.createMap(output.build());
	}

	@Override
	public RecordBuilder<T> mapBuilder()
	{
		return new SortingMapBuilder<>(super.mapBuilder(), this);
	}
	
	
	private static record SortingMapBuilder<T>(RecordBuilder<T> delegate, WriteFieldsFirstOps<T> writeOps) implements RecordBuilder<T>
	{
		@Override
		public DynamicOps<T> ops()
		{
			return delegate.ops();
		}

		@Override
		public RecordBuilder<T> add(T key, T value)
		{
			// we need to keep recreating our builder or we lose it
			return new SortingMapBuilder<>(delegate.add(key, value), writeOps);
		}

		@Override
		public RecordBuilder<T> add(T key, DataResult<T> value)
		{
			return new SortingMapBuilder<>(delegate.add(key, value), writeOps);
		}

		@Override
		public RecordBuilder<T> add(DataResult<T> key, DataResult<T> value)
		{
			return new SortingMapBuilder<>(delegate.add(key, value), writeOps);
		}

		@Override
		public RecordBuilder<T> withErrorsFrom(DataResult<?> result)
		{
			return new SortingMapBuilder<>(delegate.withErrorsFrom(result), writeOps);
		}

		@Override
		public RecordBuilder<T> setLifecycle(Lifecycle lifecycle)
		{
			return new SortingMapBuilder<>(delegate.setLifecycle(lifecycle), writeOps);
		}

		@Override
		public RecordBuilder<T> mapError(UnaryOperator<String> onError)
		{
			return new SortingMapBuilder<>(delegate.mapError(onError), writeOps);
		}

		@Override
		public DataResult<T> build(T prefix)
		{
			return delegate.build(prefix)
				.flatMap(writeOps::getMap)
				.map(maplike -> writeOps.createMap(maplike.entries()));
		}
	}
}
