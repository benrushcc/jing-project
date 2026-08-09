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
    private AnnoUtil() {
        throw new UnsupportedOperationException("utility class");
    }

    public static String javaStringLiteral(String str) {
        return "\"" + str + "\"";
    }

    public static String escapeJavaStringLiteral(String str, StringBuilder builder) {
        if (!builder.isEmpty()) {
            throw new AnnotationProcessorException("builder not empty");
        }
        builder.append("\"");
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            switch (c) {
                case '"' -> builder.append("\\\"");
                case '\\' -> builder.append("\\\\");
                case '\b' -> builder.append("\\b");
                case '\f' -> builder.append("\\f");
                case '\n' -> builder.append("\\n");
                case '\r' -> builder.append("\\r");
                case '\t' -> builder.append("\\t");
                default -> {
                    if (c < 0x20) {
                        builder.append("\\u");
                        String hex = Integer.toHexString(c);
                        builder.repeat("0", 4 - hex.length());
                        builder.append(hex);
                    } else {
                        builder.append(c); // safe for surrogate
                    }
                }
            }
        }
        builder.append("\"");
        String r = builder.toString();
        builder.setLength(0);
        return r;
    }

    public static String packageName(String fullName) {
        int index = fullName.lastIndexOf(".");
        if (index == -1) {
            throw new AnnotationProcessorException("empty package name not supported");
        }
        return fullName.substring(0, index);
    }

    public static String simpleName(String fullName) {
        int index = fullName.lastIndexOf(".");
        if (index == -1) {
            throw new AnnotationProcessorException("empty package name not supported");
        }
        String r = fullName.substring(Math.incrementExact(index));
        if (r.isEmpty()) {
            throw new AnnotationProcessorException("empty simple name not supported");
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
        return typeName.contains("<") && typeName.contains(">");
    }

    public static TypeElement castTypeElement(Element element) {
        if (element instanceof TypeElement t) {
            return t;
        } else {
            throw new AssertionError("not a typeElement : " + element.asType());
        }
    }

    public static VariableElement castVariableElement(Element element) {
        if (element instanceof VariableElement v) {
            return v;
        } else {
            throw new AssertionError("not a variableElement : " + element.asType());
        }
    }

    public static RecordComponentElement castRecordComponentElement(Element element) {
        if (element instanceof RecordComponentElement r) {
            return r;
        } else {
            throw new AssertionError("not a recordComponentElement : " + element.asType());
        }
    }

    public static ExecutableElement castExecutableElement(Element element) {
        if (element instanceof ExecutableElement e) {
            return e;
        } else {
            throw new AssertionError("not an executableElement : " + element.asType());
        }
    }

    public static DeclaredType castDeclaredType(TypeMirror typeMirror) {
        if (typeMirror instanceof DeclaredType d) {
            return d;
        } else {
            throw new AssertionError("not a declaredType : " + typeMirror);
        }
    }

    public static ArrayType castArrayType(TypeMirror typeMirror) {
        if (typeMirror instanceof ArrayType a) {
            return a;
        } else {
            throw new AssertionError("not an arrayType : " + typeMirror);
        }
    }

    public static void checkClassForRegister(Class<?> cls) {
        if (cls == null) {
            throw new AnnotationProcessorException("class cannot be null");
        }
        if (cls.isPrimitive()) {
            throw new AnnotationProcessorException("primitive class cannot be registered : " + cls);
        }
        if (cls.isAnonymousClass()) {
            throw new AnnotationProcessorException("anonymous class cannot be registered : " + cls);
        }
        if (cls.isMemberClass()) {
            throw new AnnotationProcessorException("registered class must be top-level : " + cls.getSimpleName());
        }
        if (cls.isHidden()) {
            throw new AnnotationProcessorException("hidden class cannot be registered : " + cls);
        }
    }

    public static void checkTypeElementForRegister(TypeElement typeElement) {
        if (typeElement == null) {
            throw new AnnotationProcessorException("typeElement cannot be null");
        }
        if (typeElement.asType().getKind().isPrimitive()) {
            throw new AnnotationProcessorException("typeElement cannot be primitive: " + typeElement);
        }
        if (typeElement.getNestingKind() != NestingKind.TOP_LEVEL) {
            throw new AnnotationProcessorException("typeElement must be top-level : " + typeElement);
        }
        if (!typeElement.getModifiers().contains(Modifier.PUBLIC)) {
            throw new AnnotationProcessorException("typeElement must be public : " + typeElement);
        }
        if (!typeElement.getTypeParameters().isEmpty()) {
            throw new AnnotationProcessorException("typeElement must not have any type parameters : " + typeElement);
        }
    }

    public static void checkFieldElementForRegister(Element element) {
        if (!(element instanceof VariableElement) && !(element instanceof RecordComponentElement)) {
            throw new AnnotationProcessorException("element must be of type variableElement or recordComponentElement : " + element);
        }
        checkTypeElementForRegister(castTypeElement(element.getEnclosingElement()));
        TypeMirror tm = element.asType();
        if (tm.getKind() == TypeKind.ARRAY) {
            validateArray(castArrayType(tm));
        } else if (tm.getKind() == TypeKind.DECLARED) {
            validateTypeArgs(castDeclaredType(tm).getTypeArguments());
        }
        String qualifiedName = element.toString();
        if (qualifiedName.contains("extends") || qualifiedName.contains("super")) {
            // supporting 'super' and 'extends' currently brings no benefits to the program
            // but significantly increases complexity, therefore they are not considered
            throw new AnnotationProcessorException("super and extends are not supported for registration");
        }
    }

    public static void validateTypeArgs(List<? extends TypeMirror> typeArgs) {
        for (TypeMirror typeArg : typeArgs) {
            if (typeArg.getKind() == TypeKind.DECLARED) {
                DeclaredType dt = castDeclaredType(typeArg);
                TypeElement te = castTypeElement(dt.asElement());
                if (!te.getTypeParameters().isEmpty()) {
                    throw new AnnotationProcessorException("generic type args cannot have any type args : " + te);
                }
            } else {
                throw new AnnotationProcessorException("type arg must be declared : " + typeArg);
            }
        }
    }

    public static void validateArray(ArrayType arrayType) {
        TypeMirror tm = arrayType.getComponentType();
        TypeKind tmKind = tm.getKind();
        if (tmKind == TypeKind.DECLARED) {
            DeclaredType componentDeclaredType = AnnoUtil.castDeclaredType(tm);
            TypeElement componentTypeElement = AnnoUtil.castTypeElement(componentDeclaredType.asElement());
            if (!componentTypeElement.getTypeParameters().isEmpty()) {
                throw new AnnotationProcessorException("generic array not supported : " + componentTypeElement);
            }
        } else if (tmKind == TypeKind.ARRAY) {
            throw new AnnotationProcessorException("multi dimension array not supported : " + tm);
        } else if (!tmKind.isPrimitive()) {
            throw new AnnotationProcessorException("unknown array type : " + tm);
        }
    }
}
