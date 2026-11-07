package io.jingproject.marshallcbor;

import io.jingproject.marshall.MarshallFacade;
import io.jingproject.marshall.MarshallInfo;
import io.jingproject.marshall.MarshallUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

// explicit-stack node of the serialization state machine, mirroring the json serializer.
// object and map heads are definite when null entries are written, otherwise they are
// indefinite because the final entry count is not known without a first pass. a list or
// array is always definite; a non-list collection is streamed as an indefinite array.
public final class CborSerializerNode {
    private static final byte OBJ = (byte) 0;
    private static final byte ARR = (byte) 1;
    private static final byte COL = (byte) 2;
    private static final byte LIST = (byte) 3;
    private static final byte MAP = (byte) 4;
    private static final Map<Class<?>, CborSerializerObjFunc> DIRECT_SERIALIZABLE_FUNC_MAP;
    private static final CborSerializerObjFunc[] FUNC_TABLE;

    static {
        Map<Class<?>, CborSerializerObjFunc> r = new HashMap<>();
        r.put(CborValue.class, (o, _, c) -> {
            c.serializeCborValue((CborValue) o);
            return CborSerializeResult.Continue;
        });
        r.put(CborTaggedType.class, (o, _, c) -> {
            c.serializeCborTaggedType((CborTaggedType) o);
            return CborSerializeResult.Continue;
        });
        r.put(CborBytesType.class, (o, _, c) -> {
            c.serializeCborBytesType((CborBytesType) o);
            return CborSerializeResult.Continue;
        });
        r.put(CharSequence[].class, (o, _, c) -> {
            c.serializeStrArray((CharSequence[]) o);
            return CborSerializeResult.Continue;
        });
        r.put(String[].class, (o, _, c) -> {
            c.serializeStrArray((String[]) o);
            return CborSerializeResult.Continue;
        });
        DIRECT_SERIALIZABLE_FUNC_MAP = Map.copyOf(r);
    }

