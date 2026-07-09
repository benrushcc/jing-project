package io.jingproject.marshall;

import io.jingproject.common.anno.ProcessorApi;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.*;
import java.util.*;

@ProcessorApi
public final class MarshallUtil {
    // currently 50 + 1 = 51 bits used
    public static final int BYTE_TYPE = 0;
    public static final int BOOLEAN_TYPE = 1;
    public static final int SHORT_TYPE = 2;
    public static final int CHAR_TYPE = 3;
    public static final int INT_TYPE = 4;
    public static final int LONG_TYPE = 5;
    public static final int FLOAT_TYPE = 6;
    public static final int DOUBLE_TYPE = 7;
    public static final int BYTE_WRAPPER_TYPE = 8;
    public static final int BOOLEAN_WRAPPER_TYPE = 9;
    public static final int SHORT_WRAPPER_TYPE = 10;
    public static final int CHAR_WRAPPER_TYPE = 11;
    public static final int INT_WRAPPER_TYPE = 12;
    public static final int LONG_WRAPPER_TYPE = 13;
    public static final int FLOAT_WRAPPER_TYPE = 14;
    public static final int DOUBLE_WRAPPER_TYPE = 15;

    public static final int ARRAY_OFFSET = 16;
    public static final int BYTE_ARRAY_TYPE = BYTE_TYPE + ARRAY_OFFSET;
    public static final int BOOLEAN_ARRAY_TYPE = BOOLEAN_TYPE + ARRAY_OFFSET;
    public static final int SHORT_ARRAY_TYPE = SHORT_TYPE + ARRAY_OFFSET;
    public static final int CHAR_ARRAY_TYPE = CHAR_TYPE + ARRAY_OFFSET;
    public static final int INT_ARRAY_TYPE = INT_TYPE + ARRAY_OFFSET;
    public static final int LONG_ARRAY_TYPE = LONG_TYPE + ARRAY_OFFSET;
    public static final int FLOAT_ARRAY_TYPE = FLOAT_TYPE + ARRAY_OFFSET;
    public static final int DOUBLE_ARRAY_TYPE = DOUBLE_TYPE + ARRAY_OFFSET;
    public static final int BYTE_WRAPPER_ARRAY_TYPE = BYTE_WRAPPER_TYPE + ARRAY_OFFSET;
    public static final int BOOLEAN_WRAPPER_ARRAY_TYPE = BOOLEAN_WRAPPER_TYPE + ARRAY_OFFSET;
    public static final int SHORT_WRAPPER_ARRAY_TYPE = SHORT_WRAPPER_TYPE + ARRAY_OFFSET;
    public static final int CHAR_WRAPPER_ARRAY_TYPE = CHAR_WRAPPER_TYPE + ARRAY_OFFSET;
    public static final int INT_WRAPPER_ARRAY_TYPE = INT_WRAPPER_TYPE + ARRAY_OFFSET;
    public static final int LONG_WRAPPER_ARRAY_TYPE = LONG_WRAPPER_TYPE + ARRAY_OFFSET;
    public static final int FLOAT_WRAPPER_ARRAY_TYPE = FLOAT_WRAPPER_TYPE + ARRAY_OFFSET;
    public static final int DOUBLE_WRAPPER_ARRAY_TYPE = DOUBLE_WRAPPER_TYPE + ARRAY_OFFSET;

    public static final int RAW_TYPE = 32;
    public static final int ARRAY_TYPE = 33;
    public static final int ENUM_TYPE = 34;
    public static final int CHARSEQUENCE_TYPE = 35;
    public static final int STRING_TYPE = 36;
    public static final int UUID_TYPE = 37;
    public static final int BIG_INTEGER_TYPE = 38;
    public static final int BIG_DECIMAL_TYPE = 39;
    public static final int LOCAL_DATE_TYPE = 40;
    public static final int LOCAL_TIME_TYPE = 41;
    public static final int LOCAL_DATE_TIME_TYPE = 42;
    public static final int OFFSET_TIME_TYPE = 43;
    public static final int OFFSET_DATE_TIME_TYPE = 44;
    public static final int ONE_GENERIC_TYPE = 45;
    public static final int COLLECTION_INTERFACE_TYPE = 46;
    public static final int COLLECTION_IMPL_TYPE = 47;
    public static final int TWO_GENERIC_TYPE = 48;
    public static final int MAP_INTERFACE_TYPE = 49;
    public static final int MAP_IMPL_TYPE = 50;

    private MarshallUtil() {
        throw new UnsupportedOperationException("utility class");
    }

    // currently 4 bits used
    public static final long FIELD_NAME_SIMPLE_MASK = 1L;
    public static final long MAPPED_NAME_SIMPLE_MASK = 1L << 1;
    public static final long SKIP_SERIALIZATION_MASK = 1L << 2;
    public static final long SKIP_DESERIALIZATION_MASK = 1L << 3;

