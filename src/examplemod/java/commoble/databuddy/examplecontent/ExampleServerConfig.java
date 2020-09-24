package commoble.databuddy.examplecontent;

import commoble.databuddy.config.ConfigHelper;
import commoble.databuddy.config.ConfigHelper.ConfigValueListener;
import net.minecraftforge.common.ForgeConfigSpec;

public class ExampleServerConfig
{
	public ConfigValueListener<Integer> bones;
	public ConfigValueListener<Double> bananas;

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
		builder.pop();
	}
}
