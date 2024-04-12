package ChunkSender;

import ChunkSender.request.ChunkRequestHandler;
import ChunkSender.request.LocateRequestHandler;
import com.sun.net.httpserver.HttpServer;
import org.bukkit.Registry;
import org.bukkit.plugin.java.JavaPlugin;;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChunkSender extends JavaPlugin {

	private static ChunkSender instance = null;

	public static List<String> allowedAddresses = new ArrayList<>();

	public static ExecutorService threadPool;


	@Override
	public void onEnable() {
		instance = this;

		//this.saveDefaultConfig(); todo
		//allowedAddresses = getConfig().getStringList(CONFIG_ENTRY_ADDRESSES);
		allowedAddresses.add("/0:0:0:0:0:0:0:1");
		allowedAddresses.add("/127.0.0.1");
		allowedAddresses.add("/0.0.0.0");

		HttpServer server;
		try {
			server = HttpServer.create(new InetSocketAddress(8000), 0);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		server.createContext("/chunkRequest", new ChunkRequestHandler());
		server.createContext("/locate", new LocateRequestHandler());
		server.setExecutor(null);
		server.start();

		threadPool = Executors.newFixedThreadPool(12);
	}


	public static ChunkSender getInstance() {
		return instance;
	}
}
