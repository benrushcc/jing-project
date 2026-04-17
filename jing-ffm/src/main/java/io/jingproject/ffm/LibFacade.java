package io.jingproject.ffm;

import io.jingproject.common.Os;
import io.jingproject.common.anno.ProcessorApi;

import java.util.List;
import java.util.function.Supplier;

@ProcessorApi
public interface LibFacade {
    /**
     * @return the target interface class that this binding implements.
     */
    Class<?> target();

    /**
     * @return the list of operating systems supported by this binding.
     */
    List<Os> supportedOS();

    /**
     * @return the name of the native library.
     * OS-specific prefixes (e.g., "lib") or file extensions (e.g., ".so", ".dll", ".dylib") will be resolved at best effort.
     * There is no need to manually specify them.
     */
    String libName();

    /**
     * @return the list of method names exported by this native library.
     */
    List<String> methodNames();

    /**
     * @return the supplier of the actual implementation instance of this binding.
     */
    Supplier<?> supplier();
}
