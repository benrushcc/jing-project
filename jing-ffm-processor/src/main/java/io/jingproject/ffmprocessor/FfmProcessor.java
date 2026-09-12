package io.jingproject.ffmprocessor;

import io.jingproject.common.Os;
import io.jingproject.common.anno.Generated;
import io.jingproject.common.anno.Provider;
import io.jingproject.commonprocess.AnnoUtil;
import io.jingproject.commonprocess.AnnotationProcessorException;
import io.jingproject.commonprocess.GeneratorBlock;
import io.jingproject.commonprocess.GeneratorSource;
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
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
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
                GeneratorSource facadeSource = new GeneratorSource(ffmProcessorInfo.element(), "LibFacade");
                facadeSource.addBlock(headBlock(facadeSource));
                facadeSource.addBlock(constructorBlock(facadeSource));
                facadeSource.addBlock(targetMethodBlock(facadeSource, ffmProcessorInfo));
                facadeSource.addBlock(supportedOSMethodBlock(facadeSource, ffmProcessorInfo));
                facadeSource.addBlock(libNameMethodBlock(facadeSource, ffmProcessorInfo));
                facadeSource.addBlock(methodNamesMethodBlock(facadeSource, ffmProcessorInfo));
                facadeSource.addBlock(implMethodBlock(facadeSource));
                facadeSource.addBlocks(implClassBlocks(facadeSource, ffmProcessorInfo));
                facadeSource.addBlock(new GeneratorBlock().unindent().addLine("}").newLine());
                facadeSource.writeToFiler(processingEnv);
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
        FFM ffm = t.getAnnotation(FFM.class);
        if (ffm == null) {
            throw new AnnotationProcessorException("@FFM annotation not found");
        }
        List<FfmDowncallInfo> ffmDowncallInfos = new ArrayList<>();
        int index = 0;
        for (Element el : t.getEnclosedElements()) {
            if (el.getKind() == ElementKind.METHOD) {
                ExecutableElement ex = AnnoUtil.castExecutableElement(el);
                Set<Modifier> modifiers = ex.getModifiers();
                if (modifiers.contains(Modifier.DEFAULT) || modifiers.contains(Modifier.STATIC) || modifiers.contains(Modifier.PRIVATE)) {
                    continue;
                }
                Downcall dc = ex.getAnnotation(Downcall.class);
                if (dc == null) {
                    throw new AnnotationProcessorException("@Downcall annotation not found");
                }
                ffmDowncallInfos.add(new FfmDowncallInfo(index, ex, dc.methodName(), dc.constant(), dc.critical()));
                index = Math.incrementExact(index);
            }
        }
        return new FfmProcessorInfo(t, ffm.libraryName(), ffm.supportedOS(), ffmDowncallInfos);
    }

    private GeneratorBlock headBlock(GeneratorSource facadeSource) {
        String providerClassName = facadeSource.register(Provider.class);
        String generatedClassName = facadeSource.register(Generated.class);
        String libFacadeClassName = facadeSource.register(LibFacade.class);
        String facadeClassName = facadeSource.className();
        String atomicBooleanClassName = facadeSource.register(AtomicBoolean.class);
        return new GeneratorBlock()
                .addLine("@" + providerClassName + "(target = " + libFacadeClassName + ".class)")
                .addLine("@" + generatedClassName)
                .addLine("public final class " + facadeClassName + " implements " + libFacadeClassName + " {")
                .indent()
                .addLine("private static final " + atomicBooleanClassName + " GUARD = new " + atomicBooleanClassName + "(false);")
                .addLine("private static final Impl IMPL = new Impl();")
                .newLine();
    }

    private GeneratorBlock constructorBlock(GeneratorSource facadeSource) {
        String illegalStateExceptionClassName = facadeSource.register(IllegalStateException.class);
        return new GeneratorBlock()
                .addLine("public " + facadeSource.className() + "() {")
                .indent()
                .addLine("if(!GUARD.compareAndSet(false, true)) {")
                .indent()
                .addLine("throw new " + illegalStateExceptionClassName + "();")
                .unindent()
                .addLine("}")
                .unindent()
                .addLine("}")
                .newLine();
    }

    private GeneratorBlock targetMethodBlock(GeneratorSource facadeSource, FfmProcessorInfo ffmProcessorInfo) {
        String overrideClassName = facadeSource.register(Override.class);
        String classClassName = facadeSource.register(Class.class);
        String targetClassName = facadeSource.register(ffmProcessorInfo.element());
        return new GeneratorBlock()
                .addLine("@" + overrideClassName)
                .addLine("public " + classClassName + "<?> target() {")
                .indent()
                .addLine("return " + targetClassName + ".class;")
                .unindent()
                .addLine("}")
                .newLine();
    }

    private GeneratorBlock supportedOSMethodBlock(GeneratorSource facadeSource, FfmProcessorInfo ffmProcessorInfo) {
        String overrideClassName = facadeSource.register(Override.class);
        String listClassName = facadeSource.register(List.class);
        String osClassName = facadeSource.register(Os.class);
        return new GeneratorBlock()
                .addLine("@" + overrideClassName)
                .addLine("public " + listClassName + "<" + osClassName + "> supportedOS() {")
                .indent()
                .addLine("return " + listClassName + ".of(" + Arrays.stream(ffmProcessorInfo.supportedOS()).map(o -> osClassName + "." + o.name()).collect(Collectors.joining(", ")) + ");")
                .unindent()
                .addLine("}")
                .newLine();
    }

    private GeneratorBlock libNameMethodBlock(GeneratorSource facadeSource, FfmProcessorInfo ffmProcessorInfo) {
        String overrideClassName = facadeSource.register(Override.class);
        String stringClassName = facadeSource.register(String.class);
        return new GeneratorBlock()
                .addLine("@" + overrideClassName)
                .addLine("public " + stringClassName + " libName() {")
                .indent()
                .addLine("return " + AnnoUtil.escapeJavaStringLiteral(ffmProcessorInfo.libraryName()) + ";")
                .unindent()
                .addLine("}")
                .newLine();
    }

    private GeneratorBlock methodNamesMethodBlock(GeneratorSource facadeSource, FfmProcessorInfo ffmProcessorInfo) {
        String overrideClassName = facadeSource.register(Override.class);
        String listClassName = facadeSource.register(List.class);
        String stringClassName = facadeSource.register(String.class);
        GeneratorBlock b = new GeneratorBlock()
                .addLine("@" + overrideClassName)
                .addLine("public " + listClassName + "<" + stringClassName + "> methodNames() {")
                .indent()
                .addLine("return " + listClassName + ".of(")
                .indent();
        List<FfmDowncallInfo> infos = ffmProcessorInfo.ffmDowncallInfos();
        for (int i = 0; i < infos.size(); i++) {
            String literal = AnnoUtil.escapeJavaStringLiteral(infos.get(i).methodName());
            if (i < infos.size() - 1) {
                b.addLine(literal + ",");
            } else {
                b.addLine(literal);
            }
        }
        return b.unindent()
                .addLine(");")
                .unindent()
                .addLine("}")
                .newLine();
    }

    private GeneratorBlock implMethodBlock(GeneratorSource facadeSource) {
        String overrideClassName = facadeSource.register(Override.class);
        String objectClassName = facadeSource.register(Object.class);
        return new GeneratorBlock()
                .addLine("@" + overrideClassName)
                .addLine("public " + objectClassName + " impl() {")
                .indent()
                .addLine("return IMPL;")
                .unindent()
                .addLine("}")
                .newLine();
    }

    private List<GeneratorBlock> implClassBlocks(GeneratorSource facadeSource, FfmProcessorInfo ffmProcessorInfo) {
        List<GeneratorBlock> r = new ArrayList<>();
        r.add(implClassHeadBlock(facadeSource, ffmProcessorInfo));
        for (FfmDowncallInfo ffmDowncallInfo : ffmProcessorInfo.ffmDowncallInfos()) {
            r.add(downcallMethodBlock(facadeSource, ffmDowncallInfo));
        }
        r.add(new GeneratorBlock()
                .unindent()
                .addLine("}")
                .newLine());
        return r;
    }

    private GeneratorBlock implClassHeadBlock(GeneratorSource facadeSource, FfmProcessorInfo ffmProcessorInfo) {
        String targetClassName = facadeSource.register(ffmProcessorInfo.element());
        String listClassName = facadeSource.register(List.class);
        String methodHandleClassName = facadeSource.register(MethodHandle.class);
        String assertionErrorClassName = facadeSource.register(AssertionError.class);
        GeneratorBlock b = new GeneratorBlock()
                .addLine("private static final class Impl implements " + targetClassName + " {")
                .indent()
                .addLine("private static final " + listClassName + "<" + methodHandleClassName + "> MHS = " + listClassName + ".ofLazy(" + ffmProcessorInfo.ffmDowncallInfos().size() + ", Impl::makeMHS);")
                .newLine()
                .addLine("private static " + methodHandleClassName + " makeMHS(int index) {")
                .indent()
                .addLine("return switch (index) {")
                .indent();
        for (FfmDowncallInfo ffmDowncallInfo : ffmProcessorInfo.ffmDowncallInfos()) {
            b.addLine("case " + ffmDowncallInfo.index() + " -> " +
                    methodHandleFactory(facadeSource, ffmProcessorInfo, ffmDowncallInfo));
        }
        return b.addLine("default -> throw new " + assertionErrorClassName + "();")
                .unindent()
                .addLine("};")
                .unindent()
                .addLine("}")
                .newLine();
    }

    private GeneratorBlock downcallMethodBlock(GeneratorSource facadeSource, FfmDowncallInfo ffmDowncallInfo) {
        ExecutableElement ex = ffmDowncallInfo.element();
        List<String> types = castMethodTypes(facadeSource, ex);
        String overrideClassName = facadeSource.register(Override.class);
        String runtimeExceptionClassName = facadeSource.register(RuntimeException.class);
        String errorClassName = facadeSource.register(Error.class);
        String throwableClassName = facadeSource.register(Throwable.class);
        String undeclaredThrowableExceptionClassName = facadeSource.register(UndeclaredThrowableException.class);
        return new GeneratorBlock()
                .addLine("@" + overrideClassName)
                .addLine("public " + types.getFirst() + " " + ex.getSimpleName() + "(" +
                        IntStream.range(1, types.size()).mapToObj(i -> types.get(i) + " p" + i).collect(Collectors.joining(", ")) + ") {")
                .indent()
                .addLine("try {")
                .indent()
                .addLine(("void".equals(types.getFirst()) ? "" : "return (" + types.getFirst() + ") ") +
                        "MHS.get(" + ffmDowncallInfo.index() + ").invokeExact(" +
                        IntStream.range(1, types.size()).mapToObj(i -> "p" + i).collect(Collectors.joining(", ")) + ");")
                .unindent()
                .addLine("} catch (" + runtimeExceptionClassName + " | " + errorClassName + " e) {")
                .indent()
                .addLine("throw e;")
                .unindent()
                .addLine("} catch (" + throwableClassName + " t) {")
                .indent()
                .addLine("throw new " + undeclaredThrowableExceptionClassName + "(t);")
                .unindent()
                .addLine("}")
                .unindent()
                .addLine("}")
                .newLine();
    }

    private String methodHandleFactory(GeneratorSource facadeSource, FfmProcessorInfo ffmProcessorInfo, FfmDowncallInfo ffmDowncallInfo) {
        String targetClassName = facadeSource.register(ffmProcessorInfo.element());
        String listClassName = facadeSource.register(List.class);
        String libsClassName = facadeSource.register(Libs.class);
        List<String> types = castMethodTypes(facadeSource, ffmDowncallInfo.element());
        return libsClassName +
                (ffmProcessorInfo.libraryName().equals(FFM.VM) ? ".mhFromVM(" : ".mhFromLib(" + targetClassName + ".class, ") +
                AnnoUtil.escapeJavaStringLiteral(ffmDowncallInfo.methodName()) + ", " + listClassName + ".of(" +
                types.stream().map(s -> s + ".class").collect(Collectors.joining(", "))
                + "), " + ffmDowncallInfo.critical() + ", " + ffmDowncallInfo.constant() + ");";
    }

    private List<String> castMethodTypes(GeneratorSource source, ExecutableElement ex) {
        List<String> r = new ArrayList<>();
        r.add(castFfmReturnType(source, ex.getReturnType()));
        for (VariableElement v : ex.getParameters()) {
            r.add(castFfmParameterType(source, v.asType()));
        }
        return r;
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
                if (processingEnv.getTypeUtils().isSameType(tm, memorySegmentType)) {
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
                if (processingEnv.getTypeUtils().isSameType(tm, memorySegmentType)) {
                    yield source.register(MemorySegment.class);
                }
                throw new UnsupportedOperationException("unsupported declared parameter type: " + tm);
            }
            default -> throw new UnsupportedOperationException("unsupported parameter type: " + tm);
        };
    }
}
