package io.jingproject.common.conf;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class PropertiesCfgReader {
    private final InputStream input;

    public PropertiesCfgReader(InputStream input) {
        this.input = input;
    }

    public CfgObject parse() throws IOException {
        CfgObject r = new CfgObject(new HashMap<>());
        CfgObject current = r;
        Properties prop = new Properties();
        try(Reader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            prop.load(reader);
            for (String key : prop.stringPropertyNames()) {
                if(key.isBlank()) {
                    continue ;
                }
                String value = prop.getProperty(key);
                if(value.isBlank()) {
                    continue ;
                }
                List<String> nestedKeys = CfgUtil.readCfgNestedKey(key.getBytes(StandardCharsets.UTF_8));
                switch (nestedKeys.size()) {
                    case 0 -> throw new AssertionError();
                    case 1 -> {
                        if(current.value().putIfAbsent(nestedKeys.getLast(), buildCfgObject(value)) != null) {
                            throw new CfgException("Duplicate key: " + key);
                        }
                    }
                    default -> {
                        for (String nestedKey : nestedKeys.subList(0, nestedKeys.size() - 1)) {
                            Map<String, Cfg> currentMap = current.value();
                            Cfg currentObj = currentMap.get(nestedKey);
                            if(currentObj == null) {
                                CfgObject cm = new CfgObject(new HashMap<>());
                                currentMap.put(nestedKey, cm);
                                current = cm;
                            } else if(currentObj instanceof CfgObject cm) {
                                current = cm;
                            } else {
                                throw new CfgException("Duplicate key: " + key);
                            }
                        }
                        if(current.value().putIfAbsent(nestedKeys.getLast(), buildCfgObject(value)) != null) {
                            throw new CfgException("Duplicate key: " + key);
                        }
                        current = r;
                    }
                }
            }
            return r;
        }
    }

    private static Cfg buildCfgObject(String value) {
        if(value.startsWith("[") && value.endsWith("]")) {
            String arrStr = value.substring(1, Math.subtractExact(value.length(), 1));
            List<String> arrItems = new ArrayList<>();
            for (String item : arrStr.split(",")) {
                if(!item.isBlank()) {
                    arrItems.add(item);
                }
            }
            return new CfgList(arrItems);
        }
        return new CfgItem(value);
    }
}