    static {
        FUNC_TABLE = new CborSerializerObjFunc[MarshallUtil.TYPE_SIZE];
        CborSerializerObjFunc defaultFunc = (o, inf, c) -> {
            // exclude generic types
            if (inf.firstGenericType() != null || inf.secondGenericType() != null) {
                throw new CborSerializerException("unsupported generic type : " + inf);
            }
            // matching direct serializable value
            Class<?> rawType = inf.rawType();
            CborSerializerObjFunc directSerializableFunc = directSerializableFunc(rawType);
            if (directSerializableFunc != null) {
                return directSerializableFunc.serialize(o, inf, c);
            }
            // check if current type could be overridden by option
            CborSerializeFunc customFunc = c.option().customFunc(rawType);
            if (customFunc != null) {
                return customFunc.serialize(o, c);
            }
            // assuming marshallable
            c.setObj(o);
            return CborSerializeResult.NewMarshallable;
        };
        Arrays.fill(FUNC_TABLE, defaultFunc);
        // builtin supported wrapper types
        FUNC_TABLE[MarshallUtil.BYTE_WRAPPER_TYPE] = (o, _, c) -> {
            c.serializeByte((byte) o);
            return CborSerializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.BOOLEAN_WRAPPER_TYPE] = (o, _, c) -> {
            c.serializeBoolean((boolean) o);
            return CborSerializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.SHORT_WRAPPER_TYPE] = (o, _, c) -> {
            c.serializeShort((short) o);
            return CborSerializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.CHAR_WRAPPER_TYPE] = (o, _, c) -> {
            c.serializeChar((char) o);
            return CborSerializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.INT_WRAPPER_TYPE] = (o, _, c) -> {
            c.serializeInt((int) o);
            return CborSerializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.LONG_WRAPPER_TYPE] = (o, _, c) -> {
            c.serializeLong((long) o);
            return CborSerializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.FLOAT_WRAPPER_TYPE] = (o, _, c) -> {
            c.serializeFloat((float) o);
            return CborSerializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.DOUBLE_WRAPPER_TYPE] = (o, _, c) -> {
            c.serializeDouble((double) o);
            return CborSerializeResult.Continue;
        };
        // builtin supported primitive array types; byte[] is a CBOR byte string
        FUNC_TABLE[MarshallUtil.BYTE_ARRAY_TYPE] = (o, _, c) -> {
            c.serializeBytes((byte[]) o);
            return CborSerializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.BOOLEAN_ARRAY_TYPE] = (o, _, c) -> {
            c.serializeBooleanArray((boolean[]) o);
            return CborSerializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.SHORT_ARRAY_TYPE] = (o, _, c) -> {
            c.serializeShortArray((short[]) o);
            return CborSerializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.CHAR_ARRAY_TYPE] = (o, _, c) -> {
            c.serializeCharArray((char[]) o);
            return CborSerializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.INT_ARRAY_TYPE] = (o, _, c) -> {
            c.serializeIntArray((int[]) o);
            return CborSerializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.LONG_ARRAY_TYPE] = (o, _, c) -> {
            c.serializeLongArray((long[]) o);
            return CborSerializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.FLOAT_ARRAY_TYPE] = (o, _, c) -> {
            c.serializeFloatArray((float[]) o);
            return CborSerializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.DOUBLE_ARRAY_TYPE] = (o, _, c) -> {
            c.serializeDoubleArray((double[]) o);
            return CborSerializeResult.Continue;
        };
        // builtin supported wrapper array types
        FUNC_TABLE[MarshallUtil.BYTE_WRAPPER_ARRAY_TYPE] = (o, _, c) -> {
            c.serializeByteWrapperArray((Byte[]) o);
            return CborSerializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.BOOLEAN_WRAPPER_ARRAY_TYPE] = (o, _, c) -> {
            c.serializeBooleanWrapperArray((Boolean[]) o);
            return CborSerializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.SHORT_WRAPPER_ARRAY_TYPE] = (o, _, c) -> {
            c.serializeShortWrapperArray((Short[]) o);
            return CborSerializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.CHAR_WRAPPER_ARRAY_TYPE] = (o, _, c) -> {
            c.serializeCharWrapperArray((Character[]) o);
            return CborSerializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.INT_WRAPPER_ARRAY_TYPE] = (o, _, c) -> {
            c.serializeIntWrapperArray((Integer[]) o);
            return CborSerializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.LONG_WRAPPER_ARRAY_TYPE] = (o, _, c) -> {
            c.serializeLongWrapperArray((Long[]) o);
            return CborSerializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.FLOAT_WRAPPER_ARRAY_TYPE] = (o, _, c) -> {
            c.serializeFloatWrapperArray((Float[]) o);
            return CborSerializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.DOUBLE_WRAPPER_ARRAY_TYPE] = (o, _, c) -> {
            c.serializeDoubleWrapperArray((Double[]) o);
            return CborSerializeResult.Continue;
        };
        // charsequence and string
        FUNC_TABLE[MarshallUtil.CHARSEQUENCE_TYPE] = (o, _, c) -> {
            c.serializeStr((CharSequence) o);
            return CborSerializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.STRING_TYPE] = (o, _, c) -> {
            c.serializeStr((String) o);
            return CborSerializeResult.Continue;
        };
        // reference array
        FUNC_TABLE[MarshallUtil.ARRAY_TYPE] = (o, inf, c) -> {
            CborSerializerObjFunc directSerializableFunc = directSerializableFunc(inf.rawType());
            if (directSerializableFunc != null) {
                return directSerializableFunc.serialize(o, inf, c);
            }
            c.setObj(o);
            return CborSerializeResult.NewArray;
        };
        // enum
        FUNC_TABLE[MarshallUtil.ENUM_TYPE] = (o, inf, c) -> {
            Class<?> rawType = inf.rawType();
            CborSerializeFunc customFunc = c.option().customFunc(rawType);
            if (customFunc != null) {
                customFunc.serialize(o, c);
            } else {
                c.serializeEnum((Enum<?>) o);
            }
            return CborSerializeResult.Continue;
        };
        // collection type
        CborSerializerObjFunc collectionFunc = (o, inf, c) -> {
            c.setObj(o);
            c.setType(inf.firstGenericType());
            return CborSerializeResult.NewCollection;
        };
        FUNC_TABLE[MarshallUtil.COLLECTION_INTERFACE_TYPE] = collectionFunc;
        FUNC_TABLE[MarshallUtil.COLLECTION_IMPL_TYPE] = collectionFunc;
        // map type
        FUNC_TABLE[MarshallUtil.MAP_INTERFACE_TYPE] = FUNC_TABLE[MarshallUtil.MAP_IMPL_TYPE] = (o, inf, c) -> {
            Class<?> keyType = inf.firstGenericType();
            if (keyType != CharSequence.class && keyType != String.class) {
                throw new CborSerializerException("key type not supported: " + keyType.getName());
            }
            c.setObj(o);
            c.setType(inf.secondGenericType());
            return CborSerializeResult.NewMap;
        };
    }

