package io.jingproject.marshalljsontest.twi;

import io.jingproject.marshall.Marshallable;
import io.jingproject.marshall.NamingConvention;

import java.util.Arrays;
import java.util.Objects;

@Marshallable(from = NamingConvention.CAMEL_CASE, to = NamingConvention.SNAKE_CASE)
public record TwiMedia(
        long id,
        String idStr,
        int[] indices,
        String mediaUrl,
        String mediaUrlHttps,
        String url,
        String displayUrl,
        String expandedUrl,
        String type,
        TwiSizes sizes,
        Long sourceStatusId,
        String sourceStatusIdStr
) {
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof TwiMedia(
                long id1, String str, int[] indices1, String mediaUrl1, String urlHttps, String url1,
                String displayUrl1, String expandedUrl1, String type1, TwiSizes sizes1, Long statusId,
                String statusIdStr
        ))) return false;
        return id == id1 && Objects.equals(url, url1) && Objects.equals(type, type1) && Objects.equals(idStr, str) && Arrays.equals(indices, indices1) && Objects.equals(sizes, sizes1) && Objects.equals(mediaUrl, mediaUrl1) && Objects.equals(displayUrl, displayUrl1) && Objects.equals(expandedUrl, expandedUrl1) && Objects.equals(sourceStatusId, statusId) && Objects.equals(mediaUrlHttps, urlHttps) && Objects.equals(sourceStatusIdStr, statusIdStr);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, idStr, Arrays.hashCode(indices), mediaUrl, mediaUrlHttps, url, displayUrl, expandedUrl, type, sizes, sourceStatusId, sourceStatusIdStr);
    }
}
