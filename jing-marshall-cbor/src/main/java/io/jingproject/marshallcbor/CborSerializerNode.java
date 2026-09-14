package io.jingproject.marshallcbor;

import io.jingproject.marshall.MarshallFacade;
import io.jingproject.marshall.MarshallInfo;
import io.jingproject.marshall.MarshallUtil;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

// explicit-stack node of the serialization state machine, mirroring the json
// serializer. OBJ/ARR/LIST/MAP write definite-length container heads while COL
// (a non-List Collection) streams an indefinite array (0x9f ... 0xff). OBJ and
// MAP fall back to indefinite forms in the default null-omitting mode because
// the actual entry count is then not known up front; with
// deterministicEncoding or serializeNullInObjOrMap they are definite and, in
// deterministic mode, keys are sorted by UTF-8 byte length then bytewise order.
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
        r.put(CborPrimitiveType.class, (o, _, c) -> {
            c.serializeCborPrimitiveType((CborPrimitiveType) o);
            return CborSerializeResult.Continue;
        });
        r.put(CborBoolType.class, (o, _, c) -> {
            c.serializeCborBoolType((CborBoolType) o);
            return CborSerializeResult.Continue;
        });
        r.put(CborNumberType.class, (o, _, c) -> {
            c.serializeCborNumberType((CborNumberType) o);
            return CborSerializeResult.Continue;
        });
        r.put(CborStrType.class, (o, _, c) -> {
            c.serializeCborStrType((CborStrType) o);
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
        r.put(CborPrimitiveType[].class, (o, _, c) -> {
            c.serializeCborPrimitiveTypeArray((CborPrimitiveType[]) o);
            return CborSerializeResult.Continue;
        });
        r.put(CborBoolType[].class, (o, _, c) -> {
            c.serializeCborBoolTypeArray((CborBoolType[]) o);
            return CborSerializeResult.Continue;
        });
        r.put(CborNumberType[].class, (o, _, c) -> {
            c.serializeCborNumberTypeArray((CborNumberType[]) o);
            return CborSerializeResult.Continue;
        });
        r.put(CborStrType[].class, (o, _, c) -> {
            c.serializeCborStrTypeArray((CborStrType[]) o);
            return CborSerializeResult.Continue;
        });
        r.put(CborBytesType[].class, (o, _, c) -> {
            c.serializeCborBytesTypeArray((CborBytesType[]) o);
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
        FUNC_TABLE[MarshallUtil.COLLECTION_INTERFACE_TYPE] = FUNC_TABLE[MarshallUtil.COLLECTION_IMPL_TYPE] = (o, inf, c) -> {
            c.setObj(o);
            c.setType(inf.firstGenericType());
            return CborSerializeResult.NewCollection;
        };
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
    private MarshallFacade fc;
    private Object marshallable;
    private Object[] arr;
    private List<?> list;
    private Iterator<?> colIter;
    private Iterator<? extends Map.Entry<?, ?>> mapIter;
    private MarshallInfo[] infos;
    private Map.Entry<?, ?>[] mapEntries;
    private CborSerializeFunc func;

    private void serializeMarshallKey(MarshallInfo inf, CborSerializerContext c) {
        c.serializeUtf8BytesAsStr(inf.mappedNameUtf8Bytes());
    }

    private void serializeMarshallPrimitiveValue(MarshallInfo inf, CborSerializerContext c) {
        switch (inf.type() & MarshallUtil.TYPE_MASK) {
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
    }

    private void serializeMapKey(Object key, CborSerializerContext c) {
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

    // deterministic ordering: shorter UTF-8 byte length first, then bytewise
    // unsigned lexicographic order, matching RFC 8949 core deterministic rules
    private static int compareMappedNameUtf8Bytes(MarshallInfo mi1, MarshallInfo mi2) {
        byte[] b1 = mi1.mappedNameUtf8Bytes();
        byte[] b2 = mi2.mappedNameUtf8Bytes();
        int lengthComparison = Integer.compare(b1.length, b2.length);
        if (lengthComparison != 0) {
            return lengthComparison;
        }
        return Arrays.compareUnsigned(b1, b2);
    }

    private static int compareMapKeyUtf8Bytes(Map.Entry<?, ?> e1, Map.Entry<?, ?> e2) {
        byte[] b1 = mapKeyUtf8Bytes(e1.getKey());
        byte[] b2 = mapKeyUtf8Bytes(e2.getKey());
        int lengthComparison = Integer.compare(b1.length, b2.length);
        if (lengthComparison != 0) {
            return lengthComparison;
        }
        return Arrays.compareUnsigned(b1, b2);
    }

    private static byte[] mapKeyUtf8Bytes(Object key) {
        if (key instanceof String str) {
            return str.getBytes(StandardCharsets.UTF_8);
        }
        if (key instanceof CharSequence charSequence) {
            return charSequence.toString().getBytes(StandardCharsets.UTF_8);
        }
        throw new AssertionError();
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
        this.arr = arr;
        this.func = fn;
    }

    public void initCol(Iterator<?> iter, CborSerializeFunc fn) {
        this.type = COL;
        this.written = false;
        this.indefinite = false;
        this.colIter = iter;
        this.func = fn;
    }

    public void initList(List<?> list, CborSerializeFunc fn) {
        this.type = LIST;
        this.written = false;
        this.indefinite = false;
        this.index = 0;
        this.list = list;
        this.func = fn;
    }

    public void initMap(Iterator<? extends Map.Entry<?, ?>> iter, CborSerializeFunc fn) {
        this.type = MAP;
        this.written = false;
        this.indefinite = false;
        this.index = 0;
        this.mapIter = iter;
        this.func = fn;
        this.mapEntries = null;
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
            infos = fc.marshallInfos().toArray(new MarshallInfo[0]);
            if (c.option().deterministicEncoding()) {
                Arrays.sort(infos, CborSerializerNode::compareMappedNameUtf8Bytes);
            }
        }
        if (!written) {
            written = true;
            if (c.option().effectiveSerializeNullInObjOrMap()) {
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
        while (index < infos.length) {
            MarshallInfo inf = infos[index];
            if (inf.skipSerializing()) {
                index++;
                continue;
            }
            int type = inf.type() & MarshallUtil.TYPE_MASK;
            if (type <= MarshallUtil.DOUBLE_TYPE) {
                serializeMarshallKey(inf, c);
                serializeMarshallPrimitiveValue(inf, c);
                index++;
                continue;
            }
            Object fieldValue = fc.readObject(marshallable, inf.index());
            if (fieldValue == null) {
                if (c.option().effectiveSerializeNullInObjOrMap()) {
                    serializeMarshallKey(inf, c);
                    c.serializeNull();
                }
                index++;
                continue;
            }
            serializeMarshallKey(inf, c);
            CborSerializeResult r = FUNC_TABLE[type].serialize(fieldValue, inf, c);
            index++;
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
            c.writeArrayHead(arr.length);
        }
        while (index < arr.length) {
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
            indefinite = true;
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
            c.writeArrayHead(list.size());
        }
        while (index < list.size()) {
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
        if (mapEntries == null) {
            List<Map.Entry<?, ?>> entryList = new ArrayList<>();
            while (mapIter.hasNext()) {
                Map.Entry<?, ?> entry = mapIter.next();
                // null keys are skipped entirely, mirroring the json parser
                if (entry.getKey() == null) {
                    continue;
                }
                entryList.add(entry);
            }
            mapEntries = entryList.toArray(new Map.Entry<?, ?>[0]);
            if (c.option().deterministicEncoding()) {
                Arrays.sort(mapEntries, CborSerializerNode::compareMapKeyUtf8Bytes);
            }
        }
        if (!written) {
            written = true;
            if (c.option().effectiveSerializeNullInObjOrMap()) {
                c.writeMapHead(mapEntries.length);
            } else {
                // null values are omitted, so the effective entry count is
                // unknown and an indefinite map is required
                indefinite = true;
                c.writeIndefiniteMapStart();
            }
        }
        while (index < mapEntries.length) {
            Map.Entry<?, ?> entry = mapEntries[index];
            Object value = entry.getValue();
            if (value == null) {
                if (c.option().effectiveSerializeNullInObjOrMap()) {
                    serializeMapKey(entry.getKey(), c);
                    c.serializeNull();
                }
                index++;
                continue;
            }
            serializeMapKey(entry.getKey(), c);
            CborSerializeResult r = func.serialize(value, c);
            index++;
            if (r != CborSerializeResult.Continue) {
                return r;
            }
        }
        if (indefinite) {
            c.writeBreak();
        }
        return CborSerializeResult.Finished;
    }

    @FunctionalInterface
    interface CborSerializerObjFunc {
        CborSerializeResult serialize(Object fieldValue, MarshallInfo inf, CborSerializerContext c);
    }
}