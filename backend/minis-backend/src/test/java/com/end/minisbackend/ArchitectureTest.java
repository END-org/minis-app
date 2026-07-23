package com.end.minisbackend;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "com.end.minisbackend", importOptions = DoNotIncludeTests.class)
class ArchitectureTest {
  @ArchTest
  static final ArchRule sharedDoesNotDependOnFeatures =
      noClasses()
          .that()
          .resideInAPackage("..shared..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("..identity..", "..user..");

  @ArchTest
  static final ArchRule controllersDoNotAccessInfrastructure =
      noClasses()
          .that()
          .resideInAPackage("..api..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("..infrastructure..");

  @ArchTest
  static final ArchRule controllersDoNotExposeJpaEntities =
      noClasses()
          .that()
          .resideInAPackage("..api..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("..user.domain..");

  @ArchTest
  static final ArchRule identityDoesNotDependOnUserInfrastructure =
      noClasses()
          .that()
          .resideInAPackage("..identity..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("..user.infrastructure..");

  @ArchTest
  static final ArchRule featureInfrastructureIsInternal =
      noClasses()
          .that()
          .resideOutsideOfPackages("..user.infrastructure..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("..user.infrastructure..");

  @ArchTest
  static final ArchRule featurePackagesAreFreeOfCycles =
      slices().matching("com.end.minisbackend.(*)..").should().beFreeOfCycles();
}
