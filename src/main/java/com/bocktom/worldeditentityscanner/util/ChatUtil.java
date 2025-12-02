package com.bocktom.worldeditentityscanner.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.entity.Player;

import java.util.LinkedHashMap;

public class ChatUtil {

	private static final int TABLE_PAGE_HEIGHT = 8;

	public static <T> void sendTablePaged(Player player,
										   CountedMap<T> map,
										   int page,
										   String title) {
		// Get correct page
		LinkedHashMap<T, Integer> pagedMap = new LinkedHashMap<>();
		int maxPages = (int) Math.ceil((double) map.size() / TABLE_PAGE_HEIGHT);
		int startIndex = (page - 1) * TABLE_PAGE_HEIGHT;
		int endIndex = startIndex + TABLE_PAGE_HEIGHT;
		int currentIndex = 0;
		for (var entry : map.entrySet()) {
			if (currentIndex >= startIndex && currentIndex < endIndex) {
				pagedMap.put(entry.getKey(), entry.getValue());
			}
			currentIndex++;
		}

		int maxCountLength = map.values().stream()
				.map(String::valueOf)
				.mapToInt(String::length)
				.max()
				.orElse(0);

		// e.g. "%3d" for nice right-aligned numbers
		String format = "§e%" + maxCountLength + "d §6%s";

		player.sendMessage("§7---- §e" + map.total + " §6" + title + " §7[" + map.lookupTimeMs + "ms] §7----");

		pagedMap.forEach((key, count) -> {
			String label = map.keyFormatter.apply(key);
			player.sendMessage(String.format(format, count, label));
		});

		// send Footer with buttons for navigation
		Component footer = Component.text("§7");
		if (page > 1) {
			footer = footer.append(Component.text("§a« Prev ").clickEvent(ClickEvent.callback(
					callback -> ChatUtil.sendTablePaged(player, map, page - 1, title))));
		} else {
			footer = footer.append(Component.text("§8« Prev "));
		}
		footer = footer.append(Component.text("§7| §7Page §7")).append(Component.text("§e" + page)).append(Component.text("§7/§e")).append(Component.text("§e" + maxPages)).append(Component.text(" §7| "));
		if (page < maxPages) {
			footer = footer.append(Component.text("§aNext »").clickEvent(ClickEvent.callback(
					audience -> ChatUtil.sendTablePaged(player, map, page + 1, title))));
		} else {
			footer = footer.append(Component.text("§8Next »"));
		}
		player.sendMessage(footer);
	}

	public static void sendProgress(Player player, String title, double progress, int count, int total) {
		player.sendActionBar(Component.text(String.format("§7%s: §e%.2f%% §7(§e%d§7/§e%d§7)", title, progress, count, total)));
	}
}
