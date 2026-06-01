cmake_minimum_required(VERSION 3.22.1)
project("ludo_engine" VERSION 3.0 LANGUAGES CXX)

set(CMAKE_CXX_STANDARD          17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS        OFF)

add_library(ludo_engine SHARED ludo_engine.cpp)

find_library(log-lib  log)
find_library(android-lib android)
target_link_libraries(ludo_engine ${log-lib} ${android-lib} atomic m)

target_compile_options(ludo_engine PRIVATE
    -O2
    -ffast-math
    -funroll-loops
    -fomit-frame-pointer
    -fvisibility=hidden
    -DNDEBUG
    $<$<STREQUAL:${ANDROID_ABI},arm64-v8a>:
        -march=armv8-a+simd
        -mtune=cortex-a55
    >
    $<$<STREQUAL:${ANDROID_ABI},x86_64>:
        -msse4.2
        -mpopcnt
    >
)
