package io.jingproject.marshalljsontest.test;

import io.jingproject.common.HeapReadBuffer;
import io.jingproject.common.HeapWriteBuffer;
import io.jingproject.common.SegmentReadBuffer;
import io.jingproject.common.SegmentWriteBuffer;
import io.jingproject.marshalljson.JsonDeserializer;
import io.jingproject.marshalljson.JsonDeserializerOption;
import io.jingproject.marshalljson.JsonSerializer;
import io.jingproject.marshalljson.JsonSerializerOption;
import io.jingproject.marshalljsontest.TwiUtil;
import io.jingproject.marshalljsontest.UtfUtil;
import io.jingproject.marshalljsontest.twi.Twi;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.foreign.Arena;
import java.nio.charset.StandardCharsets;

public class TwiTest {

    @Test
    public void loadTwiTest() {
        String json = TwiUtil.loadAsString();
        Assertions.assertNotNull(json);
    }

    //UTF-8 byte sequence statistics:
    //1-byte characters: 551589
    //escape characters: 1230
    //2-byte characters: 28
    //3-byte characters: 31770
    //4-byte characters: 10
    //Total characters: 583397
    //Total bytes: 646995
    //1-byte: 94.55%
    //escape: 0.21%
    //2-byte: 0.00%
    //3-byte: 5.45%
    //4-byte: 0.00%
    @Test
    @Tag("view-output")
    public void twiStatisticsTest() {
        String json = TwiUtil.loadAsString();
        UtfUtil.countUtf8Bytes(json.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    public void deserializeTest() {
        String json = TwiUtil.loadAsString();
        HeapReadBuffer readBuffer = new HeapReadBuffer(json.getBytes(StandardCharsets.UTF_8));
        JsonDeserializer jsonDeserializer = new JsonDeserializer(JsonDeserializerOption.defaultOption());
        Twi r = jsonDeserializer.deserializeMarshallableObject(Twi.class, readBuffer);
        Assertions.assertNotNull(r);
    }

    @Test
    public void roundTripTest() {
        String json = TwiUtil.loadAsString();
        Twi twi = TwiUtil.deserializeTwiUsingJackson(json);
        JsonSerializer jsonSerializer = new JsonSerializer(JsonSerializerOption.defaultOption());
        JsonDeserializer jsonDeserializer = new JsonDeserializer(JsonDeserializerOption.defaultOption());

        HeapWriteBuffer heapWriteBuffer = new HeapWriteBuffer(819200);
        jsonSerializer.serializeMarshallableObject(twi, heapWriteBuffer);
        HeapReadBuffer heapReadBuffer = new HeapReadBuffer(heapWriteBuffer.toByteArray());
        Twi r = jsonDeserializer.deserializeMarshallableObject(Twi.class, heapReadBuffer);
        Assertions.assertEquals(twi, r);

        try(Arena arena = Arena.ofConfined()) {
            SegmentWriteBuffer segmentWriteBuffer = new SegmentWriteBuffer(arena, 819200);
            jsonSerializer.serializeMarshallableObject(twi, segmentWriteBuffer);
            SegmentReadBuffer segmentReadBuffer = new SegmentReadBuffer(segmentWriteBuffer.toSegment());
            Twi r2 = jsonDeserializer.deserializeMarshallableObject(Twi.class, segmentReadBuffer);
            Assertions.assertEquals(twi, r2);
        }
    }


}
