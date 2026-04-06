package io.jingproject.marshalltest.test;

import io.jingproject.marshall.MarshallFacade;
import io.jingproject.marshall.MarshallSchema;
import org.junit.jupiter.api.Test;

public class MarshallTest {
    @Test
    public void testMarshall() {
        MarshallFacade marshallFacade = new RecordEntityMarshallImpl();
        MarshallSchema marshallSchema = marshallFacade.newSchema();

    }
}
