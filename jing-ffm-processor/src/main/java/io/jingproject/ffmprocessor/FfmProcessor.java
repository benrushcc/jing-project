package io.jingproject.ffmprocessor;

import io.jingproject.annprocess.AnnotationProcessorException;
import io.jingproject.annprocess.GeneratorBlock;
import io.jingproject.annprocess.GeneratorSource;
import io.jingproject.annprocess.Provider;
import io.jingproject.ffm.*;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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

    record FfmData(TypeElement typeElement, FFM ffm, List<DowncallData> downcallDataList) {

    }

    record DowncallData(ExecutableElement executableElement, Downcall downcall) {

    }

    private void processFFM(RoundEnvironment roundEnv) {
        for (Element element : roundEnv.getElementsAnnotatedWith(FFM.class)) {
            if(element instanceof TypeElement t) {
                if(t.getNestingKind() != NestingKind.TOP_LEVEL) {
                    throw new AnnotationProcessorException("Only top level element can be annotated with @FFM");
                }
                if(t.getKind() != ElementKind.INTERFACE) {
                    throw new AnnotationProcessorException("Only interface element can be annotated with @FFM");
                }
                if(!t.getModifiers().contains(Modifier.PUBLIC)) {
                    throw new AnnotationProcessorException("Only public interface can be annotated with @FFM");
                }
                if(t.getModifiers().contains(Modifier.SEALED)) {
                    throw new AnnotationProcessorException("Only non-sealed interface can be annotated with @FFM");
                }
                FFM f = t.getAnnotation(FFM.class);
                List<DowncallData> d = new ArrayList<>();
                for (Element el : t.getEnclosedElements()) {
                    if(el.getKind() == ElementKind.METHOD && el instanceof ExecutableElement ex) {
                        Set<Modifier> modifiers = ex.getModifiers();
                        if(modifiers.contains(Modifier.DEFAULT) || modifiers.contains(Modifier.STATIC) || modifiers.contains(Modifier.PRIVATE)) {
                            continue ;
                        }
                        if(ex.isVarArgs()) {
                            throw new AnnotationProcessorException("Only non-varargs method can be annotated with @FFM");
                        }
                        if(!ex.getThrownTypes().isEmpty()) {
                            throw new AnnotationProcessorException("Only non-thrown method can be annotated with @FFM");
                        }
                        if(!ex.getTypeParameters().isEmpty()) {
                            throw new AnnotationProcessorException("Only non-type parameters method can be annotated with @FFM");
                        }
                        Downcall downcall = ex.getAnnotation(Downcall.class);
                        if(downcall == null) {
                            throw new AnnotationProcessorException("Method must have downcall annotation");
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
        String sharedLibs = source.register(SharedLibs.class);
        String functionDescriptor = source.register(FunctionDescriptor.class);
        String linker = source.register(Linker.class);
        String memorySegment = source.register(MemorySegment.class);
        String valueLayout = source.register(ValueLayout.class);
        String methodHandle = source.register(MethodHandle.class);
        String targetClass = source.register(ffmData.typeElement());
        String generatedClass = source.className();
        List<GeneratorBlock> blocks = new ArrayList<>();
        blocks.add(new GeneratorBlock().addLine("public final class " + generatedClass + " implements " + targetClass + " {").indent());
        for (DowncallData downcallData : ffmData.downcallDataList()) {
            ExecutableElement ex = downcallData.executableElement();
            String methodName = ex.getSimpleName().toString();
            String returnType = castParameterType(memorySegment, ex.getReturnType());
            List<? extends VariableElement> parameters = ex.getParameters();
            String fullParams = parameters.stream().map(v -> castParameterType(memorySegment, v.asType()) + " " + v.getSimpleName().toString()).collect(Collectors.joining(", "));
            String shortParams = parameters.stream().map(v -> v.getSimpleName().toString()).collect(Collectors.joining(", "));
            String p1 = "\"" + ffmData.ffm().libraryName() + "\"";
            String p2 = "\"" + downcallData.downcall().methodName() + "\"";
            String p3;
            if(returnType.equals("void")) {
                p3 = functionDescriptor + ".ofVoid(" + parameters.stream().map(v -> castValueLayout(valueLayout, v.asType())).collect(Collectors.joining(", ")) + ")";
            } else {
                p3 = functionDescriptor + ".of(" + Stream.concat(Stream.of(ex.getReturnType()), parameters.stream().map(VariableElement::asType)).map(v -> castValueLayout(valueLayout, v)).collect(Collectors.joining(", "))+ ")";
            }
            String mh = String.join(", ", p1, p2, p3, downcallData.downcall().critical() ? "true" : "false");
            GeneratorBlock b = new GeneratorBlock().addLine("@Override").addLine("public " + returnType + " " +  methodName + "(" + fullParams + ") {")
                    .indent().addLine("class Holder {").indent()
                    .addLine("static final " + methodHandle + " MH = " + sharedLibs + ".getMethodHandleFromLib(" + mh + ");");
            if(downcallData.downcall().constant()) {
                if(!parameters.isEmpty()) {
                    throw new AnnotationProcessorException("Constant function could not have parameters");
                }
                if(returnType.equals("void")) {
                    throw new AnnotationProcessorException("Constant function could never return void");
                }
                b.addLine("static final " + returnType + " CACHED;").addLine("static {").indent()
                    .addLine("try {").indent().addLine("CACHED = (" + returnType + ") MH.invokeExact(" + shortParams + ");").unindent().addLine("} catch (Throwable t) {")
                    .indent().addLine("throw new RuntimeException(\"Failed to invoke " + methodName + " method\", t);").unindent().addLine("}")
                    .unindent().addLine("}").unindent().addLine("}").addLine("return Holder.CACHED;").unindent().addLine("}").newLine();
            } else {
                String invokeStatement = "Holder.MH.invokeExact(" + shortParams + ");";
                if(!returnType.equals("void")) {
                    invokeStatement = "return (" + returnType + ") " + invokeStatement;
                }
                b.unindent().addLine("}").addLine("try {").indent().addLine(invokeStatement)
                        .unindent().addLine("} catch (Throwable t) {").indent().addLine("throw new RuntimeException(\"Failed to invoke " + methodName + " method\", t);")
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
        String list = source.register(List.class);
        String supplier = source.register(Supplier.class);
        String sharedLib = source.register(SharedLib.class);
        String provider = source.register(Provider.class);
        String targetClass = source.register(ffmData.typeElement());
        String generatedClass = source.className();
        List<GeneratorBlock> blocks = new ArrayList<>();
        blocks.add(new GeneratorBlock().addLine("@" + provider + "(target = " + targetClass + ".class)").addLine("public final class " + generatedClass + " implements " + sharedLib + " {").indent().newLine());
        blocks.add(new GeneratorBlock()
                .addLine("@Override")
                .addLine("public Class<?> target() {")
                .indent().addLine("return " + targetClass + ".class;")
                .unindent().addLine("}").newLine());
        blocks.add(new GeneratorBlock().addLine("@Override")
                .addLine("public String libName() {")
                .indent().addLine("return \"" + ffmData.ffm().libraryName() + "\";")
                .unindent().addLine("}").newLine());
        String names = ffmData.downcallDataList().stream().map(downcallData -> "\"" + downcallData.executableElement().getSimpleName().toString() + "\"").collect(Collectors.joining(", "));
        blocks.add(new GeneratorBlock().addLine("@Override").addLine("public List<String> methodNames() {")
                        .indent().addLine("return " + list + ".of(" + names + ");").unindent().addLine("}").newLine());
        blocks.add(new GeneratorBlock().addLine("@Override").addLine("public " + supplier + "<?> supplier() {").indent()
                        .addLine("class Holder {").indent().addLine("static final " + targetClass + " INSTANCE = new " + implClassName + "();")
                        .unindent().addLine("}").addLine("return () -> Holder.INSTANCE;").unindent().addLine("}").newLine());
        blocks.add(new GeneratorBlock().unindent().addLine("}").newLine());
        source.addBlocks(blocks);
        source.writeToFiler();
    }

}
