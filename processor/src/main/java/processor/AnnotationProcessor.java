package processor;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.*;
import ru.nsu.palkin.ISomeInterface;
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
@SupportedAnnotationTypes("ru.nsu.palkin.MultiInheritance")
public class AnnotationProcessor extends AbstractProcessor {
    private boolean rootGenerated = false;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (rootGenerated) {
            return true;
        }

        Set<TypeElement> classElements = new HashSet<>();
        for (Element element : roundEnv.getElementsAnnotatedWith(MultiInheritance.class)) {
            if (element instanceof TypeElement) {
                classElements.add((TypeElement) element);
            }
        }

        if (!classElements.isEmpty()) {
            generateRootClass();
            rootGenerated = true;
        }

        return true;
    }

    private void generateRootClass() {
        try {
            TypeSpec rootClass = createRootClass();

            JavaFile.builder("ru.nsu.palkin", rootClass)
                    .indent("    ")
                    .skipJavaLangImports(true)
                    .build()
                    .writeTo(processingEnv.getFiler());

            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "ISomeInterfaceRoot generated successfully.");
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Error generating class: " + e.getMessage());
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

    private TypeSpec createRootClass() {
        TypeElement interfaceElement = processingEnv.getElementUtils()
                .getTypeElement("ru.nsu.palkin.ISomeInterface");

        if (interfaceElement == null) {
            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.ERROR, "Не удаётся найти интерфейс ru.nsu.palkin.ISomeInterface");
        }
        TypeSpec.Builder classBuilder = TypeSpec.classBuilder("ISomeInterfaceRoot")
                .addModifiers(Modifier.ABSTRACT, Modifier.PUBLIC)
                .addSuperinterface(ClassName.get("ru.nsu.palkin", "ISomeInterface"))
                .addMethod(createMroMethod())
                .addMethod(createGetSuperClassesMethod());

        for (ExecutableElement method : ElementFilter.methodsIn(interfaceElement.getEnclosedElements())) {
            classBuilder.addMethod(createMethod(method));
            classBuilder.addMethod(createNextMethod(method));
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

    private MethodSpec createNextMethod(ExecutableElement method) {
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

        StringBuilder call = new StringBuilder();

        String methodName = method.getSimpleName().toString();
        List<? extends VariableElement> parameters = method.getParameters();
        String parameterTypes = parameters.stream()
                .map(p -> p.asType().toString())
                .collect(Collectors.joining(".class, ")) +
                (parameters.isEmpty() ? "" : ".class");

        String sub, type, result;
        if (method.getReturnType().getKind() == TypeKind.VOID) {
            sub = "        " + callBuilder + ";\n" +
                    "        return;\n";
            type = "";
            result = "";
        } else {
            sub = "        result = " + callBuilder + ";\n" +
                    "        return result;\n";
            type = method.getReturnType().toString() + " result;\n";
            result = "throw new RuntimeException(\"No valid someMethod implementation found\");";
        }
        call.append(
                "java.util.List<Class<?>> order = C3Linearization.c3Linearization(this.getClass());\n" +
                        "order.remove(0);\n" +
                        type +
                        "for (Class<?> cls : order) {\n" +
                        "    try {\n" +
                        "        java.lang.reflect.Method method = cls.getDeclaredMethod(\"" + methodName + "\"" + (parameters.isEmpty() ? "" : ", " + parameterTypes) + ");\n" +
                        "        ISomeInterface p = (ISomeInterface) cls.getDeclaredConstructor().newInstance();\n" +
                        sub +
                        "    } catch (Exception e) {\n" +
                        "        continue;\n" +
                        "    }\n" +
                        "}\n" +
                        result);

        codeBuilder.add(call.toString());

        return MethodSpec.methodBuilder("next" + method.getSimpleName().toString())
                .addModifiers(Modifier.PRIVATE)
                .returns(ClassName.get(method.getReturnType()))
                .addParameters(parameterSpecs)
                .addCode(codeBuilder.build())
                .build();
    }
}
