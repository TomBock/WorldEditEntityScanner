package com.bocktom.worldeditentityscanner.util;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CountedMap <T> extends LinkedHashMap<T, Integer> {
	public int total;
	public long lookupTimeMs;
	public Function<T, String> keyFormatter;

	public CountedMap() {
	}

	public CountedMap(Function<T, String> keyFormatter) {
		this.keyFormatter = keyFormatter;
	}


	@Override
	public Integer put(T key, Integer value) {
		total += value;
		return super.put(key, super.merge(key, value, Integer::sum));
	}

	public Integer increment(T key) {
		total += 1;
		return super.put(key, super.merge(key, 1, Integer::sum));
	}

	public static <T> CountedMap<T> empty() {
		CountedMap<T> counted = new CountedMap<>();
		counted.total = 0;
		return counted;
	}


	public CountedMap<T> sortedByValueDescending() {
		LinkedHashMap<T, Integer> sorted = this.entrySet()
				.stream()
				.sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
				.collect(
						Collectors.toMap(
								Map.Entry::getKey,
								Map.Entry::getValue,
								(oldValue, newValue) -> oldValue,
								LinkedHashMap::new
						)
				);
		this.clear();
		this.putAll(sorted);
		return this;
	}
}

