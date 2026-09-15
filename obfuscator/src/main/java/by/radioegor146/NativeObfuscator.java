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
 *      1. Added private method `emitSourceProject(...)` and a call to it, which
 *         additionally emits a fully editable Java + C++ source project next to
 *         the output JAR (editable Loader.java, build.sh, SOURCE_PROJECT.md).
 *
 *      2. Removed the hardcoded `classNode.version = 52` downgrade. The class
 *         file version of transpiled classes is now preserved by default and can
 *         be forced via `setClassVersion(int)` / `--class-version`.
 *         Upstream always forced major version 52 (Java 8), which makes HotSpot
 *         silently ignore attributes introduced later than the requested version
 *         (e.g. the `Record` attribute below major 60), breaking
 *         `Class#isRecord()` and `Class#getRecordComponents()`.
 *
 *      1. 新增私有方法 `emitSourceProject(...)` 及对其的调用,在输出 JAR 旁额外
 *         生成一套可自由修改的 Java + C++ 源工程(可编辑的 Loader.java、build.sh、
 *         SOURCE_PROJECT.md)。
 *
 *      2. 移除硬编码的 `classNode.version = 52` 降级。转换后的类默认保留原始
 *         class 文件版本,并可通过 `setClassVersion(int)` / `--class-version`
 *         强制指定。上游始终强制 major 52(Java 8),而 HotSpot 会静默忽略晚于
 *         该版本引入的属性(如 major < 60 的 `Record` 属性),导致
 *         `Class#isRecord()` / `Class#getRecordComponents()` 失效。
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

