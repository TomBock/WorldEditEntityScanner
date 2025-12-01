package com.bocktom.worldEditEntities;

import com.bocktom.worldEditEntities.util.ChatUtil;
import com.bocktom.worldEditEntities.util.CountedMap;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.BukkitPlayer;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.entity.EntityType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.function.Predicate;

import static com.bocktom.worldEditEntities.WorldEditEntitiesPlugin.plugin;

public class AsyncWorldEditHelper {

	private static final Map<UUID, Integer> blockScanTasks = new ConcurrentHashMap<>();
	private static final Set<UUID> cancelledTasks = new ConcurrentSkipListSet<>();

	public static CompletableFuture<CountedMap<EntityType>> countEntitiesAsync(Player player, Predicate<EntityType> filter) {
		Region selection = getSelection(player);
		if(selection == null) {
			return CompletableFuture.completedFuture(CountedMap.empty());
		}
		long start = System.currentTimeMillis();
		player.sendMessage("§7Starting entity scan in " + selection.getChunks().size() + " §7chunks...");

		List<? extends Entity> entities = selection.getWorld().getEntities(selection);

		CountedMap<EntityType> map = new CountedMap<>();
		entities.stream()
				.filter(entity -> filter.test(entity.getType()))
				.forEach(entity -> map.increment(entity.getType()));

		map.lookupTimeMs = System.currentTimeMillis() - start;
		return CompletableFuture.completedFuture(map.sortedByValueDescending());
	}

	public static CompletableFuture<CountedMap<String>> countBlockTypesAsync(Player player, Predicate<String> filter) {
		Region selection = getSelection(player);
		if (selection == null) {
			return CompletableFuture.completedFuture(CountedMap.empty());
		}
		// Setup
		CompletableFuture<CountedMap<String>> future = new CompletableFuture<>();
		CountedMap<String> map = CountedMap.empty();
		UUID owner = player.getUniqueId();

		// Cancel any existing task for this owner
		if(blockScanTasks.containsKey(owner)) {
			Bukkit.getScheduler().cancelTask(blockScanTasks.get(owner));
			blockScanTasks.remove(owner);
		}

		long start = System.currentTimeMillis();
		player.sendMessage("§7Starting block scan in " + selection.getChunks().size() + " §7chunks...");

		// Run task asynchronously
		int taskId = Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
			int count = 0;
			int size = selection.getLength() * selection.getWidth() * selection.getHeight();

			long lastLogTime = System.currentTimeMillis();
			for (BlockVector3 vec : selection) {
				// Check for cancellation
				if(cancelledTasks.contains(owner)) {
					cancelledTasks.remove(owner);
					future.complete(CountedMap.empty());
					return;
				}

				// Check state
				com.sk89q.worldedit.world.block.BlockState state = vec.getBlock(selection.getWorld());
				String id = state.getBlockType().id();
				if (filter.test(id)) {
					map.increment(id);
				}

				// log an update every 5 seconds
				if (System.currentTimeMillis() - lastLogTime > 1_000) {
					double progress = (double) count / size * 100;
					int finalCount = count;
					Bukkit.getScheduler().runTask(plugin, () -> ChatUtil.sendProgress(player, "Scanning blocks", progress, finalCount, size));
					lastLogTime = System.currentTimeMillis();
				}

				count++;
			}


			// Complete the future here
			future.complete(map.sortedByValueDescending());
		}).getTaskId();
		blockScanTasks.put(owner, taskId);


		return future.whenComplete((result, error) -> {
			blockScanTasks.remove(owner);
			result.lookupTimeMs = System.currentTimeMillis() - start;
		});
	}

	private static Region getSelection(Player player) {
		WorldEdit we = WorldEdit.getInstance();
		BukkitPlayer wePlayer = BukkitAdapter.adapt(player);
		LocalSession session = we.getSessionManager().getIfPresent(wePlayer);
		if(session == null) {
			return null;
		}

		if(session.getSelectionWorld() == null) {
			return null;
		}

		Region selection = session.getSelection();

		if(selection == null || selection.getWorld() == null)
			return null;
		return selection;
	}

	public static void cancelBlockScanTask(Player player) {
		UUID owner = player.getUniqueId();
		if(blockScanTasks.containsKey(owner)) {
			Bukkit.getScheduler().cancelTask(blockScanTasks.get(owner));
			blockScanTasks.remove(owner);
			cancelledTasks.add(owner);
		}
	}
}
