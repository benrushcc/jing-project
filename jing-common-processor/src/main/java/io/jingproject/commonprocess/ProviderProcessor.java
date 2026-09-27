package io.jingproject.commonprocess;

import io.jingproject.common.anno.Provider;
import jdk.incubator.json.*;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.Writer;
import java.util.*;

public final class ProviderProcessor extends AbstractProcessor {
    // must be strictly consistent with jing-maven-plugin module
    private static final String FILE_NAME = "jing-providers.json";
    private static final String INDENT = "  ";
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

    private void writeJsonConfigurationFile() {
        try {
            FileObject fo = processingEnv.getFiler()
                    .createResource(StandardLocation.CLASS_OUTPUT, "", FILE_NAME);
            try (Writer writer = fo.openWriter()) {
                Map<String, JsonValue> object = new LinkedHashMap<>();
                for (Map.Entry<String, Set<String>> entry : data.entrySet()) {
                    ArrayList<JsonValue> values = new ArrayList<>(entry.getValue().size());
                    for (String value : entry.getValue()) {
                        values.add(JsonString.of(value));
                    }
                    object.put(entry.getKey(), JsonArray.of(values));
                }
                JsonObject json = JsonObject.of(object);
                writer.write(Json.toDisplayString(json, INDENT));
                writer.write('\n');
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
            AnnoUtil.checkTypeElementForRegister(t);
            if (!t.getModifiers().contains(Modifier.FINAL)) {
                throw new AnnotationProcessorException("only final class can be annotated with @Provider");
            }
            String targetImplName = t.getQualifiedName().toString();
            String targetInterfaceName;
            try {
                Provider provider = t.getAnnotation(Provider.class);
                if(provider == null) {
                    throw new AnnotationProcessorException("@Provider annotation not found");
                }
                targetInterfaceName = provider.target().getCanonicalName();
            } catch (MirroredTypeException mte) {
                TypeMirror mirror = mte.getTypeMirror();
                if (mirror == null) {
                    throw new AnnotationProcessorException("failed to get spi type mirror");
                }
                TypeElement targetInterfaceTypeElement = AnnoUtil.castTypeElement(processingEnv.getTypeUtils().asElement(mirror));
                if (targetInterfaceTypeElement.getKind() != ElementKind.INTERFACE) {
                    throw new AnnotationProcessorException("only interface element can be assigned to @Provider annotation");
                }
                targetInterfaceName = targetInterfaceTypeElement.getQualifiedName().toString();
            }
            if (!data.computeIfAbsent(targetInterfaceName, _ -> new HashSet<>()).add(targetImplName)) {
                throw new AnnotationProcessorException("target interface " + targetInterfaceName + " already exists");
            }
        }
    }
}
