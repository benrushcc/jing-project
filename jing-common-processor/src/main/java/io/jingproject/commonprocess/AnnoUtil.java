package io.jingproject.commonprocess;

import io.jingproject.common.anno.ProcessorApi;

import javax.lang.model.element.*;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.util.List;

@ProcessorApi
public final class AnnoUtil {
    private static final int EXTRA_CAPACITY = 16;

    private AnnoUtil() {
        throw new UnsupportedOperationException("utility class");
    }

    public static String strLiteral(String str) {
        return "\"" + str + "\"";
    }

    public static String escapeStrLiteral(String str) {
        int len = str.length();
        StringBuilder builder = new StringBuilder(Math.addExact(len, EXTRA_CAPACITY)).append('"');
        for (int i = 0; i < len; ) {
            int cp = str.codePointAt(i);
            i += Character.charCount(cp);
            switch (cp) {
                case '"' -> builder.append("\\\"");
                case '\\' -> builder.append("\\\\");
                case '\b' -> builder.append("\\b");
                case '\f' -> builder.append("\\f");
                case '\n' -> builder.append("\\n");
                case '\r' -> builder.append("\\r");
                case '\t' -> builder.append("\\t");
                default -> {
                    if (cp < 0x20) {
                        builder.append(String.format("\\u%04x", cp));
                    } else if(!Character.isBmpCodePoint(cp)) {
                        builder.appendCodePoint(cp);
                    } else {
                        char c = (char) cp;
                        if(Character.isSurrogate(c)) {
                            throw new AnnotationProcessorException("lone surrogate not supported in string literal : U+" + Integer.toHexString(cp).toUpperCase());
                        }
                        builder.append(c);
                    }
                }
            }
        }
        return builder.append('"').toString();
    }

    public static String packageName(String fullName) {
        int index = fullName.lastIndexOf(".");
        if (index == -1) {
            throw new AnnotationProcessorException("element with empty package name not supported : " + fullName);
        }
        return fullName.substring(0, index);
    }

    public static String simpleName(String fullName) {
        int index = fullName.lastIndexOf(".");
        if (index == -1) {
            throw new AnnotationProcessorException("element with empty package name not supported : " + fullName);
        }
        String r = fullName.substring(index + 1); // no overflow
        if (r.isEmpty()) {
            throw new AnnotationProcessorException("element with empty simple name not supported : " + fullName);
        }
        return r;
    }

    public static String buildClassName(String packageName, String className) {
        if (packageName == null || packageName.isBlank()) {
            throw new AnnotationProcessorException("packageName is illegal");
        }
        if (className == null || className.isBlank()) {
            throw new AnnotationProcessorException("className is illegal");
        }
        return packageName + "." + className;
    }

    public static String buildClassName(String moduleName, String packageName, String className) {
        if (moduleName == null || moduleName.isBlank()) {
            throw new AnnotationProcessorException("moduleName is illegal");
        }
        if (packageName == null || packageName.isBlank()) {
            throw new AnnotationProcessorException("packageName is illegal");
        }
        if (className == null || className.isBlank()) {
            throw new AnnotationProcessorException("className is illegal");
        }
        return moduleName + "/" + packageName + "." + className;
    }

    public static boolean isGenericType(String typeName) {
        if (typeName == null || typeName.isBlank()) {
            throw new AnnotationProcessorException("typeName is illegal");
        }
        return typeName.indexOf('<') >= 0 && typeName.indexOf('>') >= 0;
    }

    public static TypeElement castTypeElement(Element element) {
        if (element instanceof TypeElement t) {
            return t;
        }
        throw new AssertionError("not a typeElement : " + element);
    }

    public static ExecutableElement castExecutableElement(Element element) {
        if (element instanceof ExecutableElement e) {
            return e;
        }
        throw new AssertionError("not an executableElement : " + element);
    }

    public static DeclaredType castDeclaredType(TypeMirror typeMirror) {
        if (typeMirror instanceof DeclaredType d) {
            return d;
        }
        throw new AssertionError("not a declaredType : " + typeMirror);
    }

    public static ArrayType castArrayType(TypeMirror typeMirror) {
        if (typeMirror instanceof ArrayType a) {
            return a;
        }
        throw new AssertionError("not an arrayType : " + typeMirror);
    }

