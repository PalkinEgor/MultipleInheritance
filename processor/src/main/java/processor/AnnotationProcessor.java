package processor;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.*;
import ru.nsu.palkin.Homogenous;
import ru.nsu.palkin.MultiInheritance;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@SupportedSourceVersion(SourceVersion.RELEASE_21)
@AutoService(Processor.class)
@SupportedAnnotationTypes({"ru.nsu.palkin.MultiInheritance", "ru.nsu.palkin.Homogenous"})
public class AnnotationProcessor extends AbstractProcessor {
    private Set<String> generatedRootClasses = new HashSet<>();

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Set<TypeElement> multiInheritanceElements = new HashSet<>();
        for (Element element : roundEnv.getElementsAnnotatedWith(MultiInheritance.class)) {
            if (element instanceof TypeElement) {
                multiInheritanceElements.add((TypeElement) element);
            }
        }

        if (multiInheritanceElements.isEmpty()) {
            return true;
        }

        Set<TypeElement> homogenousInterfaces = roundEnv.getElementsAnnotatedWith(Homogenous.class).stream()
                .filter(e -> e.getKind() == ElementKind.INTERFACE)
                .map(e -> (TypeElement) e)
                .collect(Collectors.toSet());

        if (homogenousInterfaces.isEmpty()) {
            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.ERROR, "Не найдено ни одного интерфейса с аннотацией @Homogenous.");
            return true;
        }

        for (TypeElement interfaceElement : homogenousInterfaces) {
            String qualifiedName = interfaceElement.getQualifiedName().toString();
            if (!generatedRootClasses.contains(qualifiedName)) {
                generateRootClass(interfaceElement);
                generatedRootClasses.add(qualifiedName);
            }
        }

        return true;
    }

    private void generateRootClass(TypeElement interfaceElement) {
        try {
            TypeSpec rootClass = createRootClass(interfaceElement);
            if (rootClass == null) {
                return;
            }
            PackageElement packageElement = processingEnv.getElementUtils().getPackageOf(interfaceElement);
            String pkgName = packageElement.getQualifiedName().toString();
            JavaFile.builder(pkgName, rootClass)
                    .indent("    ")
                    .skipJavaLangImports(true)
                    .build()
                    .writeTo(processingEnv.getFiler());

            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,
                    "Root class generated successfully for interface: " + interfaceElement.getQualifiedName());
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "Error generating class for interface " + interfaceElement.getQualifiedName() + ": " + e.getMessage());
        }
    }

    private MethodSpec createMethod(ExecutableElement method) {
        List<ParameterSpec> parameterSpecs = new ArrayList<>();
        List<String> parameterNames = new ArrayList<>();
        for (VariableElement parameter : method.getParameters()) {
            parameterSpecs.add(ParameterSpec.get(parameter));
            parameterNames.add(parameter.getSimpleName().toString());
        }

        CodeBlock.Builder codeBuilder = CodeBlock.builder();

        StringBuilder callBuilder;
        if (method.getReturnType().getKind() == TypeKind.VOID) {
            callBuilder = new StringBuilder("next" + method.getSimpleName() + "(");
        } else {
            callBuilder = new StringBuilder("return next" + method.getSimpleName() + "(");
        }
        for (int i = 0; i < parameterNames.size(); i++) {
            callBuilder.append(parameterNames.get(i));
            if (i < parameterNames.size() - 1) {
                callBuilder.append(", ");
            }
        }
        callBuilder.append(")");

        codeBuilder.addStatement(callBuilder.toString());

        return MethodSpec.methodBuilder(method.getSimpleName().toString())
                .addModifiers(Modifier.PUBLIC)
                .returns(ClassName.get(method.getReturnType()))
                .addParameters(parameterSpecs)
                .addCode(codeBuilder.build())
                .build();
    }

    private TypeSpec createRootClass(TypeElement interfaceElement) {
        String interfacePackage = processingEnv.getElementUtils().getPackageOf(interfaceElement).getQualifiedName().toString();
        String interfaceSimpleName = interfaceElement.getSimpleName().toString();

        // Имя генерируемого класса
        String generatedClassName = interfaceSimpleName + "Root";

        TypeSpec.Builder classBuilder = TypeSpec.classBuilder(generatedClassName)
                .addModifiers(Modifier.ABSTRACT, Modifier.PUBLIC)
                .addSuperinterface(ClassName.get(interfacePackage, interfaceSimpleName))
                .addMethod(createMroMethod())
                .addMethod(createGetSuperClassesMethod());

        // Для каждого метода интерфейса генерируем обёртку и реализацию вызова метода next<ИмяМетода>
        for (ExecutableElement method : ElementFilter.methodsIn(interfaceElement.getEnclosedElements())) {
            classBuilder.addMethod(createMethod(method));
            classBuilder.addMethod(createNextMethod(method, interfaceElement));
        }

        return classBuilder.build();
    }

    private MethodSpec createGetSuperClassesMethod() {
        return MethodSpec.methodBuilder("getSuperClasses")
                .addModifiers(Modifier.PUBLIC)
                .returns(void.class)
                .addCode("""
                $T annotation = this.getClass().getAnnotation($T.class);
                if (annotation != null) {
                    System.out.println(java.util.Arrays.toString(annotation.value()));
                }
                """, MultiInheritance.class, MultiInheritance.class)
                .build();
    }

    private MethodSpec createMroMethod() {
        return MethodSpec.methodBuilder("mro")
                .addModifiers(Modifier.PUBLIC)
                .returns(void.class)
                .addCode("""
                java.util.List<Class<?>> order = C3Linearization.c3Linearization(this.getClass());
                System.out.println(order);
                """)
                .build();
    }

    private MethodSpec createNextMethod(ExecutableElement method, TypeElement interfaceElement) {
        List<ParameterSpec> parameterSpecs = new ArrayList<>();
        List<String> parameterNames = new ArrayList<>();
        for (VariableElement parameter : method.getParameters()) {
            parameterSpecs.add(ParameterSpec.get(parameter));
            parameterNames.add(parameter.getSimpleName().toString());
        }

        CodeBlock.Builder codeBuilder = CodeBlock.builder();

        StringBuilder callBuilder = new StringBuilder("p." + method.getSimpleName() + "(");
        for (int i = 0; i < parameterNames.size(); i++) {
            callBuilder.append(parameterNames.get(i));
            if (i < parameterNames.size() - 1) {
                callBuilder.append(", ");
            }
        }
        callBuilder.append(")");

        String methodName = method.getSimpleName().toString();
        List<? extends VariableElement> parameters = method.getParameters();
        String parameterTypes = parameters.stream()
                .map(p -> p.asType().toString() + ".class")
                .collect(Collectors.joining(", "));

        String interfaceName = interfaceElement.getQualifiedName().toString();

        String sub, type, result;
        if (method.getReturnType().getKind() == TypeKind.VOID) {
            sub = "        " + callBuilder + ";\n" + "        return;\n";
            type = "";
            result = "";
        } else {
            sub = "        result = " + callBuilder + ";\n" + "        return result;\n";
            type = method.getReturnType().toString() + " result;\n";
            result = "throw new RuntimeException(\"No valid " + methodName + " implementation found\");";
        }

        String code = "java.util.List<Class<?>> order = C3Linearization.c3Linearization(this.getClass());\n" +
                "order.remove(0);\n" +
                type +
                "for (Class<?> cls : order) {\n" +
                "    try {\n" +
                "        java.lang.reflect.Method method = cls.getDeclaredMethod(\"" + methodName + "\"" +
                (parameters.isEmpty() ? "" : ", " + parameterTypes) + ");\n" +
                "        " + interfaceName + " p = (" + interfaceName + ") cls.getDeclaredConstructor().newInstance();\n" +
                sub +
                "    } catch (Exception e) {\n" +
                "        continue;\n" +
                "    }\n" +
                "}\n" +
                result;

        codeBuilder.add(code);

        return MethodSpec.methodBuilder("next" + methodName)
                .addModifiers(Modifier.PRIVATE)
                .returns(ClassName.get(method.getReturnType()))
                .addParameters(parameterSpecs)
                .addCode(codeBuilder.build())
                .build();
    }
}