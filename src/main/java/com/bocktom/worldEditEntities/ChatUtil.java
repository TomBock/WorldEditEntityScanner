package com.bocktom.worldEditEntities;

import org.bukkit.entity.Player;

import java.util.function.Function;

public class ChatUtil {

	public static <T> void sendTable(Player player,
									 String title,
									 CountedMap<T> map,
									 Function<T, String> keyFormatter) {

		int maxCountLength = map.values().stream()
				.map(String::valueOf)
				.mapToInt(String::length)
				.max()
				.orElse(0);

		// e.g. "%3d" for nice right-aligned numbers
		String format = "§e%" + maxCountLength + "d §6%s";

		player.sendMessage("§7---- §e" + map.total + " §6" + title + " §7----");

		map.forEach((key, count) -> {
			String label = keyFormatter.apply(key); // e.g. "ZOMBIE", "Armor Stand"
			player.sendMessage(String.format(format, count, label));
		});
	}

}
