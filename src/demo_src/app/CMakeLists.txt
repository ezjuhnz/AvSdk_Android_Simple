# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.10.2)

# Declares and names the project.

project("previewdemo")

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.

add_library( # Sets the name of the library.
             native-lib

             # Sets the library as a shared library.
             SHARED

             # Provides a relative path to your source file(s).
        src/main/cpp/native-lib.cpp)

add_library( imageHelper    #库名字 去掉了 lib 与 .a
        SHARED   #必须的
        IMPORTED #必须的
        )

set_target_properties(
        imageHelper
        PROPERTIES IMPORTED_LOCATION
        ${CMAKE_CURRENT_SOURCE_DIR}/libs/${CMAKE_ANDROID_ARCH_ABI}/libimageHelper.so #库在的路径，以cmakeList.txt所在的路径起
)

add_library( cImageUtil    #库名字 去掉了 lib 与 .a
        SHARED   #必须的
        IMPORTED #必须的
        )

set_target_properties(
        cImageUtil
        PROPERTIES IMPORTED_LOCATION
        ${CMAKE_CURRENT_SOURCE_DIR}/libs/${CMAKE_ANDROID_ARCH_ABI}/libcImageUtil.so #库在的路径，以cmakeList.txt所在的路径起
)



# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

find_library( # Sets the name of the path variable.
              log-lib

              # Specifies the name of the NDK library that
              # you want CMake to locate.
              log )

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.

target_link_libraries( # Specifies the target library.
                       native-lib
                        imageHelper
                        cImageUtil
                       # Links the target library to the log library
                       # included in the NDK.
                       ${log-lib} )
