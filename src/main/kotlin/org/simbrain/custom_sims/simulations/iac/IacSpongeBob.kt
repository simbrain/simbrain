package org.simbrain.custom_sims.simulations.iac

import org.simbrain.custom_sims.SIM_WINDOW_GAP
import org.simbrain.custom_sims.addNetworkComponent
import org.simbrain.custom_sims.addSidebarInfo
import org.simbrain.custom_sims.newSim
import org.simbrain.util.place
import org.simbrain.util.point

/**
 * Student-built IAC network of SpongeBob SquarePants characters, ported from the Simbrain 3 workspace
 * SpongeBob_2015.
 */
val iacSpongeBob = newSim {

    workspace.clearWorkspace()

    val networkComponent = addNetworkComponent("SpongeBob")
    networkComponent.network.iacNetwork {
        val names = pool(
            "Names",
            "SpongeBob", "Patrick", "Squidward", "Krabs", "Sandy", "Plankton", "Pearl", "Karen Computer", "Mermaid Man", "Barnacle Boy", "Man Ray", "Dirty Bubble", "Flying Dutchman", "King Neptune", "Princess Mindy", "Patchy the Pirate",
            at = point(0, -170), columns = 8, hSpacing = 140.0
        )
        val married = pool(
            "Married",
            "No", "Yes", "Unknown",
            at = point(-750, -120), columns = 1
        )
        val residence = pool(
            "Residence",
            "Bikini Bottom", "Atlantis", "Human World",
            at = point(-750, 80), columns = 1
        )
        val morals = pool(
            "Morals",
            "Protagonist", "Hero", "Villain", "Neutral", "Antagonist",
            at = point(0, 200), columns = 5, hSpacing = 120.0
        )
        val occupation = pool(
            "Occupation",
            "Works at the Krusty Krab", "No Job", "Unknown", "Works at the Chumbucket", "Student", "Monarchy", "Retired", "Criminal", "Pirate",
            at = point(800, 0), columns = 1, vSpacing = 45.0
        )
        val characters = instances("Characters", at = point(0, 20), columns = 8, hSpacing = 140.0) {
            instance("SpongeBob", names["SpongeBob"], married["No"], residence["Bikini Bottom"], morals["Protagonist"], occupation["Works at the Krusty Krab"])
            instance("Patrick", names["Patrick"], married["No"], residence["Bikini Bottom"], morals["Protagonist"], occupation["No Job"])
            instance("Squidward", names["Squidward"], married["No"], residence["Bikini Bottom"], morals["Antagonist"], occupation["Works at the Krusty Krab"])
            instance("Krabs", names["Krabs"], married["Yes"], residence["Bikini Bottom"], morals["Neutral"], occupation["Works at the Krusty Krab"])
            instance("Sandy", names["Sandy"], married["No"], residence["Bikini Bottom"], morals["Protagonist"], occupation["Unknown"])
            instance("Plankton", names["Plankton"], married["Yes"], residence["Bikini Bottom"], morals["Antagonist"], occupation["Works at the Chumbucket"])
            instance("Pearl", names["Pearl"], married["No"], residence["Bikini Bottom"], morals["Neutral"], occupation["Student"])
            instance("Karen Computer", names["Karen Computer"], married["Yes"], residence["Bikini Bottom"], morals["Antagonist"], occupation["Works at the Chumbucket"])
            instance("Mermaid Man", names["Mermaid Man"], married["Unknown"], residence["Bikini Bottom"], morals["Hero"], occupation["Retired"])
            instance("Barnacle Boy", names["Barnacle Boy"], married["Unknown"], residence["Bikini Bottom"], morals["Hero"], occupation["Retired"])
            instance("Man Ray", names["Man Ray"], married["Unknown"], residence["Bikini Bottom"], morals["Villain"], occupation["Criminal"])
            instance("Dirty Bubble", names["Dirty Bubble"], married["Unknown"], residence["Bikini Bottom"], morals["Villain"], occupation["Criminal"])
            instance("Flying Dutchman", names["Flying Dutchman"], married["Unknown"], residence["Bikini Bottom"], morals["Villain"], occupation["Pirate"])
            instance("King Neptune", names["King Neptune"], married["Yes"], residence["Atlantis"], morals["Neutral"], occupation["Monarchy"])
            instance("Princess Mindy", names["Princess Mindy"], married["No"], residence["Atlantis"], morals["Hero"], occupation["Monarchy"])
            instance("Patchy the Pirate", names["Patchy the Pirate"], married["Unknown"], residence["Human World"], morals["Protagonist"], occupation["Unknown"])
        }
        connect(characters["Krabs"], occupation["Works at the Chumbucket"], -1.0)
    }

    withGui {
        place(networkComponent, SIM_WINDOW_GAP, SIM_WINDOW_GAP, 800, 700)
        addSidebarInfo(
            iacSidebarText(
                title = "SpongeBob SquarePants",
                body = """
                    An IAC network modeling the characters of SpongeBob SquarePants. It consists of five pools. The Names pool
                    contains a node for each character, and the remaining pools contain each character's marital status,
                    residence, occupation, and, most importantly, morals. The unlabelled nodes are instance nodes that tie
                    each character to their properties.

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
