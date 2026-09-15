#!/usr/bin/env bash
#
# KI-4(隐藏类定义进 bootstrap 域,不同插件互相冲突)最小复现脚本。
#
# 用法:  bash repro-ki4.sh <obfuscator.jar 路径> [工作目录]
#
# 场景:同一个 JVM 里装载两个都用 native-obfuscator 转译过的插件。
#   场景 1:两个插件都用默认 nativeDir(native0)      -> 预期 LinkageError
#   场景 2:第二个插件改用 --custom-lib-dir mylib     -> 预期两个都成功
#
# 对应文档: native-obfuscator/KNOWN_ISSUES.md 的 KI-4 一节。

set -u

OBF="${1:?用法: bash repro-ki4.sh <obfuscator.jar> [工作目录]}"
WORK="${2:-/tmp/repro-ki4}"

command -v javac >/dev/null || { echo "缺少 javac"; exit 1; }
command -v cmake >/dev/null || { echo "缺少 cmake"; exit 1; }
command -v g++   >/dev/null || { echo "缺少 g++";   exit 1; }
[ -f "$OBF" ] || { echo "找不到 obfuscator JAR: $OBF"; exit 1; }
OBF="$( cd "$( dirname "$OBF" )" && pwd )/$( basename "$OBF" )"

# 顺带验证隐藏类名确实跟随 nativeDir(不需要 Minecraft,也不需要真实 .so)
WORK2="$WORK/cld"

echo "obfuscator : $OBF"
echo "工作目录   : $WORK"

rm -rf "$WORK"
mkdir -p "$WORK/src/rep"

cat > "$WORK/src/rep/Item.java" <<'EOF'
package rep;
public class Item { public int v; }
EOF

# 故意不用数组,避免 KI-1 抢戏;只关心「Loader 能不能起来」
cat > "$WORK/src/rep/Victim.java" <<'EOF'
package rep;
public class Victim {
    public static final int N = 3;
}
EOF

cat > "$WORK/src/Main.java" <<'EOF'
public class Main { public static void main(String[] a) {} }
EOF

echo
echo "==> 1. 编译测试工程"
( cd "$WORK/src" && javac -d "$WORK/classes" rep/Item.java rep/Victim.java Main.java ) || exit 1
( cd "$WORK/classes" && jar cf "$WORK/rep.jar" . ) || exit 1

# ------------------------------------------------- 先验证 nativeDir -> 隐藏类名
echo
echo "==> 2. 验证隐藏类名跟随 --custom-lib-dir"
mkdir -p "$WORK2/src/p"
cat > "$WORK2/src/p/X.java" <<'EOF'
package p;
public class X { static { int n = 0; } }
EOF
( cd "$WORK2/src" && javac -d "$WORK2/classes" p/X.java ) || exit 1
( cd "$WORK2/classes" && jar cf "$WORK2/cld.jar" . ) || exit 1
for d in alpha beta; do
    ( cd "$WORK2" && JAVA_TOOL_OPTIONS= java -jar "$OBF" cld.jar "$d" \
        --custom-lib-dir "$d" -p hotspot > "$d.log" 2>&1 ) || { cat "$WORK2/$d.log"; exit 1; }
    # 生成物里形状是 output/data_alpha_hidden_Hidden0.cpp,名字是 string_pool 里的
    # "alpha/hidden/Hidden0",这里直接看 DefineClass 的落地文件名即可
    f="$( ls "$WORK2/$d/cpp/output/" 2>/dev/null | grep -o 'data_.*_hidden_Hidden[0-9]*' | head -1 )"
    printf '    --custom-lib-dir %-6s -> 隐藏类文件 = %s\n' "$d" "${f:-<未找到>}"
