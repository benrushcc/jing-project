package io.jingproject.marshall;

import io.jingproject.common.Utils;
import io.jingproject.common.anno.ProcessorApi;

import java.util.Objects;

@ProcessorApi
public interface MarshallTransformerFacade {
    Class<?> customType();

    Class<?> builtinType();

    Object toCustom(Object o);

    Object toBuiltin(Object o);

    default Object[] toBuiltinArray(Object[] o) {
        Objects.requireNonNull(o, "custom array is null");
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
        Objects.requireNonNull(o, "builtin array is null");
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
