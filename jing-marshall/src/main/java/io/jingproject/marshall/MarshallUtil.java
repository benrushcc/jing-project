package io.jingproject.marshall;

import io.jingproject.common.anno.ProcessorApi;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.*;
import java.util.*;
import java.util.function.Supplier;

@ProcessorApi
public final class MarshallUtil {
    // currently [0, 50] are used
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

    public static final int TYPE_SIZE = 64; // could expand up to 256 in the future
    public static final int TYPE_MASK = TYPE_SIZE - 1;
    // currently 4 bits used
    public static final byte FIELD_NAME_SIMPLE_MASK = 1;
    public static final byte MAPPED_NAME_SIMPLE_MASK = 1 << 1;
    public static final byte SKIP_SERIALIZATION_MASK = 1 << 2;
    public static final byte SKIP_DESERIALIZATION_MASK = 1 << 3;
    private static final Class<?>[] CLS_TABLE;
    // only interface in java.util are concerned
    private static final Map<Class<?>, Supplier<Map<?, ?>>> MAP_INTERFACE_FACTORY = Map.of(
            Map.class, HashMap::new,
            SequencedMap.class, LinkedHashMap::new,
            SortedMap.class, TreeMap::new,
            NavigableMap.class, TreeMap::new
    );
    // only common impl in java.util are concerned
    private static final Map<Class<?>, Supplier<Map<?, ?>>> MAP_IMPL_FACTORY = Map.of(
            HashMap.class, HashMap::new,
            LinkedHashMap.class, LinkedHashMap::new,
            TreeMap.class, TreeMap::new
    );
    // only interface in java.util are concerned
    private static final Map<Class<?>, Supplier<Collection<?>>> COL_INTERFACE_FACTORY = Map.of(
            Collection.class, ArrayList::new,
            List.class, ArrayList::new,
            Queue.class, ArrayDeque::new,
            Deque.class, ArrayDeque::new,
            Set.class, HashSet::new,
            SequencedCollection.class, ArrayList::new,
            SequencedSet.class, LinkedHashSet::new,
            SortedSet.class, TreeSet::new,
            NavigableSet.class, TreeSet::new
    );
    // only common impl in java.util are concerned
    private static final Map<Class<?>, Supplier<Collection<?>>> COL_IMPL_FACTORY = Map.of(
            ArrayList.class, ArrayList::new,
            LinkedList.class, LinkedList::new,
            ArrayDeque.class, ArrayDeque::new,
            PriorityQueue.class, PriorityQueue::new,
            HashSet.class, HashSet::new,
            LinkedHashSet.class, LinkedHashSet::new,
            TreeSet.class, TreeSet::new
    );

