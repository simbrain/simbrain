package org.simbrain.custom_sims.simulations.iac

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext
import org.simbrain.custom_sims.SIM_WINDOW_GAP
import org.simbrain.custom_sims.addNetworkComponent
import org.simbrain.custom_sims.addSidebarInfo
import org.simbrain.custom_sims.newSim
import org.simbrain.util.place
import org.simbrain.util.point
import javax.swing.JInternalFrame

/**
 * Reduced five-person fragment of the Jets and Sharks IAC network, ported from the Simbrain 3 workspace
 * IAC_JetsSharks_5People.
 */
val iacJetsSharks5People = newSim {

    workspace.clearWorkspace()

    val networkComponent = addNetworkComponent("Jets and Sharks (5 people)")
    val iac = networkComponent.network.iacNetwork(excitatory = 0.05, inhibitory = -0.03) {
        val names = pool(
            "Names",
            "Ralph", "Sam", "Lance", "Ned", "Rick",
            at = point(0, 0), columns = 5
        )
        val gang = pool(
            "Gang",
            "Sharks", "Jets",
            at = point(-350, 150), columns = 1
        )
        val age = pool(
            "Age",
            "20s", "30s", "40s",
            at = point(350, 150), columns = 1
        )
        val education = pool(
            "Education",
            "High School", "College", "Jr. High School",
            at = point(-350, 330), columns = 1
        )
        val marital = pool(
            "Marital Status",
            "Married", "Single", "Divorced",
            at = point(0, 330), columns = 3
        )
        val occupation = pool(
            "Occupation",
            "Bookie", "Burglar", "Pusher",
            at = point(350, 330), columns = 1
        )
        instances("People", at = point(0, 150), columns = 5) {
            instance(names["Ralph"], gang["Jets"], age["20s"], education["Jr. High School"], marital["Single"], occupation["Pusher"])
            instance(names["Sam"], gang["Jets"], age["20s"], education["Jr. High School"], marital["Married"], occupation["Bookie"])
            instance(names["Lance"], gang["Jets"], age["20s"], education["College"], marital["Single"], occupation["Bookie"])
            instance(names["Ned"], gang["Sharks"], age["30s"], education["College"], marital["Married"], occupation["Bookie"])
            instance(names["Rick"], gang["Sharks"], age["30s"], education["High School"], marital["Divorced"])
        }
    }

    // Original hand-placed arrangement, enlarged to leave room for pool titles and labels.
    val positions = listOf(
        point(0, 0), point(39, -34), point(97, -29), point(58, 27), point(112, 19),
        point(219, 163), point(214, 109),
        point(-119, 43), point(-162, 78), point(-104, 90),
        point(-163, 194), point(-202, 250), point(-148, 260),
        point(10, 341), point(67, 345), point(25, 388),
        point(223, 256), point(193, 313), point(266, 308),
        point(10, 143), point(49, 110), point(107, 122), point(67, 169), point(131, 180)
    )
    iac.neurons.zip(positions).forEach { (neuron, position) ->
        neuron.location = point(position.x * 2, position.y * 2)
    }

    withGui {
        place(networkComponent, SIM_WINDOW_GAP, SIM_WINDOW_GAP, 600, 600)
        withContext(Dispatchers.Swing) {
            (getDesktopComponent(networkComponent).parentFrame as JInternalFrame).isMaximum = true
        }
        addSidebarInfo(
            iacSidebarText(
                title = "Jets and Sharks (5 People)",
                body = """
                    A fragment of the classic McClelland Jets and Sharks IAC model, reduced to five people: Ralph, Sam,
                    Lance, Ned, and Rick. Because there are fewer nodes and links than in the full network, it is easier to
                    follow what happens on each step. Add activation to a person, or to any property, run the network, and
                    see what pattern it settles into. This models human associative memory. For more background see the
                    full Jets and Sharks simulation.

                    Separate name nodes are probably unnecessary: we usually label the central object (instance) nodes
                    directly. We retain the separate name pool here because it was part of the original model.
                """,
                credits = "[Jeff Yoshimi](https://jeffyoshimi.net/index.html)"
            )
        )
    }
}
