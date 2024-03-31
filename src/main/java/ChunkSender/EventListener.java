package ChunkSender;

import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkPopulateEvent;

public class EventListener implements Listener {

	public void onPopulate(ChunkPopulateEvent event) {
		// todo: does this even work?
		ChunkSender.getInstance().getServer().getScheduler().scheduleSyncDelayedTask(ChunkSender.getInstance(), () -> {
			event.getChunk().unload(false);
		}, 20);
	}
}
