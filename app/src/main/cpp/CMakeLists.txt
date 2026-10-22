cmake_minimum_required(VERSION 3.10.2)
project("keepalive")

add_library(
        keepalive
        SHARED
        keepalive.c)

find_library(
        log-lib
        log)

target_link_libraries(
        keepalive
        ${log-lib})
