package io.jingproject.marshalljsontest.twi;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.jingproject.marshall.MarshallAttr;
import io.jingproject.marshall.Marshallable;
import io.jingproject.marshall.NamingConvention;

@Marshallable(from = NamingConvention.CAMEL_CASE, to = NamingConvention.SNAKE_CASE)
public record TwiUser(
       long id,
       String idStr,
       String name,
       String screenName,
       String location,
       String description,
       String url,
       TwiUserEntities entities,
       @JsonProperty("protected") @MarshallAttr(mappedName = "protected") boolean isProtected,
       long followersCount,
       long friendsCount,
       long listedCount,
       String createdAt,
       long favouritesCount,
       Integer utcOffset,
       String timeZone,
       boolean geoEnabled,
       boolean verified,
       long statusesCount,
       String lang,
       boolean contributorsEnabled,
       boolean isTranslator,
       boolean isTranslationEnabled,
       String profileBackgroundColor,
       String profileBackgroundImageUrl,
       String profileBackgroundImageUrlHttps,
       boolean profileBackgroundTile,
       String profileImageUrl,
       String profileImageUrlHttps,
       String profileBannerUrl,
       String profileLinkColor,
       String profileSidebarBorderColor,
       String profileSidebarFillColor,
       String profileTextColor,
       boolean profileUseBackgroundImage,
       boolean defaultProfile,
       boolean defaultProfileImage,
       boolean following,
       boolean followRequestSent,
       boolean notifications
) {
}
