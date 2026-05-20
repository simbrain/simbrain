package org.simbrain.custom_sims.simulations

import org.json.JSONObject
import org.simbrain.custom_sims.addNetworkComponent
import org.simbrain.custom_sims.addSidebarInfo
import org.simbrain.custom_sims.newSim
import org.simbrain.network.core.SynapseGroup
import org.simbrain.network.core.addNeuronCollection
import org.simbrain.network.trainers.*
import org.simbrain.network.updaterules.SigmoidalRule
import org.simbrain.network.util.Alignment
import org.simbrain.network.util.Direction
import org.simbrain.network.util.alignNetworkModels
import org.simbrain.network.util.offsetNetworkModel
import org.simbrain.util.place


val xorSim = newSim { optionString ->

    workspace.clearWorkspace()
    val networkComponent = addNetworkComponent("XOR")
    val net = networkComponent.network

    val inputLayer = net.addNeuronCollection(2).apply {
        isClamped = true
    }
    val hiddenLayer = net.addNeuronCollection(2) { updateRule = SigmoidalRule() }
    val outputLayer = net.addNeuronCollection(1) { updateRule = SigmoidalRule() }
    val sg1 = SynapseGroup(inputLayer, hiddenLayer)
    val sg2 = SynapseGroup(hiddenLayer, outputLayer)
    val sm = SupervisedModel(inputLayer, outputLayer).apply {
        trainerConfig.weightInitializationStrategy = Xavier()
        trainerConfig.optimizer = BasicOptimizer(momentum = 0.9)
        trainerConfig.learningRate = 0.1
    }
    net.addNetworkModels(inputLayer, hiddenLayer, outputLayer, sg1, sg2, sm)
    offsetNetworkModel(inputLayer, hiddenLayer, Direction.NORTH, 150.0)
    offsetNetworkModel(hiddenLayer, outputLayer, Direction.NORTH, 150.0)
    alignNetworkModels(inputLayer, hiddenLayer, Alignment.VERTICAL)
    alignNetworkModels(inputLayer, outputLayer, Alignment.VERTICAL)
    sm.randomize()

    sm.trainingSet = TrainingDataset(
        inputs = mutableListOf(
            mutableListOf(0.0, 0.0),
            mutableListOf(1.0, 0.0),
            mutableListOf(0.0, 1.0),
            mutableListOf(1.0, 1.0)
        ),
        targets = mutableListOf(
            mutableListOf(0.0),
            mutableListOf(1.0),
            mutableListOf(1.0),
            mutableListOf(0.0)
        )
    )

    if (optionString?.isNotEmpty() == true) {
        val options = JSONObject(optionString)
        val maxIterations = options.optInt("maxIterations", 1000)
        val sampleEvery = options.optInt("sampleEvery", 100).coerceAtLeast(1)
        val learningRate = options.optDouble("learningRate", sm.trainerConfig.learningRate)
        val momentum = options.optDouble("momentum", (sm.trainerConfig.optimizer as BasicOptimizer).momentum)

        sm.trainerConfig.optimizer = BasicOptimizer(momentum = momentum)
        sm.trainerConfig.learningRate = learningRate

        val trainer = SupervisedTrainer(net, sm)
        println("XOR headless diagnostic")
        println("optimizer=BasicOptimizer learningRate=$learningRate momentum=$momentum maxIterations=$maxIterations")
        println("iteration,error,effectiveStepSize")

        repeat(maxIterations) {
            trainer.trainOnce()
            if (trainer.iteration == 1 || trainer.iteration % sampleEvery == 0 || trainer.iteration == maxIterations) {
                println("${trainer.iteration},${trainer.lastTrainingError},${trainer.lastEffectiveStepSize}")
            }
        }
    }

    addSidebarInfo(
        """
        # XOR

        A basic demonstration of the backpropagation algorithm using the classic XOR problem. XOR (exclusive or) is linearly inseparable: it cannot be solved by a single layer network, so the network must discover a useful hidden representation.

        This simulation intentionally uses only two hidden units. That is enough in principle, but it makes training noticeably sensitive to the initial random weights. This is part of the point of the demo: it gives a taste of old-school neural networks, where small networks could solve surprising problems but often needed repeated randomization to escape plateaus or poor local minima.

        # Simulation Details

        ## Training Data

        The XOR training set consists of:
        - Input `(0,0)` should produce output `0`
        - Input `(1,0)` should produce output `1`
        - Input `(0,1)` should produce output `1`
        - Input `(1,1)` should produce output `0`

        # What to Do

        1. Double-click on the `XOR` network in the `XOR` network window to open the `Train Network` window.

        2. In the top area of this window, select the `Iterate training until the stop button is pressed` button to play the simulation.

        3. Watch the `Sum Squared Error` as training runs. If it levels off above `0`, that is evidence of a local minimum or plateau. Click `Randomize` and train again; it may take a few tries before the network reaches the global minimum, where error is near `0`.

        4. Click `Apply current row as input and increment selected row` to check the network's performance.

        5. As you click through the training data, the pattern on the bottom should be matched by the pattern on the top.

        ## Experiments

        Try switching the optimizer to `Adam`, or build a similar network with three or four hidden units. More hidden units usually make XOR much easier to train, but the two-unit version better illustrates why linearly inseparable tasks were historically interesting and sometimes frustrating.

        # Credits

        Jasmine Lau

        [Jeff Yoshimi](https://jeffyoshimi.net/index.html)

        """.trimIndent()
    )

    // Location of the network in the desktop
    withGui {
        place(networkComponent, 0, 0, 700, 700)
    }

}
