@file:JvmName("ScreenElementActions")
package org.simbrain.network.gui.nodes

import org.simbrain.util.createAction

fun SubnetworkNode.createRandomNetAction() = createAction(
    name = "Randomize synapses symmetrically",
) {
    subnetwork.randomize()
}
