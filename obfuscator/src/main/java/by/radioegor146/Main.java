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
 *      Added the `--class-version` option, which forwards to
 *      `NativeObfuscator#setClassVersion(int)`. When omitted, the original class
 *      file version of each class is preserved (upstream always forced 52).
 *
 *      新增 `--class-version` 选项,转调 `NativeObfuscator#setClassVersion(int)`。
 *      不指定时保留各类的原始 class 文件版本(上游始终强制 52)。
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

import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public class Main {

    /**
     * 工具版本号。
     *
     * <p>⚠️ 必须与发布用的 git 标签保持一致:标签形如 <code>v1.4.5</code>,
     * 这里写 <code>1.4.5</code>(无 <code>v</code> 前缀)。AntiHackerX 通过对比本字符串
     * 与 GitHub <code>releases/latest</code> 的 <code>tag_name</code> 来判断本地依赖
     * 是否过期,所以发标签前记得同步改这里。</p>
     *
     * <p>上游原本填的是它自己的版本号(3.5.4r),而且发新版时常常忘记更新;
     * 本 Fork 改为跟随自己的标签,以便自动检测更新。</p>
     */
    private static final String VERSION = "1.4.8";

    @CommandLine.Command(name = "native-obfuscator", mixinStandardHelpOptions = true, version = "native-obfuscator " + VERSION,
            description = "Transpiles .jar file into .cpp files and generates output .jar file")
    private static class NativeObfuscatorRunner implements Callable<Integer> {

        @CommandLine.Parameters(index = "0", description = "Jar file to transpile")
        private File jarFile;

        @CommandLine.Parameters(index = "1", description = "Output directory")
        private String outputDirectory;

        @CommandLine.Option(names = {"-l", "--libraries"}, description = "Directory for dependent libraries")
        private File librariesDirectory;

        @CommandLine.Option(names = {"-b", "--black-list"}, description = "File with a list of blacklist classes/methods for transpilation")
        private File blackListFile;

        @CommandLine.Option(names = {"-w", "--white-list"}, description = "File with a list of whitelist classes/methods for transpilation")
        private File whiteListFile;

        @CommandLine.Option(names = {"--plain-lib-name"}, description = "Plain library name for LoaderPlain")
        private String libraryName;

        @CommandLine.Option(names = {"--custom-lib-dir"}, description = "Custom library directory for LoaderUnpack")
        private String customLibraryDirectory;

        @CommandLine.Option(names = {"--loader-name"}, description = "Simple name of the generated loader class (default: Loader)")
        private String loaderName;

        @CommandLine.Option(names = {"--hidden-name"}, description = "Simple-name prefix of synthetic hidden classes (default: Hidden)")
        private String hiddenName;

        @CommandLine.Option(names = {"-p", "--platform"}, defaultValue = "hotspot",
                description = "Target platform: hotspot - standard standalone HotSpot JRE, std_java - java standard, android - for Android builds (w/o DefineClass)")
        private Platform platform;

        @CommandLine.Option(names = {"-a", "--annotations"}, description = "Use annotations to ignore/include native obfuscation")
        private boolean useAnnotations;

        @CommandLine.Option(names = {"--debug"}, description = "Enable generation of debug .jar file (non-executable)")
        private boolean generateDebugJar;

        @CommandLine.Option(names = {"--class-version"}, paramLabel = "<version>",
                converter = ClassVersionConverter.class,
                description = {
                        "Class file version to write for the transpiled classes.",
                        "Accepts a class file major version (>= 45, e.g. 52 = Java 8, 61 = Java 17, 65 = Java 21)",
                        "or a Java release number (<= 44, e.g. 8, 11, 17, 21).",
                        "Default: preserve the original version of each class.",
                        "Note: forcing a version lower than the input makes HotSpot silently ignore",
                        "attributes introduced later (e.g. Record below major 60 breaks Class#isRecord())."
                })
        private Integer classVersion;

        @Override
        public Integer call() throws Exception {
            List<Path> libs = new ArrayList<>();
            if (librariesDirectory != null) {
                Files.walk(librariesDirectory.toPath(), FileVisitOption.FOLLOW_LINKS)
                        .filter(f -> f.toString().endsWith(".jar") || f.toString().endsWith(".zip"))
                        .forEach(libs::add);
            }

            List<String> blackList = new ArrayList<>();
            if (blackListFile != null) {
                blackList = Files.readAllLines(blackListFile.toPath(), StandardCharsets.UTF_8);
            }

            List<String> whiteList = null;
            if (whiteListFile != null) {
                whiteList = Files.readAllLines(whiteListFile.toPath(), StandardCharsets.UTF_8);
            }

            NativeObfuscator obfuscator = new NativeObfuscator();
            if (classVersion != null) {
                obfuscator.setClassVersion(classVersion);
            }
            obfuscator.setLoaderName(loaderName);
            obfuscator.setHiddenName(hiddenName);

            obfuscator.process(jarFile.toPath(), Paths.get(outputDirectory),
                    libs, blackList, whiteList, libraryName, customLibraryDirectory, platform, useAnnotations, generateDebugJar);

            return 0;
        }
    }

    /**
     * Converts a user supplied {@code --class-version} value into a class file major version.
     * <p>
     * Class file major versions start at {@code 45} (Java 1.1), while today's Java release
     * numbers are far below that, so the two ranges never overlap: values below {@code 45} are
     * read as release numbers ({@code 8} -&gt; {@code 52}, {@code 17} -&gt; {@code 61}) and values
     * of {@code 45} and above are read as raw major versions.
     * <p>
     * Implementing this as a picocli converter (instead of validating inside {@code call()})
     * means a bad value is reported as a normal usage error rather than as a stack trace.
     */
    static class ClassVersionConverter implements CommandLine.ITypeConverter<Integer> {

        @Override
        public Integer convert(String value) {
            int parsed;
            try {
                parsed = Integer.parseInt(value.trim());
            } catch (NumberFormatException ex) {
                throw new CommandLine.TypeConversionException("'" + value + "' is not a number "
                        + "(expected a Java release number like 17, or a class file major version like 61)");
            }
            if (parsed <= 0) {
                throw new CommandLine.TypeConversionException("must be a positive number, got " + parsed);
            }
            return parsed < 45 ? parsed + 44 : parsed;
        }
    }

    public static void main(String[] args) throws IOException {
        System.exit(new CommandLine(new NativeObfuscatorRunner())
                .setCaseInsensitiveEnumValuesAllowed(true).execute(args));
    }
}
