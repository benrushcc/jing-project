package io.jingproject.marshall;

public record MarshallInfo(
    Class<?> type,
    String strName,
    byte[] utf8Name,
    int index,
    int offset
) {
}
