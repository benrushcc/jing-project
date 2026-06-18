package io.jingproject.marshalljson;

/**
 * Represents the result state of a JSON serialization node.
 * It indicates whether the current node has finished writing, can continue,
 * or needs to transfer control to another node.
 */
public sealed interface JsonSerializerNodeResult
        permits JsonSerializerNodeResult.JsonSerializerNodeTransfer,
        JsonSerializerNodeResult.JsonSerializerNodeContinue,
        JsonSerializerNodeResult.JsonSerializerNodeFinished {

    /**
     * Indicates that the current node has finished writing its data.
     * Control can be returned to the parent node.
     */
    record JsonSerializerNodeFinished() implements JsonSerializerNodeResult {

    }

    /**
     * Indicates that the current node has more data to write.
     * The serialization process should continue with the same node.
     */
    record JsonSerializerNodeContinue() implements JsonSerializerNodeResult {

    }

    /**
     * Indicates that the current node cannot handle the data locally
     * and must delegate control to a newly created node {@code n}.
     * The new node will take over the subsequent processing.
     *
     * @param n the new node to which control is transferred
     */
    record JsonSerializerNodeTransfer(JsonSerializerNode n) implements JsonSerializerNodeResult {

    }
}
