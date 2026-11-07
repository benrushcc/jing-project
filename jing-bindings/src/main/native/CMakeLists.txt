cmake_minimum_required(VERSION 3.29)

# build using this command : cmake --preset=macos/linux && cmake --build build
# on windows, to obtain environment variables from VS, use . .\vs-env.ps1 && cmake --preset=windows && cmake --build build

# detecting presets
set(PRESET_NAME $ENV{JING_PRESET_NAME})
if(DEFINED PRESET_NAME)
    message(STATUS "preset name : ${PRESET_NAME}")
else()
    message(FATAL_ERROR "preset not found")
endif()

# detecting install path
if(NOT DEFINED ENV{JING_LIBRARY_PATH} OR
   "$ENV{JING_LIBRARY_PATH}" STREQUAL "")
    message(FATAL_ERROR
        "JING_LIBRARY_PATH is not set. "
        "Please set JING_LIBRARY_PATH before running cmake."
    )
endif()

set(JING_LIBRARY_PATH "$ENV{JING_LIBRARY_PATH}")
message(STATUS "JING_LIBRARY_PATH : ${JING_LIBRARY_PATH}")

# don't build thirdparty shared libraries
set(BUILD_SHARED_LIBS OFF  CACHE BOOL "whether to build shared library" FORCE)

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

function(jing_configure target)
    if(MSVC)
        target_compile_definitions(${target} PRIVATE
            WIN32=1
            _WINDOWS=1
            WIN32_LEAN_AND_MEAN=1
        )
        target_compile_options(${target} PRIVATE
            /W4
            /DWIN32_LEAN_AND_MEAN
            /utf-8
            /nologo
        )
        target_link_options(${target} PRIVATE
            /machine:x64
            /DEBUG:NONE
        )
    elseif(CMAKE_C_COMPILER_ID MATCHES "GNU|Clang")
        target_compile_options(${target} PRIVATE
            -Wall 
            -Wextra
            -g0
            -march=native
            -fvisibility=hidden
        )
    endif()
endfunction()

# out target
project(jing_bindings_project VERSION 0.0.1 LANGUAGES C)

# provided options
option(JING_BUILD_DEMO   "build jing_demo dynamic library for testing purpose"        ON)
option(JING_USE_WEPOLL   "provide wepoll related functions in jing_bindings library"  ON)
option(JING_USE_AWS_LC   "provide aws-lc related functions in jing_bindings library"  ON)
option(JING_USE_ZLIB_NG  "provide zlib-ng related functions in jing_bindings library" ON)

if(JING_BUILD_DEMO) 
    add_library(jing_demo SHARED)
    set_target_properties(jing_demo PROPERTIES
        RUNTIME_OUTPUT_DIRECTORY "${CMAKE_BINARY_DIR}"
        LIBRARY_OUTPUT_DIRECTORY "${CMAKE_BINARY_DIR}"
        ARCHIVE_OUTPUT_DIRECTORY "${CMAKE_BINARY_DIR}"
    )
    target_sources(jing_demo PRIVATE src/jing_demo.c)
    jing_configure(jing_demo)
endif()

set(JING_WEPOLL_VERSION    "v1.5.8"       CACHE STRING "wepoll library version"    FORCE)
set(JING_AWS_LC_VERSION    "v5.13.0"      CACHE STRING "aws-lc library version"    FORCE)
set(JING_ZLIB_NG_VERSION   "2.3.3"        CACHE STRING "zlib-ng   library version" FORCE)

add_library(jing_bindings SHARED)
set_target_properties(jing_bindings PROPERTIES
    RUNTIME_OUTPUT_DIRECTORY "${CMAKE_BINARY_DIR}"
    LIBRARY_OUTPUT_DIRECTORY "${CMAKE_BINARY_DIR}"
    ARCHIVE_OUTPUT_DIRECTORY "${CMAKE_BINARY_DIR}"
)
target_sources(jing_bindings PRIVATE src/jing_common.c src/jing_comp.c src/jing_ssl.c)
if(WIN32)
    target_sources(jing_bindings PRIVATE src/jing_win.c)
    target_link_libraries(jing_bindings PRIVATE ws2_32)
elseif(APPLE)
    target_sources(jing_bindings PRIVATE src/jing_posix.c src/jing_macos.c)
else()
    target_sources(jing_bindings PRIVATE src/jing_posix.c src/jing_linux.c)
endif()
jing_configure(jing_demo)
install(
    TARGETS jing_bindings
    RUNTIME DESTINATION "${JING_LIBRARY_PATH}"
    LIBRARY DESTINATION "${JING_LIBRARY_PATH}"
    ARCHIVE DESTINATION "${JING_LIBRARY_PATH}"
)

# fetch content
include(FetchContent)
set(FETCHCONTENT_BASE_DIR
    "${CMAKE_SOURCE_DIR}/.deps"
    CACHE PATH
    "base directory for thirdparty dependencies"
    FORCE
)

# setup wepoll
if(WIN32)
    if(JING_USE_WEPOLL)
        FetchContent_Declare(
            wepollproject
            GIT_REPOSITORY https://github.com/piscisaureus/wepoll.git
            GIT_TAG tags/${JING_WEPOLL_VERSION}
            GIT_SHALLOW TRUE
            SOURCE_SUBDIR none_exist
            EXCLUDE_FROM_ALL
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

#setup aws-lc
if(JING_USE_AWS_LC)
    target_compile_definitions(jing_bindings PRIVATE JING_USE_SSL)
    FetchContent_Declare(
        awslcproject
        GIT_REPOSITORY https://github.com/aws/aws-lc.git
        GIT_TAG tags/${JING_AWS_LC_VERSION}
        GIT_SHALLOW TRUE
        EXCLUDE_FROM_ALL
    )
    set(BUILD_LIBSSL      ON  CACHE BOOL "whether to build aws-lc libssl library" FORCE)
    set(BUILD_TESTING     OFF CACHE BOOL "whether to build aws-lc test targets"   FORCE)
    set(BUILD_TOOL        OFF CACHE BOOL "whether to build aws-lc bssl tool"      FORCE)
    message(STATUS "Configuring aws-lc")
    FetchContent_MakeAvailable(awslcproject)
    message(STATUS "Configuring aws-lc succeed")
    target_link_libraries(jing_bindings PRIVATE crypto ssl)
endif()

#setup zlib-ng
if(JING_USE_ZLIB_NG)
    target_compile_definitions(jing_bindings PRIVATE JING_ENABLE_ZLIB_NG)
    FetchContent_Declare(
        zlibngproject
        GIT_REPOSITORY https://github.com/zlib-ng/zlib-ng.git
        GIT_TAG tags/${JING_ZLIB_NG_VERSION}
        GIT_SHALLOW TRUE
        EXCLUDE_FROM_ALL
    )
    set(ZLIB_COMPAT   ON  CACHE BOOL "whether to compile with zlib compatible API"  FORCE)
    set(BUILD_TESTING OFF CACHE BOOL "whether to build zlib-ng tests"               FORCE)
    set(WITH_GTEST    OFF CACHE BOOL "whether to build tests using GTest framework" FORCE)
    message(STATUS "configuring zlib-ng")
    FetchContent_MakeAvailable(zlibngproject)
    message(STATUS "configuring zlib-ng succeed")
    target_link_libraries(jing_bindings PRIVATE zlib-ng)
endif()
