package commoble.databuddy.examplecontent;

import java.util.ArrayList;
import java.util.List;

import net.commoble.databuddy.data.MergeableCodecDataManager;
import net.minecraft.resources.ResourceLocation;

public class FlavorTags
{
	public static final MergeableCodecDataManager<FlavorTag, List<ResourceLocation>> DATA_LOADER = new MergeableCodecDataManager<>(
		"flavors",
		FlavorTag.CODEC,
		raws -> processFlavorTags(raws));		
	
	public static List<ResourceLocation> processFlavorTags(final List<FlavorTag> raws)
	{
		return raws.stream().reduce(new ArrayList<ResourceLocation>(), FlavorTags::processFlavorTag, (a,b) -> {
			List<ResourceLocation> list = new ArrayList<>();
			list.addAll(a);
			list.addAll(b);
			return list;
		});
	}
	
	public static List<ResourceLocation> processFlavorTag(final List<ResourceLocation> set, final FlavorTag raw)
	{
		return processFlavors(raw.getReplace()? new ArrayList<>() : set, raw.getValues());
	}
	
	public static List<ResourceLocation> processFlavors(final List<ResourceLocation> set, final List<ResourceLocation> flavors)
	{
		List<ResourceLocation> list = new ArrayList<>();
		list.addAll(set);
		list.addAll(flavors);
		return list;
	}
}
