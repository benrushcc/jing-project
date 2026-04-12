package io.jingproject.marshall;

import io.jingproject.common.anno.ProcessorApi;

import java.lang.invoke.VarHandle;

@ProcessorApi
public record MarshallInfo(
    Class<?> type,
    int index,
    String fieldName,
    String mappedName,
    VarHandle vh,
    Enum<?> enumValue,
    boolean skipSerializing,
    boolean skipDeserializing
) {

}
