package commoble.databuddy.codec;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.toml.TomlFormat;
import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import commoble.databuddy.config.ConfigHelper.TomlConfigOps;

public class TomlConfigOpsTests
{
	@Test
	void testListConversion()
	{
		Codec<List<Integer>> codec = Codec.INT.listOf();
		List<Integer> expected = new ArrayList<>();
		expected.add(4);
		expected.add(5);
		
		DataResult<Object> encodeResult = codec.encodeStart(TomlConfigOps.INSTANCE, expected);
		Object obj = encodeResult.result().get();
		List<Integer> actual = codec.parse(TomlConfigOps.INSTANCE, obj).result().get();
		
		Assertions.assertIterableEquals(expected, actual);
	}
	
	@Test
	void testNestedConfigConversion()
	{
		NestedRecord innerA = new NestedRecord(5, Lists.newArrayList("apples", "bananas"));
		NestedRecord innerB = new NestedRecord(2, Lists.newArrayList("broccoli", "cabbage"));
		TestRecord expected = new TestRecord(0, 20, Double.MAX_VALUE, true,
			Lists.newArrayList("magnesium", "tungsten", "boron"),
			Lists.newArrayList(innerA, innerB));
		
		TestRecord actual = TestRecord.CODEC.parse(TomlConfigOps.INSTANCE, 
				TestRecord.CODEC.encodeStart(TomlConfigOps.INSTANCE, expected)
					.result()
					.get())
			.result()
			.get();
		
		Assertions.assertEquals(expected, actual);
	}
	
	@Test
	void testConfigSerialization()
	{
		NestedRecord innerA = new NestedRecord(5, Lists.newArrayList("apples", "bananas"));
		NestedRecord innerB = new NestedRecord(2, Lists.newArrayList("broccoli", "cabbage"));
		TestRecord expected = new TestRecord(0, 20, Double.MAX_VALUE, true,
			Lists.newArrayList("magnesium", "tungsten", "boron"),
			Lists.newArrayList(innerA, innerB));
		
		Object toBeSerialized = TestRecord.CODEC.encodeStart(TomlConfigOps.INSTANCE, expected)
			.result()
			.get();
		
		Config config = TomlFormat.newConfig();
		config.add("test", toBeSerialized);
		Object fromConfig = config.get("test");
		
		TestRecord actual = TestRecord.CODEC.parse(TomlConfigOps.INSTANCE, fromConfig)
			.result()
			.get();
		
		Assertions.assertEquals(expected, actual);
	}
	
	static class TestRecord
	{
		public static final Codec<TestRecord> CODEC = RecordCodecBuilder.create(instance -> instance.group(
					Codec.INT.fieldOf("x").forGetter(r -> r.x),
					Codec.INT.fieldOf("y").forGetter(r -> r.y),
					Codec.DOUBLE.fieldOf("size").forGetter(r -> r.size),
					Codec.BOOL.fieldOf("huge").forGetter(r -> r.huge),
					Codec.STRING.listOf().fieldOf("things").forGetter(r -> r.things),
					NestedRecord.CODEC.listOf().fieldOf("children").forGetter(r -> r.children)
				).apply(instance, TestRecord::new));
		
		public final int x;
		public final int y;
		public final double size;
		public final boolean huge;
		public final List<String> things;
		public final List<NestedRecord> children;
		
		public TestRecord(int x, int y, double size, boolean huge, List<String> things, List<NestedRecord> children)
		{
			this.x = x;
			this.y = y;
			this.size = size;
			this.huge = huge;
			this.things = things;
			this.children = children;
		}

		// hashcode and equals generated automatically
		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = 1;
			result = prime * result + ((this.children == null) ? 0 : this.children.hashCode());
			result = prime * result + (this.huge ? 1231 : 1237);
			long temp;
			temp = Double.doubleToLongBits(this.size);
			result = prime * result + (int) (temp ^ (temp >>> 32));
			result = prime * result + ((this.things == null) ? 0 : this.things.hashCode());
			result = prime * result + this.x;
			result = prime * result + this.y;
			return result;
		}

		@Override
		public boolean equals(Object obj)
		{
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (this.getClass() != obj.getClass())
				return false;
			TestRecord other = (TestRecord) obj;
			if (this.children == null)
			{
				if (other.children != null)
					return false;
			}
			else if (!this.children.equals(other.children))
				return false;
			if (this.huge != other.huge)
				return false;
			if (Double.doubleToLongBits(this.size) != Double.doubleToLongBits(other.size))
				return false;
			if (this.things == null)
			{
				if (other.things != null)
					return false;
			}
			else if (!this.things.equals(other.things))
				return false;
			if (this.x != other.x)
				return false;
			if (this.y != other.y)
				return false;
			return true;
		}
		
	}
	
	static class NestedRecord
	{
		public static final Codec<NestedRecord> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.INT.fieldOf("number").forGetter(r -> r.number),
			Codec.STRING.listOf().fieldOf("things").forGetter(r -> r.things)
		).apply(instance, NestedRecord::new));
		
		public final int number;
		public final List<String> things;
		
		public NestedRecord(int number, List<String> things)
		{
			this.number = number;
			this.things = things;
		}
		
		// hashcode and quals generated automatically
		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = 1;
			result = prime * result + this.number;
			result = prime * result + ((this.things == null) ? 0 : this.things.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj)
		{
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (this.getClass() != obj.getClass())
				return false;
			NestedRecord other = (NestedRecord) obj;
			if (this.number != other.number)
				return false;
			if (this.things == null)
			{
				if (other.things != null)
					return false;
			}
			else if (!this.things.equals(other.things))
				return false;
			return true;
		}
	}
}
