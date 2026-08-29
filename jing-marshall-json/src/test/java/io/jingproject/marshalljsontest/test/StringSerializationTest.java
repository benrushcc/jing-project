package io.jingproject.marshalljsontest.test;

import io.jingproject.common.HeapWriteBuffer;
import io.jingproject.marshalljson.JsonSerializerContext;
import io.jingproject.marshalljson.JsonSerializerOption;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Tag("view-output")
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
                "abcd😊",
                "abcdefg😊",
                "abc😊abc😊",
                "\"\"\"\"",
                "\t\\\\\\\\\t",
                "éééé",
                "éééé".repeat(5),
                "a".repeat(5),
                "a\t".repeat(5),
                "a你😊\"\t\\bc😊你好\t",
                "abc😊def\tghi\\jkl\"mno你好",
                "abcd\nabcd",
                "http:\\/\\/abs.twimg.com\\/images\\/themes\\/theme1\\/bg.png",
                "http:\\/\\/abs.twimg.com\\/images\\/themes\\/theme1\\/bg.png".repeat(5)
        );
        for (String str : strs) {
            HeapWriteBuffer writeBuffer = new HeapWriteBuffer(1000);
            JsonSerializerContext context = new JsonSerializerContext(JsonSerializerOption.defaultOption(), writeBuffer);
            context.serializeEscapedString(str);
            String jsonStr = new String(writeBuffer.toByteArray(), StandardCharsets.UTF_8);
            System.out.println(jsonStr);
        }
    }
}
