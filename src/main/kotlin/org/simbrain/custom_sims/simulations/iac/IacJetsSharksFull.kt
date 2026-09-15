package org.simbrain.custom_sims.simulations.iac

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext
import org.simbrain.custom_sims.*
import org.simbrain.util.place
import org.simbrain.util.point
import javax.swing.JInternalFrame

/**
 * The classic McClelland (1981) Jets and Sharks IAC network, ported from the Simbrain 3 workspace IAC_Full.
 */
val iacJetsSharksFull = newSim {

    workspace.clearWorkspace()

    val networkComponent = addNetworkComponent("Jets and Sharks")
    networkComponent.network.iacNetwork(excitatory = 0.05, inhibitory = -0.03, lowerBound = -0.2) {
        val names = pool(
            "Names",
            "Jim", "Greg", "Ken", "Don", "Nick", "Ralph", "Karl", "Neal", "Fred", "Gene", "Phil", "Art", "Al", "Sam", "Clyde", "Pete", "Ol", "Ned", "Dave", "Doug", "John", "Rick", "Ike", "Lance", "Mike", "George", "Earl",
            at = point(0, -100), columns = 7
        )
        val gang = pool(
            "Gang",
            "Jets", "Sharks",
            at = point(-540, -90), columns = 1
        )
        val age = pool(
            "Age",
            "40's", "20's", "30's",
            at = point(-540, 280), columns = 1
        )
        val education = pool(
            "Education",
            "J.H.", "Col.", "H.S.",
            at = point(540, 80), columns = 1
        )
        val marital = pool(
            "Marital Status",
            "Single", "Married", "Divorced",
            at = point(560, 370), columns = 1
        )
        val occupation = pool(
            "Occupation",
            "Pusher", "Bookie", "Burglar",
            at = point(0, 540), columns = 3
        )
        instances("People", at = point(0, 280), columns = 7) {
            instance(names["Jim"], gang["Jets"], age["20's"], education["J.H."], marital["Divorced"], occupation["Burglar"])
            instance(names["Greg"], gang["Jets"], age["20's"], education["H.S."], marital["Married"], occupation["Pusher"])
            instance(names["Ken"], gang["Sharks"], age["20's"], education["H.S."], marital["Single"], occupation["Burglar"])
            instance(names["Don"], gang["Sharks"], age["30's"], education["Col."], marital["Married"], occupation["Burglar"])
            instance(names["Nick"], gang["Sharks"], age["30's"], education["H.S."], marital["Single"], occupation["Pusher"])
            instance(names["Ralph"], gang["Jets"], age["30's"], education["J.H."], marital["Single"], occupation["Pusher"])
            instance(names["Karl"], gang["Sharks"], age["40's"], education["H.S."], marital["Married"], occupation["Burglar"])
            instance(names["Neal"], gang["Sharks"], age["30's"], education["H.S."], marital["Single"], occupation["Bookie"])
            instance(names["Fred"], gang["Jets"], age["20's"], education["H.S."], marital["Single"], occupation["Pusher"])
            instance(names["Gene"], gang["Jets"], age["20's"], education["Col."], marital["Single"], occupation["Pusher"])
            instance(names["Phil"], gang["Sharks"], age["30's"], education["Col."], marital["Married"], occupation["Pusher"])
            instance(names["Art"], gang["Jets"], age["40's"], education["J.H."], marital["Single"], occupation["Pusher"])
            instance(names["Al"], gang["Jets"], age["30's"], education["J.H."], marital["Married"], occupation["Burglar"])
            instance(names["Sam"], gang["Jets"], age["20's"], education["Col."], marital["Single"], occupation["Bookie"])
            instance(names["Clyde"], gang["Jets"], age["40's"], education["J.H."], marital["Single"], occupation["Bookie"])
            instance(names["Pete"], gang["Jets"], age["20's"], education["H.S."], marital["Single"], occupation["Bookie"])
            instance(names["Ol"], gang["Sharks"], age["30's"], education["Col."], marital["Married"], occupation["Pusher"])
            instance(names["Ned"], gang["Sharks"], age["30's"], education["Col."], marital["Married"], occupation["Bookie"])
            instance(names["Dave"], gang["Sharks"], age["30's"], education["H.S."], marital["Divorced"], occupation["Pusher"])
            instance(names["Doug"], gang["Jets"], age["30's"], education["H.S."], marital["Single"], occupation["Bookie"])
            instance(names["John"], gang["Jets"], age["20's"], education["J.H."], marital["Married"], occupation["Burglar"])
            instance(names["Rick"], gang["Sharks"], age["30's"], education["H.S."], marital["Divorced"], occupation["Burglar"])
            instance(names["Ike"], gang["Sharks"], age["30's"], education["J.H."], marital["Single"], occupation["Bookie"])
            instance(names["Lance"], gang["Jets"], age["20's"], education["J.H."], marital["Married"], occupation["Burglar"])
            instance(names["Mike"], gang["Jets"], age["30's"], education["J.H."], marital["Single"], occupation["Bookie"])
            instance(names["George"], gang["Jets"], age["20's"], education["J.H."], marital["Divorced"], occupation["Burglar"])
            instance(names["Earl"], gang["Sharks"], age["40's"], education["H.S."], marital["Married"], occupation["Burglar"])
        }

        age["20's"].location = point(-575, 280)
        age["30's"].location = point(-505, 345)
        age["40's"].location = point(-510, 225)
        education["Col."].location = point(500, 50)
        education["J.H."].location = point(590, 35)
        education["H.S."].location = point(540, 130)
        marital["Married"].location = point(510, 335)
        marital["Single"].location = point(600, 320)
        marital["Divorced"].location = point(560, 410)
        occupation["Pusher"].location = point(-60, 515)
        occupation["Bookie"].location = point(65, 505)
        occupation["Burglar"].location = point(0, 585)
    }

    withGui {
        getNetworkPanel(networkComponent).freeWeightsVisible = false
        place(networkComponent, SIM_WINDOW_GAP, SIM_WINDOW_GAP, 700, 700)
        withContext(Dispatchers.Swing) {
            (getDesktopComponent(networkComponent).parentFrame as JInternalFrame).isMaximum = true
        }
        addSidebarInfo(
            iacSidebarText(
                title = "Classic IAC Jets and Sharks Network",
                body = """
                    The network is from McClelland (1981) and models an agent's knowledge of the members of two gangs, the
                    Jets and the Sharks. Each person has a name, a gang, an age, an education level, a marital status, and an
                    occupation. The unlabelled nodes in the center are instance nodes, one per person, which link a name to
                    that person's properties. Nodes within each pool inhibit each other, so only one or a few nodes in each
                    pool tend to be active at a time.

                    Separate name nodes are probably unnecessary: we usually label the central object (instance) nodes
                    directly. We retain the separate name pool here because it was part of the original model.

                    Activate a name to retrieve that person's properties. Activate a gang node, like Jets, to see who the
                    Jets are and what they tend to be like, or activate a property like Burglar to see who fits it. Note
                    that for reasons that are unclear to us this network does not produce exactly the behavior described in
                    the paper, but it comes close.
                """,
                credits = "Built by Saraching Chao and [Jeff Yoshimi](https://jeffyoshimi.net/index.html)."
            )
        )
    }
}
