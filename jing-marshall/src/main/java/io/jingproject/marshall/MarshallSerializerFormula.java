package io.jingproject.marshall;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class MarshallSerializerFormula {
    private static final MarshallSerializerFormula EMPTY_FORMULA = new MarshallSerializerFormula(Map.of());
    private final Map<Class<?>, FormulaTransformer> formulaTransformerMap;

    private MarshallSerializerFormula(Map<Class<?>, FormulaTransformer> formulaTransformerMap) {
        this.formulaTransformerMap = formulaTransformerMap;
    }

    record FormulaTransformer(Class<?> transformedType, Function<Object, Object> transformFunction) {

    }

    public static MarshallSerializerFormula of(List<Class<?>> builtinTypes, List<Class<?>> transformerTypes) {
        if(builtinTypes == null || builtinTypes.isEmpty()) {
            throw new IllegalArgumentException("builtinTypes cannot be null or empty");
        }
        if(transformerTypes == null) {
            throw new IllegalArgumentException("transformerTypes cannot be null");
        }
        if(transformerTypes.isEmpty()) {
            return EMPTY_FORMULA;
        }
        Map<Class<?>, FormulaTransformer> r = new HashMap<>();
        for (Class<?> transformerType : transformerTypes) {
            MarshallTransformerFacade transformerFacade = Marshalls.getMarshallTransformerFacade(transformerType);
            if(transformerFacade == null) {
                throw new IllegalArgumentException("transformer facade not found : " + transformerType.getName());
            }
            Class<?> builtinType = transformerFacade.builtinType();
            if(!builtinTypes.contains(builtinType)) {
                throw new IllegalArgumentException("builtinType not supported : " + builtinType.getName());
            }
            if(r.putIfAbsent(transformerFacade.customType(), new FormulaTransformer(builtinType, transformerFacade::toBuiltin)) !=  null) {
                throw new IllegalArgumentException("builtinType already exist : " + builtinType.getName());
            }
        }
        return new MarshallSerializerFormula(Map.copyOf(r));
    }
}