    public static void checkClassForRegister(Class<?> cls) {
        if (cls == null) {
            throw new AnnotationProcessorException("class cannot be null");
        }
        if (cls.isPrimitive()) {
            throw new AnnotationProcessorException("primitive class cannot be registered : " + cls.getCanonicalName());
        }
        if (cls.isAnonymousClass()) {
            throw new AnnotationProcessorException("anonymous class cannot be registered : " + cls.getCanonicalName());
        }
        if (cls.isMemberClass()) {
            throw new AnnotationProcessorException("registered class must be top-level : " + cls.getCanonicalName());
        }
        if (cls.isHidden()) {
            throw new AnnotationProcessorException("hidden class cannot be registered : " + cls.getCanonicalName());
        }
    }

    public static void checkTypeElementForRegister(TypeElement typeElement) {
        if (typeElement == null) {
            throw new AnnotationProcessorException("typeElement cannot be null");
        }
        if (typeElement.asType().getKind().isPrimitive()) {
            throw new AnnotationProcessorException("typeElement cannot be primitive: " + typeElement.getQualifiedName());
        }
        if (typeElement.getNestingKind() != NestingKind.TOP_LEVEL) {
            throw new AnnotationProcessorException("typeElement must be top-level : " + typeElement.getQualifiedName());
        }
        if (!typeElement.getModifiers().contains(Modifier.PUBLIC)) {
            throw new AnnotationProcessorException("typeElement must be public : " + typeElement.getQualifiedName());
        }
        if (!typeElement.getTypeParameters().isEmpty()) {
            throw new AnnotationProcessorException("typeElement must not have any type parameters : " + typeElement.getQualifiedName());
        }
    }

    public static void checkFieldElementForRegister(Element element) {
        if (element instanceof VariableElement || element instanceof RecordComponentElement) {
            checkFieldElementRecursively(element.asType(), element);
            return ;
        }
        throw new AnnotationProcessorException("element must be of type variableElement or recordComponentElement : " + element);
    }

    private static void checkFieldElementRecursively(TypeMirror tm, Element source) {
        TypeKind typeKind = tm.getKind();
        switch (typeKind) {
            case ERROR -> throw new AnnotationProcessorException("unresolved type not supported for field element registration : " + source.getSimpleName());
            case ARRAY -> {
                ArrayType at = castArrayType(tm);
                TypeMirror componentType = at.getComponentType();
                TypeKind componentTypeKind = componentType.getKind();
                if(componentTypeKind == TypeKind.ARRAY) {
                    throw new AnnotationProcessorException("multi dimension array not supported for field element registration : " + source.getSimpleName());
                }
                if (componentTypeKind == TypeKind.DECLARED && !castDeclaredType(componentType).getTypeArguments().isEmpty()) {
                    throw new AnnotationProcessorException("generic array not supported for field element registration : " + source.getSimpleName());
                }
                checkFieldElementRecursively(componentType, source);
            }
            case WILDCARD -> throw new AnnotationProcessorException("wildcard type not supported for field element registration : " + source.getSimpleName());
            case DECLARED -> {
                DeclaredType dt = castDeclaredType(tm);
                TypeElement te = castTypeElement(dt.asElement());
                if(te.getNestingKind() != NestingKind.TOP_LEVEL) {
                    throw new AnnotationProcessorException("field element must be top-level class : " + source.getSimpleName());
                }
                for (TypeMirror typeArgument : dt.getTypeArguments()) {
                    if (typeArgument.getKind() == TypeKind.DECLARED && !castDeclaredType(typeArgument).getTypeArguments().isEmpty()) {
                        throw new AnnotationProcessorException("nested generic type not supported for field element registration : " + source.getSimpleName());
                    }
                    checkFieldElementRecursively(typeArgument, source);
                }
            }
            default -> {
                if(!typeKind.isPrimitive()) {
                    throw new AnnotationProcessorException("unsupported field element type kind : " + typeKind + ", source : " + source.getSimpleName());
                }
            }
        }
    }

    public static void checkTypeArgs(List<? extends TypeMirror> typeArgs) {
        for (TypeMirror typeArg : typeArgs) {
            if (typeArg.getKind() == TypeKind.DECLARED) {
                DeclaredType dt = castDeclaredType(typeArg);
                TypeElement te = castTypeElement(dt.asElement());
                if (te.getTypeParameters().isEmpty()) {
                    continue ;
                }
                throw new AnnotationProcessorException("generic type args cannot have any type args : " + te);
            }
            throw new AnnotationProcessorException("type arg must be declared : " + typeArg);
        }
    }
}
