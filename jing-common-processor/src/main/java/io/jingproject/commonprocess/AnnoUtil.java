package io.jingproject.commonprocess;

import io.jingproject.common.anno.ProcessorApi;

import javax.lang.model.element.*;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

@ProcessorApi
public final class AnnoUtil {
    private AnnoUtil() {
        throw new UnsupportedOperationException("utility class");
    }

    public static String packageName(String fullName) {
        int index = fullName.lastIndexOf(".");
        if(index == -1) {
            throw new AnnotationProcessorException("empty package name not supported");
        }
        return fullName.substring(0, index);
    }

    public static String simpleName(String fullName) {
        int index = fullName.lastIndexOf(".");
        if(index == -1) {
            throw new AnnotationProcessorException("empty package name not supported");
        }
        String r = fullName.substring(Math.incrementExact(index));
        if(r.isEmpty()) {
            throw new AnnotationProcessorException("empty simple name not supported");
        }
        return r;
    }

    public static String buildClassName(String packageName, String className) {
        if(packageName == null || packageName.isBlank()) {
            throw new AnnotationProcessorException("packageName is illegal");
        }
        if(className == null || className.isBlank()) {
            throw new AnnotationProcessorException("className is illegal");
        }
        return packageName + "." + className;
    }

    public static String buildClassName(String moduleName, String packageName, String className) {
        if(moduleName == null || moduleName.isBlank()) {
            throw new AnnotationProcessorException("moduleName is illegal");
        }
        if(packageName == null || packageName.isBlank()) {
            throw new AnnotationProcessorException("packageName is illegal");
        }
        if(className == null || className.isBlank()) {
            throw new AnnotationProcessorException("className is illegal");
        }
        return moduleName + "/" + packageName + "." + className;
    }

    public static boolean isGenericType(String typeName) {
        if(typeName == null || typeName.isBlank()) {
            throw new AnnotationProcessorException("typeName is illegal");
        }
        return typeName.contains("<") && typeName.contains(">");
    }

    public static TypeElement castTypeElement(Element element) {
        if(element instanceof TypeElement t) {
            return t;
        } else {
            throw new AssertionError("not a typeElement : " + element.asType());
        }
    }

    public static VariableElement castVariableElement(Element element) {
        if(element instanceof VariableElement v) {
            return v;
        } else {
            throw new AssertionError("not a variableElement : " + element.asType());
        }
    }

    public static ExecutableElement castExecutableElement(Element element) {
        if(element instanceof ExecutableElement e) {
            return e;
        } else {
            throw new AssertionError("not an executableElement : " + element.asType());
        }
    }

    public static DeclaredType castDeclaredType(TypeMirror typeMirror) {
        if(typeMirror instanceof DeclaredType d) {
            return d;
        } else {
            throw new AssertionError("not a declaredType : " + typeMirror);
        }
    }

    public static ArrayType castArrayType(TypeMirror typeMirror) {
        if(typeMirror instanceof ArrayType a) {
            return a;
        } else {
            throw new AssertionError("not an arrayType : " + typeMirror);
        }
    }

    public static void checkClassForRegister(Class<?> cls) {
        if(cls == null) {
            throw new AnnotationProcessorException("class cannot be null");
        }
        if(cls.isPrimitive()) {
            throw new AnnotationProcessorException("primitive class cannot be registered : " + cls);
        }
        if(cls.isAnonymousClass()) {
            throw new AnnotationProcessorException("anonymous class cannot be registered : " + cls);
        }
        if(cls.isMemberClass()) {
            throw new AnnotationProcessorException("registered class must be top-level : " + cls.getSimpleName());
        }
    }

    public static void checkTypeElementForRegister(TypeElement typeElement) {
        if(typeElement == null) {
            throw new AnnotationProcessorException("typeElement cannot be null");
        }
        if(typeElement.asType().getKind().isPrimitive()) {
            throw new AnnotationProcessorException("typeElement cannot be primitive: " + typeElement);
        }
        if(typeElement.getNestingKind() != NestingKind.TOP_LEVEL) {
            throw new AnnotationProcessorException("typeElement must be top-level : " + typeElement);
        }
        if(!typeElement.getTypeParameters().isEmpty()) {
            throw new AnnotationProcessorException("typeElement must not have any type parameters : " + typeElement);
        }
    }

    public static void checkVariableElementForRegister(VariableElement variableElement) {
        if(variableElement == null) {
            throw new AnnotationProcessorException("variableElement cannot be null");
        }
        TypeElement enclosingElement = castTypeElement(variableElement.getEnclosingElement());
        if(enclosingElement.getNestingKind() != NestingKind.TOP_LEVEL) {
            throw new AnnotationProcessorException("enclosing typeElement must be top-level : " + enclosingElement);
        }
        if(!enclosingElement.getTypeParameters().isEmpty()) {
            throw new AnnotationProcessorException("enclosing typeElement must not have any type parameters : " + enclosingElement);
        }
        TypeMirror tm = variableElement.asType();
        if(tm.getKind() == TypeKind.ARRAY) {
            ArrayType arrayType = castArrayType(tm);
            if(arrayType.getComponentType().getKind() == TypeKind.ARRAY) {
                throw new AnnotationProcessorException("multi dimensional arrayType not supported: " + arrayType);
            }
        }
    }
}
