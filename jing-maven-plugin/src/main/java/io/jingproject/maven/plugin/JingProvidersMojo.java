package io.jingproject.maven.plugin;

import jdk.incubator.json.Json;
import jdk.incubator.json.JsonValue;
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
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.*;

@Mojo(name = "process-jing-providers", defaultPhase = LifecyclePhase.PROCESS_CLASSES, threadSafe = true)
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

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Path outputDir = outputDir();
        getLog().info("searching for " + FILE_NAME + " file at directory: " + outputDir.toAbsolutePath());
        Path targetPath = outputDir.resolve(FILE_NAME);
        if (Files.isRegularFile(targetPath)) {
            getLog().info("found " + FILE_NAME + " file : " + targetPath.toAbsolutePath());
            processJingProviderFile(targetPath);
            consumeJingProviderFile(targetPath);
        } else {
            throw new MojoFailureException(FILE_NAME + " file not found, maybe jing-common-processor is not effective");
        }
    }

    private static byte[] updateModuleInfoByteCodes(byte[] bytecodes, Map<String, Set<String>> providerMap) throws MojoFailureException {
        try {
            ClassFile cf = ClassFile.of();
            ClassModel model = cf.parse(bytecodes);
            ModuleAttribute currentAttribute = model.findAttribute(Attributes.module()).orElseThrow(() -> new MojoFailureException("failed to get module attributes"));
            List<ModuleProvideInfo> currentProvides = currentAttribute.provides();
            for (ModuleProvideInfo m : currentProvides) {
                if (providerMap.containsKey(m.provides().asSymbol().displayName())) {
                    throw new MojoFailureException("module-info already contains generated SPI directives");
                }
            }
            List<ModuleProvideInfo> newProvides = new ArrayList<>(currentProvides);
            for (Map.Entry<String, Set<String>> entry : providerMap.entrySet()) {
                String key = entry.getKey();
                Set<String> set = entry.getValue();
                newProvides.add(ModuleProvideInfo.of(ClassDesc.of(key), set.stream().map(ClassDesc::of).toList()));
            }
            ModuleAttribute newAttribute = ModuleAttribute.of(currentAttribute.moduleName(), currentAttribute.moduleFlagsMask(), currentAttribute.moduleVersion().orElse(null),
                    currentAttribute.requires(), currentAttribute.exports(), currentAttribute.opens(), currentAttribute.uses(), newProvides);
            return cf.buildModule(newAttribute);
        } catch (RuntimeException e) {
            throw new MojoFailureException("failed to update module-info", e);
        }
    }

    private Path outputDir() throws MojoFailureException {
        return switch (scope) {
            case "main" -> Path.of(project.getBuild().getOutputDirectory());
            case "test" -> Path.of(project.getBuild().getTestOutputDirectory());
            default -> throw new MojoFailureException("invalid scope : " + scope);
        };
    }

    private Map<String, Set<String>> buildProviderMap(Path path) throws MojoExecutionException, MojoFailureException {
        byte[] content;
        try {
            content = Files.readAllBytes(path);
        } catch (IOException e) {
            throw new MojoExecutionException("Cannot read from " + FILE_NAME + " file", e);
        }
        String json;
        try {
            json = StandardCharsets.UTF_8
                    .newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(content))
                    .toString();
        } catch (CharacterCodingException e) {
            throw new MojoFailureException(FILE_NAME + " contains invalid utf8 sequence");
        }
        try {
            Map<String, JsonValue> jsonValueMap = Json.parse(json).asMap();
            Map<String, Set<String>> providerMap = new LinkedHashMap<>(jsonValueMap.size());
            for (Map.Entry<String, JsonValue> entry : jsonValueMap.entrySet()) {
                String key = entry.getKey();
                List<JsonValue> list = entry.getValue().asList();
                if(list.isEmpty()) {
                    throw new MojoFailureException("no impl present for provider : " + key);
                }
                Set<String> providers = new LinkedHashSet<>(list.size());
                for (JsonValue element : list) {
                    String impl = element.asString();
                    if (!providers.add(impl)) {
                        throw new MojoFailureException("duplicate impl : " + impl);
                    }
                }
                providerMap.put(key, providers);
            }
            return providerMap;
        } catch (RuntimeException e) {
            throw new MojoFailureException(FILE_NAME + " contains invalid json structure", e);
        }
    }

    private void processJingProviderFile(Path path) throws MojoExecutionException, MojoFailureException {
        Map<String, Set<String>> providerMap = buildProviderMap(path);
        if (providerMap.isEmpty()) {
            return;
        }
        Path targetDirPath = outputDir().resolve(META_INF).resolve(SERVICES);
        try {
            Files.createDirectories(targetDirPath);
        } catch (IOException e) {
            throw new MojoExecutionException("cannot create " + META_INF + "/" + SERVICES + " directory", e);
        }
        for (Map.Entry<String, Set<String>> entry : providerMap.entrySet()) {
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
        Path moduleInfoPath = outputDir().resolve(MODULE_INFO_CLASS);
        if (Files.isRegularFile(moduleInfoPath)) {
            getLog().info("found module-info : " + moduleInfoPath.toAbsolutePath());
            byte[] bytecodes;
            try {
                bytecodes = Files.readAllBytes(moduleInfoPath);
                bytecodes = updateModuleInfoByteCodes(bytecodes, providerMap);
                Files.write(moduleInfoPath, bytecodes, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
            } catch (IOException e) {
                throw new MojoExecutionException("cannot perform IO operations when modifying module-info", e);
            }
        }
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