    private byte type;
    private boolean written;
    private boolean indefinite;
    private int index;
    private int size;
    private MarshallFacade fc;
    private Object marshallable;
    private Object[] arr;
    private List<?> list;
    private Collection<?> col;
    private Iterator<?> colIter;
    private Map<?, ?> map;
    private Iterator<? extends Map.Entry<?, ?>> mapIter;
    private List<MarshallInfo> infos;
    private Map.Entry<?, ?>[] mapEntries;
    private CborSerializeFunc func;

    private void serializeMarshallKey(MarshallInfo inf, CborSerializerContext c) {
        c.serializeUtf8BytesAsStr(inf.mappedNameUtf8Bytes());
    }

    private static void serializeMapKey(Object key, CborSerializerContext c) {
        if (key instanceof String str) {
            c.serializeStr(str);
        } else if (key instanceof CharSequence charSequence) {
            c.serializeStr(charSequence);
        } else {
            throw new AssertionError();
        }
    }

    private static CborSerializerObjFunc directSerializableFunc(Class<?> rawType) {
        return DIRECT_SERIALIZABLE_FUNC_MAP.get(rawType);
    }

    public void initObj(MarshallFacade marshallFacade, Object marshallable) {
        this.type = OBJ;
        this.written = false;
        this.indefinite = false;
        this.index = 0;
        this.fc = marshallFacade;
        this.marshallable = marshallable;
        this.infos = null;
    }

    public void initArr(Object[] arr, CborSerializeFunc fn) {
        this.type = ARR;
        this.written = false;
        this.indefinite = false;
        this.index = 0;
        this.size = arr.length;
        this.arr = arr;
        this.func = fn;
    }

    public void initCol(Collection<?> col, CborSerializeFunc fn) {
        this.type = COL;
        this.written = false;
        this.indefinite = false;
        this.index = 0;
        this.col = col;
        this.colIter = col.iterator();
        this.func = fn;
    }

    public void initList(List<?> list, CborSerializeFunc fn) {
        this.type = LIST;
        this.written = false;
        this.indefinite = false;
        this.index = 0;
        this.size = list.size();
        this.list = list;
        this.func = fn;
    }

    public void initMap(Map<?, ?> map, CborSerializeFunc fn) {
        this.type = MAP;
        this.written = false;
        this.indefinite = false;
        this.index = 0;
        this.map = map;
        this.mapIter = null;
        this.mapEntries = null;
        this.func = fn;
    }

    public CborSerializeResult process(CborSerializerContext c) {
        return switch (type) {
            case OBJ -> processObj(c);
            case ARR -> processArr(c);
            case COL -> processCol(c);
            case LIST -> processList(c);
            case MAP -> processMap(c);
            default -> throw new AssertionError();
        };
    }

    private CborSerializeResult processObj(CborSerializerContext c) {
        if (infos == null) {
            infos = fc.marshallInfos();
        }
        if (!written) {
            written = true;
            if (c.option().serializeNullInObjOrMap()) {
                int fieldCount = 0;
                for (MarshallInfo inf : infos) {
                    if (!inf.skipSerializing()) {
                        fieldCount++;
                    }
                }
                c.writeMapHead(fieldCount);
            } else {
                // null omission may reduce the effective entry count, so the
                // entry count is unknown and an indefinite map is required
                indefinite = true;
                c.writeIndefiniteMapStart();
            }
        }
        while (index < infos.size()) {
            MarshallInfo inf = infos.get(index++);
            if (inf.skipSerializing()) {
                continue;
            }
            int type = inf.type() & MarshallUtil.TYPE_MASK;
            if (type <= MarshallUtil.DOUBLE_TYPE) {
                serializeMarshallKey(inf, c);
                switch (type) {
                    case MarshallUtil.BYTE_TYPE -> c.serializeByte(fc.readByte(marshallable, inf.index()));
                    case MarshallUtil.BOOLEAN_TYPE -> c.serializeBoolean(fc.readBoolean(marshallable, inf.index()));
                    case MarshallUtil.SHORT_TYPE -> c.serializeShort(fc.readShort(marshallable, inf.index()));
                    case MarshallUtil.CHAR_TYPE -> c.serializeChar(fc.readChar(marshallable, inf.index()));
                    case MarshallUtil.INT_TYPE -> c.serializeInt(fc.readInt(marshallable, inf.index()));
                    case MarshallUtil.LONG_TYPE -> c.serializeLong(fc.readLong(marshallable, inf.index()));
                    case MarshallUtil.FLOAT_TYPE -> c.serializeFloat(fc.readFloat(marshallable, inf.index()));
                    case MarshallUtil.DOUBLE_TYPE -> c.serializeDouble(fc.readDouble(marshallable, inf.index()));
                    default -> throw new AssertionError();
                }
                continue;
            }
            Object fieldValue = fc.readObject(marshallable, inf.index());
            if (fieldValue == null) {
                if (c.option().serializeNullInObjOrMap()) {
                    serializeMarshallKey(inf, c);
                    c.serializeNull();
                }
                continue;
            }
            serializeMarshallKey(inf, c);
            CborSerializeResult r = FUNC_TABLE[type].serialize(fieldValue, inf, c);
            if (r != CborSerializeResult.Continue) {
                return r;
            }
        }
        if (indefinite) {
            c.writeBreak();
        }
        return CborSerializeResult.Finished;
    }

