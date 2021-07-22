/**
 * 
The MIT License (MIT)

Copyright (c) 2019 Joseph Bettendorff aka "Commoble"

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
package commoble.databuddy.nbt;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.IntStream;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraftforge.common.util.Constants;

/**
 * Helper class for writing a Map into a CompoundTag
 * 
 * @param <KEY> the type of the Map's keys
 * @param <VALUE> the type of the Map's values
 * @author Joseph aka Commoble
 * @deprecated prefer using Codecs to read and write NBT where possible
 */
@Deprecated
public class NBTMapCodec<KEY, VALUE>
{
	private final String name;
	private final BiConsumer<CompoundTag, KEY> keyWriter;
	private final Function<CompoundTag, KEY> keyReader;
	private final BiConsumer<CompoundTag, VALUE> valueWriter;
	private final Function<CompoundTag, VALUE> valueReader;
	
	/**
	 * @param name A unique identifier for the hashmap, allowing the map to be written into a CompoundTag alongside other data
	 * @param keyWriter A function that, given a CompoundTag and a Key, writes that Key into the NBT
	 * @param keyReader A function that, given a CompoundTag, returns the Key written in that NBT
	 * @param valueWriter A function that, given a CompoundTag and a Value, writes that Value into the NBT
	 * @param valueReader A Function that ,given a CompoundTag, returns the Value written in that NBT
	 */
	public NBTMapCodec(
			String name,
			BiConsumer<CompoundTag, KEY> keyWriter,
			Function<CompoundTag, KEY> keyReader,
			BiConsumer<CompoundTag, VALUE> valueWriter,
			Function<CompoundTag, VALUE> valueReader)
	{
		this.name = name;
		this.keyReader = keyReader;
		this.keyWriter = keyWriter;
		this.valueReader = valueReader;
		this.valueWriter = valueWriter;
	}
	
	/**
	 * Reconstructs and returns a {@literal Map<KEY,VALUE>} from a CompoundTag
	 * If the nbt used was given by this.write(map), the map returned will be a reconstruction of the original Map
	 * @param nbt The CompoundTag to read and construct the Map from
	 * @return A Map that the data contained in the CompoundTag represents
	 */
	public Map<KEY, VALUE> read(final CompoundTag nbt)
	{
		final Map<KEY, VALUE> newMap = new HashMap<>();

		final ListTag keyList = nbt.getList(this.name, Constants.NBT.TAG_COMPOUND);
		if (keyList == null)
			return newMap;
		
		final int keyListSize = keyList.size();
		
		if (keyListSize <= 0)
			return newMap;

		IntStream.range(0, keyListSize).mapToObj(keyIterator -> keyList.getCompound(keyIterator))
				.forEach(keyNBT -> {
					final KEY key = this.keyReader.apply(keyNBT);
					final VALUE value = this.valueReader.apply(keyNBT);
					
					newMap.put(key, value);
				});

		return newMap;
	}
	
	/**
	 * Given a map and a CompoundTag, writes the map into the NBT
	 * The same CompoundTag can be given to this.read to retrieve that map
	 * @param map A {@literal Map<KEY,VALUE>}
	 * @param compound A CompoundTag to write the map into
	 * @return a CompoundTag that, when used as the argument to this.read(nbt), will cause that function to reconstruct and return a copy of the original map
	 */
	public CompoundTag write(final Map<KEY,VALUE> map, final CompoundTag compound)
	{
		final ListTag listOfKeys = new ListTag();
		map.entrySet().forEach(entry ->
		{
			final KEY key = entry.getKey();
			final VALUE value = entry.getValue();
			
			final CompoundTag entryNBT = new CompoundTag();
			this.keyWriter.accept(entryNBT, key);
			this.valueWriter.accept(entryNBT, value);
			
			listOfKeys.add(entryNBT);
		});
		
		compound.put(this.name, listOfKeys);
		
		return compound;
	}
}