package io.jingproject.marshalljsontest.test;

import io.jingproject.common.HeapWriteBuffer;
import io.jingproject.marshalljson.JsonSerializerNode;
import io.jingproject.marshalljsontest.TwiUtil;
import io.jingproject.marshalljsontest.twi.Twi;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

@Tag("view-output")
public class TwiTest {

    Path dir = Path.of(System.getProperty("user.dir"));

    @Test
    public void loadTwiTest() {
        String json = TwiUtil.load();
        Assertions.assertNotNull(json);
    }

    @Test
    public void jacksonDeserializeTwiTest() {
        String json = TwiUtil.load();
        Twi twi = TwiUtil.deserializeTwiUsingJackson(json);
        Assertions.assertNotNull(twi);
    }

    @Test
    public void jacksonSerializeTwiTest() {
        String json = TwiUtil.load();
        Twi twi = TwiUtil.deserializeTwiUsingJackson(json);
        try(ByteArrayOutputStream outputStream = new ByteArrayOutputStream(819200)) {
            TwiUtil.serializeTwiUsingJackson(twi, outputStream);
            byte[] content = outputStream.toByteArray();
            Assertions.assertTrue(content.length > 0);
            System.out.println(content.length);
            Path outputPath = dir.resolve("twi-jackson.json");
            Files.write(outputPath, content, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
            System.out.println(outputPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void serializeTwiTest() {
        String json = TwiUtil.load();
        Twi twi = TwiUtil.deserializeTwiUsingJackson(json);
        HeapWriteBuffer writeBuffer = new HeapWriteBuffer(819200);
        TwiUtil.serializeTwi(twi, writeBuffer);
        byte[] content = writeBuffer.toByteArray();
        Assertions.assertTrue(content.length > 0);
        System.out.println(content.length);
        try {
            Path outputPath = dir.resolve("twi-jing.json");
            Files.write(outputPath, content, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
            System.out.println(outputPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
