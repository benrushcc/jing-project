package io.jingproject.marshall;

import java.util.*;

public final class Marshalls {
    private static final Map<Class<?>, MarshallFacade> MARSHALL_FACADE_MAP;
    private static final Map<Enum<?>, MarshallInfo> ENUM_MARSHALL_INFO_MAP;
    private static final Map<Class<?>, MarshallTransformerFacade> MARSHALL_TRANSFORMER_FACADE_MAP;

    static {
        List<MarshallFacade> facades = ServiceLoader.load(MarshallFacade.class).stream().map(ServiceLoader.Provider::get).toList();
        Map<Class<?>, MarshallFacade> fr = new HashMap<>();
        Map<Enum<?>, MarshallInfo> er = new HashMap<>();
        for (MarshallFacade facade : facades) {
            Class<?> type = facade.marshallableType();
            if (fr.put(type, facade) != null) {
                throw new ExceptionInInitializerError("duplicate marshallable : " + type);
            }
            if(type.isEnum()) {
                Enum<?>[] enumConstants = (Enum<?>[]) type.getEnumConstants();
                for(int i = 0; i < enumConstants.length; i++) {
                    if (er.put(enumConstants[i], Objects.requireNonNull(facade.marshallInfoByIndex(i), "marshallInfo not found : " + type)) != null) {
                        throw new ExceptionInInitializerError("duplicate enum items : " + type);
                    }
                }
            }
        }
        MARSHALL_FACADE_MAP = Map.copyOf(fr);
        ENUM_MARSHALL_INFO_MAP = Map.copyOf(er);
        Map<Class<?>, MarshallTransformerFacade> tr = new HashMap<>();
        for (MarshallTransformerFacade facade : ServiceLoader.load(MarshallTransformerFacade.class).stream().map(ServiceLoader.Provider::get).toList()) {
            if(tr.put(facade.getClass(), facade) != null) {
                throw new ExceptionInInitializerError("duplicate transformer : " + facade.getClass());
            }
        }
        MARSHALL_TRANSFORMER_FACADE_MAP = Map.copyOf(tr);
    }

    private Marshalls() {
        throw new UnsupportedOperationException("utility class");
    }

    public static MarshallFacade getMarshallFacade(Class<?> type) {
        return MARSHALL_FACADE_MAP.get(type);
    }

    public static MarshallInfo getEnumItemMarshallInfo(Enum<?> value) {
        return ENUM_MARSHALL_INFO_MAP.get(value);
    }

    public static MarshallTransformerFacade getMarshallTransformerFacade(Class<?> type) {
        return MARSHALL_TRANSFORMER_FACADE_MAP.get(type);
    }
}
