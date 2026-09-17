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
│    基线      : v3.5.5r                                          │
│    许可证    : GNU GPL v3.0 (与上游一致)                        │
│                                                                 │
│    相对上游新增:                                                │
│      ★ 输出可自由修改的 Java + C++ 源工程                       │
│      ★ 构建可用:同步上游 Gradle 9 修复 (v1.4.4)                 │
│      ★ 标签自动发布 GitHub Release                              │
│      ★ GPL-3.0 合规材料 (NOTICE / MODIFICATIONS.md)             │
│      ★ KNOWN_ISSUES.md:记录并规避上游遗留缺陷                   │
│      ★ scripts/repro-ki1.sh, repro-ki4.sh:缺陷可复现脚本        │
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
- [ ] `KNOWN_ISSUES.md`(已知缺陷)随分发物一起提供(便于使用方规避)
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

## 已知缺陷 (Known Issues)

本 Fork 目前**包含若干继承自上游的已知缺陷**,已在真实项目中验证并整理成册,
见根目录 **[`KNOWN_ISSUES.md`](KNOWN_ISSUES.md)**。

| 编号 | 缺陷 | 严重度 | 状态 |
|---|---|---|---|
| KI-1 | 数组类型的类解析在隐藏类中必然失败 | **严重**(已确认线上崩溃) | 已定位,未修复 |
| KI-2 | `MULTIANEWARRAY` 多维分支存在同源缺陷 | 中 | 已定位,尚未触发 |
| KI-3 | `HiddenMethodsPool` 方法名与计数器无分隔符拼接 | 低(代码异味) | 当前无害 |
| KI-4 | 隐藏类定义进 bootstrap 域,不同插件互相冲突 | **严重**(已实证) | 已定位,未修复 |

这些缺陷**均为上游 v3.5.4r 固有**,不是本 Fork 引入的。本 Fork 只做记录,不改变
转译行为。`KNOWN_ISSUES.md` 中给出了:可独立复现的 JNI 最小实验、全量扫描脚本、
实测影响范围,以及**不改代码即可生效的黑名单 / `--custom-lib-dir` 规避方案**。

其中 KI-1 与 KI-4 的复现已脚本化,可直接重跑(需要已构建好的 `obfuscator.jar`):

```bash
bash scripts/repro-ki1.sh obfuscator/build/libs/obfuscator.jar   # 数组类型 NoClassDefFoundError
bash scripts/repro-ki4.sh obfuscator/build/libs/obfuscator.jar   # 双插件 bootstrap 域撞名
```

两个脚本都会自动完成「生成源码 → 转译 → 编译 `.so` → 打包 JAR → 运行」,
并打印「加规避 / 不加规避」的 A/B 对照结论。

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

5. **修复上游遗留缺陷**(详见 [`KNOWN_ISSUES.md`](KNOWN_ISSUES.md))
   - [ ] KI-1:数组类型改用显式 `classloader` 解析,不再走隐藏类里的 `FindClass`
   - [ ] KI-4:隐藏类名加入内容哈希,避免不同插件在 bootstrap 域撞名
   - [ ] KI-2:多维数组分支与 KI-1 同源,一并修掉
   - [ ] KI-3:方法名与计数器之间加分隔符

   > 这些修复会**改变 `classIndex` 分配**,属于破坏性变更,需要单独一个版本发布。

### 已实施的修改

#### v1.4.7 - 2026-09-17 - 输给客户的 SOURCE_PROJECT.md 不再报基线版本号

**修改者**: xiaofanforfabric
**本仓库**: https://github.com/xiaofanforfabric/native-obfuscator

**背景**: 版本号是对抗逆向时最敏感的信息 —— 逆向者一旦在目标程序里看到
「某某工具 1.2.3」,就能直接去找对应版本的一键脱壳脚本,连工具链都不用猜。

**核查结果(实测)**: native-obfuscator **本身不往产物里写任何版本号**。
把测试用例完整加壳(含 C++ 编译)后,对最终 JAR 与 `.so` 做指纹扫描:

```
native-obfuscator  → 0 命中
radioegor146       → 0 命中
xiaofanforfabric   → 0 命中
1.0.0 / 3.5.x      → 0 命中
```

原因是版本号与文件名都只出现在**注释**里,注释不进二进制。所以对抗面本身是干净的。

**改动**: 唯一会跟着产物走的版本号是 `SOURCE_PROJECT.md` 模板里的「基线版本」一行,
把它删掉。

