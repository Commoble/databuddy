/*

The MIT License (MIT)

Copyright (c) 2020 Joseph Bettendorff a.k.a. "Commoble"

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

 */

package commoble.databuddy.codec;

import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mojang.serialization.Codec;

@Deprecated
/**
 * @deprecated See ExtraCodecs.makeSetCodec
 */
public class SetCodecHelper
{
	@Deprecated
	/**
	 * @deprecated Moved to ExtraCodecs.makeSetCodec as this current class is very superfluous and will be removed in 1.17
	 * Creates a codec for a Set given a codec for T
	 * @param <T> The type to make a set of
	 * @param codec A codec for the type to make a set of
	 * @return A codec for a Set of Ts
	 */
	public static <T> Codec<Set<T>> makeSetCodec(Codec<T> codec)
	{
		return codec.listOf().xmap(Sets::newHashSet, Lists::newArrayList);
	}
}
