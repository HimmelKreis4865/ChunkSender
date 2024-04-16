package ChunkSender.request;

import ChunkSender.ChunkEncoder;
import ChunkSender.ChunkSender;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.bukkit.Chunk;
import org.bukkit.craftbukkit.v1_20_R3.CraftChunk;
import org.bukkit.craftbukkit.v1_20_R3.CraftWorld;

import java.io.OutputStream;
import java.util.concurrent.CompletableFuture;

public class ChunkRequestHandler implements HttpHandler {

	final private static String queryRegex = "chunkX=[0-9\\-]+&chunkZ=[0-9\\-]+&dimension=(world_nether|world_the_end|world)";

	@Override
	public void handle(HttpExchange t) {
		String address = t.getRemoteAddress().getAddress().toString();
		if (!ChunkSender.allowedAddresses.contains(address) || !t.getRequestURI().getQuery().matches(queryRegex)) {
			return;
		}
		String[] queries = t.getRequestURI().getQuery().split("&");
		int chunkX;
		int chunkZ;
		String dimension;
		try {
			chunkX = Integer.parseInt(queries[0].replace("chunkX=", ""));
			chunkZ = Integer.parseInt(queries[1].replace("chunkZ=", ""));
			dimension = queries[2].replace("dimension=", "");
		} catch (Exception e) {
			return;
		}

		CraftWorld world = (CraftWorld) ChunkSender.getInstance().getServer().getWorld(dimension);
		assert world != null;

		CompletableFuture<Chunk> future = world.getChunkAtAsync(chunkX, chunkZ, true);
		future.whenComplete((chunk, e) -> {
			try {
				CraftChunk c = (CraftChunk) chunk;

				byte[] response = ChunkEncoder.encodeChunk(c);
				t.sendResponseHeaders(200, response.length);
				OutputStream os = t.getResponseBody();
				os.write(response);
				os.close();
			} catch (Throwable exception) {
				System.out.println("error: " + exception);
				throw new RuntimeException(exception);
			}

			/*try {

				// here


			} catch (Exception e2) {
				System.out.println("called 6 because : ");
				throw new RuntimeException(e2);
			}*/
		}).exceptionally(e -> {
			System.out.println("error station 1: " + e);
			return null;
		});
	}
}
