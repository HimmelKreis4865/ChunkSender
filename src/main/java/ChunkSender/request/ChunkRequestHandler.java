package ChunkSender.request;

import ChunkSender.ChunkEncoder;
import ChunkSender.ChunkSender;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.OutputStream;

public class ChunkRequestHandler implements HttpHandler {

	final private static String queryRegex = "chunkX=[0-9\\-]+&chunkZ=[0-9\\-]+&dimension=(world_nether|world_the_end|world)";

	@Override
	public void handle(HttpExchange t) {
		String address = t.getRemoteAddress().getAddress().toString();
		try {
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

			ChunkSender.threadPool.execute(new Thread(() -> {
				try {
					byte[] response = ChunkEncoder.encodeChunk(dimension, chunkX, chunkZ);

					t.sendResponseHeaders(200, response.length);
					OutputStream os = t.getResponseBody();
					os.write(response);
					os.close();
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}));
		}catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
