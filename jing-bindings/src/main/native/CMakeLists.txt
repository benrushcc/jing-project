cmake_minimum_required(VERSION 3.29)

# build using this command : cmake --preset=windows/macos/linux && cmake --build build

# detecting presets
set(PRESET_NAME $ENV{JING_PRESET_NAME})
if(DEFINED PRESET_NAME)
    message(STATUS "Preset name : ${PRESET_NAME}")
else()
    message(FATAL_ERROR "Preset not found")
endif()

# don't build thirdparty shared libraries
set(BUILD_SHARED_LIBS OFF CACHE BOOL "cmake shared library policy" FORCE)

# set apple env
if(APPLE)
    execute_process(
        COMMAND xcrun --sdk macosx --show-sdk-path
        RESULT_VARIABLE XCRUN_RESULT
        OUTPUT_VARIABLE MACOS_SDK_PATH
        OUTPUT_STRIP_TRAILING_WHITESPACE
    )
    if(NOT XCRUN_RESULT EQUAL 0)
        message(FATAL_ERROR "Failed to get macOS SDK path")
    endif()
    set(CMAKE_OSX_SYSROOT       ${MACOS_SDK_PATH}   CACHE STRING "macos sdk root"     FORCE)
    set(CMAKE_OSX_ARCHITECTURES "arm64"             CACHE STRING "macos architecture" FORCE)
endif()

# out target
project(jing_bindings_project VERSION 0.0.1 LANGUAGES C)

# provided options
option(JING_BUILD_DEMO "build jing_demo dynamic library for testing purpose" ON)
option(JING_USE_WEPOLL "provide wepoll related functions in jing_bindings library" ON)
option(JING_USE_BORINGSSL "provide boringssl related functions in jing_bindings library" ON)

if(JING_BUILD_DEMO) 
    add_library(jing_demo SHARED)
    target_sources(jing_demo PRIVATE src/jing_demo.c)
endif()

set(JING_WEPOLL_VERSION    "v1.5.8"       CACHE STRING "wepoll library version"    FORCE)
set(JING_BORINGSSL_VERSION "0.20251002.0" CACHE STRING "boringssl library version" FORCE)

add_library(jing_bindings SHARED)
target_sources(jing_bindings PRIVATE
    src/jing_common.c
    src/jing_win.c
    src/jing_posix.c
    src/jing_linux.c
    src/jing_macos.c
)

include(FetchContent)

# setup wepoll
if(WIN32)
    if(JING_USE_WEPOLL)
        FetchContent_Declare(
            wepollproject
            GIT_REPOSITORY https://github.com/piscisaureus/wepoll.git
            GIT_TAG tags/${JING_WEPOLL_VERSION}
            GIT_SHALLOW TRUE
            SOURCE_SUBDIR none_exist
        )
        message(STATUS "Configuring wepoll")
        FetchContent_MakeAvailable(wepollproject)
        message(STATUS "Configuring wepoll succeed")
        target_compile_definitions(jing_bindings PRIVATE JING_USE_WEPOLL=1)
        add_library(wepollstatic STATIC ${wepollproject_SOURCE_DIR}/wepoll.c)
        target_include_directories(wepollstatic PUBLIC ${wepollproject_SOURCE_DIR})
        target_link_libraries(wepollstatic PUBLIC ws2_32)
        target_link_libraries(jing_bindings PRIVATE wepollstatic)
    endif()
endif()

# setup boringssl
if(JING_USE_BORINGSSL)
#     target_compile_definitions(jing PRIVATE JING_ENABLE_BORINGSSL)
#     FetchContent_Declare(
#         boringsslproject
#         GIT_REPOSITORY https://github.com/google/boringssl.git
#         GIT_TAG tags/${JING_BORINGSSL_VERSION}
#         GIT_SHALLOW TRUE
#     )
#     set(BUILD_TESTING OFF CACHE BOOL "Whether to build boringssl tests" FORCE)
#     message(STATUS "Configuring boringssl")
#     FetchContent_MakeAvailable(boringsslproject)
#     message(STATUS "Configuring boringssl succeed")
#     set_target_properties(bssl PROPERTIES EXCLUDE_FROM_ALL TRUE)
#     if(APPLE)
#         target_compile_definitions(crypto PRIVATE OPENSSL_STATIC_ARMCAP)
#         target_compile_definitions(ssl PRIVATE OPENSSL_STATIC_ARMCAP)
#     endif()
#     target_link_libraries(jing PRIVATE ssl crypto)
endif()