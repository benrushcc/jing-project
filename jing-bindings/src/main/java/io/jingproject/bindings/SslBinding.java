package io.jingproject.bindings;

import io.jingproject.common.Os;
import io.jingproject.ffm.Downcall;
import io.jingproject.ffm.FFM;

@FFM(libraryName = "jing_bindings", dependsOn = {"crypto", "ssl"}, supportedOS = {Os.WINDOWS, Os.LINUX, Os.MACOS})
public interface SslBinding {
    @Downcall(methodName = "jing_ssl_version",critical = true)
    long sslVersion();
}
