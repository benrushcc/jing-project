package io.jingproject.bench.twi;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.helidon.json.binding.Json;
import io.jingproject.marshall.MarshallAttr;
import io.jingproject.marshall.Marshallable;

@Marshallable
@Json.Entity
public record TwiUser(
        @MarshallAttr(mappedName = "id") @JsonProperty("id") @Json.Property("id") long id,
        @MarshallAttr(mappedName = "id_str") @JsonProperty("id_str") @Json.Property("id_str") String idStr,
        @MarshallAttr(mappedName = "name") @JsonProperty("name") @Json.Property("name") String name,
        @MarshallAttr(mappedName = "screen_name") @JsonProperty("screen_name") @Json.Property("screen_name") String screenName,
        @MarshallAttr(mappedName = "location") @JsonProperty("location") @Json.Property("location") String location,
        @MarshallAttr(mappedName = "description") @JsonProperty("description") @Json.Property("description") String description,
        @MarshallAttr(mappedName = "url") @JsonProperty("url") @Json.Property("url") String url,
        @MarshallAttr(mappedName = "entities") @JsonProperty("entities") @Json.Property("entities") TwiUserEntities entities,
        @MarshallAttr(mappedName = "protected") @JsonProperty("protected") @Json.Property("protected") boolean isProtected,
        @MarshallAttr(mappedName = "followers_count") @JsonProperty("followers_count") @Json.Property("followers_count") long followersCount,
        @MarshallAttr(mappedName = "friends_count") @JsonProperty("friends_count") @Json.Property("friends_count") long friendsCount,
        @MarshallAttr(mappedName = "listed_count") @JsonProperty("listed_count") @Json.Property("listed_count") long listedCount,
        @MarshallAttr(mappedName = "created_at") @JsonProperty("created_at") @Json.Property("created_at") String createdAt,
        @MarshallAttr(mappedName = "favourites_count") @JsonProperty("favourites_count") @Json.Property("favourites_count") long favouritesCount,
        @MarshallAttr(mappedName = "utc_offset") @JsonProperty("utc_offset") @Json.Property("utc_offset") Integer utcOffset,
        @MarshallAttr(mappedName = "time_zone") @JsonProperty("time_zone") @Json.Property("time_zone") String timeZone,
        @MarshallAttr(mappedName = "geo_enabled") @JsonProperty("geo_enabled") @Json.Property("geo_enabled") boolean geoEnabled,
        @MarshallAttr(mappedName = "verified") @JsonProperty("verified") @Json.Property("verified") boolean verified,
        @MarshallAttr(mappedName = "statuses_count") @JsonProperty("statuses_count") @Json.Property("statuses_count") long statusesCount,
        @MarshallAttr(mappedName = "lang") @JsonProperty("lang") @Json.Property("lang") String lang,
        @MarshallAttr(mappedName = "contributors_enabled") @JsonProperty("contributors_enabled") @Json.Property("contributors_enabled") boolean contributorsEnabled,
        @MarshallAttr(mappedName = "is_translator") @JsonProperty("is_translator") @Json.Property("is_translator") boolean isTranslator,
        @MarshallAttr(mappedName = "is_translation_enabled") @JsonProperty("is_translation_enabled") @Json.Property("is_translation_enabled") boolean isTranslationEnabled,
        @MarshallAttr(mappedName = "profile_background_color") @JsonProperty("profile_background_color") @Json.Property("profile_background_color") String profileBackgroundColor,
        @MarshallAttr(mappedName = "profile_background_image_url") @JsonProperty("profile_background_image_url") @Json.Property("profile_background_image_url") String profileBackgroundImageUrl,
        @MarshallAttr(mappedName = "profile_background_image_url_https") @JsonProperty("profile_background_image_url_https") @Json.Property("profile_background_image_url_https") String profileBackgroundImageUrlHttps,
        @MarshallAttr(mappedName = "profile_background_tile") @JsonProperty("profile_background_tile") @Json.Property("profile_background_tile") boolean profileBackgroundTile,
        @MarshallAttr(mappedName = "profile_image_url") @JsonProperty("profile_image_url") @Json.Property("profile_image_url") String profileImageUrl,
        @MarshallAttr(mappedName = "profile_image_url_https") @JsonProperty("profile_image_url_https") @Json.Property("profile_image_url_https") String profileImageUrlHttps,
        @MarshallAttr(mappedName = "profile_banner_url") @JsonProperty("profile_banner_url") @Json.Property("profile_banner_url") String profileBannerUrl,
        @MarshallAttr(mappedName = "profile_link_color") @JsonProperty("profile_link_color") @Json.Property("profile_link_color") String profileLinkColor,
        @MarshallAttr(mappedName = "profile_sidebar_border_color") @JsonProperty("profile_sidebar_border_color") @Json.Property("profile_sidebar_border_color") String profileSidebarBorderColor,
        @MarshallAttr(mappedName = "profile_sidebar_fill_color") @JsonProperty("profile_sidebar_fill_color") @Json.Property("profile_sidebar_fill_color") String profileSidebarFillColor,
        @MarshallAttr(mappedName = "profile_text_color") @JsonProperty("profile_text_color") @Json.Property("profile_text_color") String profileTextColor,
        @MarshallAttr(mappedName = "profile_use_background_image") @JsonProperty("profile_use_background_image") @Json.Property("profile_use_background_image") boolean profileUseBackgroundImage,
        @MarshallAttr(mappedName = "default_profile") @JsonProperty("default_profile") @Json.Property("default_profile") boolean defaultProfile,
        @MarshallAttr(mappedName = "default_profile_image") @JsonProperty("default_profile_image") @Json.Property("default_profile_image") boolean defaultProfileImage,
        @MarshallAttr(mappedName = "following") @JsonProperty("following") @Json.Property("following") boolean following,
        @MarshallAttr(mappedName = "follow_request_sent") @JsonProperty("follow_request_sent") @Json.Property("follow_request_sent") boolean followRequestSent,
        @MarshallAttr(mappedName = "notifications") @JsonProperty("notifications") @Json.Property("notifications") boolean notifications
) {
}