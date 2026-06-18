package io.jingproject.marshalljson;

import io.jingproject.common.WriteBuffer;

@FunctionalInterface
public interface JsonValueSerializer {
    /**
     * Serializes the given object into JSON and writes the result to the buffer.
     * The caller must ensure that {@code o} is not null.
     * If this method returns null, the current value has been fully written
     * within this call and no further action is needed from the current node.
     * If it returns a non-null JsonSerializerNode, the current value cannot be
     * fully processed at this level, and control must be transferred to that
     * node for further processing.
     *
     * @param o            the object value to serialize (not null)
     * @param option       serialization options controlling formatting and behavior
     * @param writeBuffer  the buffer to write JSON output to
     * @param indent       the current indentation level for the serialized value
     * @return a JsonSerializerNode if delegation is needed, or null if complete
     */
    JsonSerializerNode serialize(Object o, JsonSerializerOption option, WriteBuffer writeBuffer, int indent);
}
