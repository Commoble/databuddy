package commoble.databuddy.codec;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.util.Direction;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.math.ChunkPos;

/**
 * Some helpful codecs for vanilla minecraft classes that may often be serialized but have no builtin codecs
 */
public class ExtraCodecs
{
	/** Serializes a ChunkPos to a record of its x- and z- coordinates **/
	public static final Codec<ChunkPos> CHUNK_POS = RecordCodecBuilder.create(instance -> instance.group(
			Codec.INT.fieldOf("x").forGetter(pos -> pos.x),
			Codec.INT.fieldOf("z").forGetter(pos -> pos.z)
		).apply(instance, ChunkPos::new));
	
	/** Serializes a ChunkPos to a long, helpful for packets or other things where smallness is good and readability isn't necessary**/
	public static final Codec<ChunkPos> COMPRESSED_CHUNK_POS = Codec.LONG.xmap(ChunkPos::new, ChunkPos::asLong);
	
	/** Serializes a Direction, should serialize as a string when serialized to NBT or uncompressed json**/
	public static final Codec<Direction> DIRECTION = IStringSerializable.createCodec(() -> Direction.values(), Direction::byName);
}