    // checks if all bytes are alphanumeric, underscore, or hyphen.
    private static boolean isAlphanumericOrSeparator(byte[] utf8Bytes) {
        for (byte b : utf8Bytes) {
            if(b >= (byte) '0' &&  b <= (byte) '9') {
                continue ;
            }
            if(b >= (byte) 'a' && b <= (byte) 'z') {
                continue ;
            }
            if(b >= (byte) 'A' &&  b <= (byte) 'Z') {
                continue ;
            }
            if(b == (byte) '_' || b == (byte) '-') {
                continue ;
            }
            return false;
        }
        return true;
    }

    // only interface in java.util are concerned
    private static boolean isMapIntefaceType(Class<?> rawType) {
        assert rawType != null;
        if(!rawType.isInterface()) {
            return false;
        }
        return rawType == Map.class || rawType == SequencedMap.class || rawType == SortedMap.class || rawType == NavigableMap.class;
    }

    // only common impl in java.util are concerned
    private static boolean isMapImplType(Class<?> rawType) {
        assert rawType != null;
        return rawType == HashMap.class || rawType == LinkedHashMap.class || rawType == TreeMap.class;
    }

    // only interface in java.util are concerned
    private static boolean isCollectionInterfaceType(Class<?> rawType) {
        assert rawType != null;
        if(!rawType.isInterface()) {
            return false;
        }
        return rawType == Collection.class || rawType == List.class || rawType == Queue.class || rawType == Deque.class || rawType == Set.class
                || rawType == SequencedCollection.class || rawType == SequencedSet.class || rawType == SortedSet.class || rawType == NavigableSet.class;
    }

    // only common impl in java.util are concerned
    private static boolean isCollectionImplType(Class<?> rawType) {
        assert rawType != null;
        return rawType == ArrayList.class || rawType == LinkedList.class || rawType == ArrayDeque.class ||
                rawType == PriorityQueue.class || rawType == HashSet.class || rawType == LinkedHashSet.class || rawType == TreeSet.class;
    }

    // returns the flag constant for a primitive type.
    private static int makePrimitiveFlag(Class<?> rawType) {
        assert rawType != null && rawType.isPrimitive();
        if(rawType == byte.class) {
            return BYTE_TYPE;
        } else if(rawType == boolean.class) {
            return BOOLEAN_TYPE;
        }else if(rawType == short.class) {
            return SHORT_TYPE;
        } else if(rawType == char.class) {
            return CHAR_TYPE;
        } else if(rawType == int.class) {
            return INT_TYPE;
        } else if(rawType == long.class) {
            return LONG_TYPE;
        } else if(rawType == float.class) {
            return FLOAT_TYPE;
        } else if(rawType == double.class) {
            return DOUBLE_TYPE;
        } else {
            throw new AssertionError();
        }
    }

    // returns the flag constant for a wrapper type.
    private static int makeRawFlag(Class<?> rawType) {
        assert rawType != null;
        if(rawType == Byte.class) {
            return BYTE_WRAPPER_TYPE;
        } else if(rawType == Boolean.class) {
            return BOOLEAN_WRAPPER_TYPE;
        } else if(rawType == Short.class) {
            return SHORT_WRAPPER_TYPE;
        } else if(rawType == Character.class) {
            return CHAR_WRAPPER_TYPE;
        } else if(rawType == Integer.class) {
            return INT_WRAPPER_TYPE;
        } else if(rawType == Long.class) {
            return LONG_WRAPPER_TYPE;
        } else if(rawType == Float.class) {
            return FLOAT_WRAPPER_TYPE;
        } else if(rawType == Double.class) {
            return DOUBLE_WRAPPER_TYPE;
        } else if(rawType == CharSequence.class){
            return CHARSEQUENCE_TYPE;
        } else if(rawType == String.class) {
            return STRING_TYPE;
        } else if(rawType == UUID.class) {
            return UUID_TYPE;
        } else if(rawType == BigInteger.class) {
            return BIG_INTEGER_TYPE;
        } else if(rawType == BigDecimal.class) {
            return BIG_DECIMAL_TYPE;
        } else if(rawType == LocalDate.class) {
            return LOCAL_DATE_TYPE;
        } else if(rawType == LocalTime.class) {
            return LOCAL_TIME_TYPE;
        } else if(rawType == LocalDateTime.class) {
            return LOCAL_DATE_TIME_TYPE;
        } else if(rawType == OffsetTime.class) {
            return OFFSET_TIME_TYPE;
        } else if (rawType == OffsetDateTime.class) {
            return OFFSET_DATE_TIME_TYPE;
        } else {
            return RAW_TYPE;
        }
    }

