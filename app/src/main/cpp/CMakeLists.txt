# ═══════════════════════════════════════════════════════════════
#  CMakeLists.txt  —  Ludo Master Engine  (NDK Native Build)
#  Place this file at:  app/src/main/cpp/CMakeLists.txt
# ═══════════════════════════════════════════════════════════════
cmake_minimum_required(VERSION 3.22.1)
project("ludo_engine" VERSION 3.0 LANGUAGES CXX)

# ── C++17 required (std::thread, std::atomic, structured bindings) ──
set(CMAKE_CXX_STANDARD          17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS        OFF)

# ── Shared library that JNI will load ───────────────────────────
add_library(ludo_engine SHARED ludo_engine.cpp)

# ── Link Android system libraries ───────────────────────────────
find_library(log-lib  log)
find_library(android-lib android)
target_link_libraries(ludo_engine ${log-lib} ${android-lib})

# ── Aggressive release optimisations (mirrors Stockfish -O3) ───
target_compile_options(ludo_engine PRIVATE
    # Core speed flags
    -O3
    -ffast-math
    -funroll-loops
    -fomit-frame-pointer
    -fvisibility=hidden

    # ARM64 NEON SIMD (Cortex-A53/A55/A75/A78/X1/X2)
    $<$<STREQUAL:${ANDROID_ABI},arm64-v8a>:
        -march=armv8-a+simd
        -mtune=cortex-a55
    >

    # x86_64 SSE4.2
    $<$<STREQUAL:${ANDROID_ABI},x86_64>:
        -msse4.2
        -mpopcnt
    >

    # Release-only: disable asserts, add LTO
    $<$<CONFIG:Release>:
        -DNDEBUG
        -flto
    >
    # Debug: sanitizers
    $<$<CONFIG:Debug>:
        -fsanitize=address
        -g3
    >
)

# ── LTO linker support ───────────────────────────────────────────
target_link_options(ludo_engine PRIVATE
    $<$<CONFIG:Release>:-flto>
)
