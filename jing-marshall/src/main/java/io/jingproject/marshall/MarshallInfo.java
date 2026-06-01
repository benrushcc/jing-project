package io.jingproject.marshall;

import io.jingproject.common.anno.ProcessorApi;

import java.lang.invoke.VarHandle;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;

@ProcessorApi
public record MarshallInfo(
        Class<?> rawType,
        Class<?> firstGenericType,
        Class<?> secondGenericType,
        int index,
        String fieldName,
        String mappedName,
        Enum<?> enumValue,
        int flags
) {

    public static final int BYTE_TYPE = 0;
    public static final int BOOLEAN_TYPE = 1;
    public static final int SHORT_TYPE = 2;
    public static final int CHAR_TYPE = 3;
    public static final int INT_TYPE = 4;
    public static final int LONG_TYPE = 5;
    public static final int FLOAT_TYPE = 6;
    public static final int DOUBLE_TYPE = 7;
    public static final int CHARSEQUENCE_TYPE = 8;
    public static final int STRING_TYPE = 9;
    public static final int COLLECTION_TYPE = 10;
    public static final int MAP_TYPE = 11;
    public static final int RAW_TYPE = 12;
    public static final int ONE_GENERIC_TYPE = 13;
    public static final int TWO_GENERIC_TYPE = 14;

    public static final int FIELD_NAME_ASCII_MASK = 1;
    public static final int MAPPED_NAME_ASCII_MASK = 1 << 1;
    public static final int SKIP_SERIALIZATION_MASK = 1 << 2;
    public static final int SKIP_DESERIALIZATION_MASK = 1 << 3;


    public static boolean isPrimitive(int leadingZeros) {
        return leadingZeros <= DOUBLE_TYPE;
    }

    public static boolean isIntegral(int leadingZeros) {
        return leadingZeros <= LONG_TYPE;
    }

    private static boolean isCompletelyDigitOrLetter(String str) {
        for (byte b : str.getBytes(StandardCharsets.UTF_8)) {
            if(b >= MarshallUtil.BYTE_ZERO &&  b <= MarshallUtil.BYTE_NINE) {
                continue ;
            }
            if(b >= MarshallUtil.BYTE_a && b <= MarshallUtil.BYTE_z) {
                continue ;
            }
            if(b >= MarshallUtil.BYTE_A &&  b <= MarshallUtil.BYTE_Z) {
                continue ;
            }
            return false;
        }
        return true;
    }

    private static int makeTypeFlags(Class<?> rawType, Class<?> firstGenericType, Class<?> secondGenericType) {
        int shift;
        if(secondGenericType != null) {
            shift = Map.class.isAssignableFrom(rawType) ? MAP_TYPE : TWO_GENERIC_TYPE;
        } else if(firstGenericType != null) {
            shift = Collection.class.isAssignableFrom(rawType) ? COLLECTION_TYPE : ONE_GENERIC_TYPE;
        } else if(rawType == String.class) {
            shift = STRING_TYPE;
        } else if(CharSequence.class.isAssignableFrom(rawType)) {
            shift = CHARSEQUENCE_TYPE;
        } else if(rawType == byte.class || rawType == Byte.class) {
            shift = BYTE_TYPE;
        } else if(rawType == boolean.class || rawType == Boolean.class) {
            shift = BOOLEAN_TYPE;
        } else if (rawType == short.class || rawType == Short.class) {
            shift = SHORT_TYPE;
        } else if (rawType == char.class || rawType == Character.class) {
            shift = CHAR_TYPE;
        } else if (rawType == int.class || rawType == Integer.class) {
            shift = INT_TYPE;
        } else if (rawType == long.class || rawType == Long.class) {
            shift = LONG_TYPE;
        } else if (rawType == float.class || rawType == Float.class) {
            shift = FLOAT_TYPE;
        } else if (rawType == double.class || rawType == Double.class) {
            shift = DOUBLE_TYPE;
        } else {
            shift = RAW_TYPE;
        }
        return 1 << (Integer.SIZE - 1 - shift);
    }

    private static int makeFlags(Class<?> rawType, Class<?> firstGenericType, Class<?> secondGenericType, String fieldName, String mappedName, boolean skipSerializing, boolean skipDeserializing) {
        if(rawType == null) {
            throw new IllegalArgumentException("rawType cannot be null");
        }
        int r = makeTypeFlags(rawType, firstGenericType, secondGenericType);
        if(isCompletelyDigitOrLetter(fieldName)) {
            r |= FIELD_NAME_ASCII_MASK;
        }
        if(isCompletelyDigitOrLetter(mappedName)) {
            r |= MAPPED_NAME_ASCII_MASK;
        }
        if(skipSerializing) {
            r |= SKIP_SERIALIZATION_MASK;
        }
        if(skipDeserializing) {
            r |= SKIP_DESERIALIZATION_MASK;
        }
        return r;
    }

    public MarshallInfo(Class<?> rawType,
                        Class<?> firstGenericType,
                        Class<?> secondGenericType,
                        int index,
                        String fieldName,
                        String mappedName,
                        Enum<?> enumValue,
                        boolean skipSerializing,
                        boolean skipDeserializing) {
        int flags = makeFlags(rawType, firstGenericType, secondGenericType, fieldName, mappedName, skipSerializing, skipDeserializing);
        this(rawType, firstGenericType, secondGenericType, index, fieldName, mappedName, enumValue, flags);
    }

    public boolean fieldNamePureAscii() {
        return (flags & FIELD_NAME_ASCII_MASK) != 0;
    }

    public boolean mappedNamePureAscii() {
        return (flags & MAPPED_NAME_ASCII_MASK) != 0;
    }

    public boolean skipSerializing() {
        return (flags & SKIP_SERIALIZATION_MASK) != 0;
    }

    public boolean skipDeserializing() {
        return (flags & SKIP_DESERIALIZATION_MASK) != 0;
    }
}
