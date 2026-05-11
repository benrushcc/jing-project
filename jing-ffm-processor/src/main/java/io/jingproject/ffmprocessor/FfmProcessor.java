package io.jingproject.ffmprocessor;

import io.jingproject.common.Os;
import io.jingproject.common.anno.Provider;
import io.jingproject.commonprocess.AnnoUtil;
import io.jingproject.commonprocess.AnnotationProcessorException;
import io.jingproject.commonprocess.GeneratorBlock;
import io.jingproject.commonprocess.GeneratorSource;
import io.jingproject.ffm.*;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class FfmProcessor extends AbstractProcessor {

    private TypeMirror memorySegmentType;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        memorySegmentType = processingEnv.getElementUtils()
                .getTypeElement(MemorySegment.class.getCanonicalName()).asType();
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
                TypeElement t = AnnoUtil.castTypeElement(e);
                checkFfmElement(t);
                FfmProcessorInfo ffmProcessorInfo = createFfmInfo(t);
                GeneratorSource implSource = new GeneratorSource(ffmProcessorInfo.element(), "LibImpl");
                GeneratorSource facadeSource = new GeneratorSource(ffmProcessorInfo.element(), "LibFacade");
                writeFfmImplSource(implSource, ffmProcessorInfo);
                writeFfmFacadeSource(facadeSource, implSource, ffmProcessorInfo);
            }
        }
        return true;
    }

    private void checkFfmElement(TypeElement t) {
        // check registration
        AnnoUtil.checkTypeElementForRegister(t);
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
                ExecutableElement ex = AnnoUtil.castExecutableElement(el);
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
            }
        }
    }

    private FfmProcessorInfo createFfmInfo(TypeElement t) {
        FFM ffm = Objects.requireNonNull(t.getAnnotation(FFM.class));
        List<FfmDowncallInfo> ffmDowncallInfos = new ArrayList<>();
        int index = 0;
        for (Element el : t.getEnclosedElements()) {
            if (el.getKind() == ElementKind.METHOD) {
                ExecutableElement ex = AnnoUtil.castExecutableElement(el);
                Set<Modifier> modifiers = ex.getModifiers();
                if (modifiers.contains(Modifier.DEFAULT) || modifiers.contains(Modifier.STATIC) || modifiers.contains(Modifier.PRIVATE)) {
                    continue;
                }
                Downcall dc = Objects.requireNonNull(ex.getAnnotation(Downcall.class));
                ffmDowncallInfos.add(new FfmDowncallInfo(index, ex, dc.methodName(), dc.constant(), dc.critical()));
                index = Math.incrementExact(index);
            }
        }
        return new FfmProcessorInfo(t, ffm.libraryName(), Arrays.stream(ffm.supportedOS()).toList(), List.copyOf(ffmDowncallInfos));
    }

    private void writeFfmImplSource(GeneratorSource implSource, FfmProcessorInfo ffmProcessorInfo) {
        List<GeneratorBlock> bs = new ArrayList<>();
        GeneratorBlock b = new GeneratorBlock();
        bs.add(b);
        String implClassName = implSource.className();
        String targetClassName = implSource.register(ffmProcessorInfo.element());
        String atomicBooleanClassName = implSource.register(AtomicBoolean.class);
        String listClassName = implSource.register(List.class);
        String methodHandleClassName = implSource.register(MethodHandle.class);
        String illegalStateExceptionClassName = implSource.register(IllegalStateException.class);
        String libsClassName = implSource.register(Libs.class);
        String assertionErrorClassName = implSource.register(AssertionError.class);
        String overrideClassName = implSource.register(Override.class);
        String throwableClassName = implSource.register(Throwable.class);
        String foreignExceptionClassName = implSource.register(ForeignException.class);
        b.addLine("public final class " + implClassName + " implements " + targetClassName + " {")
                .indent()
                .addLine("private static final " + atomicBooleanClassName + " GUARD = new " + atomicBooleanClassName + "(false);")
                .addLine("private static final " + listClassName + "<" + methodHandleClassName + "> MHS = " + listClassName + ".ofLazy(" + ffmProcessorInfo.ffmDowncallInfos().size() + ", " + implClassName + "::makeMHS);")
                .newLine()
                .addLine("public " + implClassName + "() {")
                .indent()
                .addLine("if(!GUARD.compareAndSet(false, true)) {")
                .indent()
                .addLine("throw new " + illegalStateExceptionClassName + "();")
                .unindent()
                .addLine("}")
                .unindent()
                .addLine("}")
                .newLine()
                .addLine("private static " + methodHandleClassName + " makeMHS(int index) {")
                .indent()
                .addLine("return switch (index) {")
                .indent();
        for (FfmDowncallInfo ffmDowncallInfo : ffmProcessorInfo.ffmDowncallInfos()) {
            ExecutableElement ex = ffmDowncallInfo.element();
            List<String> types = new ArrayList<>();
            types.add(castFfmReturnType(implSource, ex.getReturnType()));
            for (VariableElement v : ex.getParameters()) {
                types.add(castFfmParameterType(implSource, v.asType()));
            }
            b.addLine("case " + ffmDowncallInfo.index() + " -> " + libsClassName +
                    (ffmProcessorInfo.libraryName().equals(FFM.VM) ? ".mhFromVM(" : ".mhFromLib(" + targetClassName + ".class, ") +
                    "\"" + ffmDowncallInfo.methodName() + "\", " + listClassName + ".of(" +
                    types.stream().map(s -> s + ".class").collect(Collectors.joining(", "))
                    + "), " + ffmDowncallInfo.critical() + ", " + ffmDowncallInfo.constant() + ");");
            bs.add(new GeneratorBlock().addLine("@" + overrideClassName)
                    .addLine("public " + types.getFirst() + " " + ex.getSimpleName() + "(" +
                            IntStream.range(1, types.size()).mapToObj(i -> types.get(i) + " p" + i).collect(Collectors.joining(", ")) + ") {")
                    .indent().addLine("try {").indent()
                    .addLine(("void".equals(types.getFirst()) ? "" : "return (" + types.getFirst() + ") ") +
                            "MHS.get(" + ffmDowncallInfo.index() + ").invokeExact(" +
                            IntStream.range(1, types.size()).mapToObj(i -> "p" + i).collect(Collectors.joining(", ")) + ");")
                    .unindent().addLine("} catch (" + throwableClassName + " t) {")
                    .indent().addLine("throw new " + foreignExceptionClassName + "(\"Failed to invoke " + ffmDowncallInfo.methodName() + " native method\", t);")
                    .unindent().addLine("}").unindent().addLine("}").newLine());
        }
        b.addLine("default -> throw new " + assertionErrorClassName + "();")
                .unindent()
                .addLine("};")
                .unindent()
                .addLine("}")
                .newLine();
        bs.add(new GeneratorBlock().unindent().addLine("}").newLine());
        implSource.addBlocks(bs);
        implSource.writeToFiler(processingEnv);
    }

    private String castFfmReturnType(GeneratorSource source, TypeMirror tm) {
        return switch (tm.getKind()) {
            case VOID -> "void";
            case BYTE -> "byte";
            case CHAR -> "char";
            case SHORT -> "short";
            case INT -> "int";
            case LONG -> "long";
            case FLOAT -> "float";
            case DOUBLE -> "double";
            case DECLARED -> {
                TypeMirror targetTm = Objects.requireNonNull(memorySegmentType);
                if (processingEnv.getTypeUtils().isSameType(tm, targetTm)) {
                    yield source.register(MemorySegment.class);
                }
                throw new UnsupportedOperationException("unsupported declared return type: " + tm);
            }
            default -> throw new UnsupportedOperationException("unsupported return type: " + tm);
        };
    }

    private String castFfmParameterType(GeneratorSource source, TypeMirror tm) {
        return switch (tm.getKind()) {
            case BYTE -> "byte";
            case CHAR -> "char";
            case SHORT -> "short";
            case INT -> "int";
            case LONG -> "long";
            case FLOAT -> "float";
            case DOUBLE -> "double";
            case DECLARED -> {
                TypeMirror targetTm = Objects.requireNonNull(memorySegmentType);
                if (processingEnv.getTypeUtils().isSameType(tm, targetTm)) {
                    yield source.register(MemorySegment.class);
                }
                throw new UnsupportedOperationException("unsupported declared parameter type: " + tm);
            }
            default -> throw new UnsupportedOperationException("unsupported parameter type: " + tm);
        };
    }

    private void writeFfmFacadeSource(GeneratorSource facadeSource, GeneratorSource implSource, FfmProcessorInfo ffmProcessorInfo) {
        List<GeneratorBlock> bs = new ArrayList<>();
        String providerClassName = facadeSource.register(Provider.class);
        String facadeSourceClassName = facadeSource.className();
        String libFacadeClassName = facadeSource.register(LibFacade.class);
        String targetClassName = facadeSource.register(ffmProcessorInfo.element());
        String atomicBooleanClassName = facadeSource.register(AtomicBoolean.class);
        String illegalStateExceptionClassName = facadeSource.register(IllegalStateException.class);
        String overrideClassName = facadeSource.register(Override.class);
        String listClassName = facadeSource.register(List.class);
        String osClassName = facadeSource.register(Os.class);
        String classClassName = facadeSource.register(Class.class);
        String stringClassName = facadeSource.register(String.class);
        String implSourceClassName = facadeSource.register(implSource);
        String supplier = facadeSource.register(Supplier.class);
        bs.add(new GeneratorBlock()
                .addLine("@" + providerClassName + "(target = " + libFacadeClassName + ".class)")
                .addLine("public final class " + facadeSourceClassName + " implements " + libFacadeClassName + " {")
                .indent().newLine());
        bs.add(new GeneratorBlock()
                .addLine("private static final " + atomicBooleanClassName + " GUARD = new " + atomicBooleanClassName + "(false);")
                .newLine()
                .addLine("public " + facadeSourceClassName + "() {")
                .indent().addLine("if(!GUARD.compareAndSet(false, true)) {")
                .indent().addLine("throw new " + illegalStateExceptionClassName + "();")
                .unindent().addLine("}").unindent().addLine("}").newLine());
        bs.add(new GeneratorBlock()
                .addLine("@" + overrideClassName)
                .addLine("public " + classClassName + "<?> target() {")
                .indent().addLine("return " + targetClassName + ".class;")
                .unindent().addLine("}").newLine());
        bs.add(new GeneratorBlock()
                .addLine("@" + overrideClassName)
                .addLine("public " + listClassName + "<" + osClassName + "> supportedOS() {")
                .indent().addLine("return " + listClassName + ".of(" + ffmProcessorInfo.supportedOS().stream().map(o -> osClassName + "." + o.name()).collect(Collectors.joining(", ")) + ");")
                .unindent().addLine("}").newLine());
        bs.add(new GeneratorBlock().addLine("@" + overrideClassName)
                .addLine("public " + stringClassName + " libName() {")
                .indent().addLine("return \"" + ffmProcessorInfo.libraryName() + "\";")
                .unindent().addLine("}").newLine());
        bs.add(new GeneratorBlock().addLine("@" + overrideClassName)
                .addLine("public " + listClassName + "<" + stringClassName + "> methodNames() {")
                .indent().addLine("return " + listClassName + ".of(")
                .addLine(ffmProcessorInfo.ffmDowncallInfos().stream().map(d -> "\"" + d.methodName() + "\"").collect(Collectors.joining(", ")))
                .addLine(");").unindent().addLine("}").newLine());

        bs.add(new GeneratorBlock().addLine("@" + overrideClassName)
                .addLine("public " + supplier + "<?> supplier() {").indent()
                .addLine("return " + implSourceClassName + "::new;")
                .unindent().addLine("}").newLine());
        bs.add(new GeneratorBlock().unindent().addLine("}").newLine());
        facadeSource.addBlocks(bs);
        facadeSource.writeToFiler(processingEnv);
    }

}
