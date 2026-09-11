package io.jingproject.marshalljsontest;

import io.jingproject.common.HeapReadBuffer;
import io.jingproject.common.HeapWriteBuffer;
import io.jingproject.marshalljson.JsonDeserializerContext;
import io.jingproject.marshalljson.JsonDeserializerOption;
import io.jingproject.marshalljson.JsonSerializerContext;
import io.jingproject.marshalljson.JsonSerializerOption;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class StringSerializationTest {

    // setting jing.marshalljson.serialize.vecsize=128 to better evaluate the vectorization strategy
    @Test
    public void stringTest() {
        List<String> strs = List.of(
                "abcd",
                "abcde",
                "abcdef",
                "abcdefg",
                "abcdefgh",
                "abc\"",
                "abcd\"",
                "abcdefg\"",
                "abcdefgh\"",
                "你好",
                "abcd\ud83d\ude0a",
                "abcdefg\ud83d\ude0a",
                "abc\ud83d\ude0aabc\ud83d\ude0a",
                "\"\"\"\"",
                "\t\\\\\\\\\t",
                "\u00e9\u00e9\u00e9\u00e9",
                ("\u00e9\u00e9\u00e9\u00e9").repeat(5),
                "a".repeat(5),
                "a\t".repeat(5),
                "a\u4f60\ud83d\ude0a\"\t\\bc\ud83d\ude0a\u4f60\u597d\t",
                "abc\ud83d\ude0adef\tghi\\jkl\"mno\u4f60\u597d",
                "abcd\nabcd",
                "http:\\/\\/abs.twimg.com\\/images\\/themes\\/theme1\\/bg.png",
                "http:\\/\\/abs.twimg.com\\/images\\/themes\\/theme1\\/bg.png".repeat(5)
        );
        for (String str : strs) {
            HeapWriteBuffer writeBuffer = new HeapWriteBuffer(1000);
            JsonSerializerContext serCtx = JsonSerializerContext.newCtx(JsonSerializerOption.defaultOption(), writeBuffer);
            serCtx.serializeEscapedString(str);
            serCtx.commit();
            byte[] jsonBytes = writeBuffer.toByteArray();

            // verify output starts and ends with quote
            Assertions.assertEquals(0x22, jsonBytes[0] & 0xff, "output must start with '\"', failed for: " + str);
            Assertions.assertEquals(0x22, jsonBytes[jsonBytes.length - 1] & 0xff, "output must end with '\"', failed for: " + str);

            // verify round-trip: deserialize back to original string
            HeapReadBuffer readBuffer = new HeapReadBuffer(jsonBytes);
            byte firstByte = readBuffer.readByte();
            JsonDeserializerContext deserCtx = JsonDeserializerContext.newContext(JsonDeserializerOption.defaultOption(), readBuffer);
            String result = deserCtx.deserializeString(firstByte);
            Assertions.assertEquals(str, result, "round-trip failed for: " + str);
        }
    }
}
