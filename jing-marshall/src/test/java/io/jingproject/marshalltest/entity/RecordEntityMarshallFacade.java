package io.jingproject.marshalltest.entity;

import io.jingproject.marshall.*;

import java.lang.foreign.MemorySegment;
import java.time.LocalDateTime;
import java.util.List;

public final class RecordEntityMarshallFacade implements MarshallFacade {
    private static final MarshallFacadeInfo FACADE_INFO;

    static {
        MarshallInfo mi0 = new MarshallInfo(int.class, null, null, 0, "intValue", "intValue", null, false, false);
        MarshallInfo mi1 = new MarshallInfo(long.class, null, null, 1, "longValue", "longValue", null, false, false);
        MarshallInfo mi2 = new MarshallInfo(String.class, null, null, 2, "strValue", "strValue", null, false, false);
        MarshallInfo mi3 = new MarshallInfo(LocalDateTime.class, null, null, 3, "timeValue", "timeValue", null, false, false);
        FACADE_INFO = new MarshallFacadeInfo(List.of(mi0, mi1, mi2, mi3));
    }

    @Override
    public Class<?> marshallableType() {
        return RecordEntity.class;
    }

    @Override
    public int totalElements() {
        return 4;
    }

    @Override
    public MarshallInfo marshallInfoByIndex(int index) {
        return FACADE_INFO.infos().get(index);
    }

    @Override
    public MarshallInfo marshallInfoByFieldName(String fieldName) {
        int index = switch (fieldName) {
            case "intValue" -> 0;
            case "longValue" -> 1;
            case "strValue" -> 2;
            case "timeValue" -> 3;
            default -> throw new IllegalArgumentException("fieldName not found: " + fieldName);
        };
        return FACADE_INFO.infos().get(index);
    }

    @Override
    public MarshallInfo marshallInfoByFieldName(byte[] bytes, int offset, int len) {
        int hash = FACADE_INFO.fieldNameUtf8Hasher().hash(bytes, offset, len);
        switch (hash) {
            case 105 -> {
                if (FACADE_INFO.fieldNameEquals(0, 8, bytes, offset, len)) {
                    return FACADE_INFO.infos().get(0);
                }
            }
            case 108 -> {
                if (FACADE_INFO.fieldNameEquals(8, 9, bytes, offset, len)) {
                    return FACADE_INFO.infos().get(1);
                }
            }
            case 115 -> {
                if (FACADE_INFO.fieldNameEquals(17, 8, bytes, offset, len)) {
                    return FACADE_INFO.infos().get(2);
                }
            }
            case 116 -> {
                if (FACADE_INFO.fieldNameEquals(25, 9, bytes, offset, len)) {
                    return FACADE_INFO.infos().get(3);
                }
            }
        }
        throw new IllegalArgumentException("marshallInfo not found by fieldName");
    }

    @Override
    public MarshallInfo marshallInfoByFieldName(MemorySegment segment, long offset, long len) {
        int hash = FACADE_INFO.fieldNameUtf8Hasher().hash(segment, offset, len);
        switch (hash) {
            case 105 -> {
                if (FACADE_INFO.fieldNameEquals(0, 8, segment, offset, len)) {
                    return FACADE_INFO.infos().get(0);
                }
            }
            case 108 -> {
                if (FACADE_INFO.fieldNameEquals(8, 9, segment, offset, len)) {
                    return FACADE_INFO.infos().get(1);
                }
            }
            case 115 -> {
                if (FACADE_INFO.fieldNameEquals(17, 8, segment, offset, len)) {
                    return FACADE_INFO.infos().get(2);
                }
            }
            case 116 -> {
                if (FACADE_INFO.fieldNameEquals(25, 9, segment, offset, len)) {
                    return FACADE_INFO.infos().get(3);
                }
            }
        }
        throw new IllegalArgumentException("marshallInfo not found by fieldName");
    }

    @Override
    public MarshallInfo marshallInfoByMappedName(String mappedName) {
        int index = switch (mappedName) {
            case "intValue" -> 0;
            case "longValue" -> 1;
            case "strValue" -> 2;
            case "timeValue" -> 3;
            default -> throw new IllegalArgumentException("mappedName not found: " + mappedName);
        };
        return FACADE_INFO.infos().get(index);
    }

    @Override
    public MarshallInfo marshallInfoByMappedName(byte[] bytes, int offset, int len) {
        int hash = FACADE_INFO.mappedNameUtf8Hasher().hash(bytes, offset, len);
        switch (hash) {
            case 105 -> {
                if (FACADE_INFO.mappedNameEquals(0, 8, bytes, offset, len)) {
                    return FACADE_INFO.infos().get(0);
                }
            }
            case 108 -> {
                if (FACADE_INFO.mappedNameEquals(8, 9, bytes, offset, len)) {
                    return FACADE_INFO.infos().get(1);
                }
            }
            case 115 -> {
                if (FACADE_INFO.mappedNameEquals(17, 8, bytes, offset, len)) {
                    return FACADE_INFO.infos().get(2);
                }
            }
            case 116 -> {
                if (FACADE_INFO.mappedNameEquals(25, 9, bytes, offset, len)) {
                    return FACADE_INFO.infos().get(3);
                }
            }
        }
        throw new IllegalArgumentException("marshallInfo not found by mappedName");
    }

    @Override
    public MarshallInfo marshallInfoByMappedName(MemorySegment segment, long offset, long len) {
        int hash = FACADE_INFO.mappedNameUtf8Hasher().hash(segment, offset, len);
        switch (hash) {
            case 105 -> {
                if (FACADE_INFO.mappedNameEquals(0, 8, segment, offset, len)) {
                    return FACADE_INFO.infos().get(0);
                }
            }
            case 108 -> {
                if (FACADE_INFO.mappedNameEquals(8, 9, segment, offset, len)) {
                    return FACADE_INFO.infos().get(1);
                }
            }
            case 115 -> {
                if (FACADE_INFO.mappedNameEquals(25, 8, segment, offset, len)) {
                    return FACADE_INFO.infos().get(2);
                }
            }
            case 116 -> {
                if (FACADE_INFO.mappedNameEquals(33, 9, segment, offset, len)) {
                    return FACADE_INFO.infos().get(3);
                }
            }
        }
        throw new IllegalArgumentException("marshallInfo not found by mappedName");
    }

    @Override
    public MarshallReader newReader(Object target) {
        if(target instanceof RecordEntity instance) {
            return new RecordEntityMarshallReader(instance);
        }
        throw new IllegalArgumentException("wrong target : " + target.getClass().getName());
    }

    @Override
    public MarshallWriter newWriter() {
        return new RecordEntityMarshallWriter();
    }

    @Override
    public Object construct(MarshallWriter writer) {
        if(writer instanceof RecordEntityMarshallWriter instance) {
            return instance.build();
        }
        throw new IllegalArgumentException("wrong writer : " + writer.getClass().getName());
    }
}
