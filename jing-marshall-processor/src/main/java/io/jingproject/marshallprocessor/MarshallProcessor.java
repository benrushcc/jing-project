package io.jingproject.marshallprocessor;

import io.jingproject.commonprocess.AnnotationProcessorException;
import io.jingproject.commonprocess.GeneratorBlock;
import io.jingproject.commonprocess.GeneratorSource;
import io.jingproject.marshall.*;
import io.jingproject.marshall.hash.Hasher;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class MarshallProcessor extends AbstractProcessor {
    private static final List<Class<?>> PRIMITIVE_TYPES = List.of(boolean.class, byte.class, char.class, short.class, int.class, long.class, float.class, double.class);
    private TypeMirror objectType;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        objectType = processingEnv.getElementUtils().getTypeElement(Object.class.getCanonicalName()).asType();
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of(Marshallable.class.getCanonicalName());
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (!roundEnv.processingOver()) {
            for (Element element : roundEnv.getElementsAnnotatedWith(Marshallable.class)) {
                if (element instanceof TypeElement t) {
                    checkMarshallableElement(t);
                    MarshallGenInfo genInfo = createMarshallGenInfo(t);
                    GeneratorSource schemaSource = new GeneratorSource(processingEnv, t, "MarshallSchema");
                    GeneratorSource facadeSource = new GeneratorSource(processingEnv, t, "MarshallFacade");
                    writeMarshallSchemaSource(schemaSource, facadeSource, genInfo);
                    writeMarshallFacadeSource(schemaSource, facadeSource, genInfo);
                } else {
                    throw new AssertionError();
                }
            }
        }
        return true;
    }

    private void checkMarshallableElement(TypeElement t) {
        switch (t.getKind()) {
            case CLASS -> checkMarshallableClassElement(t);
            case RECORD -> checkMarshallableRecordElement(t);
            case ENUM -> checkMarshallableEnumElement(t);
            default -> throw new UnsupportedOperationException("only class, record or enum are supported by marshall processor");
        }
    }

    private void checkMarshallableClassElement(TypeElement t) {
        // must be top-level class
        if(t.getNestingKind() != NestingKind.TOP_LEVEL) {
            throw new AnnotationProcessorException("only top level fieldElement can be annotated with @Marshallable");
        }
        // must be public class
        if (!t.getModifiers().contains(Modifier.PUBLIC)) {
            throw new AnnotationProcessorException("only public fieldElement can be annotated with @Marshallable");
        }
        // must be non-abstract class
        if(t.getModifiers().contains(Modifier.ABSTRACT)) {
            throw new AnnotationProcessorException("abstract class can not be annotated with @Marshallable");
        }
        // if using inheritance, must be annotationed with @Marshallable in the same module
        ModuleElement rootModule = processingEnv.getElementUtils().getModuleOf(t);
        TypeMirror superType = t.getSuperclass();
        while (!processingEnv.getTypeUtils().isSameType(superType, objectType)) {
            Marshallable superTypeAnnotation = superType.getAnnotation(Marshallable.class);
            if(superTypeAnnotation == null) {
                throw new AnnotationProcessorException("super type must be annotated with @Marshallable");
            }
            Element superElement = processingEnv.getTypeUtils().asElement(superType);
            ModuleElement superModule = processingEnv.getElementUtils().getModuleOf(superElement);
            if(!rootModule.equals(superModule)) {
                throw new AnnotationProcessorException("super type must be within the same module");
            }
            if(superElement instanceof TypeElement st) {
                superType = st.getSuperclass();
            } else {
                throw new AssertionError();
            }
        }
        // must have fields
        List<? extends Element> enclosedElements = t.getEnclosedElements();
        if(enclosedElements.isEmpty()) {
            throw new AnnotationProcessorException("enclosed elements can not be empty");
        }
        // must have no-arg constructor
        for (Element enclosedElement : enclosedElements) {
            if(enclosedElement.getKind().equals(ElementKind.CONSTRUCTOR)
                    && enclosedElement instanceof ExecutableElement executableElement
                    && executableElement.getParameters().isEmpty()
                    && executableElement.getModifiers().contains(Modifier.PUBLIC)) {
                return ;
            }
        }
        throw new AnnotationProcessorException("no-arg constructor not found");
    }

    private void checkMarshallableRecordElement(TypeElement t) {
        // must be top-level record
        if(t.getNestingKind() != NestingKind.TOP_LEVEL) {
            throw new AnnotationProcessorException("only top level fieldElement can be annotated with @Marshallable");
        }
        // must be public record
        if(!t.getModifiers().contains(Modifier.PUBLIC)) {
            throw new AnnotationProcessorException("only public fieldElement can be annotated with @Marshallable");
        }
        // must have fields
        List<? extends Element> enclosedElements = t.getEnclosedElements();
        if(enclosedElements.isEmpty()) {
            throw new AnnotationProcessorException("enclosed elements can not be empty");
        }
    }

    private void checkMarshallableEnumElement(TypeElement t) {
        // must be top-level enum
        if(t.getNestingKind() != NestingKind.TOP_LEVEL) {
            throw new AnnotationProcessorException("only top level fieldElement can be annotated with @Marshallable");
        }
        // must be public enum
        if(!t.getModifiers().contains(Modifier.PUBLIC)) {
            throw new AnnotationProcessorException("only public fieldElement can be annotated with @Marshallable");
        }
        // must have enum constants
        if(t.getEnclosedElements().stream().noneMatch(e -> e.getKind().equals(ElementKind.ENUM_CONSTANT))) {
            throw new AnnotationProcessorException("enum constants can not be empty");
        }
    }

    private MarshallFieldInfo createMarshallFieldInfo(TypeElement t, int typeIndex, VariableElement v, int marshallIndex, int fieldNameIndex, int mappedNameIndex) {
        Marshallable marshallable = Objects.requireNonNull(t.getAnnotation(Marshallable.class));
        String fieldName = v.getSimpleName().toString();
        String mappedName = fieldName;
        boolean skipSerializing = false;
        boolean skipDeserializing = false;
        MarshallAttr attr = v.getAnnotation(MarshallAttr.class);
        if(attr != null) {
            String attrMappedName = attr.mappedName();
            if(!attrMappedName.isBlank()) {
                mappedName = attrMappedName;
            }
        }
        NamingConvention from = marshallable.from();
        NamingConvention to = marshallable.to();
        if(from != NamingConvention.ORIGINAL && to != NamingConvention.ORIGINAL && mappedName.equals(fieldName)) {
            mappedName = NamingConvention.cast(from, to, fieldName);
        }
        int fieldNameLength = fieldName.getBytes(StandardCharsets.UTF_8).length;
        int mappedNameLength = mappedName.getBytes(StandardCharsets.UTF_8).length;
        return new MarshallFieldInfo(t, typeIndex, v, fieldName, mappedName, marshallIndex,
                fieldNameIndex, Math.addExact(fieldNameIndex, fieldNameLength),
                mappedNameIndex, Math.addExact(mappedNameIndex, mappedNameLength),
                skipSerializing, skipDeserializing);
    }

    private MarshallGenInfo createMarshallGenInfo(TypeElement t) {
        switch (t.getKind()) {
            case CLASS -> {
                if(processingEnv.getTypeUtils().isSameType(t.getSuperclass(), objectType)) {
                    return createMarshallNormalGenInfo(t, ElementKind.FIELD);
                } else {
                    return createMarshallExtendedClassGenInfo(t);
                }
            }
            case RECORD -> {
                return createMarshallNormalGenInfo(t, ElementKind.RECORD);
            }
            case ENUM -> {
                return createMarshallNormalGenInfo(t, ElementKind.ENUM_CONSTANT);
            }
            default -> throw new AssertionError();
        }
    }

    private MarshallGenInfo createMarshallNormalGenInfo(TypeElement t, ElementKind kind) {
        List<MarshallFieldInfo> fis = new ArrayList<>();
        Map<Integer, List<MarshallFieldInfo>> fh = new HashMap<>();
        Map<Integer, List<MarshallFieldInfo>> mh = new HashMap<>();
        int marshallIndex = 0;
        int fieldNameIndex = 0;
        int mappedNameIndex = 0;
        for (Element e : t.getEnclosedElements()) {
            if(e.getKind().equals(kind)) {
                if(e instanceof VariableElement v) {
                    MarshallFieldInfo fi = createMarshallFieldInfo(t, 0, v, marshallIndex, fieldNameIndex, mappedNameIndex);
                    marshallIndex = Math.incrementExact(marshallIndex);
                    fis.add(fi);
                    fieldNameIndex = fi.fieldNameEndIndex();
                    mappedNameIndex = fi.mappedNameEndIndex();
                } else {
                    throw new AssertionError();
                }
            }
        }
        Hasher fieldNameHasher = MarshallUtil.calcHasher(fis.stream().map(MarshallFieldInfo::fieldName).toList());
        Hasher mappedNameHasher = MarshallUtil.calcHasher(fis.stream().map(MarshallFieldInfo::mappedName).toList());
        for (MarshallFieldInfo fi : fis) {
            int fieldHash = fieldNameHasher.hash(fi.fieldName());
            fh.computeIfAbsent(fieldHash, _ -> new ArrayList<>()).add(fi);
            int mappedHash = mappedNameHasher.hash(fi.mappedName());
            mh.computeIfAbsent(mappedHash, _ -> new ArrayList<>()).add(fi);
        }
        return new MarshallGenInfo(List.of(t), List.copyOf(fis), Map.copyOf(fh), Map.copyOf(mh));
    }

    private MarshallGenInfo createMarshallExtendedClassGenInfo(TypeElement t) {
        LinkedList<TypeElement> ts = new LinkedList<>();
        List<MarshallFieldInfo> fis = new ArrayList<>();
        Map<Integer, List<MarshallFieldInfo>> fh = new HashMap<>();
        Map<Integer, List<MarshallFieldInfo>> mh = new HashMap<>();
        TypeMirror head = t.asType();
        while (!processingEnv.getTypeUtils().isSameType(head, objectType)) {
            Element headElement = processingEnv.getTypeUtils().asElement(head);
            if(headElement instanceof TypeElement te) {
                ts.addFirst(te);
                head = te.getSuperclass();
            } else {
                throw new AssertionError();
            }
        }
        int marshallIndex = 0;
        int fieldNameIndex = 0;
        int mappedNameIndex = 0;
        for (int typeIndex = 0; typeIndex < ts.size(); typeIndex++) {
            TypeElement te = ts.get(typeIndex);
            for (Element e : te.getEnclosedElements()) {
                if(e.getKind().equals(ElementKind.FIELD)) {
                    if(e instanceof VariableElement v) {
                        MarshallFieldInfo fi = createMarshallFieldInfo(te, typeIndex, v, marshallIndex, fieldNameIndex, mappedNameIndex);
                        marshallIndex = Math.incrementExact(marshallIndex);
                        fis.add(fi);
                        fieldNameIndex = fi.fieldNameEndIndex();
                        mappedNameIndex = fi.mappedNameEndIndex();
                    } else {
                        throw new AssertionError();
                    }
                }
            }
        }
        Hasher fieldNameHasher = MarshallUtil.calcHasher(fis.stream().map(MarshallFieldInfo::fieldName).toList());
        Hasher mappedNameHasher = MarshallUtil.calcHasher(fis.stream().map(MarshallFieldInfo::mappedName).toList());
        for (MarshallFieldInfo fi : fis) {
            int fieldHash = fieldNameHasher.hash(fi.fieldName());
            fh.computeIfAbsent(fieldHash, _ -> new ArrayList<>()).add(fi);
            int mappedHash = mappedNameHasher.hash(fi.mappedName());
            mh.computeIfAbsent(mappedHash, _ -> new ArrayList<>()).add(fi);
        }
        return new MarshallGenInfo(List.of(t), List.copyOf(fis), Map.copyOf(fh), Map.copyOf(mh));
    }

    private void writeMarshallSchemaSource(GeneratorSource schemaSource, GeneratorSource facadeSource, MarshallGenInfo genInfo) {
        List<GeneratorBlock> blocks = new ArrayList<>();
        Map<Class<?>, List<MarshallFieldInfo>> primitiveSetters = new HashMap<>();
        List<MarshallFieldInfo> objectSetter = new ArrayList<>();
        switch (genInfo.typeElements().getLast().getKind()) {
            case CLASS -> {
                String schemaClassName = schemaSource.className();
                String marshallSchemaClassName = schemaSource.register(MarshallSchema.class);
                String facadeClassName = schemaSource.register(facadeSource);
                String targetClassName = schemaSource.register(genInfo.typeElements().getLast());
                String uoe = schemaSource.register(UnsupportedOperationException.class);
                blocks.add(new GeneratorBlock().addLine("public record " + schemaClassName + " (")
                        .indent().addLine(facadeClassName + " facade,")
                        .addLine(targetClassName + " instance").unindent()
                        .addLine(") implements " + marshallSchemaClassName + " {").newLine());
                for (MarshallFieldInfo fieldInfo : genInfo.fieldInfos()) {
                    switch (fieldInfo.fieldElement().asType().getKind()) {
                        case BOOLEAN -> primitiveSetters.computeIfAbsent(boolean.class, _ -> new ArrayList<>()).add(fieldInfo);
                        case BYTE -> primitiveSetters.computeIfAbsent(byte.class, _ -> new ArrayList<>()).add(fieldInfo);
                        case SHORT -> primitiveSetters.computeIfAbsent(short.class, _ -> new ArrayList<>()).add(fieldInfo);
                        case CHAR -> primitiveSetters.computeIfAbsent(char.class, _ -> new ArrayList<>()).add(fieldInfo);
                        case INT -> primitiveSetters.computeIfAbsent(int.class, _ -> new ArrayList<>()).add(fieldInfo);
                        case LONG -> primitiveSetters.computeIfAbsent(long.class, _ -> new ArrayList<>()).add(fieldInfo);
                        case FLOAT -> primitiveSetters.computeIfAbsent(float.class, _ -> new ArrayList<>()).add(fieldInfo);
                        case DOUBLE -> primitiveSetters.computeIfAbsent(double.class, _ -> new ArrayList<>()).add(fieldInfo);
                        default -> objectSetter.add(fieldInfo);
                    }
                }
                for (Map.Entry<Class<?>, List<MarshallFieldInfo>> entry : primitiveSetters.entrySet()) {
                    String primitiveType = entry.getKey().getSimpleName();
                    String primitiveTypeString = primitiveType.substring(0, 1).toUpperCase() + primitiveType.substring(1);
                    GeneratorBlock b = new GeneratorBlock().addLine("@Override")
                            .addLine("public void set" + primitiveTypeString + "(int offset, " + primitiveType + "value) {")
                            .indent().addLine("switch (offset) {").indent();
                    for (MarshallFieldInfo fieldInfo : entry.getValue()) {
                        int marshallIndex = fieldInfo.marshallIndex();
                        b.addLine("case " + marshallIndex + " -> facade.marshallInfoByIndex(" + marshallIndex + ").vh().set(instance, value);");
                    }
                    b.addLine("default -> throw new " + uoe + "();")
                            .unindent().addLine("}")
                            .unindent().addLine("}").newLine();
                    blocks.add(b);
                }
                if(!objectSetter.isEmpty()) {
                    GeneratorBlock b = new GeneratorBlock().addLine("@Override").addLine("public void setObject(int offset, Object value) {")
                            .indent().addLine("switch (offset) {").indent();
                    for (MarshallFieldInfo fieldInfo : objectSetter) {
                        int marshallIndex = fieldInfo.marshallIndex();
                        String fieldType = schemaSource.register(fieldInfo.fieldElement());
                        b.addLine("case " + marshallIndex + " -> facade.marshallInfoByIndex(" + marshallIndex + ").vh().set(instance, (" + fieldType + ") value);");
                    }
                    b.addLine("default -> throw new " + uoe + "();")
                            .unindent().addLine("}")
                            .unindent().addLine("}").newLine();
                    blocks.add(b);
                }
                blocks.add(new GeneratorBlock().newLine().unindent().addLine("}").newLine());
            }
            case RECORD -> {
                String schemaClassName = schemaSource.className();
                String marshallSchemaClassName = schemaSource.register(MarshallSchema.class);
                String uoe = schemaSource.register(UnsupportedOperationException.class);
                GeneratorBlock b = new GeneratorBlock().addLine("public final class " + schemaClassName + "implements " + marshallSchemaClassName + " {").indent();
                for (MarshallFieldInfo fieldInfo : genInfo.fieldInfos()) {
                    String fieldType = schemaSource.register(fieldInfo.fieldElement());
                    b.addLine("private " + fieldType + " " + fieldInfo.fieldName() + ";");
                    switch (fieldInfo.fieldElement().asType().getKind()) {
                        case BOOLEAN -> primitiveSetters.computeIfAbsent(boolean.class, _ -> new ArrayList<>()).add(fieldInfo);
                        case BYTE -> primitiveSetters.computeIfAbsent(byte.class, _ -> new ArrayList<>()).add(fieldInfo);
                        case SHORT -> primitiveSetters.computeIfAbsent(short.class, _ -> new ArrayList<>()).add(fieldInfo);
                        case CHAR -> primitiveSetters.computeIfAbsent(char.class, _ -> new ArrayList<>()).add(fieldInfo);
                        case INT -> primitiveSetters.computeIfAbsent(int.class, _ -> new ArrayList<>()).add(fieldInfo);
                        case LONG -> primitiveSetters.computeIfAbsent(long.class, _ -> new ArrayList<>()).add(fieldInfo);
                        case FLOAT -> primitiveSetters.computeIfAbsent(float.class, _ -> new ArrayList<>()).add(fieldInfo);
                        case DOUBLE -> primitiveSetters.computeIfAbsent(double.class, _ -> new ArrayList<>()).add(fieldInfo);
                        default -> objectSetter.add(fieldInfo);
                    }
                }
                b.newLine();
                blocks.add(b);
                for (Map.Entry<Class<?>, List<MarshallFieldInfo>> entry : primitiveSetters.entrySet()) {
                    String primitiveType = entry.getKey().getSimpleName();
                    String primitiveTypeString = primitiveType.substring(0, 1).toUpperCase() + primitiveType.substring(1);
                    GeneratorBlock primitiveGetBlock = new GeneratorBlock().addLine("@Override")
                            .addLine("public " + primitiveType + "get" + primitiveTypeString + "(int offset) {")
                            .indent().addLine("switch (offset) {").indent();
                    GeneratorBlock primitiveSetBlock = new GeneratorBlock().addLine("@Override")
                            .addLine("public void set" + primitiveTypeString + "(int offset, " + primitiveType + "value) {")
                            .indent().addLine("switch (offset) {").indent();
                    for (MarshallFieldInfo fieldInfo : entry.getValue()) {
                        int marshallIndex = fieldInfo.marshallIndex();
                        primitiveGetBlock.addLine("case " + marshallIndex + " -> {").indent()
                                .addLine("return " + fieldInfo.fieldName() + ";").unindent().addLine("}");
                        primitiveSetBlock.addLine("case " + marshallIndex + " -> this." + fieldInfo.fieldName() + " = value;");
                    }
                    primitiveGetBlock.addLine("default -> throw new " + uoe + "();").unindent().addLine("}")
                            .unindent().addLine("}").newLine();
                    primitiveSetBlock.addLine("default -> throw new " + uoe + "();").unindent().addLine("}")
                            .unindent().addLine("}").newLine();
                    blocks.add(primitiveGetBlock);
                    blocks.add(primitiveSetBlock);
                }
                if(!objectSetter.isEmpty()) {
                    GeneratorBlock objectGetBlock = new GeneratorBlock().addLine("@Override")
                            .addLine("public Object getObject(int offset) {").indent()
                            .addLine("switch (offset) {").indent();
                    GeneratorBlock objectSetBlock = new GeneratorBlock().addLine("@Override")
                            .addLine("public Object setObject(int offset, Object value) {").indent()
                            .addLine("switch (offset) {").indent();
                    for (MarshallFieldInfo fieldInfo : objectSetter) {
                        int marshallIndex = fieldInfo.marshallIndex();
                        String fieldType = schemaSource.register(fieldInfo.fieldElement());
                        objectGetBlock.addLine("case " + marshallIndex + " -> {")
                                .indent().addLine("return " + fieldInfo.fieldName() + ";").unindent().addLine("}");
                        objectSetBlock.addLine("case " + marshallIndex + " -> this." + fieldInfo.fieldName() + " = (" + fieldType + ") value;");
                    }
                    objectGetBlock.addLine("default -> throw new " + uoe + "();").unindent().addLine("}")
                            .unindent().addLine("}").newLine();
                    objectSetBlock.addLine("default -> throw new " + uoe + "();").unindent().addLine("}")
                            .unindent().addLine("}").newLine();
                    blocks.add(objectGetBlock);
                    blocks.add(objectSetBlock);
                }
                blocks.add(new GeneratorBlock().newLine().unindent().addLine("}").newLine());
            }
            case ENUM -> { /* skipped */}
            default -> throw new AssertionError();
        }
        schemaSource.addBlocks(blocks);
        schemaSource.writeToFiler();
    }

    private void writeMarshallFacadeSource(GeneratorSource schemaSource, GeneratorSource facadeSource, MarshallGenInfo genInfo) {
        List<GeneratorBlock> blocks = new ArrayList<>();
        List<TypeElement> ts = genInfo.typeElements();
        switch (ts.getLast().getKind()) {
            case CLASS -> {
                String facadeClassName = facadeSource.className();
                String marshallFacadeClassName = facadeSource.register(MarshallFacade.class);
                String methodHandlesClassName = facadeSource.register(MethodHandles.class);
                String varhandleClassName = facadeSource.register(VarHandle.class);
                String marshallInfoClassName = facadeSource.register(MarshallInfo.class);
                GeneratorBlock staticBlock = new GeneratorBlock().addLine("public final class " + facadeClassName + " implements " + marshallFacadeClassName + " {")
                        .indent().addLine("private static final MethodHandle CONSTRUCTOR_MH;")
                        .addLine("private static final List<MarshallInfo> MARSHALLS;")
                        .addLine("private static final Hasher MARSHALL_FIELDNAME_HASHER;")
                        .addLine("private static final byte[] MARSHALL_FIELDNAME_BYTES;")
                        .addLine("private static final MemorySegment MARSHALL_FIELDNAME_SEGMENT;")
                        .addLine("private static final Hasher MARSHALL_MAPPEDNAME_HASHER;")
                        .addLine("private static final byte[] MARSHALL_MAPPEDNAME_BYTES;")
                        .addLine("private static final MemorySegment MARSHALL_MAPPEDNAME_SEGMENT;")
                        .addLine("static {").indent()
                        .addLine("try {").indent()
                        .addLine(methodHandlesClassName + ".Lookup lookup = " + methodHandlesClassName + ".lookup();");
                for (int typeIndex = 0; typeIndex < ts.size(); typeIndex++) {
                    TypeElement te = ts.get(typeIndex);
                    String teClassName = facadeSource.register(te);
                    staticBlock.addLine(methodHandlesClassName + ".Lookup lookup"
                            + typeIndex + " = " + methodHandlesClassName +
                            "privateLookupIn(" + teClassName + ".class, lookup);");
                }
                for (MarshallFieldInfo fieldInfo : genInfo.fieldInfos()) {
                    String teClassName = facadeSource.register(fieldInfo.typeElement());
                    String fieldClassName = facadeSource.register(fieldInfo.fieldElement());
                    staticBlock.addLine(varhandleClassName + " vh" + fieldInfo.marshallIndex() +
                    " = lookup" + fieldInfo.typeIndex() + ".findVarHandle(" + teClassName +
                    ".class, \"" + fieldInfo.fieldName() + "\", " + fieldClassName + ".class);");
                }
                for (MarshallFieldInfo fieldInfo : genInfo.fieldInfos()) {
                    String fieldClassName = facadeSource.register(fieldInfo.fieldElement());
                    staticBlock.addLine(marshallInfoClassName + " mi" + fieldInfo.marshallIndex() +
                    "= new " + marshallInfoClassName + "(" + fieldClassName + ".class, " + fieldInfo.marshallIndex() +
                    ", \"" + fieldInfo.fieldName() + "\", \"" + fieldInfo.mappedName() + "\", vh" + fieldInfo.marshallIndex() +
                    ", null, " + fieldInfo.skipSerializing() + ", " + fieldInfo.skipDeserializing() + ");");
                }
            }
            case RECORD -> {

            }
            case ENUM -> {

            }
        }
    }

    private GeneratorBlock buildStaticHeadBlock(GeneratorSource facadeSource, MarshallGenInfo genInfo) {
        String facadeClassName = facadeSource.className();
        String marshallFacadeClassName = facadeSource.register(MarshallFacade.class);
        GeneratorBlock b = new GeneratorBlock()
                .addLine("public final class " + facadeClassName + " implements " + marshallFacadeClassName + " {")
                .indent();
        if(genInfo.typeElements().getLast().getKind() != ElementKind.ENUM) {
            b.addLine("private static final MethodHandle CONSTRUCTOR_MH;");
        }
        b.addLine("private static final List<MarshallInfo> MARSHALLS;")
                .addLine("private static final List<MarshallInfo> MARSHALLS;")
                .addLine("private static final Hasher MARSHALL_FIELDNAME_HASHER;")
                .addLine("private static final byte[] MARSHALL_FIELDNAME_BYTES;")
                .addLine("private static final MemorySegment MARSHALL_FIELDNAME_SEGMENT;")
                .addLine("private static final Hasher MARSHALL_MAPPEDNAME_HASHER;")
                .addLine("private static final byte[] MARSHALL_MAPPEDNAME_BYTES;")
                .addLine("private static final MemorySegment MARSHALL_MAPPEDNAME_SEGMENT;")
                .addLine("static {").indent();
        return b;
    }

    private GeneratorBlock buildLookupInitializationBlock(GeneratorSource facadeSource, MarshallGenInfo genInfo) {
        GeneratorBlock b = new GeneratorBlock();
        if (genInfo.typeElements().getLast().getKind() == ElementKind.ENUM) {
            // enum don't need lookup
            return b;
        }
        b.addLine("try {").indent();
        String methodHandlesClassName = facadeSource.register(MethodHandles.class);
        List<TypeElement> ts = genInfo.typeElements();
        for (int typeIndex = 0; typeIndex < ts.size(); typeIndex++) {
            TypeElement te = ts.get(typeIndex);
            String teClassName = facadeSource.register(te);
            b.addLine(methodHandlesClassName + ".Lookup lookup"
                    + typeIndex + " = " + methodHandlesClassName +
                    "privateLookupIn(" + teClassName + ".class, lookup);");
        }
        return b;
    }

    private GeneratorBlock buildVarhandleInitializationBlock(GeneratorSource facadeSource, MarshallGenInfo genInfo) {
        GeneratorBlock b = new GeneratorBlock();
        if(genInfo.typeElements().getLast().getKind() == ElementKind.ENUM) {
            // enum don't need varhandle
            return b;
        }
        String varhandleClassName = facadeSource.register(VarHandle.class);
        for (MarshallFieldInfo fieldInfo : genInfo.fieldInfos()) {
            String teClassName = facadeSource.register(fieldInfo.typeElement());
            String fieldClassName = facadeSource.register(fieldInfo.fieldElement());
            b.addLine(varhandleClassName + " vh" + fieldInfo.marshallIndex() +
                    " = lookup" + fieldInfo.typeIndex() + ".findVarHandle(" + teClassName +
                    ".class, \"" + fieldInfo.fieldName() + "\", " + fieldClassName + ".class);");
        }
        return b;
    }

    private GeneratorBlock buildMarshallInfoInitializationBlock(GeneratorSource facadeSource, MarshallGenInfo genInfo) {
        GeneratorBlock b = new GeneratorBlock();
        String marshallInfoClassName = facadeSource.register(MarshallInfo.class);

        for (MarshallFieldInfo fieldInfo : genInfo.fieldInfos()) {
            String fieldClassName = facadeSource.register(fieldInfo.fieldElement());
            String enumValue = "null";
            if(fieldInfo.typeElement().getKind() == ElementKind.ENUM) {
                enumValue = facadeSource.register(fieldInfo.typeElement()) + "." + fieldInfo.fieldName();
            }
            b.addLine(marshallInfoClassName + " mi" + fieldInfo.marshallIndex() +
                    "= new " + marshallInfoClassName + "(" + fieldClassName + ".class, " + fieldInfo.marshallIndex() +
                    ", \"" + fieldInfo.fieldName() + "\", \"" + fieldInfo.mappedName() + "\", vh" + fieldInfo.marshallIndex() +
                    ", " + enumValue + ", " + fieldInfo.skipSerializing() + ", " + fieldInfo.skipDeserializing() + ");");
        }
        return b;
    }

    private GeneratorBlock buildStaticTailBlock(GeneratorSource facadeSource, MarshallGenInfo genInfo) {
        GeneratorBlock b = new GeneratorBlock();
        TypeElement targetElement = genInfo.typeElements().getLast();
        String targetClassName = facadeSource.register(targetElement);
        String listClassName = facadeSource.register(List.class);
        String methodTypeClassName = facadeSource.register(MethodType.class);
        String marshallInfoClassName = facadeSource.register(MarshallInfo.class);
        String stringClassName = facadeSource.register(String.class);
        String marshallUtilClassName = facadeSource.register(MarshallUtil.class);
        String memorySegmentClassName = facadeSource.register(MemorySegment.class);
        boolean isEnum = targetElement.getKind() == ElementKind.ENUM;
        boolean isRecord = targetElement.getKind() == ElementKind.RECORD;
        if(!isEnum) {
            // build CONSTRUCTOR_MH
            List<String> params = new ArrayList<>();
            params.add("void.class");
            if(isRecord) {
                for (MarshallFieldInfo fieldInfo : genInfo.fieldInfos()) {
                    String fieldClassName = facadeSource.register(fieldInfo.fieldElement());
                    params.add(fieldClassName + ".class");
                }
            }
            b.addLine("CONSTRUCTOR_MH = lookup" + (genInfo.typeElements().size() - 1) +
                    ".findConstructor(" + targetClassName + ".class, " + methodTypeClassName +
                    ".methodType(" + params.stream().collect(Collectors.joining(", ")) + "));");
        }
        // build MARSHALLS
        b.addLine("MARSHALLS = " + listClassName + "of(" + IntStream.range(0, genInfo.fieldInfos().size()).mapToObj(i -> "mi" + i).collect(Collectors.joining(", ")) + ");");
        // build field name related structure
        b.addLine(listClassName + "<" + stringClassName + "> fieldNames = MARSHALLS.stream().map(" + marshallInfoClassName + "::fieldName).toList();")
                .addLine("MARSHALL_FIELDNAME_HASHER = " + marshallUtilClassName + ".calcHasher(fieldNames);")
                .addLine("MARSHALL_FIELDNAME_BYTES = " + marshallUtilClassName + ".calcBytes(fieldNames);")
                .addLine("MARSHALL_FIELDNAME_SEGMENT = " + memorySegmentClassName + ".ofArray(MARSHALL_FIELDNAME_BYTES).asReadOnly();");
        // build mapped name related structure
        b.addLine(listClassName + "<" + stringClassName + "> mappedNames = MARSHALLS.stream().map(" + marshallInfoClassName + "::mappedName).toList();")
                .addLine("MARSHALL_MAPPEDNAME_HASHER = " + marshallUtilClassName + ".calcHasher(mappedNames);")
                .addLine("MARSHALL_MAPPEDNAME_BYTES = " + marshallUtilClassName + ".calcBytes(mappedNames);")
                .addLine("MARSHALL_MAPPEDNAME_SEGMENT = " + memorySegmentClassName + ".ofArray(MARSHALL_MAPPEDNAME_BYTES).asReadOnly();");
        if(isEnum) {
            b.unindent().addLine("}").newLine();
        } else {
            String exceptionClassName = facadeSource.register(Exception.class);
            String exceptionInInitializerErrorClassName = facadeSource.register(ExceptionInInitializerError.class);
            b.unindent().addLine("} catch (" + exceptionClassName + " e) {")
                    .indent().addLine("throw new " + exceptionInInitializerErrorClassName + "(e);")
                    .unindent().addLine("}").unindent().addLine("}").newLine();
        }
        return b;
    }

    private GeneratorBlock buildMarshallableTypeMethod(GeneratorSource facadeSource, MarshallGenInfo genInfo) {
        String clsClassName = facadeSource.register(Class.class);
        String targetClassName = facadeSource.register(genInfo.typeElements().getLast());
        return new GeneratorBlock().addLine("@Override")
                .addLine("public " + clsClassName + "<?> marshallableType() {")
                .indent().addLine("return " + targetClassName + ".class;")
                .unindent().addLine("}").newLine();
    }

    private GeneratorBlock buildConstructorMethod(GeneratorSource facadeSource, MarshallGenInfo genInfo) {
        String methodHandleClassName = facadeSource.register(MethodHandle.class);
        GeneratorBlock b = new GeneratorBlock().addLine("@Override")
                .addLine("public " + methodHandleClassName + " constructor() {").indent();
        if(genInfo.typeElements().getLast().getKind() == ElementKind.ENUM) {
            String uoeClassName = facadeSource.register(UnsupportedOperationException.class);
            b.addLine("throw new " + uoeClassName + "();");
        } else {
            b.addLine("return CONSTRUCTOR_MH;");
        }
        return b.unindent().addLine("}").newLine();
    }

    private GeneratorBlock buildConstructMethod(GeneratorSource facadeSource, MarshallGenInfo genInfo) {
        String objectClassName = facadeSource.register(Object.class);
        String marshallSchemaClassName = facadeSource.register(MarshallSchema.class);
        GeneratorBlock b = new GeneratorBlock().addLine("@Override")
                .addLine("public " + objectClassName + " construct(" + marshallSchemaClassName + " schema) {")
                .indent();
        TypeElement targetElement = genInfo.typeElements().getLast();
        switch (targetElement.getKind()) {
            case CLASS -> {
                String facadeClassName = facadeSource.className();
                String targetClassName = facadeSource.register(targetElement);
                String iaeClassName = facadeSource.register(IllegalArgumentException.class);
                b.addLine("if(schema instanceof " + facadeClassName + "(_, " + targetClassName + " instance)) {")
                        .indent().addLine("return instance;").unindent().addLine("}")
                        .addLine("throw new " + iaeClassName + "(\"wrong schema type\");")
                        .unindent().addLine("}").newLine();
            }
            case RECORD -> {
                String targetClassName = facadeSource.register(targetElement);
                for (MarshallFieldInfo fieldInfo : genInfo.fieldInfos()) {
                    VariableElement fieldElement = fieldInfo.fieldElement();
                    int marshallIndex = fieldInfo.marshallIndex();
                    switch (fieldElement.asType().getKind()) {
                        case BOOLEAN -> b.addLine("boolean v" + marshallIndex + " = schema.getBoolean(" + marshallIndex + ");");
                        case BYTE -> b.addLine("byte v" + marshallIndex + " = schema.getByte(" + marshallIndex + ");");
                        case SHORT -> b.addLine("short v" + marshallIndex + " = schema.getShort(" + marshallIndex + ");");
                        case CHAR -> b.addLine("char v" + marshallIndex + " = schema.getChar(" + marshallIndex + ");");
                        case INT -> b.addLine("int v" + marshallIndex + " = schema.getInt(" + marshallIndex + ");");
                        case LONG -> b.addLine("long v" + marshallIndex + " = schema.getLong(" + marshallIndex + ");");
                        case FLOAT -> b.addLine("float v" + marshallIndex + " = schema.getFloat(" + marshallIndex + ");");
                        case DOUBLE -> b.addLine("double v" + marshallIndex + " = schema.getDouble(" + marshallIndex + ");");
                        default -> {
                            String fieldTypeClassName = facadeSource.register(fieldElement);
                            b.addLine(fieldTypeClassName + " v" + marshallIndex + " = (" + fieldTypeClassName + ") schema.getObject(" + marshallIndex + ");");
                        }
                    }
                }
                b.addLine("return new " + targetClassName + "(" + IntStream.range(0, genInfo.fieldInfos().size()).mapToObj(i -> "v" + i).collect(Collectors.joining(", ")) + ");")
                        .unindent().addLine("}").newLine();
            }
            case ENUM -> {
                String uoeClassName = facadeSource.register(UnsupportedOperationException.class);
                b.addLine("throw new " + uoeClassName + "();")
                        .unindent().addLine("}").newLine();
            }
            default -> throw new AssertionError();
        }
        return b;
    }
}
