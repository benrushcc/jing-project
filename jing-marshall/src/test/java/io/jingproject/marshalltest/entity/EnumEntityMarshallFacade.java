package io.jingproject.marshalltest.entity;

import io.jingproject.common.WriteBuffer;
import io.jingproject.marshall.*;

import java.lang.foreign.MemorySegment;
import java.util.List;

public final class EnumEntityMarshallFacade implements MarshallFacade {
    private static final MarshallFacadeInfo FACADE_INFO;

    static {
        MarshallInfo mi0 = new MarshallInfo(EnumEntity.class, null, null, 0, "INT", "INT", EnumEntity.INT, false, false);
        MarshallInfo mi1 = new MarshallInfo(EnumEntity.class, null, null, 1, "LONG", "LONG", EnumEntity.LONG, false, false);
        MarshallInfo mi2 = new MarshallInfo(EnumEntity.class, null, null, 2, "STR", "STR", EnumEntity.STR, false, false);
        MarshallInfo mi3 = new MarshallInfo(EnumEntity.class, null, null, 3, "TIME", "TIME", EnumEntity.TIME, false, false);
        FACADE_INFO = new MarshallFacadeInfo(List.of(mi0, mi1, mi2, mi3));
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
    public void writeFieldNameByIndex(WriteBuffer writeBuffer, int index) {
        switch (index) {
            case 0 -> writeBuffer.writeBytes(FACADE_INFO.fieldNameBytes(), 0, 3);
            case 1 -> writeBuffer.writeBytes(FACADE_INFO.fieldNameBytes(), 3, 4);
            case 2 -> writeBuffer.writeBytes(FACADE_INFO.fieldNameBytes(), 7, 3);
            case 3 -> writeBuffer.writeBytes(FACADE_INFO.fieldNameBytes(), 10, 4);
            default -> throw new IllegalArgumentException("wrong index");
        }
    }

    @Override
    public void writeMappedNameByIndex(WriteBuffer writeBuffer, int index) {
        switch (index) {
            case 0 -> writeBuffer.writeBytes(FACADE_INFO.mappedNameBytes(), 0, 3);
            case 1 -> writeBuffer.writeBytes(FACADE_INFO.mappedNameBytes(), 3, 4);
            case 2 -> writeBuffer.writeBytes(FACADE_INFO.mappedNameBytes(), 7, 3);
            case 3 -> writeBuffer.writeBytes(FACADE_INFO.mappedNameBytes(), 10, 4);
            default -> throw new IllegalArgumentException("wrong index");
        }
    }

    @Override
    public MarshallInfo marshallInfoByIndex(int index) {
        return FACADE_INFO.infos().get(index);
    }

    @Override
    public MarshallInfo marshallInfoByFieldName(String fieldName) {
        int index = switch (fieldName) {
            case "INT" -> 0;
            case "LONG" -> 1;
            case "STR" -> 2;
            case "TIME" -> 3;
            default -> throw new IllegalArgumentException("fieldName not found: " + fieldName);
        };
        return FACADE_INFO.infos().get(index);
    }

    @Override
    public MarshallInfo marshallInfoByFieldName(byte[] bytes, int offset, int len) {
        int hash = FACADE_INFO.fieldNameHasher().hash(bytes, offset, len);
        switch (hash) {
            case 73 -> {
                if (FACADE_INFO.fieldNameEquals(0, 3, bytes, offset, len)) {
                    return FACADE_INFO.infos().get(0);
                }
            }
            case 76 -> {
                if (FACADE_INFO.fieldNameEquals(3, 4, bytes, offset, len)) {
                    return FACADE_INFO.infos().get(1);
                }
            }
            case 83 -> {
                if (FACADE_INFO.fieldNameEquals(7, 3, bytes, offset, len)) {
                    return FACADE_INFO.infos().get(2);
                }
            }
            case 84 -> {
                if (FACADE_INFO.fieldNameEquals(10, 4, bytes, offset, len)) {
                    return FACADE_INFO.infos().get(3);
                }
            }
        }
        throw new IllegalArgumentException("marshallInfo not found by fieldName");
    }

    @Override
    public MarshallInfo marshallInfoByFieldName(MemorySegment segment, long offset, long len) {
        int hash = FACADE_INFO.fieldNameHasher().hash(segment, offset, len);
        switch (hash) {
            case 73 -> {
                if (FACADE_INFO.fieldNameEquals(0, 3, segment, offset, len)) {
                    return FACADE_INFO.infos().get(0);
                }
            }
            case 76 -> {
                if (FACADE_INFO.fieldNameEquals(3, 4, segment, offset, len)) {
                    return FACADE_INFO.infos().get(1);
                }
            }
            case 83 -> {
                if (FACADE_INFO.fieldNameEquals(7, 3, segment, offset, len)) {
                    return FACADE_INFO.infos().get(2);
                }
            }
            case 84 -> {
                if (FACADE_INFO.fieldNameEquals(10, 4, segment, offset, len)) {
                    return FACADE_INFO.infos().get(3);
                }
            }
        }
        throw new IllegalArgumentException("marshallInfo not found by fieldName");
    }

    @Override
    public MarshallInfo marshallInfoByMappedName(String mappedName) {
        int index = switch (mappedName) {
            case "INT" -> 0;
            case "LONG" -> 1;
            case "STR" -> 2;
            case "TIME" -> 3;
            default -> throw new IllegalArgumentException("mappedName not found: " + mappedName);
        };
        return FACADE_INFO.infos().get(index);
    }

    @Override
    public MarshallInfo marshallInfoByMappedName(byte[] bytes, int offset, int len) {
        int hash = FACADE_INFO.mappedNameHasher().hash(bytes, offset, len);
        switch (hash) {
            case 73 -> {
                if (FACADE_INFO.mappedNameEquals(0, 3, bytes, offset, len)) {
                    return FACADE_INFO.infos().get(0);
                }
            }
            case 76 -> {
                if (FACADE_INFO.mappedNameEquals(3, 4, bytes, offset, len)) {
                    return FACADE_INFO.infos().get(1);
                }
            }
            case 83 -> {
                if (FACADE_INFO.mappedNameEquals(7, 3, bytes, offset, len)) {
                    return FACADE_INFO.infos().get(2);
                }
            }
            case 84 -> {
                if (FACADE_INFO.mappedNameEquals(10, 4, bytes, offset, len)) {
                    return FACADE_INFO.infos().get(3);
                }
            }
        }
        throw new IllegalArgumentException("marshallInfo not found by mappedName");
    }

    @Override
    public MarshallInfo marshallInfoByMappedName(MemorySegment segment, long offset, long len) {
        int hash = FACADE_INFO.mappedNameHasher().hash(segment, offset, len);
        switch (hash) {
            case 73 -> {
                if (FACADE_INFO.mappedNameEquals(0, 3, segment, offset, len)) {
                    return FACADE_INFO.infos().get(0);
                }
            }
            case 76 -> {
                if (FACADE_INFO.mappedNameEquals(3, 4, segment, offset, len)) {
                    return FACADE_INFO.infos().get(1);
                }
            }
            case 83 -> {
                if (FACADE_INFO.mappedNameEquals(7, 3, segment, offset, len)) {
                    return FACADE_INFO.infos().get(2);
                }
            }
            case 84 -> {
                if (FACADE_INFO.mappedNameEquals(10, 4, segment, offset, len)) {
                    return FACADE_INFO.infos().get(3);
                }
            }
        }
        throw new IllegalArgumentException("marshallInfo not found by mappedName");
    }
}
