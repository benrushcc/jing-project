package io.jingproject.marshalljsontest.twi;

import io.jingproject.marshall.Marshallable;
import io.jingproject.marshall.NamingConvention;

import java.util.Arrays;
import java.util.Objects;

@Marshallable(from = NamingConvention.CAMEL_CASE, to = NamingConvention.SNAKE_CASE)
public record TwiHashTag(
        String text,
        int[] indices
) {
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof TwiHashTag(String text1, int[] indices1))) return false;
        return Objects.equals(text, text1) && Arrays.equals(indices, indices1);
    }

    @Override
    public int hashCode() {
        return Objects.hash(text, Arrays.hashCode(indices));
    }
}
