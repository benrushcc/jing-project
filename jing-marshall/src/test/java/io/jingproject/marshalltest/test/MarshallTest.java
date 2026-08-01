package io.jingproject.marshalltest.test;

import io.jingproject.marshall.MarshallFacade;
import io.jingproject.marshall.MarshallInfo;
import io.jingproject.marshall.MarshallWriter;
import io.jingproject.marshalltest.entity.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

public class MarshallTest {

    @Test
    public void testMarshallBean() {
        MarshallFacade marshallFacade = new BeanEntityMarshallFacade();
        MarshallWriter writer = marshallFacade.newWriter();
        LocalDateTime now = LocalDateTime.now();
        writer.setInt(0, 123);
        writer.setLong(1, 456L);
        writer.setObject(2, "hello world");
        writer.setObject(3, now);
        BeanEntity beanEntity = (BeanEntity) marshallFacade.construct(writer);
        Assertions.assertEquals(123, beanEntity.intValue());
        Assertions.assertEquals(456L, beanEntity.longValue());
        Assertions.assertEquals("hello world", beanEntity.strValue());
        Assertions.assertEquals(now, beanEntity.timeValue());
    }

    @Test
    public void testMarshallExtendedBean() {
        MarshallFacade marshallFacade = new ExtendEntityMarshallFacade();
        MarshallWriter writer = marshallFacade.newWriter();
        LocalDateTime now = LocalDateTime.now();
        Duration duration = Duration.ofDays(7355608);
        Map<Integer, String> m = Map.of();
        writer.setInt(0, 123);
        writer.setLong(1, 456L);
        writer.setObject(2, "hello world");
        writer.setObject(3, now);
        writer.setObject(4, duration);
        writer.setObject(5, m);
        ExtendEntity extendEntity = (ExtendEntity) marshallFacade.construct(writer);
        Assertions.assertEquals(123, extendEntity.intValue());
        Assertions.assertEquals(456L, extendEntity.longValue());
        Assertions.assertEquals("hello world", extendEntity.strValue());
        Assertions.assertEquals(now, extendEntity.timeValue());
        Assertions.assertEquals(duration, extendEntity.durationValue());
        Assertions.assertEquals(m, extendEntity.mapValue());
    }

    @Test
    public void testMarshallRecord() {
        MarshallFacade marshallFacade = new RecordEntityMarshallFacade();
        MarshallWriter writer = marshallFacade.newWriter();
        LocalDateTime now = LocalDateTime.now();
        writer.setInt(0, 123);
        writer.setLong(1, 456L);
        writer.setObject(2, "hello world");
        writer.setObject(3, now);
        Object constructed = marshallFacade.construct(writer);
        if (constructed instanceof RecordEntity(
                int intValue, long longValue, String strValue, LocalDateTime timeValue
        )) {
            Assertions.assertEquals(123, intValue);
            Assertions.assertEquals(456L, longValue);
            Assertions.assertEquals("hello world", strValue);
            Assertions.assertEquals(now, timeValue);
        } else {
            throw new AssertionError();
        }
    }

    @Test
    public void testMarshallEnum() {
        MarshallFacade marshallFacade = new EnumEntityMarshallFacade();
        MarshallInfo marshallInfo = marshallFacade.marshallInfoByFieldName("INT");
        EnumEntity enumEntity = EnumEntity.values()[marshallInfo.index()];
        Assertions.assertEquals(EnumEntity.INT, enumEntity);
    }
}
