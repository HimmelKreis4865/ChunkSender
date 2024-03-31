package ChunkSender.request;

import ChunkSender.ChunkSender;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Climate;
import org.bukkit.Registry;
import org.bukkit.block.Biome;
import org.bukkit.craftbukkit.v1_20_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_20_R3.block.CraftBiome;
import org.bukkit.craftbukkit.v1_20_R3.generator.structure.CraftStructure;
import org.bukkit.generator.structure.Structure;

import java.io.OutputStream;
import java.util.*;

public class LocateRequestHandler implements HttpHandler {

	final private static String queryRegex = "category=(structure|biome)&type=[a-z_]+&x=[0-9\\-]+&y=[0-9\\-]+&z=[0-9\\-]+&dimension=(world_nether|world_the_end|world)";

	@Override
	public void handle(HttpExchange t) {
		String address = t.getRemoteAddress().getAddress().toString();
		try {
			if (!ChunkSender.allowedAddresses.contains(address) || !t.getRequestURI().getQuery().matches(queryRegex)) {
				return;
			}
			String[] queries = t.getRequestURI().getQuery().split("&");
			boolean isStructure;
			String type;
			int x;
			int y;
			int z;
			String dimension;
			try {
				isStructure = queries[0].replace("category=", "").equals("structure");
				type = queries[1].replace("type=", "");
				x = Integer.parseInt(queries[2].replace("x=", ""));
				y = Integer.parseInt(queries[3].replace("y=", ""));
				z = Integer.parseInt(queries[4].replace("z=", ""));
				dimension = queries[5].replace("dimension=", "");
			} catch (Exception e) {
				return;
			}

			ChunkSender.getInstance().getServer().getScheduler().callSyncMethod(ChunkSender.getInstance(), () -> {
				try {
					long nanoTime = System.nanoTime();
					CraftWorld world = (CraftWorld) ChunkSender.getInstance().getServer().getWorld(dimension);
					if (world == null) {
						return 0;
					}
					ServerLevel level = world.getHandle();
					BlockPos pos = new BlockPos(x, y, z);
					byte[] response;
					if (isStructure) {
						// structure selection
						Structure structure = org.bukkit.Registry.STRUCTURE.match(type);
						assert structure != null;
						List<Holder<net.minecraft.world.level.levelgen.structure.Structure>> holders = new ArrayList<>();
						holders.add(Holder.direct(CraftStructure.bukkitToMinecraft(structure)));
						Pair<BlockPos, Holder<net.minecraft.world.level.levelgen.structure.Structure>> result = level.getChunkSource().getGenerator().findNearestMapStructure(level, HolderSet.direct(holders), pos, 100, false);

						if (result == null) {
							response = "[]".getBytes();
						} else {
							BlockPos position = result.getFirst();
							JsonObject jsonResponse = new JsonObject();
							jsonResponse.addProperty("x", position.getX());
							jsonResponse.addProperty("y", position.getY());
							jsonResponse.addProperty("z", position.getZ());
							response = jsonResponse.toString().getBytes();
						}
					} else {
						// biome selection
						Biome biome = Registry.BIOME.match(type);
						assert biome != null;
						Set<Holder<net.minecraft.world.level.biome.Biome>> holders = new HashSet<>();
						holders.add(CraftBiome.bukkitToMinecraftHolder(biome));
						Climate.Sampler sampler = level.getChunkSource().randomState().sampler();
						Pair<BlockPos, Holder<net.minecraft.world.level.biome.Biome>> result = level.getChunkSource().getGenerator().getBiomeSource().findClosestBiome3d(pos, 100, 32, 64, holders::contains, sampler, level);


						if (result == null) {
							response = "[]".getBytes();
						} else {
							BlockPos position = result.getFirst();
							JsonObject jsonResponse = new JsonObject();
							jsonResponse.addProperty("x", position.getX());
							jsonResponse.addProperty("y", position.getY());
							jsonResponse.addProperty("z", position.getZ());
							response = jsonResponse.toString().getBytes();
						}
					}
					t.sendResponseHeaders(200, response.length);
					OutputStream os = t.getResponseBody();
					os.write(response);
					os.close();
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
				return 0;
			});
		}catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
