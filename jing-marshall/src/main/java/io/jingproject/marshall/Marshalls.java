package io.jingproject.marshall;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

public final class Marshalls {

    private static final Map<Class<?>, MarshallFacade> MARSHALL_FACADE_MAP = createMarshallFacadeMap();
    private static final Map<Class<?>, MarshallTransformerFacade> MARSHALL_TRANSFORMER_FACADE_MAP = createMarshallTransformerFacadeMap();

    private static Map<Class<?>, MarshallFacade> createMarshallFacadeMap() {
        List<MarshallFacade> facades = ServiceLoader.load(MarshallFacade.class).stream().map(ServiceLoader.Provider::get).toList();
        Map<Class<?>, MarshallFacade> r = new HashMap<>();
        for (MarshallFacade facade : facades) {
            if (r.put(facade.marshallableType(), facade) != null) {
                throw new IllegalStateException("duplicate marshallable : " + facade.marshallableType());
            }
        }
        return Map.copyOf(r);
    }

    private static Map<Class<?>, MarshallTransformerFacade> createMarshallTransformerFacadeMap() {
        List<MarshallTransformerFacade> facades = ServiceLoader.load(MarshallTransformerFacade.class).stream().map(ServiceLoader.Provider::get).toList();
        Map<Class<?>, MarshallTransformerFacade> r = new HashMap<>();
        for (MarshallTransformerFacade facade : facades) {
            if(r.put(facade.getClass(), facade) != null) {
                throw new IllegalStateException("duplicate transformer : " + facade.getClass());
            }
        }
        return Map.copyOf(r);
    }

    private Marshalls() {
        throw new UnsupportedOperationException("utility class");
    }

    public static MarshallFacade getMarshallFacade(Class<?> type) {
        return MARSHALL_FACADE_MAP.get(type);
    }

    public static MarshallTransformerFacade getMarshallTransformerFacade(Class<?> type) {
        return MARSHALL_TRANSFORMER_FACADE_MAP.get(type);
    }
}
