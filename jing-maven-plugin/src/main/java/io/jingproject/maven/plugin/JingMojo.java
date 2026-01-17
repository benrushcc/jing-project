package io.jingproject.maven.plugin;

import org.apache.maven.model.Resource;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;

@Mojo(name = "process-jing-providers", defaultPhase = LifecyclePhase.PROCESS_CLASSES)
@SuppressWarnings("unused")
public final class JingMojo extends AbstractMojo {

    private static final String FILE_NAME = "jing-providers.json";

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        for (Resource resource : project.getResources()) {
            Path resourceDir = Paths.get(resource.getDirectory());
            getLog().debug("Searching for Jing-providers.json file: " + resourceDir.toAbsolutePath());
            Path targetPath = resourceDir.resolve(FILE_NAME);
            if (Files.isRegularFile(targetPath)) {
                getLog().debug("Found Jing-providers.json file: " + targetPath.toAbsolutePath());
                processJingProviderFile(targetPath);
            }
        }
        getLog().debug("Jing-providers.json file not found, skipping process Jing-providers.json");
    }

    private void processJingProviderFile(Path path) throws MojoExecutionException, MojoFailureException {
        if (!Files.isReadable(path)) {
            throw new MojoExecutionException("Jing-providers.json is not readable");
        }
        byte[] content;
        try {
            content = Files.readAllBytes(path);
        } catch (IOException e) {
            throw new MojoExecutionException("Cannot read from Jing-providers.json", e);
        }
        Map<String, Set<String>> data = parseProviderData(content);
        Path targetDirPath = Path.of(project.getBuild().getOutputDirectory(), "META-INF", "services");
        try {
            Files.createDirectories(targetDirPath);
        } catch (IOException e) {
            throw new MojoExecutionException("Cannot create META-INF/services directory", e);
        }
        for (Map.Entry<String, Set<String>> entry : data.entrySet()) {
            String key = entry.getKey();
            Set<String> lines = entry.getValue();
            getLog().debug("processing key : " + key);
            getLog().debug("processing lines : " + lines);
            Path targetPath = targetDirPath.resolve(key);
            try {
                Files.write(targetPath, lines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            } catch (IOException e) {
                throw new MojoExecutionException("Cannot write SPI file: " + targetPath.toAbsolutePath(), e);
            }
        }
        Path moduleInfoPath = Path.of(project.getBuild().getOutputDirectory(), "module-info.class");
        if (Files.isRegularFile(moduleInfoPath)) {
            getLog().debug("Found module-info class: " + moduleInfoPath.toAbsolutePath());
            byte[] bytecodes;
            try {
                bytecodes = Files.readAllBytes(moduleInfoPath);
                bytecodes = updateModuleInfoByteCodes(bytecodes, data);
                Files.write(moduleInfoPath, bytecodes, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
            } catch (IOException e) {
                throw new MojoExecutionException("Cannot perform IO operations", e);
            }
        }
    }

    private static Map<String, Set<String>> parseProviderData(byte[] content) throws MojoFailureException {
        Map<String, Set<String>> r = new HashMap<>();
        String key;
        Set<String> set = new HashSet<>();
        int index = 0, nextIndex;
        index = searchByte(content, index, b -> b == (byte) '{');
        for (; ; ) {
            index = searchByte(content, index, b -> b == (byte) '"');
            nextIndex = searchByte(content, index, b -> b == (byte) '"');
            key = new String(content, index, nextIndex - index - 1, StandardCharsets.UTF_8);
            index = searchByte(content, nextIndex, b -> b == (byte) ':');
            index = searchByte(content, index, b -> b == (byte) '[');
            do {
                index = searchByte(content, index, b -> b == (byte) '"');
                nextIndex = searchByte(content, index, b -> b == (byte) '"');
                if (!set.add(new String(content, index, nextIndex - index - 1, StandardCharsets.UTF_8))) {
                    throw new MojoFailureException("Jing-providers.json contains duplicate data");
                }
                index = searchByte(content, nextIndex, b -> b == (byte) ',' || b == (byte) ']');
            } while (content[index - 1] != (byte) ']');
            if (r.put(key, set) != null) {
                throw new MojoFailureException("Jing-providers.json contains duplicate key");
            }
            set = new HashSet<>();
            searchByte(content, index, b -> b == (byte) ',' || b == (byte) '}');
            if (content[index - 1] != (byte) '}') {
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
        throw new MojoFailureException("Invalid json structure");
    }

    private static byte[] updateModuleInfoByteCodes(byte[] bytecodes, Map<String, Set<String>> data) throws MojoFailureException {
        ClassFile cf = ClassFile.of();
        ClassModel model = cf.parse(bytecodes);
        ModuleAttribute currentAttribute = model.findAttribute(Attributes.module()).orElseThrow(() -> new MojoFailureException("Failed to get module attributes"));
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

}
