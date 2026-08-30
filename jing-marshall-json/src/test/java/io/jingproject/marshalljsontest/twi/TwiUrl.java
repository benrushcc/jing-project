package io.jingproject.marshalljsontest.twi;

import io.jingproject.marshall.Marshallable;
import io.jingproject.marshall.NamingConvention;

import java.util.Arrays;
import java.util.Objects;

@Marshallable(from = NamingConvention.CAMEL_CASE, to = NamingConvention.SNAKE_CASE)
public record TwiUrl(
        String url,
        String expandedUrl,
        String displayUrl,
        int[] indices
) {
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof TwiUrl(String url1, String expandedUrl1, String displayUrl1, int[] indices1))) return false;
        return Objects.equals(url, url1) && Arrays.equals(indices, indices1) && Objects.equals(displayUrl, displayUrl1) && Objects.equals(expandedUrl, expandedUrl1);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, expandedUrl, displayUrl, Arrays.hashCode(indices));
    }
}
