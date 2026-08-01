package io.jingproject.marshall;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

public final class Marshalls {

//    private static final Map<Class<?>, MarshallFacade> CLASS_MARSHALL_FACADE_MAP;
//    private static final Map<Class<?>, MarshallFacade> RECORD_MARSHALL_FACADE_MAP;
//    private static final Map<Class<?>, MarshallFacade> ENUM_MARSHALL_FACADE_MAP;
//    private static final Map<Class<?>, MarshallFacade> MARSHALL_FACADE_MAP;
//
//    static {
//        List<MarshallFacade> facades = ServiceLoader.load(MarshallFacade.class).stream().map(ServiceLoader.Provider::get).toList();
//        Map<Class<?>, MarshallFacade> cf = new HashMap<>();
//        Map<Class<?>, MarshallFacade> rf = new HashMap<>();
//        Map<Class<?>, MarshallFacade> ef = new HashMap<>();
//        Map<Class<?>, MarshallFacade> f = new HashMap<>();
//        for (MarshallFacade facade : facades) {
//            Class<?> facadeType = facade.marshallableType();
//            if (f.put(facadeType, facade) != null) {
//                throw new IllegalStateException("duplicate marshallable : " + facadeType);
//            }
//            if(facadeType.isEnum()) {
//                ef.put(facadeType, facade);
//            } else if(facadeType.isRecord()) {
//                rf.put(facadeType, facade);
//            } else {
//                cf.put(facadeType, facade);
//            }
//        }
//        CLASS_MARSHALL_FACADE_MAP = Map.copyOf(cf);
//        RECORD_MARSHALL_FACADE_MAP = Map.copyOf(rf);
//        ENUM_MARSHALL_FACADE_MAP = Map.copyOf(ef);
//        MARSHALL_FACADE_MAP = Map.copyOf(f);
//    }

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
//
//    public static MarshallFacade getClassMarshallFacade(Class<?> type) {
//        return CLASS_MARSHALL_FACADE_MAP.get(type);
//    }
//
//    public static MarshallFacade getRecordMarshallFacade(Class<?> type) {
//        return RECORD_MARSHALL_FACADE_MAP.get(type);
//    }
//
//    public static MarshallFacade getEnumMarshallFacade(Class<?> type) {
//        return ENUM_MARSHALL_FACADE_MAP.get(type);
//    }

    public static MarshallTransformerFacade getMarshallTransformerFacade(Class<?> type) {
        return MARSHALL_TRANSFORMER_FACADE_MAP.get(type);
    }
}
