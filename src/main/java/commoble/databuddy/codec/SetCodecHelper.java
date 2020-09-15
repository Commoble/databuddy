package commoble.databuddy.codec;

import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mojang.serialization.Codec;

public class SetCodecHelper
{
	public static <T> Codec<Set<T>> makeSetCodec(Codec<T> codec)
	{
		return codec.listOf().xmap(Sets::newHashSet, Lists::newArrayList);
	}
}