    // returns the flag constant for a primitive array type.
    private static int makePrimitiveArrayFlag(Class<?> rawType) {
        assert rawType != null && rawType.isArray() && rawType.getComponentType().isPrimitive();
        if(rawType == byte[].class) {
            return BYTE_ARRAY_TYPE;
        } else if(rawType == boolean[].class) {
            return BOOLEAN_ARRAY_TYPE;
        } else if(rawType == short[].class) {
            return SHORT_ARRAY_TYPE;
        }  else if(rawType == char[].class) {
            return CHAR_ARRAY_TYPE;
        } else if(rawType == int[].class) {
            return INT_ARRAY_TYPE;
        } else if(rawType == long[].class) {
            return LONG_ARRAY_TYPE;
        } else if(rawType == float[].class) {
            return FLOAT_ARRAY_TYPE;
        } else if(rawType == double[].class) {
            return DOUBLE_ARRAY_TYPE;
        } else {
            throw new AssertionError();
        }
    }

    // returns the flag constant for a wrapper array type.
    private static int makeWrapperArrayFlag(Class<?> rawType) {
        assert rawType != null && rawType.isArray();
        if(rawType == Byte[].class) {
            return BYTE_WRAPPER_ARRAY_TYPE;
        } else if(rawType == Boolean[].class) {
            return BOOLEAN_WRAPPER_ARRAY_TYPE;
        } else if(rawType == Short[].class) {
            return SHORT_WRAPPER_ARRAY_TYPE;
        } else if(rawType == Character[].class) {
            return CHAR_WRAPPER_ARRAY_TYPE;
        } else if(rawType == Integer[].class) {
            return INT_WRAPPER_ARRAY_TYPE;
        } else if(rawType == Long[].class) {
            return LONG_WRAPPER_ARRAY_TYPE;
        } else if(rawType == Float[].class) {
            return FLOAT_WRAPPER_ARRAY_TYPE;
        } else if(rawType == Double[].class) {
            return DOUBLE_WRAPPER_ARRAY_TYPE;
        } else {
            return ARRAY_TYPE;
        }
    }

    // calculate the shift based on its type
    public static int makeShift(Class<?> rawType, Class<?> firstGenericType, Class<?> secondGenericType) {
        if(secondGenericType != null) {
            if(isMapIntefaceType(rawType)) {
                return MAP_INTERFACE_TYPE;
            }
            if(isMapImplType(rawType)) {
                return MAP_IMPL_TYPE;
            }
            return TWO_GENERIC_TYPE;
        }
        if(firstGenericType != null) {
            if(isCollectionInterfaceType(rawType)) {
                return COLLECTION_INTERFACE_TYPE;
            }
            if(isCollectionImplType(rawType)) {
                return COLLECTION_IMPL_TYPE;
            }
            return ONE_GENERIC_TYPE;
        }
        if(rawType.isEnum()) {
            return ENUM_TYPE;
        }
        if(rawType.isPrimitive()) {
            return makePrimitiveFlag(rawType);
        }
        if(rawType.isArray()) {
            Class<?> componentType = rawType.getComponentType();
            if(componentType.isPrimitive()) {
                return makePrimitiveArrayFlag(rawType);
            } else {
                return makeWrapperArrayFlag(rawType);
            }
        }
        return makeRawFlag(rawType);
    }

    public static long makeFlags(Class<?> rawType, Class<?> firstGenericType, Class<?> secondGenericType,
                                 byte[] fieldNameUtf8Bytes, byte[] mappedNameUtf8Bytes, boolean skipSerializing, boolean skipDeserializing) {
        // raw type must not be null
        if(rawType == null) {
            throw new IllegalArgumentException("rawType cannot be null");
        }
        // fieldNameUtf8Bytes must not be null
        if(fieldNameUtf8Bytes == null || fieldNameUtf8Bytes.length == 0) {
            throw new IllegalArgumentException("fieldNameUtf8Bytes cannot be null or blank");
        }
        // mappedNameUtf8Bytes must not be null
        if(mappedNameUtf8Bytes == null || mappedNameUtf8Bytes.length == 0) {
            throw new IllegalArgumentException("mappedNameUtf8Bytes cannot be null or blank");
        }
        // flags are made of shifts and masks
        long r = 1L << (Long.SIZE - 1 - makeShift(rawType, firstGenericType, secondGenericType));
        if(isAlphanumericOrSeparator(fieldNameUtf8Bytes)) {
            r |= FIELD_NAME_SIMPLE_MASK;
        }
        if(isAlphanumericOrSeparator(mappedNameUtf8Bytes)) {
            r |= MAPPED_NAME_SIMPLE_MASK;
        }
        if(skipSerializing) {
            r |= SKIP_SERIALIZATION_MASK;
        }
        if(skipDeserializing) {
            r |= SKIP_DESERIALIZATION_MASK;
        }
        return r;
    }
}
