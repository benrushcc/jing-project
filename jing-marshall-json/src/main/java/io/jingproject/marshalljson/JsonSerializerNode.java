package io.jingproject.marshalljson;

import io.jingproject.marshall.MarshallFacade;
import io.jingproject.marshall.MarshallInfo;
import io.jingproject.marshall.MarshallUtil;

import java.util.*;

public final class JsonSerializerNode {
    private static final byte OBJ = (byte) 0;
    private static final byte ARR = (byte) 1;
    private static final byte COL = (byte) 2;
    private static final byte LIST = (byte) 3;
    private static final byte MAP = (byte) 4;
    private static final Map<Class<?>, JsonSerializerObjFunc> DIRECT_SERIALIZABLE_FUNC_MAP;
    private static final JsonSerializerObjFunc[] FUNC_TABLE;

    static {
        Map<Class<?>, JsonSerializerObjFunc> r = new HashMap<>();
        r.put(JsonPrimitiveType.class, (o, _, _, c) -> {
            c.serializeJsonPrimitiveType((JsonPrimitiveType) o);
            return JsonSerializeResult.Continue;
        });
        r.put(JsonBoolType.class, (o, _, _, c) -> {
            c.serializeJsonBoolType((JsonBoolType) o);
            return JsonSerializeResult.Continue;
        });
        r.put(JsonNumberType.class, (o, _, _, c) -> {
            c.serializeJsonNumberType((JsonNumberType) o);
            return JsonSerializeResult.Continue;
        });
        r.put(JsonStrType.class, (o, _, _, c) -> {
            c.serializeJsonStrType((JsonStrType) o);
            return JsonSerializeResult.Continue;
        });
        r.put(CharSequence[].class, (o, _, i, c) -> {
            c.serializeEscapedCharSequenceArray((CharSequence[]) o, i);
            return JsonSerializeResult.Continue;
        });
        r.put(String[].class, (o, _, i, c) -> {
            c.serializeEscapedStringArray((String[]) o, i);
            return JsonSerializeResult.Continue;
        });
        r.put(JsonPrimitiveType[].class, (o, _, i, c) -> {
            c.serializeJsonPrimitiveTypeArray((JsonPrimitiveType[]) o, i);
            return JsonSerializeResult.Continue;
        });
        r.put(JsonBoolType[].class, (o, _, i, c) -> {
            c.serializeJsonBoolTypeArray((JsonBoolType[]) o, i);
            return JsonSerializeResult.Continue;
        });
        r.put(JsonNumberType[].class, (o, _, i, c) -> {
            c.serializeJsonNumberTypeArray((JsonNumberType[]) o, i);
            return JsonSerializeResult.Continue;
        });
        r.put(JsonStrType[].class, (o, _, i, c) -> {
            c.serializeJsonStrTypeArray((JsonStrType[]) o, i);
            return JsonSerializeResult.Continue;
        });
        DIRECT_SERIALIZABLE_FUNC_MAP = Map.copyOf(r);
    }