    static {
        CLS_TABLE = new Class<?>[TYPE_SIZE];
        CLS_TABLE[BYTE_TYPE] = byte.class;
        CLS_TABLE[BOOLEAN_TYPE] = boolean.class;
        CLS_TABLE[SHORT_TYPE] = short.class;
        CLS_TABLE[CHAR_TYPE] = char.class;
        CLS_TABLE[INT_TYPE] = int.class;
        CLS_TABLE[LONG_TYPE] = long.class;
        CLS_TABLE[FLOAT_TYPE] = float.class;
        CLS_TABLE[DOUBLE_TYPE] = double.class;

        CLS_TABLE[BYTE_WRAPPER_TYPE] = Byte.class;
        CLS_TABLE[BOOLEAN_WRAPPER_TYPE] = Boolean.class;
        CLS_TABLE[SHORT_WRAPPER_TYPE] = Short.class;
        CLS_TABLE[CHAR_WRAPPER_TYPE] = Character.class;
        CLS_TABLE[INT_WRAPPER_TYPE] = Integer.class;
        CLS_TABLE[LONG_WRAPPER_TYPE] = Long.class;
        CLS_TABLE[FLOAT_WRAPPER_TYPE] = Float.class;
        CLS_TABLE[DOUBLE_WRAPPER_TYPE] = Double.class;

        CLS_TABLE[BYTE_ARRAY_TYPE] = byte[].class;
        CLS_TABLE[BOOLEAN_ARRAY_TYPE] = boolean[].class;
        CLS_TABLE[SHORT_ARRAY_TYPE] = short[].class;
        CLS_TABLE[CHAR_ARRAY_TYPE] = char[].class;
        CLS_TABLE[INT_ARRAY_TYPE] = int[].class;
        CLS_TABLE[LONG_ARRAY_TYPE] = long[].class;
        CLS_TABLE[FLOAT_ARRAY_TYPE] = float[].class;
        CLS_TABLE[DOUBLE_ARRAY_TYPE] = double[].class;

        CLS_TABLE[BYTE_WRAPPER_ARRAY_TYPE] = Byte[].class;
        CLS_TABLE[BOOLEAN_WRAPPER_ARRAY_TYPE] = Boolean[].class;
        CLS_TABLE[SHORT_WRAPPER_ARRAY_TYPE] = Short[].class;
        CLS_TABLE[CHAR_WRAPPER_ARRAY_TYPE] = Character[].class;
        CLS_TABLE[INT_WRAPPER_ARRAY_TYPE] = Integer[].class;
        CLS_TABLE[LONG_WRAPPER_ARRAY_TYPE] = Long[].class;
        CLS_TABLE[FLOAT_WRAPPER_ARRAY_TYPE] = Float[].class;
        CLS_TABLE[DOUBLE_WRAPPER_ARRAY_TYPE] = Double[].class;

        CLS_TABLE[CHARSEQUENCE_TYPE] = CharSequence.class;
        CLS_TABLE[STRING_TYPE] = String.class;
        CLS_TABLE[UUID_TYPE] = UUID.class;
        CLS_TABLE[BIG_INTEGER_TYPE] = BigInteger.class;
        CLS_TABLE[BIG_DECIMAL_TYPE] = BigDecimal.class;
        CLS_TABLE[LOCAL_DATE_TYPE] = LocalDate.class;
        CLS_TABLE[LOCAL_TIME_TYPE] = LocalTime.class;
        CLS_TABLE[LOCAL_DATE_TIME_TYPE] = LocalDateTime.class;
        CLS_TABLE[OFFSET_TIME_TYPE] = OffsetTime.class;
        CLS_TABLE[OFFSET_DATE_TIME_TYPE] = OffsetDateTime.class;
    }


    private MarshallUtil() {
        throw new UnsupportedOperationException("utility class");
    }

    private static int scan(Class<?> target, int start, int end) {
        for (int i = start; i <= end; i++) {
            Class<?> c = CLS_TABLE[i];
            if (c != null && target == c) {
                return i;
            }
        }
        return -1;
    }

    // checks if all bytes are alphanumeric, underscore, or hyphen.
    private static boolean isAlphanumericOrSeparator(byte[] utf8Bytes) {
        for (byte b : utf8Bytes) {
            if (b >= (byte) '0' && b <= (byte) '9') {
                continue;
            }
            if (b >= (byte) 'a' && b <= (byte) 'z') {
                continue;
            }
            if (b >= (byte) 'A' && b <= (byte) 'Z') {
                continue;
            }
            if (b == (byte) '_' || b == (byte) '-') {
                continue;
            }
            return false;
        }
        return true;
    }

    public static boolean isMapInterfaceType(Class<?> rawType) {

        return MAP_INTERFACE_FACTORY.containsKey(rawType);
    }

    public static Map<?, ?> newMapInterface(Class<?> rawType) {

        return MAP_INTERFACE_FACTORY.get(rawType).get();
    }

    public static boolean isMapImplType(Class<?> rawType) {

        return MAP_IMPL_FACTORY.containsKey(rawType);
    }

    public static Map<?, ?> newMapImpl(Class<?> rawType) {

        return MAP_IMPL_FACTORY.get(rawType).get();
    }

