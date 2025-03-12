package org.simbrain.custom_sims.simulations.patterns_of_activity

import org.simbrain.custom_sims.addNetworkComponent
import org.simbrain.custom_sims.addSidebarInfo
import org.simbrain.custom_sims.newSim
import org.simbrain.network.core.addNeuron
import org.simbrain.network.core.addSynapse
import org.simbrain.util.place
import org.simbrain.util.point

/**
 * Generic 3 object -> recurrent net example using neuron array
 */
val iacJetsSharks5People = newSim {

    workspace.clearWorkspace()

    //workspace.clearWorkspace()
    val networkComponent = addNetworkComponent("Network")
    val network = networkComponent.network

    val neuronList = buildList {
        add(network.addNeuron {
            location = point(0.2961309523809524, 0.2961309523809524)
            label = "Ralph"
        })
        add(network.addNeuron {
            location = point(38.56366322937623, -33.85811396154705)
            label = "Sam"
        })
        add(network.addNeuron {
            location = point(97.18873658618377, -29.187583836351443)
            label = "Lance"
        })
        add(network.addNeuron {
            location = point(57.74881581153585, 26.63748463000222)
            label = "Ned"
        })
        add(network.addNeuron {
            location = point(112.27529761904759, 18.712797619047606)
            label = "Rick"
        })
        add(network.addNeuron {
            location = point(10.033378429106733, 142.91106675760935)
            label = ""
        })
        add(network.addNeuron {
            location = point(49.12628895136614, 109.58220008894541)
            label = ""
        })
        add(network.addNeuron {
            location = point(106.92598406290948, 122.32843132084476)
            label = ""
        })
        add(network.addNeuron {
            location = point(67.48606328826159, 169.25242043523056)
            label = ""
        })
        add(network.addNeuron {
            location = point(131.0166548686353, 180.15450840389627)
            label = ""
        })
        add(network.addNeuron {
            location = point(219.2977049998131, 162.56762394623172)
            label = "Sharks"
        })
        add(network.addNeuron {
            location = point(214.39923415795664, 109.40299317879445)
            label = "Jets"
        })
        add(network.addNeuron {
            location = point(-119.06926814112705, 42.77463699775413)
            label = "20s"
        })
        add(network.addNeuron {
            location = point(-161.70391122882748, 77.83401042926663)
            label = "30s"
        })
        add(network.addNeuron {
            location = point(-103.98270710826323, 89.62371510269357)
            label = "40s"
        })
        add(network.addNeuron {
            location = point(9.797318667214723, 341.12480005815166)
            label = "Married"
        })
        add(network.addNeuron {
            location = point(66.92467610483637, 344.69020546547597)
            label = "Single"
        })
        add(network.addNeuron {
            location = point(-162.88582666138197, 194.4217482896226)
            label = "High School"
        })
        add(network.addNeuron {
            location = point(-202.32574743602999, 250.2468167559762)
            label = "College"
        })
        add(network.addNeuron {
            location = point(-147.8516093321983, 259.54897418550746)
            label = "Jr. High School"
        })
        add(network.addNeuron {
            location = point(222.67123194062094, 256.40134457803623)
            label = "Bookie"
        })
        add(network.addNeuron {
            location = point(192.88837477831513, 313.0044463064119)
            label = "Burglar"
        })
        add(network.addNeuron {
            location = point(265.6454754499517, 308.17177841411683)
            label = "Pusher"
        })
        add(network.addNeuron {
            location = point(24.842201556015027, 387.9100631095202)
            label = "Divorced"
        })

    }

    val synapses = buildList {
        add(network.addSynapse(neuronList[0], neuronList[1]) {
            strength = -0.03
        })
        add(network.addSynapse(neuronList[0], neuronList[2]) {
            strength = -0.03
        })
        add(network.addSynapse(neuronList[0], neuronList[3]) {
            strength = -0.03
        })
        add(network.addSynapse(neuronList[0], neuronList[4]) {
            strength = -0.03
        })
        add(network.addSynapse(neuronList[1], neuronList[0]) {
            strength = -0.03
        })
        add(network.addSynapse(neuronList[1], neuronList[2]) {
            strength = -0.03
        })
        add(network.addSynapse(neuronList[1], neuronList[3]) {
            strength = -0.03
        })
        add(network.addSynapse(neuronList[1], neuronList[4]) {
            strength = -0.03
        })
        add(network.addSynapse(neuronList[2], neuronList[0]) {
            strength = -0.03
        })
        add(network.addSynapse(neuronList[2], neuronList[1]) {
            strength = -0.03
        })
        add(network.addSynapse(neuronList[2], neuronList[3]) {
            strength = -0.03
        })
        add(network.addSynapse(neuronList[2], neuronList[4]) {
            strength = -0.03
        })
        add(network.addSynapse(neuronList[3], neuronList[0]) {
            strength = -0.03
        })
        add(network.addSynapse(neuronList[3], neuronList[1]) {
            strength = -0.03
        })
        add(network.addSynapse(neuronList[3], neuronList[2]) {
            strength = -0.03
        })
        add(network.addSynapse(neuronList[3], neuronList[4]) {
            strength = -0.03
        })
        add(network.addSynapse(neuronList[4], neuronList[0]) {
            strength = -0.03
        })
        add(network.addSynapse(neuronList[4], neuronList[1]) {
            strength = -0.03
        })
        add(network.addSynapse(neuronList[4], neuronList[2]) {
            strength = -0.03
        })
        add(network.addSynapse(neuronList[4], neuronList[3]) {
            strength = -0.03
        })
        add(network.addSynapse(neuronList[9], neuronList[8]) {
            strength = -0.03
        })
        add(network.addSynapse(neuronList[9], neuronList[7]) {
            strength = -0.03
        })
        add(network.addSynapse(neuronList[9], neuronList[6]) {
            strength = -0.03
        })
        add(network.addSynapse(neuronList[9], neuronList[5]) {
            strength = -0.03
        })
        add(network.addSynapse(neuronList[8], neuronList[9]) {
            strength = -0.03
        })
        add(network.addSynapse(neuronList[8], neuronList[7]) {
            strength = -0.03
        })
        add(network.addSynapse(neuronList[8], neuronList[6]) {
            strength = -0.03
        })
        add(network.addSynapse(neuronList[8], neuronList[5]) {
            strength = -0.03
        })
        add(network.addSynapse(neuronList[7], neuronList[9]) {
            strength = -0.03
        })
        add(network.addSynapse(neuronList[7], neuronList[8]) {
            strength = -0.03
        })
        add(network.addSynapse(neuronList[7], neuronList[6]) {
            strength = -0.03
        })
        add(network.addSynapse(neuronList[7], neuronList[5]) {
            strength = -0.03
        })
        add(network.addSynapse(neuronList[6], neuronList[9]) {
            strength = -0.03
        })
        add(network.addSynapse(neuronList[6], neuronList[8]) {
            strength = -0.03
        })
        add(network.addSynapse(neuronList[6], neuronList[7]) {
            strength = -0.03
        })
        add(network.addSynapse(neuronList[6], neuronList[5]) {
            strength = -0.03
        })
        add(network.addSynapse(neuronList[5], neuronList[9]) {
            strength = -0.03
        })
        add(network.addSynapse(neuronList[5], neuronList[8]) {
            strength = -0.03
        })
        add(network.addSynapse(neuronList[5], neuronList[7]) {
            strength = -0.03
        })
        add(network.addSynapse(neuronList[5], neuronList[6]) {
            strength = -0.03
        })
        add(network.addSynapse(neuronList[11], neuronList[10]) {
            strength = -0.03
        })
        add(network.addSynapse(neuronList[10], neuronList[11]) {
            strength = -0.03
        })
        add(network.addSynapse(neuronList[14], neuronList[13]) {
            strength = -0.03
        })
        add(network.addSynapse(neuronList[14], neuronList[12]) {
            strength = -0.03
        })
        add(network.addSynapse(neuronList[13], neuronList[14]) {
            strength = -0.03
        })
        add(network.addSynapse(neuronList[13], neuronList[12]) {
            strength = -0.03
        })
        add(network.addSynapse(neuronList[12], neuronList[14]) {
            strength = -0.03
        })
        add(network.addSynapse(neuronList[12], neuronList[13]) {
            strength = -0.03
        })
        add(network.addSynapse(neuronList[15], neuronList[16]) {
            strength = -0.03
        })
        add(network.addSynapse(neuronList[16], neuronList[15]) {
            strength = -0.03
        })
        add(network.addSynapse(neuronList[17], neuronList[18]) {
            strength = -0.03
        })
        add(network.addSynapse(neuronList[17], neuronList[19]) {
            strength = -0.03
        })
        add(network.addSynapse(neuronList[18], neuronList[17]) {
            strength = -0.03
        })
        add(network.addSynapse(neuronList[18], neuronList[19]) {
            strength = -0.03
        })
        add(network.addSynapse(neuronList[19], neuronList[17]) {
            strength = -0.03
        })
        add(network.addSynapse(neuronList[19], neuronList[18]) {
            strength = -0.03
        })
        add(network.addSynapse(neuronList[1], neuronList[6]) {
            strength = 0.05
        })
        add(network.addSynapse(neuronList[6], neuronList[1]) {
            strength = 0.05
        })
        add(network.addSynapse(neuronList[6], neuronList[11]) {
            strength = 0.05
        })
        add(network.addSynapse(neuronList[11], neuronList[6]) {
            strength = 0.05
        })
        add(network.addSynapse(neuronList[6], neuronList[12]) {
            strength = 0.05
        })
        add(network.addSynapse(neuronList[12], neuronList[6]) {
            strength = 0.05
        })
        add(network.addSynapse(neuronList[6], neuronList[15]) {
            strength = 0.05
        })
        add(network.addSynapse(neuronList[15], neuronList[6]) {
            strength = 0.05
        })
        add(network.addSynapse(neuronList[6], neuronList[19]) {
            strength = 0.05
        })
        add(network.addSynapse(neuronList[19], neuronList[6]) {
            strength = 0.05
        })
        add(network.addSynapse(neuronList[22], neuronList[21]) {
            strength = -0.03
        })
        add(network.addSynapse(neuronList[22], neuronList[20]) {
            strength = -0.03
        })
        add(network.addSynapse(neuronList[21], neuronList[22]) {
            strength = -0.03
        })
        add(network.addSynapse(neuronList[21], neuronList[20]) {
            strength = -0.03
        })
        add(network.addSynapse(neuronList[20], neuronList[22]) {
            strength = -0.03
        })
        add(network.addSynapse(neuronList[20], neuronList[21]) {
            strength = -0.03
        })
        add(network.addSynapse(neuronList[6], neuronList[20]) {
            strength = 0.05
        })
        add(network.addSynapse(neuronList[20], neuronList[6]) {
            strength = 0.05
        })
        add(network.addSynapse(neuronList[2], neuronList[7]) {
            strength = 0.05
        })
        add(network.addSynapse(neuronList[7], neuronList[2]) {
            strength = 0.05
        })
        add(network.addSynapse(neuronList[7], neuronList[11]) {
            strength = 0.05
        })
        add(network.addSynapse(neuronList[11], neuronList[7]) {
            strength = 0.05
        })
        add(network.addSynapse(neuronList[7], neuronList[12]) {
            strength = 0.05
        })
        add(network.addSynapse(neuronList[12], neuronList[7]) {
            strength = 0.05
        })
        add(network.addSynapse(neuronList[7], neuronList[18]) {
            strength = 0.05
        })
        add(network.addSynapse(neuronList[18], neuronList[7]) {
            strength = 0.05
        })
        add(network.addSynapse(neuronList[7], neuronList[16]) {
            strength = 0.05
        })
        add(network.addSynapse(neuronList[16], neuronList[7]) {
            strength = 0.05
        })
        add(network.addSynapse(neuronList[7], neuronList[20]) {
            strength = 0.05
        })
        add(network.addSynapse(neuronList[20], neuronList[7]) {
            strength = 0.05
        })
        add(network.addSynapse(neuronList[0], neuronList[5]) {
            strength = 0.05
        })
        add(network.addSynapse(neuronList[5], neuronList[0]) {
            strength = 0.05
        })
        add(network.addSynapse(neuronList[5], neuronList[11]) {
            strength = 0.05
        })
        add(network.addSynapse(neuronList[11], neuronList[5]) {
            strength = 0.05
        })
        add(network.addSynapse(neuronList[5], neuronList[12]) {
            strength = 0.05
        })
        add(network.addSynapse(neuronList[12], neuronList[5]) {
            strength = 0.05
        })
        add(network.addSynapse(neuronList[5], neuronList[22]) {
            strength = 0.05
        })
        add(network.addSynapse(neuronList[22], neuronList[5]) {
            strength = 0.05
        })
        add(network.addSynapse(neuronList[5], neuronList[16]) {
            strength = 0.05
        })
        add(network.addSynapse(neuronList[16], neuronList[5]) {
            strength = 0.05
        })
        add(network.addSynapse(neuronList[5], neuronList[19]) {
            strength = 0.05
        })
        add(network.addSynapse(neuronList[19], neuronList[5]) {
            strength = 0.05
        })
        add(network.addSynapse(neuronList[3], neuronList[8]) {
            strength = 0.05
        })
        add(network.addSynapse(neuronList[8], neuronList[3]) {
            strength = 0.05
        })
        add(network.addSynapse(neuronList[8], neuronList[10]) {
            strength = 0.05
        })
        add(network.addSynapse(neuronList[10], neuronList[8]) {
            strength = 0.05
        })
        add(network.addSynapse(neuronList[8], neuronList[13]) {
            strength = 0.05
        })
        add(network.addSynapse(neuronList[13], neuronList[8]) {
            strength = 0.05
        })
        add(network.addSynapse(neuronList[8], neuronList[18]) {
            strength = 0.05
        })
        add(network.addSynapse(neuronList[18], neuronList[8]) {
            strength = 0.05
        })
        add(network.addSynapse(neuronList[8], neuronList[15]) {
            strength = 0.05
        })
        add(network.addSynapse(neuronList[15], neuronList[8]) {
            strength = 0.05
        })
        add(network.addSynapse(neuronList[8], neuronList[20]) {
            strength = 0.05
        })
        add(network.addSynapse(neuronList[20], neuronList[8]) {
            strength = 0.05
        })
        add(network.addSynapse(neuronList[4], neuronList[9]) {
            strength = 0.05
        })
        add(network.addSynapse(neuronList[9], neuronList[4]) {
            strength = 0.05
        })
        add(network.addSynapse(neuronList[9], neuronList[10]) {
            strength = 0.05
        })
        add(network.addSynapse(neuronList[10], neuronList[9]) {
            strength = 0.05
        })
        add(network.addSynapse(neuronList[9], neuronList[13]) {
            strength = 0.05
        })
        add(network.addSynapse(neuronList[13], neuronList[9]) {
            strength = 0.05
        })
        add(network.addSynapse(neuronList[9], neuronList[17]) {
            strength = 0.05
        })
        add(network.addSynapse(neuronList[17], neuronList[9]) {
            strength = 0.05
        })
        add(network.addSynapse(neuronList[9], neuronList[23]) {
            strength = 0.05
        })
        add(network.addSynapse(neuronList[23], neuronList[9]) {
            strength = 0.05
        })
        add(network.addSynapse(neuronList[23], neuronList[15]) {
            strength = -0.03
        })
        add(network.addSynapse(neuronList[15], neuronList[23]) {
            strength = -0.03
        })
        add(network.addSynapse(neuronList[23], neuronList[16]) {
            strength = -0.03
        })
        add(network.addSynapse(neuronList[16], neuronList[23]) {
            strength = -0.03
        })

    }


    withGui{
        place(networkComponent, 0, 0, 600, 600)
        addSidebarInfo(
            """
                # Jets and Sharks
                A fragment of the classic McClelland Jets and Sharks IAC Model.  This is a fragment of the full
                [IAC](https://en.wikipedia.org/wiki/Interactive_activation_and_competition_networks) model
                described in the documentation for IAC_Full. Because there are fewer nodes and links, this one runs
                faster.  To see it in action, add activation to a person, or to any propery, run the network, and see what
                pattern it settles in to. This models human associative memory. Again, for more info, see the documentation
                for IAC_Full.
            """.trimIndent()
        )
    }

}