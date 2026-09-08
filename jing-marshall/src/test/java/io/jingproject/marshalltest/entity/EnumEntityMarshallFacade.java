package io.jingproject.marshalltest.entity;

import io.jingproject.marshall.MarshallFacade;
import io.jingproject.marshall.MarshallHashInfo;
import io.jingproject.marshall.MarshallInfo;

import java.lang.foreign.MemorySegment;
import java.util.List;

public final class EnumEntityMarshallFacade implements MarshallFacade {
    private static final List<MarshallInfo> MARSHALL_INFOS;
    private static final MarshallHashInfo HASH_INFO;

    static {
        MarshallInfo mi0 = new MarshallInfo(EnumEntity.class, null, null, 0, "INT", "INT", false, false);
        MarshallInfo mi1 = new MarshallInfo(EnumEntity.class, null, null, 1, "LONG", "LONG", false, false);
        MarshallInfo mi2 = new MarshallInfo(EnumEntity.class, null, null, 2, "STR", "STR", false, false);
        MarshallInfo mi3 = new MarshallInfo(EnumEntity.class, null, null, 3, "TIME", "TIME", false, false);
        MARSHALL_INFOS = List.of(mi0, mi1, mi2, mi3);
        HASH_INFO = new MarshallHashInfo(MARSHALL_INFOS);
    }

    @Override
    public Class<?> marshallableType() {
        return EnumEntity.class;
    }

    @Override
    public int totalElements() {
        return 4;
    }

    @Override
    public List<MarshallInfo> marshallInfos() {
        return MARSHALL_INFOS;
    }

    @Override
    public MarshallInfo marshallInfoByFieldName(String fieldName) {
        return switch (fieldName) {
            case "INT" -> MARSHALL_INFOS.get(0);
            case "LONG" -> MARSHALL_INFOS.get(1);
            case "STR" -> MARSHALL_INFOS.get(2);
            case "TIME" -> MARSHALL_INFOS.get(3);
            default -> null;
        };
    }

    @Override
    public MarshallInfo marshallInfoByFieldName(byte[] bytes, int from, int to) {
        int hash = HASH_INFO.fieldNameUtf8Hasher().hash(bytes, from, to);
        switch (hash) {
            case 73 -> {
                if (HASH_INFO.fieldNameEquals(0, 3, bytes, from, to)) {
                    return MARSHALL_INFOS.get(0);
                }
            }
            case 76 -> {
                if (HASH_INFO.fieldNameEquals(3, 7, bytes, from, to)) {
                    return MARSHALL_INFOS.get(1);
                }
            }
            case 83 -> {
                if (HASH_INFO.fieldNameEquals(7, 10, bytes, from, to)) {
                    return MARSHALL_INFOS.get(2);
                }
            }
            case 84 -> {
                if (HASH_INFO.fieldNameEquals(10, 14, bytes, from, to)) {
                    return MARSHALL_INFOS.get(3);
                }
            }
        }
        return null;
    }

    @Override
    public MarshallInfo marshallInfoByFieldName(MemorySegment segment, long from, long to) {
        int hash = HASH_INFO.fieldNameUtf8Hasher().hash(segment, from, to);
        switch (hash) {
            case 73 -> {
                if (HASH_INFO.fieldNameEquals(0, 3, segment, from, to)) {
                    return MARSHALL_INFOS.get(0);
                }
            }
            case 76 -> {
                if (HASH_INFO.fieldNameEquals(3, 7, segment, from, to)) {
                    return MARSHALL_INFOS.get(1);
                }
            }
            case 83 -> {
                if (HASH_INFO.fieldNameEquals(7, 10, segment, from, to)) {
                    return MARSHALL_INFOS.get(2);
                }
            }
            case 84 -> {
                if (HASH_INFO.fieldNameEquals(10, 14, segment, from, to)) {
                    return MARSHALL_INFOS.get(3);
                }
            }
        }
        return null;
    }

    @Override
    public MarshallInfo marshallInfoByMappedName(String mappedName) {
        return switch (mappedName) {
            case "INT" -> MARSHALL_INFOS.get(0);
            case "LONG" -> MARSHALL_INFOS.get(1);
            case "STR" -> MARSHALL_INFOS.get(2);
            case "TIME" -> MARSHALL_INFOS.get(3);
            default -> null;
        };
    }

    @Override
    public MarshallInfo marshallInfoByMappedName(byte[] bytes, int from, int to) {
        int hash = HASH_INFO.mappedNameUtf8Hasher().hash(bytes, from, to);
        switch (hash) {
            case 73 -> {
                if (HASH_INFO.mappedNameEquals(0, 3, bytes, from, to)) {
                    return MARSHALL_INFOS.get(0);
                }
            }
            case 76 -> {
                if (HASH_INFO.mappedNameEquals(3, 7, bytes, from, to)) {
                    return MARSHALL_INFOS.get(1);
                }
            }
            case 83 -> {
                if (HASH_INFO.mappedNameEquals(7, 10, bytes, from, to)) {
                    return MARSHALL_INFOS.get(2);
                }
            }
            case 84 -> {
                if (HASH_INFO.mappedNameEquals(10, 14, bytes, from, to)) {
                    return MARSHALL_INFOS.get(3);
                }
            }
        }
        return null;
    }

    @Override
    public MarshallInfo marshallInfoByMappedName(MemorySegment segment, long from, long to) {
        int hash = HASH_INFO.mappedNameUtf8Hasher().hash(segment, from, to);
        switch (hash) {
            case 73 -> {
                if (HASH_INFO.mappedNameEquals(0, 3, segment, from, to)) {
                    return MARSHALL_INFOS.get(0);
                }
            }
            case 76 -> {
                if (HASH_INFO.mappedNameEquals(3, 7, segment, from, to)) {
                    return MARSHALL_INFOS.get(1);
                }
            }
            case 83 -> {
                if (HASH_INFO.mappedNameEquals(7, 10, segment, from, to)) {
                    return MARSHALL_INFOS.get(2);
                }
            }
            case 84 -> {
                if (HASH_INFO.mappedNameEquals(10, 14, segment, from, to)) {
                    return MARSHALL_INFOS.get(3);
                }
            }
        }
        return null;
    }
}
