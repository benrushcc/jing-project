package io.jingproject.marshalltest.entity;

import io.jingproject.marshall.MarshallFacade;
import io.jingproject.marshall.MarshallInfo;
import io.jingproject.marshall.MarshallSchema;
import io.jingproject.marshall.MarshallUtil;
import io.jingproject.marshall.hash.Hasher;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;
import java.util.Arrays;
import java.util.List;

public final class EnumEntityMarshallFacade implements MarshallFacade {
    private static final List<MarshallInfo> MARSHALLS;
    private static final Hasher MARSHALL_FIELDNAME_HASHER;
    private static final byte[] MARSHALL_FIELDNAME_BYTES;
    private static final MemorySegment MARSHALL_FIELDNAME_SEGMENT;
    private static final Hasher MARSHALL_MAPPEDNAME_HASHER;
    private static final byte[] MARSHALL_MAPPEDNAME_BYTES;
    private static final MemorySegment MARSHALL_MAPPEDNAME_SEGMENT;

    static {
        MarshallInfo mi0 = new MarshallInfo(EnumEntity.class, 0, "INT", "INT", null, EnumEntity.INT, false, false);
        MarshallInfo mi1 = new MarshallInfo(EnumEntity.class, 1, "LONG", "LONG", null, EnumEntity.LONG, false, false);
        MarshallInfo mi2 = new MarshallInfo(EnumEntity.class, 2, "STR", "STR", null, EnumEntity.STR, false, false);
        MarshallInfo mi3 = new MarshallInfo(EnumEntity.class, 3, "TIME", "TIME", null, EnumEntity.TIME, false, false);
        MARSHALLS = List.of(mi0, mi1, mi2, mi3);
        List<String> fieldNames = MARSHALLS.stream().map(MarshallInfo::fieldName).toList();
        MARSHALL_FIELDNAME_HASHER = MarshallUtil.calcHasher(fieldNames);
        MARSHALL_FIELDNAME_BYTES = MarshallUtil.calcBytes(fieldNames);
        MARSHALL_FIELDNAME_SEGMENT = MemorySegment.ofArray(MARSHALL_FIELDNAME_BYTES).asReadOnly();
        List<String> mappedNames = MARSHALLS.stream().map(MarshallInfo::mappedName).toList();
        MARSHALL_MAPPEDNAME_HASHER = MarshallUtil.calcHasher(mappedNames);
        MARSHALL_MAPPEDNAME_BYTES = MarshallUtil.calcBytes(mappedNames);
        MARSHALL_MAPPEDNAME_SEGMENT =  MemorySegment.ofArray(MARSHALL_MAPPEDNAME_BYTES).asReadOnly();
    }

    @Override
    public Class<?> marshallableType() {
        return EnumEntity.class;
    }

    @Override
    public MethodHandle constructor() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object construct(MarshallSchema schema) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int totalElements() {
        return 4;
    }

    @Override
    public MarshallInfo marshallInfoByIndex(int index) {
        return MARSHALLS.get(index);
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
        return MARSHALLS.get(index);
    }

    @Override
    public MarshallInfo marshallInfoByFieldName(byte[] bytes, int offset, int len) {
        int hash = MARSHALL_FIELDNAME_HASHER.hash(bytes, offset, len);
        switch (hash) {
            case 73 -> {
                if(Arrays.equals(MARSHALL_FIELDNAME_BYTES, 0, 3, bytes, offset, len)) {
                    return MARSHALLS.get(0);
                }
            }
            case 76 -> {
                if(Arrays.equals(MARSHALL_FIELDNAME_BYTES, 3, 7, bytes, offset, len)) {
                    return MARSHALLS.get(1);
                }
            }
            case 83 -> {
                if(Arrays.equals(MARSHALL_FIELDNAME_BYTES, 7, 10, bytes, offset, len)) {
                    return MARSHALLS.get(2);
                }
            }
            case 84 -> {
                if(Arrays.equals(MARSHALL_FIELDNAME_BYTES, 10, 14, bytes, offset, len)) {
                    return MARSHALLS.get(3);
                }
            }
        }
        throw new IllegalArgumentException("marshallInfo not found by fieldName");
    }

