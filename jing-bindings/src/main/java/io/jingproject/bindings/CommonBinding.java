package io.jingproject.bindings;

import io.jingproject.ffm.Downcall;
import io.jingproject.ffm.FFM;

@FFM(libraryName = "jing_bindings")
public interface CommonBinding {
    default String versionString() {
        return majorVersion() + "." + minorVersion() + "." + patchVersion();
    }

    @Downcall(methodName = "jing_major_version",critical = true)
    int majorVersion();

    @Downcall(methodName = "jing_minor_version",critical = true)
    int minorVersion();

    @Downcall(methodName = "jing_patch_version",critical = true)
    int patchVersion();

    @Downcall(methodName = "jing_ptr_err_flag",critical = true)
    long ptrErrFlag();
}