done
echo "    (DefineClass 的名字即把下划线还原成斜杠,可在 native_jvm_output.cpp 里确认)"
grep -o 'data_[a-z]*_hidden_Hidden[0-9]*' "$WORK2/alpha/cpp/native_jvm_output.cpp" | head -1 | sed 's/^/    alpha 引用: /'
grep -o 'data_[a-z]*_hidden_Hidden[0-9]*' "$WORK2/beta/cpp/native_jvm_output.cpp"  | head -1 | sed 's/^/    beta  引用: /'

# ---------------------------------------------------------------- 转译 + 构建
build_variant() {
    local name="$1"; shift
    echo
    echo "==> 转译 $name ($*)"
    ( cd "$WORK" && JAVA_TOOL_OPTIONS= java -jar "$OBF" rep.jar "$name" -p hotspot "$@" ) \
        || return 1
    echo "==> 编译 $name 的原生库"
    ( cd "$WORK/$name" \
        && cmake -S cpp -B cpp/build -DCMAKE_BUILD_TYPE=Release > cmake.log 2>&1 \
        && cmake --build cpp/build -j4                         > build.log 2>&1 \
        && JAVA_TOOL_OPTIONS= bash build.sh                    > pack.log  2>&1 ) \
        || { echo "!! $name 构建失败,详见 $WORK/$name/*.log"; return 1; }
    echo "    $name 构建完成"
}

build_variant plug1                     || exit 1
build_variant plug2                     || exit 1
build_variant plug2b --custom-lib-dir mylib || exit 1

# ---------------------------------------------------------------- 同 JVM 双装载
cat > "$WORK/TwoPlugins.java" <<'EOF'
import java.io.File;
import java.net.*;
import java.util.*;

public class TwoPlugins {
    // 找到 JAR 里的 /Loader.class,兼容任意 --custom-lib-dir
    static String loaderName(File f) throws Exception {
        try (java.util.jar.JarFile jf = new java.util.jar.JarFile(f)) {
            for (Enumeration<java.util.jar.JarEntry> e = jf.entries(); e.hasMoreElements(); ) {
                String n = e.nextElement().getName();
                if (n.endsWith("/Loader.class"))
                    return n.substring(0, n.length() - 6).replace('/', '.');
            }
        }
        return null;
    }
    public static void main(String[] a) throws Exception {
        for (File f : new File[]{ new File(a[0]), new File(a[1]) }) {
            System.out.println("  ---- 装载 " + f.getName() + " ----");
            URLClassLoader cl = new URLClassLoader(new URL[]{ f.toURI().toURL() },
                    ClassLoader.getPlatformClassLoader());
            String ln = loaderName(f);
            try {
                Class.forName(ln, true, cl);
                System.out.println("       " + ln + " 装载成功");
            } catch (Throwable t) {
                System.out.println("       失败: " + t);
                Throwable x = t.getCause();
                while (x != null) {
                    System.out.println("         -> " + x.getClass().getName() + ": " + x.getMessage());
                    x = x.getCause();
                }
            }
        }
    }
}
EOF
( cd "$WORK" && javac -d . TwoPlugins.java ) || exit 1

echo
echo "######## 场景 1:两个插件都用默认 nativeDir(都是 native0) ########"
( cd "$WORK" && JAVA_TOOL_OPTIONS= java -cp . TwoPlugins plug1/rep.jar plug2/rep.jar ) 2>&1 \
    | grep -v JAVA_TOOL_OPTIONS
S1=$?

echo
echo "######## 场景 2:第二个插件改用 --custom-lib-dir mylib ########"
( cd "$WORK" && JAVA_TOOL_OPTIONS= java -cp . TwoPlugins plug1/rep.jar plug2b/rep.jar ) 2>&1 \
    | grep -v JAVA_TOOL_OPTIONS

echo
echo "=========================== 判读 ==========================="
echo "  场景 1 出现 'attempted duplicate class definition for native0.hidden.Hidden0' -> KI-4 复现"
echo "  场景 2 两个 Loader 都装载成功                                   -> 规避有效"
