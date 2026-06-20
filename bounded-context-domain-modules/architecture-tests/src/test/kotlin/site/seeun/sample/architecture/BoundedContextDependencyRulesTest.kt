package site.seeun.sample.architecture

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import kotlin.test.Test

class BoundedContextDependencyRulesTest {
    private val classes = ClassFileImporter().importPackages("site.seeun.sample")

    @Test
    fun `domain modules do not depend on applications or support adapters`() {
        noClasses()
            .that()
            .resideInAPackage("..domain..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("..apps..", "..support..")
            .check(classes)
    }

    @Test
    fun `api application does not depend on backoffice-only contexts`() {
        noClasses()
            .that()
            .resideInAPackage("..apps.api..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("..apps.backoffice..", "..domain.organization..", "..support..")
            .check(classes)
    }

    @Test
    fun `backoffice application does not depend on learning or catalog flows`() {
        noClasses()
            .that()
            .resideInAPackage("..apps.backoffice..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("..apps.api..", "..domain.learning..", "..domain.catalog..", "..support..")
            .check(classes)
    }
}

