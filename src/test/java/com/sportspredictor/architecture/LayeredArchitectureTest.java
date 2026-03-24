package com.sportspredictor.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/** Enforces layered architecture constraints per ADR-002. */
@AnalyzeClasses(packages = "com.sportspredictor", importOptions = ImportOption.DoNotIncludeTests.class)
class LayeredArchitectureTest {

    /** Tools depend only on services — never on repositories, clients, or entities directly. */
    @ArchTest
    static final ArchRule toolsShouldOnlyDependOnServices = noClasses()
            .that()
            .resideInAPackage("com.sportspredictor.tool..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                    "com.sportspredictor.repository..", "com.sportspredictor.client..", "com.sportspredictor.entity..")
            .allowEmptyShould(true);

    /** Services must not depend on tools — data flows tool -> service, not the reverse. */
    @ArchTest
    static final ArchRule servicesShouldNotDependOnTools = noClasses()
            .that()
            .resideInAPackage("com.sportspredictor.service..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("com.sportspredictor.tool..")
            .allowEmptyShould(true);

    /** Clients are self-contained — no dependencies on entities, services, tools, or repositories. */
    @ArchTest
    static final ArchRule clientsShouldBeSelfContained = noClasses()
            .that()
            .resideInAPackage("com.sportspredictor.client..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                    "com.sportspredictor.entity..",
                    "com.sportspredictor.service..",
                    "com.sportspredictor.tool..",
                    "com.sportspredictor.repository..")
            .allowEmptyShould(true);

    /** Resources depend only on services — same constraints as tools. */
    @ArchTest
    static final ArchRule resourcesShouldOnlyDependOnServices = noClasses()
            .that()
            .resideInAPackage("com.sportspredictor.resource..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                    "com.sportspredictor.repository..", "com.sportspredictor.client..", "com.sportspredictor.entity..")
            .allowEmptyShould(true);

    /** No circular dependencies between top-level packages. */
    @ArchTest
    static final ArchRule noCyclicDependencies =
            slices().matching("com.sportspredictor.(*)..").should().beFreeOfCycles();
}
