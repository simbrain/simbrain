package org.simbrain.custom_sims.simulations.iac

import org.simbrain.custom_sims.SIM_WINDOW_GAP
import org.simbrain.custom_sims.addNetworkComponent
import org.simbrain.custom_sims.addSidebarInfo
import org.simbrain.custom_sims.newSim
import org.simbrain.util.place
import org.simbrain.util.point

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
            at = point(0, -60), columns = 7
        )
        val gang = pool(
            "Gang",
            "Jets", "Sharks",
            at = point(-520, 150), columns = 1
        )
        val age = pool(
            "Age",
            "40's", "20's", "30's",
            at = point(-520, 320), columns = 1
        )
        val education = pool(
            "Education",
            "J.H.", "Col.", "H.S.",
            at = point(520, 150), columns = 1
        )
        val marital = pool(
            "Marital Status",
            "Single", "Married", "Divorced",
            at = point(520, 320), columns = 1
        )
        val occupation = pool(
            "Occupation",
            "Pusher", "Bookie", "Burglar",
            at = point(0, 420), columns = 3
        )
        instances("People", at = point(0, 200), columns = 7) {
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
    }

    withGui {
        place(networkComponent, SIM_WINDOW_GAP, SIM_WINDOW_GAP, 700, 700)
        addSidebarInfo(
            iacSidebarText(
                title = "Classic IAC Jets and Sharks Network",
                body = """
                    The network is from McClelland (1981) and models an agent's knowledge of the members of two gangs, the
                    Jets and the Sharks. Each person has a name, a gang, an age, an education level, a marital status, and an
                    occupation. The unlabelled nodes in the center are instance nodes, one per person, which link a name to
                    that person's properties. Nodes within each pool inhibit each other, so only one or a few nodes in each
                    pool tend to be active at a time.

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
