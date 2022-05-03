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