package io.jingproject.marshall;

import io.jingproject.common.Utils;

public final class MarshallFactory {
    private static final int DEFAULT_OBJECT_CAPACITY = 64;
    private static final int DEFAULT_PRIMITIVE_CAPACITY = 256;
    private Object[] objectBuffer;
    private byte[] primitiveBuffer;
    private int objectIndex = 0;
    private int primitiveIndex = 0;

    public MarshallFactory(int objectCapacity, int primitiveCapacity) {
        if(objectCapacity < Utils.minCapacity() || objectCapacity > Utils.maxCapacity()) {
            throw new IllegalArgumentException("objectCapacity out of range");
        }
        if(primitiveCapacity < Utils.minCapacity() || primitiveCapacity > Utils.maxCapacity()) {
            throw new IllegalArgumentException("primitiveCapacity out of range");
        }
        this.objectBuffer = new Object[objectCapacity];
        this.primitiveBuffer = new byte[primitiveCapacity];
    }

    public MarshallFactory() {
        this(DEFAULT_OBJECT_CAPACITY, DEFAULT_PRIMITIVE_CAPACITY);
    }

    public MarshallSchema newSchema(MarshallFacade marshallFacade) {
        int objectElements = marshallFacade.objectElements();
        int primitiveElements = marshallFacade.primitiveElements();
        Object[] schemaObjectArray = Utils.emptyObjectArray();
        int schemaObjectIndex = 0;
        if(objectElements > 0) {
            int newObjectIndex = Math.addExact(objectElements, objectIndex);
            if(newObjectIndex > objectBuffer.length) {
                objectBuffer = schemaObjectArray = new Object[objectBuffer.length];
                objectIndex = objectElements;
            } else {
                schemaObjectArray = objectBuffer;
                schemaObjectIndex = objectIndex;
                objectIndex = newObjectIndex;
            }
        }
        byte[] schemaPrimitiveArray = Utils.emptyByteArray();
        int schemaPrimitiveIndex = 0;
        if(primitiveElements > 0) {
            int newPrimitiveIndex = Math.addExact(primitiveElements, primitiveIndex);
            if(newPrimitiveIndex > primitiveBuffer.length) {
                primitiveBuffer = schemaPrimitiveArray = new byte[primitiveBuffer.length];
                primitiveIndex = primitiveElements;
            }  else {
                schemaPrimitiveArray = primitiveBuffer;
                schemaPrimitiveIndex = primitiveIndex;
                primitiveIndex = newPrimitiveIndex;
            }
        }
        return new MarshallSchema(schemaObjectArray, schemaPrimitiveArray, schemaObjectIndex, schemaPrimitiveIndex, objectElements, primitiveElements);
    }
}
