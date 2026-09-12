package io.jingproject.ffm;

import io.jingproject.common.Os;
import io.jingproject.common.anno.ProcessorApi;

import java.util.List;
import java.util.function.Supplier;

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

    // the list of method names exported by this native library.
    List<String> methodNames();

    // the actual implementation instance of this binding.
    Object impl();
}
