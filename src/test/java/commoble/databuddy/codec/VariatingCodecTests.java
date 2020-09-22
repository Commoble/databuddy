package commoble.databuddy.codec;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.IntSupplier;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.nbt.INBT;
import net.minecraft.nbt.NBTDynamicOps;
import net.minecraft.util.ResourceLocation;

public class VariatingCodecTests
{
	@Test
	void testCodecs()
	{
		Map<String, Codec<Integer>> map = new HashMap<>();
//		Codec<Pair<String, Integer>> codec = Codec.STRING.dispatch("type", pair -> pair.getFirst(), s -> map.get(s));
		Codec<Pair<String, Integer>> codec = VariatingCodec.makeVariatingCodec("type", Codec.STRING, key ->
		{
			Codec<Integer> c = map.get(key);
			return c == null ? DataResult.error("No valid test codec for: " + key) : DataResult.success(c);
		});
		BiConsumer<String, Codec<Integer>> registerIntCodec = (string, c) -> {
			map.put(string, c);
		};
		registerIntCodec.accept("one", Codec.unit(1));
		registerIntCodec.accept("two", Codec.unit(2));
		registerIntCodec.accept("three", Codec.unit(3));
		registerIntCodec.accept("constant", RecordCodecBuilder.create(instance -> instance.group(
				Codec.INT.optionalFieldOf("value", 0).forGetter(x -> x)
			).apply(instance, x -> Integer.valueOf(x)))
		);
		registerIntCodec.accept("product_of_sum", RecordCodecBuilder.create(instance -> instance.group(
				Codec.INT.listOf().optionalFieldOf("sum", Lists.newArrayList(0)).forGetter(x -> Lists.newArrayList(x)),
				Codec.INT.optionalFieldOf("multiply", 1).forGetter(x -> 1)
			).apply(instance, (list, scale) -> list.stream().reduce(0, (a,b) -> a+b)*scale))
		);
		
		String unitJson = "{\r\n" + 
			"	\"type\": \"one\"\r\n" + 
			"}";
		
		Pair<String, Integer> unitPair = this.runCodec(unitJson, codec);
		
		Assertions.assertEquals("one", unitPair.getFirst());
		Assertions.assertEquals(1, unitPair.getSecond());
		
		String constantJson = "{\r\n" + 
			"	\"type\": \"constant\",\r\n" + 
			"	\"value\": 5\r\n" + 
			"}";
		
		Pair<String, Integer> constantPair = this.runCodec(constantJson, codec);
		Assertions.assertEquals("constant", constantPair.getFirst());
		Assertions.assertEquals(5, constantPair.getSecond());
		
		String productOfSumJson = "{\r\n" + 
			"	\"type\": \"product_of_sum\",\r\n" + 
			"	\"sum\": [0,1,1,2,3,5,8],\r\n" + 
			"	\"multiply\": 3\r\n" + 
			"}";
		
		Pair<String, Integer> mathPair = this.runCodec(productOfSumJson, codec);
		Assertions.assertEquals("product_of_sum", mathPair.getFirst());
		Assertions.assertEquals(60, mathPair.getSecond());
		
		String justKeyJson = "two";
		
		Pair<String, Integer> noMapPair = this.runCodec(justKeyJson, codec);
		Assertions.assertEquals("two", noMapPair.getFirst());
		Assertions.assertEquals(2, noMapPair.getSecond());
		
		// test if the singleton field gets json'd back into the value as well
		JsonElement parsedSingleton = new JsonParser().parse(justKeyJson);
		Pair<String, Integer> singletonPairFromJson = codec.decode(JsonOps.INSTANCE, parsedSingleton).result().get().getFirst();
		JsonElement encodedSingleton = codec.encodeStart(JsonOps.INSTANCE, singletonPairFromJson).result().get();
		String restringedSingleton = new Gson().fromJson(encodedSingleton, String.class);
		Assertions.assertEquals("two", restringedSingleton);
		
	}
	
	static class TwoInts
	{
		public final int a;
		public final int b;
		public TwoInts(int a, int b)
		{
			this.a = a;
			this.b = b;
		}
	}
	
