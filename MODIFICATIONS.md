# Native Obfuscator 扩展分支 (Extended Fork)

本仓库是 [radioegor146/native-obfuscator](https://github.com/radioegor146/native-obfuscator) 的一个开源扩展分支,
以 GPL-3.0 授权,可自由使用、修改与再分发。

## Fork 关系(Fork Relationship)

```
┌─────────────────────────────────────────────────────────────────┐
│  上游原项目 (Upstream / Original)                               │
│                                                                 │
│    native-obfuscator                                            │
│    https://github.com/radioegor146/native-obfuscator            │
│                                                                 │
│    作者      : radioegor146 and contributors                    │
│    版本      : v3.5.4r                                          │
│    许可证    : GNU GPL v3.0                                     │
└───────────────────────────┬─────────────────────────────────────┘
                            │
                            │  fork (2026-09-16)
                            │  分叉自上述提交: e481761
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│  本 Fork (This Fork)                                            │
│                                                                 │
│    native-obfuscator (Extended Fork)                            │
│    https://github.com/xiaofanforfabric/native-obfuscator        │
│                                                                 │
│    维护者    : xiaofanforfabric                                 │
│    基线      : v3.5.4r                                          │
│    许可证    : GNU GPL v3.0 (与上游一致)                        │
│                                                                 │
│    相对上游新增:                                                │
│      ★ 输出可自由修改的 Java + C++ 源工程                       │
│      ★ 修复构建:Gradle 版本降回 8.14.2 (上游 9.x 无法构建)      │
│      ★ 标签自动发布 GitHub Release                              │
│      ★ GPL-3.0 合规材料 (NOTICE / MODIFICATIONS.md)            │
│      ★ 行为与上游完全兼容,原有输出流程未改动                    │
└─────────────────────────────────────────────────────────────────┘
```

> **本 Fork 与上游的关系**:本仓库是上游的**派生作品(derivative work)**,
> 保留了上游全部原始版权声明,并**额外**增加了上述新功能。
> 本仓库**不是**上游官方仓库,上游作者**不对**本 Fork 的改动负责。

## 原始项目信息

- **原作者**: radioegor146
- **原仓库**: https://github.com/radioegor146/native-obfuscator
- **许可证**: GPL 3.0
- **版本**: v3.5.4r (基础版本)

## 本仓库(Fork)信息

- **维护者**: xiaofanforfabric
- **本仓库**: https://github.com/xiaofanforfabric/native-obfuscator
- **定位**: 通用开源工具 —— 将 Java 字节码转译为等价的 C++/JNI 原生代码
- **许可证**: GPL 3.0(与上游一致)

## 许可证说明

本项目继续使用 **GPL 3.0** 许可证,与原项目保持一致。

**重要**: 由于 GPL 3.0 的传染性,任何使用本工具的项目如果静态链接或直接集成,也必须使用 GPL 3.0 许可证。

本工具设计为**独立的命令行程序**,推荐通过**进程隔离**的方式调用,以隔离许可证影响:
```
你的程序 (任意许可证) --exec/fork--> native-obfuscator (GPL 3.0)
```

## GPL 合规指引

### 何时触发开源义务

GPL-3.0 的义务**只在「分发」(convey / distribute)时触发**:

| 场景 | 是否触发义务 |
|---|---|
| 自己 clone、修改、自用 | 否 |
| 组织内部自用(同一法人实体) | 通常否 |
| 把二进制发给外部用户 / 发布到网上 / 随产品交付 | **是** |

### 分发时必须做的 4 件事

1. **保留 GPL-3.0 许可证** —— 不得换成其他许可证,不得闭源。见根目录 `LICENSE`。
2. **保留原作者版权声明** —— 见根目录 `NOTICE`。
3. **醒目标注修改**(GPL-3.0 §5(a))—— 每个被修改的源文件顶部必须声明「已被修改」并注明修改者与日期。
   本仓库已对 `NativeObfuscator.java` 完成。
4. **提供完整对应源码**(Complete Corresponding Source)—— 二进制接收者有权获得源码,三选一:
   - 提供公开可访问的仓库地址;或
   - 随二进制附带完整源码副本;或
   - 提供书面的源码获取承诺(有效期三年)。

### 交付检查清单

- [ ] `LICENSE`(GPL-3.0 全文)随分发物一起提供
- [ ] `NOTICE`(版权归属)随分发物一起提供
- [ ] `MODIFICATIONS.md`(变更记录)随分发物一起提供
- [ ] 所有修改过的源文件顶部有醒目变更声明
- [ ] 用户能通过某个渠道获取完整对应源码
- [ ] 未删除或篡改上游版权头

### 与调用方程序的关系

本工具是**独立的命令行程序**,与调用它的程序通过进程隔离解耦:

- 若**只分发你自己的程序**、未捆绑本工具 → 本工具的 GPL 义务不涉及你(用户需自备本工具)。
- 若**把本工具的 JAR 一起打包分发** → 属于分发本工具,**必须同时提供本工具的完整源码**。

> ⚠️ 以上为通用合规说明,非法律意见。涉及商业分发建议咨询专业律师。

## 架构关系

```
┌─────────────────────────────────────┐
│  调用方程序 (任意许可证)            │
│  - 功能: GUI 界面 / 进程管理 / ...  │
└──────────────┬──────────────────────┘
               │
               │ QProcess::start() / ProcessBuilder
               │ 参数: -jar native-obfuscator.jar
               │ 通信: stdout/stderr
               │
┌──────────────▼──────────────────────┐
│  native-obfuscator (本仓库)         │
│  - 许可证: GPL 3.0                  │
│  - 语言: Java + Gradle              │
│  - 功能: .class → .cpp 转换         │
└─────────────────────────────────────┘
```

## 修改记录

### 未来计划修改

以下是计划中的修改方向 (尚未实施):

1. **性能优化**
   - [ ] 增量编译支持 (只转换修改过的类)
   - [ ] 多线程并行处理
   - [ ] 缓存机制 (避免重复转换相同的类)

2. **输出格式改进**
   - [ ] 添加 JSON 格式进度输出
   - [ ] 结构化日志输出
   - [ ] 统计信息输出 (转换时间、类数量等)

3. **功能增强**
   - [ ] 支持 Java 11+ 新特性
   - [ ] 改进 Android 平台支持
   - [ ] 添加更多混淆选项

4. **可用性提升**
   - [ ] 更友好的错误提示
   - [ ] 配置文件支持 (YAML/JSON)
   - [ ] 交互式配置向导

### 已实施的修改

#### v1.3.0 - 2026-09-16 - 修复 Gradle 版本(构建可用)+ 标签自动发布

**修改者**: xiaofanforfabric
**本仓库**: https://github.com/xiaofanforfabric/native-obfuscator

**问题**: 上游提交 `5cf558a Update Gradle wrapper to version 9.3.1` 把 wrapper 升到了
Gradle 9.3.1,但 `shadow` 插件仍是 `8.1.1`,两者不兼容。结果是 `./gradlew build` /
`./gradlew assemble` 必然失败:

```
Execution failed for task ':obfuscator:shadowJar'
> Could not add META-INF to ZIP
```

也就是说,上游的构建说明在当前代码状态下是失效的。

**改动文件**:

- `gradle/wrapper/gradle-wrapper.properties`
  - `distributionUrl` 从 `gradle-9.3.1-all.zip` 改为 `gradle-8.14.2-bin.zip`。
  - 新增 `distributionSha256Sum`(Gradle 发行包完整性校验)。
- `gradle/wrapper/gradle-wrapper.jar` / `gradlew` / `gradlew.bat`
  - 用 Gradle 8.14.2 重新生成,保证 wrapper 与发行包版本匹配。
- `.github/workflows/main.yml` —— **删除**
  - 上游的多平台测试矩阵(JDK 8/11/17/21/25 × ubuntu/macos/windows,
    另加 macOS-13 for JDK8),需要额外安装 Krakatau(Rust 工具链)才能跑通,
    对本 Fork 的维护成本大于收益。
- `.github/workflows/release.yml` —— **新增**
  - 推送标签(`v*` 或纯数字开头,如 `3.5.4r`)时自动构建并发布 GitHub Release。
  - 走 `./gradlew`(wrapper 已固定在 8.14.2),顺带在 CI 里守住"不能用 Gradle 9"这个约束。
  - 只构建 `:obfuscator:shadowJar`,不跑测试套件(因此 CI 无需安装 Krakatau)。
  - 构建后执行 `java -jar ... --version` 做产物自检。
  - Release 资产:
    - `native-obfuscator-<tag>.zip` —— 工具 JAR + `LICENSE`/`NOTICE`/`README`/`MODIFICATIONS.md`
    - `native-obfuscator-<tag>-src.zip` —— 该标签的完整对应源码(`git archive`,满足 GPL-3.0)
    - `SHA256SUMS.txt` —— 校验和
  - 幂等:同一标签重跑会覆盖旧资产并更新说明,不报错。
  - 预发布识别:标签含 `-`(如 `1.0.0-beta`)或以 `b` 结尾(本项目 beta 约定,如 `3.5.2b`)。
- `README.md` / `MODIFICATIONS.md` —— 同步更新构建与发布说明。

**验证**: 本地用 `./gradlew clean :obfuscator:shadowJar` 构建通过,
产出 `obfuscator/build/libs/obfuscator.jar`(约 2.7 MB),
`java -jar obfuscator.jar --version` 输出 `native-obfuscator 3.5.4r`。

#### v1.1.0 - 2026-09-16 - 输出可自由修改的 Java + C++ 源项目

**修改者**: xiaofanforfabric
**本仓库**: https://github.com/xiaofanforfabric/native-obfuscator

**目标**: 上游只把加载器以编译后的 `Loader.class`(黑盒)写入输出 JAR,
用户无法修改加载器逻辑。本修改让工具额外输出一套**完整的、可自由修改的源码工程**。

**改动文件**:

- `obfuscator/src/main/java/by/radioegor146/NativeObfuscator.java`
  - 新增私有方法 `emitSourceProject(...)`,在写出 `native{N}/Loader.class` 之后调用。
  - 保持原有 JAR 输出流程**完全不变**(向后兼容),仅**额外**写出源工程。
- 新增资源模板:
  - `obfuscator/src/main/resources/compiletime/LoaderUnpack.java.template`
  - `obfuscator/src/main/resources/compiletime/LoaderPlain.java.template`
  - `obfuscator/src/main/resources/sources/rebuild_project.sh.template`
  - `obfuscator/src/main/resources/sources/SOURCE_PROJECT.md.template`

**新增输出结构**(位于输出目录,与 `<jar名>` 同级):

```
output/
├── <jar名>                     # 混淆后的 JAR(不变)
├── build.sh                    # 一键: 编译C++ + 编译Loader.java + 重新打包
├── SOURCE_PROJECT.md           # 使用说明
├── java/
│   └── native{N}/
│       └── Loader.java         # 可自由修改的加载器源码
└── cpp/                        # 转译出的 C++ 源码(原有)
    ├── CMakeLists.txt
    ├── native_jvm.cpp/hpp
    ├── native_jvm_output.cpp
    ├── string_pool.cpp/hpp
    └── output/*.cpp/hpp
```

**Loader.java 生成逻辑**:

- 读取对应 `.java.template`,把 `package by.radioegor146.compiletime;` 替换为
  实际的 `package native{N};`(与写入 JAR 的 `native{N}/Loader.class` 一致)。
- 把类名 `LoaderUnpack` / `LoaderPlain` 统一替换为 `Loader`。
- 若使用 `--plain-lib-name`,把 `%LIB_NAME%` 替换为实际的库名。
- 编辑后运行 `build.sh` 即可重新编译并替换 JAR 中的 `Loader.class` 与原生库。

**流程说明**: 这**不会**改变原有的转译行为;`getResourceAsStream("compiletime/LoaderUnpack.class")`
读取的仍是编译后的模板类。新增的 `.java` 仅作为**可编辑源码副本**输出。

**许可证**: 本次修改仍在 `native-obfuscator/` 独立仓库内,继续遵循 GPL 3.0。
输出的 `Loader.java` 头注释已标明来源与许可证。

#### v1.0.0 - 2024-01-15 (初始 fork)
- 从 radioegor146/native-obfuscator v3.5.4r fork
- 无代码修改,仅用于独立部署

## 构建说明

### 环境要求

- **JDK 17**(CI 与本地验证所用版本;源码 target 为 Java 1.8)
- **Gradle 8.14.2** —— 由 `gradle/wrapper/gradle-wrapper.properties` 固定,
  直接用 `./gradlew` 即可,无需自行安装
- CMake 3.x (用于 C++ 编译)
- C++ 编译器 (GCC/Clang/MSVC)

> ⚠️ 不要使用 Gradle 9.x。`shadow` 插件 8.1.1 与 Gradle 9 不兼容,会导致
> `:obfuscator:shadowJar` 报 `Could not add META-INF to ZIP`。

### 构建步骤

```bash
# 1. 克隆仓库
git clone https://github.com/xiaofanforfabric/native-obfuscator.git
cd native-obfuscator

# 2. 构建 fat jar(只打包,不跑测试)
./gradlew clean :obfuscator:shadowJar

# 3. 查找输出
ls -lh obfuscator/build/libs/obfuscator.jar
```

### 输出文件

构建完成后,可执行的 fat jar 位于:
```
obfuscator/build/libs/obfuscator.jar
```

将此文件复制到你的程序目录中使用:
```bash
cp obfuscator/build/libs/obfuscator.jar /path/to/your-app/lib/native-obfuscator.jar
```

### 发布

推送标签即会自动构建并发布 Release(见 `.github/workflows/release.yml`):

```bash
git tag v1.0.0
git push origin v1.0.0     # ⚠️ 不要用 git push --tags,仓库里有大量上游历史标签
```

Release 会附带:分发包 `native-obfuscator-<tag>.zip`、对应源码
`native-obfuscator-<tag>-src.zip`、以及 `SHA256SUMS.txt`。

## 使用方式

### 命令行使用

```bash
java -jar native-obfuscator.jar input.jar output/ \
    -b blacklist.txt \
    -w whitelist.txt \
    -l libraries/ \
    -p hotspot \
    -a
```

### 参数说明

| 参数 | 说明 | 示例 |
|------|------|------|
| `<input>` | 输入 JAR 文件 | `input.jar` |
| `<output>` | 输出目录 | `output/` |
| `-b` | 黑名单文件 (不转换的类) | `-b blacklist.txt` |
| `-w` | 白名单文件 (只转换的类) | `-w whitelist.txt` |
| `-l` | 依赖库目录 | `-l libs/` |
| `-p` | 目标平台 | `-p hotspot` (hotspot/std_java/android) |
| `-a` | 使用注解模式 | `-a` |
| `--debug` | 调试模式 | `--debug` |
| `--custom-lib-dir` | 自定义库目录 | `--custom-lib-dir mylibs/` |

### 进程调用示例

从你的程序中通过进程方式调用(以 C++/Qt 为例):

```cpp
QProcess *proc = new QProcess(this);
proc->setProgram("java");
proc->setArguments({
    "-jar", "/path/to/native-obfuscator.jar",
    "input.jar", "output/",
    "-p", "hotspot"
});
proc->start();
```

## 开发指南

### 代码结构

```
native-obfuscator/
├── annotations/          # 注解支持
├── obfuscator/          # 主要代码
│   └── src/main/java/
│       └── by/radioegor146/
│           ├── Main.java           # 入口点
│           ├── Obfuscator.java     # 核心混淆器
│           ├── MethodProcessor.java # 方法处理
│           └── ...
├── build.gradle         # 构建配置
└── settings.gradle      # Gradle 设置
```

### 调试技巧

```bash
# 启用详细日志
java -jar native-obfuscator.jar input.jar output/ --debug

# 查看 Gradle 任务
./gradlew tasks

# 运行测试
./gradlew test

# 清理构建
./gradlew clean
```

## 贡献指南

### 提交修改

1. 创建功能分支: `git checkout -b feature/my-improvement`
2. 提交修改: `git commit -am "Add: description"`
3. 推送分支: `git push origin feature/my-improvement`
4. 创建 Pull Request

### 提交信息格式

```
<类型>: <简短描述>

<详细说明>

<相关 Issue>
```

类型:
- `Add`: 新增功能
- `Fix`: 错误修复
- `Improve`: 性能/代码改进
- `Docs`: 文档更新
- `Refactor`: 代码重构

### 测试要求

提交前请确保:
- [ ] 代码通过 `./gradlew build`
- [ ] 测试用例通过
- [ ] 更新相关文档
- [ ] 遵循原项目代码风格

## 许可证与归属

### 原项目归属

本项目基于 radioegor146/native-obfuscator,所有原始代码版权归原作者所有。

### GPL 3.0 义务

根据 GPL 3.0 许可证,使用本工具时:

✅ **允许**:
- 自由使用、修改、分发
- 商业使用
- 私有使用

⚠️ **要求**:
- 必须开源修改后的代码
- 必须保持 GPL 3.0 许可证
- 必须声明修改内容
- 必须提供源代码访问

❌ **禁止**:
- 闭源分发
- 更改许可证
- 专利诉讼

### 免责声明

本工具按 "原样" 提供,不提供任何明示或暗示的保证。作者不对使用本工具造成的任何损失负责。

## 联系方式

- **原项目**: https://github.com/radioegor146/native-obfuscator
- **本 Fork**: https://github.com/xiaofanforfabric/native-obfuscator

## 致谢

感谢 [@radioegor146](https://github.com/radioegor146) 创建了这个优秀的工具!

---

**最后更新**: 2026-09-16  
**维护者**: xiaofanforfabric  
**许可证**: GPL 3.0 (与上游一致)
