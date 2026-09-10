package io.jingproject.marshall;

import java.util.*;

public final class Marshalls {
    private static final Map<Class<?>, MarshallFacade> BEAN_MARSHALL_FACADE_MAP;
    private static final Map<Class<?>, MarshallFacade> ENUM_MARSHALL_FACADE_MAP;
    private static final Map<Enum<?>, MarshallInfo> ENUM_MARSHALL_INFO_MAP;
    private static final Map<Class<?>, MarshallTransformerFacade> MARSHALL_TRANSFORMER_FACADE_MAP;

    static {
        List<MarshallFacade> facades = ServiceLoader.load(MarshallFacade.class).stream().map(ServiceLoader.Provider::get).toList();
        Map<Class<?>, MarshallFacade> beanMarshallFacadeMap = new HashMap<>();
        Map<Class<?>, MarshallFacade> enumMarshallFacadeMap = new HashMap<>();
        Map<Enum<?>, MarshallInfo> enumMarshallInfoMap = new HashMap<>();
        for (MarshallFacade fc : facades) {
            Class<?> type = fc.marshallableType();
            if(type.isEnum()) {
                List<MarshallInfo> marshallInfos = fc.marshallInfos();
                if (enumMarshallFacadeMap.put(type, fc) != null) {
                    throw new ExceptionInInitializerError("duplicate enum marshallable type : " + type);
                }
                Enum<?>[] enumConstants = (Enum<?>[]) type.getEnumConstants();
                for(int i = 0; i < enumConstants.length; i++) {
                    enumMarshallInfoMap.put(enumConstants[i], marshallInfos.get(i));
                }
            } else if(beanMarshallFacadeMap.put(type, fc) != null) {
                throw new ExceptionInInitializerError("duplicate bean marshallable type : " + type);
            }
        }
        BEAN_MARSHALL_FACADE_MAP = Map.copyOf(beanMarshallFacadeMap);
        ENUM_MARSHALL_FACADE_MAP = Map.copyOf(enumMarshallFacadeMap);
        ENUM_MARSHALL_INFO_MAP = Map.copyOf(enumMarshallInfoMap);

        Map<Class<?>, MarshallTransformerFacade> m4 = new HashMap<>();
        for (MarshallTransformerFacade facade : ServiceLoader.load(MarshallTransformerFacade.class).stream().map(ServiceLoader.Provider::get).toList()) {
            if(m4.put(facade.transformerType(), facade) != null) {
                throw new ExceptionInInitializerError("duplicate transformer type : " + facade.getClass());
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
