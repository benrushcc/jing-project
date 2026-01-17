package io.jingproject.annprocess;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.*;
import javax.lang.model.util.Elements;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Writer;
import java.util.*;

public final class GeneratorSource {

    private final ProcessingEnvironment env;
    private final String sourceModuleName;
    private final String sourcePackageName;
    private final String sourceClassName;
    private final Set<String> imports = new LinkedHashSet<>();
    private final Map<String, String> references = new HashMap<>();
    private final List<GeneratorLine> lines = new ArrayList<>();
    private int indent = 0;

    public GeneratorSource(ProcessingEnvironment processingEnv, TypeElement el, String tag) {
        env = Objects.requireNonNull(processingEnv);
        Elements elm = env.getElementUtils();
        ModuleElement moduleElement = elm.getModuleOf(el);
        PackageElement packageElement = elm.getPackageOf(el);
        if (moduleElement.isUnnamed() || elm.isAutomaticModule(moduleElement)) {
            throw new RuntimeException("ModuleElement cannot be unnamed or automatic");
        }
        sourceModuleName = moduleElement.getQualifiedName().toString();
        sourcePackageName = packageElement.getQualifiedName().toString();
        sourceClassName = "_" + el.getSimpleName() + "$$" + tag;
    }

    public String className() {
        return sourceClassName;
    }

    public String register(VariableElement variableElement) {
        return register(variableElement.asType());
    }

    public String register(TypeMirror typeMirror) {
        if (typeMirror.getKind() == TypeKind.DECLARED && typeMirror instanceof DeclaredType declaredType && declaredType.asElement() instanceof TypeElement typeElement) {
            return register(typeElement);
        } else if (typeMirror.getKind() == TypeKind.TYPEVAR && typeMirror instanceof TypeVariable typeVariable && typeVariable.asElement() instanceof TypeParameterElement typeParameterElement) {
            return register(typeParameterElement.getBounds().getFirst());
        } else if (typeMirror.getKind() == TypeKind.ARRAY && typeMirror instanceof ArrayType arrayType) {
            return register(arrayType.getComponentType()) + "[]";
        } else if (typeMirror.getKind().isPrimitive()) {
            return typeMirror.toString();
        } else {
            throw new RuntimeException("Unsupported type " + typeMirror);
        }
    }

    public String register(TypeElement typeElement) {
        if (typeElement.getNestingKind() != NestingKind.TOP_LEVEL) {
            throw new RuntimeException("Registered element must be top-level : " + typeElement.getSimpleName());
        }
        String packageName = env.getElementUtils().getPackageOf(typeElement).getQualifiedName().toString();
        String fullName = typeElement.getQualifiedName().toString();
        String simpleName = typeElement.getSimpleName().toString();
        return register(packageName, fullName, simpleName);
    }

    public String register(Class<?> clazz) {
        if (clazz.isMemberClass()) {
            throw new RuntimeException("Registered class must be top-level : " + clazz.getSimpleName());
        }
        String packageName = clazz.getPackageName();
        String fullName = clazz.getName();
        String simpleName = clazz.getSimpleName();
        return register(packageName, fullName, simpleName);
    }

    private String register(String packageName, String fullName, String simpleName) {
        String current = references.get(simpleName);
        if (current == null) {
            if (!packageName.equals(sourcePackageName) && !packageName.equals("java.lang")) {
                imports.add(fullName);
            }
            references.put(simpleName, fullName);
            return simpleName;
        } else if (fullName.equals(current)) {
            return simpleName;
        } else {
            return fullName;
        }
    }

    public void addBlock(GeneratorBlock b) {
        for (GeneratorLine l : b.lines()) {
            lines.add(new GeneratorLine(l.content(), Math.addExact(l.indent(), indent)));
        }
        indent = Math.addExact(indent, b.currentIndent());
    }

    public void addBlocks(List<GeneratorBlock> blocks) {
        for (GeneratorBlock b : blocks) {
            addBlock(b);
        }
    }

    public void writeToFiler() {
        try {
            JavaFileObject fo = env.getFiler().createSourceFile(sourceModuleName + "/" + sourcePackageName + "." + sourceClassName);
            try (Writer writer = fo.openWriter()) {
                writer.write("package " + sourcePackageName + ";\n\n");
                for (String im : imports) {
                    writer.write("import " + im + ";\n");
                }
                writer.write("\n");
                for (GeneratorLine line : lines) {
                    writer.write("\t".repeat(line.indent()));
                    writer.write(line.content());
                    writer.write("\n");
                }
                writer.flush();
            }
        } catch (IOException e) {
            throw new AnnotationProcessorException("Failed to write to filer", e);
        }
    }
}
