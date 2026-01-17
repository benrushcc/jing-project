package io.jingproject.ffmprocessor;

import io.jingproject.annprocess.AnnotationProcessorException;
import io.jingproject.annprocess.GeneratorBlock;
import io.jingproject.annprocess.GeneratorSource;
import io.jingproject.annprocess.Provider;
import io.jingproject.common.Os;
import io.jingproject.ffm.Downcall;
import io.jingproject.ffm.FFM;
import io.jingproject.ffm.SharedLib;
import io.jingproject.ffm.SharedLibs;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class FfmProcessor extends AbstractProcessor {

    private TypeMirror memorySegmentType;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        memorySegmentType = processingEnv.getElementUtils().getTypeElement(MemorySegment.class.getCanonicalName()).asType();
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of(FFM.class.getCanonicalName());
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (!roundEnv.processingOver()) {
            processFFM(roundEnv);
        }
        return true;
    }

    record FfmData(
            TypeElement typeElement,
            FFM ffm,
            List<DowncallData> downcallDataList
    ) {

    }

    record DowncallData(
            ExecutableElement executableElement,
            Downcall downcall
    ) {

    }

    private void processFFM(RoundEnvironment roundEnv) {
        for (Element element : roundEnv.getElementsAnnotatedWith(FFM.class)) {
            if (element instanceof TypeElement t) {
                if (t.getNestingKind() != NestingKind.TOP_LEVEL) {
                    throw new AnnotationProcessorException("Only top level element can be annotated with @FFM");
                }
                if (t.getKind() != ElementKind.INTERFACE) {
                    throw new AnnotationProcessorException("Only interface element can be annotated with @FFM");
                }
                if (!t.getModifiers().contains(Modifier.PUBLIC)) {
                    throw new AnnotationProcessorException("Only public interface can be annotated with @FFM");
                }
                if (t.getModifiers().contains(Modifier.SEALED)) {
                    throw new AnnotationProcessorException("Only non-sealed interface can be annotated with @FFM");
                }
                FFM f = t.getAnnotation(FFM.class);
                List<DowncallData> d = new ArrayList<>();
                for (Element el : t.getEnclosedElements()) {
                    if (el.getKind() == ElementKind.METHOD && el instanceof ExecutableElement ex) {
                        Set<Modifier> modifiers = ex.getModifiers();
                        if (modifiers.contains(Modifier.DEFAULT) || modifiers.contains(Modifier.STATIC) || modifiers.contains(Modifier.PRIVATE)) {
                            continue;
                        }
                        Downcall downcall = ex.getAnnotation(Downcall.class);
                        if (downcall == null) {
                            throw new AnnotationProcessorException("Method must have @Downcall annotation");
                        }
                        if (ex.isVarArgs()) {
                            throw new AnnotationProcessorException("Only non-varargs method can be annotated with @Downcall");
                        }
                        if (!ex.getThrownTypes().isEmpty()) {
                            throw new AnnotationProcessorException("Only non-thrown method can be annotated with @Downcall");
                        }
                        if (!ex.getTypeParameters().isEmpty()) {
                            throw new AnnotationProcessorException("Only non-type parameters method can be annotated with @Downcall");
                        }
                        d.add(new DowncallData(ex, downcall));
                    }
                }
                FfmData ffmData = new FfmData(t, f, d);
                String implClassName = generateFFMImplSource(ffmData);
                generateFFMProviderSource(ffmData, implClassName);
            } else {
                throw new AnnotationProcessorException("Should never be reached");
            }
        }
    }

    private String generateFFMImplSource(FfmData ffmData) {
        GeneratorSource source = new GeneratorSource(processingEnv, ffmData.typeElement(), "LibImpl");
        String generatedClass = source.className();
        String targetClass = source.register(ffmData.typeElement());
        List<GeneratorBlock> blocks = new ArrayList<>();
        blocks.add(new GeneratorBlock()
                .addLine("public final class " + generatedClass + " implements " + targetClass + " {")
                .indent());
        String atomicBoolean = source.register(AtomicBoolean.class);
        String illegalStateException = source.register(IllegalStateException.class);
        blocks.add(new GeneratorBlock()
                .addLine("private static final " + atomicBoolean + " FFM_PROVIDER_INSTANCE_CREATED = new " + atomicBoolean + "(false);")
                .newLine()
                .addLine("public " + generatedClass + "() {")
                .indent().addLine("if(!FFM_PROVIDER_INSTANCE_CREATED.compareAndSet(false, true)) {")
                .indent().addLine("throw new " + illegalStateException + "(\"" + generatedClass + " instance has already been created\");")
                .unindent().addLine("}").unindent().addLine("}").newLine());
        String memorySegment = source.register(MemorySegment.class);
        for (DowncallData downcallData : ffmData.downcallDataList()) {
            ExecutableElement ex = downcallData.executableElement();
            String methodName = ex.getSimpleName().toString();
            String returnType = castParameterType(memorySegment, ex.getReturnType());
            List<? extends VariableElement> parameters = ex.getParameters();
            String fullParams = parameters.stream().map(v -> castParameterType(memorySegment, v.asType()) + " " + v.getSimpleName()).collect(Collectors.joining(", "));
            String shortParams = parameters.stream().map(v -> v.getSimpleName().toString()).collect(Collectors.joining(", "));
            String p1 = "\"" + ffmData.ffm().libraryName() + "\"";
            String p2 = "\"" + downcallData.downcall().methodName() + "\"";
            String p3;
            String functionDescriptor = source.register(FunctionDescriptor.class);
            String valueLayout = source.register(ValueLayout.class);
            if (returnType.equals("void")) {
                p3 = functionDescriptor + ".ofVoid(" + parameters.stream().map(v -> castValueLayout(valueLayout, v.asType())).collect(Collectors.joining(", ")) + ")";
            } else {
                p3 = functionDescriptor + ".of(" + Stream.concat(Stream.of(ex.getReturnType()), parameters.stream().map(VariableElement::asType)).map(v -> castValueLayout(valueLayout, v)).collect(Collectors.joining(", ")) + ")";
            }
            String mh = String.join(", ", p1, p2, p3, downcallData.downcall().critical() ? "true" : "false");
            String methodHandle = source.register(MethodHandle.class);
            String sharedLibs = source.register(SharedLibs.class);
            GeneratorBlock b = new GeneratorBlock().addLine("@Override").addLine("public " + returnType + " " + methodName + "(" + fullParams + ") {")
                    .indent().addLine("class Holder {").indent()
                    .addLine("static final " + methodHandle + " MH = " + sharedLibs + ".getMethodHandleFromLib(" + mh + ");");
            String runtimeException = source.register(RuntimeException.class);
            if (downcallData.downcall().constant()) {
                if (!parameters.isEmpty()) {
                    throw new AnnotationProcessorException("Constant function can not have parameters");
                }
                if (returnType.equals("void")) {
                    throw new AnnotationProcessorException("Constant function can not have void as its return type");
                }
                b.addLine("static final " + returnType + " CACHED;").addLine("static {").indent()
                        .addLine("try {").indent().addLine("CACHED = (" + returnType + ") MH.invokeExact(" + shortParams + ");").unindent().addLine("} catch (Throwable t) {")
                        .indent().addLine("throw new " + runtimeException + "(\"Failed to invoke " + methodName + " method\", t);").unindent().addLine("}")
                        .unindent().addLine("}").unindent().addLine("}").addLine("return Holder.CACHED;").unindent().addLine("}").newLine();
            } else {
                String invokeStatement = "Holder.MH.invokeExact(" + shortParams + ");";
                if (!returnType.equals("void")) {
                    invokeStatement = "return (" + returnType + ") " + invokeStatement;
                }
                b.unindent().addLine("}").addLine("try {").indent().addLine(invokeStatement)
                        .unindent().addLine("} catch (Throwable t) {").indent().addLine("throw new " + runtimeException + "(\"Failed to invoke " + methodName + " method\", t);")
                        .unindent().addLine("}").unindent().addLine("}").newLine();
            }
            blocks.add(b);
        }
        blocks.add(new GeneratorBlock().unindent().addLine("}").newLine());
        source.addBlocks(blocks);
        source.writeToFiler();
        return generatedClass;
    }

    private String castParameterType(String memorySegment, TypeMirror type) {
        return switch (type.getKind()) {
            case VOID -> "void";
            case BYTE -> "byte";
            case CHAR -> "char";
            case SHORT -> "short";
            case INT -> "int";
            case LONG -> "long";
            case FLOAT -> "float";
            case DOUBLE -> "double";
            case DECLARED -> {
                if (processingEnv.getTypeUtils().isSameType(type, memorySegmentType)) {
                    yield memorySegment;
                }
                throw new UnsupportedOperationException("Unsupported type: " + type);
            }
            default -> throw new UnsupportedOperationException("Unsupported type: " + type);
        };
    }

    private String castValueLayout(String valueLayout, TypeMirror type) {
        return switch (type.getKind()) {
            case BYTE -> valueLayout + ".JAVA_BYTE";
            case CHAR -> valueLayout + ".JAVA_CHAR";
            case SHORT -> valueLayout + ".JAVA_SHORT";
            case INT -> valueLayout + ".JAVA_INT";
            case LONG -> valueLayout + ".JAVA_LONG";
            case FLOAT -> valueLayout + ".JAVA_FLOAT";
            case DOUBLE -> valueLayout + ".JAVA_DOUBLE";
            case DECLARED -> {
                if (processingEnv.getTypeUtils().isSameType(type, memorySegmentType)) {
                    yield valueLayout + ".ADDRESS";
                }
                throw new UnsupportedOperationException("Unsupported type: " + type);
            }
            default -> throw new UnsupportedOperationException("Unsupported type: " + type);
        };
    }

    private void generateFFMProviderSource(FfmData ffmData, String implClassName) {
        GeneratorSource source = new GeneratorSource(processingEnv, ffmData.typeElement(), "LibProvider");
        String provider = source.register(Provider.class);
        String targetClass = source.register(ffmData.typeElement());
        String generatedClass = source.className();
        String sharedLib = source.register(SharedLib.class);
        List<GeneratorBlock> blocks = new ArrayList<>();
        blocks.add(new GeneratorBlock()
                .addLine("@" + provider + "(target = " + targetClass + ".class)")
                .addLine("public final class " + generatedClass + " implements " + sharedLib + " {")
                .indent().newLine());
        String atomicBoolean = source.register(AtomicBoolean.class);
        String illegalStateException = source.register(IllegalStateException.class);
        blocks.add(new GeneratorBlock()
                .addLine("private static final " + atomicBoolean + " FFM_PROVIDER_INSTANCE_CREATED = new " + atomicBoolean + "(false);")
                .newLine()
                .addLine("public " + generatedClass + "() {")
                .indent().addLine("if(!FFM_PROVIDER_INSTANCE_CREATED.compareAndSet(false, true)) {")
                .indent().addLine("throw new " + illegalStateException + "(\"" + generatedClass + " instance has already been created\");")
                .unindent().addLine("}").unindent().addLine("}").newLine());
        blocks.add(new GeneratorBlock()
                .addLine("@Override")
                .addLine("public Class<?> target() {")
                .indent().addLine("return " + targetClass + ".class;")
                .unindent().addLine("}").newLine());
        String list = source.register(List.class);
        String os = source.register(Os.class);
        blocks.add(new GeneratorBlock()
                .addLine("@Override")
                .addLine("public " + list + "<" + os + "> supportedOS() {")
                .indent().addLine("return " + list + ".of(" + Arrays.stream(ffmData.ffm().supportedOS()).map(o -> os + "." + o.name()).collect(Collectors.joining(", ")) + ");")
                .unindent().addLine("}").newLine());
        blocks.add(new GeneratorBlock().addLine("@Override")
                .addLine("public String libName() {")
                .indent().addLine("return \"" + ffmData.ffm().libraryName() + "\";")
                .unindent().addLine("}").newLine());
        GeneratorBlock b = new GeneratorBlock().addLine("@Override").addLine("public List<String> methodNames() {")
                .indent().addLine("return " + list + ".of(").indent();
        Iterator<DowncallData> iter = ffmData.downcallDataList().iterator();
        while (iter.hasNext()) {
            DowncallData downcallData = iter.next();
            String funcName = "\"" + downcallData.executableElement().getSimpleName() + "\"";
            b.addLine(iter.hasNext() ? funcName + ", " : funcName);
        }
        b.unindent().addLine(");").unindent().addLine("}").newLine();
        blocks.add(b);
        String supplier = source.register(Supplier.class);
        blocks.add(new GeneratorBlock().addLine("@Override").addLine("public " + supplier + "<?> supplier() {").indent()
                .addLine("class Holder {").indent().addLine("static final " + targetClass + " INSTANCE = new " + implClassName + "();")
                .unindent().addLine("}").addLine("return () -> Holder.INSTANCE;").unindent().addLine("}").newLine());
        blocks.add(new GeneratorBlock().unindent().addLine("}").newLine());
        source.addBlocks(blocks);
        source.writeToFiler();
    }

}
