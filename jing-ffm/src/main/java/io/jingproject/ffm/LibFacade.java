package io.jingproject.ffm;

import io.jingproject.common.Os;
import io.jingproject.common.anno.ProcessorApi;

import java.util.List;

@ProcessorApi
public interface LibFacade {

    // the target interface class that this binding implements.
    Class<?> target();

    // the list of operating systems supported by this binding.
    List<Os> supportedOS();

    // the name of the native library.
    // OS-specific prefixes (e.g., "lib") or file extensions
    // (e.g., ".so", ".dll", ".dylib") are resolved at best effort,
    // so there is no need to specify them manually.
    String libName();

    // the list of native library names that this library depends on.
    // each entry follows the same naming convention as libName(), that is,
    // OS-specific prefixes and file extensions are resolved at best effort
    // and should not be specified manually.
    // the loader resolves these depLookups (recursively, if they are
    // themselves represented by a LibFacade) before loading this library,
    // so that dependent libraries are loaded in the correct order.
    // an empty list means no explicit dependency, in which case the loader
    // falls back to the platform's native dependency resolution (the OS
    // loader searching the library path on its own).
    List<String> dependsOn();

    // the list of method names exported by this native library.
    List<String> methodNames();

    // the actual implementation instance of this binding.
    Object impl();
}