- `resources/sources/SOURCE_PROJECT.md.template` —— 删除「基线版本」表格行;
  「许可证」行补上 Output Exception 说明。
- 本条目。

**刻意保留(已确认)**: 工具名与 GitHub 链接**继续留在**这几个文件里
(`SOURCE_PROJECT.md`、`build.sh`、`cpp/*`、`Loader.java`)。理由:

1. 它们只随**输出目录**走,不进终端用户那份 JAR(上面扫描已证明);
2. 客户本来就知道自己用的是哪个工具;
3. 它们是 GPL 的**溯源记录**,也是 Output Exception 对用户可见的入口 ——
   删掉会削弱合规性,却换不来实际的对抗收益。

#### v1.4.6 - 2026-09-17 - 客户产物里的许可证声明改为「适用 Output Exception」

**修改者**: xiaofanforfabric
**本仓库**: https://github.com/xiaofanforfabric/native-obfuscator

**问题**: `SOURCE_PROJECT.md.template` 的「许可证与归属」一节写着:

> 修改与再分发需遵循 **GNU GPL v3.0**

这在 Output Exception 合入上游(v3.5.5r / PR #102)之后**低估了客户的权利** ——
例外明确允许把工具输出的 runtime 代码以任意条款链接、嵌入、编译与分发。
这句话等于在吓自己的客户,方向恰好与例外相反。

**改动**:

- `resources/sources/SOURCE_PROJECT.md.template` —— 「许可证与归属」一节改为
  **引用例外原文** + 指向上游 `LICENSE`,不再自行解释。措辞刻意保守:
  - 直接贴 Output Exception 原文,让读者自己看范围
  - 明确「例外只覆盖工具**输出**的代码;工具本身仍是完整 GPL-3.0」
  - 加一段**范围说明**:例外原文写的是 "the runtime code emitted by this tool",
    即 `java/` 与 `cpp/`;而 `SOURCE_PROJECT.md` 与 `build.sh` 是本 Fork 额外输出的
    辅助文件,**不在该例外的字面范围内**。不做超出原文的授权声明 ——
    否则等于给出自己兑现不了的承诺。将来若要明确授予,应先在上游把范围写清楚。
- 本条目。

**验证**: `./gradlew clean :obfuscator:shadowJar` 通过;实际混淆一个 JAR 后检查输出
目录里的 `SOURCE_PROJECT.md`,新的许可证段落已正确写入。

#### v1.4.5 - 2026-09-17 - 版本号改为跟随本 Fork 的发布标签

**修改者**: xiaofanforfabric
**本仓库**: https://github.com/xiaofanforfabric/native-obfuscator

**问题**: `--version` / `-V` 参数虽然存在,但值来自 `Main.java` 里硬编码的
`VERSION = "3.5.4r"`,而上游从 3.5.4r 之后就没再更新过这个字符串(发 3.5.5r 时也没改)。
结果是**任何两个版本的 jar 都印同一个版本号**,无法据此判断手上这份是不是最新:

```
上游原版   2,762,655 字节   --version → native-obfuscator 3.5.4r
Fork 构建  2,804,946 字节   --version → native-obfuscator 3.5.4r
```

另注:jar 内既无 `Implementation-Version`(MANIFEST),也无本项目自己的
`META-INF/maven/.../pom.properties`,所以当时没有别的地方能读到版本。

**改动**:

- `Main.java` —— `VERSION` 由 `"3.5.4r"` 改为 `"1.4.5"`,并补注释说明它
  **必须与发布用的 git 标签一致**(标签 `v1.4.5` ↔ 常量 `1.4.5`)。
- `resources/sources/SOURCE_PROJECT.md.template` —— 输给用户的 `SOURCE_PROJECT.md`
  里「基线版本」仍写着 `v3.5.4r`,同步为 `v3.5.5r`(v1.4.4 合并上游时漏改)。
- 本条目。

**为什么这样能解决问题**: Release 的 `tag_name` 是权威且可靠的
(`releases/latest` 返回 `v1.4.5`),现在本地 jar 的 `--version` 也会报告同一版本号,
AntiHackerX 只要两边对比就能判断是否需要更新。

> ⚠️ 对比时注意归一化:标签是 `v1.4.5`,而 `--version` 输出
> `native-obfuscator 1.4.5`(无 `v` 前缀),需去掉前缀再比。

**待办(本次未做)**: `SOURCE_PROJECT.md.template` 的「许可证与归属」一节仍写着
「修改与再分发需遵循 GNU GPL v3.0」,这在 Output Exception 合入后已**低估了用户权利**
—— 该例外明确允许把工具输出的 runtime 代码以任意条款分发。建议下个版本修正措辞,
但属于法律表述,需要先确认例外是否也覆盖本 Fork 额外输出的 `build.sh` 与本文件自身。

#### v1.4.4 - 2026-09-17 - 同步上游 master;解除 Gradle 8.14.2 pin

**修改者**: xiaofanforfabric
**本仓库**: https://github.com/xiaofanforfabric/native-obfuscator

**背景**: 上游合入了本仓库提交的两个 PR,并发布了 `3.5.5r`。

- **PR #103**(本仓库提交):修复 `shadowJar` 在 Gradle 9 下的构建失败
  (`com.github.johnrengelman.shadow` 8.1.1 → `com.gradleup.shadow` 9.6.1)。
- **PR #104**(本仓库提交):把 Output Exception 声明写进会被逐字复制到用户
  输出目录的 4 个 runtime 文件(`native_jvm.*`、`native_jvm_output.hpp`、
  `string_pool.hpp`),使下游审计时能看到例外的存在。
- **PR #102**(第三方 `utafrali`,源自 issue #101):在 `LICENSE` 中加入
  Output Exception —— 允许把工具**发出的** runtime 代码以任意条款链接、
  嵌入、编译、分发;工具本身仍是完整 GPL-3.0。

**本仓库本次改动**:

- 合并 `upstream/master`(`b3b3a40` / `3.5.5r`)。
  - `README.md` 尾部有 1 处冲突:上游新增 `### Licensing` 段,本仓库原有
    「📌 联系与反馈(本 Fork)」段。两边都保留。
  - 其余文件(`LICENSE`、`obfuscator/build.gradle`、4 个 runtime 文件)
    全部自动合并,无冲突。
- `gradle/wrapper/gradle-wrapper.properties` —— **解除 v1.3.0 引入的
  8.14.2 pin**,回到上游的 `gradle-9.3.1-all.zip`。
  - 依据:该 pin 存在的唯一理由是「shadow 8.1.1 与 Gradle 9.x 不兼容」,
    而这正是上游 PR #103 修掉的问题。
  - 实测:保留 pin 而合并上游的结果是构建直接失败 ——
    ```
    'void org.gradle.api.component.AdhocComponentWithVariants
            .addVariantsFromConfiguration(Provider, Action)'
    ```
    (`com.gradleup.shadow` 9.x 依赖只有 Gradle 9 才提供的 API。)
- `.github/workflows/main.yml` —— JDK 矩阵从 `[8, 11, 17, 21]` 改为
  `[17, 21, 25]`。
  - 依据:Gradle 9.3.1 要求用 **JVM 17~25** 执行 Gradle 本身
    (<https://docs.gradle.org/9.3.1/userguide/compatibility.html>);
    JDK 8/11 对应的 "Support for running Gradle" 只到 8.14.x。
  - 顺带把 JDK 25 加了回来 —— 旧矩阵去掉它的原因(Gradle 8.14.2 不支持
    JVM 25)已随 pin 一起消失。
  - 原来那项「JDK 8,只编译不打包」的静态检查(用于拦截误用 Java 9+ 新 API)
    已随 JDK 8 一起移除;要恢复应改用 `options.release = 8`,不必装 JDK 8。
- `.github/workflows/release.yml` —— `GRADLE_VERSION` 8.14.2 → 9.3.1,
  并更新相关注释。
- `MODIFICATIONS.md` —— 本条目;`### 环境要求` 同步更新。

**验证**: 本地 `./gradlew clean :obfuscator:shadowJar` 构建通过;
`java -jar obfuscator.jar --version` 输出正常;并用 `-a` 注解模式实测
`@Native` / `@NotNative` 仍被正确识别(`@Native` 类的方法变成 `native`,
`@NotNative` 的方法保留在 Java 层)。

**上游现状备注**(与本仓库无关,仅作记录):上游 CI 目前是红的。实测 run
`35110882936`(commit `b3b3a40`):15 个 job 中只有「JDK 11 on
ubuntu-latest」真的失败,其余 14 个被 fail-fast 连坐 cancel。根因就是上面
那条 —— `5cf558a` 升 wrapper 到 9.x 时没有同步调整矩阵。

#### v1.4.3 - 2026-09-16 - CI:重新引入精简版 `main.yml`

**修改者**: xiaofanforfabric
**本仓库**: https://github.com/xiaofanforfabric/native-obfuscator

**背景**: v1.3.0 直接把上游的 `main.yml`(多平台测试矩阵)删掉了,导致分支和
PR 完全没有构建校验。把上游那份原版配置加回来后 CI 立刻全红 —— 但原因不是
代码坏了,而是上游的配置与本 Fork 的 Gradle 版本约束**互相矛盾**。

**失败根因**(实测 run `35033974729`):15 个 job 里其实只有 **1 个**真的失败,
其余 14 个是被 `fail-fast` 连带取消的(`canceled`)。真正失败的是
**JDK 25 on macos-latest**,而它必定失败的原因是:

> 本仓库 wrapper 固定 **Gradle 8.14.2**(shadow 8.1.1 与 Gradle 9.x 不兼容,
> 见下面的 v1.3.0),而 Gradle 8.14.2 官方只支持用 **JDK 8~24** 来运行:
> *"A JVM version between 8 and 24 is required to execute Gradle.
> JVM 25 and later versions are not yet supported."*
> —— <https://docs.gradle.org/8.14.2/userguide/compatibility.html>

于是 `./gradlew` 在启动阶段就挂了,报 `Unsupported class file major version`。

**改动**:重写 `.github/workflows/main.yml`,相对上游原版做了 5 处调整:

| 调整 | 理由 |
|---|---|
| 矩阵去掉 JDK 25 | Gradle 8.14.2 不支持用 JDK 25 运行(见上) |
| 只跑 `ubuntu-latest` | windows 用的 VS2019 Enterprise `vcvars64.bat` 在现代 runner 上已不存在(现为 VS2022);macOS runner 按 10 倍计费 |
| `fail-fast: false` | 单个 JDK 失败不再把其余 job 一起 cancel,能一次看清全部结果 |
| 不跑 `./gradlew test` | 测试依赖 krak2(要 clone + `cargo build` 好几分钟),每次 push 代价过大;发布前的完整校验由 `release.yml` 负责 |
| 加 `concurrency` / `permissions` | 连续 push 自动取消上一次运行;token 权限收紧为 `contents: read` |

保留 JDK 8/11/17/21 四个矩阵项,其中 **JDK 8 只跑 `compileJava`** —— 因为 shadow
插件本身需要 JDK 11+ 才能运行 Gradle。这一项是唯一能拦住「误用 Java 9+ 新 API」
的检查:`targetCompatibility = 1.8` 只约束字节码版本,并不检查 API 是否存在于 JDK 8。

**验证**:本地用 JDK 17 执行 CI 中的同两条命令,均通过:

```
$ ./gradlew :obfuscator:shadowJar --console=plain
BUILD SUCCESSFUL in 7s

$ java -jar obfuscator/build/libs/obfuscator.jar --version
native-obfuscator 3.5.4r
```

**⚠️ 本次提交只改 CI 配置,不涉及任何转译行为。**

#### v1.4.2 - 2026-09-16 - 缺陷调查:记录并规避上游遗留缺陷(不改转译行为)

**修改者**: xiaofanforfabric
**本仓库**: https://github.com/xiaofanforfabric/native-obfuscator

**背景**: 把一个用本工具转译过的 Bukkit 插件部署到真实 Minecraft 服务器后,
插件在类初始化阶段崩溃。顺着这条线索做了一次系统性的缺陷排查,
又发现了另外几处同类问题,现全部整理成 [`KNOWN_ISSUES.md`](KNOWN_ISSUES.md)。

**本次新增**:

- [`KNOWN_ISSUES.md`](KNOWN_ISSUES.md) —— 4 条已知缺陷的完整记录
  (现象 / 根因 / 证据链 / 实测影响范围 / 规避 / 修复方向);
- `scripts/repro-ki1.sh` —— KI-1 的一键复现(无需 Minecraft,
  自动完成「生成源码 → 转译 → 编译 `.so` → 打包 → 运行」并打印 A/B 对照);
- `scripts/repro-ki4.sh` —— KI-4 的一键复现(同一 JVM 里装载两个插件);
- `README.md` / `MODIFICATIONS.md` —— 显著位置提示已知缺陷与规避方法。

**记录的四条缺陷**:

| 编号 | 缺陷 | 严重度 | 状态 |
|---|---|---|---|
| KI-1 | 数组类型的类解析在隐藏类中必然失败 | **严重** | 已定位,未修复 |
| KI-2 | `MULTIANEWARRAY` 多维分支存在同源缺陷 | 中 | 已定位,本项目不可达 |
| KI-3 | `HiddenMethodsPool` 方法名与计数器无分隔符拼接 | 低 | 已证明当前无害 |
| KI-4 | 隐藏类定义进 bootstrap 域,不同插件互相冲突 | **严重** | 已实证,未修复 |

**⚠️ 本次提交不包含任何转译行为的修改。** 四条缺陷全部继承自上游 v3.5.4r,
使用方通过**黑名单**(`-b`)或 **`--custom-lib-dir`** 即可规避,无需改动本工具。

#### v1.4.1 - 2026-09-16 - 修复 native 函数名歧义导致的 C++ 重定义

**修改者**: xiaofanforfabric
**本仓库**: https://github.com/xiaofanforfabric/native-obfuscator

**问题**: 某些类在转译后生成的 `.cpp` **无法通过 C++ 编译**:

```
error: redefinition of 'void native_jvm::classes::__ngen_ac_grim_grimac_utils_blockplace_BlockPlaceResult_443::__ngen_native_lambdau36staticu36779(JNIEnv*, jclass, jobject, jobject)'
  |  void JNICALL __ngen_native_lambdau36staticu36779(JNIEnv *env, jclass clazz, ...)
  |  note: '...::__ngen_native_lambdau36staticu36779(...)' previously defined here
```

同一个类里出现了**两个同名同签名的 C++ 函数定义**。这不是转译逻辑出错,
而是**函数名拼接有歧义** —— 两个不同的 Java 方法被映射到了同一个符号。

**根因**: `DefaultSpecialMethodProcessor.preProcess()` 中:

```java
return "native_" + context.method.name + context.methodIndex;   // ← 漏洞在这里
```

方法名和方法索引**直接拼接,中间没有分隔符**,于是方法名尾部的数字和索引
糊在一起、无法区分。紧接着 `MethodProcessor` 又会调用
`Util.escapeCppNameString()` 做转义,而它**只改写非字母数字的字符**:

```java
// Util.java
Matcher m = Pattern.compile("([^a-zA-Z_0-9])").matcher(value);
m.appendReplacement(sb, String.format("u%d", (int) m.group(1).charAt(0)));
```

注意 `_` 和数字都**不在**被转义的字符集里,所以拼接后的歧义会**原样保留**到
最终符号名。

**真实触发案例**(GrimAC Bukkit 反作弊插件,
`ac/grim/grimac/utils/blockplace/BlockPlaceResult`):

| 方法索引 | 原始方法名 | 拼接结果 | 转义后的 C++ 符号 |
|---|---|---|---|
| `9` | `lambda$static$77` | `native_lambda$static$779` | `__ngen_native_lambdau36staticu36779` |
| `79` | `lambda$static$7` | `native_lambda$static$779` | `__ngen_native_lambdau36staticu36779` |

`lambda$static$77` + `9` 与 `lambda$static$7` + `79` 得到完全相同的字符串。
该触发条件需要同时满足:

1. 类里存在**两个**这样编号的合成方法(如 `lambda$static$7` 与 `lambda$static$77`),
   即"一个方法名是另一个加了数字后缀";
2. 类的**方法总数足够多**(索引位数发生变化),使 `名+索引` 与 `短名+长索引` 相等。

所以小的测试类几乎碰不到,只有大型真实项目才会踩中。任何形式的
`access$N` / `lambda$...$N` 编号方法都有同样的风险。

**改动文件**:

- `obfuscator/src/main/java/by/radioegor146/special/DefaultSpecialMethodProcessor.java`
  - 补上 GPL 修改声明头。
  - `"native_" + context.method.name + context.methodIndex`
    → `"native_" + context.method.name + "_" + context.methodIndex`

同文件中的另外两个命名点本来就是安全的,无需改动:

```java
String.format("interfacestatic_%d_%d", context.classIndex, context.methodIndex)
String.format("special_clinit_%d_%d", context.classIndex, context.methodIndex)   // ClInitSpecialMethodProcessor
```

**为什么加 `_` 就够了**:`Util.escapeCppNameString()` 会保留 `_`,而
`methodIndex` 是**纯数字串**。设拼接结果为 `名 + "_" + 索引`,由于索引里不含 `_`,
**最后一个 `_` 之后的部分必然就是索引**,因此 `(方法名, 索引) → 符号名` 的映射是
单射,不可能再冲突。

**兼容性**:

- 生成的原生函数名会变化,但该名字**只用于原生库内部**:它同时被写入
  `.cpp` 函数定义、`.hpp` 声明、`__ngen_methods[]` 注册表和
  `HiddenCppMethod`(`NativeObfuscator:370`),全部由同一个字符串派生,
  所以改名是**自洽**的,不影响任何 Java 侧可见行为。
- 输出 JAR 的 Java 字节码不变,只有 `lib/` 里的 `.so` 内容不同。
- **需要重新编译 C++**:函数名变了,旧的目标文件必须清掉重建。

**验证**(GrimAC Bukkit 插件,10.5 MB / 4601 个类,530 个类被转译):

| 检查项 | 修复前 | 修复后 |
|---|---|---|
| 转译是否报错 | 否(静默生成坏代码) | 否 |
| `BlockPlaceResult` 的 lambda 符号 | `...u3677` + `9` = `...u36779`(与索引 79 撞车) | `...u3677_9`(唯一) |
| 全量 529 个 `.cpp` 的函数定义重名 | 1 组 | **0 组** |
| 全量 `__ngen_methods[]` 注册重名 | 1 组 | **0 组** |
| C++ 编译 | 编译到 83% 时 `error: redefinition` 中断 | **通过:529 + 3 个文件全部编译,0 错误,21 分 27 秒** |
| 链接产物 | 无 | `libnative_library.so`,32.9 MB,7210 个导出符号,含 `JNI_OnLoad` |
| 打包后 JAR | 无 | 18.9 MB / 5323 条目,`native0/x64-linux.so` + `native0/Loader.class` 就位 |
| 被转译类方法 | —— | `BlockPlaceResult` 85 个方法全部 `native`(89 个方法减去 4 个构造函数) |
| 被转译类版本 | —— | `major=61`(Java 17,证明 v1.4.0 版本保留亦生效) |
| 加载链路 | —— | `GrimACBukkitLoaderPlugin.<clinit>` → `native0/Loader.registerNativesForClass(int, Class)` |

复现用的独立验证方法(不依赖 C++ 编译器,可快速排查同类问题):

```bash
# 1. 粗筛:扫描生成的 .cpp 是否有重复函数定义
for c in cpp/output/*.cpp; do
    dup=$(grep -oE "JNICALL __ngen_[A-Za-z0-9_]+\(JNIEnv" "$c" | sort | uniq -d)
    [ -n "$dup" ] && { echo "[冲突] $c"; echo "$dup"; }
done

# 2. 精确定位:按转译器同样的算法(方法名 + 方法索引 + 转义)枚举符号并查重
#    见 testp/grimAC/work/inspect 下的模拟脚本思路:
#    symbol = "__ngen_" + escape("native_" + name + "_" + index)
```

#### v1.4.0 - 2026-09-16 - 保留原始 class 版本(修复 Java 9+ 特性被静默丢弃)

**修改者**: xiaofanforfabric
**本仓库**: https://github.com/xiaofanforfabric/native-obfuscator

**问题**: 上游在 `NativeObfuscator` 中**无条件**执行 `classNode.version = 52`,
即无论输入类原本是什么版本,输出**一律改写成 Java 8**。

HotSpot 会**静默忽略**"版本号高于 class 文件所声明版本"的属性——不报错、不警告,
属性直接当不存在。最直观的受害者是 `Record` 属性(只有 major version ≥ 60,即
Java 16 起才被识别):

```java
record Point(int x, int y) {}
Point.class.isRecord();          // 被降级成 52 后返回 false
Point.class.getRecordComponents(); // 返回 null
```

`Record` 属性其实**仍然存在于 class 文件里**,只是 JVM 按版本号把它跳过了。
这也是上游"Java 9+ 支持完全是实验性的"这一结论的重要来源之一:很多现代特性
不是不能被转译,而是转译后被这行代码削掉了。

**改动文件**:

- `obfuscator/src/main/java/by/radioegor146/NativeObfuscator.java`
  - 新增字段 `targetClassVersion`(默认 `-1`,`<= 0` 表示保留原版本)。
  - 新增公开 API `setClassVersion(int)` / `getClassVersion()`,支持链式调用。
  - 把 `classNode.version = 52;` 改为:
    `targetClassVersion > 0 ? targetClassVersion : classNode.version`,
    并把生效版本同步给 `hiddenMethodsPool`。
  - 在生成隐藏类(synthetic `HiddenN`)之前统一套用生效版本:显式指定时用指定值,
    否则用所有已处理类中的**最高**版本(隐藏类体内联了来自不同类的代码,
    取最高版本可保证这些代码用到的属性不被丢弃)。
- `obfuscator/src/main/java/by/radioegor146/HiddenMethodsPool.java`
  - 新增 `classVersion` 字段与 `bumpClassVersion(int)` / `getClassVersion()`,
    跟踪所处理类的最高版本。
  - `getMethod(...)` 中原先硬编码的 `classNode.version = 52;` 改为 `classNode.version = classVersion;`。
- `obfuscator/src/main/java/by/radioegor146/Main.java`
  - 新增 `--class-version <version>` 命令行选项(默认 `null` = 保留原版本)。
  - 新增静态内部类 `ClassVersionConverter implements CommandLine.ITypeConverter<Integer>`,
    把"Java 版本号"与"class 文件 major version"统一成 major version
    (`< 45` 视为 Java 版本号,`+44`)。做成 picocli 转换器而不是在 `call()` 里校验,
    是为了让错误值走 picocli 的标准用法错误通道(打印 `Invalid value for option
    '--class-version': ...` + 用法,退出码 2),而不是抛栈:

    ```text
    $ java -jar obfuscator.jar a.jar out --class-version 0
    Invalid value for option '--class-version': must be a positive number, got 0
    Usage: native-obfuscator [-ahV] [--debug] [-b=<blackListFile>] ...

    $ java -jar obfuscator.jar a.jar out --class-version abc
    Invalid value for option '--class-version': 'abc' is not a number
      (expected a Java release number like 17, or a class file major version like 61)
    Usage: native-obfuscator [-ahV] [--debug] [-b=<blackListFile>] ...
    ```

- `README.md` / `MODIFICATIONS.md` —— 同步更新参数说明与用法。

**兼容性**: 默认行为对 Java 8 输入**完全等价**于上游(Java 8 类的原版本就是 52),
只有 Java 9+ 输入的行为发生变化(这是修复)。需要旧行为时显式加
`--class-version 8`;作为库调用时用 `setClassVersion(52)`。

**新增命令行选项**:

```bash
# 默认: 保留每个类的原始版本(推荐)
java -jar native-obfuscator.jar input.jar output/ -p hotspot

# 强制 Java 8 输出(上游旧行为)
java -jar native-obfuscator.jar input.jar output/ -p hotspot --class-version 8

# 强制 Java 17 输出
java -jar native-obfuscator.jar input.jar output/ -p hotspot --class-version 17
```

**验证**:

测试素材为本地 Java 17 特性矩阵工程(record / sealed interface / 嵌套类 / 接口私有方法 /
switch 表达式 / 模式匹配 instanceof / text block / var / lambda / 匿名类),
`javac` 编译基线为 **major version 61**。`VerifyMain` 末尾打印 `Class#isRecord()` 的结果,
作为"版本是否被降级"的探针。

输出 JAR 中的 class 版本(`native0/Loader.class` 恒为 52,它是工具自带的加载器,
与输入无关):

| 类 | 默认(保留) | `--class-version 8` | `--class-version 17` |
|---|---|---|---|
| `test17/Circle.class`(record) | 61 | **52** | 61 |
| `test17/Square.class`(record) | 61 | **52** | 61 |
| `test17/Iface.class` | 61 | **52** | 61 |
| `test17/Main.class` | 61 | **52** | 61 |
| `test17/Switches.class` | 61 | **52** | 61 |
| `test17/NestDemo$Inner.class` | 61 | **52** | 61 |
| `test17/Ann.class`、`Main$1.class`、`NestDemo.class`、`Shape.class` | 61 | 61 | 61 |
| `native0/Loader.class` | 52 | 52 | 52 |

> 最后一行之外的例外说明: `Ann`(注解类型)、`Main$1`(编译器合成的 switch-map 持有类)、
> `NestDemo`(仅有 `<init>`)、`Shape`(无方法体的 sealed 接口)**没有可转译的方法体**,
> 因此原样透传,版本不受 `--class-version` 影响。这是预期行为——版本只作用于
> 真正被转译的类。

运行结果(`java -cp <jar> test17.Main`,编译原生库后):

| 探针 | 未混淆基线 | 默认 | `--class-version 8` | `--class-version 17` |
|---|---|---|---|---|
| `isRecord(Circle.class)` | `true` | `true` | **`false`** | `true` |
| `isRecord(Rectangle.class)` | `false` | `false` | `false` | `false` |
| 其余全部输出行 | —— | 与基线**逐行完全一致** | 与基线一致 | 与基线**逐行完全一致** |

也就是说:

- **默认行为修复了问题**: record 的 `Record` 属性被保留,`Class#isRecord()` 正常返回 `true`。
- **`--class-version 8` 精确复现上游缺陷**: 程序逻辑照旧能跑,但 `isRecord()` 退化成
  `false`——这正是"Java 9+ 特性看起来不支持"的真实成因,而不是转译本身出错。

**构建前提**: `./gradlew clean :obfuscator:shadowJar`(Gradle 需为 8.14.2,见 v1.3.0),
产物 `obfuscator/build/libs/obfuscator.jar`;`java -jar .../obfuscator.jar --version`
输出 `native-obfuscator 3.5.4r`。

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
  - 后续说明:**v1.4.3 又补回了一个精简版 `main.yml`**(去掉 JDK 25、只跑
    ubuntu、关掉 fail-fast、不跑测试套件),因为完全不校验分支也很危险。
    详见下方的 v1.4.3。
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

- **JDK 17**(CI 与本地验证所用版本;源码 target 为 Java 1.8)。
  注意:执行 Gradle 本身需要 **JVM 17~25**,JDK 8/11 不行(见下方说明)。
- **Gradle 9.3.1** —— 由 `gradle/wrapper/gradle-wrapper.properties` 固定,
  直接用 `./gradlew` 即可,无需自行安装
- CMake 3.x (用于 C++ 编译)
- C++ 编译器 (GCC/Clang/MSVC)

> 📖 这件事的经过:上游 `5cf558a` 把 wrapper 升到 Gradle 9.3.1,却没有同步
> 更换 shadow 插件(仍是 8.1.1),导致构建必挂(`Could not add META-INF to
> ZIP` / `No such property: mode`)。本仓库当时只能把 wrapper 降回 8.14.2
> 绕过。该问题现已由上游合入 **PR #103**(迁移到 `com.gradleup.shadow`)
> 修复,v1.4.4 起本仓库直接用上游版本,不再 pin。
>
> ⚠️ 反过来注意:一旦用 Gradle 9.x,**JDK 8 和 JDK 11 就跑不了 Gradle 了**
> (兼容表里它们的 "Support for running Gradle" 只到 8.14.x)。

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
| `--class-version` | 输出 class 文件版本,可填 Java 版本号(`8`/`17`/`21`)或 major version(`52`/`61`/`65`);不填则**保留原始版本** | `--class-version 17` |

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

### 作为库使用(Java 编程接口)

除了命令行,也可以把 `obfuscator.jar` 直接放进 classpath,调用 `NativeObfuscator`
的公开 API。`setClassVersion(...)` 返回 `this`,支持链式调用:

```java
import by.radioegor146.NativeObfuscator;
import by.radioegor146.Platform;
import java.nio.file.Paths;
import java.util.Collections;

public class Run {
    public static void main(String[] args) throws Exception {
        NativeObfuscator obfuscator = new NativeObfuscator();

        // 可选: 指定输出 class 文件版本。
        // 不调用则保留每个类的原始版本(推荐)。
        // 传 class 文件 major version, 如 52 = Java 8, 61 = Java 17, 65 = Java 21。
        obfuscator.setClassVersion(61);

        // 读取当前生效的版本; -1 表示"保留原始版本"
        int version = obfuscator.getClassVersion();

        obfuscator.process(
                Paths.get("input.jar"),
                Paths.get("output"),
                Collections.emptyList(),   // libraries
                null,                      // blackList
                null,                      // whiteList
                "native",                  // libraryName
                null,                      // customLibraryDirectory
                Platform.HOTSPOT,
                false,                     // useAnnotations
                false                      // generateDebugJar
        );
    }
}
```

> 注意: `NativeObfuscator` 的实例状态在 `process(...)` 调用之间是复用的,
> 若需要为不同 JAR 使用不同版本,请分别构造实例或重新调用 `setClassVersion(...)`。

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
