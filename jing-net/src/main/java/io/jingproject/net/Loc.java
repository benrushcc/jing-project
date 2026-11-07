package io.jingproject.net;

public record Loc(
        int ipType,
        String ipAddress,
        int port
) {
}
