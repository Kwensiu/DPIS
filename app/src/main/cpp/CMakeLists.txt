cmake_minimum_required(VERSION 3.22.1)

project(dpis_native)

add_library(dpis_native SHARED dpis_native.cpp)

target_compile_features(dpis_native PRIVATE cxx_std_20)
target_compile_options(dpis_native PRIVATE -Wall -Wextra -Werror)
target_link_libraries(dpis_native log dl)
