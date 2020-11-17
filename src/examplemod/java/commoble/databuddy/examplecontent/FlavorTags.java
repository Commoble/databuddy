package commoble.databuddy.examplecontent;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;

import com.google.common.collect.Sets;

import commoble.databuddy.data.MergeableCodecDataManager;
import net.minecraft.util.ResourceLocation;

public class FlavorTags
{
	public static final MergeableCodecDataManager<FlavorTag, Set<ResourceLocation>> DATA_LOADER = new MergeableCodecDataManager<>(
		"flavors",										// folder name
		LogManager.getLogger(),
		FlavorTag.CODEC,
		raws -> processFlavorTags(raws));					// data merger/processer					
	

	
	public static Set<ResourceLocation> processFlavorTags(final List<FlavorTag> raws)
	{
		Set<ResourceLocation> set = raws.stream().reduce(new HashSet<ResourceLocation>(), FlavorTags::processFlavorTag, Sets::union);
		
		return set;
	}
	
	public static Set<ResourceLocation> processFlavorTag(final Set<ResourceLocation> set, final FlavorTag raw)
	{
		return processFlavors(raw.getReplace()? new HashSet<>() : set, raw.getValues());
	}
	
	public static Set<ResourceLocation> processFlavors(final Set<ResourceLocation> set, final List<ResourceLocation> flavors)
	{
		set.addAll(flavors);
		return set;
	}
}
