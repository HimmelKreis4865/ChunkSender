package ChunkSender;

import ChunkSender.utils.BinaryStream;
import ChunkSender.utils.NbtConverter;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.SimpleBitStorage;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import org.bukkit.craftbukkit.v1_20_R3.CraftChunk;
import org.bukkit.craftbukkit.v1_20_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_20_R3.block.CraftBiome;
import org.bukkit.craftbukkit.v1_20_R3.block.data.CraftBlockData;
import org.bukkit.craftbukkit.v1_20_R3.generator.structure.CraftStructure;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class ChunkEncoder {

	public static byte[] encodeChunk(String dimension, int chunkX, int chunkZ) {
		CraftWorld world = (CraftWorld) ChunkSender.getInstance().getServer().getWorld(dimension);
		CraftChunk iChunk = (CraftChunk) world.getChunkAt(chunkX, chunkZ);
		LevelChunk chunk = (LevelChunk) iChunk.getHandle(ChunkStatus.FULL);
		int statesStrategySize = PalettedContainer.Strategy.SECTION_STATES.size();
		int biomesStrategySize = PalettedContainer.Strategy.SECTION_BIOMES.size();
		IdMap<BlockState> blockRegistry = Block.BLOCK_STATE_REGISTRY;
		IdMap<Holder<net.minecraft.world.level.biome.Biome>> biomeRegistry = chunk.r.registryAccess().registryOrThrow(Registries.BIOME).asHolderIdMap();

		BinaryStream finalStream = new BinaryStream();

		// generating the blocks and biomes
		for (LevelChunkSection section : chunk.getSections()) {

			try {
				ByteBuf buffer = Unpooled.wrappedBuffer(new byte[section.getSerializedSize()]);
				buffer.writerIndex(0);
				FriendlyByteBuf stream = new FriendlyByteBuf(buffer);
				section.getStates().write(stream);

				buffer.readerIndex(0);
				finalStream.putByte((byte) (section.hasOnlyAir() ? 1 : 0));
				if (section.hasOnlyAir()) {
					continue;
				}


				// blocks
				byte bitsPerBlock = stream.readByte();
				finalStream.putByte(bitsPerBlock);
				if (bitsPerBlock == 0) {
					BlockState state = blockRegistry.byId(stream.readVarInt());
					finalStream.putString(stringifyBlock(state));
				} else {
					int valuesPerLong = (char) (64 / bitsPerBlock);
					int blockPaletteSize = stream.readVarInt();
					finalStream.putInt(blockPaletteSize);

					for (int i = 0; i < blockPaletteSize; i++) {
						BlockState state = blockRegistry.byId(stream.readVarInt());
						finalStream.putString(stringifyBlock(state));
					}

					long[] storage = stream.readLongArray();
					SimpleBitStorage bitStorage = new SimpleBitStorage(bitsPerBlock, statesStrategySize, storage);

					finalStream.putInt(Math.min(4096, storage.length * valuesPerLong));
					bitStorage.getAll(finalStream::putInt);
				}

				// biomes
				ByteBuf buffer2 = Unpooled.wrappedBuffer(new byte[section.getBiomes().getSerializedSize()]);
				buffer2.writerIndex(0);
				FriendlyByteBuf stream2 = new FriendlyByteBuf(buffer2);
				section.getBiomes().write(stream2);

				byte bitsPerBiome = stream2.readByte();
				finalStream.putByte(bitsPerBiome);

				if (bitsPerBiome == 0) {
					finalStream.putString(stringifyBiome(biomeRegistry.byId(stream2.readVarInt())));
					continue;
				}

				int biomePaletteSize = stream2.readVarInt();
				finalStream.putInt(biomePaletteSize);

				for (int i = 0; i < biomePaletteSize; i++) {
					finalStream.putString(stringifyBiome(biomeRegistry.byId(stream2.readVarInt())));
				}

				long[] biomeStorage = stream2.readLongArray();
				SimpleBitStorage bitBiomeStorage = new SimpleBitStorage(bitsPerBiome, biomesStrategySize, biomeStorage);

				finalStream.putInt(64); // todo: can we use this / why does the one above not work? (3 bits, 84 length)
				bitBiomeStorage.getAll(i -> finalStream.putByte((byte) i));


			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		Set<BlockPos> tiles = chunk.getBlockEntitiesPos();
		finalStream.putInt(tiles.size());

		Iterator<BlockPos> iterator = tiles.iterator();
		while(iterator.hasNext()) {
			BlockPos blockposition = iterator.next();

			CompoundTag nbt = chunk.getBlockEntityNbtForSaving(blockposition);
			assert nbt != null;
			finalStream.putString(NbtConverter.compoundTagToJson(nbt).toString());
		}

		Map<Structure, StructureStart> structures = chunk.getAllStarts();
		finalStream.putInt(structures.size());
		structures.forEach((structure, start) -> {
			net.minecraft.world.level.levelgen.structure.BoundingBox box = start.getBoundingBox();
			finalStream.putString(box.toString());
			finalStream.putString(CraftStructure.minecraftToBukkit(structure).getKey().getKey());
		});
		return finalStream.getBuffer();
	}

	public static String stringifyBlock(BlockState state) {
		assert state != null;
		CraftBlockData data = CraftBlockData.fromData(state);
		return data.getAsString();
	}

	public static String stringifyBiome(Holder<net.minecraft.world.level.biome.Biome> biomeHolder) {
		assert biomeHolder != null;
		org.bukkit.block.Biome biome = CraftBiome.minecraftHolderToBukkit(biomeHolder);

		return biome.name().toLowerCase();
	}
}
