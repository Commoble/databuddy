package commoble.databuddy.codec;

import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.nbt.INBT;
import net.minecraft.nbt.NBTDynamicOps;

public class NullableFieldTests
{
	public static final Codec<Integer> DEFAULT_FIELD_CODEC = RecordCodecBuilder.create(instance -> instance.group(
		NullableFieldCodec.makeDefaultableField("value", Codec.INT, 5).forGetter(x -> x)
	).apply(instance, x->x));
	
	@Test
	void testEmpty()
	{
		String emptyJson = "{}";
		int emptyResult = this.runCodec(emptyJson, DEFAULT_FIELD_CODEC);
		Assertions.assertEquals(5, emptyResult);
	}
	
	@Test
	void testPresent()
	{
		String presentJson = "{\"value\": 1}";
		int presentResult = this.runCodec(presentJson, DEFAULT_FIELD_CODEC);
		Assertions.assertEquals(1, presentResult);
	}
	
	@Test
	void testWrong()
	{
		String wrongJson = "{\"value\": \"wrong\"}";
		Optional<Pair<Integer, JsonElement>> wrongResult = DEFAULT_FIELD_CODEC.decode(JsonOps.INSTANCE, new JsonParser().parse(wrongJson)).result();
		Assertions.assertFalse(wrongResult.isPresent());
	}
	
	@Test
	void testNull()
	{
		String nullJson = "{ \"value\": null }";
		Integer nullResult = this.runCodec(nullJson, DEFAULT_FIELD_CODEC);
		Assertions.assertEquals(5, nullResult);
	}
	
	@Test
	void testNullEncoding()
	{
		int result = DEFAULT_FIELD_CODEC.encodeStart(NBTDynamicOps.INSTANCE, null)
			.flatMap(nbt -> DEFAULT_FIELD_CODEC.decode(NBTDynamicOps.INSTANCE, nbt))
			.result().get().getFirst();
		
		Assertions.assertEquals(5, result);
	}

	public static final Codec<Optional<Integer>> OPTIONAL_FIELD_CODEC = RecordCodecBuilder.create(instance -> instance.group(
		NullableFieldCodec.makeOptionalField("value", Codec.INT).forGetter(x -> x)
	).apply(instance, x->x));
	
	@Test
	void testEmptyOptional()
	{
		String emptyJson = "{}";
		Optional<Integer> emptyResult = this.runCodec(emptyJson, OPTIONAL_FIELD_CODEC);
		Assertions.assertFalse(emptyResult.isPresent());
	}
	
	@Test
	void testPresentOptional()
	{
		String presentJson = "{\"value\": 1}";
		Optional<Integer> presentResult = this.runCodec(presentJson, OPTIONAL_FIELD_CODEC);
		Assertions.assertEquals(1, presentResult.get());
	}
	
	@Test
	void testWrongOptional()
	{
		String wrongJson = "{\"value\": \"wrong\"}";
		Optional<Pair<Optional<Integer>, JsonElement>> wrongResult = OPTIONAL_FIELD_CODEC.decode(JsonOps.INSTANCE, new JsonParser().parse(wrongJson)).result();
		Assertions.assertFalse(wrongResult.isPresent());
	}
	
	@Test
	void testNullOptional()
	{
		String nullJson = "{ \"value\": null }";
		Optional<Integer> nullResult = this.runCodec(nullJson, OPTIONAL_FIELD_CODEC);
		Assertions.assertFalse(nullResult.isPresent());
	}
	
	@Test
	void testNullOptionalEncoding()
	{
		Optional<Integer> result = OPTIONAL_FIELD_CODEC.encodeStart(NBTDynamicOps.INSTANCE, Optional.empty())
			.flatMap(nbt -> OPTIONAL_FIELD_CODEC.decode(NBTDynamicOps.INSTANCE, nbt))
			.result().get().getFirst();

		Assertions.assertFalse(result.isPresent());
	}
	
	@Test
	void testNullOptionalNPE()
	{
		Assertions.assertThrows(NullPointerException.class, () ->
			OPTIONAL_FIELD_CODEC.encodeStart(NBTDynamicOps.INSTANCE, null)
				.flatMap(nbt -> OPTIONAL_FIELD_CODEC.decode(NBTDynamicOps.INSTANCE, nbt))
				.result().get().getFirst());
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