    static {
        FUNC_TABLE = new JsonSerializerObjFunc[MarshallUtil.TYPE_SIZE];
        JsonSerializerObjFunc defaultFunc = (o, inf, i, c) -> {
            // exclude generic types
            if (inf.firstGenericType() != null || inf.secondGenericType() != null) {
                throw new JsonSerializerException("unsupported generic type : " + inf);
            }
            // matching direct serializable value
            Class<?> rawType = inf.rawType();
            JsonSerializerObjFunc directSerializableFunc = directSerializableFunc(rawType);
            if (directSerializableFunc != null) {
                return directSerializableFunc.serialize(o, inf, i, c);
            }
            // check if current type could be override by option
            JsonSerializeFunc customFunc = c.option().customFunc(rawType);
            if (customFunc != null) {
                return customFunc.serialize(o, i, c);
            }
            // assuming marshallable
            c.setObj(o);
            return JsonSerializeResult.NewMarshallable;
        };
        Arrays.fill(FUNC_TABLE, defaultFunc);
        // builtin supported wrapper types
        FUNC_TABLE[MarshallUtil.BYTE_WRAPPER_TYPE] = (o, _, _, c) -> {
            c.serializeByte((byte) o);
            return JsonSerializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.BOOLEAN_WRAPPER_TYPE] = (o, _, _, c) -> {
            c.serializeBoolean((boolean) o);
            return JsonSerializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.SHORT_WRAPPER_TYPE] = (o, _, _, c) -> {
            c.serializeShort((short) o);
            return JsonSerializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.CHAR_WRAPPER_TYPE] = (o, _, _, c) -> {
            c.serializeChar((char) o);
            return JsonSerializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.INT_WRAPPER_TYPE] = (o, _, _, c) -> {
            c.serializeInt((int) o);
            return JsonSerializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.LONG_WRAPPER_TYPE] = (o, _, _, c) -> {
            c.serializeLong((long) o);
            return JsonSerializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.FLOAT_WRAPPER_TYPE] = (o, _, _, c) -> {
            c.serializeFloat((float) o);
            return JsonSerializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.DOUBLE_WRAPPER_TYPE] = (o, _, _, c) -> {
            c.serializeDouble((double) o);
            return JsonSerializeResult.Continue;
        };
        // builtin supported primitive array types
        FUNC_TABLE[MarshallUtil.BYTE_ARRAY_TYPE] = (o, _, i, c) -> {
            c.serializeByteArray((byte[]) o, i);
            return JsonSerializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.BOOLEAN_ARRAY_TYPE] = (o, _, i, c) -> {
            c.serializeBooleanArray((boolean[]) o, i);
            return JsonSerializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.SHORT_ARRAY_TYPE] = (o, _, i, c) -> {
            c.serializeShortArray((short[]) o, i);
            return JsonSerializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.CHAR_ARRAY_TYPE] = (o, _, i, c) -> {
            c.serializeCharArray((char[]) o, i);
            return JsonSerializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.INT_ARRAY_TYPE] = (o, _, i, c) -> {
            c.serializeIntArray((int[]) o, i);
            return JsonSerializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.LONG_ARRAY_TYPE] = (o, _, i, c) -> {
            c.serializeLongArray((long[]) o, i);
            return JsonSerializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.FLOAT_ARRAY_TYPE] = (o, _, i, c) -> {
            c.serializeFloatArray((float[]) o, i);
            return JsonSerializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.DOUBLE_ARRAY_TYPE] = (o, _, i, c) -> {
            c.serializeDoubleArray((double[]) o, i);
            return JsonSerializeResult.Continue;
        };
        // builtin supported wrapper array types
        FUNC_TABLE[MarshallUtil.BYTE_WRAPPER_ARRAY_TYPE] = (o, _, i, c) -> {
            c.serializeByteWrapperArray((Byte[]) o, i);
            return JsonSerializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.BOOLEAN_WRAPPER_ARRAY_TYPE] = (o, _, i, c) -> {
            c.serializeBooleanWrapperArray((Boolean[]) o, i);
            return JsonSerializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.SHORT_WRAPPER_ARRAY_TYPE] = (o, _, i, c) -> {
            c.serializeShortWrapperArray((Short[]) o, i);
            return JsonSerializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.CHAR_WRAPPER_ARRAY_TYPE] = (o, _, i, c) -> {
            c.serializeCharWrapperArray((Character[]) o, i);
            return JsonSerializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.INT_WRAPPER_ARRAY_TYPE] = (o, _, i, c) -> {
            c.serializeIntWrapperArray((Integer[]) o, i);
            return JsonSerializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.LONG_WRAPPER_ARRAY_TYPE] = (o, _, i, c) -> {
            c.serializeLongWrapperArray((Long[]) o, i);
            return JsonSerializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.FLOAT_WRAPPER_ARRAY_TYPE] = (o, _, i, c) -> {
            c.serializeFloatWrapperArray((Float[]) o, i);
            return JsonSerializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.DOUBLE_WRAPPER_ARRAY_TYPE] = (o, _, i, c) -> {
            c.serializeDoubleWrapperArray((Double[]) o, i);
            return JsonSerializeResult.Continue;
        };
        // charsequence and string
        FUNC_TABLE[MarshallUtil.CHARSEQUENCE_TYPE] = (o, _, _, c) -> {
            c.serializeEscapedCharSequence((CharSequence) o);
            return JsonSerializeResult.Continue;
        };
        FUNC_TABLE[MarshallUtil.STRING_TYPE] = (o, _, _, c) -> {
            c.serializeEscapedString((String) o);
            return JsonSerializeResult.Continue;
        };
        // array
        FUNC_TABLE[MarshallUtil.ARRAY_TYPE] = (o, inf, i, c) -> {
            JsonSerializerObjFunc directSerializableFunc = directSerializableFunc(inf.rawType());
            if (directSerializableFunc != null) {
                return directSerializableFunc.serialize(o, inf, i, c);
            }
            c.setObj(o);
            return JsonSerializeResult.NewArray;
        };
        // enum
        FUNC_TABLE[MarshallUtil.ENUM_TYPE] = (o, inf, i, c) -> {
            Class<?> rawType = inf.rawType();
            JsonSerializeFunc customFunc = c.option().customFunc(rawType);
            if (customFunc != null) {
                customFunc.serialize(o, i, c);
            } else {
                c.serializeEnum((Enum<?>) o);
            }
            return JsonSerializeResult.Continue;
        };
        // collection type
        FUNC_TABLE[MarshallUtil.COLLECTION_INTERFACE_TYPE] = FUNC_TABLE[MarshallUtil.COLLECTION_IMPL_TYPE] = (o, inf, _, c) -> {
            c.setObj(o);
            c.setType(inf.firstGenericType());
            return JsonSerializeResult.NewCollection;
        };
        // map type
        FUNC_TABLE[MarshallUtil.MAP_INTERFACE_TYPE] = FUNC_TABLE[MarshallUtil.MAP_IMPL_TYPE] = (o, inf, _, c) -> {
            Class<?> keyType = inf.firstGenericType();
            if (keyType != CharSequence.class && keyType != String.class) {
                throw new JsonSerializerException("key type not supported: " + keyType.getName());
            }
            c.setObj(o);
            c.setType(inf.secondGenericType());
            return JsonSerializeResult.NewMap;
        };
    }

