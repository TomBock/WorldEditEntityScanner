package com.bocktom.worldEditEntities;

import com.sk89q.worldedit.world.entity.EntityType;
import org.bukkit.Material;
import org.bukkit.block.TileState;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;

import static com.bocktom.worldEditEntities.FilterUtil.getFilter;

public class CountEntitiesCommand implements CommandExecutor, TabCompleter {


	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String s, @NotNull String @NotNull [] args) {
		if(!(sender instanceof Player player)) {
			sender.sendMessage("This command can only be run by a player.");
			return true;
		}

		String filterRaw = args.length > 0 ? args[0] : "";

		// Entities
		Predicate<EntityType> entityFilter = getFilter(filterRaw, FilterUtil::getEntityFilter);
		CountedMap<EntityType> entityCounts = WorldEditHelper.getEntitiesInSelection(player, entityFilter);

		if(entityCounts.isEmpty()) {
			player.sendMessage("§cNo entities found in the selected region or no region selected.");
		} else {
			player.sendMessage("§e" + entityCounts.total + " §6Entities in your selection:");
			entityCounts.forEach((type, count) -> {
				player.sendMessage("§a" + type.getName() + ": §e" + count);
			});
		}


		// Tile Entities
		Predicate<TileState> blockFilter = getFilter(filterRaw, FilterUtil::getBlockFilter);
		CountedMap<Material> blockCounts = WorldEditHelper.getBlockTypesInSelection(player, blockFilter);
		if(blockCounts.isEmpty()) {
			player.sendMessage("§cNo blocks found in the selected region or no region selected.");
		} else {
			player.sendMessage("§e" + blockCounts.total + " §6Tile Entities in your selection:");
			blockCounts.forEach((material, count) -> {
				player.sendMessage("§a" + material.name() + ": §e" + count);
			});
		}


		return true;
	}


	@Override
	public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String s, @NotNull String @NotNull [] args) {
		return List.of();
	}
}
