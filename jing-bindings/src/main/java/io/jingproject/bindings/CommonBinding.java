package io.jingproject.bindings;

import io.jingproject.ffm.Downcall;
import io.jingproject.ffm.FFM;

@FFM(libraryName = "jing_bindings")
public interface CommonBinding {
    default String versionString() {
        return majorVersion() + "." + minorVersion() + "." + patchVersion();
    }

    @Downcall(methodName = "jing_major_version", constant = true, critical = true)
    int majorVersion();

    @Downcall(methodName = "jing_minor_version", constant = true, critical = true)
    int minorVersion();

    @Downcall(methodName = "jing_patch_version", constant = true, critical = true)
    int patchVersion();

    @Downcall(methodName = "jing_ptr_err_flag", constant = true, critical = true)
    long ptrErrFlag();
}
