package io.jingproject.commonprocess;

import io.jingproject.common.anno.Provider;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.Writer;
import java.util.*;

public final class ProviderProcessor extends AbstractProcessor {
    private static final String INDENT = "    ";
    private final Map<String, Set<String>> data = new HashMap<>();

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of(Provider.class.getCanonicalName());
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            writeJsonConfigurationFile();
        } else {
            processSpiData(roundEnv);
        }
        return true;
    }

    // TODO after json API got stablized into JDK, could replace it here to reduce the complexity
    private void writeJsonConfigurationFile() {
        try {
            FileObject fo = Objects.requireNonNull(processingEnv).getFiler().createResource(StandardLocation.SOURCE_OUTPUT, "", "jing-providers.json");
            try (Writer writer = fo.openWriter()) {
                writer.write("{\n");
                for (Iterator<Map.Entry<String, Set<String>>> it = data.entrySet().iterator(); it.hasNext(); ) {
                    Map.Entry<String, Set<String>> entry = it.next();
                    String key = entry.getKey();
                    Set<String> value = entry.getValue();
                    writer.write(INDENT + "\"");
                    writer.write(key);
                    writer.write("\": [\n");
                    Iterator<String> valueIterator = value.iterator();
                    while (valueIterator.hasNext()) {
                        String val = valueIterator.next();
                        writer.write(INDENT.repeat(2) + "\"");
                        writer.write(val);
                        writer.write("\"");
                        if (valueIterator.hasNext()) {
                            writer.write(",");
                        }
                        writer.write("\n");
                    }
                    writer.write(INDENT + "]");
                    if (it.hasNext()) {
                        writer.write(",");
                    }
                    writer.write("\n");
                }
                writer.write("}\n");
                writer.flush();
            } catch (IOException e) {
                throw new AnnotationProcessorException("failed to open writer", e);
            }
        } catch (IOException e) {
            throw new AnnotationProcessorException("failed to write json configuration file", e);
        }
    }

    private void processSpiData(RoundEnvironment roundEnv) {
        Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(Provider.class);
        for (Element element : elements) {
            TypeElement t = AnnoUtil.castTypeElement(element);
            if (t.getNestingKind() != NestingKind.TOP_LEVEL) {
                throw new AnnotationProcessorException("only top level element can be annotated with @Provider");
            }
            if (!t.getModifiers().contains(Modifier.FINAL)) {
                throw new AnnotationProcessorException("only final element can be annotated with @Provider");
            }
            String targetImplName = t.getQualifiedName().toString();
            String targetInterfaceName;
            try {
                targetInterfaceName = Objects.requireNonNull(t.getAnnotation(Provider.class)).target().getCanonicalName();
            } catch (MirroredTypeException mte) {
                TypeMirror mirror = mte.getTypeMirror();
                if(mirror == null) {
                    throw new AnnotationProcessorException("failed to get spi type mirror");
                }
                TypeElement targetInterfaceTypeElement = AnnoUtil.castTypeElement(processingEnv.getTypeUtils().asElement(mirror));
                if(targetInterfaceTypeElement.getKind() != ElementKind.INTERFACE) {
                    throw new AnnotationProcessorException("only interface element can be assigned to @Provider annotation");
                }
                targetInterfaceName = targetInterfaceTypeElement.getQualifiedName().toString();
            }
            data.computeIfAbsent(targetInterfaceName, _ -> new HashSet<>()).add(targetImplName);
        }
    }
}
