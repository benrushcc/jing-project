package io.jingproject.marshalltest.entity;

import io.jingproject.common.WriteBuffer;
import io.jingproject.marshall.*;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.time.LocalDateTime;
import java.util.List;

public final class BeanEntityMarshallFacade implements MarshallFacade {
    private static final MarshallFacadeInfo FACADE_INFO;
    private static final List<VarHandle> VHS;

    static {
        MarshallInfo mi0 = new MarshallInfo(int.class, null, null, 0, "intValue", "intValue", null, false, false);
        MarshallInfo mi1 = new MarshallInfo(long.class, null, null, 1, "longValue", "longValue", null, false, false);
        MarshallInfo mi2 = new MarshallInfo(String.class, null, null, 2, "strValue", "strValue", null, false, false);
        MarshallInfo mi3 = new MarshallInfo(LocalDateTime.class, null, null, 3, "timeValue", "timeValue", null, false, false);
        FACADE_INFO = new MarshallFacadeInfo(List.of(mi0, mi1, mi2, mi3));
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            MethodHandles.Lookup lookup0 = MethodHandles.privateLookupIn(BeanEntity.class, lookup);
            VarHandle vh0 = lookup0.findVarHandle(BeanEntity.class, "intValue", int.class);
            VarHandle vh1 = lookup0.findVarHandle(BeanEntity.class, "longValue", long.class);
            VarHandle vh2 = lookup0.findVarHandle(BeanEntity.class, "strValue", String.class);
            VarHandle vh3 = lookup0.findVarHandle(BeanEntity.class, "timeValue", LocalDateTime.class);
            VHS = List.of(vh0, vh1, vh2, vh3);
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    static VarHandle vh(int index) {
        return VHS.get(index);
    }

    @Override
    public Class<?> marshallableType() {
        return BeanEntity.class;
    }

    @Override
    public int totalElements() {
        return 4;
    }

    @Override
    public void writeFieldNameByIndex(WriteBuffer writeBuffer, int index) {
        switch (index) {
            case 0 -> writeBuffer.writeBytes(FACADE_INFO.fieldNameBytes(), 0, 8);
            case 1 -> writeBuffer.writeBytes(FACADE_INFO.fieldNameBytes(), 8, 9);
            case 2 -> writeBuffer.writeBytes(FACADE_INFO.fieldNameBytes(), 17, 8);
            case 3 -> writeBuffer.writeBytes(FACADE_INFO.fieldNameBytes(), 25, 9);
            default -> throw new IllegalArgumentException("wrong index");
        }
    }

    @Override
    public void writeMappedNameByIndex(WriteBuffer writeBuffer, int index) {
        switch (index) {
            case 0 -> writeBuffer.writeBytes(FACADE_INFO.mappedNameBytes(), 0, 8);
            case 1 -> writeBuffer.writeBytes(FACADE_INFO.mappedNameBytes(), 8, 9);
            case 2 -> writeBuffer.writeBytes(FACADE_INFO.mappedNameBytes(), 17, 8);
            case 3 -> writeBuffer.writeBytes(FACADE_INFO.mappedNameBytes(), 25, 9);
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
        int hash = FACADE_INFO.fieldNameHasher().hash(bytes, offset, len);
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
        int hash = FACADE_INFO.fieldNameHasher().hash(segment, offset, len);
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
        int hash = FACADE_INFO.mappedNameHasher().hash(bytes, offset, len);
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
        int hash = FACADE_INFO.mappedNameHasher().hash(segment, offset, len);
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
                if (FACADE_INFO.mappedNameEquals(17, 8, segment, offset, len)) {
                    return FACADE_INFO.infos().get(2);
                }
            }
            case 116 -> {
                if (FACADE_INFO.mappedNameEquals(25, 9, segment, offset, len)) {
                    return FACADE_INFO.infos().get(3);
                }
            }
        }
        throw new IllegalArgumentException("marshallInfo not found by mappedName");
    }

    @Override
    public MarshallReader newReader(Object target) {
        if(target instanceof BeanEntity instance) {
            return new BeanEntityMarshallReader(instance);
        }
        throw new IllegalArgumentException("wrong target : " + target.getClass().getName());
    }

    @Override
    public MarshallWriter newWriter() {
        BeanEntity instance = new BeanEntity();
        return new BeanEntityMarshallWriter(instance);
    }

    @Override
    public Object construct(MarshallWriter writer) {
        if (writer instanceof BeanEntityMarshallWriter(BeanEntity instance)) {
            return instance;
        }
        throw new IllegalArgumentException("wrong writer : " + writer.getClass().getName());
    }
}
