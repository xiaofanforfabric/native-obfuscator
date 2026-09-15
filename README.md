# native-obfuscator
Java .class to .cpp converter for use with JNI

---

> ## 🔀 关于本仓库(About this repository)
>
> **这是一个 Fork,不是上游原项目。**
>
> | | |
> |---|---|
> | **上游原项目** | [radioegor146/native-obfuscator](https://github.com/radioegor146/native-obfuscator) |
> | **原作者** | radioegor146 and contributors |
> | **本 Fork** | [xiaofanforfabric/native-obfuscator](https://github.com/xiaofanforfabric/native-obfuscator) |
> | **本 Fork 维护者** | xiaofanforfabric |
> | **基线版本** | v3.5.4r |
> | **许可证** | GNU GPL v3.0(与上游一致) |
>
> ### 本 Fork 相对上游新增了什么?
>
> **输出可自由修改的 Java + C++ 源工程**(新增功能,完全向后兼容)。
>
> 上游只把加载器以编译后的 `Loader.class`(黑盒)写进输出 JAR,无法修改。
> 本 Fork 在此基础上**额外**输出一套完整、可自由编辑的源码工程:
>
> ```
> output/
> ├── <jarName>                    # 混淆后的 JAR(与上游一致)
> ├── build.sh                     # 一键: 编译C++ + 编译Loader.java + 重新打包
> ├── SOURCE_PROJECT.md            # 使用说明
> ├── java/nativeN/Loader.java     # 可自由修改的加载器源码
> └── cpp/                         # 转译出的 C++ 源码(上游已有)
> ```
>
> **原有 JAR 输出流程未做任何改动**,因此本 Fork 与上游在行为上完全兼容。
>
> ### 变更与许可
>
> - 详细的变更记录、GPL-3.0 合规说明见 [`MODIFICATIONS.md`](MODIFICATIONS.md)
> - 版权归属与源码获取方式见 [`NOTICE`](NOTICE)
> - 本 Fork 继续以 **GPL-3.0** 授权,任何修改与分发均需遵循该许可证
> - 上游原始作品的版权归原作者及贡献者所有,本 Fork 保留其全部原始版权声明
>
> ---
>
> 以下为上游原始 README 内容(用于说明本工具的基础功能)。

---

Currently, fully supports only Java 8. Java 9+ and Android support is entirely experimental

Warning: blacklist/whitelist usage is recommended because this tool slows down code significantly (like do not obfuscate full Minecraft .jar)

Also, this tool does not particularly obfuscate your code; it just transpiles it to native. Remember to use protectors like VMProtect, Themida, or obfuscator-llvm (in case of clang usage)

---

### To run this tool, you need to have these tools installed:
1. JDK 8

    - For Windows:
        
        I recommend downloading Oracle JDK 8, though you need to have some login credentials on Oracle.
    - For Linux/MacOS:
    
        Google "your distro install jdk 8", and install the required packages
2. CMake
   
    - For Windows:
     
        Download the latest release from [CMake](https://cmake.org/download/)
    
    - For Linux/MacOS:
    
        Google "your distro install cmake" and install the required package (default - `apt/yum/brew install cmake`)
3. C++/C compiler toolchain

    - For Windows:
    
        Download the freeware version of MSVS from [Microsoft](https://visualstudio.microsoft.com/ru/)
        and select Visual C++ compiler in opt-ins
      
        Or install mingw if you have any experience with this.
     
    - For Linux/MacOS:
        
        Google "your distro install g++"
      
---

### General usage:
```
Usage: native-obfuscator [-ahV] [--debug] [-b=<blackListFile>]
                         [--custom-lib-dir=<customLibraryDirectory>]
                         [-l=<librariesDirectory>] [-p=<platform>]
                         [--plain-lib-name=<libraryName>] [-w=<whiteListFile>]
                         <jarFile> <outputDirectory>
Transpiles .jar file into .cpp files and generates output .jar file
      <jarFile>           Jar file to transpile
      <outputDirectory>   Output directory
  -a, --annotations       Use annotations to ignore/include native obfuscation
  -b, --black-list=<blackListFile>
                          File with a list of blacklist classes/methods for
                            transpilation
      --custom-lib-dir=<customLibraryDirectory>
                          Custom library directory for LoaderUnpack
      --debug             Enable generation of debug .jar file (non-executable)
  -h, --help              Show this help message and exit.
  -l, --libraries=<librariesDirectory>
                          Directory for dependent libraries
  -p, --platform=<platform>
                          Target platform: hotspot - standard standalone
                            HotSpot JRE, std_java - java standard, android -
                            for Android builds (w/o DefineClass)
      --plain-lib-name=<libraryName>
                          Plain library name for LoaderPlain
  -V, --version           Print version information and exit.
  -w, --white-list=<whiteListFile>
                          File with a list of whitelist classes/methods for
                            transpilation
```

#### Arguments:
`<jarFile>` - input .jar file to obfuscate

`<outputDirectory>` - output directory where C++/new .jar file where be created

`-l <librariesDirectory>` - directory where dependant libraries should be, optional, but preferable

`-p <platform>` - JVM platform to run library on

Three options are available:
 - `hotspot`: will use HotSpot JVM internals and should work with most obfuscators (even with stack trace checking as well)
 - `std_java`: will use only minor JVM internals that must be available on all JVMs
 - `android`: use this method when building library for Android. Will use no JVM internals, as well as no DefineClass for hidden methods (obfuscators that rely on stack for string/name obfuscator will not work due to the fact that some methods will not be hidden)

`-a` - enable annotation processing

To use annotations for black/whitelisting methods/classes as `native` you can add the following library to your project:

`com.github.radioegor146.native-obfuscator:annotations:master-SNAPSHOT`

Also, you need to add [JitPack](https://jitpack.io) to your repositories.

You can add `@Native` annotation to include classes/methods to the native obfuscation process and add `@NotNative` annotation to ignore methods in classes marked as `@Native`

Whitelist/Blacklist has higher priority than annotations.

`-w <whiteList>` - path to .txt file for whitelist of methods and classes if required

`-b <blackList>` - path to a .txt file for a blacklist of methods and classes if required

Both of them should come in such form:
```
<class>
<class>#<method name>#<method descriptor>
mypackage/myotherpackage/Class1
mypackage/myotherpackage/Class1#doSomething!()V
mypackage/myotherpackage/Class1$SubClass#doOther!(I)V
```
It uses internal names of classes and method descriptors for filtering (you can read more about it by googling "java internal class names" or "java method descriptors")

Also, you can use a wildcard matcher like these:
```
mypackage/myotherpackage/*
mypackage/myotherpackagewithnested/**
mypackage/myotherpackage/*/Class1
mypackage/myotherpackagewithnested/**/Class1
mypackage/myotherpackage/Class*
```
`*` matches a single entry (divided by `/`) in the class/package name

`**` matches all entries in class/package name


`--plain-lib-name` - if you ship your .jar separately from the result native libraries, or you use it for Android, you can specify the name of the native library that it will try to search while using.

`--custom-lib-dir` - if you want to set custom directory for storing libraries inside the jar

If you want to ship your .jar with native libraries in it, you should omit that argument, and after building native files, add them in the form of
```
x64-windows.dll
x64-linux.so
x86-windows.dll
x64-macos.dylib
arm64-linux.so
arm64-windows.dll
```
to the directory of the .jar file that this tool will print in `stdout` (by default `native0/` or custom if `--custom-lib-dir` is present)

#### Basic usage:
1. Transpile your code using `java -jar native-obfuscator.jar <input jar> <output directory>`
2. Run `cmake .` in the result `cpp` directory
3. Add changes to .cpp code if necessary
4. Run `cmake --build . --config Release` in result `cpp` directory to build .so/.dll file
5. Copy result .dll/.so from `build/libs/` to the path specified in the previous paragraph.
6. Run created .jar `java -jar <output jar>` and enjoy!

---

### Building the tool by yourself
1. Run `gradlew assemble` to force gradle not to run tests after the build

---

### Tests
You need to have [Krakatau](https://github.com/Storyyeller/Krakatau) installed to your PATH, because test suite is using `krak2` for some tests

1. Run `gradlew build` to assemble and run full test suite

This tool uses tests from [huzpsb/JavaObfuscatorTest](https://github.com/huzpsb/JavaObfuscatorTest)

---

---

## 📌 联系与反馈(本 Fork)

> 下面这部分**已被本 Fork 改写**,不再指向上游作者。

- **本 Fork Issues**(报告本 Fork 引入的问题、请求新功能):
  https://github.com/xiaofanforfabric/native-obfuscator/issues
- **上游项目 Issues**(报告与本工具底层转译相关的问题):
  https://github.com/radioegor146/native-obfuscator/issues
- **上游作者联系方式**: [re146.dev](https://re146.dev)(仅限上游原项目)

> ⚠️ 请勿就本 Fork 引入的改动(如「输出可修改源工程」)去打扰上游作者。

### 上游 Stargazers over time

[![Stargazers over time](https://starchart.cc/radioegor146/native-obfuscator.svg?variant=adaptive)](https://starchart.cc/radioegor146/native-obfuscator)
