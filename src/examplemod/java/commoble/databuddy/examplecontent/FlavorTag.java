package commoble.databuddy.examplecontent;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.util.ResourceLocation;

public class FlavorTag
{
	public static final Codec<FlavorTag> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.BOOL.optionalFieldOf("replace", false).forGetter(FlavorTag::getReplace),
			ResourceLocation.CODEC.listOf().fieldOf("values").forGetter(FlavorTag::getValues)
		).apply(instance, FlavorTag::new));
	
	private final boolean replace; public boolean getReplace() {return this.replace;}
	private final List<ResourceLocation> values; public List<ResourceLocation> getValues() {return this.values;}
	
	public FlavorTag(final boolean replace, final List<ResourceLocation> values)
	{
		this.replace = replace;
		this.values = values;
	}
	
	
}
