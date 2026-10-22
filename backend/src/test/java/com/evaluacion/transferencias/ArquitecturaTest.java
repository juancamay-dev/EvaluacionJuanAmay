package com.evaluacion.transferencias;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "com.evaluacion.transferencias", importOptions = ImportOption.DoNotIncludeTests.class)
class ArquitecturaTest {

    @ArchTest
    static final ArchRule capas = layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .withOptionalLayers(true)
            .layer("Api").definedBy("..api..")
            .layer("Application").definedBy("..application..")
            .layer("Domain").definedBy("..domain..")
            .layer("Infrastructure").definedBy("..infrastructure..")
            .whereLayer("Api").mayNotBeAccessedByAnyLayer()
            .whereLayer("Application").mayOnlyBeAccessedByLayers("Api")
            .whereLayer("Infrastructure").mayOnlyBeAccessedByLayers("Application")
            .whereLayer("Domain").mayOnlyBeAccessedByLayers("Api", "Application", "Infrastructure");

    @ArchTest
    static final ArchRule resourcesNoUsanEntidades = noClasses().that().resideInAPackage("..api..")
            .should().dependOnClassesThat().resideInAPackage("..domain..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule sinInyeccionPorCampo = noFields()
            .should().beAnnotatedWith(jakarta.inject.Inject.class)
            .allowEmptyShould(true);
}
