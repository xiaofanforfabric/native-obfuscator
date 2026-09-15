/*
 * Native Obfuscator - Translates JVM bytecode to C++ and compiles it into a native library.
 * Copyright (C) 2018-2025  by.radioegor146
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * ---------------------------------------------------------------------------
 * 本文件已被本地修改 / This file has been locally modified.
 * 修改内容 / Modification:
 *   修复 native 函数名拼接歧义导致的 C++ 重定义错误。
 *   Fixed ambiguous native function name concatenation that produced duplicate
 *   C++ definitions (see `preProcess` below for the full explanation).
 * 详见 MODIFICATIONS.md / See MODIFICATIONS.md for details.
 * ---------------------------------------------------------------------------
 */

package by.radioegor146.special;

import by.radioegor146.MethodContext;
import by.radioegor146.Util;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class DefaultSpecialMethodProcessor implements SpecialMethodProcessor {

    @Override
    public String preProcess(MethodContext context) {
        if (Util.getFlag(context.clazz.access, Opcodes.ACC_INTERFACE)) {
            List<Type> arguments = (Arrays.stream(Type.getArgumentTypes(context.method.desc)).collect(Collectors.toList()));
            arguments.add(0, Type.getType(Object.class));
            String resultDesc = Type.getMethodDescriptor(Type.getReturnType(context.method.desc), arguments.toArray(new Type[0]));

            String methodName = String.format("interfacestatic_%d_%d", context.classIndex, context.methodIndex);
            context.proxyMethod = context.obfuscator.getHiddenMethodsPool()
                    .getMethod(methodName, resultDesc, methodNode -> {
                        methodNode.signature = context.method.signature;
                        methodNode.access = Opcodes.ACC_NATIVE | Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC | Opcodes.ACC_BRIDGE;
                        methodNode.visibleAnnotations = new ArrayList<>();
                        methodNode.visibleAnnotations.add(new AnnotationNode("Ljava/lang/invoke/LambdaForm$Hidden;"));
                        methodNode.visibleAnnotations.add(new AnnotationNode("Ljdk/internal/vm/annotation/Hidden;"));
                    });
            return methodName;
        }
        context.method.access |= Opcodes.ACC_NATIVE;
        // === Local modification: fix ambiguous native function name (see MODIFICATIONS.md) ===
        // Upstream (v3.5.4r) used:
        //
        //     return "native_" + context.method.name + context.methodIndex;
        //
        // This concatenates the method name and the method index without any
        // separator, so the trailing digits of the name and the index become
        // indistinguishable. `Util.escapeCppNameString` only rewrites
        // non-alphanumeric characters (keeping `_` intact), so two distinct
        // methods can produce the exact same C++ symbol and the generated
        // `.cpp` then fails to compile with:
        //
        //     error: redefinition of 'void ...::__ngen_native_...'
        //
        // Real-world example (GrimAC Bukkit plugin,
        // ac/grim/grimac/utils/blockplace/BlockPlaceResult):
        //     index 9,  `lambda$static$77` -> "__ngen_native_lambdau36staticu36779"
        //     index 79, `lambda$static$7`  -> "__ngen_native_lambdau36staticu36779"
        //
        // The `_` separator makes the mapping (name, index) -> symbol injective:
        // the index is a pure digit sequence, so the substring after the last
        // `_` always identifies the index unambiguously.
        return "native_" + context.method.name + "_" + context.methodIndex;
        // === End local modification ===
    }

    @Override
    public void postProcess(MethodContext context) {
        context.method.instructions.clear();
        if (Util.getFlag(context.clazz.access, Opcodes.ACC_INTERFACE)) {
            InsnList list = new InsnList();

            if (Util.getFlag(context.method.access, Opcodes.ACC_STATIC)) {
                list.add(new LdcInsnNode(Type.getObjectType(context.clazz.name)));
            }

            int localVarsPosition = 0;

            for (Type arg : context.argTypes) {
                list.add(new VarInsnNode(arg.getOpcode(Opcodes.ILOAD), localVarsPosition));
                localVarsPosition += arg.getSize();
            }
            if (context.nativeMethod == null) {
                throw new RuntimeException("Native method not created?!");
            }
            list.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                    context.proxyMethod.getClassNode().name,
                    context.proxyMethod.getMethodNode().name,
                    context.proxyMethod.getMethodNode().desc, false));
            list.add(new InsnNode(Type.getReturnType(context.method.desc).getOpcode(Opcodes.IRETURN)));
            context.method.instructions = list;
        }
    }
}
