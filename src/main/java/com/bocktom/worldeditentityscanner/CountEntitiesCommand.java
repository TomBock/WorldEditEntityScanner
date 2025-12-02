package com.bocktom.worldeditentityscanner;

import com.bocktom.worldeditentityscanner.util.ChatUtil;
import com.bocktom.worldeditentityscanner.util.Config;
import com.bocktom.worldeditentityscanner.util.FilterUtil;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.entity.EntityType;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;

import static com.bocktom.worldeditentityscanner.util.FilterUtil.collectFilters;

public class CountEntitiesCommand implements CommandExecutor, TabCompleter {


	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String s, @NotNull String @NotNull [] args) {
		if(!(sender instanceof Player player)) {
			sender.sendMessage("This command can only be run by a player.");
			return true;
		}

		switch (cmd.getName()) {
			case "/scanentities" -> count(player, args, true);
			case "/scantileentities" -> count(player, args, false);
			case "/scanall" -> {
				count(player, args, true);
				count(player, args, false);
			}
			case "/scanstop" -> {
				AsyncWorldEditHelper.cancelBlockScanTask(player);
				sender.sendMessage("§7Cancelled any ongoing block scan tasks.");
			}
			default -> {
				sender.sendMessage("Unknown command.");
				return true;
			}
		}

		return true;
	}

	private void count(Player player, String[] args, boolean entity) {
		Optional<Region> selection = AsyncWorldEditHelper.getSelectionOrUserRange(player, args);
		if(selection.isEmpty()) {
			player.sendMessage("§cNo region specified.");
			return;
		}
		String filterArg = Arrays.stream(args).filter(arg -> arg.startsWith("filter="))
				.map(arg -> arg.substring("filter=".length()))
				.findFirst()
				.orElse("");

		int page = getPageFromArgs(args);

		if(entity) {
			countEntities(player, selection.get(), filterArg, page);
		} else {
			countTileEntities(player, selection.get(), filterArg, page);
		}
	}

	private void countEntities(Player player, Region selection, String filterArg, int page) {

		Predicate<EntityType> entityFilter = collectFilters(filterArg, FilterUtil::getEntityFilter);

		AsyncWorldEditHelper.countEntitiesAsync(player, selection, entityFilter)
				.thenAccept(map -> {
					if(!player.isOnline())
						return;

					if(map.isEmpty()) {
						player.sendMessage("§cNo fitting entities found.");
					} else {
						map.keyFormatter = type -> type.getName().replace("minecraft:", "");
						ChatUtil.sendTablePaged(player, map, page, "Entities");
					}
				});
	}

	private void countTileEntities(Player player, Region selection, String filterArg, int page) {

		Predicate<String> defaultBlockFilter = FilterUtil.getBlockFilter("tile_entities"); // always active
		Predicate<String> blockFilter = defaultBlockFilter.and(collectFilters(filterArg, FilterUtil::getBlockFilter));
		AsyncWorldEditHelper.countBlockTypesAsync(player, selection, blockFilter)
				.thenAccept(map -> {
					if(!player.isOnline())
						return;

					if(map.isEmpty()) {
						player.sendMessage("§cNo fitting entities found.");
					} else {
						map.keyFormatter = id -> id.replace("minecraft:", "");
						ChatUtil.sendTablePaged(player, map, page, "Tile Entities");
					}
				});
	}

	private static int getPageFromArgs(String[] args) {
		for (String arg : args) {
			if(arg.startsWith("page=")) {
				try {
					return Integer.parseInt(arg.substring("page=".length()));
				} catch (NumberFormatException e) {
					return 1;
				}
			}
		}
		return 1;
	}

	@Override
	public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String s, @NotNull String @NotNull [] args) {
		List<String> completions = new ArrayList<>(List.of("user=", "range=", "filter="));

		if(args.length > 0 && args[args.length - 1].startsWith("user=")) {
			return Bukkit.getOnlinePlayers().stream().map(user -> "user=" + user.getName()).toList();
		}

		if(args.length > 0 && args[args.length - 1].startsWith("filter=")) {

			switch (cmd.getName()) {
				case "/scanentities" -> {
					completions.addAll(getEntityTypeCompletions(args));
				}
				case "/scantileentities" -> {
					completions.addAll(getTileEntityCompletions(args));
				}
				case "/scanall" -> {
					completions.addAll(getEntityTypeCompletions(args));
					completions.addAll(getTileEntityCompletions(args));
				}
			}
			String lastArg = args[args.length - 1];
			String untilComma = lastArg.contains(",") ? lastArg.substring(0, lastArg.lastIndexOf(",") + 1) : "filter=";
			completions.replaceAll(completion -> untilComma + completion);
		}

		return completions;
	}

	private List<String> getEntityTypeCompletions(@NotNull String[] args) {
		return RegistryAccess.registryAccess().getRegistry(RegistryKey.ENTITY_TYPE)
				.stream()
				.map(type -> type.name().replace("minecraft:", "").toLowerCase(Locale.ROOT))
				.toList();
	}

	private List<String> getTileEntityCompletions(@NotNull String[] args) {
		List<String> autoComplete = new ArrayList<>();
		Config.tileEntities.get.getStringList("tile_entities")
				.stream()
				.map(id -> id.replace("minecraft:", ""))
				.forEach(autoComplete::add);
		autoComplete.addFirst("container");
		return autoComplete;
	}
}
