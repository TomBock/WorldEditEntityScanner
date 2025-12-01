package com.bocktom.worldEditEntities.util;

import com.sk89q.worldedit.world.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Mob;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.Predicate;

public class FilterUtil {

	public static @NotNull Predicate<EntityType> getEntityFilter(String filterRaw) {
		return switch (filterRaw.toLowerCase(Locale.ROOT)) {
			case "mobs" -> type -> type instanceof Mob;
			case "items" -> type -> type instanceof Item;
			default -> type -> type.getName().replace("minecraft:", "").toLowerCase(Locale.ROOT).equals(filterRaw);
		};
	}

	public static @NotNull Predicate<String> getBlockFilter(String filterRaw) {
		// Check for custom list
		List<String> customList = Config.tileEntities.get.getStringList(filterRaw).stream()
				.map(raw -> {
					if(raw.contains(":")) {
						return raw;
					} else {
						return "minecraft:" + raw;
					}
				}).toList();

		if(!customList.isEmpty()) {
			return customList::contains;
		}

		if(filterRaw.contains(":")) {
			// Check for namespaced ID
			return id -> id.equals(filterRaw);
		} else {
			// Check for raw ID
			return id -> id.replace("minecraft:", "").equals(filterRaw);
		}
	}

	public static <T> Predicate<T> getFilter(String[] filters, Function<String, Predicate<T>> getFilterFunction) {
		Predicate<T> filterFull = t -> false;

		if(filters.length == 0) {
			filterFull = t -> true;
			return filterFull;
		}

		for (String filterRaw : filters) {
			if(filterRaw.isBlank()) {
				continue;
			}

			Predicate<T> filter;
			boolean reversed = false;
			if(filterRaw.startsWith("!")) {
				reversed = true;
				filterRaw = filterRaw.substring(1);
			}
			filter = getFilterFunction.apply(filterRaw);
			filter = reversed ? filter.negate() : filter;
			filterFull = filterFull.or(filter);
		}

		return filterFull;
	}
}
