package io.jingproject.marshallcbortest.entity;

import io.jingproject.marshall.Marshallable;
import io.jingproject.marshallcbor.CborValue;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Marshallable
public record RecordEntity (
        int intValue,
        Long longValue,
        String stringValue,
        EnumEntity enumValue,
        String[] stringArray,
        List<CborValue> cborValueList,
        Map<String, RecordEntity> beanEntityMap
) {
    @Override
    public boolean equals(Object o) {
        return o instanceof RecordEntity(int value, Long longValue1, String stringValue1, EnumEntity enumValue1,
                String[] array, List<CborValue> valueList, Map<String, RecordEntity> entityMap) &&
                intValue == value &&
                Objects.equals(longValue, longValue1) &&
                Objects.equals(stringValue, stringValue1) &&
                enumValue == enumValue1 &&
                Arrays.equals(stringArray, array) &&
                Objects.equals(cborValueList, valueList) &&
                Objects.equals(beanEntityMap, entityMap);
    }

    @Override
    public int hashCode() {
        return Objects.hash(intValue, longValue, stringValue, enumValue,
                Arrays.hashCode(stringArray), cborValueList, beanEntityMap);
    }
}
