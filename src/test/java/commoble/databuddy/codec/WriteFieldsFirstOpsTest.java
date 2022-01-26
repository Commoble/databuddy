package commoble.databuddy.codec;

import java.util.function.Supplier;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public class WriteFieldsFirstOpsTest
{
	private static interface StringProviderType extends Supplier<Codec<? extends StringProvider>>
	{
		public static final Codec<StringProviderType> CODEC = Codec.STRING.xmap(STRING_PROVIDERS::get, provider -> STRING_PROVIDERS.inverse().get(provider));
	}
	private static interface StringProvider extends Supplier<String>
	{
		public static final Codec<StringProvider> CODEC = StringProviderType.CODEC.dispatch(StringProvider::getType, StringProviderType::get);
		StringProviderType getType();
	}
	private static record String2String(String s) implements StringProvider
	{
		private static final Codec<String2String> CODEC = Codec.STRING.xmap(String2String::new, String2String::s).fieldOf("a").codec();

		@Override
		public String get()
		{
			return this.s();
		}
		@Override
		public StringProviderType getType()
		{
			return STRING_PROVIDERS.get("string");
		}
	}
	private static record Int2String(int i) implements StringProvider
	{
		private static final Codec<Int2String> CODEC = Codec.INT.xmap(Int2String::new, Int2String::i).fieldOf("b").codec();
		@Override
		public String get()
		{
			return String.valueOf(this.i());
		}
		@Override
		public StringProviderType getType()
		{
			return STRING_PROVIDERS.get("int");
		}
	}
	private static record Bool2String(boolean b) implements StringProvider
	{
		private static final Codec<Bool2String> CODEC = Codec.BOOL.xmap(Bool2String::new, Bool2String::b).fieldOf("c").codec();
		@Override
		public String get()
		{
			return String.valueOf(this.b());
		}
		@Override
		public StringProviderType getType()
		{
			return STRING_PROVIDERS.get("bool");
		}
	}
	private static final BiMap<String, StringProviderType> STRING_PROVIDERS = HashBiMap.create();
	static
	{
		STRING_PROVIDERS.put("string", () -> String2String.CODEC);
		STRING_PROVIDERS.put("int", () -> Int2String.CODEC);
		STRING_PROVIDERS.put("bool", () -> Bool2String.CODEC);
	}
	
	// test type-dispatching
	
	@Test
	void typeControlCase()
	{
		Int2String five = new Int2String(5);
		JsonElement json = StringProvider.CODEC.encodeStart(JsonOps.INSTANCE, five)
			.result()
			.get();
		
		// standard behavior serializes the types in alphabetical order
		Assertions.assertEquals("{\"b\":5,\"type\":\"int\"}", json.toString());
	}
	
	@Test
	void testTypeFirst()
	{
		Int2String five = new Int2String(5);
		JsonElement json = StringProvider.CODEC.encodeStart(WriteFieldsFirstOps.TYPE_FIRST_JSON_OPS, five)
			.result()
			.get();
		Assertions.assertEquals("{\"type\":\"int\",\"b\":5}", json.toString());
	}
	
	// test RecordCodecBuilder
	
	private static record TestRecord(int a, int c, int e, int d, int b)
	{
		public static final Codec<TestRecord> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.INT.fieldOf("a").forGetter(TestRecord::a),
				Codec.INT.fieldOf("c").forGetter(TestRecord::c),
				Codec.INT.fieldOf("e").forGetter(TestRecord::e),
				Codec.INT.fieldOf("d").forGetter(TestRecord::d),
				Codec.INT.fieldOf("b").forGetter(TestRecord::b)
			).apply(instance, TestRecord::new));
	}
	
	@Test
	void recordControlCase()
	{
		TestRecord five = new TestRecord(1,3,5,4,2);
		JsonElement json = TestRecord.CODEC.encodeStart(JsonOps.INSTANCE, five)
			.result()
			.get();
		
		// default implementation seems to be only semi-deterministic
		// (order of fields is consistent but not based on alphabetical nor declaration order)
		// keep this in the tests so we can be aware if the order determinism ever changes
		Assertions.assertEquals("{\"e\":5,\"d\":4,\"b\":2,\"a\":1,\"c\":3}", json.toString());
	}
	
	@Test
	void testOrderedRecordFields()
	{
		var ops = WriteFieldsFirstOps.of(JsonOps.INSTANCE, "a","b", "cream", "cheese", "c");
		TestRecord five = new TestRecord(1,3,5,4,2);
		JsonElement json = TestRecord.CODEC.encodeStart(ops, five)
			.result()
			.get();
		
		Assertions.assertEquals("{\"a\":1,\"b\":2,\"c\":3,\"e\":5,\"d\":4}", json.toString());
	}
}
