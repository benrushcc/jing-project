package io.jingproject.marshall;

import io.jingproject.common.anno.ProcessorApi;

import java.lang.invoke.VarHandle;
import java.nio.charset.StandardCharsets;

@ProcessorApi
public record MarshallInfo(
        Class<?> rawType,
        Class<?> firstGenericType,
        Class<?> secondGenericType,
        int index,
        String fieldName,
        String mappedName,
        VarHandle vh,
        Enum<?> enumValue,
        boolean fieldNamePureAscii,
        boolean mappedNamePureAscii,
        boolean skipSerializing,
        boolean skipDeserializing
) {

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

    public MarshallInfo(Class<?> rawType,
                        Class<?> firstGenericType,
                        Class<?> secondGenericType,
                        int index,
                        String fieldName,
                        String mappedName,
                        VarHandle vh,
                        Enum<?> enumValue,
                        boolean skipSerializing,
                        boolean skipDeserializing) {
        this(rawType, firstGenericType, secondGenericType, index, fieldName, mappedName, vh, enumValue,
                isCompletelyDigitOrLetter(fieldName), isCompletelyDigitOrLetter(mappedName), skipSerializing, skipDeserializing);
    }
}