    private CborSerializeResult processArr(CborSerializerContext c) {
        if (!written) {
            written = true;
            c.writeArrayHead(size);
        }
        while (index < size) {
            Object instance = arr[index++];
            if (instance == null) {
                c.serializeNull();
                continue;
            }
            CborSerializeResult r = func.serialize(instance, c);
            if (r != CborSerializeResult.Continue) {
                return r;
            }
        }
        return CborSerializeResult.Finished;
    }

    private CborSerializeResult processCol(CborSerializerContext c) {
        if (!written) {
            written = true;
            c.writeIndefiniteArrayStart();
        }
        while (colIter.hasNext()) {
            Object instance = colIter.next();
            if (instance == null) {
                c.serializeNull();
                continue;
            }
            CborSerializeResult r = func.serialize(instance, c);
            if (r != CborSerializeResult.Continue) {
                return r;
            }
        }
        c.writeBreak();
        return CborSerializeResult.Finished;
    }

    private CborSerializeResult processList(CborSerializerContext c) {
        if (!written) {
            written = true;
            c.writeArrayHead(size);
        }
        while (index < size) {
            Object instance = list.get(index++);
            if (instance == null) {
                c.serializeNull();
                continue;
            }
            CborSerializeResult r = func.serialize(instance, c);
            if (r != CborSerializeResult.Continue) {
                return r;
            }
        }
        return CborSerializeResult.Finished;
    }

    private CborSerializeResult processMap(CborSerializerContext c) {
        if (c.option().serializeNullInObjOrMap()) {
            if (mapEntries == null) {
                List<Map.Entry<?, ?>> entries = new ArrayList<>(map.size());
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    // null keys are skipped entirely, mirroring the json serializer
                    if (entry.getKey() != null) {
                        entries.add(entry);
                    }
                }
                mapEntries = entries.toArray(new Map.Entry<?, ?>[0]);
            }
            if (!written) {
                written = true;
                c.writeMapHead(mapEntries.length);
            }
            while (index < mapEntries.length) {
                Map.Entry<?, ?> entry = mapEntries[index++];
                serializeMapKey(entry.getKey(), c);
                Object value = entry.getValue();
                if (value == null) {
                    c.serializeNull();
                    continue;
                }
                CborSerializeResult r = func.serialize(value, c);
                if (r != CborSerializeResult.Continue) {
                    return r;
                }
            }
            return CborSerializeResult.Finished;
        }
        if (!written) {
            written = true;
            indefinite = true;
            mapIter = map.entrySet().iterator();
            c.writeIndefiniteMapStart();
        }
        while (mapIter.hasNext()) {
            Map.Entry<?, ?> entry = mapIter.next();
            Object key = entry.getKey();
            if (key == null) {
                continue;
            }
            Object value = entry.getValue();
            // null values are omitted in the default mode, hence the indefinite head
            if (value == null) {
                continue;
            }
            serializeMapKey(key, c);
            CborSerializeResult r = func.serialize(value, c);
            if (r != CborSerializeResult.Continue) {
                return r;
            }
        }
        c.writeBreak();
        return CborSerializeResult.Finished;
    }

    @FunctionalInterface
    interface CborSerializerObjFunc {
        CborSerializeResult serialize(Object fieldValue, MarshallInfo inf, CborSerializerContext c);
    }
}
