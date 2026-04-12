package io.jingproject.ffmprocessor;

import io.jingproject.commonprocess.AnnotationProcessorException;
import io.jingproject.commonprocess.GeneratorBlock;
import io.jingproject.commonprocess.GeneratorSource;
import io.jingproject.common.Os;
import io.jingproject.common.anno.Provider;
import io.jingproject.ffm.Downcall;
import io.jingproject.ffm.FFM;
import io.jingproject.ffm.LibFacade;
import io.jingproject.ffm.Libs;

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
            for (Element e : roundEnv.getElementsAnnotatedWith(FFM.class)) {
                if(e instanceof TypeElement t) {
                    checkFfmElement(t);
                    FfmInfo ffmInfo = createFfmInfo(t);
                    GeneratorSource implSource = new GeneratorSource(processingEnv, ffmInfo.element(), "LibImpl");
                    GeneratorSource facadeSource = new GeneratorSource(processingEnv, ffmInfo.element(), "LibFacade");
                    writeFfmImplSource(implSource, ffmInfo);
                    writeFfmFacadeSource(facadeSource, implSource, ffmInfo);
                } else {
                    throw new AnnotationProcessorException("only typeElement could be annotated with @FFM");
                }
            }
        }
        return true;
    }

    private void checkFfmElement(TypeElement t) {
        // must be top level
        if (t.getNestingKind() != NestingKind.TOP_LEVEL) {
            throw new AnnotationProcessorException("only top level element can be annotated with @FFM");
        }
        // must be interface
        if (t.getKind() != ElementKind.INTERFACE) {
            throw new AnnotationProcessorException("only interface element can be annotated with @FFM");
        }
        // must be public
        if (!t.getModifiers().contains(Modifier.PUBLIC)) {
            throw new AnnotationProcessorException("only public interface can be annotated with @FFM");
        }
        // must be non-sealed
        if (t.getModifiers().contains(Modifier.SEALED)) {
            throw new AnnotationProcessorException("only non-sealed interface can be annotated with @FFM");
        }
        // check downcall information
        for (Element el : t.getEnclosedElements()) {
            if (el.getKind() == ElementKind.METHOD) {
                if(el instanceof ExecutableElement ex) {
                    // skip default, static or private methods
                    Set<Modifier> modifiers = ex.getModifiers();
                    if (modifiers.contains(Modifier.DEFAULT) || modifiers.contains(Modifier.STATIC) || modifiers.contains(Modifier.PRIVATE)) {
                        continue;
                    }
                    Downcall downcall = ex.getAnnotation(Downcall.class);
                    // downcall annotation must not be null
                    if (downcall == null) {
                        throw new AnnotationProcessorException("method must have @Downcall annotation");
                    }
                    // downcall method must not be var-args
                    if (ex.isVarArgs()) {
                        throw new AnnotationProcessorException("only non-varargs method can be annotated with @Downcall");
                    }
                    // downcall method must not throw exceptions
                    if (!ex.getThrownTypes().isEmpty()) {
                        throw new AnnotationProcessorException("only non-thrown method can be annotated with @Downcall");
                    }
                    // downcall method cannot have type parameters
                    if (!ex.getTypeParameters().isEmpty()) {
                        throw new AnnotationProcessorException("Only non-type parameters method can be annotated with @Downcall");
                    }
                } else {
                    throw new AssertionError();
                }
            }
        }
    }

    private FfmInfo createFfmInfo(TypeElement t) {
        FFM ffm = Objects.requireNonNull(t.getAnnotation(FFM.class));
        List<DowncallInfo> downcallInfos = new ArrayList<>();
        for (Element el : t.getEnclosedElements()) {
            if (el.getKind() == ElementKind.METHOD) {
                if(el instanceof ExecutableElement ex) {
                    Set<Modifier> modifiers = ex.getModifiers();
                    if (modifiers.contains(Modifier.DEFAULT) || modifiers.contains(Modifier.STATIC) || modifiers.contains(Modifier.PRIVATE)) {
                        continue;
                    }
                    Downcall dc = Objects.requireNonNull(ex.getAnnotation(Downcall.class));
                    downcallInfos.add(new DowncallInfo(ex, dc.methodName(), dc.constant(), dc.critical()));
                } else {
                    throw new AssertionError();
                }
            }
        }
        return new FfmInfo(t, ffm.libraryName(), Arrays.stream(ffm.supportedOS()).toList(), List.copyOf(downcallInfos));
    }

    private void writeFfmImplSource(GeneratorSource implSource, FfmInfo ffmInfo) {
        String implClassName = implSource.className();
        String targetClassName = implSource.register(ffmInfo.element());
        List<GeneratorBlock> blocks = new ArrayList<>();
        blocks.add(new GeneratorBlock()
                .addLine("public final class " + implClassName + " implements " + targetClassName + " {")
                .indent());
        String atomicBoolean = implSource.register(AtomicBoolean.class);
        String illegalStateException = implSource.register(IllegalStateException.class);
        blocks.add(new GeneratorBlock()
                .addLine("private static final " + atomicBoolean + " FFM_IMPL_INSTANCE_CREATED = new " + atomicBoolean + "(false);")
                .newLine()
                .addLine("public " + implClassName + "() {")
                .indent().addLine("if(!FFM_IMPL_INSTANCE_CREATED.compareAndSet(false, true)) {")
                .indent().addLine("throw new " + illegalStateException + "(\"" + implClassName + " instance has already been created\");")
                .unindent().addLine("}").unindent().addLine("}").newLine());
        String memorySegment = implSource.register(MemorySegment.class);
        for (DowncallInfo downcallInfo : ffmInfo.downcallInfos()) {
            ExecutableElement ex = downcallInfo.element();
            String methodName = ex.getSimpleName().toString();
            String returnType = castParameterType(memorySegment, ex.getReturnType());
            List<? extends VariableElement> parameters = ex.getParameters();
            String fullParams = parameters.stream().map(v -> castParameterType(memorySegment, v.asType()) + " " + v.getSimpleName()).collect(Collectors.joining(", "));
            String shortParams = parameters.stream().map(v -> v.getSimpleName().toString()).collect(Collectors.joining(", "));
            String p1 = "\"" + ffmInfo.libraryName() + "\"";
            String p2 = "\"" + downcallInfo.methodName() + "\"";
            String p3;
            String functionDescriptor = implSource.register(FunctionDescriptor.class);
            String valueLayout = implSource.register(ValueLayout.class);
            if (returnType.equals("void")) {
                p3 = functionDescriptor + ".ofVoid(" + parameters.stream().map(v -> castValueLayout(valueLayout, v.asType())).collect(Collectors.joining(", ")) + ")";
            } else {
                p3 = functionDescriptor + ".of(" + Stream.concat(Stream.of(ex.getReturnType()), parameters.stream().map(VariableElement::asType)).map(v -> castValueLayout(valueLayout, v)).collect(Collectors.joining(", ")) + ")";
            }
            String mh = String.join(", ", p1, p2, p3, Boolean.toString(downcallInfo.critical()));
            String methodHandle = implSource.register(MethodHandle.class);
            String libs = implSource.register(Libs.class);
            GeneratorBlock b = new GeneratorBlock().addLine("@Override").addLine("public " + returnType + " " + methodName + "(" + fullParams + ") {")
                    .indent().addLine("class Holder {").indent()
                    .addLine("static final " + methodHandle + " MH = " + libs + ".getMethodHandleFromLib(" + mh + ");");
            String runtimeException = implSource.register(RuntimeException.class);
            if (downcallInfo.constant()) {
                if (!parameters.isEmpty()) {
                    throw new AnnotationProcessorException("Constant function can not have parameters");
                }
                if (returnType.equals("void")) {
                    throw new AnnotationProcessorException("Constant function can not have void as its return type");
                }
                b.addLine("static final " + returnType + " CACHED;").addLine("static {").indent()
                        .addLine("try {").indent().addLine("CACHED = (" + returnType + ") MH.invokeExact(" + shortParams + ");").unindent().addLine("} catch (Throwable t) {")
                        .indent().addLine("throw new " + runtimeException + "(\"failed to invoke " + methodName + " method\", t);").unindent().addLine("}")
                        .unindent().addLine("}").unindent().addLine("}").addLine("return Holder.CACHED;").unindent().addLine("}").newLine();
            } else {
                String invokeStatement = "Holder.MH.invokeExact(" + shortParams + ");";
                if (!returnType.equals("void")) {
                    invokeStatement = "return (" + returnType + ") " + invokeStatement;
                }
                b.unindent().addLine("}").addLine("try {").indent().addLine(invokeStatement)
                        .unindent().addLine("} catch (Throwable t) {").indent().addLine("throw new " + runtimeException + "(\"failed to invoke " + methodName + " method\", t);")
                        .unindent().addLine("}").unindent().addLine("}").newLine();
            }
            blocks.add(b);
        }
        blocks.add(new GeneratorBlock().unindent().addLine("}").newLine());
        implSource.addBlocks(blocks);
        implSource.writeToFiler();
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
                throw new UnsupportedOperationException("Unsupported declared type: " + type);
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
                throw new UnsupportedOperationException("Unsupported declared type: " + type);
            }
            default -> throw new UnsupportedOperationException("Unsupported type: " + type);
        };
    }

    private void writeFfmFacadeSource(GeneratorSource facadeSource, GeneratorSource implSource, FfmInfo ffmInfo) {
        String implClassName = facadeSource.register(implSource);
        String provider = facadeSource.register(Provider.class);
        String targetClass = facadeSource.register(ffmInfo.element());
        String generatedClass = facadeSource.className();
        String libFacade = facadeSource.register(LibFacade.class);
        List<GeneratorBlock> blocks = new ArrayList<>();
        blocks.add(new GeneratorBlock()
                .addLine("@" + provider + "(target = " + targetClass + ".class)")
                .addLine("public final class " + generatedClass + " implements " + libFacade + " {")
                .indent().newLine());
        String atomicBoolean = facadeSource.register(AtomicBoolean.class);
        String illegalStateException = facadeSource.register(IllegalStateException.class);
        blocks.add(new GeneratorBlock()
                .addLine("private static final " + atomicBoolean + " FFM_FACADE_INSTANCE_CREATED = new " + atomicBoolean + "(false);")
                .newLine()
                .addLine("public " + generatedClass + "() {")
                .indent().addLine("if(!FFM_FACADE_INSTANCE_CREATED.compareAndSet(false, true)) {")
                .indent().addLine("throw new " + illegalStateException + "(\"" + generatedClass + " instance has already been created\");")
                .unindent().addLine("}").unindent().addLine("}").newLine());
        blocks.add(new GeneratorBlock()
                .addLine("@Override")
                .addLine("public Class<?> target() {")
                .indent().addLine("return " + targetClass + ".class;")
                .unindent().addLine("}").newLine());
        String list = facadeSource.register(List.class);
        String os = facadeSource.register(Os.class);
        blocks.add(new GeneratorBlock()
                .addLine("@Override")
                .addLine("public " + list + "<" + os + "> supportedOS() {")
                .indent().addLine("return " + list + ".of(" + ffmInfo.supportedOS().stream().map(o -> os + "." + o.name()).collect(Collectors.joining(", ")) + ");")
                .unindent().addLine("}").newLine());
        blocks.add(new GeneratorBlock().addLine("@Override")
                .addLine("public String libName() {")
                .indent().addLine("return \"" + ffmInfo.libraryName() + "\";")
                .unindent().addLine("}").newLine());
        blocks.add(new GeneratorBlock().addLine("@Override").addLine("public List<String> methodNames() {")
                .indent().addLine("return " + list + ".of(")
                .addLine(ffmInfo.downcallInfos().stream().map(d -> "\"" + d.element().getSimpleName() + "\"").collect(Collectors.joining(", ")))
                .addLine(");").unindent().addLine("}").newLine());
        String supplier = facadeSource.register(Supplier.class);
        blocks.add(new GeneratorBlock().addLine("@Override").addLine("public " + supplier + "<?> supplier() {").indent()
                .addLine("class Holder {").indent().addLine("static final " + targetClass + " INSTANCE = new " + implClassName + "();")
                .unindent().addLine("}").addLine("return () -> Holder.INSTANCE;").unindent().addLine("}").newLine());
        blocks.add(new GeneratorBlock().unindent().addLine("}").newLine());
        facadeSource.addBlocks(blocks);
        facadeSource.writeToFiler();
    }

}
