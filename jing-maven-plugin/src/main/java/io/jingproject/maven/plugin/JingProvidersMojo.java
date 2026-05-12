package io.jingproject.maven.plugin;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.IOException;
import java.lang.classfile.Attributes;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.attribute.ModuleAttribute;
import java.lang.classfile.attribute.ModuleProvideInfo;
import java.lang.constant.ClassDesc;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

@Mojo(name = "process-jing-providers", defaultPhase = LifecyclePhase.PROCESS_CLASSES)
@SuppressWarnings("unused")
public final class JingProvidersMojo extends AbstractMojo {
    // must be strictly consistent with jing-common-processor module
    private static final String FILE_NAME = "jing-providers.json";
    private static final String CONSUMED_FILE_NAME = "jing-providers-consumed.json";
    private static final String META_INF = "META-INF";
    private static final String SERVICES = "services";
    private static final String MODULE_INFO_CLASS = "module-info.class";

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    @Parameter(defaultValue = "main")
    private String scope; // main or test

    private String outputDir() throws MojoFailureException {
        if(scope == null || scope.isBlank()) {
            throw new MojoFailureException("empty scope");
        }else if(scope.equals("main")) {
            return project.getBuild().getOutputDirectory();
        } else if(scope.equals("test")) {
            return project.getBuild().getTestOutputDirectory();
        } else {
            throw new MojoFailureException("invalid scope : " + scope);
        }
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Path outputDir = Path.of(outputDir());
        getLog().info("searching for " + FILE_NAME + " file at directory: " + outputDir.toAbsolutePath());
        Path targetPath = outputDir.resolve(FILE_NAME);
        if (Files.isRegularFile(targetPath)) {
            getLog().info("found " + FILE_NAME + " file : " + targetPath.toAbsolutePath());
            processJingProviderFile(targetPath);
            consumeJingProviderFile(targetPath);
        } else {
            throw new MojoExecutionException(FILE_NAME + " file not found, maybe jing-common-processor is not effective");
        }
    }

