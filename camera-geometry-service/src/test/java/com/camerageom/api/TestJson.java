package com.camerageom.api;

import java.util.LinkedHashMap;
import java.util.Map;

/** Small helper to build JSON-able request maps in tests. */
final class TestJson {

    private TestJson() {
    }

    static Map<String, Object> obj(Object... keyValues) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put((String) keyValues[i], keyValues[i + 1]);
        }
        return map;
    }
}