	@Test
	void testCompoundData()
	{
		final Map<ResourceLocation, Codec<IntSupplier>> intMap = new HashMap<>();
		intMap.put(new ResourceLocation("test:five"), Codec.unit(() -> () -> 5));
		intMap.put(new ResourceLocation("test:constant"), RecordCodecBuilder.create(instance -> instance.group(
				Codec.INT.fieldOf("value").forGetter(x -> x.getAsInt())
			).apply(instance, x -> () -> x)));
		
		Codec<Pair<ResourceLocation, IntSupplier>> intVariantCodec = VariatingCodec.makeMapBackedVariatingCodec("type", ResourceLocation.CODEC, () -> intMap);
		Codec<CompoundData> compoundCodec = RecordCodecBuilder.create(instance -> instance.group(
				NullableFieldCodec.makeDefaultableField("int", intVariantCodec, Pair.of(new ResourceLocation("test:constant"), () -> 0)).forGetter(x -> x.intGetter),
				NullableFieldCodec.makeOptionalField("optional_int", intVariantCodec).forGetter(x -> x.optionalIntGetter)
			).apply(instance, (x,o) -> new CompoundData(x,o)));
		
//		Codec<ExpectedUseCase> codec = RecordCodecBuilder.of(getter, codec)
	
		String emptyJson = "{}";
		CompoundData emptyData = this.runCodec(emptyJson, compoundCodec);
		Assertions.assertEquals(0, emptyData.intGetter.getSecond().getAsInt());
		Assertions.assertFalse(emptyData.optionalIntGetter.isPresent());
		
		String justConcreteJson = "{\r\n" + 
			"	\"int\": \"test:five\"\r\n" + 
			"}";
		CompoundData justConcreteData = this.runCodec(justConcreteJson, compoundCodec);
		Assertions.assertEquals(5, justConcreteData.intGetter.getSecond().getAsInt());
		Assertions.assertFalse(justConcreteData.optionalIntGetter.isPresent());
		
		String justOptionalJson = "{\r\n" + 
			"	\"optional_int\": \"test:five\"\r\n" + 
			"}";
		CompoundData justOptionalData = this.runCodec(justOptionalJson, compoundCodec);
		Assertions.assertEquals(0, justOptionalData.intGetter.getSecond().getAsInt());
		Assertions.assertEquals(5, justOptionalData.optionalIntGetter.get().getSecond().getAsInt());
		
		String bothSimpleJson = "{\r\n" + 
			"	\"int\": \"test:five\",\r\n" + 
			"	\"optional_int\": \"test:five\"\r\n" + 
			"}";
		CompoundData bothSimpleData = this.runCodec(bothSimpleJson, compoundCodec);
		Assertions.assertEquals(5, bothSimpleData.intGetter.getSecond().getAsInt());
		Assertions.assertEquals(5, bothSimpleData.optionalIntGetter.get().getSecond().getAsInt());
		
		String complexJson = "{\r\n" + 
			"	\"int\": \"test:five\",\r\n" + 
			"	\"optional_int\":\r\n" + 
			"	{\r\n" + 
			"		\"type\": \"test:constant\",\r\n" + 
			"		\"value\": 10\r\n" + 
			"	}\r\n" + 
			"}";
		CompoundData complexData = this.runCodec(complexJson, compoundCodec);
		Assertions.assertEquals(5, complexData.intGetter.getSecond().getAsInt());
		Assertions.assertEquals(10, complexData.optionalIntGetter.get().getSecond().getAsInt());
		
		String badJson = "{\r\n" + 
				"	\"optional_int\": \"test:fiive\"\r\n" + 
				"}";
		Optional<Pair<CompoundData, JsonElement>> badResult = compoundCodec.decode(JsonOps.INSTANCE, new JsonParser().parse(badJson)).result();
		Assertions.assertFalse(badResult.isPresent());
		
	}
	static class CompoundData
	{
		public final Pair<ResourceLocation, IntSupplier> intGetter;
		public final Optional<Pair<ResourceLocation, IntSupplier>> optionalIntGetter;
		
		public CompoundData(Pair<ResourceLocation, IntSupplier> intGetter, Optional<Pair<ResourceLocation, IntSupplier>> optionalIntGetter)
		{
			this.intGetter = intGetter;
			this.optionalIntGetter = optionalIntGetter;
		}
	}
	
	private <T> T runCodec(String jsonString, Codec<T> codec)
	{
		JsonElement parsed = new JsonParser().parse(jsonString);
		T thingAfterRead = codec
			.decode(JsonOps.INSTANCE, parsed)
			.result()
			.get()
			.getFirst();
		INBT nbt = codec.encodeStart(NBTDynamicOps.INSTANCE, thingAfterRead).result().get();
		T thingAfterNBT = codec.decode(NBTDynamicOps.INSTANCE, nbt).result().get().getFirst();
		JsonElement jsonAgain = codec.encodeStart(JsonOps.INSTANCE, thingAfterNBT).result().get();
		T finalThing = codec.decode(JsonOps.INSTANCE, jsonAgain).result().get().getFirst();
		
		return finalThing;
	}
}
