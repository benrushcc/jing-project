package io.jingproject.marshalltest.entity;

import io.jingproject.marshall.MarshallFacade;
import io.jingproject.marshall.MarshallInfo;
import io.jingproject.marshall.MarshallSchema;
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

public final class BeanEntityMarshallFacade implements MarshallFacade {
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
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(BeanEntity.class, MethodHandles.lookup());
            VarHandle intValueVh = lookup.findVarHandle(BeanEntity.class, "intValue", int.class);
            VarHandle longValueVh = lookup.findVarHandle(BeanEntity.class, "longValue", long.class);
            VarHandle strValueVh = lookup.findVarHandle(BeanEntity.class, "strValue", String.class);
            VarHandle timeValueVh = lookup.findVarHandle(BeanEntity.class, "timeValue", LocalDateTime.class);
            MarshallInfo intValueMi = new MarshallInfo(int.class, 0, "intValue", "intValue", intValueVh, null, false, false);
            MarshallInfo longValueMi = new MarshallInfo(long.class, 1, "longValue", "longValue", longValueVh, null, false, false);
            MarshallInfo strValueMi = new MarshallInfo(String.class, 2, "strValue", "strValue", strValueVh, null, false, false);
            MarshallInfo timeValueMi = new MarshallInfo(LocalDateTime.class, 3, "timeValue", "timeValue", timeValueVh, null, false, false);
            CONSTRUCTOR_MH = lookup.findConstructor(BeanEntity.class, MethodType.methodType(void.class));
            MARSHALLS = List.of(intValueMi, longValueMi, strValueMi, timeValueMi);
            List<String> fieldNames = MARSHALLS.stream().map(MarshallInfo::fieldName).toList();
            MARSHALL_FIELDNAME_HASHER = MarshallUtil.calcHasher(fieldNames);
            MARSHALL_FIELDNAME_BYTES = MarshallUtil.calcBytes(fieldNames);
            MARSHALL_FIELDNAME_SEGMENT = MemorySegment.ofArray(MARSHALL_FIELDNAME_BYTES).asReadOnly();
            List<String> mappedNames = MARSHALLS.stream().map(MarshallInfo::mappedName).toList();
            MARSHALL_MAPPEDNAME_HASHER = MarshallUtil.calcHasher(mappedNames);
            MARSHALL_MAPPEDNAME_BYTES = MarshallUtil.calcBytes(mappedNames);
            MARSHALL_MAPPEDNAME_SEGMENT =  MemorySegment.ofArray(MARSHALL_MAPPEDNAME_BYTES).asReadOnly();
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @Override
    public Class<?> marshallableType() {
        return BeanEntity.class;
    }

    @Override
    public MethodHandle constructor() {
        return CONSTRUCTOR_MH;
    }

    @Override
    public Object construct(MarshallSchema schema) {
        if(schema instanceof BeanEntityMarshallSchema(_, BeanEntity instance)) {
            return instance;
        }
        throw new IllegalArgumentException("wrong schema type");
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
            case "intValue" -> 0;
            case "longValue" -> 1;
            case "strValue" -> 2;
            case "timeValue" -> 3;
            default -> throw new IllegalArgumentException("fieldName not found: " + fieldName);
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
                if(Arrays.equals(MARSHALL_FIELDNAME_BYTES, 17, 25, bytes, offset, len)) {
                    return MARSHALLS.get(2);
                }
            }
            case 116 -> {
                if(Arrays.equals(MARSHALL_FIELDNAME_BYTES, 25, 34, bytes, offset, len)) {
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
                if(MemorySegment.mismatch(MARSHALL_FIELDNAME_SEGMENT, 17L, 25L, segment, offset, len) == -1L) {
                    return MARSHALLS.get(2);
                }
            }
            case 116 -> {
                if(MemorySegment.mismatch(MARSHALL_FIELDNAME_SEGMENT, 25L, 34L, segment, offset, len) == -1L) {
                    return MARSHALLS.get(3);
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
                if(Arrays.equals(MARSHALL_MAPPEDNAME_BYTES, 17, 25, bytes, offset, len)) {
                    return MARSHALLS.get(2);
                }
            }
            case 116 -> {
                if(Arrays.equals(MARSHALL_MAPPEDNAME_BYTES, 25, 34, bytes, offset, len)) {
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
                if(MemorySegment.mismatch(MARSHALL_MAPPEDNAME_SEGMENT, 17L, 25L, segment, offset, len) == -1L) {
                    return MARSHALLS.get(2);
                }
            }
            case 116 -> {
                if(MemorySegment.mismatch(MARSHALL_MAPPEDNAME_SEGMENT, 25L, 34L, segment, offset, len) == -1L) {
                    return MARSHALLS.get(3);
                }
            }
        }
        throw new IllegalArgumentException("marshallInfo not found by mappedName");
    }

    @Override
    public MarshallSchema newSchema() {
        BeanEntity instance = new BeanEntity();
        return new BeanEntityMarshallSchema(this, instance);
    }
}
