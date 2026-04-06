package io.jingproject.marshalltest.test;

import io.jingproject.marshall.MarshallFacade;
import io.jingproject.marshall.MarshallInfo;
import io.jingproject.marshall.MarshallUtil;
import io.jingproject.marshall.hash.Hasher;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

public final class RecordEntityMarshallImpl implements MarshallFacade {
    private static final MethodHandle CONSTRUCTOR_MH;
    private static final List<MarshallInfo> MARSHALLS;
    private static final Hasher MARSHALL_FIELDNAME_HASHER;
    private static final byte[] MARSHALL_FIELDNAME_BYTES;
    private static final MemorySegment MARSHALL_FIELDNAME_SEGMENT;
    private static final Hasher MARSHALL_MAPPEDNAME_HASHER;
    private static final byte[] MARSHALL_MAPPEDNAME_BYTES;
    private static final MemorySegment MARSHALL_MAPPEDNAME_SEGMENT;

    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(RecordEntity.class, MethodHandles.lookup());
            CONSTRUCTOR_MH = lookup.findConstructor(RecordEntity.class, MethodType.methodType(void.class, int.class, long.class, String.class, LocalDateTime.class));
            VarHandle intValueVh = lookup.findVarHandle(RecordEntity.class, "intValue", int.class);
            VarHandle longValueVh = lookup.findVarHandle(RecordEntity.class, "longValue", long.class);
            VarHandle strValueVh = lookup.findVarHandle(RecordEntity.class, "strValue", String.class);
            VarHandle timeValueVh = lookup.findVarHandle(RecordEntity.class, "timeValue", LocalDateTime.class);
            MarshallInfo intValueMi = new MarshallInfo(int.class, 0, "intValue", "intValue", intValueVh, 0);
            MarshallInfo longValueMi = new MarshallInfo(long.class, 1, "longValue", "longValue", longValueVh, 4);
            MarshallInfo strValueMi = new MarshallInfo(String.class, 2, "strValue", "strValue", strValueVh, 0);
            MarshallInfo timeValueMi = new MarshallInfo(LocalDateTime.class, 3, "timeValue", "timeValue", timeValueVh, 1);
            MARSHALLS = List.of(intValueMi, longValueMi, strValueMi, timeValueMi);
            MARSHALL_FIELDNAME_HASHER = MarshallUtil.calcHasher(MARSHALLS, MarshallInfo::fieldName);
            MARSHALL_FIELDNAME_BYTES = MarshallUtil.calcBytes(MARSHALLS, MarshallInfo::fieldName);
            MARSHALL_FIELDNAME_SEGMENT = MemorySegment.ofArray(MARSHALL_FIELDNAME_BYTES).asReadOnly();
            MARSHALL_MAPPEDNAME_HASHER = MarshallUtil.calcHasher(MARSHALLS, MarshallInfo::mappedName);
            MARSHALL_MAPPEDNAME_BYTES = MarshallUtil.calcBytes(MARSHALLS, MarshallInfo::mappedName);
            MARSHALL_MAPPEDNAME_SEGMENT =  MemorySegment.ofArray(MARSHALL_MAPPEDNAME_BYTES).asReadOnly();
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @Override
    public String marshallableName() {
        return "RecordEntity";
    }

    @Override
    public MethodHandle constructor() {
        return CONSTRUCTOR_MH;
    }

    @Override
    public int objectElements() {
        return 2;
    }

    @Override
    public int primitiveElements() {
        return 2;
    }

    @Override
    public int primitiveBytes() {
        return 12;
    }

    @Override
    public MarshallInfo marshallInfoByIndex(int index) {
        return MARSHALLS.get(index);
    }

    @Override
    public MarshallInfo marshallInfoByFieldName(String fieldName) {
        int index = switch (fieldName) {
            case "intValue" -> 0;
            case "longValue" -> 1;
            case "strValue" -> 2;
            case "timeValue" -> 3;
            default -> throw new IllegalArgumentException("FieldName not found: " + fieldName);
        };
        return MARSHALLS.get(index);
    }

