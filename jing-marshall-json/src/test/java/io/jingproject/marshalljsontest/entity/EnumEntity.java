package io.jingproject.marshalljsontest.entity;

import io.jingproject.marshall.MarshallAttr;
import io.jingproject.marshall.Marshallable;

@Marshallable
public enum EnumEntity {
    ENUM_ENTITY1,
    ENUM_ENTITY2,
    @MarshallAttr(mappedName = "\"ENUM_ESCAPE\"")
    ENUM_ENTITY3,
    ENUM_ENTITY4,
}
