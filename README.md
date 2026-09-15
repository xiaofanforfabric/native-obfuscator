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
> ### ⚠️ 已知缺陷(使用前务必阅读)
>
> 本工具存在**若干继承自上游的已知缺陷**,其中两条会**在运行时直接崩溃**:
>
> | 编号 | 现象 | 严重度 |
> |---|---|---|
> | [KI-1](KNOWN_ISSUES.md#ki-1数组类型的类解析在隐藏类中必然失败) | 类初始化时报 `NoClassDefFoundError: [L你自己的类;` | **严重** |
> | [KI-4](KNOWN_ISSUES.md#ki-4-隐藏类定义进-bootstrap-域不同插件互相冲突) | 同机装多个本工具处理过的插件时 `LinkageError: duplicate class definition` | **严重** |
>
> **两条都可以不改代码规避**:
>
> - **KI-1**:用 `-b/--black-list` 把报错的类排除掉(见
>   `KNOWN_ISSUES.md` 中的检测脚本,可提前扫描出所有受影响的类);
> - **KI-4**:**每个**插件都用 `--custom-lib-dir <唯一名字>`(如插件名)转译。
>
> 完整清单、根因分析、实测影响范围与可重跑的复现脚本见
> **[`KNOWN_ISSUES.md`](KNOWN_ISSUES.md)**。
>
> ---
>
> 以下为上游原始 README 内容(用于说明本工具的基础功能)。

---

Currently, fully supports only Java 8. Java 9+ and Android support is entirely experimental

> **Note from this fork**: the sentence above is upstream's original statement. A large part of
> the "Java 9+ does not work" reputation comes from a single hardcoded line
> (`classNode.version = 52`) that rewrote every class to Java 8. Because HotSpot **silently
> ignores** class file attributes newer than the declared version, modern features (most
> visible: `record` classes) appeared to be broken. This fork preserves the original class
> version by default, so modern inputs keep their modern attributes. See the
> [`--class-version`](#arguments) option if you need the old behaviour back.
>
> Android support remains experimental as upstream stated.

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
                         [--class-version=<version>]
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
      --class-version=<version>
                          Class file version to write for the transpiled
                            classes.
                          Accepts a class file major version (>= 45, e.g. 52 =
                            Java 8, 61 = Java 17, 65 = Java 21)
                          or a Java release number (<= 44, e.g. 8, 11, 17, 21).
                          Default: preserve the original version of each class.
                          Note: forcing a version lower than the input makes
                            HotSpot silently ignore
                          attributes introduced later (e.g. Record below major
                            60 breaks Class#isRecord()).
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

`--class-version <version>` - class file version to write for the transpiled classes, optional

Accepts either a **class file major version** (`>= 45`, e.g. `52` = Java 8, `61` = Java 17, `65` = Java 21)
or a **Java release number** (`<= 44`, e.g. `8`, `11`, `17`, `21`). The two ranges never overlap, so
both spellings are unambiguous: `--class-version 8` and `--class-version 52` mean the same thing.

By default (**recommended**) the original version of each input class is preserved, so a Java 8 JAR
still produces version 52 output while a Java 17 JAR keeps major version 61 and therefore keeps all
of its modern attributes.

```bash
# preserve original class versions (default, recommended)
java -jar native-obfuscator.jar input.jar output/ -p hotspot

# force Java 8 output (legacy upstream behaviour)
java -jar native-obfuscator.jar input.jar output/ -p hotspot --class-version 8

# force Java 17 output
java -jar native-obfuscator.jar input.jar output/ -p hotspot --class-version 17
```

> **Why the default changed**: upstream unconditionally executed `classNode.version = 52`, which
> downgraded *every* class to Java 8 regardless of its input version. HotSpot ignores class file
> attributes that are newer than the version declared in the class file, and it does so
> **silently**. The most visible casualty is the `Record` attribute (only honoured from major
> version 60 / Java 16 onwards): a downgraded `record` class still contains the attribute, but
> `Class#isRecord()` returns `false` and `Class#getRecordComponents()` returns `null`. Use
> `--class-version 8` only if you specifically need the legacy behaviour.

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

> **本 Fork 说明:** 需要 **Gradle 8.14.2**(wrapper 已固定在该版本)。
> 上游曾把 wrapper 升到 9.x,但 `shadow` 插件 8.1.1 与 Gradle 9 不兼容,会导致
> `:obfuscator:shadowJar` 失败(`Could not add META-INF to ZIP`)。详见 `MODIFICATIONS.md`。

---

### Tests
You need to have [Krakatau](https://github.com/Storyyeller/Krakatau) installed to your PATH, because test suite is using `krak2` for some tests

1. Run `gradlew build` to assemble and run full test suite

> **本 Fork 说明:** 本 Fork 不再在 CI 中运行上游的测试矩阵(其测试依赖 Krakatau 与
> 多版本 JDK 工具链)。发布流程只构建 `:obfuscator:shadowJar`。
> 如需本地跑测试,请自行安装 Krakatau 后执行 `gradlew test`。

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
