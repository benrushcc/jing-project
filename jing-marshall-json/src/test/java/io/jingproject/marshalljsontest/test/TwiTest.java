package io.jingproject.marshalljsontest.test;

import io.jingproject.common.HeapWriteBuffer;
import io.jingproject.marshalljsontest.TwiUtil;
import io.jingproject.marshalljsontest.UtfUtil;
import io.jingproject.marshalljsontest.twi.Twi;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

@Tag("view-output")
public class TwiTest {

    final Path dir = Path.of(System.getProperty("user.dir"));

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
    public void jacksonDeserializeTwiTest() {
        String json = TwiUtil.loadAsString();
        Twi twi = TwiUtil.deserializeTwiUsingJackson(json);
        Assertions.assertNotNull(twi);
    }

    // 477706
    @Test
    public void jacksonSerializeTwiTest() {
        String json = TwiUtil.loadAsString();
        Twi twi = TwiUtil.deserializeTwiUsingJackson(json);
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream(819200)) {
            TwiUtil.serializeTwiUsingJackson(twi, outputStream);
            byte[] content = outputStream.toByteArray();
            Assertions.assertTrue(content.length > 0);
            System.out.println(content.length);
            Path outputPath = dir.resolve("twi-jackson.json");
            Files.write(outputPath, content, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
            System.out.println(outputPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // 442181 when jing.marshalljson.escapeslash is enabled, or 436137 when it is disabled
    @Test
    public void serializeTwiTest() {
        String json = TwiUtil.loadAsString();
        Twi twi = TwiUtil.deserializeTwiUsingJackson(json);
        HeapWriteBuffer writeBuffer = new HeapWriteBuffer(819200);
        TwiUtil.serializeTwi(twi, writeBuffer);
        byte[] content = writeBuffer.toByteArray();
        Assertions.assertTrue(content.length > 0);
        System.out.println(content.length);
        try {
            Path outputPath = dir.resolve("twi-jing.json");
            Files.write(outputPath, content, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
            System.out.println(outputPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
