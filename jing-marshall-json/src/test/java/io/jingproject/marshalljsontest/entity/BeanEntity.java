package io.jingproject.marshalljsontest.entity;

import io.jingproject.marshall.Marshallable;
import io.jingproject.marshalljson.JsonPrimitiveType;

import java.util.List;
import java.util.Map;

@Marshallable
public final class BeanEntity {
    private int intValue;
    private Long longValue;
    private String stringValue;
    private EnumEntity enumValue;
    private String[] stringArray;
    private List<JsonPrimitiveType> jsonPrimitiveTypeList;
    private Map<String, BeanEntity> beanEntityMap;

    public int intValue() {
        return intValue;
    }

    public void setIntValue(int intValue) {
        this.intValue = intValue;
    }

    public Long longValue() {
        return longValue;
    }

    public void setLongValue(Long longValue) {
        this.longValue = longValue;
    }

    public String stringValue() {
        return stringValue;
    }

    public void setStringValue(String stringValue) {
        this.stringValue = stringValue;
    }

    public EnumEntity enumValue() {
        return enumValue;
    }

    public void setEnumValue(EnumEntity enumValue) {
        this.enumValue = enumValue;
    }

    public String[] stringArray() {
        return stringArray;
    }

    public void setStringArray(String[] stringArray) {
        this.stringArray = stringArray;
    }

    public List<JsonPrimitiveType> jsonPrimitiveTypeList() {
        return jsonPrimitiveTypeList;
    }

    public void setJsonPrimitiveTypeList(List<JsonPrimitiveType> jsonPrimitiveTypeList) {
        this.jsonPrimitiveTypeList = jsonPrimitiveTypeList;
    }

    public Map<String, BeanEntity> beanEntityMap() {
        return beanEntityMap;
    }

    public void setBeanEntityMap(Map<String, BeanEntity> beanEntityMap) {
        this.beanEntityMap = beanEntityMap;
    }
}
