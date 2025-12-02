package com.bocktom.worldeditentityscanner;

import com.bocktom.worldeditentityscanner.util.ChatUtil;
import com.bocktom.worldeditentityscanner.util.CountedMap;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.BukkitPlayer;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.entity.EntityType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.function.Predicate;

import static com.bocktom.worldeditentityscanner.WorldEditEntityScannerPlugin.plugin;

public class AsyncWorldEditHelper {

	private static final Map<UUID, Integer> blockScanTasks = new ConcurrentHashMap<>();
	private static final Set<UUID> cancelledTasks = new ConcurrentSkipListSet<>();

	public static CompletableFuture<CountedMap<EntityType>> countEntitiesAsync(Player player, Region selection, Predicate<EntityType> filter) {

		long start = System.currentTimeMillis();
		player.sendMessage("§7Starting entity scan in " + selection.getChunks().size() + " §7chunks...");

		List<? extends Entity> entities = selection.getWorld().getEntities(selection);

		CountedMap<EntityType> map = new CountedMap<>();
		entities.stream()
				.filter(entity -> filter.test(entity.getType()))
				.forEach(entity -> map.increment(entity.getType()));

		return CompletableFuture.completedFuture(map.sortedByValueDescending())
				.whenComplete((result, error) -> {
					result.lookupTimeMs = System.currentTimeMillis() - start;
				});
	}

	public static CompletableFuture<CountedMap<String>> countBlockTypesAsync(Player player, Region selection, Predicate<String> filter) {
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

	public static void cancelBlockScanTask(Player player) {
		UUID owner = player.getUniqueId();
		if(blockScanTasks.containsKey(owner)) {
			Bukkit.getScheduler().cancelTask(blockScanTasks.get(owner));
			blockScanTasks.remove(owner);
			cancelledTasks.add(owner);
		}
	}

	public static Optional<Region> getSelectionOrUserRange(Player player, String[] args) {
		String userArg = null;
		int rangeArg = 5;
		for (String arg : args) {
			if(arg.startsWith("user=")) {
				userArg = arg.substring("user=".length());
			}
			if(arg.startsWith("range=")) {
				try {
					rangeArg = Integer.parseInt(arg.substring("range=".length()));
				} catch (NumberFormatException e) {
					rangeArg = 20;
				}
			}
		}
		if(rangeArg > 1_200) {
			player.sendMessage("§cRange capped to 1200 blocks.");
			rangeArg = 1_200;
		}
		// Use own selection if no user specified
		if (userArg == null || Bukkit.getPlayer(userArg) == null) {
			return getSelection(player);
		}

		Player target = Bukkit.getPlayer(userArg);
		BlockVector3 min = BukkitAdapter.asBlockVector(target.getLocation()).subtract(rangeArg, rangeArg, rangeArg);
		BlockVector3 max = BukkitAdapter.asBlockVector(target.getLocation()).add(rangeArg, rangeArg, rangeArg);

		return Optional.of(new CuboidRegion(BukkitAdapter.adapt(target.getWorld()), min, max));
	}

	private static Optional<Region> getSelection(Player player) {
		WorldEdit we = WorldEdit.getInstance();
		BukkitPlayer wePlayer = BukkitAdapter.adapt(player);
		LocalSession session = we.getSessionManager().getIfPresent(wePlayer);
		if(session == null) {
			return Optional.empty();
		}

		if(session.getSelectionWorld() == null) {
			return Optional.empty();
		}

		try {
			session.getSelection();
		} catch (IncompleteRegionException e) {
			return Optional.empty();
		}

		return Optional.of(session.getSelection());
	}
}
