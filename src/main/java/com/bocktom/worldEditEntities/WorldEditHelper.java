package com.bocktom.worldEditEntities;

import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.BukkitPlayer;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.entity.EntityType;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.function.Predicate;

public class WorldEditHelper {

	public static CountedMap<EntityType> getEntitiesInSelection(Player player, Predicate<EntityType> filter) {
		Region selection = getSelection(player);
		if(selection == null) {
			return CountedMap.empty();
		}

		List<? extends Entity> entities = selection.getWorld().getEntities(selection);

		CountedMap<EntityType> map = new CountedMap<>();
		entities.stream()
				.filter(entity -> filter.test(entity.getType()))
				.forEach(entity -> map.increment(entity.getType()));

		return map.sortedByValueDescending();
	}

	public static CountedMap<Material> getBlockTypesInSelection(Player player, Predicate<TileState> filter) {
		org.bukkit.World world = player.getWorld();
		Region selection = getSelection(player);

		if (selection == null) {
			return CountedMap.empty();
		}

		CountedMap<Material> map = new CountedMap<>();
		selection.forEach(vec -> {

			Block block = world.getBlockAt(vec.x(), vec.y(), vec.z());
			org.bukkit.block.BlockState state = block.getState();

			if (state instanceof TileState tileState && filter.test(tileState)) {
				map.increment(tileState.getType());
			}
		});
		return map.sortedByValueDescending();
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
}
