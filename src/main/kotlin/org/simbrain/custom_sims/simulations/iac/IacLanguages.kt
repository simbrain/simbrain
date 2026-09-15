package org.simbrain.custom_sims.simulations.iac

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext
import org.simbrain.custom_sims.*
import org.simbrain.util.place
import org.simbrain.util.point
import javax.swing.JInternalFrame

/**
 * Student-built IAC network classifying twenty languages by family, script, typology, and noun genders, ported
 * from the Simbrain 3 workspace Languages_2015.
 */
val iacLanguages = newSim {

    workspace.clearWorkspace()

    val networkComponent = addNetworkComponent("Language Classifier")
    networkComponent.network.iacNetwork {
        val family = pool(
            "Family",
            "Indo-European - Romance", "Indo-European - Germanic", "Indo-European - Slavic", "Indo-European - Others", "Semitic", "Other",
            at = point(-550, -40), columns = 1
        )
        val typology = pool(
            "Typology",
            "Fusional", "Agglutinative", "Analytic",
            at = point(700, -160), columns = 1
        )
        val genders = pool(
            "Noun Genders",
            "1", "2", "3",
            at = point(700, 120), columns = 1
        )
        val script = pool(
            "Script",
            "Latin", "Cyrillic", "Arabic / Abjad", "Logographic / Syllabic", "Own",
            at = point(0, 290), columns = 5, hSpacing = 180.0
        )
        instances("Languages", at = point(0, 0), columns = 5, hSpacing = 130.0, vSpacing = 75.0) {
            instance("Afrikaans", family["Indo-European - Germanic"], script["Latin"], typology["Analytic"], genders["1"])
            instance("Arabic", family["Semitic"], script["Arabic / Abjad"], typology["Fusional"], genders["2"])
            instance("Armenian", family["Indo-European - Others"], script["Own"], typology["Agglutinative"], genders["1"])
            instance("Basque", family["Other"], script["Latin"], typology["Agglutinative"], genders["2"])
            instance("Chinese", family["Other"], script["Logographic / Syllabic"], typology["Analytic"], genders["1"])
            instance("Czech", family["Indo-European - Slavic"], script["Latin"], typology["Fusional"], genders["3"])
            instance("Dutch", family["Indo-European - Germanic"], script["Latin"], typology["Fusional"], genders["2"])
            instance("English", family["Indo-European - Germanic"], script["Latin"], typology["Analytic"], genders["1"])
            instance("French", family["Indo-European - Romance"], script["Latin"], typology["Fusional"], genders["2"])
            instance("German", family["Indo-European - Germanic"], script["Latin"], typology["Fusional"], genders["3"])
            instance("Greek", family["Indo-European - Others"], script["Own"], typology["Fusional"], genders["3"])
            instance("Hebrew", family["Semitic"], script["Arabic / Abjad"], typology["Fusional"], genders["2"])
            instance("Japanese", family["Other"], script["Logographic / Syllabic"], typology["Agglutinative"], genders["1"])
            instance("Latin", family["Indo-European - Romance"], script["Latin"], typology["Fusional"], genders["3"])
            instance("Macedonian", family["Indo-European - Slavic"], script["Cyrillic"], typology["Analytic"], genders["3"])
            instance("Maltese", family["Semitic"], script["Latin"], typology["Fusional"], genders["2"])
            instance("Persian", family["Indo-European - Others"], script["Arabic / Abjad"], typology["Agglutinative"], genders["1"])
            instance("Serbian", family["Indo-European - Slavic"], script["Latin"], script["Cyrillic"], typology["Fusional"], genders["3"])
            instance("Spanish", family["Indo-European - Romance"], script["Latin"], typology["Fusional"], genders["2"])
            instance("Turkish", family["Other"], script["Latin"], typology["Agglutinative"], genders["1"])
        }

        typology["Fusional"].location = point(650, -180)
        typology["Agglutinative"].location = point(790, -160)
        typology["Analytic"].location = point(720, -70)
        genders["1"].location = point(650, 125)
        genders["2"].location = point(760, 110)
        genders["3"].location = point(700, 205)
    }

    withGui {
        getNetworkPanel(networkComponent).freeWeightsVisible = false
        place(networkComponent, SIM_WINDOW_GAP, SIM_WINDOW_GAP, 800, 550)
        withContext(Dispatchers.Swing) {
            (getDesktopComponent(networkComponent).parentFrame as JInternalFrame).isMaximum = true
        }
        addSidebarInfo(
            iacSidebarText(
                title = "Language Classifier",
                body = """
                    An IAC network modeling the categorization of 20 languages. Each language has a central instance node labelled
                    with its full name and linked directly to four property pools:

                    - [Language family](https://en.wikipedia.org/wiki/Language_family). Indo-European is split into
                    Romance, Germanic, Slavic, and Others; then Semitic and Other.
                    - [Script](https://en.wikipedia.org/wiki/Alphabet): Latin, Cyrillic, Arabic / Abjad, Logographic /
                    Syllabic (as for Chinese or Japanese), and Own for languages such as Greek or Armenian. Only Serbian
                    uses both Latin and Cyrillic.
                    - Typology: [Fusional](https://en.wikipedia.org/wiki/Fusional_language) like most Indo-European
                    languages, [Agglutinative](https://en.wikipedia.org/wiki/Agglutinative_language) like Japanese, and
                    [Analytic](https://en.wikipedia.org/wiki/Category:Analytic_languages) like Chinese, or even English and
                    Macedonian inasmuch as they are relatively more analytic.
                    - [Noun genders](https://en.wikipedia.org/wiki/List_of_languages_by_type_of_grammatical_genders): the
                    number of grammatical genders specifically of nouns. English has three genders for third person
                    singular pronouns but no noun gender, so it counts as 1.

                    Experiments reported by the author:

                    1. Activating Indo-European - Others correctly activated Armenian and Persian after 100 iterations, and
                    so Agglutinative and 1 gender. Greek was initially fairly active but, differing in typology and number
                    of genders, did not persist in the Languages pool.
                    2. Activating Logographic / Syllabic correctly matched Chinese and Japanese, both in the Other family with
                    1 noun gender, with Agglutinative activated for Japanese and Analytic for Chinese.
                    3. Activating Analytic together with 3 genders settled on Indo-European - Slavic with Macedonian and
                    Serbian. The best answer is just Macedonian, but Serbian is similar enough (three noun genders and
                    Cyrillic) that it was activated too, spreading activation to Fusional and Latin.
                    4. Activating individual languages (Basque, English, French, Maltese, Serbian, Spanish) recalled the
                    correct family, script, typology, and gender count for each.

                    The network has no trouble classifying particular languages. It works less well when asked to name
                    languages sharing a property, because the property pools overlap with many exceptions: Maltese uses
                    Latin script unlike other Semitic languages, English and Afrikaans are analytic while Dutch and German
                    are fusional, Czech uses Latin script while the other Slavic languages here use Cyrillic, and so on.
                    This could support the connectionist hypothesis that humans also remember well-classifiable data with
                    fewer exceptions more easily.
                """,
                credits = "Jozef Franzen, 2015. From Jeff Yoshimi's collection of standout student IAC networks."
            )
        )
    }
}
