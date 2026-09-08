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
    // guard against jvm's current 65535 fields limitation
    private static final int MAX_CLASS_FIELDS = 65535;
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
                GeneratorSource facadeSource = new GeneratorSource(t, "MarshallFacade");
                facadeSource.addBlock(headBlock(facadeSource, info));
                facadeSource.addBlock(new GeneratorBlock().addLine("static {").indent());
                facadeSource.addBlock(vhInitializationBlock(facadeSource, info));
                facadeSource.addBlock(facadeInfoInitializationBlock(facadeSource, info));
                facadeSource.addBlock(new GeneratorBlock().unindent().addLine("}").newLine());
                facadeSource.addBlock(marshallableTypeMethod(facadeSource, info));
                facadeSource.addBlock(marshallInfosMethod(facadeSource));
                facadeSource.addBlock(primitiveElementsMethod(facadeSource, info));
                facadeSource.addBlock(marshallInfoByStringMethod(facadeSource, info, true));
                facadeSource.addBlock(marshallInfoByBinaryMethod(facadeSource, info, true, false));
                facadeSource.addBlock(marshallInfoByBinaryMethod(facadeSource, info, true, true));
                facadeSource.addBlock(marshallInfoByStringMethod(facadeSource, info, false));
                facadeSource.addBlock(marshallInfoByBinaryMethod(facadeSource, info, false, false));
                facadeSource.addBlock(marshallInfoByBinaryMethod(facadeSource, info, false, true));
                facadeSource.addBlocks(readerBlocks(facadeSource, info));
                facadeSource.addBlock(newBuilderMethod(facadeSource,info));
                facadeSource.addBlock(constructMethod(facadeSource, info));
                facadeSource.addBlocks(builderBlocks(facadeSource, info));
                facadeSource.addBlock(new GeneratorBlock().unindent().addLine("}").newLine());
                facadeSource.writeToFiler(processingEnv);
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
            default -> throw new UnsupportedOperationException("only class, record or enum are supported by marshall processor");
        }
    }

    private void checkMarshallableClassElement(TypeElement t) {
        // must be non-abstract class
        if (t.getModifiers().contains(Modifier.ABSTRACT)) {
            throw new AnnotationProcessorException("abstract class can not be annotated with @Marshallable");
        }
        // if using inheritance, must be annotated with @Marshallable in the same module
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
        if (fields.isEmpty()) {
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
            if (tm.getKind() == TypeKind.DECLARED && AnnoUtil.castDeclaredType(tm).getTypeArguments().size() > 2) {
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
            if (tm.getKind() == TypeKind.DECLARED && AnnoUtil.castDeclaredType(tm).getTypeArguments().size() > 2) {
                throw new AnnotationProcessorException("field cannot have more than 2 type args");
            }
        });
    }

    private void checkMarshallableEnumElement(TypeElement t) {
        // must be public enum
        if (!t.getModifiers().contains(Modifier.PUBLIC)) {
            throw new AnnotationProcessorException("only public enum can be annotated with @Marshallable");
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
            if (t.getKind() == ElementKind.ENUM && (attr.skipSerializing() || attr.skipDeserializing())) {
                throw new AnnotationProcessorException("enum constant doesn't support skip serializing or deserializing");
            }
            skipSerializing = attr.skipSerializing();
            skipDeserializing = attr.skipDeserializing();
        }
        NamingConvention from = marshallable.from();
        NamingConvention to = marshallable.to();
        if (from != NamingConvention.ORIGINAL && to != NamingConvention.ORIGINAL && mappedName.equals(fieldName)) {
            mappedName = NamingConvention.cast(from, to, fieldName);
        }
        return new MarshallFieldInfo(t, typeIndex, fieldElement, fieldName, mappedName, marshallIndex,
                fieldNameOffset, fieldName.getBytes(StandardCharsets.UTF_8),
                mappedNameOffset, mappedName.getBytes(StandardCharsets.UTF_8),
                skipSerializing, skipDeserializing);
    }

    private MarshallProcessorInfo createMarshallProcessorInfo(TypeElement t) {
        List<TypeElement> typeElements = createTypeElements(t);
        List<MarshallFieldInfo> fieldInfos = createFieldInfos(typeElements);
        List<MarshallTypeInfo> typeInfos = createTypeInfos(fieldInfos);
        int fieldHashIndex = HashUtil.selectUtf8Hasher(fieldInfos, MarshallFieldInfo::fieldNameUtf8Bytes);
        List<MarshallSwitchInfo> fieldHashInfos = createHashInfos(fieldInfos, MarshallFieldInfo::fieldNameUtf8Bytes, fieldHashIndex);
        int mappedHashIndex = HashUtil.selectUtf8Hasher(fieldInfos, MarshallFieldInfo::mappedNameUtf8Bytes);
        List<MarshallSwitchInfo> mappedHashInfos = createHashInfos(fieldInfos, MarshallFieldInfo::mappedNameUtf8Bytes, mappedHashIndex);
        return new MarshallProcessorInfo(typeElements, fieldInfos, typeInfos, fieldHashIndex, fieldHashInfos, mappedHashIndex, mappedHashInfos);
    }

    private List<TypeElement> createTypeElements(TypeElement t) {
        if (t.getKind() == ElementKind.CLASS) {
            List<TypeElement> r = new ArrayList<>();
            Types typeUtils = processingEnv.getTypeUtils();
            TypeMirror head = t.asType();
            while (!typeUtils.isSameType(head, objectType)) {
                TypeElement te = AnnoUtil.castTypeElement(typeUtils.asElement(head));
                r.add(te);
                head = te.getSuperclass();
            }
            return List.copyOf(r.reversed());
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
                    fieldNameIndex = Math.addExact(fieldNameIndex, fi.fieldNameUtf8Bytes().length);
                    mappedNameIndex = Math.addExact(mappedNameIndex, fi.mappedNameUtf8Bytes().length);
                }
            }
        }
        if (fieldInfos.size() > MAX_CLASS_FIELDS) {
            throw new AnnotationProcessorException("too many fields : " + fieldInfos.size());
        }
        return List.copyOf(fieldInfos);
    }

    private List<MarshallTypeInfo> createTypeInfos(List<MarshallFieldInfo> fieldInfos) {
        List<MarshallTypeInfo> r = new ArrayList<>();
        outer : for (MarshallFieldInfo fieldInfo : fieldInfos) {
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
            for (MarshallTypeInfo marshallTypeInfo : r) {
                if(marshallTypeInfo.type() == targetClass) {
                    marshallTypeInfo.fieldInfos().add(fieldInfo);
                    continue outer;
                }
            }
            List<MarshallFieldInfo> fis = new ArrayList<>();
            fis.add(fieldInfo);
            r.add(new MarshallTypeInfo(targetClass, fis));
        }
        return List.copyOf(r);
    }

    private List<MarshallSwitchInfo> createHashInfos(List<MarshallFieldInfo> fieldInfos, Function<MarshallFieldInfo, byte[]> fn, int hashIndex) {
        List<MarshallSwitchInfo> r = new ArrayList<>();
        Hasher hasher = HashUtil.hasher(hashIndex);
        outer : for (MarshallFieldInfo fieldInfo : fieldInfos) {
            int hash = hasher.hash(fn.apply(fieldInfo));
            for (MarshallSwitchInfo marshallSwitchInfo : r) {
                if (marshallSwitchInfo.hash() == hash) {
                    marshallSwitchInfo.fieldInfos().add(fieldInfo);
                    continue outer;
                }
            }
            List<MarshallFieldInfo> list = new ArrayList<>();
            list.add(fieldInfo);
            r.add(new MarshallSwitchInfo(hash, list));
        }
        return List.copyOf(r);
    }

    private GeneratorBlock headBlock(GeneratorSource facadeSource, MarshallProcessorInfo info) {
        String providerClassName = facadeSource.register(Provider.class);
        String facadeClassName = facadeSource.className();
        String marshallFacadeClassName = facadeSource.register(MarshallFacade.class);
        String listClassName = facadeSource.register(List.class);
        String marshallInfoClassName = facadeSource.register(MarshallInfo.class);
        String marshallHashInfoClassName = facadeSource.register(MarshallHashInfo.class);
        GeneratorBlock b = new GeneratorBlock()
                .addLine("@" + providerClassName + "(target = " + marshallFacadeClassName + ".class)")
                .addLine("public final class " + facadeClassName + " implements " + marshallFacadeClassName + " {")
                .indent();
        if (info.typeElements().getLast().getKind() == ElementKind.CLASS) {
            String varhandleClassName = facadeSource.register(VarHandle.class);
            b.addLine("private static final " + listClassName + "<" + varhandleClassName + "> VHS;");
        }
        return b.addLine("private static final " + listClassName + "<" + marshallInfoClassName + "> MARSHALL_INFOS;")
                .addLine("private static final " + marshallHashInfoClassName + " HASH_INFO;")
                .newLine();
    }

    private GeneratorBlock vhInitializationBlock(GeneratorSource facadeSource, MarshallProcessorInfo info) {
        StringBuilder builder = facadeSource.builder();
        GeneratorBlock b = new GeneratorBlock();
        if (info.typeElements().getLast().getKind() == ElementKind.CLASS) {
            String methodHandlesClassName = facadeSource.register(MethodHandles.class);
            String varhandleClassName = facadeSource.register(VarHandle.class);
            String listClassName = facadeSource.register(List.class);
            String exceptionClassName = facadeSource.register(Exception.class);
            String exceptionInInitializerErrorClassName = facadeSource.register(ExceptionInInitializerError.class);
            b.addLine("try {")
                .indent()
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
                        ".class, " + AnnoUtil.escapeJavaStringLiteral(fieldInfo.fieldName(), builder) + ", " + fieldRawClassName + ".class);");
            }
            b.addLine("VHS = " + listClassName + ".of(" +
                            IntStream.range(0, info.fieldInfos().size()).mapToObj(i -> "vh" + i)
                                    .collect(Collectors.joining(", ")) + ");")
                    .unindent()
                    .addLine("} catch (" + exceptionClassName + " e) {")
                    .indent()
                    .addLine("throw new " + exceptionInInitializerErrorClassName + "(e);")
                    .unindent()
                    .addLine("}");
        }
        return b;
    }

    private List<String> getGenericTypeLiterals(GeneratorSource source, TypeMirror tm) {
        List<String> r = new ArrayList<>();
        if (tm.getKind() == TypeKind.DECLARED) {
            DeclaredType d = AnnoUtil.castDeclaredType(tm);
            for (TypeMirror typeArg : d.getTypeArguments()) {
                if (typeArg.getKind() == TypeKind.DECLARED) {
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

    private GeneratorBlock facadeInfoInitializationBlock(GeneratorSource facadeSource, MarshallProcessorInfo info) {
        StringBuilder builder = facadeSource.builder();
        GeneratorBlock b = new GeneratorBlock();
        String marshallInfoClassName = facadeSource.register(MarshallInfo.class);
        String marshallHashInfoClassName = facadeSource.register(MarshallHashInfo.class);
        String listClassName = facadeSource.register(List.class);
        for (MarshallFieldInfo fieldInfo : info.fieldInfos()) {
            Element fieldElement = fieldInfo.fieldElement();
            String fieldRawClassName = facadeSource.registerRawFieldElement(fieldElement);
            List<String> genericTypeLiterals = getGenericTypeLiterals(facadeSource, fieldElement.asType());
            String marshallInfoParams = String.join(", ", List.of(
                    fieldRawClassName + ".class",
                    !genericTypeLiterals.isEmpty() ? genericTypeLiterals.get(0) + ".class" : "null",
                    genericTypeLiterals.size() > 1 ? genericTypeLiterals.get(1) + ".class" : "null",
                    String.valueOf(fieldInfo.marshallIndex()),
                    AnnoUtil.escapeJavaStringLiteral(fieldInfo.fieldName(), builder),
                    AnnoUtil.escapeJavaStringLiteral(fieldInfo.mappedName(), builder),
                    String.valueOf(fieldInfo.skipSerializing()),
                    String.valueOf(fieldInfo.skipDeserializing())
            ));
            b.addLine(marshallInfoClassName + " mi" + fieldInfo.marshallIndex() +
                    " = new " + marshallInfoClassName + "(" + marshallInfoParams + ");");
        }
        String miParams = IntStream.range(0, info.fieldInfos().size()).mapToObj(i -> "mi" + i).collect(Collectors.joining(", "));
        return b.addLine("MARSHALL_INFOS = " + listClassName + ".of(" + miParams + ");")
                .addLine("HASH_INFO = new " + marshallHashInfoClassName +
                        "(MARSHALL_INFOS, " + info.fieldHashIndex() + ", " + info.mappedHashIndex() + ");");
    }

    private GeneratorBlock marshallableTypeMethod(GeneratorSource facadeSource, MarshallProcessorInfo info) {
        String overrideClassName = facadeSource.register(Override.class);
        String clsClassName = facadeSource.register(Class.class);
        String targetClassName = facadeSource.register(info.typeElements().getLast());
        return new GeneratorBlock().addLine("@" + overrideClassName)
                .addLine("public " + clsClassName + "<?> marshallableType() {")
                .indent()
                .addLine("return " + targetClassName + ".class;")
                .unindent()
                .addLine("}")
                .newLine();
    }

    private GeneratorBlock marshallInfosMethod(GeneratorSource facadeSource) {
        String overrideClassName = facadeSource.register(Override.class);
        String listClassName = facadeSource.register(List.class);
        String marshallInfoClassName = facadeSource.register(MarshallInfo.class);
        return new GeneratorBlock().addLine("@" + overrideClassName)
                .addLine("public " + listClassName + "<" + marshallInfoClassName + "> marshallInfos() {")
                .indent()
                .addLine("return MARSHALL_INFOS;")
                .unindent()
                .addLine("}")
                .newLine();
    }

    private GeneratorBlock primitiveElementsMethod(GeneratorSource facadeSource, MarshallProcessorInfo info) {
        TypeElement targetElement = info.typeElements().getLast();
        GeneratorBlock b = new GeneratorBlock();
        if (targetElement.getKind() == ElementKind.ENUM) {
            return b;
        }
        int primitiveElements = 0;
        for (MarshallFieldInfo fieldInfo : info.fieldInfos()) {
            Element fieldElement = fieldInfo.fieldElement();
            if (fieldElement.asType().getKind().isPrimitive()) {
                primitiveElements++;
            }
        }
        String overrideClassName = facadeSource.register(Override.class);
        return b.addLine("@" + overrideClassName)
                .addLine("public int primitiveElements() {")
                .indent()
                .addLine("return " + primitiveElements + ";")
                .unindent()
                .addLine("}")
                .newLine();
    }

    private GeneratorBlock marshallInfoByStringMethod(GeneratorSource facadeSource, MarshallProcessorInfo info, boolean f) {
        StringBuilder builder = facadeSource.builder();
        String overrideClassName = facadeSource.register(Override.class);
        String marshallInfoClassName = facadeSource.register(MarshallInfo.class);
        String stringClassName = facadeSource.register(String.class);
        String functionName = f ? "marshallInfoByFieldName" : "marshallInfoByMappedName";
        String argName = f ? "fieldName" : "mappedName";
        GeneratorBlock b = new GeneratorBlock()
                .addLine("@" + overrideClassName)
                .addLine("public " + marshallInfoClassName + " " + functionName + "(" + stringClassName + " " + argName + ") {")
                .indent()
                .addLine("return switch (" + argName + ") {")
                .indent();
        for (MarshallFieldInfo fieldInfo : info.fieldInfos()) {
            b.addLine("case " + AnnoUtil.escapeJavaStringLiteral(f ? fieldInfo.fieldName() : fieldInfo.mappedName(), builder) +
                    " -> MARSHALL_INFOS.get(" + fieldInfo.marshallIndex() + ");");
        }
        return b.addLine("default -> null;")
                .unindent()
                .addLine("};")
                .unindent()
                .addLine("}")
                .newLine();
    }

    private GeneratorBlock marshallInfoByBinaryMethod(GeneratorSource facadeSource, MarshallProcessorInfo info, boolean f, boolean m) {
        String overrideClassName = facadeSource.register(Override.class);
        String marshallInfoClassName = facadeSource.register(MarshallInfo.class);
        String paramType = m ? facadeSource.register(MemorySegment.class) : "byte[]";
        String paramName = m ? "segment" : "bytes";
        String paramUnit = m ? "long" : "int";
        String functionName = f ? "marshallInfoByFieldName" : "marshallInfoByMappedName";
        String hasherName = f ? "fieldNameUtf8Hasher" : "mappedNameUtf8Hasher";
        String eqName = f ? "fieldNameEquals" : "mappedNameEquals";
        GeneratorBlock b = new GeneratorBlock()
                .addLine("@" + overrideClassName)
                .addLine("public " + marshallInfoClassName + " " + functionName + "(" + paramType + " " +
                        paramName + ", " + paramUnit + " from, " + paramUnit + " to) {")
                .indent()
                .addLine("final int hash = HASH_INFO." + hasherName + "().hash(" + paramName + ", from, to);")
                .addLine("switch (hash) {")
                .indent();
        List<MarshallSwitchInfo> hashInfo = f ? info.fieldHashInfos() : info.mappedHashInfos();
        for (MarshallSwitchInfo h : hashInfo) {
            int hash = h.hash();
            List<MarshallFieldInfo> fis = h.fieldInfos();
            b.addLine("case " + hash + " -> {").indent();
            for (MarshallFieldInfo fi : fis) {
                String from = String.valueOf(f ? fi.fieldNameOffset() : fi.mappedNameOffset());
                String to = String.valueOf(f ? fi.fieldNameOffset() + fi.fieldNameUtf8Bytes().length : fi.mappedNameOffset() + fi.mappedNameUtf8Bytes().length);
                b.addLine("if(HASH_INFO." + eqName + "(" + String.join(", ", List.of(from, to, paramName, "from", "to")) + ")) {")
                        .indent()
                        .addLine("return MARSHALL_INFOS.get(" + fi.marshallIndex() + ");")
                        .unindent()
                        .addLine("}");
            }
            b.unindent().addLine("}");
        }
        return b.unindent()
                .addLine("}")
                .addLine("return null;")
                .unindent()
                .addLine("}")
                .newLine();
    }

    private List<GeneratorBlock> readerBlocks(GeneratorSource facadeSource, MarshallProcessorInfo info) {
        TypeElement targetElement = info.typeElements().getLast();
        ElementKind targetElementKind = targetElement.getKind();
        List<GeneratorBlock> r = new ArrayList<>();
        if (targetElementKind == ElementKind.ENUM) {
            return r;
        }
        String overrideClassName = facadeSource.register(Override.class);
        String objectClassName = facadeSource.register(Object.class);
        String targetClassName = facadeSource.register(targetElement);
        String unsupportedOperationExceptionClassName = facadeSource.register(UnsupportedOperationException.class);
        for (MarshallTypeInfo typeInfo : info.typeInfos()) {
            boolean marked = false;
            Class<?> cls = typeInfo.type();
            String rTypeName = (cls == Object.class) ? objectClassName : cls.getSimpleName();
            String fTypeName = (cls == Object.class) ? rTypeName : rTypeName.substring(0, 1).toUpperCase() + rTypeName.substring(1);
            GeneratorBlock b = new GeneratorBlock()
                    .addLine("@" + overrideClassName)
                    .addLine("public " + rTypeName + " read" + fTypeName + "(" + objectClassName + " instance, int index) {")
                    .indent()
                    .addLine(targetClassName + " entity = (" + targetClassName + ") instance;")
                    .addLine("return switch (index) {")
                    .indent();
            for (MarshallFieldInfo fieldInfo : typeInfo.fieldInfos()) {
                int marshallIndex = fieldInfo.marshallIndex();
                String assignStatement = switch (targetElementKind) {
                    case CLASS -> {
                        String fieldTypeName;
                        if (cls == Object.class) {
                            fieldTypeName = facadeSource.registerFieldElement(fieldInfo.fieldElement());
                            if (!marked && AnnoUtil.isGenericType(fieldTypeName)) {
                                marked = true;
                            }
                        } else {
                            fieldTypeName = rTypeName;
                        }
                        yield "(" + fieldTypeName + ") VHS.get(" + marshallIndex + ").get(entity);";
                    }
                    case RECORD -> "entity." + fieldInfo.fieldName() + "();";
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
                String suppressWarningsClassName = facadeSource.register(SuppressWarnings.class);
                b.prependLine("@" + suppressWarningsClassName + "(" + AnnoUtil.javaStringLiteral("unchecked") + ")");
            }
            r.add(b);
        }
        return r;
    }

    private GeneratorBlock newBuilderMethod(GeneratorSource facadeSource, MarshallProcessorInfo info) {
        TypeElement targetElement = info.typeElements().getLast();
        GeneratorBlock b = new GeneratorBlock();
        if (targetElement.getKind() == ElementKind.ENUM) {
            return b;
        }
        String overrideClassName = facadeSource.register(Override.class);
        String marshallWriterClassName = facadeSource.register(MarshallBuilder.class);
        b.addLine("@" + overrideClassName)
                .addLine("public " + marshallWriterClassName + " newBuilder() {")
                .indent();
        switch (targetElement.getKind()) {
            case CLASS -> b.addLine("return new Builder(new " + facadeSource.register(targetElement) + "());");
            case RECORD -> b.addLine("return new Builder();");
        }
        return b.unindent()
                .addLine("}")
                .newLine();
    }

    private GeneratorBlock constructMethod(GeneratorSource facadeSource, MarshallProcessorInfo info) {
        TypeElement targetElement = info.typeElements().getLast();
        GeneratorBlock b = new GeneratorBlock();
        if (targetElement.getKind() == ElementKind.ENUM) {
            return b;
        }
        String overrideClassName = facadeSource.register(Override.class);
        String objectClassName = facadeSource.register(Object.class);
        String marshallWriterClassName = facadeSource.register(MarshallBuilder.class);
        String illegalArgumentExceptionClassName = facadeSource.register(IllegalArgumentException.class);
        b.addLine("@" + overrideClassName)
                .addLine("public " + objectClassName + " construct(" + marshallWriterClassName + " writer) {")
                .indent();
        switch (targetElement.getKind()) {
            case CLASS -> b.addLine("if (writer instanceof Builder(" + facadeSource.register(targetElement) + " instance)) {")
                            .indent()
                            .addLine("return instance;")
                            .unindent()
                            .addLine("}");
            case RECORD -> b.addLine("if(writer instanceof Builder builder) {")
                            .indent()
                            .addLine("return builder.build();")
                            .unindent()
                            .addLine("}");
            default -> throw new AssertionError();
        }
        return b.addLine("throw new " + illegalArgumentExceptionClassName + "(" + AnnoUtil.javaStringLiteral("wrong writer : ") + " + writer.getClass().getName());")
                .unindent()
                .addLine("}")
                .newLine();
    }

    private List<GeneratorBlock> builderBlocks(GeneratorSource facadeSource, MarshallProcessorInfo info) {
        TypeElement targetElement = info.typeElements().getLast();
        ElementKind targetElementKind = targetElement.getKind();
        List<GeneratorBlock> r = new ArrayList<>();
        if (targetElementKind == ElementKind.ENUM) {
            return r;
        }
        String marshallBuilderClassName = facadeSource.register(MarshallBuilder.class);
        String targetClassName = facadeSource.register(targetElement);
        String overrideClassName = facadeSource.register(Override.class);
        String unsupportedOperationExceptionClassName = facadeSource.register(UnsupportedOperationException.class);
        if (targetElementKind == ElementKind.RECORD) {
            GeneratorBlock b = new GeneratorBlock()
                    .addLine("public final class Builder implements " + marshallBuilderClassName + " {")
                    .indent();
            for (MarshallFieldInfo fieldInfo : info.fieldInfos()) {
                b.addLine("private " + facadeSource.registerFieldElement(fieldInfo.fieldElement()) + " " + fieldInfo.fieldName() + ";");
            }
            r.add(b);
        } else if (targetElementKind == ElementKind.CLASS) {
            r.add(new GeneratorBlock()
                    .addLine("private record Builder(" + targetClassName + " instance) implements " + marshallBuilderClassName + " {")
                    .indent());
        } else {
            throw new AssertionError();
        }
        for (MarshallTypeInfo typeInfo : info.typeInfos()) {
            boolean marked = false;
            Class<?> cls = typeInfo.type();
            String rTypeName = (cls == Object.class) ? facadeSource.register(Object.class) : cls.getSimpleName();
            String fTypeName = (cls == Object.class) ? rTypeName : rTypeName.substring(0, 1).toUpperCase() + rTypeName.substring(1);
            GeneratorBlock b = new GeneratorBlock()
                    .addLine("@" + overrideClassName)
                    .addLine("public void write" + fTypeName + "(int index, " + rTypeName + " value) {")
                    .indent()
                    .addLine("switch (index) {")
                    .indent();
            for (MarshallFieldInfo fieldInfo : typeInfo.fieldInfos()) {
                int marshallIndex = fieldInfo.marshallIndex();
                String castExpr = "";
                if (cls == Object.class) {
                    String fieldTypeName = facadeSource.registerFieldElement(fieldInfo.fieldElement());
                    if (!marked && AnnoUtil.isGenericType(fieldTypeName)) {
                        marked = true;
                    }
                    castExpr = "(" + fieldTypeName + ") ";
                }
                String assignStatement = switch (targetElementKind) {
                    case CLASS -> "VHS.get(" + marshallIndex + ").set(instance, " + castExpr + "value);";
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
                String suppressWarningsClassName = facadeSource.register(SuppressWarnings.class);
                b.prependLine("@" + suppressWarningsClassName + "(" + AnnoUtil.javaStringLiteral("unchecked") + ")");
            }
            r.add(b);
        }
        if (targetElementKind == ElementKind.RECORD) {
            r.add(new GeneratorBlock()
                    .addLine(targetClassName + " build() {")
                    .indent()
                    .addLine("return new " + targetClassName + "(" +
                            info.fieldInfos().stream().map(MarshallFieldInfo::fieldName).collect(Collectors.joining(", ")) + ");")
                    .unindent()
                    .addLine("}")
                    .newLine());
        }
        r.add(new GeneratorBlock()
                .unindent()
                .addLine("}")
                .newLine());
        return r;
    }
}
