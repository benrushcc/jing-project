package io.jingproject.marshall;

import java.util.function.Function;

public record MarshallFormulaInfo(Class<?> type, Function<Object, Object> fn) {

}
