package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.addNetworkComponent
import org.simbrain.custom_sims.newSim
import org.simbrain.network.connections.RadialProbabilistic
import org.simbrain.network.connections.Sparse
import org.simbrain.network.core.*
import org.simbrain.network.layouts.GridLayout
import org.simbrain.network.updaterules.KuramotoRule
import org.simbrain.util.place
import org.simbrain.util.point
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Create a simulation of Cortex...
 */
val cortexKuramoto = newSim {

    // Basic setup
    workspace.clearWorkspace()
    val networkComponent = addNetworkComponent("Cortex Simulation")
    val network = networkComponent.network

    // Template for Kuramoto neuron
    fun Neuron.kuramotoTemplate() {
        updateRule = KuramotoRule().apply {
            slope = 100 * Math.random()
        }
        upperBound = 5.0
    }

    // Sparse connectivity
    val sparse = Sparse(connectionDensity = 0.1).apply {
        percentExcitatory = 20.0
    }

    // Radial connectivity
    val radial = RadialProbabilistic().apply {
        excitatoryRadius = 150.0
        excitatoryProbability = .4
        inhibitoryRadius = 150.0
        inhibitoryProbability = .9
    }

    // SUBNETWORKS
    // Based on Schmidt et al., 2015: https://bmcneurosci.biomedcentral.com/articles/10.1186/s12868-015-0193-z
    val numNetworks = 11
    val numNodesLowerBound = 11
    val numNodesUpperBound = 31
    val targetNumNodes = 219

    // For spacing regions apart in the GUI
    val xPositionIncrement = 1000
    val yPositionIncrement = 500
    var xCoordinateFactor = 1
    var xCoordinate = 0
    var yCoordinate = 0

    // For comparing against targetNumNodes (219)
    var currentNumNodes = 0

    // For iterating regions when connecting them
    var neuronRegionList: MutableList<AbstractNeuronCollection> = mutableListOf()                                       // https://www.educba.com/kotlin-empty-list/, https://stackoverflow.com/questions/46850554/kotlin-unresolved-reference-add-after-converting-from-java-code

    // For holding 11 random numbers of nodes (between 11 and 31 that sum to 219)
    var numNodesList: MutableList<Int> = mutableListOf()

    // For adjusting the random numbers to sum to 219
    var numNodesListAdjusted: MutableList<Int> = mutableListOf()

    // Generate 11 random numbers (of nodes) between 11 and 31 (that will eventually sum to 219)
    for (i in 1..numNetworks) {
        val numNodes = kotlin.random.Random.nextInt(numNodesLowerBound, numNodesUpperBound)                             // https://stackoverflow.com/questions/54340057/first-app-random-nextint-unresolved-reference
        currentNumNodes += numNodes
        numNodesList.add(numNodes)
    }

    // Adjust the numbers of nodes as needed to sum (close) to 219
    // Kinda hacky?
    for (i in numNodesList) {
        numNodesListAdjusted.add(((i.toDouble() / currentNumNodes) * targetNumNodes).roundToInt())                      // https://stackoverflow.com/questions/2640053/getting-n-random-numbers-whose-sum-is-m
    }
    currentNumNodes = numNodesListAdjusted.sum()
    println(numNodesListAdjusted) // debug
    println(currentNumNodes) // debug

    // Adjust the numbers of nodes as needed to sum (exactly) to 219 (because the sum from above can still be off by a few)
    // Maybe also kinda hacky
    while (currentNumNodes != targetNumNodes) {
        val difference = targetNumNodes - currentNumNodes
        var randomIndex = 0
        var randomValue: Int

        if (difference > 0) {
            do {
                randomIndex = kotlin.random.Random.nextInt(numNodesListAdjusted.size);                                  // https://www.baeldung.com/kotlin/list-get-random-item
                randomValue = numNodesListAdjusted[randomIndex]
            }
            while (numNodesUpperBound - randomValue < difference)
        } else if (difference < 0) {
            do {
                randomIndex = kotlin.random.Random.nextInt(numNodesListAdjusted.size);                                  // DRY
                randomValue = numNodesListAdjusted[randomIndex]
            }
            while (randomValue - numNodesLowerBound < abs(difference))
        }
        numNodesListAdjusted[randomIndex] += difference
        currentNumNodes += difference
    }
    println(numNodesListAdjusted) // debug
    println(currentNumNodes) // debug

    // Finally create neuron regions!
    for (i in 1..numNetworks) {
        val regionNeurons = network.addNeurons(numNodesListAdjusted[i - 1]) { kuramotoTemplate() }
        val region = NeuronCollection(regionNeurons)

        network.addNetworkModelAsync(region)
        neuronRegionList.add(region)

        // Increment GUI row after 3 neuron regions have been displayed
        if (xCoordinateFactor % 4 == 0) {
            yCoordinate += yPositionIncrement
            xCoordinateFactor = 1
        }

        // Format region
        region.apply {
            label = "Region ${i}"
            layout(GridLayout())
            location = point(xCoordinate + (xCoordinateFactor * xPositionIncrement), yCoordinate + yPositionIncrement)
        }

        //  Connect neurons within region
        radial.connectNeurons(regionNeurons, regionNeurons).addToNetwork(network)

        // Increment GUI column for next neuron region to be displayed (up to 3, per above)
        xCoordinateFactor += 1
    }

    // Make connections between regions
    for (i in neuronRegionList) {
        for (j in neuronRegionList) {

            // Don't connect to itself
            if (i !== j) {
                //val sg = SynapseGroup2(i, j, sparse)
                //sg.displaySynapses = true
                //network.addNetworkModel(sg)
                sparse.connectNeurons(i.neuronList, j.neuronList).addToNetwork(network)
            }
        }
    }

    // Location of the network in the desktop
    withGui {
        place(networkComponent) {
            location = point(0, 0)
            width = 400
            height = 400
        }
    }
}
