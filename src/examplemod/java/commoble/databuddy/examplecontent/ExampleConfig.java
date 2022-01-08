package commoble.databuddy.examplecontent;

import java.util.List;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import commoble.databuddy.config.ConfigHelper;
import commoble.databuddy.config.ConfigHelper.ConfigObject;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;

public record ExampleConfig(
	ConfigValue<Integer> bones,
	ConfigValue<Double> bananas,
	ConfigObject<TestObject> testObject)
{
	public static ExampleConfig create(ForgeConfigSpec.Builder builder, ConfigHelper helper)
	{
		builder.push("General Category");
		
		ConfigValue<Integer> bones = builder
			.comment("Bones")
			.translation("configexample.bones")
			.defineInRange("bones", 10, 1, 20);
		
		ConfigValue<Double> bananas = builder
			.comment("Bananas")
			.translation("configexample.bananas")
			.defineInRange("bananas", 0.5D, -10D, Double.MAX_VALUE);
		
		// comments and such for ConfigObjects can be defined before defining the object
		builder.comment("Example object");
		ConfigObject<TestObject> testObject = helper.defineObject("testObject", TestObject.CODEC,
			new TestObject(new BlockPos(3,9,-41),
				Lists.newArrayList(
					new ResourceLocation("minecraft:dirt"),
					new ResourceLocation("minecraft:clay"),
					new ResourceLocation("minecraft:iron")),
					true));
		
		builder.pop();
		
		return new ExampleConfig(bones, bananas, testObject);
	}
	
	public static record TestObject(BlockPos pos, List<ResourceLocation> ids, boolean bool)
	{
		public static final Codec<TestObject> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				BlockPos.CODEC.fieldOf("pos").forGetter(TestObject::pos),
				ResourceLocation.CODEC.listOf().fieldOf("ids").forGetter(TestObject::ids),
				Codec.BOOL.fieldOf("bool").forGetter(TestObject::bool)
			).apply(instance, TestObject::new));	
	}
}
