package io.jingproject.marshall;

import java.util.*;

public final class Marshalls {
    private static final Map<Class<?>, MarshallFacade> BEAN_MARSHALL_FACADE_MAP;
    private static final Map<Class<?>, MarshallFacade> ENUM_MARSHALL_FACADE_MAP;
    private static final Map<Enum<?>, MarshallInfo> ENUM_MARSHALL_INFO_MAP;
    private static final Map<Class<?>, MarshallTransformerFacade> MARSHALL_TRANSFORMER_FACADE_MAP;

    static {
        List<MarshallFacade> facades = ServiceLoader.load(MarshallFacade.class).stream().map(ServiceLoader.Provider::get).toList();
        Map<Class<?>, MarshallFacade> m1 = new HashMap<>();
        Map<Class<?>, MarshallFacade> m2 = new HashMap<>();
        Map<Enum<?>, MarshallInfo> m3 = new HashMap<>();
        for (MarshallFacade facade : facades) {
            Class<?> type = facade.marshallableType();
            if(type.isEnum()) {
                if (m2.put(type, facade) != null) {
                    throw new ExceptionInInitializerError("duplicate enum marshallable : " + type);
                }
                Enum<?>[] enumConstants = (Enum<?>[]) type.getEnumConstants();
                for(int i = 0; i < enumConstants.length; i++) {
                    if (m3.put(enumConstants[i], Objects.requireNonNull(facade.marshallInfoByIndex(i), "marshallInfo not found : " + type)) != null) {
                        throw new ExceptionInInitializerError("duplicate enum items : " + type);
                    }
                }
                continue ;
            }
            if (m1.put(type, facade) != null) {
                throw new ExceptionInInitializerError("duplicate bean marshallable : " + type);
            }
        }
        BEAN_MARSHALL_FACADE_MAP = Map.copyOf(m1);
        ENUM_MARSHALL_FACADE_MAP = Map.copyOf(m2);
        ENUM_MARSHALL_INFO_MAP = Map.copyOf(m3);
        Map<Class<?>, MarshallTransformerFacade> m4 = new HashMap<>();
        for (MarshallTransformerFacade facade : ServiceLoader.load(MarshallTransformerFacade.class).stream().map(ServiceLoader.Provider::get).toList()) {
            if(m4.put(facade.getClass(), facade) != null) {
                throw new ExceptionInInitializerError("duplicate transformer : " + facade.getClass());
            }
        }
        MARSHALL_TRANSFORMER_FACADE_MAP = Map.copyOf(m4);
    }

    private Marshalls() {
        throw new UnsupportedOperationException("utility class");
    }

    public static MarshallFacade beanMarshallFacade(Class<?> type) {
        return BEAN_MARSHALL_FACADE_MAP.get(type);
    }

    public static MarshallFacade enumMarshallFacade(Class<?> type) {
        return ENUM_MARSHALL_FACADE_MAP.get(type);
    }

    public static MarshallInfo enumItemMarshallInfo(Enum<?> value) {
        return ENUM_MARSHALL_INFO_MAP.get(value);
    }

    public static MarshallTransformerFacade marshallTransformerFacade(Class<?> type) {
        return MARSHALL_TRANSFORMER_FACADE_MAP.get(type);
    }
}