    @Override
    public MarshallInfo marshallInfoByFieldName(byte[] bytes, int offset, int len) {
        int hash = MARSHALL_FIELDNAME_HASHER.hash(bytes, offset, len);
        switch (hash) {
            case 105 -> {
                if(Arrays.equals(MARSHALL_FIELDNAME_BYTES, 0, 8, bytes, offset, len)) {
                    return MARSHALLS.get(0);
                }
            }
            case 108 -> {
                if(Arrays.equals(MARSHALL_FIELDNAME_BYTES, 8, 17, bytes, offset, len)) {
                    return MARSHALLS.get(1);
                }
            }
            case 115 -> {
                if(Arrays.equals(MARSHALL_FIELDNAME_BYTES, 25, 33, bytes, offset, len)) {
                    return MARSHALLS.get(2);
                }
            }
            case 116 -> {
                if(Arrays.equals(MARSHALL_FIELDNAME_BYTES, 33, 42, bytes, offset, len)) {
                    return MARSHALLS.get(3);
                }
            }
        }
        throw new IllegalArgumentException("MarshallInfo not found by fieldName");
    }

    @Override
    public MarshallInfo marshallInfoByFieldName(MemorySegment segment, long offset, long len) {
        int hash = MARSHALL_FIELDNAME_HASHER.hash(segment, offset, len);
        switch (hash) {
            case 105 -> {
                if(MemorySegment.mismatch(MARSHALL_FIELDNAME_SEGMENT, 0L, 8L, segment, offset, len) == -1L) {
                    return MARSHALLS.get(0);
                }
            }
            case 108 -> {
                if(MemorySegment.mismatch(MARSHALL_FIELDNAME_SEGMENT, 8L, 17L, segment, offset, len) == -1L) {
                    return MARSHALLS.get(1);
                }
            }
            case 115 -> {
                if(MemorySegment.mismatch(MARSHALL_FIELDNAME_SEGMENT, 25L, 33L, segment, offset, len) == -1L) {
                    return MARSHALLS.get(2);
                }
            }
            case 116 -> {
                if(MemorySegment.mismatch(MARSHALL_FIELDNAME_SEGMENT, 33L, 42L, segment, offset, len) == -1L) {
                    return MARSHALLS.get(3);
                }
            }
        }
        throw new IllegalArgumentException("MarshallInfo not found by fieldName");
    }

    @Override
    public MarshallInfo marshallInfoByMappedName(String mappedName) {
        int index = switch (mappedName) {
            case "intValue" -> 0;
            case "longValue" -> 1;
            case "strValue" -> 2;
            case "timeValue" -> 3;
            default -> throw new IllegalArgumentException("MappedName not found: " + mappedName);
        };
        return MARSHALLS.get(index);
    }

    @Override
    public MarshallInfo marshallInfoByMappedName(byte[] bytes, int offset, int len) {
        int hash = MARSHALL_MAPPEDNAME_HASHER.hash(bytes, offset, len);
        switch (hash) {
            case 105 -> {
                if(Arrays.equals(MARSHALL_MAPPEDNAME_BYTES, 0, 8, bytes, offset, len)) {
                    return MARSHALLS.get(0);
                }
            }
            case 108 -> {
                if(Arrays.equals(MARSHALL_MAPPEDNAME_BYTES, 8, 17, bytes, offset, len)) {
                    return MARSHALLS.get(1);
                }
            }
            case 115 -> {
                if(Arrays.equals(MARSHALL_MAPPEDNAME_BYTES, 25, 33, bytes, offset, len)) {
                    return MARSHALLS.get(2);
                }
            }
            case 116 -> {
                if(Arrays.equals(MARSHALL_MAPPEDNAME_BYTES, 33, 42, bytes, offset, len)) {
                    return MARSHALLS.get(3);
                }
            }
        }
        throw new IllegalArgumentException("MarshallInfo not found by mappedName");
    }

    @Override
    public MarshallInfo marshallInfoByMappedName(MemorySegment segment, long offset, long len) {
        int hash = MARSHALL_MAPPEDNAME_HASHER.hash(segment, offset, len);
        switch (hash) {
            case 105 -> {
                if(MemorySegment.mismatch(MARSHALL_MAPPEDNAME_SEGMENT, 0L, 8L, segment, offset, len) == -1L) {
                    return MARSHALLS.get(0);
                }
            }
            case 108 -> {
                if(MemorySegment.mismatch(MARSHALL_MAPPEDNAME_SEGMENT, 8L, 17L, segment, offset, len) == -1L) {
                    return MARSHALLS.get(1);
                }
            }
            case 115 -> {
                if(MemorySegment.mismatch(MARSHALL_MAPPEDNAME_SEGMENT, 25L, 33L, segment, offset, len) == -1L) {
                    return MARSHALLS.get(2);
                }
            }
            case 116 -> {
                if(MemorySegment.mismatch(MARSHALL_MAPPEDNAME_SEGMENT, 33L, 42L, segment, offset, len) == -1L) {
                    return MARSHALLS.get(3);
                }
            }
        }
        throw new IllegalArgumentException("MarshallInfo not found by mappedName");
    }
}
