package commoble.databuddy.nbt;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.Codec;

import commoble.databuddy.codec.MapCodecHelper;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NBTDynamicOps;
import net.minecraft.nbt.StringNBT;

public class MapCodecHelperTest
{
	@Test
	void testStringKeyedMap()
	{
		// given a map with strings as keys
		String[] strings = {"a", "b", "c"};
		int[] values = {10, 20, 30};
		Map<String, Integer> expected = new HashMap<>();
		for (int i=0; i<3; i++)
		{
			expected.put(strings[i], values[i]);
		}

		// when we use a codec to convert it to NBT and back
		Codec<Map<String, Integer>> codec = MapCodecHelper.makeStringKeyedCodec(Codec.INT);

		// then the codec should recreate the original map
		testCodec(codec, expected);
	}
	
	@Test
	void testStringableKeyMap()
	{
		// given a map with keys that can be converted to strings
		Map<Integer, String> expected = new HashMap<>();
		for (int i=0; i<10; i++)
		{
			expected.put(i, Integer.toString(i));
		}

		// when we use a codec to convert it to NBT and back
		Codec<Map<Integer, String>> codec = MapCodecHelper.makeStringKeyedCodec(Integer::decode, i -> Integer.toString(i), Codec.STRING);

		// then the codec should recreate the original map
		testCodec(codec, expected);

	}
	
	@Test
	void testEntryListMap()
	{
		// given a map
		Map<Integer, Integer> expected = new HashMap<>();
		for (int i=0; i<10; i++)
		{
			expected.put(i, i*10);
		}

		// when we use a codec to convert it to NBT and back
		Codec<Map<Integer, Integer>> codec = MapCodecHelper.makeEntryListCodec(Codec.INT, Codec.INT);

		// then the codec should recreate the original map
		testCodec(codec, expected);
	}
	
	private static <K,V> void testCodec(Codec<Map<K,V>> codec, Map<K,V> expected)
	{
		testNonEmptyMap(codec, expected);
		testEmptyMap(codec, expected);
	}
	
	// test a non-empty map
	private static <K,V> void testNonEmptyMap(Codec<Map<K,V>> codec, Map<K,V> expected)
	{
		Map<K,V> actual = toMapAndBack(codec, expected);
		
		// then the new map should match the original map, but not a different map
		Assertions.assertEquals(expected, actual);
		Assertions.assertNotEquals(new HashMap<>(), actual);
	}
	
	private static <K,V> void testEmptyMap(Codec<Map<K,V>> codec, Map<K,V> nonEmptyMap)
	{
		Map<K,V> expected = ImmutableMap.of();
		Map<K,V> actual = toMapAndBack(codec, expected);
		
		Assertions.assertEquals(expected, actual);
		Assertions.assertNotEquals(nonEmptyMap, actual);
	}
	
	private static <K, V> Map<K,V> toMapAndBack(Codec<Map<K,V>> codec, Map<K,V> map)
	{
		CompoundNBT nbt = new CompoundNBT();
		codec.encodeStart(NBTDynamicOps.INSTANCE, map)
			.resultOrPartial(e -> {throw new IllegalStateException(e);})
			.ifPresent(mapNBT -> nbt.put("test", mapNBT));
		return codec.parse(NBTDynamicOps.INSTANCE, (nbt.get("test")))
			.resultOrPartial(e -> {throw new IllegalStateException(e);})
			.orElse(new HashMap<>());
	}
	
	@Test
	void testReadingIncorrectNBTAsMap()
	{
		// given a non-compound NBT
		StringNBT input = StringNBT.of("test");
		
		// when we read it as a map
		Codec<Map<String, String>> codec = MapCodecHelper.makeStringKeyedCodec(Codec.STRING);
		
		// then it should throw an exception
		Assertions.assertThrows(IllegalArgumentException.class, () -> codec.parse(NBTDynamicOps.INSTANCE, input).result().orElseThrow(() -> new IllegalArgumentException()));
		
	}
}
