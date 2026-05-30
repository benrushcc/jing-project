package io.jingproject.marshallprocessor;

import io.jingproject.common.anno.Provider;
import io.jingproject.commonprocess.AnnoUtil;
import io.jingproject.commonprocess.AnnotationProcessorException;
import io.jingproject.commonprocess.GeneratorBlock;
import io.jingproject.commonprocess.GeneratorSource;
import io.jingproject.marshall.MarshallTransformer;
import io.jingproject.marshall.MarshallTransformerFacade;
import io.jingproject.marshall.Transformable;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class MarshallTransformerProcessor extends AbstractProcessor {

    private TypeMirror marshallTransformerRawType;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        TypeMirror marshallTransformerTm = processingEnv.getElementUtils()
                .getTypeElement(MarshallTransformer.class.getCanonicalName()).asType();
        marshallTransformerRawType = processingEnv.getTypeUtils().erasure(marshallTransformerTm);
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of(Transformable.class.getCanonicalName());
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if(!roundEnv.processingOver()) {
            for (Element e : roundEnv.getElementsAnnotatedWith(Transformable.class)) {
                TypeElement t = AnnoUtil.castTypeElement(e);
                checkMarshallTransformerElement(t);
                MarshallTransformerInfo info = createTransformerInfo(t);
                GeneratorSource facadeSource = new GeneratorSource(t, "MarshallTransformerFacade");
                writeMarshallTransformerFacadeSource(facadeSource, info);
            }
        }
        return true;
    }

    private void checkMarshallTransformerElement(TypeElement t) {
        // must be class or records
        ElementKind elementKind = t.getKind();
        if(!elementKind.equals(ElementKind.CLASS) && !elementKind.equals(ElementKind.RECORD)) {
            throw new AnnotationProcessorException("only class and record are supported for @Transformable elements");
        }
        // must be top-level
        if(t.getNestingKind() != NestingKind.TOP_LEVEL) {
            throw new AnnotationProcessorException("only top level fieldElement can be annotated with @Transformable");
        }
        // must be public
        if (!t.getModifiers().contains(Modifier.PUBLIC)) {
            throw new AnnotationProcessorException("only public fieldElement can be annotated with @Transformable");
        }
        // must be non-abstract class
        if (t.getModifiers().contains(Modifier.ABSTRACT)) {
            throw new AnnotationProcessorException("abstract class can not be annotated with @Transformable");
        }
        // class or record must have no-arg constructor
        boolean foundNoArgConstructor = false;
        for (Element e : t.getEnclosedElements()) {
            if(e.getKind().equals(ElementKind.CONSTRUCTOR)) {
                ExecutableElement ex = AnnoUtil.castExecutableElement(e);
                if (ex.getParameters().isEmpty() && ex.getModifiers().contains(Modifier.PUBLIC)) {
                    foundNoArgConstructor = true;
                    break ;
                }
            }
        }
        if(!foundNoArgConstructor) {
            throw new AnnotationProcessorException("no-arg constructor not found");
        }
        // must implement MarshallTransformer interface
        boolean implementMarshallTransformerInterface = false;
        Types typeUtils = processingEnv.getTypeUtils();
        for (TypeMirror tm : t.getInterfaces()) {
            TypeMirror erasuredTm = typeUtils.erasure(tm);
            TypeMirror targetTm = Objects.requireNonNull(marshallTransformerRawType);
            if(typeUtils.isSameType(erasuredTm, targetTm)) {
                implementMarshallTransformerInterface = true;
                List<? extends TypeMirror> typeArgs = AnnoUtil.castDeclaredType(tm).getTypeArguments();
                if(typeArgs.size() != 2) {
                    throw new AssertionError();
                }
                AnnoUtil.validateTypeArgs(typeArgs);
                if(typeUtils.isSameType(typeArgs.getFirst(), typeArgs.getLast())) {
                    throw new AnnotationProcessorException("transformer must be supplied with different types");
                }
                break ;
            }
        }
        if(!implementMarshallTransformerInterface) {
            throw new AnnotationProcessorException("target element must implement MarshallTransformer interface");
        }
    }

    private MarshallTransformerInfo createTransformerInfo(TypeElement t) {
        TypeElement fromElement = null;
        TypeElement toElement = null;
        Types typeUtils = processingEnv.getTypeUtils();
        for (TypeMirror tm : t.getInterfaces()) {
            TypeMirror erasuredTm = typeUtils.erasure(tm);
            TypeMirror targetTm = Objects.requireNonNull(marshallTransformerRawType);
            if(typeUtils.isSameType(erasuredTm, targetTm)) {
                List<? extends TypeMirror> typeArgs = AnnoUtil.castDeclaredType(tm).getTypeArguments();
                fromElement = AnnoUtil.castTypeElement(AnnoUtil.castDeclaredType(typeArgs.getFirst()).asElement());
                toElement = AnnoUtil.castTypeElement(AnnoUtil.castDeclaredType(typeArgs.getLast()).asElement());
                break ;
            }
        }
        return new MarshallTransformerInfo(t, Objects.requireNonNull(fromElement), Objects.requireNonNull(toElement));
    }


    private void writeMarshallTransformerFacadeSource(GeneratorSource facadeSource, MarshallTransformerInfo info) {
        String providerClassName = facadeSource.register(Provider.class);
        String targetClassName = facadeSource.register(info.typeElement());
        String customTypeClassName = facadeSource.register(info.customTypeElement());
        String builtinTypeClassName = facadeSource.register(info.builtInElement());
        String facadeSourceClassName = facadeSource.className();
        String marshallTransformerFacadeClassName = facadeSource.register(MarshallTransformerFacade.class);
        String marshallTransformerClassName = facadeSource.register(MarshallTransformer.class);
        String overrideClassName = facadeSource.register(Override.class);
        String clsClassName = facadeSource.register(Class.class);
        GeneratorBlock b = new GeneratorBlock();
        b.addLine("@" + providerClassName + "(target = " + marshallTransformerFacadeClassName + ".class)")
                .addLine("public final class " + facadeSourceClassName + " implements " + marshallTransformerFacadeClassName + " {")
                .indent()
                .addLine("private static final " + marshallTransformerClassName + "<" + customTypeClassName + ", " + builtinTypeClassName +
                        "> INSTANCE = new " + targetClassName + "();")
                .newLine();
        b.addLine("@" + overrideClassName)
                .addLine("public " + clsClassName + "<?> customType() {")
                .indent()
                .addLine("return " + customTypeClassName + ".class;")
                .unindent()
                .addLine("}")
                .newLine();
        b.addLine("@" + overrideClassName)
                .addLine("public " + clsClassName + "<?> builtinType() {")
                .indent()
                .addLine("return " + builtinTypeClassName + ".class;")
                .unindent()
                .addLine("}")
                .newLine();
        b.addLine("@" + overrideClassName)
                .addLine("public " + marshallTransformerClassName + "<?, ?> transformer() {")
                .indent()
                .addLine("return INSTANCE;")
                .unindent()
                .addLine("}")
                .newLine();
        b.unindent().addLine("}").newLine();
        facadeSource.addBlock(b);
        facadeSource.writeToFiler(processingEnv);
    }
}