import by.radioegor146.bytecode.PreprocessorRunner;
import by.radioegor146.source.CMakeFilesBuilder;
import by.radioegor146.source.ClassSourceBuilder;
import by.radioegor146.source.MainSourceBuilder;
import by.radioegor146.source.StringPool;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.gravit.launchserver.asm.ClassMetadataReader;
import ru.gravit.launchserver.asm.SafeClassWriter;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class NativeObfuscator {

    private static final Logger logger = LoggerFactory.getLogger(NativeObfuscator.class);

    private final Snippets snippets;
    private final StringPool stringPool;
    private final MethodProcessor methodProcessor;

    private final NodeCache<String> cachedStrings;
    private final NodeCache<String> cachedClasses;
    private final NodeCache<CachedMethodInfo> cachedMethods;
    private final NodeCache<CachedFieldInfo> cachedFields;

    public static class InvokeDynamicInfo {
        private final String methodName;
        private final int index;

        public InvokeDynamicInfo(String methodName, int index) {
            this.methodName = methodName;
            this.index = index;
        }

        public String getMethodName() {
            return methodName;
        }

        public int getIndex() {
            return index;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            InvokeDynamicInfo that = (InvokeDynamicInfo) o;
            return index == that.index && Objects.equals(methodName, that.methodName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(methodName, index);
        }
    }

    private HiddenMethodsPool hiddenMethodsPool;

    private int currentClassId;
    private String nativeDir;

    /**
     * Target class file <b>major</b> version for the classes written back to the output JAR.
     * <p>
     * A value of {@code <= 0} (the default) means <i>preserve the version read from the input
     * class</i>. That is the correct behaviour for every input: a Java&nbsp;8 input still ends
     * up as version {@code 52}, while a modern input keeps its own version and therefore keeps
     * its modern attributes ({@code Record}, {@code PermittedSubclasses}, ...).
     * <p>
     * Upstream hardcoded {@code 52} here. Because HotSpot silently ignores attributes that were
     * introduced after the requested class file version, that downgrade disabled the
     * {@code Record} attribute below major version {@code 60} and thus made
     * {@link Class#isRecord()} and {@link Class#getRecordComponents()} return {@code false} /
     * {@code null} — while the attribute itself was still present in the class file.
     */
    private int targetClassVersion = -1;

    public NativeObfuscator() {
        stringPool = new StringPool();
        snippets = new Snippets(stringPool);
        cachedStrings = new NodeCache<>("(cstrings[%d])");
        cachedClasses = new NodeCache<>("(cclasses[%d])");
        cachedMethods = new NodeCache<>("(cmethods[%d])");
        cachedFields = new NodeCache<>("(cfields[%d])");
        methodProcessor = new MethodProcessor(this);
    }

    /**
     * Forces every transpiled class to the given class file <b>major</b> version.
     * <p>
     * By default (never calling this method) the original version of each class is preserved,
     * which is what virtually every user wants. Forcing a lower version is only useful when the
     * output has to run on an older JVM — but note that HotSpot silently ignores attributes
     * introduced after the requested version, so e.g. forcing {@code 52} (Java 8) will break
     * {@link Class#isRecord()} and {@link Class#getRecordComponents()} on classes compiled with
     * records.
     * <p>
     * The synthetic hidden classes (`nativeN/hidden/HiddenN`) are emitted with the highest
     * version seen among the transpiled classes, or with the forced version when this method was
     * called.
     *
     * @param classVersion class file major version, e.g. {@code 52} for Java 8, {@code 61} for
     *                     Java 17, {@code 65} for Java 21. Values {@code <= 0} restore the
     *                     default "preserve the original version" behaviour.
     * @return {@code this}, so calls can be chained:
     *         {@code new NativeObfuscator().setClassVersion(61).process(...)}
     */
    public NativeObfuscator setClassVersion(int classVersion) {
        this.targetClassVersion = classVersion;
        return this;
    }

    /**
     * @return the forced class file major version, or {@code -1} when the original version of each
     *         class is preserved (the default).
     * @see #setClassVersion(int)
     */
    public int getClassVersion() {
        return targetClassVersion;
    }

    public void process(Path inputJarPath, Path outputDir, List<Path> inputLibs,
                        List<String> blackList, List<String> whiteList, String plainLibName,
                        String customLibraryDirectory,
                        Platform platform, boolean useAnnotations, boolean generateDebugJar) throws IOException {
        if (Files.exists(outputDir) && Files.isSameFile(inputJarPath.toRealPath().getParent(), outputDir.toRealPath())) {
            throw new RuntimeException("Input jar can't be in the same directory as output directory");
        }

        List<Path> libs = new ArrayList<>(inputLibs);
        libs.add(inputJarPath);
        ClassMethodFilter classMethodFilter = new ClassMethodFilter(ClassMethodList.parse(blackList), ClassMethodList.parse(whiteList), useAnnotations);
        ClassMetadataReader metadataReader = new ClassMetadataReader(libs.stream().map(x -> {
            try {
                return new JarFile(x.toFile());
            } catch (IOException ex) {
                return null;
            }
        }).collect(Collectors.toList()));

        Path cppDir = outputDir.resolve("cpp");
        Path cppOutput = cppDir.resolve("output");
        Files.createDirectories(cppOutput);

        Util.copyResource("sources/native_jvm.cpp", cppDir);
        Util.copyResource("sources/native_jvm.hpp", cppDir);
        Util.copyResource("sources/native_jvm_output.hpp", cppDir);
        Util.copyResource("sources/string_pool.hpp", cppDir);

        String projectName = "native_library";

        CMakeFilesBuilder cMakeBuilder = new CMakeFilesBuilder(projectName);
        cMakeBuilder.addMainFile("native_jvm.hpp");
        cMakeBuilder.addMainFile("native_jvm.cpp");
        cMakeBuilder.addMainFile("native_jvm_output.hpp");
        cMakeBuilder.addMainFile("native_jvm_output.cpp");
        cMakeBuilder.addMainFile("string_pool.hpp");
        cMakeBuilder.addMainFile("string_pool.cpp");

        if (platform == Platform.HOTSPOT) {
            cMakeBuilder.addFlag("USE_HOTSPOT");
        }

        MainSourceBuilder mainSourceBuilder = new MainSourceBuilder();

        File jarFile = inputJarPath.toAbsolutePath().toFile();
        try (JarFile jar = new JarFile(jarFile);
             ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(outputDir.resolve(jarFile.getName())));
             ZipOutputStream debug = generateDebugJar ? new ZipOutputStream(
                     Files.newOutputStream(outputDir.resolve("debug.jar"))) : null) {

            logger.info("Processing {}...", jarFile);

            if (customLibraryDirectory != null) {
                nativeDir = customLibraryDirectory;

                if (jar.stream().anyMatch(x -> x.getName().equals(nativeDir) ||
                                               x.getName().startsWith(nativeDir + "/"))) {
                    logger.warn("Directory '{}' already exists in input jar file", nativeDir);
                }
            } else {
                int nativeDirId = IntStream.iterate(0, i -> i + 1)
                        .filter(i -> jar.stream().noneMatch(x -> x.getName().equals("native" + i) ||
                                                                 x.getName().startsWith("native" + i + "/")))
                        .findFirst().orElseThrow(RuntimeException::new);
                nativeDir = "native" + nativeDirId;
            }

            hiddenMethodsPool = new HiddenMethodsPool(nativeDir + "/hidden");

            Integer[] classIndexReference = new Integer[]{0};

            jar.stream().forEach(entry -> {
                if (entry.getName().equals(JarFile.MANIFEST_NAME)) return;

                try {
                    if (!entry.getName().endsWith(".class")) {
                        Util.writeEntry(jar, out, entry);
                        if (debug != null) {
                            Util.writeEntry(jar, debug, entry);
                        }
                        return;
                    }

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    try (InputStream in = jar.getInputStream(entry)) {
                        Util.transfer(in, baos);
                    }
                    byte[] src = baos.toByteArray();

                    if (Util.byteArrayToInt(Arrays.copyOfRange(src, 0, 4)) != 0xCAFEBABE) {
                        Util.writeEntry(out, entry.getName(), src);
                        if (debug != null) {
                            Util.writeEntry(debug, entry.getName(), src);
                        }
                        return;
                    }

                    StringBuilder nativeMethods = new StringBuilder();
                    List<HiddenCppMethod> hiddenMethods = new ArrayList<>();

                    ClassReader classReader = new ClassReader(src);
                    ClassNode rawClassNode = new ClassNode(Opcodes.ASM9);
                    classReader.accept(rawClassNode, 0);

                    if (!classMethodFilter.shouldProcess(rawClassNode) ||
                        rawClassNode.methods.stream().noneMatch(method -> MethodProcessor.shouldProcess(method) &&
                                                                          classMethodFilter.shouldProcess(rawClassNode, method))) {
                        logger.info("Skipping {}", rawClassNode.name);
                        if (useAnnotations) {
                            ClassMethodFilter.cleanAnnotations(rawClassNode);
                            ClassWriter clearedClassWriter = new SafeClassWriter(metadataReader, 0);
                            rawClassNode.accept(clearedClassWriter);
                            Util.writeEntry(out, entry.getName(), clearedClassWriter.toByteArray());
                            if (debug != null) {
                                Util.writeEntry(debug, entry.getName(), clearedClassWriter.toByteArray());
                            }
                            return;
                        }
                        Util.writeEntry(out, entry.getName(), src);
                        if (debug != null) {
                            Util.writeEntry(debug, entry.getName(), src);
                        }
                        return;
                    }

                    logger.info("Preprocessing {}", rawClassNode.name);

                    rawClassNode.methods.stream()
                            .filter(MethodProcessor::shouldProcess)
                            .filter(methodNode -> classMethodFilter.shouldProcess(rawClassNode, methodNode))
                            .forEach(methodNode -> PreprocessorRunner.preprocess(rawClassNode, methodNode, platform));

                    ClassWriter preprocessorClassWriter = new SafeClassWriter(metadataReader, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
                    rawClassNode.accept(preprocessorClassWriter);
                    if (debug != null) {
                        Util.writeEntry(debug, entry.getName(), preprocessorClassWriter.toByteArray());
                    }
                    classReader = new ClassReader(preprocessorClassWriter.toByteArray());
                    ClassNode classNode = new ClassNode(Opcodes.ASM9);
                    classReader.accept(classNode, 0);

                    logger.info("Processing {}", classNode.name);

                    if (classNode.methods.stream().noneMatch(x -> x.name.equals("<clinit>"))) {
                        classNode.methods.add(new MethodNode(Opcodes.ASM9, Opcodes.ACC_STATIC,
                                "<clinit>", "()V", null, new String[0]));
                    }

                    cachedStrings.clear();
                    cachedClasses.clear();
                    cachedMethods.clear();
                    cachedFields.clear();

                    try (ClassSourceBuilder cppBuilder =
                                 new ClassSourceBuilder(cppOutput, classNode.name, classIndexReference[0]++, stringPool)) {
                        StringBuilder instructions = new StringBuilder();

                        for (int i = 0; i < classNode.methods.size(); i++) {
                            MethodNode method = classNode.methods.get(i);

                            if (!MethodProcessor.shouldProcess(method)) {
                                continue;
                            }

                            if (!classMethodFilter.shouldProcess(classNode, method)) {
                                continue;
                            }

                            MethodContext context = new MethodContext(this, method, i, classNode, currentClassId);
                            methodProcessor.processMethod(context);
                            instructions.append(context.output.toString().replace("\n", "\n    "));

                            nativeMethods.append(context.nativeMethods);

                            if (context.proxyMethod != null) {
                                hiddenMethods.add(new HiddenCppMethod(context.proxyMethod, context.cppNativeMethodName));
                            }

                            if ((classNode.access & Opcodes.ACC_INTERFACE) > 0) {
                                method.access &= ~Opcodes.ACC_NATIVE;
                            }
                        }

                        if (useAnnotations) {
                            ClassMethodFilter.cleanAnnotations(classNode);
                        }

                        // === Local modification: keep the original class file version ===
                        // Upstream forced `classNode.version = 52` here, which silently disabled
                        // every attribute introduced after Java 8 (e.g. `Record` below major 60).
                        int effectiveClassVersion = targetClassVersion > 0
                                ? targetClassVersion : classNode.version;
                        classNode.version = effectiveClassVersion;
                        hiddenMethodsPool.bumpClassVersion(effectiveClassVersion);
                        // === End local modification ===

                        ClassWriter classWriter = new SafeClassWriter(metadataReader,
                                ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
                        classNode.accept(classWriter);
                        Util.writeEntry(out, entry.getName(), classWriter.toByteArray());

                        cppBuilder.addHeader(cachedStrings.size(), cachedClasses.size(), cachedMethods.size(), cachedFields.size());
                        cppBuilder.addInstructions(instructions.toString());
                        cppBuilder.registerMethods(cachedStrings, cachedClasses, nativeMethods.toString(), hiddenMethods);

                        cMakeBuilder.addClassFile("output/" + cppBuilder.getHppFilename());
                        cMakeBuilder.addClassFile("output/" + cppBuilder.getCppFilename());

                        mainSourceBuilder.addHeader(cppBuilder.getHppFilename());
                        mainSourceBuilder.registerClassMethods(currentClassId, cppBuilder.getFilename());
                    }

                    currentClassId++;
                } catch (IOException ex) {
                    logger.error("Error while processing {}", entry.getName(), ex);
                }
            });

            // === Local modification: resolve the class file version of the synthetic hidden classes ===
            // Hidden classes are created lazily while classes are being processed, so the highest
            // version is only known once every entry has been handled. Apply it just before emitting.
            int hiddenClassVersion = targetClassVersion > 0
                    ? targetClassVersion : hiddenMethodsPool.getClassVersion();
            for (ClassNode hiddenClass : hiddenMethodsPool.getClasses()) {
                hiddenClass.version = hiddenClassVersion;
            }
            // === End local modification ===

            if (platform == Platform.ANDROID) {
                for (ClassNode hiddenClass : hiddenMethodsPool.getClasses()) {
                    ClassWriter classWriter = new SafeClassWriter(metadataReader, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
                    hiddenClass.accept(classWriter);
                    Util.writeEntry(out, hiddenClass.name + ".class", classWriter.toByteArray());
                }
            } else {
                for (ClassNode hiddenClass : hiddenMethodsPool.getClasses()) {
                    String hiddenClassFileName = "data_" + Util.escapeCppNameString(hiddenClass.name.replace('/', '_'));

                    cMakeBuilder.addClassFile("output/" + hiddenClassFileName + ".hpp");
                    cMakeBuilder.addClassFile("output/" + hiddenClassFileName + ".cpp");

                    mainSourceBuilder.addHeader(hiddenClassFileName + ".hpp");
                    mainSourceBuilder.registerDefine(stringPool.get(hiddenClass.name), hiddenClassFileName);

                    ClassWriter classWriter = new SafeClassWriter(metadataReader, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
                    hiddenClass.accept(classWriter);
                    byte[] rawData = classWriter.toByteArray();
                    List<Byte> data = new ArrayList<>(rawData.length);
                    for (byte b : rawData) {
                        data.add(b);
                    }

                    if (debug != null) {
                        Util.writeEntry(debug, hiddenClass.name + ".class", rawData);
                    }

                    try (BufferedWriter hppWriter = Files.newBufferedWriter(cppOutput.resolve(hiddenClassFileName + ".hpp"))) {
                        hppWriter.append("#include \"../native_jvm.hpp\"\n\n");
                        hppWriter.append("#ifndef ").append(hiddenClassFileName.toUpperCase()).append("_HPP_GUARD\n\n");
                        hppWriter.append("#define ").append(hiddenClassFileName.toUpperCase()).append("_HPP_GUARD\n\n");
                        hppWriter.append("namespace native_jvm::data::__ngen_").append(hiddenClassFileName).append(" {\n");
                        hppWriter.append("    const jbyte* get_class_data();\n");
                        hppWriter.append("    const jsize get_class_data_length();\n");
                        hppWriter.append("}\n\n");
                        hppWriter.append("#endif\n");
                    }

                    try (BufferedWriter cppWriter = Files.newBufferedWriter(cppOutput.resolve(hiddenClassFileName + ".cpp"))) {
                        cppWriter.append("#include \"").append(hiddenClassFileName).append(".hpp\"\n\n");
                        cppWriter.append("namespace native_jvm::data::__ngen_").append(hiddenClassFileName).append(" {\n");
                        cppWriter.append("    static const jbyte class_data[").append(String.valueOf(data.size())).append("] = { ");
                        cppWriter.append(data.stream().map(String::valueOf).collect(Collectors.joining(", ")));
                        cppWriter.append("};\n");
                        cppWriter.append("    static const jsize class_data_length = ").append(String.valueOf(data.size())).append(";\n\n");
                        cppWriter.append("    const jbyte* get_class_data() { return class_data; }\n");
                        cppWriter.append("    const jsize get_class_data_length() { return class_data_length; }\n");
                        cppWriter.append("}\n");
                    }
                }
            }

            String loaderClassName = nativeDir + "/Loader";

            ClassNode loaderClass;

            if (plainLibName == null) {
                ClassReader loaderClassReader = new ClassReader(Objects.requireNonNull(NativeObfuscator.class
                        .getResourceAsStream("compiletime/LoaderUnpack.class")));
                loaderClass = new ClassNode(Opcodes.ASM9);
                loaderClassReader.accept(loaderClass, 0);
                loaderClass.sourceFile = "synthetic";
                System.out.println("/" + nativeDir + "/");
            } else {
                ClassReader loaderClassReader = new ClassReader(Objects.requireNonNull(NativeObfuscator.class
                        .getResourceAsStream("compiletime/LoaderPlain.class")));
                loaderClass = new ClassNode(Opcodes.ASM9);
                loaderClassReader.accept(loaderClass, 0);
                loaderClass.sourceFile = "synthetic";
                loaderClass.methods.forEach(method -> {
                    for (int i = 0; i < method.instructions.size(); i++) {
                        AbstractInsnNode insnNode = method.instructions.get(i);
                        if (insnNode instanceof LdcInsnNode && ((LdcInsnNode) insnNode).cst instanceof String &&
                            ((LdcInsnNode) insnNode).cst.equals("%LIB_NAME%")) {
                            ((LdcInsnNode) insnNode).cst = plainLibName;
                        }
                    }
                });
            }

            ClassNode resultLoaderClass = new ClassNode(Opcodes.ASM9);
            String originalLoaderClassName = loaderClass.name;
            loaderClass.accept(new ClassRemapper(resultLoaderClass, new Remapper() {
                @Override
                public String map(String internalName) {
                    return internalName.equals(originalLoaderClassName) ? loaderClassName : internalName;
                }
            }));

            ClassWriter classWriter = new SafeClassWriter(metadataReader, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
            resultLoaderClass.accept(classWriter);
            Util.writeEntry(out, loaderClassName + ".class", classWriter.toByteArray());

            // === Local modification: emit a fully modifiable Java + C++ source project ===
            emitSourceProject(outputDir, jarFile.getName(), nativeDir, plainLibName);

            logger.info("Jar file ready!");
            Manifest mf = jar.getManifest();
            if (mf != null) {
                out.putNextEntry(new ZipEntry(JarFile.MANIFEST_NAME));
                mf.write(out);
            }
            out.closeEntry();
            metadataReader.close();
        }

        Files.write(cppDir.resolve("string_pool.cpp"), stringPool.build().getBytes(StandardCharsets.UTF_8));

        Files.write(cppDir.resolve("native_jvm_output.cpp"), mainSourceBuilder.build(nativeDir, currentClassId)
                .getBytes(StandardCharsets.UTF_8));

        Files.write(cppDir.resolve("CMakeLists.txt"), cMakeBuilder.build().getBytes(StandardCharsets.UTF_8));
    }

    public Snippets getSnippets() {
        return snippets;
    }

    public StringPool getStringPool() {
        return stringPool;
    }

    public NodeCache<String> getCachedStrings() {
        return cachedStrings;
    }

    public NodeCache<String> getCachedClasses() {
        return cachedClasses;
    }

    public NodeCache<CachedMethodInfo> getCachedMethods() {
        return cachedMethods;
    }

    public NodeCache<CachedFieldInfo> getCachedFields() {
        return cachedFields;
    }

    public String getNativeDir() {
        return nativeDir;
    }

    public HiddenMethodsPool getHiddenMethodsPool() {
        return hiddenMethodsPool;
    }

    /**
     * Local modification.
     *
     * Emits a fully modifiable Java + C++ source project next to the output jar so the
     * user can freely edit the loader and the transpiled native code, then rebuild.
     *
     * Layout:
     * <pre>
     *   outputDir/
     *   ├── &lt;jarName&gt;              # obfuscated jar
     *   ├── build.sh                # one-shot rebuild + repack script
     *   ├── SOURCE_PROJECT.md       # usage instructions
     *   ├── java/&lt;nativeDir&gt;/Loader.java   # editable loader source
     *   └── cpp/...                 # generated native sources
     * </pre>
     */
    private void emitSourceProject(Path outputDir, String jarName, String nativeDir, String plainLibName)
            throws IOException {
        String dotPackage = nativeDir.replace('/', '.').replace('\\', '.');

        // --- 1. Loader.java source ---
        String loaderSource;
        if (plainLibName == null) {
            loaderSource = Util.readResource("compiletime/LoaderUnpack.java.template")
                    .replace("package by.radioegor146.compiletime;", "package " + dotPackage + ";")
                    .replace("LoaderUnpack", "Loader");
        } else {
            loaderSource = Util.readResource("compiletime/LoaderPlain.java.template")
                    .replace("package by.radioegor146.compiletime;", "package " + dotPackage + ";")
                    .replace("LoaderPlain", "Loader")
                    .replace("%LIB_NAME%", plainLibName);
        }

        Path packageDir = outputDir.resolve("java").resolve(nativeDir);
        Files.createDirectories(packageDir);
        Files.write(packageDir.resolve("Loader.java"), loaderSource.getBytes(StandardCharsets.UTF_8));

        // --- 2. one-shot rebuild script ---
        String buildScript = Util.readResource("sources/rebuild_project.sh.template")
                .replace("@JAR_NAME@", jarName)
                .replace("@NATIVE_DIR@", nativeDir)
                .replace("@LOADER_PACKAGE@", dotPackage);
        Path buildScriptPath = outputDir.resolve("build.sh");
        Files.write(buildScriptPath, buildScript.getBytes(StandardCharsets.UTF_8));
        try {
            buildScriptPath.toFile().setExecutable(true);
        } catch (SecurityException ignored) {
            // best-effort only
        }

        // --- 3. instructions ---
        String readme = Util.readResource("sources/SOURCE_PROJECT.md.template")
                .replace("@JAR_NAME@", jarName)
                .replace("@NATIVE_DIR@", nativeDir)
                .replace("@LOADER_PACKAGE@", dotPackage);
        Files.write(outputDir.resolve("SOURCE_PROJECT.md"), readme.getBytes(StandardCharsets.UTF_8));

        logger.info("Editable source project emitted to {} (java/{}/Loader.java)", outputDir, nativeDir);
    }
}
