package com.kerosene.architecture;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaConstructorCall;
import com.tngtech.archunit.core.domain.JavaFieldAccess;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Predicate;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArchitectureGuardrailsTest {

    private static final JavaClasses PRODUCTION_CLASSES = new ClassFileImporter()
            .withImportOption(new ImportOption.DoNotIncludeTests())
            .importPackages("com.kerosene");

    @Test
    void productionCodeDoesNotUseMisspelledStructuralNames() {
        List<String> violations = PRODUCTION_CLASSES.stream()
                .filter(hasMisspelledPackageOrClassName())
                .map(JavaClass::getName)
                .sorted()
                .toList();

        assertTrue(violations.isEmpty(), () -> "Misspelled package/class names found:\n" + String.join("\n", violations));
    }

    @Test
    void productionCodeDoesNotWriteToSystemErr() {
        List<String> violations = PRODUCTION_CLASSES.stream()
                .flatMap(javaClass -> javaClass.getFieldAccessesFromSelf().stream())
                .filter(this::isSystemErrAccess)
                .map(this::describeFieldAccess)
                .sorted()
                .toList();

        assertTrue(violations.isEmpty(), () -> "System.err access found:\n" + String.join("\n", violations));
    }

    @Test
    void objectMappersAreCreatedOnlyInSpringConfiguration() {
        List<String> violations = PRODUCTION_CLASSES.stream()
                .filter(javaClass -> !javaClass.getPackageName().startsWith("com.kerosene.config"))
                .filter(javaClass -> !javaClass.isAssignableTo(jakarta.persistence.AttributeConverter.class))
                .flatMap(javaClass -> javaClass.getConstructorCallsFromSelf().stream())
                .filter(this::isObjectMapperConstructorCall)
                .map(this::describeConstructorCall)
                .sorted()
                .toList();

        assertTrue(violations.isEmpty(), () -> "ObjectMapper constructors outside configuration:\n"
                + String.join("\n", violations));
    }

    @Test
    void coreProductionCodeDoesNotDependOnKfeImplementationPackages() {
        noClasses()
                .should().dependOnClassesThat().resideInAPackage("com.kerosene.kfe..")
                .check(PRODUCTION_CLASSES);
    }

    @Test
    void coreDoesNotContainKfeImplementationClasses() {
        List<String> violations = PRODUCTION_CLASSES.stream()
                .filter(javaClass -> javaClass.getPackageName().startsWith("com.kerosene.kfe"))
                .map(JavaClass::getName)
                .sorted()
                .toList();

        assertTrue(violations.isEmpty(), () -> "KFE implementation classes found in Core:\n"
                + String.join("\n", violations));
    }

    @Test
    void applicationAndDomainLayersDoNotDependOnControllers() {
        noClasses()
                .that().resideInAnyPackage("com.kerosene..application..", "com.kerosene..domain..")
                .should().dependOnClassesThat().resideInAPackage("com.kerosene..controller..")
                .check(PRODUCTION_CLASSES);
    }

    @Test
    void domainLayerDoesNotDependOnSpringOrInfrastructureAdapters() {
        noClasses()
                .that().resideInAPackage("com.kerosene..domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework..",
                        "com.kerosene..controller..",
                        "com.kerosene..infra..",
                        "com.kerosene..repository..")
                .check(PRODUCTION_CLASSES);
    }

    private Predicate<JavaClass> hasMisspelledPackageOrClassName() {
        return javaClass -> {
            String packageName = javaClass.getPackageName();
            String className = javaClass.getSimpleName();
            return packageName.contains(".persistance.")
                    || packageName.contains(".contratcs.")
                    || className.contains("Bcript")
                    || className.contains("Usuario")
                    || className.contains("Alredy");
        };
    }

    private boolean isSystemErrAccess(JavaFieldAccess access) {
        return System.class.getName().equals(access.getTarget().getOwner().getName())
                && "err".equals(access.getTarget().getName());
    }

    private boolean isObjectMapperConstructorCall(JavaConstructorCall call) {
        return ObjectMapper.class.getName().equals(call.getTarget().getOwner().getName());
    }

    private String describeFieldAccess(JavaFieldAccess access) {
        return access.getOriginOwner().getName() + ":" + access.getLineNumber();
    }

    private String describeConstructorCall(JavaConstructorCall call) {
        return call.getOriginOwner().getName() + ":" + call.getLineNumber();
    }
}
