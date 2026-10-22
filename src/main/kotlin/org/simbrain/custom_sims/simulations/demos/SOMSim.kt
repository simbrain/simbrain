/**
 * SOM demo based on the animal map of Ritter and Kohonen (1989): sixteen animals described by thirteen binary
 * features. Every animal's label is kept on the neuron that currently wins it, using [PrototypeLabeler], so the
 * labels show the map organizing into birds, hunters, and grazers as it trains.
 */
package org.simbrain.custom_sims.simulations.demos

import org.simbrain.custom_sims.*
import org.simbrain.network.core.activations
import org.simbrain.network.subnetworks.SOMNetwork
import org.simbrain.util.place
import org.simbrain.util.stats.distributions.UniformRealDistribution

val SOMSim = newSim {

    workspace.clearWorkspace()
    val networkComponent = addNetworkComponent("Network")
    val network = networkComponent.network

    val features = listOf(
        "small", "medium", "big", "2 legs", "4 legs", "hair", "hooves", "mane", "feathers",
        "hunts", "runs", "flies", "swims"
    )

    // Feature values from Ritter and Kohonen (1989), in the order of the features above
    val animals = linkedMapOf(
        "Dove" to listOf(1, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 1, 0),
        "Hen" to listOf(1, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0),
        "Duck" to listOf(1, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 1),
        "Goose" to listOf(1, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 1, 1),
        "Owl" to listOf(1, 0, 0, 1, 0, 0, 0, 0, 1, 1, 0, 1, 0),
        "Hawk" to listOf(1, 0, 0, 1, 0, 0, 0, 0, 1, 1, 0, 1, 0),
        "Eagle" to listOf(0, 1, 0, 1, 0, 0, 0, 0, 1, 1, 0, 1, 0),
        "Fox" to listOf(0, 1, 0, 0, 1, 1, 0, 0, 0, 1, 0, 0, 0),
        "Dog" to listOf(0, 1, 0, 0, 1, 1, 0, 0, 0, 0, 1, 0, 0),
        "Wolf" to listOf(0, 1, 0, 0, 1, 1, 0, 1, 0, 1, 1, 0, 0),
        "Cat" to listOf(1, 0, 0, 0, 1, 1, 0, 0, 0, 1, 0, 0, 0),
        "Tiger" to listOf(0, 0, 1, 0, 1, 1, 0, 0, 0, 1, 1, 0, 0),
        "Lion" to listOf(0, 0, 1, 0, 1, 1, 0, 1, 0, 1, 1, 0, 0),
        "Horse" to listOf(0, 0, 1, 0, 1, 1, 1, 1, 0, 0, 1, 0, 0),
        "Zebra" to listOf(0, 0, 1, 0, 1, 1, 1, 1, 0, 0, 1, 0, 0),
        "Cow" to listOf(0, 0, 1, 0, 1, 1, 1, 0, 0, 0, 0, 0, 0),
    ).mapValues { (_, values) -> values.map { it.toDouble() } }

    val som = SOMNetwork(features.size, 36)
    network.addNetworkModelAsync(som)
    som.inputLayer.neuronList.zip(features).forEach { (neuron, feature) -> neuron.label = feature }
    // Keep the features in one row so the labels read left to right
    som.inputLayer.gridThreshold = features.size + 1
    som.applySimulationLayout(inputNeuronInterval = SOM_SIM_INPUT_INTERVAL, somNeuronInterval = SOM_SIM_SOM_INTERVAL)
    som.inputLayer.setUpperBound(1.0)

    som.initialLearningRate = 0.3
    som.initNeighborhoodSize = 200.0
    som.learningDecayRate = 0.004
    som.neighborhoodDecayAmount = 0.2
    // Start weights in the same range as the binary features; Reset uses the same range
    val initialWeights = UniformRealDistribution(0.0, 1.0)
    som.randomize(initialWeights)
    som.reset()

    // Each animal's feature vector is its prototype. An animal is labeled once training has presented it.
    val labeler = PrototypeLabeler(som.som.neuronList, som::winnerFor)
    workspace.addUpdateAction("Label SOM nodes") { labeler.refresh() }

    var selectedAnimal = animals.keys.first()

    fun showSelectedAnimal() {
        som.inputLayer.neuronList.activations = animals.getValue(selectedAnimal)
    }

    // Put the selected animal on the input layer and light up the neuron that wins it, without learning
    fun testSelectedAnimal() {
        showSelectedAnimal()
        val winner = som.winnerFor(som.inputLayer.activationArray)
        som.som.neuronList.forEach { it.activation = if (it === winner) 1.0 else 0.0 }
    }

    suspend fun train(rounds: Int) {
        if (workspace.updater.isRunning) return
        repeat(rounds) {
            animals.entries.shuffled().forEach { (name, values) ->
                som.inputLayer.neuronList.activations = values
                labeler.addSample(name, values.toDoubleArray())
                workspace.iterateSuspend()
            }
        }
        testSelectedAnimal()
    }

    withGui {
        val controlPanel = createControlPanel("Control Panel", SIM_WINDOW_GAP, SIM_WINDOW_GAP) {
            addComboBox("Animal", animals.keys.toList(), selectedAnimal,
                labelToolTip = "Put an animal's features on the input nodes") {
                selectedAnimal = it
                showSelectedAnimal()
            }.toolTipText = "Put an animal's features on the input nodes"
            addButton("Test") { testSelectedAnimal() }.toolTipText =
                "Show which neuron wins the selected animal without changing any weights"

            addSeparator()

            addButton("Train All") { train(1) }.toolTipText =
                "Present all sixteen animals once, in random order (16 training steps)"
            addButton("Train All ×10") { train(10) }.toolTipText =
                "Present all sixteen animals in random order, ten times over (160 training steps)"

            addSeparator()

            addButton("Reset") {
                som.randomize(initialWeights)
                som.reset()
                labeler.clear()
                testSelectedAnimal()
            }.toolTipText = "Randomize the weights, restore the initial learning rate and neighborhood size, and clear the labels"
        }.awaitLayout()
        place(networkComponent, controlPanel.rightEdgeWithGap(), SIM_WINDOW_GAP, 680, 750)
    }
    testSelectedAnimal()

    addSidebarInfo(
        """
        # SOM Network (Animals)

        A Self-Organizing Map (SOM), also known as a Kohonen map, is an unsupervised network that arranges its
        inputs on a two-dimensional sheet of neurons so that similar inputs end up at nearby neurons. This
        simulation recreates a classic demonstration by Ritter and Kohonen (1989): sixteen animals, each described
        only by a list of features, sort themselves into birds, hunters, and grazers without the network ever being
        told those categories.

        <img src="//localfiles/simulations/images/somAnimals/SOM_Animals.png" width="300" alt="Ritter and Kohonen's animal map" />

        The map from Ritter and Kohonen (1989). The lines mark the regions for birds, hunters, and grazers. Their
        inputs also included a code identifying each animal, which is why `Owl` and `Hawk`, and `Horse` and
        `Zebra`, have neurons of their own there but share one here.

        # Simulation Details

        Each animal is a pattern of `13` binary features on the input layer: its size (`small`, `medium`, `big`),
        its body (`2 legs`, `4 legs`, `hair`, `hooves`, `mane`, `feathers`), and what it does (`hunts`, `runs`,
        `flies`, `swims`). For example, `Duck` is `small`, `2 legs`, `feathers`, and `swims`.

        Each SOM neuron has `13` weights, one per feature, and the winner for an input is the neuron whose weights
        are closest to it. During training, the winner and every neuron within the neighborhood, a radius in pixels
        around the winner, move their weights toward the input. Because neighbors learn together, nearby neurons
        come to respond to similar animals.

        The learning rate (starting at `0.3`) and the neighborhood size (starting at `200` pixels, which covers
        most of the map) shrink with every training step, shown as `Learning rate` and `N-size` below the network.
        Early training, with a wide neighborhood, lays out the broad regions. Later training, with a narrow
        neighborhood, gives similar animals neurons of their own.

        Each SOM neuron is labeled with the animals it currently wins. After every training step, each animal is
        checked against the current weights, so the labels move as the map organizes. `Owl` and `Hawk` have
        identical features, as do `Horse` and `Zebra`, so each pair always shares a neuron.

        ## Control Panel Settings

        - `Animal`: Put that animal's features on the input nodes.
        - `Test`: Light up the neuron that wins the selected animal, without changing any weights.
        - `Train All`: Present all sixteen animals once, in random order (`16` training steps).
        - `Train All ×10`: Present all sixteen animals in random order, ten times over (`160` training steps).
        - `Reset`: Randomize the weights, restore the initial learning rate and neighborhood size, and clear the
          labels.

        After `Train All`, `Train All ×10`, or `Reset`, the selected animal goes back on the input nodes and its winner
        lights up.

        # What to Do

        1. The map opens untrained, with random weights and no labels. Press `Train All` once: each animal is
           labeled as soon as training presents it, and at this point the animals are scattered or piled onto a
           few neurons in no particular order. Press `Reset` at any time to return to an untrained map.
        2. Press `Train All` a few more times and watch the labels. While the neighborhood is wide, whole groups of animals
           move together.
        3. Press `Train All ×10`. The birds, the hunters (`Fox`, `Dog`, `Wolf`, `Cat`, `Tiger`, `Lion`), and the
           hoofed grazers (`Horse`, `Zebra`, `Cow`) should now occupy separate regions of the map.
        4. Press `Train All ×10` two or three more times. The regions stay put while similar animals, such as `Tiger`
           and `Lion`, often move onto neighboring neurons of their own.
        5. Choose animals from `Animal` and press `Test` to see which neuron wins each one. Compare the features of animals that
           are neighbors on the map, such as `Duck` and `Goose`.
        6. Press `Reset` and train again. The regions land in different places each time, but the same groups
           form.

        ## Things to Try

        - Select a SOM neuron, right-click the SOM network's interaction box, and choose `Recall SOM memory`. The
          neuron's weights appear on the input nodes, showing the feature profile it has learned. Try an unlabeled
          neuron between two regions. Choose an animal from `Animal` to put a real animal back on the inputs.
        - Right-click the SOM network and choose its `Edit` item to change the learning parameters, then press
          `Reset` so they take effect. With `Initial Neighborhood size` set to `0`, only the winner learns: the
          animals pile up on a handful of neurons and most of the map is never used.
        - Choose an animal and press `Run` in the main toolbar to train on that animal alone, over and over. The
          neurons around its winner all move toward it, so animals that won nearby neurons may move elsewhere,
          and `Learning rate` and `N-size` keep shrinking while only one animal is being learned. Press `Stop`
          when you have seen enough.
        - The `SOM network (smell)` simulation applies the same ideas to smells sensed by a mouse moving through
          a world of cheeses and flowers.

        # References

        Kohonen, T. (1982). [_Self-organized formation of topologically correct feature maps_](https://doi.org/10.1007/BF00337288). _Biological Cybernetics_, _43_(1), 59-69.

        Ritter, H., & Kohonen, T. (1989). [_Self-organizing semantic maps_](https://doi.org/10.1007/BF00203171). _Biological Cybernetics_, _61_(4), 241-254.

        # Credits

        [Jeff Yoshimi](https://jeffyoshimi.net/index.html)

        """.trimIndent()
    )
}
