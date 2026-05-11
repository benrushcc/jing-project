package io.jingproject.marshallprocessor;

import io.jingproject.common.WriteBuffer;
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
import javax.lang.model.element.*;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
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
                GeneratorSource schemaSource = new GeneratorSource(t, "MarshallSchema");
                GeneratorSource facadeSource = new GeneratorSource(t, "MarshallFacade");
                writeMarshallSchemaSource(schemaSource, facadeSource, info);
                writeMarshallFacadeSource(facadeSource, schemaSource, info);
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
        // must be public class
        if (!t.getModifiers().contains(Modifier.PUBLIC)) {
            throw new AnnotationProcessorException("only public fieldElement can be annotated with @Marshallable");
        }
        // must be non-abstract class
        if (t.getModifiers().contains(Modifier.ABSTRACT)) {
            throw new AnnotationProcessorException("abstract class can not be annotated with @Marshallable");
        }
        // if using inheritance, must be annotationed with @Marshallable in the same module
        Elements elements = processingEnv.getElementUtils();
        Types typeUtils = processingEnv.getTypeUtils();
        ModuleElement rootModule = elements.getModuleOf(t);
        TypeMirror superType = t.getSuperclass();
        while (!typeUtils.isSameType(superType, objectType)) {
            Marshallable superTypeAnnotation = superType.getAnnotation(Marshallable.class);
            if (superTypeAnnotation == null) {
                throw new AnnotationProcessorException("super rawType must be annotated with @Marshallable");
            }
            Element superElement = typeUtils.asElement(superType);
            ModuleElement superModule = elements.getModuleOf(superElement);
            if (!rootModule.equals(superModule)) {
                throw new AnnotationProcessorException("super rawType must be within the same module");
            }
            superType = AnnoUtil.castTypeElement(superElement).getSuperclass();
        }
        // must have no-arg constructor, and fields cannot be final
        boolean foundNoArgConstructor = false;
        boolean foundFieldElement = false;
        for (Element e : t.getEnclosedElements()) {
            if (!foundNoArgConstructor && e.getKind().equals(ElementKind.CONSTRUCTOR)) {
                ExecutableElement ex = AnnoUtil.castExecutableElement(e);
                if (ex.getParameters().isEmpty() && ex.getModifiers().contains(Modifier.PUBLIC)) {
                    foundNoArgConstructor = true;
                }
            }
            if (e.getKind().equals(ElementKind.FIELD)) {
                VariableElement va = AnnoUtil.castVariableElement(e);
                if (va.getModifiers().contains(Modifier.STATIC)) {
                    continue;
                }
                if (va.getModifiers().contains(Modifier.FINAL)) {
                    throw new AnnotationProcessorException("only non-final fields could appear in normal classes");
                }
                checkVariableElementType(va);
                foundFieldElement = true;
            }
        }
        if (!foundNoArgConstructor) {
            throw new AnnotationProcessorException("no-arg constructor not found");
        }
        if (!foundFieldElement) {
            throw new AnnotationProcessorException("field not found");
        }
    }

    private void checkMarshallableRecordElement(TypeElement t) {
        // must be public record
        if (!t.getModifiers().contains(Modifier.PUBLIC)) {
            throw new AnnotationProcessorException("only public fieldElement can be annotated with @Marshallable");
        }
        // must have fields
        List<? extends Element> fields = t.getEnclosedElements().stream().filter(e -> e.getKind().equals(ElementKind.RECORD_COMPONENT)).toList();
        if (fields.isEmpty()) {
            throw new AnnotationProcessorException("enclosed elements can not be empty");
        }
        // check variable element type
        for (Element e : fields) {
            VariableElement va = AnnoUtil.castVariableElement(e);
            checkVariableElementType(va);
        }
    }

    private void checkMarshallableEnumElement(TypeElement t) {
        // must be public enum
        if (!t.getModifiers().contains(Modifier.PUBLIC)) {
            throw new AnnotationProcessorException("only public fieldElement can be annotated with @Marshallable");
        }
        // must have enum constants
        if (t.getEnclosedElements().stream().noneMatch(e -> e.getKind().equals(ElementKind.ENUM_CONSTANT))) {
            throw new AnnotationProcessorException("enum constants can not be empty");
        }
    }

    // variable can have at most two type args, and type args cannot have any more type args
    private void checkVariableElementType(VariableElement va) {
        TypeMirror tm = va.asType();
        if (tm.getKind() == TypeKind.DECLARED) {
            List<? extends TypeMirror> typeArgs = AnnoUtil.castDeclaredType(tm).getTypeArguments();
            // by design, at most 2 generic types are supported
            if (typeArgs.size() > 2) {
                throw new AnnotationProcessorException("field cannot have more than 2 type args");
            }
            AnnoUtil.validateTypeArgs(typeArgs);
        } else if(tm.getKind() == TypeKind.ARRAY) {
            AnnoUtil.validateArray(AnnoUtil.castArrayType(tm));
        }
    }

    private MarshallFieldInfo createMarshallFieldInfo(TypeElement t, int typeIndex, VariableElement v, int marshallIndex, int fieldNameOffset, int mappedNameOffset) {
        Marshallable marshallable = Objects.requireNonNull(t.getAnnotation(Marshallable.class));
        String fieldName = v.getSimpleName().toString();
        String mappedName = fieldName;
        boolean skipSerializing = false;
        boolean skipDeserializing = false;
        MarshallAttr attr = v.getAnnotation(MarshallAttr.class);
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
        return new MarshallFieldInfo(t, typeIndex, v, fieldName, mappedName, marshallIndex,
                fieldNameOffset, fieldNameLen,
                mappedNameOffset, mappedNameLen,
                skipSerializing, skipDeserializing);
    }

    private MarshallProcessorInfo createMarshallProcessorInfo(TypeElement t) {
        if (t.getKind() == ElementKind.CLASS && processingEnv.getTypeUtils().isSameType(t.getSuperclass(), objectType)) {
            return createMarshallExtendedProcessorInfo(t);
        } else {
            return createMarshallNormalProcessorInfo(t);
        }
    }

    private MarshallProcessorInfo createMarshallNormalProcessorInfo(TypeElement t) {
        List<MarshallFieldInfo> fis = new ArrayList<>();
        Map<Integer, List<MarshallFieldInfo>> fh = new HashMap<>();
        Map<Integer, List<MarshallFieldInfo>> mh = new HashMap<>();
        int marshallIndex = 0;
        int fieldNameIndex = 0;
        int mappedNameIndex = 0;
        ElementKind targetKind = switch (t.getKind()) {
            case CLASS -> ElementKind.FIELD;
            case RECORD -> ElementKind.RECORD_COMPONENT;
            case ENUM -> ElementKind.ENUM_CONSTANT;
            default -> throw new AssertionError();
        };
        for (Element e : t.getEnclosedElements()) {
            if (e.getKind().equals(targetKind)) {
                VariableElement v = AnnoUtil.castVariableElement(e);
                MarshallFieldInfo fi = createMarshallFieldInfo(t, 0, v, marshallIndex, fieldNameIndex, mappedNameIndex);
                marshallIndex = Math.incrementExact(marshallIndex);
                fis.add(fi);
                fieldNameIndex = Math.addExact(fieldNameIndex, fi.fieldNameLen());
                mappedNameIndex = Math.addExact(mappedNameIndex, fi.mappedNameLen());
            }
        }
        Hasher fieldNameHasher = HashUtil.calcHasher(fis.stream().map(MarshallFieldInfo::fieldName).toList());
        Hasher mappedNameHasher = HashUtil.calcHasher(fis.stream().map(MarshallFieldInfo::mappedName).toList());
        for (MarshallFieldInfo fi : fis) {
            int fieldHash = fieldNameHasher.hash(fi.fieldName());
            fh.computeIfAbsent(fieldHash, _ -> new ArrayList<>()).add(fi);
            int mappedHash = mappedNameHasher.hash(fi.mappedName());
            mh.computeIfAbsent(mappedHash, _ -> new ArrayList<>()).add(fi);
        }
        return new MarshallProcessorInfo(List.of(t), List.copyOf(fis), Map.copyOf(fh), Map.copyOf(mh));
    }

    private MarshallProcessorInfo createMarshallExtendedProcessorInfo(TypeElement t) {
        LinkedList<TypeElement> ts = new LinkedList<>();
        List<MarshallFieldInfo> fis = new ArrayList<>();
        Map<Integer, List<MarshallFieldInfo>> fh = new HashMap<>();
        Map<Integer, List<MarshallFieldInfo>> mh = new HashMap<>();
        Types typeUtils = processingEnv.getTypeUtils();
        TypeMirror head = t.asType();
        while (!typeUtils.isSameType(head, objectType)) {
            TypeElement te = AnnoUtil.castTypeElement(typeUtils.asElement(head));
            ts.addFirst(te);
            head = te.getSuperclass();
        }
        int marshallIndex = 0;
        int fieldNameIndex = 0;
        int mappedNameIndex = 0;
        for (int typeIndex = 0; typeIndex < ts.size(); typeIndex++) {
            TypeElement te = ts.get(typeIndex);
            for (Element e : te.getEnclosedElements()) {
                if (e.getKind().equals(ElementKind.FIELD)) {
                    VariableElement v = AnnoUtil.castVariableElement(e);
                    MarshallFieldInfo fi = createMarshallFieldInfo(te, typeIndex, v, marshallIndex, fieldNameIndex, mappedNameIndex);
                    marshallIndex = Math.incrementExact(marshallIndex);
                    fis.add(fi);
                    fieldNameIndex = Math.addExact(fieldNameIndex, fi.fieldNameLen());
                    mappedNameIndex = Math.addExact(mappedNameIndex, fi.mappedNameLen());
                }
            }
        }
        Hasher fieldNameHasher = HashUtil.calcHasher(fis.stream().map(MarshallFieldInfo::fieldName).toList());
        Hasher mappedNameHasher = HashUtil.calcHasher(fis.stream().map(MarshallFieldInfo::mappedName).toList());
        for (MarshallFieldInfo fi : fis) {
            int fieldHash = fieldNameHasher.hash(fi.fieldName());
            fh.computeIfAbsent(fieldHash, _ -> new ArrayList<>()).add(fi);
            int mappedHash = mappedNameHasher.hash(fi.mappedName());
            mh.computeIfAbsent(mappedHash, _ -> new ArrayList<>()).add(fi);
        }
        return new MarshallProcessorInfo(List.of(t), List.copyOf(fis), Map.copyOf(fh), Map.copyOf(mh));
    }

    private void writeMarshallSchemaSource(GeneratorSource schemaSource, GeneratorSource facadeSource, MarshallProcessorInfo info) {
        TypeElement targetElement = info.typeElements().getLast();
        if (targetElement.getKind().equals(ElementKind.ENUM)) {
            // enum don't need schemas
            return;
        }
        List<GeneratorBlock> blocks = new ArrayList<>();
        String schemaClassName = schemaSource.className();
        String marshallSchemaClassName = schemaSource.register(MarshallSchema.class);
        String unsupportedOperationExceptionClassName = schemaSource.register(UnsupportedOperationException.class);
        String overrideClassName = schemaSource.register(Override.class);
        Map<Class<?>, List<MarshallFieldInfo>> switchMap = generateSwitchMap(info);
        switch (targetElement.getKind()) {
            case CLASS -> {
                String facadeClassName = schemaSource.register(facadeSource);
                String targetClassName = schemaSource.register(targetElement);
                blocks.add(new GeneratorBlock()
                        .addLine("public record " + schemaClassName + " (")
                        .indent()
                        .addLine(facadeClassName + " facade,")
                        .addLine(targetClassName + " instance")
                        .unindent()
                        .addLine(") implements " + marshallSchemaClassName + " {")
                        .newLine()
                        .indent());
                for (Map.Entry<Class<?>, List<MarshallFieldInfo>> entry : switchMap.entrySet()) {
                    Class<?> cls = entry.getKey();
                    List<MarshallFieldInfo> fis = entry.getValue();
                    if (cls == Object.class) {
                        boolean marked = false;
                        String objectClassName = schemaSource.register(Object.class);
                        GeneratorBlock b = new GeneratorBlock()
                                .addLine("@" + overrideClassName)
                                .addLine("public void setObject(int offset, " + objectClassName + " value) {")
                                .indent()
                                .addLine("switch (offset) {")
                                .indent();
                        for (MarshallFieldInfo fieldInfo : fis) {
                            int marshallIndex = fieldInfo.marshallIndex();
                            String fieldType = schemaSource.register(fieldInfo.fieldElement());
                            if (!marked && AnnoUtil.isGenericType(fieldType)) {
                                marked = true;
                            }
                            b.addLine("case " + marshallIndex + " -> facade.marshallInfoByIndex(" +
                                    marshallIndex + ").vh().set(instance, (" + fieldType + ") value);");
                        }
                        b.addLine("default -> throw new " + unsupportedOperationExceptionClassName + "();")
                                .unindent()
                                .addLine("}")
                                .unindent()
                                .addLine("}")
                                .newLine();
                        if (marked) {
                            String suppressWarningsClassName = schemaSource.register(SuppressWarnings.class);
                            blocks.add(new GeneratorBlock().addLine("@" + suppressWarningsClassName + "(\"unchecked\")"));
                        }
                        blocks.add(b);
                    } else {
                        String primitiveType = cls.getSimpleName();
                        String primitiveTypeString = primitiveType.substring(0, 1).toUpperCase() + primitiveType.substring(1);
                        GeneratorBlock b = new GeneratorBlock()
                                .addLine("@" + overrideClassName)
                                .addLine("public void set" + primitiveTypeString + "(int offset, " + primitiveType + " value) {")
                                .indent()
                                .addLine("switch (offset) {")
                                .indent();
                        for (MarshallFieldInfo fieldInfo : fis) {
                            int marshallIndex = fieldInfo.marshallIndex();
                            b.addLine("case " + marshallIndex + " -> facade.marshallInfoByIndex(" + marshallIndex + ").vh().set(instance, value);");
                        }
                        b.addLine("default -> throw new " + unsupportedOperationExceptionClassName + "();")
                                .unindent()
                                .addLine("}")
                                .unindent()
                                .addLine("}")
                                .newLine();
                        blocks.add(b);
                    }
                }
                blocks.add(new GeneratorBlock().newLine().unindent().addLine("}").newLine());
            }
            case RECORD -> {
                GeneratorBlock b = new GeneratorBlock()
                        .addLine("public final class " + schemaClassName +
                                " implements " + marshallSchemaClassName + " {")
                        .indent();
                for (MarshallFieldInfo fieldInfo : info.fieldInfos()) {
                    String fieldType = schemaSource.register(fieldInfo.fieldElement());
                    b.addLine("private " + fieldType + " " + fieldInfo.fieldName() + ";");
                }
                blocks.add(b.newLine());
                for (Map.Entry<Class<?>, List<MarshallFieldInfo>> entry : switchMap.entrySet()) {
                    Class<?> cls = entry.getKey();
                    List<MarshallFieldInfo> fis = entry.getValue();
                    if (cls == Object.class) {
                        boolean marked = false;
                        String objectClassName = schemaSource.register(Object.class);
                        GeneratorBlock objectGetBlock = new GeneratorBlock().addLine("@" + overrideClassName)
                                .addLine("public " + objectClassName + " getObject(int offset) {").indent()
                                .addLine("switch (offset) {").indent();
                        GeneratorBlock objectSetBlock = new GeneratorBlock().addLine("@" + overrideClassName)
                                .addLine("public void setObject(int offset, " + objectClassName + " value) {").indent()
                                .addLine("switch (offset) {").indent();
                        for (MarshallFieldInfo fieldInfo : fis) {
                            int marshallIndex = fieldInfo.marshallIndex();
                            String fieldType = schemaSource.register(fieldInfo.fieldElement());
                            if (!marked && AnnoUtil.isGenericType(fieldType)) {
                                marked = true;
                            }
                            objectGetBlock.addLine("case " + marshallIndex + " -> {")
                                    .indent().addLine("return " + fieldInfo.fieldName() + ";")
                                    .unindent().addLine("}");
                            objectSetBlock.addLine("case " + marshallIndex + " -> this." + fieldInfo.fieldName() + " = (" + fieldType + ") value;");
                        }
                        objectGetBlock.addLine("default -> throw new " + unsupportedOperationExceptionClassName + "();")
                                .unindent().addLine("}").unindent().addLine("}").newLine();
                        objectSetBlock.addLine("default -> throw new " + unsupportedOperationExceptionClassName + "();")
                                .unindent().addLine("}").unindent().addLine("}").newLine();
                        blocks.add(objectGetBlock);
                        if (marked) {
                            String suppressWarningsClassName = schemaSource.register(SuppressWarnings.class);
                            blocks.add(new GeneratorBlock().addLine("@" + suppressWarningsClassName + "(\"unchecked\")"));
                        }
                        blocks.add(objectSetBlock);
                    } else {
                        String primitiveType = cls.getSimpleName();
                        String primitiveTypeString = primitiveType.substring(0, 1).toUpperCase() + primitiveType.substring(1);
                        GeneratorBlock primitiveGetBlock = new GeneratorBlock().addLine("@" + overrideClassName)
                                .addLine("public " + primitiveType + " get" + primitiveTypeString + "(int offset) {")
                                .indent().addLine("switch (offset) {").indent();
                        GeneratorBlock primitiveSetBlock = new GeneratorBlock().addLine("@" + overrideClassName)
                                .addLine("public void set" + primitiveTypeString + "(int offset, " + primitiveType + " value) {")
                                .indent().addLine("switch (offset) {").indent();
                        for (MarshallFieldInfo fieldInfo : fis) {
                            int marshallIndex = fieldInfo.marshallIndex();
                            primitiveGetBlock.addLine("case " + marshallIndex + " -> {").indent()
                                    .addLine("return " + fieldInfo.fieldName() + ";").unindent().addLine("}");
                            primitiveSetBlock.addLine("case " + marshallIndex + " -> this." + fieldInfo.fieldName() + " = value;");
                        }
                        primitiveGetBlock.addLine("default -> throw new " + unsupportedOperationExceptionClassName + "();")
                                .unindent().addLine("}").unindent().addLine("}").newLine();
                        primitiveSetBlock.addLine("default -> throw new " + unsupportedOperationExceptionClassName + "();")
                                .unindent().addLine("}").unindent().addLine("}").newLine();
                        blocks.add(primitiveGetBlock);
                        blocks.add(primitiveSetBlock);
                    }
                }
                blocks.add(new GeneratorBlock().newLine().unindent().addLine("}").newLine());
            }
            default -> throw new AssertionError();
        }
        schemaSource.addBlocks(blocks);
        schemaSource.writeToFiler(processingEnv);
    }

    private Map<Class<?>, List<MarshallFieldInfo>> generateSwitchMap(MarshallProcessorInfo info) {
        Map<Class<?>, List<MarshallFieldInfo>> r = new HashMap<>();
        for (MarshallFieldInfo fieldInfo : info.fieldInfos()) {
            switch (fieldInfo.fieldElement().asType().getKind()) {
                case BOOLEAN -> r.computeIfAbsent(boolean.class, _ -> new ArrayList<>()).add(fieldInfo);
                case BYTE -> r.computeIfAbsent(byte.class, _ -> new ArrayList<>()).add(fieldInfo);
                case SHORT -> r.computeIfAbsent(short.class, _ -> new ArrayList<>()).add(fieldInfo);
                case CHAR -> r.computeIfAbsent(char.class, _ -> new ArrayList<>()).add(fieldInfo);
                case INT -> r.computeIfAbsent(int.class, _ -> new ArrayList<>()).add(fieldInfo);
                case LONG -> r.computeIfAbsent(long.class, _ -> new ArrayList<>()).add(fieldInfo);
                case FLOAT -> r.computeIfAbsent(float.class, _ -> new ArrayList<>()).add(fieldInfo);
                case DOUBLE -> r.computeIfAbsent(double.class, _ -> new ArrayList<>()).add(fieldInfo);
                default -> r.computeIfAbsent(Object.class, _ -> new ArrayList<>()).add(fieldInfo);
            }
        }
        return Map.copyOf(r);
    }

    private void writeMarshallFacadeSource(GeneratorSource facadeSource, GeneratorSource schemaSource, MarshallProcessorInfo info) {
        facadeSource.addBlocks(List.of(
                buildStaticHeadBlock(facadeSource, info),
                buildLookupInitializationBlock(facadeSource, info),
                buildVarhandleInitializationBlock(facadeSource, info),
                buildMarshallInfoInitializationBlock(facadeSource, info),
                buildStaticTailBlock(facadeSource, info),
                buildMarshallableTypeMethod(facadeSource, info),
                buildConstructorMethod(facadeSource, info),
                buildConstructMethod(facadeSource, schemaSource, info),
                buildTotalElementsMethod(facadeSource, info),
                buildWriteNameByIndexMethod(facadeSource, info, true),
                buildWriteNameByIndexMethod(facadeSource, info, false),
                buildMarshallInfoByIndexMethod(facadeSource),
                buildMarshallInfoByStringMethod(facadeSource, info, true),
                buildMarshallInfoByBinaryMethod(facadeSource, info, true, false),
                buildMarshallInfoByBinaryMethod(facadeSource, info, true, true),
                buildMarshallInfoByStringMethod(facadeSource, info, false),
                buildMarshallInfoByBinaryMethod(facadeSource, info, false, false),
                buildMarshallInfoByBinaryMethod(facadeSource, info, false, true),
                buildNewSchemaMethod(facadeSource, schemaSource, info),
                new GeneratorBlock().unindent().addLine("}").newLine()
        ));
        facadeSource.writeToFiler(processingEnv);
    }

    private GeneratorBlock buildStaticHeadBlock(GeneratorSource facadeSource, MarshallProcessorInfo info) {
        String providerClassName = facadeSource.register(Provider.class);
        String facadeClassName = facadeSource.className();
        String marshallFacadeClassName = facadeSource.register(MarshallFacade.class);
        String marshallFacadeInfoClassName = facadeSource.register(MarshallFacadeInfo.class);
        GeneratorBlock b = new GeneratorBlock()
                .addLine("@" + providerClassName + "(target = " + marshallFacadeClassName + ".class)")
                .addLine("public final class " + facadeClassName + " implements " + marshallFacadeClassName + " {")
                .indent();
        if (info.typeElements().getLast().getKind() != ElementKind.ENUM) {
            String methodHandleClassName = facadeSource.register(MethodHandle.class);
            b.addLine("private static final " + methodHandleClassName + " CONSTRUCTOR_MH;");
        }
        return b.addLine("private static final " + marshallFacadeInfoClassName + " FACADE_INFO;")
                .newLine()
                .addLine("static {")
                .indent();
    }

    private GeneratorBlock buildLookupInitializationBlock(GeneratorSource facadeSource, MarshallProcessorInfo info) {
        GeneratorBlock b = new GeneratorBlock();
        if (info.typeElements().getLast().getKind() == ElementKind.ENUM) {
            // enum don't need lookup
            return b;
        }
        String methodHandlesClassName = facadeSource.register(MethodHandles.class);
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
        return b;
    }

    private GeneratorBlock buildVarhandleInitializationBlock(GeneratorSource facadeSource, MarshallProcessorInfo info) {
        GeneratorBlock b = new GeneratorBlock();
        if (info.typeElements().getLast().getKind() == ElementKind.ENUM) {
            // enum don't need varhandle
            return b;
        }
        String varhandleClassName = facadeSource.register(VarHandle.class);
        for (MarshallFieldInfo fieldInfo : info.fieldInfos()) {
            String teClassName = facadeSource.register(fieldInfo.typeElement());
            String fieldRawClassName = facadeSource.registerRaw(fieldInfo.fieldElement());
            b.addLine(varhandleClassName + " vh" + fieldInfo.marshallIndex() +
                    " = lookup" + fieldInfo.typeIndex() + ".findVarHandle(" + teClassName +
                    ".class, \"" + fieldInfo.fieldName() + "\", " + fieldRawClassName + ".class);");
        }
        return b;
    }

    private GeneratorBlock buildMarshallInfoInitializationBlock(GeneratorSource facadeSource, MarshallProcessorInfo info) {
        GeneratorBlock b = new GeneratorBlock();
        String marshallInfoClassName = facadeSource.register(MarshallInfo.class);

        for (MarshallFieldInfo fieldInfo : info.fieldInfos()) {
            VariableElement fieldElement = fieldInfo.fieldElement();
            String fieldRawClassName = facadeSource.registerRaw(fieldElement);
            String fieldFirstGenericTypeClassName = "";
            String fieldSecondGenericTypeClassName = "";
            String vhValue = "null";
            String enumValue = "null";
            if (fieldInfo.typeElement().getKind() == ElementKind.ENUM) {
                enumValue = facadeSource.register(fieldInfo.typeElement()) + "." + fieldInfo.fieldName();
            } else {
                vhValue = "vh" + fieldInfo.marshallIndex();
                TypeMirror tm = fieldElement.asType();
                if (tm.getKind() == TypeKind.DECLARED) {
                    DeclaredType d = AnnoUtil.castDeclaredType(tm);
                    List<? extends TypeMirror> typeArgs = d.getTypeArguments();
                    if (!typeArgs.isEmpty()) {
                        TypeMirror firstGenericTypeMirror = typeArgs.get(0);
                        if(firstGenericTypeMirror.getKind() == TypeKind.DECLARED) {
                            DeclaredType firstGenericDeclaredType = AnnoUtil.castDeclaredType(firstGenericTypeMirror);
                            TypeElement firstGenericTypeElement = AnnoUtil.castTypeElement(firstGenericDeclaredType.asElement());
                            fieldFirstGenericTypeClassName = facadeSource.register(firstGenericTypeElement);
                        } else {
                            throw new AnnotationProcessorException("not a declared generic type : " + firstGenericTypeMirror);
                        }
                        // at most 2 typeArgs
                        if (typeArgs.size() == 2) {
                            TypeMirror secondGenericTypeMirror = typeArgs.get(1);
                            if(secondGenericTypeMirror.getKind() == TypeKind.DECLARED) {
                                DeclaredType secondGenericDeclaredType = AnnoUtil.castDeclaredType(secondGenericTypeMirror);
                                TypeElement secondGenericTypeElement = AnnoUtil.castTypeElement(secondGenericDeclaredType.asElement());
                                fieldSecondGenericTypeClassName = facadeSource.register(secondGenericTypeElement);
                            } else {
                                throw new AnnotationProcessorException("not a declared generic type : " + secondGenericTypeMirror);
                            }
                        }
                    }
                }
            }
            String marshallInfoParams = String.join(", ", List.of(
                    fieldRawClassName + ".class",
                    fieldFirstGenericTypeClassName.isEmpty() ? "null" : fieldFirstGenericTypeClassName + ".class",
                    fieldSecondGenericTypeClassName.isEmpty() ? "null" : fieldSecondGenericTypeClassName + ".class",
                    String.valueOf(fieldInfo.marshallIndex()),
                    "\"" + fieldInfo.fieldName() + "\"",
                    "\"" + fieldInfo.mappedName() + "\"",
                    vhValue,
                    enumValue,
                    String.valueOf(fieldInfo.skipSerializing()),
                    String.valueOf(fieldInfo.skipDeserializing())
            ));
            b.addLine(marshallInfoClassName + " mi" + fieldInfo.marshallIndex() +
                    "= new " + marshallInfoClassName + "(" + marshallInfoParams + ");");
        }
        return b;
    }

    private GeneratorBlock buildStaticTailBlock(GeneratorSource facadeSource, MarshallProcessorInfo info) {
        GeneratorBlock b = new GeneratorBlock();
        TypeElement targetElement = info.typeElements().getLast();
        String targetClassName = facadeSource.register(targetElement);
        String methodTypeClassName = facadeSource.register(MethodType.class);
        String marshallFacadeInfoClassName = facadeSource.register(MarshallFacadeInfo.class);
        String listClassName = facadeSource.register(List.class);
        boolean isEnum = targetElement.getKind() == ElementKind.ENUM;
        boolean isRecord = targetElement.getKind() == ElementKind.RECORD;
        if (!isEnum) {
            List<String> constructorParams = new ArrayList<>();
            constructorParams.add("void.class");
            if (isRecord) {
                for (MarshallFieldInfo fieldInfo : info.fieldInfos()) {
                    String fieldRawClassName = facadeSource.registerRaw(fieldInfo.fieldElement());
                    constructorParams.add(fieldRawClassName + ".class");
                }
            }
            b.addLine("CONSTRUCTOR_MH = lookup" + (info.typeElements().size() - 1) + ".findConstructor(" +
                    targetClassName + ".class, " + methodTypeClassName + ".methodType(" +
                    String.join(", ", constructorParams) + "));");
        }
        b.addLine("FACADE_INFO = new " + marshallFacadeInfoClassName + "(" + listClassName +
                ".of(" + IntStream.range(0, info.fieldInfos().size()).mapToObj(i -> "mi" + i).collect(Collectors.joining(", ")) + "));");
        if (isEnum) {
            b.unindent().addLine("}").newLine();
        } else {
            String exceptionClassName = facadeSource.register(Exception.class);
            String exceptionInInitializerErrorClassName = facadeSource.register(ExceptionInInitializerError.class);
            b.unindent().addLine("} catch (" + exceptionClassName + " p) {")
                    .indent().addLine("throw new " + exceptionInInitializerErrorClassName + "(p);")
                    .unindent().addLine("}").unindent().addLine("}").newLine();
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

    private GeneratorBlock buildConstructorMethod(GeneratorSource facadeSource, MarshallProcessorInfo info) {
        String overrideClassName = facadeSource.register(Override.class);
        String methodHandleClassName = facadeSource.register(MethodHandle.class);
        GeneratorBlock b = new GeneratorBlock().addLine("@" + overrideClassName)
                .addLine("public " + methodHandleClassName + " constructor() {").indent();
        if (info.typeElements().getLast().getKind() == ElementKind.ENUM) {
            String uoeClassName = facadeSource.register(UnsupportedOperationException.class);
            b.addLine("throw new " + uoeClassName + "();");
        } else {
            b.addLine("return CONSTRUCTOR_MH;");
        }
        return b.unindent().addLine("}").newLine();
    }

    private GeneratorBlock buildConstructMethod(GeneratorSource facadeSource, GeneratorSource schemaSource, MarshallProcessorInfo info) {
        String overrideClassName = facadeSource.register(Override.class);
        String objectClassName = facadeSource.register(Object.class);
        String marshallSchemaClassName = facadeSource.register(MarshallSchema.class);
        GeneratorBlock b = new GeneratorBlock().addLine("@" + overrideClassName)
                .addLine("public " + objectClassName + " construct(" + marshallSchemaClassName + " schema) {").indent();
        TypeElement targetElement = info.typeElements().getLast();
        switch (targetElement.getKind()) {
            case CLASS -> {
                String schemaClassName = facadeSource.register(schemaSource);
                String targetClassName = facadeSource.register(targetElement);
                String iaeClassName = facadeSource.register(IllegalArgumentException.class);
                b.addLine("if(schema instanceof " + schemaClassName + "(_, " + targetClassName + " instance)) {")
                        .indent().addLine("return instance;").unindent().addLine("}")
                        .addLine("throw new " + iaeClassName + "(\"wrong schema type\");")
                        .unindent().addLine("}").newLine();
            }
            case RECORD -> {
                String targetClassName = facadeSource.register(targetElement);
                for (MarshallFieldInfo fieldInfo : info.fieldInfos()) {
                    VariableElement fieldElement = fieldInfo.fieldElement();
                    int marshallIndex = fieldInfo.marshallIndex();
                    switch (fieldElement.asType().getKind()) {
                        case BOOLEAN ->
                                b.addLine("boolean v" + marshallIndex + " = schema.getBoolean(" + marshallIndex + ");");
                        case BYTE -> b.addLine("byte v" + marshallIndex + " = schema.getByte(" + marshallIndex + ");");
                        case SHORT ->
                                b.addLine("short v" + marshallIndex + " = schema.getShort(" + marshallIndex + ");");
                        case CHAR -> b.addLine("char v" + marshallIndex + " = schema.getChar(" + marshallIndex + ");");
                        case INT -> b.addLine("int v" + marshallIndex + " = schema.getInt(" + marshallIndex + ");");
                        case LONG -> b.addLine("long v" + marshallIndex + " = schema.getLong(" + marshallIndex + ");");
                        case FLOAT ->
                                b.addLine("float v" + marshallIndex + " = schema.getFloat(" + marshallIndex + ");");
                        case DOUBLE ->
                                b.addLine("double v" + marshallIndex + " = schema.getDouble(" + marshallIndex + ");");
                        default -> {
                            String fieldTypeClassName = facadeSource.register(fieldElement);
                            if (AnnoUtil.isGenericType(fieldTypeClassName)) {
                                String suppressWarningsClassName = facadeSource.register(SuppressWarnings.class);
                                b.addLine("@" + suppressWarningsClassName + "(\"unchecked\")");
                            }
                            b.addLine(fieldTypeClassName + " v" + marshallIndex + " = (" + fieldTypeClassName + ") schema.getObject(" + marshallIndex + ");");
                        }
                    }
                }
                b.addLine("return new " + targetClassName + "(" + IntStream.range(0, info.fieldInfos().size()).mapToObj(i -> "v" + i).collect(Collectors.joining(", ")) + ");")
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

    private GeneratorBlock buildTotalElementsMethod(GeneratorSource facadeSource, MarshallProcessorInfo info) {
        String overrideClassName = facadeSource.register(Override.class);
        return new GeneratorBlock().addLine("@" + overrideClassName)
                .addLine("public int totalElements() {")
                .indent()
                .addLine("return " + info.fieldInfos().size() + ";")
                .unindent()
                .addLine("}").newLine();
    }

    private GeneratorBlock buildWriteNameByIndexMethod(GeneratorSource facadeSource, MarshallProcessorInfo info, boolean f) {
        String overrideClassName = facadeSource.register(Override.class);
        String writeBufferClassName = facadeSource.register(WriteBuffer.class);
        String illegalArgumentExceptionClassName = facadeSource.register(IllegalArgumentException.class);
        String lowerType = f ? "fieldName" : "mappedName";
        String upperType = f ? "FieldName" : "MappedName";
        GeneratorBlock b = new GeneratorBlock().addLine("@" + overrideClassName)
                .addLine("public void write" + upperType + "ByIndex(" + writeBufferClassName + " writeBuffer, int index) {")
                .indent().addLine("switch (index) {")
                .indent();
        for (MarshallFieldInfo fi : info.fieldInfos()) {
            int offset = f ? fi.fieldNameOffset() : fi.mappedNameOffset();
            int len = f ? fi.fieldNameLen() : fi.mappedNameLen();
            b.addLine("case " + fi.marshallIndex() + " -> writeBuffer.writeBytes(FACADE_INFO." + lowerType +
                    "Bytes(), " + offset + ", " + len + ");");
        }
        return b.addLine("default -> throw new " + illegalArgumentExceptionClassName + "(\"wrong index\");")
                .unindent().addLine("}").unindent().addLine("}").newLine();
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
        Map<Integer, List<MarshallFieldInfo>> hashInfo = f ? info.fieldHash() : info.mappedHash();
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

    private GeneratorBlock buildNewSchemaMethod(GeneratorSource facadeSource, GeneratorSource schemaSource, MarshallProcessorInfo info) {
        String overrideClassName = facadeSource.register(Override.class);
        String marshallSchemaClassName = facadeSource.register(MarshallSchema.class);
        GeneratorBlock b = new GeneratorBlock().addLine("@" + overrideClassName)
                .addLine("public " + marshallSchemaClassName + " newSchema() {").indent();
        TypeElement targetElement = info.typeElements().getLast();
        switch (targetElement.getKind()) {
            case CLASS -> {
                String targetClassName = facadeSource.register(targetElement);
                String schemaClassName = facadeSource.register(schemaSource);
                b.addLine(targetClassName + " instance = new " + targetClassName + "();")
                        .addLine("return new " + schemaClassName + "(this, instance);");
            }
            case RECORD -> {
                String schemaClassName = facadeSource.register(schemaSource);
                b.addLine("return new " + schemaClassName + "();");
            }
            case ENUM -> {
                String unsupportedOperationExceptionClassName = facadeSource.register(UnsupportedOperationException.class);
                b.addLine("throw new " + unsupportedOperationExceptionClassName + "();");
            }
            default -> throw new AssertionError();
        }
        return b.unindent().addLine("}").newLine();
    }
}
