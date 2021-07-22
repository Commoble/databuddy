package commoble.databuddy.examplecontent;

import java.util.List;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import commoble.databuddy.config.ConfigHelper;
import commoble.databuddy.config.ConfigHelper.ConfigObjectListener;
import commoble.databuddy.config.ConfigHelper.ConfigValueListener;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;

public class ExampleServerConfig
{
	public ConfigValueListener<Integer> bones;
	public ConfigValueListener<Double> bananas;
	public ConfigObjectListener<TestObject> testObject;

	public ExampleServerConfig(ForgeConfigSpec.Builder builder, ConfigHelper.Subscriber subscriber)
	{
		builder.push("General Category");
		this.bones = subscriber.subscribe(builder
			.comment("Bones")
			.translation("configexample.bones")
			.defineInRange("bones", 10, 1, 20));
		this.bananas = subscriber.subscribe(builder
			.comment("Bananas")
			.translation("configexample.bananas")
			.defineInRange("bananas", 0.5D, -10D, Double.MAX_VALUE));
		this.testObject = subscriber.subscribeObject(builder, "testObject", TestObject.CODEC,
			new TestObject(new BlockPos(3,9,-41),
				Lists.newArrayList(
					new ResourceLocation("minecraft:dirt"),
					new ResourceLocation("minecraft:clay"),
					new ResourceLocation("minecraft:iron")),
					true));
		builder.pop();
	}
	
	public static class TestObject
	{
		public static final Codec<TestObject> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				BlockPos.CODEC.fieldOf("pos").forGetter(TestObject::getPos),
				ResourceLocation.CODEC.listOf().fieldOf("ids").forGetter(TestObject::getIDs),
				Codec.BOOL.fieldOf("bool").forGetter(TestObject::getBool)
			).apply(instance, TestObject::new));
		
		private final BlockPos pos;	public BlockPos getPos() { return this.pos; }
		private final List<ResourceLocation> ids;	public List<ResourceLocation> getIDs() { return this.ids; }
		private final boolean bool; public boolean getBool() { return this.bool; }
		
		public TestObject(BlockPos pos, List<ResourceLocation> ids, boolean bool)
		{
			this.pos = pos;
			this.ids = ids;
			this.bool = bool;
		}

		@Override
		public String toString()
		{
			return String.format("Pos: {%s}, IDs: {%s}, Bool: {%s}", this.pos, this.ids, this.bool);
		}		
	}
}