    private byte type;
    private boolean written;
    private int indent;
    private int index;
    private MarshallFacade fc;
    private Object marshallable;
    private Object[] arr;
    private List<?> list;
    private Iterator<?> colIter;
    private Iterator<? extends Map.Entry<?, ?>> mapIter;
    private JsonSerializeFunc func;

    private void serializeMarshallKey(MarshallInfo marshallInfo, JsonSerializerContext c) {
        c.serializePrefix(written ? (byte) ',' : (byte) '{', indent + 1);
        written = true;
        byte[] mappedNameUtf8Bytes = marshallInfo.mappedNameUtf8Bytes();
        if (marshallInfo.mappedNameSimple()) {
            c.serializeNonEscapedUtf8BytesAsStr(mappedNameUtf8Bytes);
        } else {
            c.serializeEscapedUtf8BytesAsStr(mappedNameUtf8Bytes);
        }
        c.append((byte) ':', (byte) ' ');
    }

    private void serializeMarshallPrimitiveValue(int type, JsonSerializerContext c) {
        switch (type) {
            case MarshallUtil.BYTE_TYPE -> c.serializeByte(fc.readByte(marshallable, index));
            case MarshallUtil.BOOLEAN_TYPE -> c.serializeBoolean(fc.readBoolean(marshallable, index));
            case MarshallUtil.SHORT_TYPE -> c.serializeShort(fc.readShort(marshallable, index));
            case MarshallUtil.CHAR_TYPE -> c.serializeChar(fc.readChar(marshallable, index));
            case MarshallUtil.INT_TYPE -> c.serializeInt(fc.readInt(marshallable, index));
            case MarshallUtil.LONG_TYPE -> c.serializeLong(fc.readLong(marshallable, index));
            case MarshallUtil.FLOAT_TYPE -> c.serializeFloat(fc.readFloat(marshallable, index));
            case MarshallUtil.DOUBLE_TYPE -> c.serializeDouble(fc.readDouble(marshallable, index));
            default -> throw new AssertionError();
        }
    }

    private void serializeMapKey(Object key, JsonSerializerContext c) {
        c.serializePrefix(written ? (byte) ',' : (byte) '{', indent + 1);
        written = true;
        if (key instanceof String str) {
            c.serializeEscapedString(str);
        } else if (key instanceof CharSequence charSequence) {
            c.serializeEscapedCharSequence(charSequence);
        } else {
            throw new AssertionError();
        }
        c.append((byte) ':', (byte) ' ');
    }

    private static JsonSerializerObjFunc directSerializableFunc(Class<?> rawType) {
        return DIRECT_SERIALIZABLE_FUNC_MAP.get(rawType);
    }

    public int indent() {
        return indent;
    }

    public void initObj(MarshallFacade marshallFacade, Object marshallable, int indent) {
        this.type = OBJ;
        this.written = false;
        this.indent = indent;
        this.index = 0;
        this.fc = marshallFacade;
        this.marshallable = marshallable;
    }

    public void initArr(Object[] arr, int indent, JsonSerializeFunc fn) {
        this.type = ARR;
        this.written = false;
        this.indent = indent;
        this.index = 0;
        this.arr = arr;
        this.func = fn;
    }

    public void initCol(Iterator<?> iter, int indent, JsonSerializeFunc fn) {
        this.type = COL;
        this.written = false;
        this.indent = indent;
        this.colIter = iter;
        this.func = fn;
    }

    public void initList(List<?> list, int indent, JsonSerializeFunc fn) {
        this.type = LIST;
        this.written = false;
        this.indent = indent;
        this.index = 0;
        this.list = list;
        this.func = fn;
    }

