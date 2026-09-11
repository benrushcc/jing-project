package io.jingproject.marshall;

import io.jingproject.common.Utils;
import io.jingproject.common.anno.ProcessorApi;

@ProcessorApi
public interface MarshallTransformerFacade {

    Class<?> transformerType();

    Class<?> customType();

    Class<?> builtinType();

    Object toCustom(Object o);

    Object toBuiltin(Object o);

    default Object[] toBuiltinArray(Object[] o) {
        if(o == null) {
            throw new IllegalArgumentException("custom array must not be null");
        }
        if(o.length == 0) {
            return Utils.emptyObjectArray();
        }
        Object[] r = new Object[o.length];
        for(int i = 0; i < o.length; i++) {
            r[i] = toBuiltin(o[i]);
        }
        return r;
    }

    default Object[] toCustomArray(Object[] o) {
        if(o == null) {
            throw new IllegalArgumentException("builtin array must not be null");
        }
        if(o.length == 0) {
            return Utils.emptyObjectArray();
        }
        Object[] r = new Object[o.length];
        for(int i = 0; i < o.length; i++) {
            r[i] = toCustom(o[i]);
        }
        return r;
    }
}
