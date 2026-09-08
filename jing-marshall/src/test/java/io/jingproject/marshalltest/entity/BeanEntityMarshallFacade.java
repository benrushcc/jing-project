package io.jingproject.marshalltest.entity;

import io.jingproject.marshall.*;

import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.time.LocalDateTime;
import java.util.List;

public final class BeanEntityMarshallFacade implements MarshallFacade {
    private static final List<MarshallInfo> MARSHALL_INFOS;
    private static final MarshallHashInfo HASH_INFO;
    private static final List<VarHandle> VHS;

    static {
        MarshallInfo mi0 = new MarshallInfo(int.class, null, null, 0, "intValue", "intValue", false, false);
        MarshallInfo mi1 = new MarshallInfo(long.class, null, null, 1, "longValue", "longValue", false, false);
        MarshallInfo mi2 = new MarshallInfo(String.class, null, null, 2, "strValue", "strValue", false, false);
        MarshallInfo mi3 = new MarshallInfo(LocalDateTime.class, null, null, 3, "timeValue", "timeValue", false, false);
        MARSHALL_INFOS = List.of(mi0, mi1, mi2, mi3);
        HASH_INFO = new MarshallHashInfo(MARSHALL_INFOS);
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

    private static VarHandle vh(int index) {
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
    public int primitiveElements() {
        return 2;
    }

    @Override
    public List<MarshallInfo> marshallInfos() {
        return MARSHALL_INFOS;
    }

    @Override
    public MarshallInfo marshallInfoByFieldName(String fieldName) {
        return switch (fieldName) {
            case "intValue" -> MARSHALL_INFOS.get(0);
            case "longValue" -> MARSHALL_INFOS.get(1);
            case "strValue" -> MARSHALL_INFOS.get(2);
            case "timeValue" -> MARSHALL_INFOS.get(3);
            case null -> throw new IllegalArgumentException("fieldName is null");
            default -> null;
        };
    }

    @Override
    public MarshallInfo marshallInfoByFieldName(byte[] bytes, int from, int to) {
        int hash = HASH_INFO.fieldNameUtf8Hasher().hash(bytes, from, to);
        switch (hash) {
            case 105 -> {
                if (HASH_INFO.fieldNameEquals(0, 8, bytes, from, to)) {
                    return MARSHALL_INFOS.get(0);
                }
            }
            case 108 -> {
                if (HASH_INFO.fieldNameEquals(8, 17, bytes, from, to)) {
                    return MARSHALL_INFOS.get(1);
                }
            }
            case 115 -> {
                if (HASH_INFO.fieldNameEquals(17, 25, bytes, from, to)) {
                    return MARSHALL_INFOS.get(2);
                }
            }
            case 116 -> {
                if (HASH_INFO.fieldNameEquals(25, 34, bytes, from, to)) {
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
            case 105 -> {
                if (HASH_INFO.fieldNameEquals(0, 8, segment, from, to)) {
                    return MARSHALL_INFOS.get(0);
                }
            }
            case 108 -> {
                if (HASH_INFO.fieldNameEquals(8, 17, segment, from, to)) {
                    return MARSHALL_INFOS.get(1);
                }
            }
            case 115 -> {
                if (HASH_INFO.fieldNameEquals(17, 25, segment, from, to)) {
                    return MARSHALL_INFOS.get(2);
                }
            }
            case 116 -> {
                if (HASH_INFO.fieldNameEquals(25, 34, segment, from, to)) {
                    return MARSHALL_INFOS.get(3);
                }
            }
        }
        return null;
    }

    @Override
    public MarshallInfo marshallInfoByMappedName(String mappedName) {
        return switch (mappedName) {
            case "intValue" -> MARSHALL_INFOS.get(0);
            case "longValue" -> MARSHALL_INFOS.get(1);
            case "strValue" -> MARSHALL_INFOS.get(2);
            case "timeValue" -> MARSHALL_INFOS.get(3);
            case null -> throw new IllegalArgumentException("mappedName is null");
            default -> null;
        };
    }

    @Override
    public MarshallInfo marshallInfoByMappedName(byte[] bytes, int from, int to) {
        int hash = HASH_INFO.mappedNameUtf8Hasher().hash(bytes, from, to);
        switch (hash) {
            case 105 -> {
                if (HASH_INFO.mappedNameEquals(0, 8, bytes, from, to)) {
                    return MARSHALL_INFOS.get(0);
                }
            }
            case 108 -> {
                if (HASH_INFO.mappedNameEquals(8, 17, bytes, from, to)) {
                    return MARSHALL_INFOS.get(1);
                }
            }
            case 115 -> {
                if (HASH_INFO.mappedNameEquals(17, 25, bytes, from, to)) {
                    return MARSHALL_INFOS.get(2);
                }
            }
            case 116 -> {
                if (HASH_INFO.mappedNameEquals(25, 34, bytes, from, to)) {
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
            case 105 -> {
                if (HASH_INFO.mappedNameEquals(0, 8, segment, from, to)) {
                    return MARSHALL_INFOS.get(0);
                }
            }
            case 108 -> {
                if (HASH_INFO.mappedNameEquals(8, 17, segment, from, to)) {
                    return MARSHALL_INFOS.get(1);
                }
            }
            case 115 -> {
                if (HASH_INFO.mappedNameEquals(17, 25, segment, from, to)) {
                    return MARSHALL_INFOS.get(2);
                }
            }
            case 116 -> {
                if (HASH_INFO.mappedNameEquals(25, 34, segment, from, to)) {
                    return MARSHALL_INFOS.get(3);
                }
            }
        }
        return null;
    }

    @Override
    public int readInt(Object instance, int index) {
        BeanEntity entity = (BeanEntity) instance;
        return switch (index) {
            case 0 -> (int) VHS.get(0).get(entity);
            default -> throw new UnsupportedOperationException();
        };
    }

    @Override
    public long readLong(Object instance, int index) {
        BeanEntity entity = (BeanEntity) instance;
        return switch (index) {
            case 1 -> (long) VHS.get(1).get(entity);
            default -> throw new UnsupportedOperationException();
        };
    }

    @Override
    public Object readObject(Object instance, int index) {
        BeanEntity entity = (BeanEntity) instance;
        return switch (index) {
            case 2 -> (String) VHS.get(2).get(entity);
            case 3 -> (LocalDateTime) VHS.get(3).get(entity);
            default -> throw new UnsupportedOperationException();
        };
    }

    @Override
    public MarshallBuilder newBuilder() {
        return new Builder(new BeanEntity());
    }

    @Override
    public Object construct(MarshallBuilder writer) {
        if (writer instanceof Builder(BeanEntity instance)) {
            return instance;
        }
        throw new IllegalArgumentException("wrong writer : " + writer.getClass().getName());
    }

    private record Builder(BeanEntity instance) implements MarshallBuilder {

        @Override
        public void writeInt(int index, int value) {
            switch (index) {
                case 0 -> VHS.get(0).set(instance, value);
                default -> throw new UnsupportedOperationException();
            }
        }

        @Override
        public void writeLong(int index, long value) {
            switch (index) {
                case 1 -> VHS.get(1).set(instance, value);
                default -> throw new UnsupportedOperationException();
            }
        }

        @Override
        public void writeObject(int index, Object value) {
            switch (index) {
                case 2 -> VHS.get(2).set(instance, (String) value);
                case 3 -> VHS.get(3).set(instance, (LocalDateTime) value);
                default -> throw new UnsupportedOperationException();
            }
        }
    }
}
