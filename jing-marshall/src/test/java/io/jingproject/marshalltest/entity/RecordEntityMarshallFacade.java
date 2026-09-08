package io.jingproject.marshalltest.entity;

import io.jingproject.marshall.*;

import java.lang.foreign.MemorySegment;
import java.time.LocalDateTime;
import java.util.List;

public final class RecordEntityMarshallFacade implements MarshallFacade {
    private static final List<MarshallInfo> MARSHALL_INFOS;
    private static final MarshallHashInfo FACADE_INFO;

    static {
        MarshallInfo mi0 = new MarshallInfo(int.class, null, null, 0, "intValue", "intValue", false, false);
        MarshallInfo mi1 = new MarshallInfo(long.class, null, null, 1, "longValue", "longValue", false, false);
        MarshallInfo mi2 = new MarshallInfo(String.class, null, null, 2, "strValue", "strValue", false, false);
        MarshallInfo mi3 = new MarshallInfo(LocalDateTime.class, null, null, 3, "timeValue", "timeValue", false, false);
        MARSHALL_INFOS = List.of(mi0, mi1, mi2, mi3);
        FACADE_INFO = new MarshallHashInfo(MARSHALL_INFOS);
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
    public int primitiveElements() {
        return 2;
    }

    @Override
    public List<MarshallInfo> marshallInfos() {
        return MARSHALL_INFOS;
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
        return MARSHALL_INFOS.get(index);
    }

    @Override
    public MarshallInfo marshallInfoByFieldName(byte[] bytes, int from, int to) {
        int hash = FACADE_INFO.fieldNameUtf8Hasher().hash(bytes, from, to);
        switch (hash) {
            case 105 -> {
                if (FACADE_INFO.fieldNameEquals(0, 8, bytes, from, to)) {
                    return MARSHALL_INFOS.get(0);
                }
            }
            case 108 -> {
                if (FACADE_INFO.fieldNameEquals(8, 17, bytes, from, to)) {
                    return MARSHALL_INFOS.get(1);
                }
            }
            case 115 -> {
                if (FACADE_INFO.fieldNameEquals(17, 25, bytes, from, to)) {
                    return MARSHALL_INFOS.get(2);
                }
            }
            case 116 -> {
                if (FACADE_INFO.fieldNameEquals(25, 34, bytes, from, to)) {
                    return MARSHALL_INFOS.get(3);
                }
            }
        }
        throw new IllegalArgumentException("marshallInfo not found by fieldName");
    }

    @Override
    public MarshallInfo marshallInfoByFieldName(MemorySegment segment, long from, long to) {
        int hash = FACADE_INFO.fieldNameUtf8Hasher().hash(segment, from, to);
        switch (hash) {
            case 105 -> {
                if (FACADE_INFO.fieldNameEquals(0, 8, segment, from, to)) {
                    return MARSHALL_INFOS.get(0);
                }
            }
            case 108 -> {
                if (FACADE_INFO.fieldNameEquals(8, 17, segment, from, to)) {
                    return MARSHALL_INFOS.get(1);
                }
            }
            case 115 -> {
                if (FACADE_INFO.fieldNameEquals(17, 25, segment, from, to)) {
                    return MARSHALL_INFOS.get(2);
                }
            }
            case 116 -> {
                if (FACADE_INFO.fieldNameEquals(25, 34, segment, from, to)) {
                    return MARSHALL_INFOS.get(3);
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
        return MARSHALL_INFOS.get(index);
    }

    @Override
    public MarshallInfo marshallInfoByMappedName(byte[] bytes, int from, int to) {
        int hash = FACADE_INFO.mappedNameUtf8Hasher().hash(bytes, from, to);
        switch (hash) {
            case 105 -> {
                if (FACADE_INFO.mappedNameEquals(0, 8, bytes, from, to)) {
                    return MARSHALL_INFOS.get(0);
                }
            }
            case 108 -> {
                if (FACADE_INFO.mappedNameEquals(8, 17, bytes, from, to)) {
                    return MARSHALL_INFOS.get(1);
                }
            }
            case 115 -> {
                if (FACADE_INFO.mappedNameEquals(17, 25, bytes, from, to)) {
                    return MARSHALL_INFOS.get(2);
                }
            }
            case 116 -> {
                if (FACADE_INFO.mappedNameEquals(25, 34, bytes, from, to)) {
                    return MARSHALL_INFOS.get(3);
                }
            }
        }
        throw new IllegalArgumentException("marshallInfo not found by mappedName");
    }

    @Override
    public MarshallInfo marshallInfoByMappedName(MemorySegment segment, long from, long to) {
        int hash = FACADE_INFO.mappedNameUtf8Hasher().hash(segment, from, to);
        switch (hash) {
            case 105 -> {
                if (FACADE_INFO.mappedNameEquals(0, 8, segment, from, to)) {
                    return MARSHALL_INFOS.get(0);
                }
            }
            case 108 -> {
                if (FACADE_INFO.mappedNameEquals(8, 17, segment, from, to)) {
                    return MARSHALL_INFOS.get(1);
                }
            }
            case 115 -> {
                if (FACADE_INFO.mappedNameEquals(17, 25, segment, from, to)) {
                    return MARSHALL_INFOS.get(2);
                }
            }
            case 116 -> {
                if (FACADE_INFO.mappedNameEquals(25, 34, segment, from, to)) {
                    return MARSHALL_INFOS.get(3);
                }
            }
        }
        throw new IllegalArgumentException("marshallInfo not found by mappedName");
    }

    @Override
    public int readInt(Object instance, int offset) {
        RecordEntity entity = (RecordEntity) instance;
        return switch (offset) {
            case 0 -> entity.intValue();
            default -> throw new UnsupportedOperationException();
        };
    }

    @Override
    public long readLong(Object instance, int offset) {
        RecordEntity entity = (RecordEntity) instance;
        return switch (offset) {
            case 0 -> entity.longValue();
            default -> throw new UnsupportedOperationException();
        };
    }

    @Override
    public Object readObject(Object instance, int offset) {
        RecordEntity entity = (RecordEntity) instance;
        return switch (offset) {
            case 2 -> entity.strValue();
            case 3 -> entity.timeValue();
            default -> throw new UnsupportedOperationException();
        };
    }

    @Override
    public MarshallBuilder newBuilder() {
        return new Builder();
    }

    @Override
    public Object construct(MarshallBuilder writer) {
        if (writer instanceof Builder builder) {
            return builder.build();
        }
        throw new IllegalArgumentException("wrong writer : " + writer.getClass().getName());
    }

    private static final class Builder implements MarshallBuilder {
        private int intValue;
        private long longValue;
        private String strValue;
        private LocalDateTime timeValue;

        @Override
        public void writeInt(int index, int value) {
            switch (index) {
                case 0 -> this.intValue = value;
                default -> throw new UnsupportedOperationException();
            }
        }

        @Override
        public void writeLong(int index, long value) {
            switch (index) {
                case 1 -> this.longValue = value;
                default -> throw new UnsupportedOperationException();
            }
        }

        @Override
        public void writeObject(int index, Object value) {
            switch (index) {
                case 2 -> this.strValue = (String) value;
                case 3 -> this.timeValue = (LocalDateTime) value;
                default -> throw new UnsupportedOperationException();
            }
        }

        RecordEntity build() {
            return new RecordEntity(intValue, longValue, strValue, timeValue);
        }
    }
}
