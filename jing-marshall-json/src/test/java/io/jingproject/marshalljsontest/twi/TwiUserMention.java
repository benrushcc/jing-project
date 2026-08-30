package io.jingproject.marshalljsontest.twi;

import io.jingproject.marshall.Marshallable;
import io.jingproject.marshall.NamingConvention;

import java.util.Arrays;
import java.util.Objects;

@Marshallable(from = NamingConvention.CAMEL_CASE, to = NamingConvention.SNAKE_CASE)
public record TwiUserMention(
        String screenName,
        String name,
        long id,
        String idStr,
        int[] indices
) {
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof TwiUserMention(String screenName1, String name1, long id1, String str, int[] indices1))) return false;
        return id == id1 && Objects.equals(name, name1) && Objects.equals(idStr, str) && Arrays.equals(indices, indices1) && Objects.equals(screenName, screenName1);
    }

    @Override
    public int hashCode() {
        return Objects.hash(screenName, name, id, idStr, Arrays.hashCode(indices));
    }
}
