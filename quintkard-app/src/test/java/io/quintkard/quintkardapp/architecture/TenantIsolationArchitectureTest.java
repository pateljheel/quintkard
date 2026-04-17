package io.quintkard.quintkardapp.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.JpaRepository;

class TenantIsolationArchitectureTest {

    private final JavaClasses importedClasses = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("io.quintkard.quintkardapp");

    @Test
    void tenantOwnedRepositoriesDoNotExtendJpaRepository() {
        classes()
                .that()
                .haveSimpleNameEndingWith("Repository")
                .and()
                .resideInAnyPackage("..message..", "..card..", "..agent..", "..orchestrator..")
                .and()
                .doNotHaveSimpleName("UserRepository")
                .should()
                .notBeAssignableTo(JpaRepository.class)
                .check(importedClasses);
    }

    @Test
    void userFacingServicesAndControllersDoNotDependOnInternalRepositories() {
        noClasses()
                .that()
                .resideInAnyPackage("..message..", "..card..", "..agent..", "..orchestrator..")
                .and()
                .haveSimpleNameEndingWith("Controller")
                .or()
                .haveSimpleName("MessageServiceImpl")
                .or()
                .haveSimpleName("CardServiceImpl")
                .or()
                .haveSimpleName("AgentServiceImpl")
                .or()
                .haveSimpleName("OrchestratorServiceImpl")
                .should()
                .dependOnClassesThat()
                .haveSimpleNameStartingWith("Internal")
                .andShould()
                .haveSimpleNameEndingWith("Repository")
                .check(importedClasses);
    }
}
