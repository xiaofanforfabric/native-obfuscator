#!/usr/bin/env bash
#
# KI-1(数组类型的类解析在隐藏类中必然失败)最小自包含复现脚本。
#
# 用法:  bash repro-ki1.sh <obfuscator.jar 路径> [工作目录]
#
# 脚本会:
#   1. 生成 3 个类的 Java 工程(/tmp 或指定目录);
#   2. [A] 不加黑名单转译 -> 编译 -> 打包 -> 运行,预期失败;
#   3. [B] 用黑名单排除 rep/Victim -> 编译 -> 打包 -> 运行,预期成功。
#
# 对应文档: native-obfuscator/KNOWN_ISSUES.md 的「一分钟自包含复现」一节。
#
# 注意: 运行 obfuscator JAR 与编译出来的 .so 时必须清空 JAVA_TOOL_OPTIONS,
#       否则代理设置会被传进 JVM。

set -u

OBF="${1:?用法: bash repro-ki1.sh <obfuscator.jar> [工作目录]}"
WORK="${2:-/tmp/repro-ki1}"

echo "obfuscator : $OBF"
echo "工作目录   : $WORK"

command -v javac  >/dev/null || { echo "缺少 javac";  exit 1; }
command -v cmake  >/dev/null || { echo "缺少 cmake";  exit 1; }
command -v g++    >/dev/null || { echo "缺少 g++";    exit 1; }
[ -f "$OBF" ] || { echo "找不到 obfuscator JAR: $OBF"; exit 1; }
# 后面会 cd 进工作目录,这里统一转成绝对路径
OBF="$( cd "$( dirname "$OBF" )" && pwd )/$( basename "$OBF" )"
echo "obfuscator : $OBF (绝对路径)"

rm -rf "$WORK"
mkdir -p "$WORK/src/rep"

cat > "$WORK/src/rep/Item.java" <<'EOF'
package rep;
public class Item { public int v; }
EOF

cat > "$WORK/src/rep/Victim.java" <<'EOF'
package rep;
public class Victim {
    public static final int N;
    static {
        Object o = new Item[]{ new Item(), new Item(), new Item() };
        Item[] arr = (Item[]) o;      // javac 生成 CHECKCAST [Lrep/Item;
        N = arr.length;
    }
}
EOF

cat > "$WORK/src/Main.java" <<'EOF'
public class Main {
    public static void main(String[] args) {
        System.out.println("Victim.N = " + rep.Victim.N);
    }
}
EOF

echo
echo "==> 1. 编译测试工程"
( cd "$WORK/src" && javac -d "$WORK/classes" rep/Item.java rep/Victim.java Main.java ) || exit 1
( cd "$WORK/classes" && jar cf "$WORK/rep.jar" . ) || exit 1

echo
echo "==> 2. 确认 javac 真的生成了数组类型的 checkcast"
javap -c -p -cp "$WORK/classes" rep.Victim | grep -E 'checkcast.*\[L' \
    || { echo "!! javac 未生成 [Lrep/Item; 的 checkcast,复现前提不成立"; exit 1; }

# ---------------------------------------------------------------- 转译 + 构建
build_variant() {
    local name="$1"; shift
    echo
    echo "==> 转译变体 $name ($*)"
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

printf 'rep/Victim\n' > "$WORK/bl.txt"

build_variant outA          || exit 1
build_variant outB -b "$WORK/bl.txt" || exit 1

# ---------------------------------------------------------------- 运行对照
run_variant() {
    local name="$1" expect="$2"
    echo
    echo "-------------------- [$name] 期望:$expect --------------------"
    local out
    out="$( JAVA_TOOL_OPTIONS= java -cp "$WORK/$name/rep.jar" Main 2>&1 )"
    echo "$out" | sed 's/^/    /'
    echo
    case "$name:$out" in
        outA:*"NoClassDefFoundError: [Lrep/Item;"*)
            echo "    => [A] 复现成功:KI-1 缺陷确实存在";            A_OK=1 ;;
        outB:*"Victim.N = 3"*)
            echo "    => [B] 规避成功:黑名单排除后功能正常";          B_OK=1 ;;
        *)
            echo "    => !! 结果与预期不符,请检查日志" ;;
    esac
}

A_OK=0; B_OK=0
run_variant outA "NoClassDefFoundError: [Lrep/Item;"
run_variant outB "Victim.N = 3"

echo
echo "=========================== 总结 ==========================="
[ "$A_OK" = 1 ] && echo "  [A] 未加黑名单 -> 复现 KI-1        : 通过" \
                || echo "  [A] 未加黑名单 -> 复现 KI-1        : **未通过**"
[ "$B_OK" = 1 ] && echo "  [B] 加黑名单   -> 缺陷被规避        : 通过" \
                || echo "  [B] 加黑名单   -> 缺陷被规避        : **未通过**"
echo "  KI-2(MULTIANEWARRAY):本工程不含多维数组,未覆盖"
echo "  KI-3(命名拼接)      :静态分析项,运行时不可观测"
echo
[ "$A_OK" = 1 ] && [ "$B_OK" = 1 ] && exit 0 || exit 1
