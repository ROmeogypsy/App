cmake_minimum_required(VERSION 3.22.1)
project("stray_llama")

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# llama.cpp source directory
set(LLAMA_DIR ${CMAKE_CURRENT_SOURCE_DIR}/llama)

# Check if llama.cpp is present
if(EXISTS ${LLAMA_DIR}/CMakeLists.txt)
    # Build llama.cpp as a subdirectory
    set(LLAMA_BUILD_TESTS OFF CACHE BOOL "" FORCE)
    set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
    set(LLAMA_NATIVE OFF CACHE BOOL "" FORCE)
    add_subdirectory(${LLAMA_DIR} llama_build)

    # JNI bridge shared library
    add_library(
        stray_llama
        SHARED
        llama_bridge.cpp
    )

    target_include_directories(stray_llama PRIVATE
        ${LLAMA_DIR}
        ${LLAMA_DIR}/common
        ${LLAMA_DIR}/include
        ${LLAMA_DIR}/ggml/include
    )

    target_link_libraries(
        stray_llama
        llama
        android
        log
    )
else()
    # Stub library when llama.cpp is not present (CI / source-only build)
    message(WARNING "llama.cpp source not found at ${LLAMA_DIR}. Building stub JNI library.")

    add_library(
        stray_llama
        SHARED
        llama_bridge_stub.cpp
    )

    target_link_libraries(
        stray_llama
        android
        log
    )
endif()
