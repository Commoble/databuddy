# 7.0.0.0
* Updated to MC 1.21.5 / Neoforge 21.5.41-beta
* Removed NBTListCodec/NBTMapCodec due to nbt rewrites and increasing migration to Codecs/StreamCodecs
* Removed CodecJsonDataManager as SimpleJsonResourceReloadListener is now codec-based and very similarly implemented
* JsonDataProvider now has static methods that accept a GatherDataEvent and add a provider to the event's datagenerator
* BlockStateFile has been renamed and rewritten as BlockStateBuilder and is now primarily a helper to build vanilla BlockModelDefinitions

# 6.0.0.0
* Updated to MC 1.21 / Neoforge 21.0.40-beta
* Removed NullableFieldCodec as DFU optional fields now similarly will not silently ignore parse failures
* RegistryDispatcher no longer accepts a mod bus to create a registry dispatcher; it uses the mod bus of the modid matching the registry namespace
* RegistryDispatcher now creates a registry of mapcodecs instead of codecs
* ConfigHelper#register now requires a modid

# 5.0.0.0
* Updated to MC 1.20.4 / Neoforge 20.4.167. Some APIs have received undocumented changes as a result of this.
* Moved all packages to the net.commoble domain
* No longer providing individual sublibraries like :config at this time (feel free to paste classes into your mods if you don't need all of databuddy)
* Removed KeyedCOdecJsonDataManager, migrated its functionality to CodecJsonDataManager

# 4.0.0.1
* Temporarily added a KeyedCodecJsonDataManager class, which allows non-resourcelocation keys. This will be removed and merged into CodecJsonDataManager in the future.

# 4.0.0.0
* Updated to Minecraft 1.20.1

# 3.0.0.7
* ConfigHelper.ConfigObject now has a "set" method to set/save the config value

# 3.0.0.6
* Fix crash on server start due to MergeableCodecDataManager referring to missing ImmutableMap class

# 3.0.0.5
* Fix ConfigHelper#defineObject not working when the default value was an empty list

# 3.0.0.4
* Fix sublibrary dependencies not working in dev environments

# 3.0.0.3
* Update to forge 41.0.98+
* Add SimpleModel#createWithoutRenderType helper

# 3.0.0.2
* Fix BlockStateFile not generating `"uvlock": true` when uvlock=true

# 3.0.0.1
* Updated SimpleModel to support forge 41.0.64's block model jsons' render_type field

# 3.0.0.0
* Add SimpleModel and BlockStateFile to datagen package (alternative dataproviders for blockstate/model datagen)
* Remove JsonDataProvider and DynamicRegistryDataGenerator (superceded by forge's JsonCodecProvider)
* Remove non-static methods from ConfigHelper, it now only contains static utility methods
* Refactored RegistryDispatcher based on forge's registry rewrite. Now directly creates registries of Codecs and does not require IForgeRegistryEntries (which no longer exist)
* MergeableCodecDataManager now provides a getter to its data map, which is now immutable
* CodecDataManager and MergeableCodecDataManager no longer accept loggers on construction
* Un-deprecate NBTListCodec and NBTMapCodec as they have use-cases for reading entity/blockentity data during
server runtime, where mojang codecs may be too slow to be practical

# 2.2.0.1
* Change DynamicRegistryDataGenerator to use memoized frozen registries

# 2.2.0.0
* Add DynamicRegistryDataGenerator
* Add RegistryDispatcher
* Update to 1.18.2

# 2.1.0.2
* Make ConfigHelper more static, deprecate non-static methods for removal in MC 1.19

# 2.1.0.1
* Added WriteFieldFirstOps delegating ops to codec package
* Added public getters for the data maps in CodecJsonDataManager and MergeableCodecDataManager
* Deprecated the public value-for-key getter in CodecJsonDataManager and the public map field in MergeableCodecDataManager

# 2.1.0.0
* Updated to MC 1.18
* Refactored ConfigHelper, removed ConfigValueListeners due to forge automatically updating configs now
* Refactored data managers to use OnDatapackSyncEvent

# 2.0.0.1
* Add JsonDataProvider class, a data provider for generating jsons from codecs

# 2.0.0.0
* PluginLoader no longer requires that callers provide a logger (they can log their own things)
  * A new static method provides a list of error results, the old method has become deprecated and will be removed in 1.18
* ConfigHelper's TomlConfigOps now serializes booleans as true/false instead of 1/0 (old configs should still work)
* Removed VariatingCodec
  * It was originally believed that it was able to do things that vanilla KeyDispatchCodecs weren't but this was due to a mistaken analysis of dispatch codecs
* Removed all of the small codec helper methods