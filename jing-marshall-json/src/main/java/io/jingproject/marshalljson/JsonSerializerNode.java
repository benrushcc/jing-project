package io.jingproject.marshalljson;

import io.jingproject.common.WriteBuffer;
import io.jingproject.marshall.MarshallFacade;
import io.jingproject.marshall.MarshallInfo;
import io.jingproject.marshall.MarshallReader;
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
            c.set(o);
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
            c.set(o);
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
            c.set(o, inf.firstGenericType());
            return JsonSerializeResult.NewCollection;
        };
        // map type
        FUNC_TABLE[MarshallUtil.MAP_INTERFACE_TYPE] = FUNC_TABLE[MarshallUtil.MAP_IMPL_TYPE] = (o, inf, _, c) -> {
            Class<?> keyType = inf.firstGenericType();
            if (keyType != CharSequence.class && keyType != String.class) {
                throw new JsonSerializerException("key type not supported: " + keyType.getName());
            }
            c.set(o, inf.secondGenericType());
            return JsonSerializeResult.NewMap;
        };
    }

    private byte type;
    private boolean written;
    private int indent;
    private int index;
    private int size;
    private Object val;
    private MarshallFacade fc;
    private JsonSerializeFunc func;

    private static void serializeMarshallKey(MarshallInfo marshallInfo, int indent, boolean written, JsonSerializerContext c) {
        WriteBuffer w = c.writeBuffer();
        if (written) {
            w.writeByte((byte) ',');
        }
        c.serializeIndent(indent);
        byte[] mappedNameUtf8Bytes = marshallInfo.mappedNameUtf8Bytes();
        if (marshallInfo.mappedNameSimple()) {
            c.serializeNonEscapedUtf8Bytes(mappedNameUtf8Bytes);
        } else {
            c.serializeEscapedUtf8Bytes(mappedNameUtf8Bytes);
        }
        w.writeBytes((byte) ':', (byte) ' ');
    }

    private static void serializeMarshallPrimitiveValue(MarshallReader reader, int index, int type, JsonSerializerContext c) {
        switch (type) {
            case MarshallUtil.BYTE_TYPE -> c.serializeByte(reader.getByte(index));
            case MarshallUtil.BOOLEAN_TYPE -> c.serializeBoolean(reader.getBoolean(index));
            case MarshallUtil.SHORT_TYPE -> c.serializeShort(reader.getShort(index));
            case MarshallUtil.CHAR_TYPE -> c.serializeChar(reader.getChar(index));
            case MarshallUtil.INT_TYPE -> c.serializeInt(reader.getInt(index));
            case MarshallUtil.LONG_TYPE -> c.serializeLong(reader.getLong(index));
            case MarshallUtil.FLOAT_TYPE -> c.serializeFloat(reader.getFloat(index));
            case MarshallUtil.DOUBLE_TYPE -> c.serializeDouble(reader.getDouble(index));
            default -> throw new AssertionError();
        }
    }

    private static void serializeMapKey(Object key, int indent, boolean written, JsonSerializerContext c) {
        WriteBuffer w = c.writeBuffer();
        if (written) {
            w.writeByte((byte) ',');
        }
        c.serializeIndent(indent);
        if (key instanceof String str) {
            c.serializeEscapedString(str);
        } else if (key instanceof CharSequence charSequence) {
            c.serializeEscapedCharSequence(charSequence);
        } else {
            throw new JsonSerializerException("unsupported json key : " + key);
        }
        w.writeBytes((byte) ':', (byte) ' ');
    }

    private static JsonSerializerObjFunc directSerializableFunc(Class<?> rawType) {
        return DIRECT_SERIALIZABLE_FUNC_MAP.get(rawType);
    }

    public int indent() {
        return indent;
    }

    public void initObj(MarshallFacade fc, Object marshallable, int indent) {
        this.type = OBJ;
        this.written = false;
        this.indent = indent;
        this.index = 0;
        this.size = fc.totalElements();
        this.val = fc.newReader(marshallable);
        this.fc = fc;
    }

    public void initArr(Object[] arr, int indent, JsonSerializeFunc fn) {
        this.type = ARR;
        this.written = false;
        this.indent = indent;
        this.index = 0;
        this.val = arr;
        this.func = fn;
    }

    public void initCol(int size, Iterator<?> iter, int indent, JsonSerializeFunc fn) {
        this.type = COL;
        this.written = false;
        this.indent = indent;
        this.index = 0;
        this.size = size;
        this.val = iter;
        this.func = fn;
    }

    public void initList(List<?> list, int indent, JsonSerializeFunc fn) {
        this.type = LIST;
        this.written = false;
        this.indent = indent;
        this.index = 0;
        this.val = list;
        this.func = fn;
    }

    public void initMap(int size, Iterator<? extends Map.Entry<?, ?>> iter, int indent, JsonSerializeFunc fn) {
        this.type = MAP;
        this.written = false;
        this.indent = indent;
        this.index = 0;
        this.size = size;
        this.val = iter;
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
        final MarshallFacade m = this.fc;
        final MarshallReader reader = (MarshallReader) this.val;
        final int range = this.size;
        final int ind = this.indent;
        boolean wtn = this.written;
        for (int i = index; i < range; i++) {
            MarshallInfo inf = m.marshallInfoByIndex(i);
            if (inf.skipSerializing()) {
                continue;
            }
            int type = inf.type() & MarshallUtil.TYPE_MASK;
            if (type <= MarshallUtil.DOUBLE_TYPE) {
                serializeMarshallKey(inf, ind, wtn, c);
                wtn = true;
                serializeMarshallPrimitiveValue(reader, i, type, c);
                continue;
            }
            Object fieldValue = reader.getObject(i);
            if (fieldValue == null) {
                if (c.option().serializeNullInObjOrMap()) {
                    serializeMarshallKey(inf, ind, wtn, c);
                    wtn = true;
                    c.serializeNull();
                }
                continue;
            }
            serializeMarshallKey(inf, ind, wtn, c);
            wtn = true;
            JsonSerializeResult r = FUNC_TABLE[type].serialize(fieldValue, inf, indent, c);
            if (r == JsonSerializeResult.Continue) {
                continue;
            }
            written = true;
            index = i + 1;
            return r;
        }
        c.writeBuffer().writeByte((byte) '}');
        return JsonSerializeResult.Finished;
    }

    private JsonSerializeResult processArr(JsonSerializerContext c) {
        final WriteBuffer w = c.writeBuffer();
        final Object[] arr = (Object[]) this.val;
        final int ind = this.indent;
        final JsonSerializeFunc fn = this.func;
        boolean wtn = this.written;
        for (int i = index; i < arr.length; i++) {
            if (wtn) {
                w.writeByte((byte) ',');
            }
            wtn = true;
            c.serializeIndent(ind);
            Object instance = arr[i];
            if (instance == null) {
                c.serializeNull();
                continue;
            }
            JsonSerializeResult r = fn.serialize(instance, ind, c);
            if (r == JsonSerializeResult.Continue) {
                continue;
            }
            written = true;
            index = i + 1;
            return r;
        }
        w.writeByte((byte) ']');
        return JsonSerializeResult.Finished;
    }

    private JsonSerializeResult processCol(JsonSerializerContext c) {
        final WriteBuffer w = c.writeBuffer();
        final Iterator<?> iter = (Iterator<?>) this.val;
        final int ind = this.indent;
        final JsonSerializeFunc fn = this.func;
        final int range = this.size;
        boolean wtn = this.written;
        for (int i = index; i < range; i++) {
            if (wtn) {
                w.writeByte((byte) ',');
            }
            wtn = true;
            c.serializeIndent(ind);
            Object instance = iter.next();
            if (instance == null) {
                c.serializeNull();
                continue;
            }
            JsonSerializeResult r = fn.serialize(instance, ind, c);
            if (r == JsonSerializeResult.Continue) {
                continue;
            }
            written = true;
            index = i + 1;
            return r;
        }
        w.writeByte((byte) ']');
        return JsonSerializeResult.Finished;
    }

    private JsonSerializeResult processList(JsonSerializerContext c) {
        final WriteBuffer w = c.writeBuffer();
        final List<?> list = (List<?>) this.val;
        final int ind = this.indent;
        final JsonSerializeFunc fn = this.func;
        final int range = list.size();
        boolean wtn = this.written;
        for (int i = index; i < range; i++) {
            if (wtn) {
                w.writeByte((byte) ',');
            }
            wtn = true;
            c.serializeIndent(ind);
            Object instance = list.get(i);
            if (instance == null) {
                c.serializeNull();
                continue;
            }
            JsonSerializeResult r = fn.serialize(instance, ind, c);
            if (r == JsonSerializeResult.Continue) {
                continue;
            }
            written = true;
            index = i + 1;
            return r;
        }
        w.writeByte((byte) ']');
        return JsonSerializeResult.Finished;
    }

    @SuppressWarnings("unchecked")
    private JsonSerializeResult processMap(JsonSerializerContext c) {
        final Iterator<? extends Map.Entry<?, ?>> iter = (Iterator<? extends Map.Entry<?, ?>>) this.val;
        final int ind = this.indent;
        final JsonSerializeFunc fn = this.func;
        final int range = this.size;
        boolean wtn = this.written;
        for (int i = index; i < range; i++) {
            Map.Entry<?, ?> entry = iter.next();
            Object key = entry.getKey();
            if (key == null) {
                continue;
            }
            Object value = entry.getValue();
            if (value == null) {
                if (c.option().serializeNullInObjOrMap()) {
                    serializeMapKey(key, ind, wtn, c);
                    wtn = true;
                    c.serializeNull();
                }
                continue;
            }
            serializeMapKey(key, ind, wtn, c);
            wtn = true;
            JsonSerializeResult r = fn.serialize(value, ind, c);
            if (r == JsonSerializeResult.Continue) {
                continue;
            }
            written = true;
            index = i + 1;
            return r;
        }
        c.writeBuffer().writeByte((byte) '}');
        return JsonSerializeResult.Finished;
    }

    @FunctionalInterface
    interface JsonSerializerObjFunc {
        JsonSerializeResult serialize(Object fieldValue, MarshallInfo inf, int indent, JsonSerializerContext c);
    }

}
