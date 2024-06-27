package commoble.databuddy.examplecontent;

import java.util.List;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.commoble.databuddy.config.ConfigHelper;
import net.commoble.databuddy.config.ConfigHelper.ConfigObject;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;

public record ExampleConfig(
	ConfigValue<Integer> bones,
	ConfigValue<Double> bananas,
	ConfigObject<Integer> incrementableField,
	ConfigObject<TestObject> testObject,
	ConfigObject<List<Long>> list)
{
	public static ExampleConfig create(ModConfigSpec.Builder builder)
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
		
		// This object is used to test writing new values to configs via ConfigObjects
		ConfigObject<Integer> incrementableField = ConfigHelper.defineObject(builder, "incrementableField", Codec.INT, 0);
		
		// comments and such for ConfigObjects can be defined before defining the object
		builder.comment("Example object");
		ConfigObject<TestObject> testObject = ConfigHelper.defineObject(builder, "testObject", TestObject.CODEC,
			new TestObject(new BlockPos(3,9,-41),
				Lists.newArrayList(
					ResourceLocation.withDefaultNamespace("dirt"),
					ResourceLocation.withDefaultNamespace("clay"),
					ResourceLocation.withDefaultNamespace("iron")),
					true));
		
		builder.comment("Empty list");
		ConfigObject<List<Long>> list = ConfigHelper.defineObject(builder, "list", Codec.LONG.listOf(), List.of());
		
		builder.pop();
		
		return new ExampleConfig(bones, bananas, incrementableField, testObject, list);
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