    @Override
    public MarshallInfo marshallInfoByFieldName(MemorySegment segment, long offset, long len) {
        int hash = MARSHALL_FIELDNAME_HASHER.hash(segment, offset, len);
        switch (hash) {
            case 73 -> {
                if (MemorySegment.mismatch(MARSHALL_FIELDNAME_SEGMENT, 0, 3, segment, offset, len) == -1L) {
                    return MARSHALLS.get(0);
                }
            }
            case 76 -> {
                if (MemorySegment.mismatch(MARSHALL_FIELDNAME_SEGMENT, 3, 7, segment, offset, len) == -1L) {
                    return MARSHALLS.get(1);
                }
            }
            case 83 -> {
                if (MemorySegment.mismatch(MARSHALL_FIELDNAME_SEGMENT, 7, 10, segment, offset, len) == -1L) {
                    return MARSHALLS.get(2);
                }
            }
            case 84 -> {
                if (MemorySegment.mismatch(MARSHALL_FIELDNAME_SEGMENT, 10, 14, segment, offset, len) == -1L) {
                    return MARSHALLS.get(3);
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
        return MARSHALLS.get(index);
    }

    @Override
    public MarshallInfo marshallInfoByMappedName(byte[] bytes, int offset, int len) {
        int hash = MARSHALL_MAPPEDNAME_HASHER.hash(bytes, offset, len);
        switch (hash) {
            case 73 -> {
                if(Arrays.equals(MARSHALL_MAPPEDNAME_BYTES, 0, 3, bytes, offset, len)) {
                    return MARSHALLS.get(0);
                }
            }
            case 76 -> {
                if(Arrays.equals(MARSHALL_MAPPEDNAME_BYTES, 3, 7, bytes, offset, len)) {
                    return MARSHALLS.get(1);
                }
            }
            case 83 -> {
                if(Arrays.equals(MARSHALL_MAPPEDNAME_BYTES, 7, 10, bytes, offset, len)) {
                    return MARSHALLS.get(2);
                }
            }
            case 84 -> {
                if(Arrays.equals(MARSHALL_MAPPEDNAME_BYTES, 10, 14, bytes, offset, len)) {
                    return MARSHALLS.get(3);
                }
            }
        }
        throw new IllegalArgumentException("marshallInfo not found by mappedName");
    }

    @Override
    public MarshallInfo marshallInfoByMappedName(MemorySegment segment, long offset, long len) {
        int hash = MARSHALL_MAPPEDNAME_HASHER.hash(segment, offset, len);
        switch (hash) {
            case 73 -> {
                if (MemorySegment.mismatch(MARSHALL_MAPPEDNAME_SEGMENT, 0, 3, segment, offset, len) == -1L) {
                    return MARSHALLS.get(0);
                }
            }
            case 76 -> {
                if (MemorySegment.mismatch(MARSHALL_MAPPEDNAME_SEGMENT, 3, 7, segment, offset, len) == -1L) {
                    return MARSHALLS.get(1);
                }
            }
            case 83 -> {
                if (MemorySegment.mismatch(MARSHALL_MAPPEDNAME_SEGMENT, 7, 10, segment, offset, len) == -1L) {
                    return MARSHALLS.get(2);
                }
            }
            case 84 -> {
                if (MemorySegment.mismatch(MARSHALL_MAPPEDNAME_SEGMENT, 10, 14, segment, offset, len) == -1L) {
                    return MARSHALLS.get(3);
                }
            }
        }
        throw new IllegalArgumentException("marshallInfo not found by mappedName");
    }

    @Override
    public MarshallSchema newSchema() {
        throw new UnsupportedOperationException();
    }
}
