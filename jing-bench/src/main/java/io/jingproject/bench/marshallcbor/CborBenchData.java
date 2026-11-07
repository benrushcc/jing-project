package io.jingproject.bench.marshallcbor;

import java.util.HashMap;
import java.util.Map;

final class CborBenchData {
    private CborBenchData() {
        throw new UnsupportedOperationException("utility class");
    }

    static Map<String, Integer> integerMap(int size) {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < size; i++) {
            map.put(i + "", i);
        }
        return Map.copyOf(map);
    }
}
