# 2.0.0.1
* Add JsonDataProvider class, a data provider for generating jsons from codecs

# 2.0.0.0
* PluginLoader no longer requires that callers provide a logger (they can log their own things)
  * A new static method provides a list of error results, the old method has become deprecated and will be removed in 1.18
* ConfigHelper's TomlConfigOps now serializes booleans as true/false instead of 1/0 (old configs should still work)
* Removed VariatingCodec
  * It was originally believed that it was able to do things that vanilla KeyDispatchCodecs weren't but this was due to a mistaken analysis of dispatch codecs
* Removed all of the small codec helper methods