    public static boolean isCollectionInterfaceType(Class<?> rawType) {

        return COL_INTERFACE_FACTORY.containsKey(rawType);
    }

    public static Collection<?> newCollectionInterface(Class<?> rawType) {

        return COL_INTERFACE_FACTORY.get(rawType).get();
    }

    public static boolean isCollectionImplType(Class<?> rawType) {

        return COL_IMPL_FACTORY.containsKey(rawType);
    }

    public static Collection<?> newCollectionImpl(Class<?> rawType) {

        return COL_IMPL_FACTORY.get(rawType).get();
    }

    // returns the flag constant for a primitive type.
    private static int makePrimitiveType(Class<?> rawType) {

        int r = scan(rawType, BYTE_TYPE, DOUBLE_TYPE);
        if (r < 0) {
            throw new AssertionError();
        }
        return r;
    }

    // returns the flag constant for a wrapper type.
    private static int makeRawType(Class<?> rawType) {

        int r = scan(rawType, BYTE_WRAPPER_TYPE, DOUBLE_WRAPPER_TYPE);
        if (r < 0) {
            r = scan(rawType, CHARSEQUENCE_TYPE, OFFSET_DATE_TIME_TYPE);
            if (r < 0) {
                return RAW_TYPE;
            }
        }
        return r;
    }

    // returns the flag constant for a primitive array type.
    private static int makePrimitiveArrayType(Class<?> rawType) {

        int r = scan(rawType, BYTE_ARRAY_TYPE, DOUBLE_ARRAY_TYPE);
        if (r < 0) {
            throw new AssertionError();
        }
        return r;
    }

    // returns the flag constant for a wrapper array type.
    private static int makeWrapperArrayType(Class<?> rawType) {

        int r = scan(rawType, BYTE_WRAPPER_ARRAY_TYPE, DOUBLE_WRAPPER_ARRAY_TYPE);
        if (r < 0) {
            return ARRAY_TYPE;
        }
        return r;
    }

    public static byte makeType(Class<?> rawType, Class<?> firstGenericType, Class<?> secondGenericType) {

        int r;
        if (secondGenericType != null) {
            if (isMapInterfaceType(rawType)) {
                r = MAP_INTERFACE_TYPE;
            } else if (isMapImplType(rawType)) {
                r = MAP_IMPL_TYPE;
            } else {
                r = TWO_GENERIC_TYPE;
            }
        } else if (firstGenericType != null) {
            if (isCollectionInterfaceType(rawType)) {
                r = COLLECTION_INTERFACE_TYPE;
            } else if (isCollectionImplType(rawType)) {
                r = COLLECTION_IMPL_TYPE;
            } else {
                r = ONE_GENERIC_TYPE;
            }
        } else if (rawType.isEnum()) {
            r = ENUM_TYPE;
        } else if (rawType.isPrimitive()) {
            r = makePrimitiveType(rawType);
        } else if (rawType.isArray()) {
            r = rawType.getComponentType().isPrimitive() ? makePrimitiveArrayType(rawType) : makeWrapperArrayType(rawType);
        } else {
            r = makeRawType(rawType);
        }
        return (byte) r;
    }

    public static byte makeFlags(byte[] fieldNameUtf8Bytes, byte[] mappedNameUtf8Bytes, boolean skipSerializing, boolean skipDeserializing) {

        byte r = 0;
        if (isAlphanumericOrSeparator(fieldNameUtf8Bytes)) {
            r |= FIELD_NAME_SIMPLE_MASK;
        }
        if (isAlphanumericOrSeparator(mappedNameUtf8Bytes)) {
            r |= MAPPED_NAME_SIMPLE_MASK;
        }
        if (skipSerializing) {
            r |= SKIP_SERIALIZATION_MASK;
        }
        if (skipDeserializing) {
            r |= SKIP_DESERIALIZATION_MASK;
        }
        return r;
    }
}
