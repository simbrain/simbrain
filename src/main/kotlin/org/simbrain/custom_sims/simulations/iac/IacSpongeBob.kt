package org.simbrain.custom_sims.simulations.iac

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext
import org.simbrain.custom_sims.*
import org.simbrain.util.place
import org.simbrain.util.point
import javax.swing.JInternalFrame

/**
 * Student-built IAC network of SpongeBob SquarePants characters, ported from the Simbrain 3 workspace
 * SpongeBob_2015.
 */
val iacSpongeBob = newSim {

    workspace.clearWorkspace()

    val networkComponent = addNetworkComponent("SpongeBob")
    networkComponent.network.iacNetwork {
        val married = pool(
            "Married",
            "No", "Yes", "Unknown",
            at = point(-440, -110), columns = 1
        )
        val residence = pool(
            "Residence",
            "Bikini Bottom", "Atlantis", "Human World",
            at = point(-440, 160), columns = 1
        )
        val morals = pool(
            "Morals",
            "Protagonist", "Hero", "Villain", "Neutral", "Antagonist",
            at = point(0, 290), columns = 5, hSpacing = 120.0
        )
        val occupation = pool(
            "Occupation",
            "Works at the Krusty Krab", "No Job", "Unknown", "Works at the Chumbucket", "Student", "Monarchy", "Retired", "Criminal", "Pirate",
            at = point(480, 0), columns = 1, vSpacing = 45.0
        )
        val characters = instances("Characters", at = point(0, 0), columns = 4, hSpacing = 150.0, vSpacing = 80.0) {
            instance("SpongeBob", married["No"], residence["Bikini Bottom"], morals["Protagonist"], occupation["Works at the Krusty Krab"])
            instance("Patrick", married["No"], residence["Bikini Bottom"], morals["Protagonist"], occupation["No Job"])
            instance("Squidward", married["No"], residence["Bikini Bottom"], morals["Antagonist"], occupation["Works at the Krusty Krab"])
            instance("Krabs", married["Yes"], residence["Bikini Bottom"], morals["Neutral"], occupation["Works at the Krusty Krab"])
            instance("Sandy", married["No"], residence["Bikini Bottom"], morals["Protagonist"], occupation["Unknown"])
            instance("Plankton", married["Yes"], residence["Bikini Bottom"], morals["Antagonist"], occupation["Works at the Chumbucket"])
            instance("Pearl", married["No"], residence["Bikini Bottom"], morals["Neutral"], occupation["Student"])
            instance("Karen Computer", married["Yes"], residence["Bikini Bottom"], morals["Antagonist"], occupation["Works at the Chumbucket"])
            instance("Mermaid Man", married["Unknown"], residence["Bikini Bottom"], morals["Hero"], occupation["Retired"])
            instance("Barnacle Boy", married["Unknown"], residence["Bikini Bottom"], morals["Hero"], occupation["Retired"])
            instance("Man Ray", married["Unknown"], residence["Bikini Bottom"], morals["Villain"], occupation["Criminal"])
            instance("Dirty Bubble", married["Unknown"], residence["Bikini Bottom"], morals["Villain"], occupation["Criminal"])
            instance("Flying Dutchman", married["Unknown"], residence["Bikini Bottom"], morals["Villain"], occupation["Pirate"])
            instance("King Neptune", married["Yes"], residence["Atlantis"], morals["Neutral"], occupation["Monarchy"])
            instance("Princess Mindy", married["No"], residence["Atlantis"], morals["Hero"], occupation["Monarchy"])
            instance("Patchy the Pirate", married["Unknown"], residence["Human World"], morals["Protagonist"], occupation["Unknown"])
        }
        connect(characters["Krabs"], occupation["Works at the Chumbucket"], -1.0)
    }

    withGui {
        getNetworkPanel(networkComponent).freeWeightsVisible = false
        place(networkComponent, SIM_WINDOW_GAP, SIM_WINDOW_GAP, 800, 700)
        withContext(Dispatchers.Swing) {
            (getDesktopComponent(networkComponent).parentFrame as JInternalFrame).isMaximum = true
        }
        addSidebarInfo(
            iacSidebarText(
                title = "SpongeBob SquarePants",
                body = """
                    An IAC network modeling the characters of SpongeBob SquarePants. Its five pools contain the
                    characters and their marital status, residence, occupation, and morals. The central instance nodes
                    are labelled directly with character names and connect to their properties.

                    Activating the Protagonist node results in the correct generalization to SpongeBob, Patrick, and Sandy.
                    Activating the Atlantis residence node correctly identifies King Neptune and Princess Mindy. When either
                    Plankton or Karen Computer is activated, the other's instance node also shows some activation, because
                    the two are closely related in their characteristics.
                """,
                credits = "Juanita Sprowell, 2015. From Jeff Yoshimi's collection of standout student IAC networks."
            )
        )
    }
}
