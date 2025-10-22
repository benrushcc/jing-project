package io.jingproject.annprocess;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.Writer;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public final class ProviderProcessor extends AbstractProcessor {

    private final Map<String, Set<String>> data = new HashMap<>();

    private final Lock lock = new ReentrantLock();

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
        if(roundEnv.processingOver()) {
            writeJsonConfigurationFile();
        } else {
            processSpiData(roundEnv);
        }
        return true;
    }

    // TODO After json API got stabled in JDK, replace it here
    private void writeJsonConfigurationFile() {
        lock.lock();
        try {
            FileObject fo = Objects.requireNonNull(processingEnv).getFiler().createResource(StandardLocation.SOURCE_OUTPUT, "", "jing-providers.json");
            try(Writer writer = fo.openWriter()) {
                writer.write("{\n");
                for (Iterator<Map.Entry<String, Set<String>>> it = data.entrySet().iterator(); it.hasNext();) {
                    Map.Entry<String, Set<String>> entry = it.next();
                    String key = entry.getKey();
                    Set<String> value = entry.getValue();
                    writer.write("\t\"");
                    writer.write(key);
                    writer.write("\": [\n");
                    Iterator<String> valueIterator = value.iterator();
                    while (valueIterator.hasNext()) {
                        String val = valueIterator.next();
                        writer.write("\t\t\"");
                        writer.write(val);
                        writer.write("\"");
                        if (valueIterator.hasNext()) {
                            writer.write(",");
                        }
                        writer.write("\n");
                    }
                    writer.write("\t]");
                    if (it.hasNext()) {
                        writer.write(",");
                    }
                    writer.write("\n");
                }
                writer.write("}\n");
            } catch (IOException e) {
                throw new AnnotationProcessorException("Failed to open writer", e);
            }
        } catch (IOException e) {
            throw new AnnotationProcessorException("Failed to write json configuration file", e);
        } finally {
            lock.unlock();
        }
    }

    private void processSpiData(RoundEnvironment roundEnv) {
        Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(Provider.class);
        for (Element element : elements) {
            if(element instanceof TypeElement t) {
                if(t.getNestingKind() != NestingKind.TOP_LEVEL) {
                    throw new AnnotationProcessorException("Only top level element can be annotated with @Provider");
                }
                if(!t.getModifiers().contains(Modifier.FINAL)) {
                    throw new AnnotationProcessorException("Only final element can be annotated with @Provider");
                }
                String targetInterfaceName;
                try {
                    targetInterfaceName = t.getAnnotation(Provider.class).target().getCanonicalName();
                } catch (MirroredTypeException mte) {
                    TypeMirror mirror = mte.getTypeMirror();
                    if(mirror instanceof DeclaredType declaredType && declaredType.asElement() instanceof TypeElement typeElement) {
                        targetInterfaceName = typeElement.getQualifiedName().toString();
                    } else {
                        throw new AnnotationProcessorException("Should never be reached");
                    }
                }
                String targetProviderName = t.getQualifiedName().toString();
                lock.lock();
                try {
                    data.computeIfAbsent(targetInterfaceName, _ -> new HashSet<>()).add(targetProviderName);
                } finally {
                    lock.unlock();
                }
            } else {
                throw new AnnotationProcessorException("Should never be reached");
            }
        }
    }
}