    public void initMap(Iterator<? extends Map.Entry<?, ?>> iter, int indent, JsonSerializeFunc fn) {
        this.type = MAP;
        this.written = false;
        this.indent = indent;
        this.mapIter = iter;
        this.func = fn;
    }

    public JsonSerializeResult process(JsonSerializerContext c) {
        return switch (type) {
            case OBJ -> processObj(c);
            case ARR -> processArr(c);
            case COL -> processCol(c);
            case LIST -> processList(c);
            case MAP -> processMap(c);
            default -> throw new AssertionError();
        };
    }

    private JsonSerializeResult processObj(JsonSerializerContext c) {
        List<MarshallInfo> marshallInfos = fc.marshallInfos();
        while (index < marshallInfos.size()) {
            MarshallInfo inf = marshallInfos.get(index);
            if (inf.skipSerializing()) {
                index++;
                continue;
            }
            int type = inf.type() & MarshallUtil.TYPE_MASK;
            if (type <= MarshallUtil.DOUBLE_TYPE) {
                serializeMarshallKey(inf, c);
                serializeMarshallPrimitiveValue(type, c);
                index++;
                continue;
            }
            Object fieldValue = fc.readObject(marshallable, index);
            if (fieldValue == null) {
                if (c.option().serializeNullInObjOrMap()) {
                    serializeMarshallKey(inf, c);
                    c.serializeNull();
                }
                index++;
                continue;
            }
            serializeMarshallKey(inf, c);
            JsonSerializeResult r = FUNC_TABLE[type].serialize(fieldValue, inf, indent, c);
            index++;
            if (r != JsonSerializeResult.Continue) {
                return r;
            }
        }
        if(!written) {
            c.serializePrefix((byte) '{', 0);
        }
        c.serializeSuffix((byte) '}', indent);
        return JsonSerializeResult.Finished;
    }

    private JsonSerializeResult processArr(JsonSerializerContext c) {
        while (index < arr.length) {
            c.serializePrefix(written ? (byte) ',' : (byte) '[', indent + 1);
            written = true;
            Object instance = arr[index++];
            if (instance == null) {
                c.serializeNull();
                continue;
            }
            JsonSerializeResult r = func.serialize(instance, indent, c);
            if (r != JsonSerializeResult.Continue) {
                return r;
            }
        }
        if(!written) {
            c.serializePrefix((byte) '[', 0);
        }
        c.serializeSuffix((byte) ']', indent);
        return JsonSerializeResult.Finished;
    }

    private JsonSerializeResult processCol(JsonSerializerContext c) {
        while (colIter.hasNext()) {
            c.serializePrefix(written ? (byte) ',' : (byte) '[', indent + 1);
            written = true;
            Object instance = colIter.next();
            if (instance == null) {
                c.serializeNull();
                continue;
            }
            JsonSerializeResult r = func.serialize(instance, indent, c);
            if (r != JsonSerializeResult.Continue) {
                return r;
            }
        }
        if(!written) {
            c.serializePrefix((byte) '[', 0);
        }
        c.serializeSuffix((byte) ']', indent);
        return JsonSerializeResult.Finished;
    }

    private JsonSerializeResult processList(JsonSerializerContext c) {
        while (index < list.size()) {
            c.serializePrefix(written ? (byte) ',' : (byte) '[', indent + 1);
            written = true;
            Object instance = list.get(index++);
            if (instance == null) {
                c.serializeNull();
                continue;
            }
            JsonSerializeResult r = func.serialize(instance, indent, c);
            if (r != JsonSerializeResult.Continue) {
                return r;
            }
        }
        if(!written) {
            c.serializePrefix((byte) '[', 0);
        }
        c.serializeSuffix((byte) ']', indent);
        return JsonSerializeResult.Finished;
    }

    private JsonSerializeResult processMap(JsonSerializerContext c) {
        while (mapIter.hasNext()) {
            Map.Entry<?, ?> entry = mapIter.next();
            Object key = entry.getKey();
            if (key == null) {
                continue;
            }
            Object value = entry.getValue();
            if (value == null) {
                if (c.option().serializeNullInObjOrMap()) {
                    serializeMapKey(key, c);
                    c.serializeNull();
                }
                continue;
            }
            serializeMapKey(key, c);
            JsonSerializeResult r = func.serialize(value, indent, c);
            if (r != JsonSerializeResult.Continue) {
                return r;
            }
        }
        if(!written) {
            c.serializePrefix((byte) '{', 0);
        }
        c.serializeSuffix((byte) '}', indent);
        return JsonSerializeResult.Finished;
    }

    @FunctionalInterface
    interface JsonSerializerObjFunc {
        JsonSerializeResult serialize(Object fieldValue, MarshallInfo inf, int indent, JsonSerializerContext c);
    }

}
