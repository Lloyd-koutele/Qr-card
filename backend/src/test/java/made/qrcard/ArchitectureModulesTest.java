package made.qrcard;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/** Sens autorise : admin -> organisation -> carte -> compte -> commun, et carnet -> carte. */
@AnalyzeClasses(packages = "made.qrcard", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureModulesTest
{

    @ArchTest
    static final ArchRule modules_sans_cycle = slices().matching("made.qrcard.(*)..").should().beFreeOfCycles();

    @ArchTest
    static final ArchRule commun_ne_connait_aucun_module = noClasses().that().resideInAPackage("made.qrcard.commun..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "made.qrcard.compte..", "made.qrcard.carte..", "made.qrcard.carnet..", "made.qrcard.organisation..", "made.qrcard.admin..");

    @ArchTest
    static final ArchRule compte_ne_connait_pas_les_modules_metier = noClasses().that().resideInAPackage("made.qrcard.compte..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "made.qrcard.carte..", "made.qrcard.carnet..", "made.qrcard.organisation..", "made.qrcard.admin..");

    @ArchTest
    static final ArchRule carte_ne_connait_pas_l_organisation = noClasses().that().resideInAPackage("made.qrcard.carte..")
            .should().dependOnClassesThat().resideInAnyPackage("made.qrcard.organisation..", "made.qrcard.admin..");

    /** Les objets remis a une organisation ne doivent pas pouvoir exposer un compte. */
    @ArchTest
    static final ArchRule dto_organisation_sans_compte = noClasses().that().resideInAPackage("made.qrcard.organisation.dto..")
            .should().dependOnClassesThat().resideInAPackage("made.qrcard.compte.domaine..");
}
