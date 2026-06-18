package io.jingproject.commonprocess;

import io.jingproject.common.Utils;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Writer;
import java.util.*;

public final class GeneratorSource {
    private static final String INDENT = "    ";
    private static final String SUPER = "super";
    private static final String EXTENDS = "extends";
    private static final String JAVA_LANG = Object.class.getPackageName();
    private final TypeElement targetElement;
    private final String packageName;
    private final String simpleName;
    private final Set<String> imports = new LinkedHashSet<>(64);
    private final Map<String, String> packageReferences = new HashMap<>(64);
    private final List<GeneratorLine> lines = new ArrayList<>(256);
    private int indent = 0;

    public GeneratorSource(TypeElement t, String tag) {
        AnnoUtil.checkTypeElementForRegister(t);
        if (tag == null || tag.isBlank()) {
            throw new AnnotationProcessorException("empty tag");
        }
        String fullName = t.toString();
        targetElement = t;
        packageName = AnnoUtil.packageName(fullName);
        simpleName = Utils.generateClassName(AnnoUtil.simpleName(fullName), tag);
        packageReferences.put(simpleName, packageName); // register itself first
    }

    public String packageName() {
        return packageName;
    }

    public String className() {
        return simpleName;
    }

    public String registerRaw(VariableElement variableElement) {
        AnnoUtil.checkVariableElementForRegister(variableElement);
        TypeMirror tm = variableElement.asType();
        String qualifiedName = tm.toString();
        if (tm.getKind().isPrimitive()) {
            return qualifiedName;
        }
        int firstGenericIndex = qualifiedName.indexOf('<');
        String rawName = firstGenericIndex == -1 ? qualifiedName : qualifiedName.substring(0, firstGenericIndex);
        String packageName = AnnoUtil.packageName(rawName);
        String simpleName = AnnoUtil.simpleName(rawName);
        return register(packageName, simpleName);
    }

    public String register(VariableElement variableElement) {
        AnnoUtil.checkVariableElementForRegister(variableElement);
        TypeMirror tm = variableElement.asType();
        String qualifiedName = tm.toString();
        if (tm.getKind().isPrimitive()) {
            return qualifiedName;
        }
        StringTokenizer stringTokenizer = buildTokenizer(qualifiedName);
        while (stringTokenizer.hasMoreTokens()) {
            String s = stringTokenizer.nextToken();
            if (s.equals(EXTENDS) || s.equals(SUPER)) {
                // supporting 'super' and 'extends' currently brings no benefits to the program but significantly increases complexity, therefore they are not considered
                throw new AnnotationProcessorException("super and extends are not supported for registration");
            }
            String packageName = AnnoUtil.packageName(s);
            String simpleName = AnnoUtil.simpleName(s);
            String registeredName = register(packageName, simpleName);
            if (!registeredName.equals(s)) {
                qualifiedName = qualifiedName.replace(s, registeredName);
            }
        }
        return qualifiedName;
    }

    private static StringTokenizer buildTokenizer(String qualifiedName) {
        StringBuilder sb = new StringBuilder();
        for (char c : qualifiedName.toCharArray()) {
            // & is not possible if enclosing typeElement is not generified
            if (c == '<' || c == '>' || c == '[' || c == ']' || c == '?' || c == ',') {
                sb.append(' ');
            } else if (c == '&') {
                throw new AssertionError();
            } else {
                sb.append(c);
            }
        }
        return new StringTokenizer(sb.toString(), " ");
    }

    public String register(Class<?> cls) {
        AnnoUtil.checkClassForRegister(cls);
        String packageName = cls.getPackageName();
        String simpleName = cls.getSimpleName();
        return register(packageName, simpleName);
    }

    public String register(TypeElement typeElement) {
        AnnoUtil.checkTypeElementForRegister(typeElement);
        String fullName = typeElement.toString();
        String packageName = AnnoUtil.packageName(fullName);
        String simpleName = AnnoUtil.simpleName(fullName);
        return register(packageName, simpleName);
    }

    public String register(GeneratorSource generatorSource) {
        return register(generatorSource.packageName(), generatorSource.className());
    }

    private String register(String packageName, String simpleName) {
        if (packageName == null || packageName.isEmpty()) {
            throw new AnnotationProcessorException("packageName cannot be empty");
        }
        if (simpleName == null || simpleName.isEmpty()) {
            throw new AnnotationProcessorException("simpleName cannot be empty");
        }
        String currentPackage = packageReferences.get(simpleName);
        if (currentPackage == null) {
            // not referenced yet, we could do import
            if (!packageName.equals(this.packageName) && !packageName.equals(JAVA_LANG)) {
                imports.add(AnnoUtil.buildClassName(packageName, simpleName));
            }
            packageReferences.put(simpleName, packageName);
            return simpleName;
        } else if (currentPackage.equals(packageName)) {
            // already imported, directly return
            return simpleName;
        } else {
            // imported but with name conflict, use fullname instead
            return AnnoUtil.buildClassName(packageName, simpleName);
        }
    }

    public void addBlock(GeneratorBlock b) {
        if (b == null) {
            throw new AnnotationProcessorException("block cannot be null");
        }
        if (b.isEmpty()) {
            return;
        }
        for (GeneratorLine l : b.lines()) {
            int newIndent = Math.addExact(l.indent(), indent);
            if (newIndent < 0) {
                throw new AnnotationProcessorException("invalid indent for : " + l.content());
            }
            lines.add(new GeneratorLine(l.content(), newIndent));
        }
        indent = Math.addExact(indent, b.currentIndent());
    }

    public void addBlocks(List<GeneratorBlock> blocks) {
        if (blocks == null) {
            throw new AnnotationProcessorException("blocks cannot be null");
        }
        for (GeneratorBlock b : blocks) {
            addBlock(b);
        }
    }

    public void writeToFiler(ProcessingEnvironment env) {
        ModuleElement moduleElement = env.getElementUtils().getModuleOf(targetElement);
        if (moduleElement == null) {
            throw new AnnotationProcessorException("moduleElement not found");
        }
        String name;
        if (moduleElement.isUnnamed()) {
            name = AnnoUtil.buildClassName(packageName, simpleName);
        } else {
            String moduleName = moduleElement.getQualifiedName().toString();
            name = AnnoUtil.buildClassName(moduleName, packageName, simpleName);
        }
        try {
            JavaFileObject fo = env.getFiler().createSourceFile(name);
            try (Writer writer = fo.openWriter()) {
                writer.write("package " + packageName + ";\n\n");
                List<String> sortedImports = imports.stream().sorted().toList();
                for (String im : sortedImports) {
                    writer.write("import " + im + ";\n");
                }
                writer.write("\n");
                for (GeneratorLine line : lines) {
                    writer.write(INDENT.repeat(line.indent()));
                    writer.write(line.content());
                    writer.write("\n");
                }
                writer.flush();
            }
        } catch (IOException e) {
            throw new AnnotationProcessorException("failed to write source code to filer : " + name, e);
        }
    }
}
