/*
 * ============================================================================
 *  MODIFIED FILE / 已修改文件
 * ============================================================================
 *
 *  This file is part of native-obfuscator, which is licensed under the
 *  GNU General Public License v3.0 (see the LICENSE file in the repository root).
 *
 *  本文件属于 native-obfuscator 项目,依据 GNU GPL v3.0 授权
 *  (见仓库根目录 LICENSE 文件)。
 *
 *  MODIFIED BY / 修改者:
 *      xiaofanforfabric
 *
 *  MODIFICATION DATE / 修改日期:
 *      2026-09-16
 *
 *  DESCRIPTION OF CHANGES / 修改内容:
 *      The synthetic hidden classes are no longer hardcoded to class file major
 *      version 52. They now follow the highest version seen among the transpiled
 *      classes, so that they can reference them without a version downgrade.
 *
 *      合成隐藏类不再硬编码为 class 文件 major 版本 52,而是跟随被转换类中出现
 *      的最高版本,以便在不降级的前提下引用它们。
 *
 *      See MODIFICATIONS.md in the repository root for the full change log.
 *      完整变更记录见仓库根目录 MODIFICATIONS.md。
 *
 *  ORIGINAL WORK / 原始作品:
 *      Copyright (C) radioegor146 and contributors
 *      https://github.com/radioegor146/native-obfuscator
 * ============================================================================
 */

package by.radioegor146;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;

public class HiddenMethodsPool {

    private final String baseName;

    /**
     * Simple-name prefix of the synthetic hidden classes.
     * <p>
     * Defaults to {@code Hidden} (upstream behaviour); AntiHackerX passes a random
     * name so that two packed plugins can never collide: the hidden classes are
     * defined into the <b>bootstrap</b> loader by {@code DefineClass(..., nullptr, ...)},
     * so their names are global across the whole JVM.
     */
    private final String hiddenPrefix;

    /**
     * Class file <b>major</b> version used for the synthetic hidden classes. It is raised to the
     * highest version seen among the transpiled classes, so a hidden class is never emitted with a
     * version lower than the classes it has to reference.
     * <p>
     * Upstream hardcoded {@code 52} at the point of use; {@code NativeObfuscator} did the same for
     * the transpiled classes themselves.
     */
    private int classVersion = Opcodes.V1_8;

    public HiddenMethodsPool(String baseName) {
        this(baseName, "Hidden");
    }

    public HiddenMethodsPool(String baseName, String hiddenPrefix) {
        this.baseName = baseName;
        this.hiddenPrefix = hiddenPrefix == null || hiddenPrefix.isEmpty() ? "Hidden" : hiddenPrefix;
    }

    /**
     * Raises the class file version used for synthetic hidden classes. Never lowers it, so the
     * result is the maximum of every version passed in.
     *
     * @param version class file major version of a transpiled class
     */
    public void bumpClassVersion(int version) {
        if (version > classVersion) {
            classVersion = version;
        }
    }

    /**
     * @return the highest class file major version seen so far; never below
     *         {@code 52} ({@link Opcodes#V1_8}).
     */
    public int getClassVersion() {
        return classVersion;
    }

    private final HashMap<String, Integer> namePool = new HashMap<>();
    private final HashMap<String, HashMap<String, HiddenMethod>> methods = new HashMap<>();
    private final List<ClassNode> classes = new ArrayList<>();

    public static class HiddenMethod {

        private final ClassNode classNode;
        private final MethodNode methodNode;

        private HiddenMethod(ClassNode classNode, MethodNode methodNode) {
            this.classNode = classNode;
            this.methodNode = methodNode;
        }

        public ClassNode getClassNode() {
            return classNode;
        }

        public MethodNode getMethodNode() {
            return methodNode;
        }
    }

    public HiddenMethod getMethod(String name, String desc, Consumer<MethodNode> creator) {
        HiddenMethod existingMethod = methods.computeIfAbsent(name, unused -> new HashMap<>()).get(desc);
        if (existingMethod != null) {
            return existingMethod;
        }

        String newName = name + namePool.compute(name, (otherName, value) -> value == null ? 0 : value + 1);
        MethodNode newMethod = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_BRIDGE |
                Opcodes.ACC_SYNTHETIC, newName, desc, null, new String[0]);
        creator.accept(newMethod);
        ClassNode classNode = classes.isEmpty() ? null : classes.get(classes.size() - 1).methods.size() > 10000 ? null : classes.get(classes.size() - 1);
        if (classNode == null) {
            classNode = new ClassNode(Opcodes.ASM9);
            classNode.access = Opcodes.ACC_PUBLIC;
            classNode.version = classVersion;
              classNode.name = baseName + "/" + hiddenPrefix + classes.size();
            classNode.superName = Type.getInternalName(Object.class);
            classes.add(classNode);
        }
        classNode.methods.add(newMethod);
        HiddenMethod hiddenMethod = new HiddenMethod(classNode, newMethod);
        methods.computeIfAbsent(name, unused -> new HashMap<>()).put(desc, hiddenMethod);
        return hiddenMethod;
    }

    public List<ClassNode> getClasses() {
        return classes;
    }
}
