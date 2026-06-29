package io.jingproject.marshallprocessor;

import io.jingproject.common.anno.Provider;
import io.jingproject.commonprocess.AnnoUtil;
import io.jingproject.commonprocess.AnnotationProcessorException;
import io.jingproject.commonprocess.GeneratorBlock;
import io.jingproject.commonprocess.GeneratorSource;
import io.jingproject.marshall.*;
import io.jingproject.marshall.hash.HashUtil;
import io.jingproject.marshall.hash.Hasher;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class MarshallProcessor extends AbstractProcessor {
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
            for (Element e : roundEnv.getElementsAnnotatedWith(Marshallable.class)) {
                TypeElement t = AnnoUtil.castTypeElement(e);
                checkMarshallableElement(t);
                MarshallProcessorInfo info = createMarshallProcessorInfo(t);
                GeneratorSource readerSource = new GeneratorSource(t, "MarshallReader");
                GeneratorSource writerSource = new GeneratorSource(t, "MarshallWriter");
                GeneratorSource facadeSource = new GeneratorSource(t, "MarshallFacade");
                writeMarshallReaderSource(readerSource, facadeSource, info);
                writeMarshallWriterSource(writerSource, facadeSource, info);
                writeMarshallFacadeSource(facadeSource, readerSource, writerSource, info);
            }
        }
        return true;
    }

    private void checkMarshallableElement(TypeElement t) {
        // check registration
        AnnoUtil.checkTypeElementForRegister(t);
        switch (t.getKind()) {
            case CLASS -> checkMarshallableClassElement(t);
            case RECORD -> checkMarshallableRecordElement(t);
            case ENUM -> checkMarshallableEnumElement(t);
            default ->
                    throw new UnsupportedOperationException("only class, record or enum are supported by marshall processor");
        }
    }

    private void checkMarshallableClassElement(TypeElement t) {
        // must be non-abstract class
        if (t.getModifiers().contains(Modifier.ABSTRACT)) {
            throw new AnnotationProcessorException("abstract class can not be annotated with @Marshallable");
        }
        // if using inheritance, must be annotationed with @Marshallable in the same module
        Elements elements = processingEnv.getElementUtils();
        Types typeUtils = processingEnv.getTypeUtils();
        TypeMirror rootModuleType = elements.getModuleOf(t).asType();
        TypeMirror superType = t.getSuperclass();
        while (!typeUtils.isSameType(superType, objectType)) {
            Marshallable superTypeAnnotation = superType.getAnnotation(Marshallable.class);
            if (superTypeAnnotation == null) {
                throw new AnnotationProcessorException("super class must be annotated with @Marshallable");
            }
            Element superElement = typeUtils.asElement(superType);
            TypeMirror superModuleType = elements.getModuleOf(superElement).asType();
            if (!typeUtils.isSameType(rootModuleType, superModuleType)) {
                throw new AnnotationProcessorException("super class must be within the same module");
            }
            superType = AnnoUtil.castTypeElement(superElement).getSuperclass();
        }
        // must have no-arg constructor
        if (t.getEnclosedElements().stream().noneMatch(e -> e.getKind() == ElementKind.CONSTRUCTOR
                && e.getModifiers().contains(Modifier.PUBLIC)
                && AnnoUtil.castExecutableElement(e).getParameters().isEmpty())) {
            throw new AnnotationProcessorException("no-arg constructor not found");
        }
        List<? extends Element> fields = t.getEnclosedElements().stream().filter(e -> e.getKind() == ElementKind.FIELD
                && !e.getModifiers().contains(Modifier.STATIC)).toList();
        // fields cannot be empty
        if(fields.isEmpty()) {
            throw new AnnotationProcessorException("field not found");
        }
        fields.forEach(e -> {
            AnnoUtil.checkFieldElementForRegister(e);
            // fields cannot be final
            if (e.getModifiers().contains(Modifier.FINAL)) {
                throw new AnnotationProcessorException("only non-final fields could appear in normal classes");
            }
            // fields cannot have more than 2 type args
            TypeMirror tm = e.asType();
            if(tm.getKind() == TypeKind.DECLARED && AnnoUtil.castDeclaredType(tm).getTypeArguments().size() > 2) {
                throw new AnnotationProcessorException("field cannot have more than 2 type args");
            }
        });
    }

    private void checkMarshallableRecordElement(TypeElement t) {
        // must have fields
        List<? extends Element> fields = t.getEnclosedElements().stream().filter(e -> e.getKind() == ElementKind.RECORD_COMPONENT).toList();
        if (fields.isEmpty()) {
            throw new AnnotationProcessorException("record component not found");
        }
        // check variable element type
        fields.forEach(e -> {
            AnnoUtil.checkFieldElementForRegister(e);
            // fields cannot have more than 2 type args
            TypeMirror tm = e.asType();
            if(tm.getKind() == TypeKind.DECLARED && AnnoUtil.castDeclaredType(tm).getTypeArguments().size() > 2) {
                throw new AnnotationProcessorException("field cannot have more than 2 type args");
            }
        });
    }

    private void checkMarshallableEnumElement(TypeElement t) {
        // must be public enum
        if (!t.getModifiers().contains(Modifier.PUBLIC)) {
            throw new AnnotationProcessorException("only public fieldElement can be annotated with @Marshallable");
        }
        // must have enum constants
        if (t.getEnclosedElements().stream().noneMatch(e -> e.getKind() == ElementKind.ENUM_CONSTANT)) {
            throw new AnnotationProcessorException("enum constants can not be empty");
        }
    }

    private MarshallFieldInfo createMarshallFieldInfo(TypeElement t, int typeIndex, Element fieldElement, int marshallIndex, int fieldNameOffset, int mappedNameOffset) {
        Marshallable marshallable = Objects.requireNonNull(t.getAnnotation(Marshallable.class));
        String fieldName = fieldElement.getSimpleName().toString();
        String mappedName = fieldName;
        boolean skipSerializing = false;
        boolean skipDeserializing = false;
        MarshallAttr attr = fieldElement.getAnnotation(MarshallAttr.class);
        if (attr != null) {
            String attrMappedName = attr.mappedName();
            if (!attrMappedName.isBlank()) {
                mappedName = attrMappedName;
            }
        }
        NamingConvention from = marshallable.from();
        NamingConvention to = marshallable.to();
        if (from != NamingConvention.ORIGINAL && to != NamingConvention.ORIGINAL && mappedName.equals(fieldName)) {
            mappedName = NamingConvention.cast(from, to, fieldName);
        }
        int fieldNameLen = fieldName.getBytes(StandardCharsets.UTF_8).length;
        int mappedNameLen = mappedName.getBytes(StandardCharsets.UTF_8).length;
        return new MarshallFieldInfo(t, typeIndex, fieldElement, fieldName, mappedName, marshallIndex,
                fieldNameOffset, fieldNameLen,
                mappedNameOffset, mappedNameLen,
                skipSerializing, skipDeserializing);
    }

    private MarshallProcessorInfo createMarshallProcessorInfo(TypeElement t) {
        List<TypeElement> typeElements = createTypeElements(t);
        List<MarshallFieldInfo> fieldInfos = createFieldInfos(typeElements);
        Map<Class<?>, List<MarshallFieldInfo>> fieldTypeInfo = createTypeInfo(fieldInfos);
        Map<Integer, List<MarshallFieldInfo>> fieldHashInfo = createHashInfo(fieldInfos, MarshallFieldInfo::fieldName);
        Map<Integer, List<MarshallFieldInfo>> mappedHashInfo = createHashInfo(fieldInfos, MarshallFieldInfo::mappedName);
        return new MarshallProcessorInfo(typeElements, fieldInfos, fieldTypeInfo, fieldHashInfo, mappedHashInfo);
    }

    private List<TypeElement> createTypeElements(TypeElement t) {
        if(t.getKind() == ElementKind.CLASS) {
            List<TypeElement> temp = new LinkedList<>();
            Types typeUtils = processingEnv.getTypeUtils();
            TypeMirror head = t.asType();
            while (!typeUtils.isSameType(head, objectType)) {
                TypeElement te = AnnoUtil.castTypeElement(typeUtils.asElement(head));
                temp.addFirst(te);
                head = te.getSuperclass();
            }
            return List.copyOf(temp);
        } else {
            return List.of(t);
        }
    }

    private List<MarshallFieldInfo> createFieldInfos(List<TypeElement> typeElements) {
        List<MarshallFieldInfo> fieldInfos = new ArrayList<>();
        int marshallIndex = 0;
        int fieldNameIndex = 0;
        int mappedNameIndex = 0;
        ElementKind targetKind = switch (typeElements.getLast().getKind()) {
            case CLASS -> ElementKind.FIELD;
            case RECORD -> ElementKind.RECORD_COMPONENT;
            case ENUM -> ElementKind.ENUM_CONSTANT;
            default -> throw new AssertionError();
        };
        for (int typeIndex = 0; typeIndex < typeElements.size(); typeIndex++) {
            TypeElement te = typeElements.get(typeIndex);
            for (Element e : te.getEnclosedElements()) {
                if (e.getKind() == targetKind) {
                    MarshallFieldInfo fi = createMarshallFieldInfo(te, typeIndex, e, marshallIndex, fieldNameIndex, mappedNameIndex);
                    marshallIndex = Math.incrementExact(marshallIndex);
                    fieldInfos.add(fi);
                    fieldNameIndex = Math.addExact(fieldNameIndex, fi.fieldNameLen());
                    mappedNameIndex = Math.addExact(mappedNameIndex, fi.mappedNameLen());
                }
            }
        }
        return List.copyOf(fieldInfos);
    }

    private Map<Class<?>, List<MarshallFieldInfo>> createTypeInfo(List<MarshallFieldInfo> fieldInfos) {
        Map<Class<?>, List<MarshallFieldInfo>> r = new HashMap<>();
        for (MarshallFieldInfo fieldInfo : fieldInfos) {
            Class<?> targetClass = switch (fieldInfo.fieldElement().asType().getKind()) {
                case BOOLEAN -> boolean.class;
                case BYTE -> byte.class;
                case SHORT -> short.class;
                case CHAR -> char.class;
                case INT -> int.class;
                case LONG -> long.class;
                case FLOAT -> float.class;
                case DOUBLE -> double.class;
                default -> Object.class;
            };
            r.computeIfAbsent(targetClass, _ -> new ArrayList<>()).add(fieldInfo);
        }
        return Map.copyOf(r);
    }

    private Map<Integer, List<MarshallFieldInfo>> createHashInfo(List<MarshallFieldInfo> fieldInfos, Function<MarshallFieldInfo, String> extractor) {
        Map<Integer, List<MarshallFieldInfo>> r = new HashMap<>();
        Hasher hasher = HashUtil.calcHasher(fieldInfos.stream().map(extractor).toList());
        for (MarshallFieldInfo marshallFieldInfo : fieldInfos) {
            int hash = hasher.hash(extractor.apply(marshallFieldInfo));
            r.computeIfAbsent(hash, _ -> new ArrayList<>()).add(marshallFieldInfo);
        }
        return Map.copyOf(r);
    }

    private void writeMarshallReaderSource(GeneratorSource readerSource, GeneratorSource facadeSource, MarshallProcessorInfo info) {
        TypeElement targetElement = info.typeElements().getLast();
        ElementKind targetElementKind = targetElement.getKind();
        if (targetElementKind == ElementKind.ENUM) {
            // enum don't need readers
            return;
        }
        List<GeneratorBlock> blocks = new ArrayList<>();
        String readerClassName = readerSource.className();
        String targetClassName = readerSource.register(targetElement);
        String marshallReaderClassName = readerSource.register(MarshallReader.class);
        String unsupportedOperationExceptionClassName = readerSource.register(UnsupportedOperationException.class);
        String overrideClassName = readerSource.register(Override.class);
        blocks.add(new GeneratorBlock()
                .addLine("public record " + readerClassName + " (")
                .indent()
                .addLine(targetClassName + " instance")
                .unindent()
                .addLine(") implements " + marshallReaderClassName + " {")
                .newLine()
                .indent());
        for (Map.Entry<Class<?>, List<MarshallFieldInfo>> entry : info.fieldTypeInfo().entrySet()) {
            boolean marked = false;
            Class<?> cls = entry.getKey();
            List<MarshallFieldInfo> fis = entry.getValue();
            String rTypeName = (cls == Object.class) ? readerSource.register(Object.class) : cls.getSimpleName();
            String fTypeName = (cls == Object.class) ? rTypeName : rTypeName.substring(0, 1).toUpperCase() + rTypeName.substring(1);
            GeneratorBlock b = new GeneratorBlock()
                    .addLine("@" + overrideClassName)
                    .addLine("public " + rTypeName + " get" + fTypeName + "(int offset) {")
                    .indent()
                    .addLine("return switch (offset) {")
                    .indent();
            for (MarshallFieldInfo fieldInfo : fis) {
                int marshallIndex = fieldInfo.marshallIndex();
                String assignStatement = switch (targetElementKind) {
                    case CLASS -> {
                        String facadeClassName = readerSource.register(facadeSource);
                        String fieldTypeName;
                        if(cls == Object.class) {
                            fieldTypeName = readerSource.registerFieldElement(fieldInfo.fieldElement());
                            if (!marked && AnnoUtil.isGenericType(fieldTypeName)) {
                                marked = true;
                            }
                        } else {
                            fieldTypeName = rTypeName;
                        }
                        yield "(" + fieldTypeName + ") " + facadeClassName + ".vh(" + marshallIndex + ").get(instance);";
                    }
                    case RECORD -> "instance." + fieldInfo.fieldName() + "();";
                    default -> throw new AssertionError();
                };
                b.addLine("case " + marshallIndex + " -> " + assignStatement);
            }
            b.addLine("default -> throw new " + unsupportedOperationExceptionClassName + "();")
                    .unindent()
                    .addLine("};")
                    .unindent()
                    .addLine("}")
                    .newLine();
            if (marked) {
                String suppressWarningsClassName = readerSource.register(SuppressWarnings.class);
                blocks.add(new GeneratorBlock().addLine("@" + suppressWarningsClassName + "(\"unchecked\")"));
            }
            blocks.add(b);
        }
        blocks.add(new GeneratorBlock().newLine().unindent().addLine("}").newLine());
        readerSource.addBlocks(blocks);
        readerSource.writeToFiler(processingEnv);
    }

    private void writeMarshallWriterSource(GeneratorSource writerSource, GeneratorSource facadeSource, MarshallProcessorInfo info) {
        TypeElement targetElement = info.typeElements().getLast();
        ElementKind targetElementKind = targetElement.getKind();
        if (targetElementKind == ElementKind.ENUM) {
            // enum don't need writers
            return;
        }
        List<GeneratorBlock> blocks = new ArrayList<>();
        String writerClassName = writerSource.className();
        String targetClassName = writerSource.register(targetElement);
        String marshallWriterClassName = writerSource.register(MarshallWriter.class);
        String unsupportedOperationExceptionClassName = writerSource.register(UnsupportedOperationException.class);
        String overrideClassName = writerSource.register(Override.class);
        if(targetElementKind == ElementKind.RECORD) {
            GeneratorBlock b = new GeneratorBlock()
                    .addLine("public final class " + writerClassName + " implements " + marshallWriterClassName + " {")
                    .indent();
            for (MarshallFieldInfo fieldInfo : info.fieldInfos()) {
                String fieldTypeName = writerSource.registerFieldElement(fieldInfo.fieldElement());
                b.addLine("private " + fieldTypeName + " " + fieldInfo.fieldName() + ";");
            }
            blocks.add(b.newLine());
        } else if(targetElementKind == ElementKind.CLASS) {
            blocks.add(new GeneratorBlock().addLine("public record " + writerClassName + " (")
                    .indent().addLine(targetClassName + " instance")
                    .unindent().addLine(") implements " + marshallWriterClassName + " {").indent().newLine());
        } else {
            throw new AssertionError();
        }
        for (Map.Entry<Class<?>, List<MarshallFieldInfo>> entry : info.fieldTypeInfo().entrySet()) {
            boolean marked = false;
            Class<?> cls = entry.getKey();
            List<MarshallFieldInfo> fis = entry.getValue();
            String rTypeName = (cls == Object.class) ? writerSource.register(Object.class) : cls.getSimpleName();
            String fTypeName = (cls == Object.class) ? rTypeName : rTypeName.substring(0, 1).toUpperCase() + rTypeName.substring(1);
            GeneratorBlock b = new GeneratorBlock()
                    .addLine("@" + overrideClassName)
                    .addLine("public void set" + fTypeName + "(int offset, " + rTypeName + " value) {")
                    .indent()
                    .addLine("switch (offset) {")
                    .indent();
            for (MarshallFieldInfo fieldInfo : fis) {
                int marshallIndex = fieldInfo.marshallIndex();
                String castExpr = "";
                if(cls == Object.class) {
                    String fieldTypeName = writerSource.registerFieldElement(fieldInfo.fieldElement());
                    if (!marked && AnnoUtil.isGenericType(fieldTypeName)) {
                        marked = true;
                    }
                    castExpr = "(" + fieldTypeName + ") ";
                }
                String assignStatement = switch (targetElementKind) {
                    case CLASS -> {
                        String facadeClassName = writerSource.register(facadeSource);

                        yield facadeClassName + ".vh(" + marshallIndex + ").set(instance, " + castExpr + "value);";
                    }
                    case RECORD -> "this." + fieldInfo.fieldName() + " = " + castExpr + "value;";
                    default -> throw new AssertionError();
                };
                b.addLine("case " + marshallIndex + " -> " + assignStatement);
            }
            b.addLine("default -> throw new " + unsupportedOperationExceptionClassName + "();")
                    .unindent()
                    .addLine("}")
                    .unindent()
                    .addLine("}")
                    .newLine();
            if (marked) {
                String suppressWarningsClassName = writerSource.register(SuppressWarnings.class);
                blocks.add(new GeneratorBlock().addLine("@" + suppressWarningsClassName + "(\"unchecked\")"));
            }
            blocks.add(b);
        }
        if(targetElementKind == ElementKind.RECORD) {
            blocks.add(new GeneratorBlock().addLine(targetClassName + " build() {")
                    .indent().addLine("return new " + targetClassName + "(" +
                            info.fieldInfos().stream().map(MarshallFieldInfo::fieldName).collect(Collectors.joining(", ")) + ");")
                    .unindent().addLine("}").newLine());
        }
        blocks.add(new GeneratorBlock().newLine().unindent().addLine("}").newLine());
        writerSource.addBlocks(blocks);
        writerSource.writeToFiler(processingEnv);
    }

    private void writeMarshallFacadeSource(GeneratorSource facadeSource, GeneratorSource readerSource, GeneratorSource writerSource, MarshallProcessorInfo info) {
        facadeSource.addBlocks(List.of(
                buildHeadBlock(facadeSource, info),
                new GeneratorBlock().addLine("static {").indent(),
                buildVarhandleListInitializationBlock(facadeSource, info),
                buildMarshallFacadeInfoInitializationBlock(facadeSource, info),
                new GeneratorBlock().unindent().addLine("}").newLine(),
                buildPackagePrivateVhMethod(facadeSource, info),
                buildMarshallableTypeMethod(facadeSource, info),
                buildTotalElementsMethod(facadeSource, info),
                buildMarshallInfoByIndexMethod(facadeSource),
                buildMarshallInfoByStringMethod(facadeSource, info, true),
                buildMarshallInfoByBinaryMethod(facadeSource, info, true, false),
                buildMarshallInfoByBinaryMethod(facadeSource, info, true, true),
                buildMarshallInfoByStringMethod(facadeSource, info, false),
                buildMarshallInfoByBinaryMethod(facadeSource, info, false, false),
                buildMarshallInfoByBinaryMethod(facadeSource, info, false, true),
                buildNewReaderMethod(facadeSource, readerSource, info),
                buildNewWriterMethod(facadeSource, writerSource, info),
                buildConstructMethod(facadeSource, writerSource, info),
                new GeneratorBlock().unindent().addLine("}").newLine()
        ));
        facadeSource.writeToFiler(processingEnv);
    }

    private GeneratorBlock buildHeadBlock(GeneratorSource facadeSource, MarshallProcessorInfo info) {
        String providerClassName = facadeSource.register(Provider.class);
        String facadeClassName = facadeSource.className();
        String marshallFacadeClassName = facadeSource.register(MarshallFacade.class);
        String marshallFacadeInfoClassName = facadeSource.register(MarshallFacadeInfo.class);
        GeneratorBlock b = new GeneratorBlock()
                .addLine("@" + providerClassName + "(target = " + marshallFacadeClassName + ".class)")
                .addLine("public final class " + facadeClassName + " implements " + marshallFacadeClassName + " {")
                .indent();
        if (info.typeElements().getLast().getKind() == ElementKind.CLASS) {
            String listClassName = facadeSource.register(List.class);
            String varhandleClassName = facadeSource.register(VarHandle.class);
            b.addLine("private static final " + listClassName + "<" + varhandleClassName + "> VHS;");
        }
        return b.addLine("private static final " + marshallFacadeInfoClassName + " FACADE_INFO;")
                .newLine();
    }

    private GeneratorBlock buildVarhandleListInitializationBlock(GeneratorSource facadeSource, MarshallProcessorInfo info) {
        GeneratorBlock b = new GeneratorBlock();
        if (info.typeElements().getLast().getKind() == ElementKind.CLASS) {
            String methodHandlesClassName = facadeSource.register(MethodHandles.class);
            String varhandleClassName = facadeSource.register(VarHandle.class);
            String listClassName = facadeSource.register(List.class);
            String exceptionClassName = facadeSource.register(Exception.class);
            String exceptionInInitializerErrorClassName = facadeSource.register(ExceptionInInitializerError.class);
            b.addLine("try {").indent()
                    .addLine(methodHandlesClassName + ".Lookup lookup = " +
                    methodHandlesClassName + ".lookup();");
            List<TypeElement> ts = info.typeElements();
            for (int typeIndex = 0; typeIndex < ts.size(); typeIndex++) {
                TypeElement te = ts.get(typeIndex);
                String teClassName = facadeSource.register(te);
                b.addLine(methodHandlesClassName + ".Lookup lookup"
                        + typeIndex + " = " + methodHandlesClassName +
                        ".privateLookupIn(" + teClassName + ".class, lookup);");
            }
            for (MarshallFieldInfo fieldInfo : info.fieldInfos()) {
                String teClassName = facadeSource.register(fieldInfo.typeElement());
                String fieldRawClassName = facadeSource.registerRawFieldElement(fieldInfo.fieldElement());
                b.addLine(varhandleClassName + " vh" + fieldInfo.marshallIndex() +
                        " = lookup" + fieldInfo.typeIndex() + ".findVarHandle(" + teClassName +
                        ".class, \"" + fieldInfo.fieldName() + "\", " + fieldRawClassName + ".class);");
            }
            b.addLine("VHS = " + listClassName + ".of(" +
                    IntStream.range(0, info.fieldInfos().size()).mapToObj(i -> "vh" + i).collect(Collectors.joining(", ")) + ");")
                    .unindent().addLine("} catch (" + exceptionClassName + " e) {")
                    .indent().addLine("throw new " + exceptionInInitializerErrorClassName + "(e);")
                    .unindent().addLine("}");
        }
        return b;
    }

    private List<String> getGenericTypeLiterals(GeneratorSource source, TypeMirror tm) {
        List<String> r = new ArrayList<>();
        if (tm.getKind() == TypeKind.DECLARED) {
            DeclaredType d = AnnoUtil.castDeclaredType(tm);
            for (TypeMirror typeArg : d.getTypeArguments()) {
                if(typeArg.getKind() == TypeKind.DECLARED) {
                    DeclaredType dt = AnnoUtil.castDeclaredType(typeArg);
                    TypeElement te = AnnoUtil.castTypeElement(dt.asElement());
                    r.add(source.register(te));
                } else {
                    throw new AnnotationProcessorException("not a declared generic type : " + typeArg);
                }
            }
        }
        return List.copyOf(r);
    }

    private GeneratorBlock buildMarshallFacadeInfoInitializationBlock(GeneratorSource facadeSource, MarshallProcessorInfo info) {
        GeneratorBlock b = new GeneratorBlock();
        String marshallInfoClassName = facadeSource.register(MarshallInfo.class);
        String marshallFacadeInfoClassName = facadeSource.register(MarshallFacadeInfo.class);
        String listClassName = facadeSource.register(List.class);
        for (MarshallFieldInfo fieldInfo : info.fieldInfos()) {
            Element fieldElement = fieldInfo.fieldElement();
            String fieldRawClassName = facadeSource.registerRawFieldElement(fieldElement);
            List<String> genericTypeLiterals = List.of();
            String enumValue = null;
            if (fieldInfo.typeElement().getKind() == ElementKind.ENUM) {
                enumValue = facadeSource.register(fieldInfo.typeElement()) + "." + fieldInfo.fieldName();
            } else {
                genericTypeLiterals = getGenericTypeLiterals(facadeSource, fieldElement.asType());
            }
            String marshallInfoParams = String.join(", ", List.of(
                    fieldRawClassName + ".class",
                    !genericTypeLiterals.isEmpty() ? genericTypeLiterals.get(0) + ".class" : "null",
                    genericTypeLiterals.size() > 1 ? genericTypeLiterals.get(1) + ".class" : "null",
                    String.valueOf(fieldInfo.marshallIndex()),
                    "\"" + fieldInfo.fieldName() + "\"",
                    "\"" + fieldInfo.mappedName() + "\"",
                    enumValue == null ? "null" : enumValue,
                    String.valueOf(fieldInfo.skipSerializing()),
                    String.valueOf(fieldInfo.skipDeserializing())
            ));
            b.addLine(marshallInfoClassName + " mi" + fieldInfo.marshallIndex() +
                    "= new " + marshallInfoClassName + "(" + marshallInfoParams + ");");
        }
        b.addLine("FACADE_INFO = new " + marshallFacadeInfoClassName + "(" + listClassName +
                ".of(" + IntStream.range(0, info.fieldInfos().size()).mapToObj(i -> "mi" + i).collect(Collectors.joining(", ")) + "));");
        return b;
    }

    private GeneratorBlock buildPackagePrivateVhMethod(GeneratorSource facadeSource, MarshallProcessorInfo info) {
        GeneratorBlock b = new GeneratorBlock();
        if(info.typeElements().getLast().getKind() == ElementKind.CLASS) {
            String varhandleClassName = facadeSource.register(VarHandle.class);
            b.addLine("static " + varhandleClassName + " vh(int index) {")
                    .indent().addLine("return VHS.get(index);")
                    .unindent().addLine("}").newLine();
        }
        return b;
    }

    private GeneratorBlock buildMarshallableTypeMethod(GeneratorSource facadeSource, MarshallProcessorInfo info) {
        String overrideClassName = facadeSource.register(Override.class);
        String clsClassName = facadeSource.register(Class.class);
        String targetClassName = facadeSource.register(info.typeElements().getLast());
        return new GeneratorBlock().addLine("@" + overrideClassName)
                .addLine("public " + clsClassName + "<?> marshallableType() {")
                .indent().addLine("return " + targetClassName + ".class;")
                .unindent().addLine("}").newLine();
    }

    private GeneratorBlock buildTotalElementsMethod(GeneratorSource facadeSource, MarshallProcessorInfo info) {
        String overrideClassName = facadeSource.register(Override.class);
        return new GeneratorBlock().addLine("@" + overrideClassName)
                .addLine("public int totalElements() {")
                .indent()
                .addLine("return " + info.fieldInfos().size() + ";")
                .unindent()
                .addLine("}").newLine();
    }

    private GeneratorBlock buildMarshallInfoByIndexMethod(GeneratorSource facadeSource) {
        String overrideClassName = facadeSource.register(Override.class);
        String marshallInfoClassName = facadeSource.register(MarshallInfo.class);
        return new GeneratorBlock().addLine("@" + overrideClassName)
                .addLine("public " + marshallInfoClassName + " marshallInfoByIndex(int index) {")
                .indent().addLine("return FACADE_INFO.infos().get(index);")
                .unindent().addLine("}").newLine();
    }

    private GeneratorBlock buildMarshallInfoByStringMethod(GeneratorSource facadeSource, MarshallProcessorInfo info, boolean f) {
        String overrideClassName = facadeSource.register(Override.class);
        String marshallInfoClassName = facadeSource.register(MarshallInfo.class);
        String stringClassName = facadeSource.register(String.class);
        String illegalArgumentExceptionClassName = facadeSource.register(IllegalArgumentException.class);
        String lowerType = f ? "fieldName" : "mappedName";
        String upperType = f ? "FieldName" : "MappedName";
        GeneratorBlock b = new GeneratorBlock().addLine("@" + overrideClassName)
                .addLine("public " + marshallInfoClassName + " marshallInfoBy" + upperType + "(" + stringClassName + " " + lowerType + ") {")
                .indent().addLine("int index = switch (" + lowerType + ") {").indent();
        for (MarshallFieldInfo fieldInfo : info.fieldInfos()) {
            b.addLine("case \"" + (f ? fieldInfo.fieldName() : fieldInfo.mappedName()) + "\" -> " + fieldInfo.marshallIndex() + ";");
        }
        return b.addLine("default -> throw new " + illegalArgumentExceptionClassName + "(\"" + lowerType +
                        " not found: \" + " + lowerType + ");").unindent().addLine("};")
                .addLine("return FACADE_INFO.infos().get(index);").unindent().addLine("}").newLine();
    }

    private GeneratorBlock buildMarshallInfoByBinaryMethod(GeneratorSource facadeSource, MarshallProcessorInfo info, boolean f, boolean m) {
        String overrideClassName = facadeSource.register(Override.class);
        String marshallInfoClassName = facadeSource.register(MarshallInfo.class);
        String illegalArgumentExceptionClassName = facadeSource.register(IllegalArgumentException.class);
        String paramType = m ? facadeSource.register(MemorySegment.class) : "byte[]";
        String paramName = m ? "segment" : "bytes";
        String paramUnit = m ? "long" : "int";
        String lowerType = f ? "fieldName" : "mappedName";
        String upperType = f ? "FieldName" : "MappedName";
        GeneratorBlock b = new GeneratorBlock().addLine("@" + overrideClassName)
                .addLine("public " + marshallInfoClassName + " marshallInfoBy" + upperType + "(" + paramType + " " +
                        paramName + ", " + paramUnit + " offset, " + paramUnit + " len) {").indent()
                .addLine("int hash = FACADE_INFO." + lowerType + "Hasher().hash(" + paramName + ", offset, len);")
                .addLine("switch (hash) {").indent();
        Map<Integer, List<MarshallFieldInfo>> hashInfo = f ? info.fieldHashInfo() : info.mappedHashInfo();
        for (Map.Entry<Integer, List<MarshallFieldInfo>> entry : hashInfo.entrySet()) {
            Integer hash = entry.getKey();
            List<MarshallFieldInfo> fis = entry.getValue();
            b.addLine("case " + hash + " -> {").indent();
            for (MarshallFieldInfo fi : fis) {
                String offset = String.valueOf(f ? fi.fieldNameOffset() : fi.mappedNameOffset());
                String len = String.valueOf(f ? fi.mappedNameLen() : fi.fieldNameLen());
                b.addLine("if(FACADE_INFO." + lowerType + "Equals(" + String.join(", ", List.of(offset, len, paramName, "offset", "len")) + ")) {")
                        .indent()
                        .addLine("return FACADE_INFO.infos().get(" + fi.marshallIndex() + ");")
                        .unindent().addLine("}");
            }
            b.unindent().addLine("}");
        }
        return b.unindent().addLine("}")
                .addLine("throw new " + illegalArgumentExceptionClassName + "(\"marshallInfo not found by " + lowerType + "\");")
                .unindent().addLine("}").newLine();
    }

    private GeneratorBlock buildNewReaderMethod(GeneratorSource facadeSource, GeneratorSource readerSource, MarshallProcessorInfo info) {
        TypeElement targetElement = info.typeElements().getLast();
        GeneratorBlock b = new GeneratorBlock();
        if(targetElement.getKind() == ElementKind.ENUM) {
            return b;
        }
        String overrideClassName = facadeSource.register(Override.class);
        String marshallReaderClassName = facadeSource.register(MarshallReader.class);
        String objectClassName = facadeSource.register(Object.class);
        String targetClassName = facadeSource.register(targetElement);
        String readerClassName = facadeSource.register(readerSource);
        String illegalArgumentExceptionClassName = facadeSource.register(IllegalArgumentException.class);
        return b.addLine("@" + overrideClassName)
                .addLine("public " + marshallReaderClassName + " newReader(" + objectClassName + " target) {")
                .indent()
                .addLine("if(target instanceof " + targetClassName + " instance) {")
                .indent().addLine("return new " + readerClassName + "(instance);")
                .unindent().addLine("}")
                .addLine("throw new " + illegalArgumentExceptionClassName + "(\"wrong target : \" + target.getClass().getName());")
                .unindent().addLine("}").newLine();
    }

    private GeneratorBlock buildNewWriterMethod(GeneratorSource facadeSource, GeneratorSource writerSource, MarshallProcessorInfo info) {
        TypeElement targetElement = info.typeElements().getLast();
        GeneratorBlock b = new GeneratorBlock();
        if(targetElement.getKind() == ElementKind.ENUM) {
            return b;
        }
        String overrideClassName = facadeSource.register(Override.class);
        String marshallWriterClassName = facadeSource.register(MarshallWriter.class);
        String writerClassName = facadeSource.register(writerSource);
        b.addLine("@" + overrideClassName)
                .addLine("public " + marshallWriterClassName + " newWriter() {")
                .indent();
        switch (targetElement.getKind()) {
            case CLASS -> {
                String targetClassName = facadeSource.register(targetElement);
                b.addLine(targetClassName + " instance = new " + targetClassName + "();")
                        .addLine("return new " + writerClassName + "(instance);");
            }
            case RECORD -> b.addLine("return new " + writerClassName + "();");
        }
        return b.unindent().addLine("}").newLine();
    }

    private GeneratorBlock buildConstructMethod(GeneratorSource facadeSource, GeneratorSource writerSource, MarshallProcessorInfo info) {
        TypeElement targetElement = info.typeElements().getLast();
        GeneratorBlock b = new GeneratorBlock();
        if(targetElement.getKind() == ElementKind.ENUM) {
            return b;
        }
        String overrideClassName = facadeSource.register(Override.class);
        String objectClassName = facadeSource.register(Object.class);
        String marshallWriterClassName = facadeSource.register(MarshallWriter.class);
        String writerClassName = facadeSource.register(writerSource);
        String illegalArgumentExceptionClassName = facadeSource.register(IllegalArgumentException.class);
        b.addLine("@" + overrideClassName)
                .addLine("public " + objectClassName + " construct(" + marshallWriterClassName + " writer) {")
                .indent();
        switch (targetElement.getKind()) {
            case CLASS -> b.addLine("if (writer instanceof " + writerClassName + "(" + facadeSource.register(targetElement) + " instance)) {")
                    .indent().addLine("return instance;")
                    .unindent().addLine("}");
            case RECORD -> b.addLine("if(writer instanceof " + writerClassName + " instance) {")
                    .indent().addLine("return instance.build();")
                    .unindent().addLine("}");
            default -> throw new AssertionError();
        }
        return b.addLine("throw new " + illegalArgumentExceptionClassName + "(\"wrong writer : \" + writer.getClass().getName());")
                .unindent().addLine("}").newLine();
    }
}