    private void processJingProviderFile(Path path) throws MojoExecutionException, MojoFailureException {
        if (!Files.isReadable(path)) {
            throw new MojoExecutionException(FILE_NAME + " is not readable");
        }
        byte[] content;
        try {
            content = Files.readAllBytes(path);
        } catch (IOException e) {
            throw new MojoExecutionException("Cannot read from " + FILE_NAME + " file", e);
        }
        Map<String, Set<String>> data = parseProviderData(content);
        if(data.isEmpty()) {
            return ;
        }
        Path targetDirPath = Path.of(outputDir(), META_INF, SERVICES);
        try {
            Files.createDirectories(targetDirPath);
        } catch (IOException e) {
            throw new MojoExecutionException("cannot create " + META_INF + "/" + SERVICES + " directory", e);
        }
        for (Map.Entry<String, Set<String>> entry : data.entrySet()) {
            String key = entry.getKey();
            Set<String> lines = entry.getValue();
            getLog().info("processing key : " + key);
            getLog().info("processing lines : " + lines);
            Path targetPath = targetDirPath.resolve(key);
            try {
                Files.write(targetPath, lines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            } catch (IOException e) {
                throw new MojoExecutionException("cannot write SPI file : " + targetPath.toAbsolutePath(), e);
            }
        }
        Path moduleInfoPath = Path.of(outputDir(), MODULE_INFO_CLASS);
        if (Files.isRegularFile(moduleInfoPath)) {
            getLog().info("found module-info : " + moduleInfoPath.toAbsolutePath());
            byte[] bytecodes;
            try {
                bytecodes = Files.readAllBytes(moduleInfoPath);
                bytecodes = updateModuleInfoByteCodes(bytecodes, data);
                Files.write(moduleInfoPath, bytecodes, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
            } catch (IOException e) {
                throw new MojoExecutionException("cannot perform IO operations when modifying module-info", e);
            }
        }
    }

    private static Map<String, Set<String>> parseProviderData(byte[] content) throws MojoFailureException {
        if(content == null || content.length < 2) {
            throw new MojoFailureException("empty provider data");
        }
        Map<String, Set<String>> r = new HashMap<>();
        String key;
        Set<String> set = new HashSet<>();
        int idx1 = 0, idx2;
        idx1 = searchByte(content, idx1, b -> b == (byte) '{');
        idx2 = searchByte(content, idx1, b -> b != (byte) ' ' && b != (byte) '\t' && b != (byte) '\r' && b != (byte) '\n');
        if(content[idx2 - 1] == (byte) '}') {
            return r;
        }
        for (; ; ) {
            idx1 = searchByte(content, idx1, b -> b == (byte) '"');
            idx2 = searchByte(content, idx1, b -> b == (byte) '"');
            key = new String(content, idx1, idx2 - idx1 - 1, StandardCharsets.UTF_8);
            idx1 = searchByte(content, idx2, b -> b == (byte) ':');
            idx1 = searchByte(content, idx1, b -> b == (byte) '[');
            do {
                idx1 = searchByte(content, idx1, b -> b == (byte) '"');
                idx2 = searchByte(content, idx1, b -> b == (byte) '"');
                if (!set.add(new String(content, idx1, idx2 - idx1 - 1, StandardCharsets.UTF_8))) {
                    throw new MojoFailureException(FILE_NAME + " contains duplicate data");
                }
                idx1 = searchByte(content, idx2, b -> b == (byte) ',' || b == (byte) ']');
            } while (content[idx1 - 1] == (byte) ',');
            if (r.put(key, set) != null) {
                throw new MojoFailureException(FILE_NAME + " contains duplicate key");
            }
            set = new HashSet<>();
            idx1 = searchByte(content, idx1, b -> b == (byte) ',' || b == (byte) '}');
            if (content[idx1 - 1] == (byte) '}') {
                return r;
            }
        }
    }

    @FunctionalInterface
    interface ByteConsumer {
        boolean accept(byte b);
    }

    private static int searchByte(byte[] content, int fromIndex, ByteConsumer consumer) throws MojoFailureException {
        if (fromIndex < 0 || fromIndex >= content.length) {
            throw new MojoFailureException("File format corrupted");
        }
        for (int i = fromIndex; i < content.length; i++) {
            if (consumer.accept(content[i])) {
                return i + 1;
            }
        }
        throw new MojoFailureException("invalid json structure");
    }

    private static byte[] updateModuleInfoByteCodes(byte[] bytecodes, Map<String, Set<String>> data) throws MojoFailureException {
        ClassFile cf = ClassFile.of();
        ClassModel model = cf.parse(bytecodes);
        ModuleAttribute currentAttribute = model.findAttribute(Attributes.module()).orElseThrow(() -> new MojoFailureException("failed to get module attributes"));
        List<ModuleProvideInfo> currentProvides = currentAttribute.provides();
        for (ModuleProvideInfo m : currentProvides) {
            if (data.containsKey(m.provides().asSymbol().displayName())) {
                throw new MojoFailureException("Module-info already contains generated SPI directives");
            }
        }
        List<ModuleProvideInfo> newProvides = new ArrayList<>(currentProvides);
        for (Map.Entry<String, Set<String>> entry : data.entrySet()) {
            String key = entry.getKey();
            Set<String> set = entry.getValue();
            newProvides.add(ModuleProvideInfo.of(ClassDesc.of(key), set.stream().map(ClassDesc::of).toList()));
        }
        ModuleAttribute newAttribute = ModuleAttribute.of(currentAttribute.moduleName(), currentAttribute.moduleFlagsMask(), currentAttribute.moduleVersion().orElse(null),
                currentAttribute.requires(), currentAttribute.exports(), currentAttribute.opens(), currentAttribute.uses(), newProvides);
        return cf.buildModule(newAttribute);
    }

    private void consumeJingProviderFile(Path path) throws MojoExecutionException {
        try {
            Path consumedPath = path.resolveSibling(CONSUMED_FILE_NAME);
            Files.move(path, consumedPath, StandardCopyOption.REPLACE_EXISTING);
            getLog().info(FILE_NAME + " consumed, rename to " + CONSUMED_FILE_NAME);
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to rename " + FILE_NAME + " to " + CONSUMED_FILE_NAME + " after processing", e);
        }
    }

}
