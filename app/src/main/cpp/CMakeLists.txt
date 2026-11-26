cmake_minimum_required(VERSION 3.22.1)
project(retrohero CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

add_library(retrohero SHARED
        jni_bridge.cpp
        frontend.cpp
        audio_output.cpp
        video_output.cpp
        hw_renderer.cpp
        rollback_netplay.cpp
        builtin_herobox16.cpp
        builtin_chip8.cpp
        builtin_nes.cpp)

target_include_directories(retrohero PRIVATE ${CMAKE_CURRENT_SOURCE_DIR}/libretro)
target_compile_options(retrohero PRIVATE -O3 -ffast-math -fno-rtti -Wall -Wextra -Wno-unused-parameter)
# 16 KB page alignment (required for Android 15+ devices with 16 KB pages).
target_link_options(retrohero PRIVATE "-Wl,-z,max-page-size=16384")
target_link_libraries(retrohero android log aaudio dl EGL GLESv3)
