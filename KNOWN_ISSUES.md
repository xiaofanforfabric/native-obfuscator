# 已知缺陷 (Known Issues)

本文记录**已在真实项目中验证到的缺陷**,以及**已定位但尚未触发**的同源缺陷。
每条缺陷都附带:可复现证据、影响范围实测数据、以及**不改代码即可用的规避手段**。

| 编号 | 缺陷 | 严重度 | 状态 |
|---|---|---|---|
| [KI-1](#ki-1-数组类型的类解析在隐藏类中必然失败) | 数组类型的类解析在隐藏类中必然失败 | **严重**(已确认线上崩溃) | 已定位,未修复 |
| [KI-2](#ki-2-multianewarray-多维分支存在同源缺陷未被触发) | `MULTIANEWARRAY` 多维分支存在同源缺陷 | 中 | 已定位,**尚未触发** |
| [KI-3](#ki-3-hiddenmethodspool-方法名与计数器无分隔符拼接) | `HiddenMethodsPool` 方法名与计数器无分隔符拼接 | 低(代码异味) | 已定位,当前无害 |
| [KI-4](#ki-4-隐藏类定义进-bootstrap-域不同插件互相冲突) | 隐藏类定义进 bootstrap 域,不同插件互相冲突 | **严重**(已实证) | 已定位,未修复 |

> **说明**:本仓库当前**不包含**上述缺陷的修复。它们全部继承自上游
> v3.5.4r,并非本 Fork 引入。本文只做记录与规避,不改变转译行为。

### 快速结论(只想看怎么办)

| 遇到什么 | 用哪条规避 |
|---|---|
| 类初始化时报 `NoClassDefFoundError: [L你自己的类;` | [KI-1 黑名单](#规避用黑名单排除受影响的类) |
| 服务端里装了**多个** native-obfuscator 插件 | [KI-4 用 `--custom-lib-dir` 区分](#ki-4-隐藏类定义进-bootstrap-域不同插件互相冲突) |
| 其它情况 | 目前无需动作 |

---

## KI-1:数组类型的类解析在隐藏类中必然失败

### 一句话结论

当 `<clinit>`(或接口静态方法)中出现**数组类型**的 `checkcast` / `instanceof` /
`LDC` 时,转译器生成 `env->FindClass("[Lpkg/Cls;")`。而这段代码被搬到了
**用 `DefineClass(..., nullptr, ...)` 定义在 bootstrap 类加载器上的隐藏类**里,
`FindClass` 因此只能在 bootstrap 域解析,看不到插件自己加载的类 —— **必然失败**。

### 一分钟自包含复现(不需要 Minecraft)

> 本仓库已把这段流程脚本化:`scripts/repro-ki1.sh`(KI-1)、
> `scripts/repro-ki4.sh`(KI-4)。
> ```bash
> bash scripts/repro-ki1.sh obfuscator/build/libs/obfuscator.jar
> ```
> 脚本会自动完成「生成源码 → 转译 → 编译 → 打包 → 运行」并给出 A/B 结论。
> 下面是手写版,便于理解每一步在干什么。

只需要 3 个类、30 行 Java,就能百分百复现本文这个缺陷。**建议先跑这个**,
它是理解后续所有分析的最短路径。

```java
// rep/Item.java
package rep;
public class Item { public int v; }

// rep/Victim.java —— 关键是 <clinit> 里对「自己的数组类型」做 checkcast
package rep;
public class Victim {
    public static final int N;
    static {
        Object o = new Item[]{ new Item(), new Item(), new Item() };
        Item[] arr = (Item[]) o;      // javac 生成 CHECKCAST [Lrep/Item;
        N = arr.length;
    }
}

// Main.java
public class Main {
    public static void main(String[] a) {
        System.out.println("Victim.N = " + rep.Victim.N);
    }
}
```

确认 `javac` 真的生成了数组 `checkcast`:

```text
$ javap -c -p -cp classes rep.Victim | grep checkcast
      36: checkcast     #10      // class "[Lrep/Item;"
```

转译后生成的 C++(`outA/cpp/output/rep_Victim_1.cpp:116`)与线上崩溃处
**形态完全一致**:

```c
// CHECKCAST [Lrep/Item;; Stack: 1
if (!cclasses[1] || env->IsSameObject(cclasses[1], NULL)) { cclasses_mtx[1].lock();
  if (!cclasses[1] || env->IsSameObject(cclasses[1], NULL)) {
    if (jclass clazz = env->FindClass(((char *)(string_pool + 1395LL)))) {   // ← 致命
      cclasses[1] = (jclass) env->NewWeakGlobalRef(clazz);
      env->DeleteLocalRef(clazz);
    } } cclasses_mtx[1].unlock();
  if (env->ExceptionCheck()) { return (void) 0; } }
```

运行结果(A/B 对照,均已实测):

```text
################ [A] 不加黑名单(rep/Victim 已转译) ################
Exception in thread "main" java.lang.NoClassDefFoundError: [Lrep/Item;
        at rep.Victim.<clinit>(Victim.java)
        at Main.main(Native Method)

################ [B] 加黑名单(rep/Victim 未转译) ################
Victim.N = 3
```

注意 [A] 里的 `[Lrep/Item;` —— 这正是线上日志里 `NoClassDefFoundError: [Lac/grim/.../StateType;`
的同一件事,只是元素类换成了最小工程的 `rep/Item`。

完整命令:

```bash
# 0. 编译
javac -d classes rep/Item.java rep/Victim.java Main.java
(cd classes && jar cf ../rep.jar .)

# 1. [A] 不加黑名单
java -jar obfuscator.jar rep.jar outA -p hotspot
(cd outA && cmake -S cpp -B cpp/build -DCMAKE_BUILD_TYPE=Release \
              && cmake --build cpp/build -j4 && bash build.sh)
java -cp outA/rep.jar Main        # → NoClassDefFoundError: [Lrep/Item;

# 2. [B] 用黑名单排除 rep/Victim
printf 'rep/Victim\n' > bl.txt
java -jar obfuscator.jar rep.jar outB -p hotspot -b bl.txt
(cd outB && cmake -S cpp -B cpp/build -DCMAKE_BUILD_TYPE=Release \
              && cmake --build cpp/build -j4 && bash build.sh)
java -cp outB/rep.jar Main        # → Victim.N = 3
```

### 现象

在 Minecraft 1.21.4 + Paper 服务端加载 GrimAC 反作弊插件,玩家进入服务器后立刻掉线:

```
[Netty Epoll Server IO #1] ERROR ... Disconnected xiaofan due to an invalid packet!
java.lang.ExceptionInInitializerError: Exception java.lang.NoClassDefFoundError:
    [Lac/grim/grimac/shaded/com/github/retrooper/packetevents/protocol/world/states/type/StateType;
    [in thread "Netty Epoll Server IO #1"]
        at ac.grim.grimac.utils.collisions.CollisionData.<clinit>(CollisionData.java)
```

随即所有触碰到 `CollisionData` 的调用点都变成:

```
java.lang.NoClassDefFoundError: Could not initialize class ac.grim.grimac.utils.collisions.CollisionData
    at ac.grim.grimac.utils.collisions.Collisions.getCollisionBoxes(Native Method)
    at ac.grim.grimac.utils.collisions.datatypes.CollisionFactory.create(Native Method)
    ...
    at ac.grim.grimac.manager.CheckManagerListener.handleFlying(Native Method)
```

异常穿到 PacketEvents 的解码线程后被当成「非法数据包」,于是客户端被踢下线。

### 根因(四层叠加)

#### 第 1 层 —— 数组类型的类解析走了另一条分支

`obfuscator/src/main/java/by/radioegor146/MethodProcessor.java:98`

```java
public static String getClassGetter(MethodContext context, String desc) {
    if (desc.startsWith("[")) {
        return "env->FindClass(" + context.getStringPool().get(desc) + ")";   // ← 数组类型
    }
    if (desc.endsWith(";")) {
        desc = desc.substring(1, desc.length() - 1);
    }
    return "utils::find_class_wo_static(env, classloader, " + ... )";          // ← 普通类型
}
```

这个分支本身写得没错:JNI 的 `FindClass` **确实接受**数组描述符,而
`ClassLoader.loadClass` 不接受(所以不能统一走 `find_class_wo_static`)。
问题在于:`FindClass` **丢掉了那个显式捕获的 `classloader`**,而它的解析域由
JNI 规范另行决定(见第 3 层)。

#### 第 2 层 —— `<clinit>` 被搬进了隐藏类

`ClInitSpecialMethodProcessor.preProcess()` 把 `<clinit>` 整体移交给
`HiddenMethodsPool`,并在 `postProcess()` 中把原 `<clinit>` 替换为一个桩:

```
LDC classIndex
LDC Type.getObjectType(clazz.name)
INVOKESTATIC <nativeDir>/Loader.registerNativesForClass(ILjava/lang/Class;)V
LDC class type
INVOKESTATIC <hiddenClass>.<hiddenMethod>(Ljava/lang/Class;)V
RETURN
```

真正的初始化逻辑变成隐藏类 `native0/hidden/Hidden0` 上的静态 native 方法
(`HiddenMethodsPool.java:123`:`classNode.name = baseName + "/Hidden" + classes.size()`)。
**接口的静态方法走同一机制**(`interfacestatic_*`)。

#### 第 3 层 —— 隐藏类被定义到 bootstrap 类加载器

`obfuscator/src/main/java/by/radioegor146/source/MainSourceBuilder.java:27`

```c
env->DeleteLocalRef(env->DefineClass(%s, nullptr, ...));
//                                       ^^^^^^^ 第二个参数是 loader → nullptr = bootstrap
```

而 JNI 规范规定:`FindClass` 从**「当前 native 方法所属类的定义类加载器」**开始解析。
native 方法声明在 `native0/hidden/Hidden0` 上,该类由 bootstrap 定义,
于是 `FindClass` 只能在 bootstrap 域查找。

在 Bukkit/Paper 上,插件类由 `PluginClassLoader` 加载,它是应用类加载器的**子**加载器。
bootstrap 加载器**永远看不到子加载器里的类**,`FindClass` 返回 `null` 并挂起
`NoClassDefFoundError`。

> 对比:同一个 `<clinit>` 里的普通类型(如 `ANEWARRAY StateType`)走
> `utils::find_class_wo_static(env, classloader, ...)`,用的是从**参数 `clazz`**
> (即真正的目标类)取来的插件 `classloader`,所以工作正常。
> **同一个类、同一个 `<clinit>`,只因为一个是数组类型、一个是普通类型,结果一个成功一个失败。**

#### 第 4 层 —— 静默夭折被放大成整个类不可用

生成代码的每一处都可能带异常检查:

```c
if (env->ExceptionCheck()) { return (void) 0; }
```

`<clinit>` 的 native 一旦检测到待决异常就**直接返回**,既不清理也不继续。于是:

1. `CollisionData` 的静态初始化中途夭折 → JVM 抛 `ExceptionInInitializerError`,
   并把该类标记为 `erroneous`;
2. 之后**任何**触碰 `CollisionData` 的地方都变成
   `NoClassDefFoundError: Could not initialize class ...`;
3. 这些访问点自身也是 native,异常沿着 native 边界一路穿出到 PacketEvents 的解码线程;
4. PacketEvents 把任何异常视为「非法数据包」→ 踢人。

所以「一个 `checkcast` 失败」最终表现为「整个插件在玩家进服时炸掉」。

### 证据链

**① 异常消息里的类名格式就是 `FindClass` 的入参**

```
NoClassDefFoundError: [Lac/grim/grimac/shaded/.../type/StateType;
```

以 `[` 开头、用 `/` 分隔 —— 这是**原样**出现在异常里的数组描述符。
JVM 只有一处会这样产生带斜杠的类名:`JNI FindClass` 失败时抛
`NoClassDefFoundError(传入的原始字符串)`。(`ClassLoader.loadClass` 抛出的消息是
二进制名,用 `.` 分隔。)

**② 生成代码逐字对应**

`CollisionData_436.cpp:22337`(529 类版本):

```c
// CHECKCAST [Lac/grim/grimac/shaded/.../type/StateType;; Stack: 6
if (!cclasses[55] || env->IsSameObject(cclasses[55], NULL)) { cclasses_mtx[55].lock(); if (!cclasses[55] || env->IsSameObject(cclasses[55], NULL)) { if (jclass clazz = env->FindClass(((char *)(string_pool + 298717LL)))) { cclasses[55] = (jclass) env->NewWeakGlobalRef(clazz); env->DeleteLocalRef(clazz); } } cclasses_mtx[55].unlock(); if (env->ExceptionCheck()) { return (void) 0; } }
```

`string_pool` 偏移 `298717` 处的字符串经解析后正是:

```
[Lac/grim/grimac/shaded/com/github/retrooper/packetevents/protocol/world/states/type/StateType;
```

**③ 同屏对照:普通类型成功、数组类型失败**

同一段 `<clinit>` 往上 4 行:

```c
// ANEWARRAY .../type/StateType; Stack: 7
... if (jclass clazz = utils::find_class_wo_static(env, classloader, (cstrings[8]))) ...
```

`ANEWARRAY` 用 `classloader` → **成功拿到 `StateType`**;紧接着的 `CHECKCAST`
换成 `FindClass` → 失败。同一个类,唯一区别就是解析用的加载器。

**④ JNI 最小复现实验**

不依赖任何 Minecraft / 编译器,50 行 JNI 即可复现。三步:
`DefineClass(name, NULL, bytes, len)` 定义一个类 → 读它的 `getClassLoader()` →
在它声明的 native 方法里 `FindClass` 一个应用类。

```java
// App.java
public class App { public static String name() { return "app"; } }

// Hidden1.java —— 被「隐藏类」模拟的对象
public class Hidden1 {
    public static native void probe2();
    public static void dummy() {}
}

// Probe.java
public class Probe {
    static native void probe(byte[] classBytes);
    public static void main(String[] args) throws Exception {
        System.out.println("App loaded by : " + App.class.getClassLoader());
        System.load("/tmp/jnitest/libprobe.so");
        probe(java.nio.file.Files.readAllBytes(
                java.nio.file.Path.of("/tmp/jnitest/Hidden1.class")));
    }
}
```

```c
/* probe.c 关键部分 */
static void JNICALL probe2(JNIEnv *env, jclass c) {          /* 声明在 Hidden1 上 */
    jclass app = (*env)->FindClass(env, "App");
    if (app == NULL) { (*env)->ExceptionDescribe(env); (*env)->ExceptionClear(env); }
}
JNIEXPORT void JNICALL Java_Probe_probe(JNIEnv *env, jclass c, jbyteArray bytes) {
    jsize len = (*env)->GetArrayLength(env, bytes);
    jbyte *buf = malloc(len);
    (*env)->GetByteArrayRegion(env, bytes, 0, len, buf);

    jclass h = (*env)->DefineClass(env, "Hidden1", NULL, buf, len);   /* ← loader = NULL */
    printf("[1] DefineClass(\"Hidden1\", NULL, ...) -> %s\n", h ? "成功" : "失败");

    jmethodID getCL = (*env)->GetMethodID(env, (*env)->FindClass(env, "java/lang/Class"),
                                          "getClassLoader", "()Ljava/lang/ClassLoader;");
    jobject cl = (*env)->CallObjectMethod(env, h, getCL);
    printf("[2] Hidden1.getClassLoader() = %s\n", cl == NULL ? "null (即 bootstrap)" : "非 null");

    JNINativeMethod m[] = { { "probe2", "()V", (void *) &probe2 } };
    (*env)->RegisterNatives(env, h, m, 1);
    (*env)->CallStaticVoidMethod(env, h, (*env)->GetStaticMethodID(env, h, "probe2", "()V"));
    free(buf);
}
```

编译运行:

```bash
JH=$(dirname $(dirname $(readlink -f $(which javac))))
gcc -shared -fPIC -o libprobe.so probe.c -I "$JH/include" -I "$JH/include/linux"
javac App.java Hidden1.java Probe.java
java -cp . Probe
```

实测输出(JDK 17.0.20):

```
App loaded by : jdk.internal.loader.ClassLoaders$AppClassLoader@30946e09
[1] DefineClass("Hidden1", NULL, ...) 成功 -> class=0x76b6a817aae0
[2] Hidden1.getClassLoader() = null (即 bootstrap)
[probe2] FindClass("App") 失败, 待决异常如下:
java.lang.NoClassDefFoundError: App
        at Hidden1.probe2(Native Method)
```

三点全部证实:①`DefineClass(..., NULL, ...)` 合法;②得到的类 loader 是 `null`
(bootstrap);③**在该类的 native 里 `FindClass` 应用类必然失败**,且异常类型与
消息格式和线上日志一致。

**⑤ 反证:`FindClass` 的数组分支本身没问题**

把 native 声明在**正常加载**的类上(即「普通 native」的情形),数组描述符一切正常:

```c
/* 声明在 A 上,A 由应用类加载器加载 */
JNIEXPORT void JNICALL Java_A_t(JNIEnv *env, jclass c) {
    printf("FindClass(\"A\")            -> %s\n", (*env)->FindClass(env, "A") ? "成功" : "失败");
    printf("FindClass(\"[LA;\")          -> %s\n", (*env)->FindClass(env, "[LA;") ? "成功" : "失败");
    printf("FindClass(\"[[LA;\")         -> %s\n", (*env)->FindClass(env, "[[LA;") ? "成功" : "失败");
    printf("FindClass(\"[Ljava/lang/Object;\") -> %s\n", (*env)->FindClass(env, "[Ljava/lang/Object;") ? "成功" : "失败");
}
```

```
A 的类加载器 = jdk.internal.loader.ClassLoaders$AppClassLoader@30946e09
[1] FindClass("A")            -> 成功
[2] FindClass("[LA;")          -> 成功
[3] FindClass("[[LA;")         -> 成功
[4] FindClass("[Ljava/lang/Object;") -> 成功
```

**所以缺陷不是「数组分支写错了」,而是「数组分支 + 隐藏类 = bootstrap 域」
这个组合错误。** 这也解释了为什么下面的 C 类调用点(55 处)全部无害。

### 影响范围(实测)

对 GrimAC Bukkit 插件(`LightningGrim-bukkit-2.3.72-326e200.jar`,
10.5 MB / 5321 条目 / 4601 个类,其中 529 个被转译)做全量扫描:

| 类别 | 位置 | 处数 | 是否致命 |
|---|---|---|---|
| **A** | 隐藏类方法(`__ngen_special_clinit_*` / `__ngen_interfacestatic_*`)中的数组类型 `FindClass` | **51**(4 个类) | **是** |
| B | 隐藏类方法中的 `MULTIANEWARRAY` | 0 | 见 [KI-2](#ki-2-multianewarray-多维分支存在同源缺陷未被触发) |
| C | 普通 native 中的数组类型 `FindClass` | 55 | 否(loader 正确) |
| D | 普通 native 中的 `MULTIANEWARRAY` | 0 | —— |

A 类命中的 4 个类:

| 类 | 生成文件(529 类版本) | 命中 | 数组元素类型 |
|---|---|---|---|
| `ac/grim/grimac/utils/collisions/CollisionData` | `..._CollisionData_436.cpp` | 30 | `[L…/world/states/type/StateType;` |
| `ac/grim/grimac/utils/collisions/HitboxData` | `..._HitboxData_435.cpp` | 14 | `[L…/world/states/type/StateType;` |
| `ac/grim/grimac/utils/blockplace/BlockPlaceResult` | `..._BlockPlaceResult_443.cpp` | 6 | `[L…/protocol/item/type/ItemType;` |
| `ac/grim/grimac/utils/webhook/JsonSerializable` | `..._JsonSerializable_447.cpp` | 1 | `[L…/utils/webhook/JsonSerializable;` |

**细化:A 类 51 处的数组描述符分布**(把 `string_pool + N` 的 N 反解码得到)

| 处数 | 维数 | 元素类型 | bootstrap 能否看见 | 后果 |
|---|---|---|---|---|
| 44 | 1 | `ac/grim/grimac/shaded/…/states/type/StateType` | 否 | **必然失败** |
| 6 | 1 | `ac/grim/grimac/shaded/…/item/type/ItemType` | 否 | **必然失败** |
| 1 | 1 | `ac/grim/grimac/utils/webhook/JsonSerializable` | 否 | **必然失败** |
| **0** | —— | JDK 类 / 基本类型 | 是 | (侥幸能成功,但本项目中一处都没有) |

> 值得注意:**本项目里 51 处全部致命**,没有任何一处因“元素类型恰好是
> JDK 类而侥幸通过”。也就是说这个缺陷不是“部分失效”,而是
> “只要命中就 100% 炸”。

**触发条件**(三者需同时成立):

1. `<clinit>`(或接口静态方法)里存在**数组类型**的 `checkcast` / `instanceof` / `LDC`。
   javac 主要在 `new T[]{...}` 的变长参数展开、`Collection.toArray(new T[0])`
   这类场景生成这种指令,所以**不是每个项目都有**;
2. 数组的元素类由**插件自己的类加载器**加载(即不在 bootstrap 可见范围);
3. 恰好落在被搬进隐藏类的 `<clinit>` / 接口静态方法上。

> 这也是为什么本缺陷在小型测试类里极难发现:必须同时有「集合转数组」
> 和「静态初始化」两个条件。

**为什么只有 `CollisionData` 第一个炸**:它是碰撞注册表,玩家一移动就会触发
`<clinit>`,排在所有其他命中类之前。其余 3 个类同样会炸,只是时机更晚。

### 检测方法

不需要 C++ 编译器,解析 `string_pool.cpp` + 扫描生成的 `.cpp` 即可:

```python
#!/usr/bin/env python3
"""检测 KI-1:隐藏类方法中的数组类型 FindClass。用法: check.py <output/cpp 目录>"""
import re, os, sys

FUNC = re.compile(r'^\s*(?:\w[\w:<>]*\s+)+(\w+)\(JNIEnv \*env,')
FINDARR = re.compile(r'FindClass\(\(\(char \*\)\(string_pool \+ (\d+)LL\)\)\)')

def hidden(fn):
    return bool(fn) and (fn.startswith('__ngen_special_clinit') or
                         fn.startswith('__ngen_interfacestatic'))

cpp = sys.argv[1]
src = open(os.path.join(cpp, 'string_pool.cpp'), encoding='utf-8', errors='replace').read()
m = re.search(r'static char pool\[(\d+)LL\] = \{(.*?)\};', src, re.S)
pool = bytes(int(x) for x in re.findall(r'\d+', m.group(2)))

def s(off):
    return pool[off:pool.index(b'\0', off)].decode('utf-8', 'replace')

hits = 0
for fn in sorted(os.listdir(os.path.join(cpp, 'output'))):
    if not fn.endswith('.cpp'):
        continue
    cur = None
    for ln, line in enumerate(open(os.path.join(cpp, 'output', fn), encoding='utf-8',
                                  errors='replace'), 1):
        f = FUNC.match(line)
        if f:
            cur = f.group(1)
        for mm in FINDARR.finditer(line):
            st = s(int(mm.group(1)))
            if st.startswith('[') and hidden(cur):
                print(f'  {fn}:{ln}  {cur}  ->  {st}')
                hits += 1
print(f'命中 {hits} 处' + ('  ← 会导致运行时崩溃' if hits else '  ← 未发现 KI-1'))
sys.exit(1 if hits else 0)
```

> 注意 `__ngen_register_methods` 里也有一处 `FindClass(... + 3788LL)`,
> 那是 `prepare_lib` 注册隐藏类 native 用的(`native0/hidden/Hidden0`),
> 由 `JNI_OnLoad` 触发、加载器正确,**不是**缺陷。检测脚本会把它排除。

对同一份输入跑「黑名单前 / 黑名单后」的对照结果:

```
黑名单之前 (529 类): A = 51 处  ← 会导致运行时崩溃
黑名单之后 (525 类): A =  0 处  ← 未发现 KI-1
```

### 规避:用黑名单排除受影响的类

把命中的类加入黑名单(`-b`),它们会以**原始 `.class`** 形式保留在输出 JAR 中,
功能完全正常,只是没有被 native 化:

```text
# bug-blacklist.txt
ac/grim/grimac/shaded/**

ac/grim/grimac/utils/collisions/CollisionData
ac/grim/grimac/utils/collisions/HitboxData
ac/grim/grimac/utils/blockplace/BlockPlaceResult
ac/grim/grimac/utils/webhook/JsonSerializable
```

```bash
java -jar obfuscator.jar input.jar out/ \
     -b bug-blacklist.txt \
     -w core-whitelist.txt \
     -p hotspot
```

验证要点:

- 日志出现 `Skipping ac/grim/grimac/utils/collisions/CollisionData` 等 4 行;
- `out/cpp/output/` 下不再有对应的 `_436.cpp` / `_435.cpp` / `_443.cpp` / `_447.cpp`;
- 重跑检测脚本命中数归零。

**实测结果**(GrimAC 全量,529 类 → 525 类):

| 指标 | 不加黑名单 | 加黑名单 |
|---|---|---|
| 转译类数 | 529 | 525 |
| 检测脚本 A 类命中 | **51** | **0** |
| 其中元素类型属于插件自己的 | 51(100%) | 0 |
| 混淆退出码 | 0 | 0 |
| C++ 构建 | 成功 | 成功(8m23s / `-j4`) |
| 打包出的 JAR | 17.0 MB | 17.8 MB |

另外用「把插件里所有类逐个强制初始化并统计失败原因」的运行时探测
(`Sweep.java`,4574 个类)做黑盒复核:

```text
======== 结果 ========
成功: 4265   失败: 309   其中 KI-1 特征: 0
```

309 个失败**全部**是探测环境的依赖缺失(`com/google/gson/*`、`org/bukkit/*`、
`com/viaversion/*`、`com/mojang/brigadier/*` 不在类路径上,或 PacketEvents 未初始化),
没有任何一个是**数组类型 `NoClassDefFoundError`** 特征。

> **这个探测的局限**:GrimAC 的 `CollisionData.<clinit>` 在**第 62 行**就要
> 访问 `StateTypes`,而 `StateTypes` 又依赖 PacketEvents 已初始化 —— 在裸 JVM 里
> 会先在这里炸掉,**走不到**后面那段数组 `checkcast`。也就是说这个探测**无法证明**
> 黑名单修好了 KI-1,它只能证明「没有引入新的、以数组 `NoClassDefFoundError`
> 为特征的问题」。要证明修复有效,请用上面那段
> [一分钟自包含复现](#一分钟自包含复现不需要-minecraft)—— 它的控制流里没有
> 任何外部依赖,能直达出错点。

**代价**:这 4 个类未被混淆(其中 `CollisionData` / `HitboxData` 体量不小,
含大量碰撞箱逻辑),保护强度下降。除此之外无行为差异。

> ⚠️ **黑名单会改变后续所有类的编号**。`classIndex` 是按处理顺序分配的序号,
> 跳过类会让其后的类整体前移(实测 529 → 525 后,`ac/grim/grimac/api/common/GenericReloadable`
> 从 `_512` 变成 `_508`)。因此 **Java 侧与 `.so` 必须成对重新生成**,
> 不能拿旧 `.so` 配新 `JAR`。

### 修复方向(仅供参考,本仓库未实施)

按侵入性从低到高:

1. **让数组类型也走显式 `classloader`**:`getClassGetter` 的数组分支不要用
   `FindClass`,改成基于元素类的二元名解析后组装数组类
   (先 `find_class_wo_static(env, classloader, "pkg.Cls")`,再用
   `env->FindClass("[L...;")` 或在 `GetObjectClass` 上做 `NewObjectArray`)。
   这是最小改动。
2. **把隐藏类定义到正确的加载器**:`registerDefine` 里把 `nullptr` 换成
   目标插件的 `ClassLoader`(需要为该隐藏类取一次 `get_classloader_from_class`),
   使隐藏类与插件同域。这样 `FindClass` 自然能看到插件类。
3. **改掉 `<clinit>` 的搬移机制**:不把 `<clinit>` 挪进隐藏类,而是为原类
   `RegisterNatives` 一个真正的 `<clinit>` native。改动最大,但最彻底。

任何方案都需注意:隐藏类的类名/定义方式变化会同时影响
`Loader` 的注册逻辑与 `data_*.cpp` 的生成。

---

## KI-2:`MULTIANEWARRAY` 多维分支存在同源缺陷(未被触发)

`obfuscator/src/main/resources/sources/native_jvm.cpp:186`

```cpp
jobjectArray create_multidim_array(JNIEnv *env, jobject classloader, jint count, jint required_count,
    const char *class_name, int line, std::initializer_list<jint> sizes, int dim_index) {
    ...
    if (count == 1) {
        /* 正确:一维数组用传入的 classloader */
        std::string renamed_class_name(class_name);
        std::replace(renamed_class_name.begin(), renamed_class_name.end(), '/', '.');
        jstring renamed_class_name_string = env->NewStringUTF(renamed_class_name.c_str());
        jclass clazz = find_class_wo_static(env, classloader, renamed_class_name_string);
        ...
    }
    /* 缺陷:多维数组忽略 classloader,改用 FindClass */
    std::string clazz_name = std::string(count - 1, '[') + "L" + std::string(class_name) + ";";
    if (jclass clazz = env->FindClass(clazz_name.c_str())) {      // ← 同 KI-1
        ...
    } else {
        return nullptr;
    }
```

**这是与 KI-1 完全同源的缺陷**:一维分支用对了 `classloader`,多维分支却退回
`FindClass`。区别只是触发它需要在 `<clinit>` 里出现 `MULTIANEWARRAY`
(如 `new T[a][b]`),比「集合转数组」更少见。

**实测**:在当前 GrimAC 插件里,**整个产物中 `MULTIANEWARRAY` 指令为 0 处**
(对照:`ANEWARRAY` 有 1807 处),所以 KI-2 在这个项目里**根本不可达**,
连“隐藏类里为 0、普通 native 里有一些”都不是 —— 是一处都没有。

但这仍然是一个随时可能出现的隐患 —— 任何在静态初始化块里 `new T[x][y]`
的类都会踩中。触发面比 KI-1 窄(需要多维数组),所以标为“中”。

本仓库中的 `native_jvm.cpp` 是**随输出复制的可编辑源码**,所以使用方也可以
在本地产物里直接改这个文件,而不必改转译器。

---

## KI-3:`HiddenMethodsPool` 方法名与计数器无分隔符拼接

`obfuscator/src/main/java/by/radioegor146/HiddenMethodsPool.java:114`

```java
String newName = name + namePool.compute(name, (otherName, value) -> value == null ? 0 : value + 1);
```

方法名和计数器**直接拼接,中间没有分隔符**。这与已修复的 v1.4.1
(`DefaultSpecialMethodProcessor` 的 `"native_" + name + methodIndex`)是**同一个模式**。

在当前设计下它是**无害的**,原因是:

- 基名由 `String.format("special_clinit_%d_%d", classIndex, methodIndex)` 生成,
  每个 `(类, 方法)` 组合唯一;`<clinit>` 每类至多一个,接口静态方法亦然;
- 基名从不重复请求,所以计数器**恒为 0**,`newName` 始终是唯一的
  (实测 GrimAC 产物:541 个隐藏方法名,唯一 541 个,**重复 0 个**)。

数学上也是安全的:计数器恒为 0 时,`newName` = `special_clinit_{c}_{m}0`,
而基名以 `_{m}` 结尾、`m` 是纯数字串。设 `special_clinit_{c}_{m1}0` 与
`special_clinit_{c}_{m2}0` 相等,则 `m1` 与 `m2` 的十进制表示相同,即 `m1 == m2`。

但它是一个**潜在陷阱**:一旦将来出现「同一基名被请求两次」的场景(例如新增
其他类型的代理方法、或方法合并),`...78` + `0` 与 `...7` + `80` 就会撞名,
退化成 v1.4.1 那种 C++ 重定义错误。建议在改动该文件时顺手补上分隔符
(`name + "_" + counter`)。

---

## KI-4:隐藏类定义进 bootstrap 域,不同插件互相冲突

> 与 KI-1 是**同一个根因的另一个后果**(都把隐藏类塞进了 bootstrap 域),
> 但表现和触发条件完全不同,因此单独列出。

### 一句话结论

隐藏类的名字是 `nativeDir + "/hidden/HiddenN"`,而 `nativeDir` 只根据
<b>输入 JAR 自己</b>的内容去重(通常是固定的 `native0`)。这些类又被
`DefineClass(..., nullptr, ...)` 定义进 **bootstrap 域**。bootstrap 域是**整个 JVM
全局共享**的,因此**同一 JVM 里第二个用 native-obfuscator 转译过的插件,会在
加载原生库时直接抛 `LinkageError`**。

### 现象

真实产物 + `URLClassLoader` 双插件模拟(`/tmp/sweep/DualLoad.java`)的实测输出:

```text
---- 插件 #1 装载 native0.Loader ----
     成功: class native0.Loader  由 java.net.URLClassLoader@45f45fa1 定义
---- 插件 #2 装载 native0.Loader ----
     失败: java.lang.LinkageError: loader 'bootstrap' attempted duplicate class
           definition for native0.hidden.Hidden0.
           (native0.hidden.Hidden0 is in unnamed module of loader 'bootstrap')

---- 后果:插件 #2 初始化自己的第一个转译类 ----
     失败: java.lang.NoClassDefFoundError: Could not initialize class native0.Loader
        -> java.lang.ExceptionInInitializerError: Exception java.lang.LinkageError:
           loader 'bootstrap' attempted duplicate class definition for
           native0.hidden.Hidden0. [in thread "main"]
```

### 根因

第 1 步,隐藏类名**不含任何全局唯一信息**:

```java
// NativeObfuscator.java:255-262
int nativeDirId = IntStream.iterate(0, i -> i + 1)
        .filter(i -> !jarContains("native" + i))     // ← 只看输入 JAR 自己
        .findFirst().getAsInt();
nativeDir = "native" + nativeDirId;
...
hiddenMethodsPool = new HiddenMethodsPool(nativeDir + "/hidden");

// HiddenMethodsPool.java:123
classNode.name = baseName + "/Hidden" + classes.size();   // ← 从 0 开始,每次都一样
```

任何两个默认参数的插件,第一个隐藏类都叫 `native0/hidden/Hidden0`。

第 2 步,它们被定义进 **bootstrap** 域(`MainSourceBuilder.java:27`):

```c
env->DeleteLocalRef(env->DefineClass("%s", nullptr,
        native_jvm::data::__ngen_%s::get_class_data(),
        native_jvm::data::__ngen_%s::get_class_data_length()));
```

第二参数 `nullptr` 表示“用 bootstrap 加载器定义”。bootstrap 域的命名空间是
**全局唯一**的,同一个名字不能被定义两次 —— 即使两次定义来自两个毫不相干的
插件、字节码内容也完全不同。

第 3 步,异常处理**不清理待决异常,直接 return**,把错误放大到整个插件不可用
(模板 `sources/native_jvm_output.cpp`,已从生成物 `native_jvm_output.cpp` 第 1073 行
处验证):

```c
env->DeleteLocalRef(env->DefineClass(((char *)(string_pool + 3788LL)), nullptr, ...));

if (env->ExceptionCheck())
    return;                       // ← 从 prepare_lib 直接返回,LinkageError 仍待决

char method_name[] = "registerNativesForClass";
JNINativeMethod loader_methods[] = { { method_name, method_desc, (void *)&register_for_class } };
env->RegisterNatives(env->FindClass("native0/Loader"), loader_methods, 1);
                                          // ↑ 这一行永远不会被执行
```

后果链:

1. `prepare_lib` 提前 `return`,所以 **`native0/Loader.registerNativesForClass` 从未被注册**;
2. `prepare_lib` 是从 `JNI_OnLoad` 调的,而 `JNI_OnLoad` 带着待决异常返回 ——
   JVM 把 `LinkageError` 抛给 `System.load` 的调用者,即 `native0/Loader` 的静态块;
3. 静态块失败 → `ExceptionInInitializerError` → `native0/Loader` 被标记为错误;
4. 之后插件里**每一个**类的 `<clinit>` 都会 `INVOKESTATIC native0/Loader.registerNativesForClass`,
   于是变成 `NoClassDefFoundError: Could not initialize class native0.Loader`。

注意第 4 步的症状与 KI-1 **长得一模一样**(都是 `NoClassDefFoundError: Could not
initialize class X`),但根因完全不同:

| | KI-1 | KI-4 |
|---|---|---|
| 失败的类 | 某个业务类(如 `CollisionData`) | `native0.Loader` **全体** |
| 前提条件 | 该类 `<clinit>` 里有数组类型 `checkcast` | JVM 里存在**第二个** native-obfuscator 插件 |
| 触发时刻 | 该类第一次被使用时 | 插件加载时(更早) |
| 单插件环境 | 出现 | **不出现** |

### 复现步骤(不需要 Minecraft)

> 同样已脚本化:`bash scripts/repro-ki4.sh obfuscator/build/libs/obfuscator.jar`。
> 它会从同一个输入 JAR 构建出 `plug1` / `plug2`(默认 `native0`)与
> `plug2b`(`--custom-lib-dir mylib`),然后在**同一个 JVM** 里两两装载做对照。
> 实测输出:
> ```text
> ######## 场景 1:两个插件都用默认 nativeDir(都是 native0) ########
>   ---- 装载 rep.jar ----
>        native0.Loader 装载成功
>   ---- 装载 rep.jar ----
>        失败: java.lang.LinkageError: loader 'bootstrap' attempted duplicate class
>              definition for native0.hidden.Hidden0.
>
> ######## 场景 2:第二个插件改用 --custom-lib-dir mylib ########
>   ---- 装载 rep.jar ----
>        native0.Loader 装载成功
>   ---- 装载 rep.jar ----
>        mylib.Loader 装载成功
> ```

以下是手工步骤,便于逐步观察:

```java
// DualLoad.java —— 用两个独立的类加载器加载同一个插件 JAR
for (int i = 1; i <= 2; i++) {
    URLClassLoader cl = new URLClassLoader(new URL[]{jar.toURI().toURL()},
            ClassLoader.getPlatformClassLoader());
    Class.forName("native0.Loader", true, cl);   // 第 2 次抛 LinkageError
}
```

底层机制也可用 30 行 JNI 代码单独证明(`/tmp/jnitest/probe.c` 第 4 步):

```c
jclass h  = (*env)->DefineClass(env, "Hidden1", NULL, buf, len); // 成功
jclass h2 = (*env)->DefineClass(env, "Hidden1", NULL, buf, len); // LinkageError
```

实测输出:

```text
[1] DefineClass("Hidden1", NULL, ...) 成功 -> class=0x7d698817aae0
[2] Hidden1.getClassLoader() = null (即 bootstrap)
[4] 第二次 DefineClass("Hidden1", NULL, ...) —— 模拟第二个插件:
    抛出异常(说明重复定义被拒绝):
    java.lang.LinkageError: loader 'bootstrap' attempted duplicate class definition
    for Hidden1. (Hidden1 is in unnamed module of loader 'bootstrap')
```

### 规避

改 `--custom-lib-dir`,让每个插件的 `nativeDir` 不同,隐藏类名随之不同:

```bash
tools/native-obfuscator.jar plugin-a.jar out-a --custom-lib-dir mylib_a -p hotspot
tools/native-obfuscator.jar plugin-b.jar out-b --custom-lib-dir mylib_b -p hotspot
```

`--custom-lib-dir` 是上游已有参数(见 `Main.java`),不需要改代码。

> 注意:`--custom-lib-dir` 会同时改变 `Loader` 的包名与原生库资源路径,
> 输出 JAR 需要重新打包(输出目录里的 `build.sh` 会自动处理)。

### 验证(已实测)

隐藏类名**确实**跟着 `nativeDir` 走 —— 同一个输入 JAR,只改 `--custom-lib-dir`:

```text
alpha  -> 隐藏类名 = 'alpha/hidden/Hidden0'
beta   -> 隐藏类名 = 'beta/hidden/Hidden0'
```

用最小复现工程做「一个 JVM 里装两个插件」的对照:

```text
######## 场景 1:两个插件都用默认 nativeDir(=native0) ########
---- 装载 plug1.jar ----
     native0.Loader 装载成功
---- 装载 plug2.jar ----
     失败: java.lang.LinkageError: loader 'bootstrap' attempted duplicate class
           definition for native0.hidden.Hidden0.

######## 场景 2:第二个插件改用 --custom-lib-dir mylib ########
---- 装载 plug1.jar ----
     native0.Loader 装载成功
---- 装载 plug2b.jar ----
     mylib.Loader 装载成功          ← 不再冲突
```

如果第二个插件里的类**真的需要** `registerNativesForClass`(即它不是全部被
黑名单排除的),后果就是彻底的:

```text
######## 场景 3:两个插件都用默认 nativeDir,且都完整转译 ########
---- 装载 plugA1.jar ----
     native0.Loader 装载成功
     rep.Victim 失败: java.lang.NoClassDefFoundError: [Lrep/Item;   ← 又一个 KI-1
---- 装载 plugA2.jar ----
     失败: java.lang.LinkageError: loader 'bootstrap' attempted duplicate class
           definition for native0.hidden.Hidden0.
     rep.Victim 失败: java.lang.NoClassDefFoundError: Could not initialize class native0.Loader
```

### 严重度为何标「严重」

- 触发条件**极容易满足**:只要服务器上装了**两个**用 native-obfuscator 转译过的插件。
  这在「同一个团队给多个插件做保护」时几乎必然发生;
- 失败**发生在插件加载阶段**,比 KI-1 更早,插件直接不可用,没有任何业务代码能跑;
- 报错信息 `loader 'bootstrap' attempted duplicate class definition for native0.hidden.Hidden0`
  完全看不出是「两个插件撞名」,排查成本很高;
- 规避成本却极低(一个 `--custom-lib-dir`),**属于必须写进使用文档的注意事项**。

### 修复方向(仅供参考,本仓库未实施)

1. 给 `nativeDir`/隐藏类名加一个内容哈希后缀(`nativeDir` 用输入 JAR 的
   SHA-256 前 8 位),从源头保证跨插件唯一;
2. 或者不再用 bootstrap,而是用插件的类加载器 `DefineClass` —— 但这会连带
   影响 KI-1,需要一起评估;
3. 无论选哪种,都要把 `if (env->ExceptionCheck()) return;` 改成
   `ExceptionClear()` + 明确报错,否则同样的“静默夭折”模式还会再犯。

---

## 附:如何自行排查

1. **先跑[一分钟自包含复现](#一分钟自包含复现不需要-minecraft)**。如果它在你这里
   就失败,说明缺陷存在且环境正确,再去看大项目;
2. 用上面的检测脚本扫一遍你自己的产物:只要有命中就可以直接上黑名单,
   **不需要看完本文的推导过程**;
3. 运行时报 `NoClassDefFoundError: [L...;` 且类名以 `[` 开头 —— 几乎可以确定是 KI-1;
4. 报 `linker 'bootstrap' attempted duplicate class definition for native0/hidden/Hidden0`
   —— 几乎可以确定是 KI-4,回忆一下是不是有第二个插件;
5. 本文所有结论都附了可重跑的步骤。若你的结论与本文不一致,
   请以你的实测为准并在 `MODIFICATIONS.md` 里补一条记录。

1. 用本文 [检测方法](#检测方法) 的脚本跑一遍 `out/cpp`,看 A 类命中数与列表;
2. 有命中 → 加入黑名单(`-b`)后重新转译,再跑一遍确认归零;
3. 观察日志里的 `Skipping <类名>` 是否覆盖了脚本列出的全部类;
4. 确认 `.so` 与 JAR 是同一次转译的产物(黑名单会改变类编号)。

## 相关文档

- `MODIFICATIONS.md` —— 本 Fork 的完整变更记录(v1.4.0 / v1.4.1 等)
- 缺陷 KI-1 的定位过程与完整证据链见本文件「证据链」一节
