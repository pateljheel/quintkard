package io.quintkard.quintkardapp.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import io.quintkard.quintkardapp.message.MessageRepository;
import io.quintkard.quintkardapp.messagepipeline.InternalMessageQueueRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.JpaRepository;

class TenantIsolationArchitectureTest {

    private final JavaClasses importedClasses = new ClassFileImporter()
            .importPackages("io.quintkard.quintkardapp");

    @Test
    void userFacingPackagesDoNotDependOnInternalMessageQueueRepository() {
        noClasses()
                .that()
                .resideInAnyPackage("..message..", "..card..", "..agent..", "..orchestrator..")
                .should()
                .dependOnClassesThat()
                .haveFullyQualifiedName(InternalMessageQueueRepository.class.getName())
                .check(importedClasses);
    }

    @Test
    void userFacingMessageRepositoryDoesNotExtendJpaRepository() {
        classes()
                .that()
                .haveFullyQualifiedName(MessageRepository.class.getName())
                .should()
                .notBeAssignableTo(JpaRepository.class)
                .check(importedClasses);
    }
}
