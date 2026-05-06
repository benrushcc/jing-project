package io.jingproject.marshall;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

public final class Marshalls {

    private static final Map<Class<?>, MarshallFacade> FACADE_MAP = createFacadeMap();

    private static Map<Class<?>, MarshallFacade> createFacadeMap() {
        List<MarshallFacade> facades = ServiceLoader.load(MarshallFacade.class).stream().map(ServiceLoader.Provider::get).toList();
        Map<Class<?>, MarshallFacade> r = new HashMap<>();
        for (MarshallFacade facade : facades) {
            if (r.put(facade.marshallableType(), facade) != null) {
                throw new IllegalStateException("duplicate marshallable type: " + facade.marshallableType());
            }
        }
        return Map.copyOf(r);
    }

    private Marshalls() {
        throw new UnsupportedOperationException("utility class");
    }

    public static MarshallFacade getMarshallFacade(Class<?> type) {
        return FACADE_MAP.get(type);
    }
}
