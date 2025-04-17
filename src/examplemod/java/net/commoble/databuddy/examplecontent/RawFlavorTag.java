package net.commoble.databuddy.examplecontent;

import java.util.List;

import com.google.common.collect.ImmutableList;

/** To be serialized from json via GSON **/
public class RawFlavorTag
{
	/** If true, will replace data from lower-priority datapacks instead of adding to them **/
	public boolean replace = false;
	/** list of strings, e.g. "databuddy:chocolate" **/
	public List<String> values = ImmutableList.of();
}
