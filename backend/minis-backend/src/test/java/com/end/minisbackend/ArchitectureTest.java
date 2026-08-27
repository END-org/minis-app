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
          .resideInAnyPackage("..admin..", "..creator..", "..identity..", "..user..");

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
          .resideInAnyPackage("..admin.domain..", "..creator.domain..", "..user.domain..");

  @ArchTest
  static final ArchRule domainDoesNotDependOnApi =
      noClasses()
          .that()
          .resideInAPackage("..domain..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("..api..");

  @ArchTest
  static final ArchRule domainDoesNotDependOnSpringWeb =
      noClasses()
          .that()
          .resideInAPackage("..domain..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("org.springframework.web..");

  @ArchTest
  static final ArchRule applicationDoesNotDependOnApi =
      noClasses()
          .that()
          .resideInAPackage("..application..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("..api..");

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
  static final ArchRule creatorInfrastructureIsInternal =
      noClasses()
          .that()
          .resideOutsideOfPackages("..creator.infrastructure..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("..creator.infrastructure..");

  @ArchTest
  static final ArchRule adminInfrastructureIsInternal =
      noClasses()
          .that()
          .resideOutsideOfPackages("..admin.infrastructure..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("..admin.infrastructure..");

  @ArchTest
  static final ArchRule contentInfrastructureIsInternal =
      noClasses()
          .that()
          .resideOutsideOfPackages("..content.infrastructure..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("..content.infrastructure..");

  @ArchTest
  static final ArchRule featurePackagesAreFreeOfCycles =
      slices().matching("com.end.minisbackend.(*)..").should().beFreeOfCycles();
}
