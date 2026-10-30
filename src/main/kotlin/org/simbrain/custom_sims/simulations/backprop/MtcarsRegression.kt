/**
 * Backprop regression on R's Motor Trend cars (mtcars) dataset: predict a car's miles per gallon, horsepower, and
 * quarter-mile time from its cylinders, weight, and transmission. The data are read from
 * `simulations/tables/mtcars.csv`.
 */
package org.simbrain.custom_sims.simulations

import kotlinx.coroutines.Dispatchers
import org.simbrain.custom_sims.*
import org.simbrain.network.core.NeuronArray
import org.simbrain.network.subnetworks.BackpropNetwork
import org.simbrain.network.trainers.BackpropLossFunction
import org.simbrain.network.trainers.TrainingDataset
import org.simbrain.network.trainers.splitDataSet
import org.simbrain.network.updaterules.LinearRule
import org.simbrain.util.div
import org.simbrain.util.place
import org.simbrain.util.updateAction

val mtcarsRegression = newSim("mtcars_regression") {

    workspace.clearWorkspace()
    val networkComponent = addNetworkComponent("Cars Dataset")
    val net = networkComponent.network

    val cars = parseMtcars(readSimulationFileContents("tables" / "mtcars.csv"))

    // Min-max scale every column to [0, 1] using all 32 cars. Manual is already 0 or 1.
    fun range(column: (Car) -> Double) = cars.minOf(column)..cars.maxOf(column)
    fun ClosedFloatingPointRange<Double>.scale(value: Double) = (value - start) / (endInclusive - start)
    val cylinderRange = range { it.cylinders }
    val weightRange = range { it.weight }
    val outputColumns = listOf<Pair<String, (Car) -> Double>>(
        "MPG" to { it.mpg },
        "Horsepower" to { it.horsepower },
        "Quarter mile" to { it.quarterMile }
    )
    val outputRanges = outputColumns.map { (_, column) -> range(column) }
    val inputNames = listOf("Cylinders", "Weight", "Manual")
    val outputNames = outputColumns.map { it.first }

    val dataset = TrainingDataset(
        inputs = cars.map { car ->
            mutableListOf(cylinderRange.scale(car.cylinders), weightRange.scale(car.weight), car.manual)
        }.toMutableList(),
        targets = cars.map { car ->
            outputColumns.zip(outputRanges) { (_, column), range -> range.scale(column(car)) }.toMutableList()
        }.toMutableList(),
        inputRowNames = cars.map { it.model },
        targetRowNames = cars.map { it.model },
        inputColumnNames = inputNames,
        targetColumnNames = outputNames
    )

    // 25 training cars and 7 testing cars
    val (training, testing) = splitDataSet(dataset, 0.8)

    // Linear output nodes, as is usual for regression, so the predictions are not squashed into a fixed range
    val bp = BackpropNetwork(intArrayOf(3, 6, 3))
    bp.outputLayer.updateRule = LinearRule().apply { clippingType = LinearRule.ClippingType.NoClipping }
    bp.trainingSet = training
    bp.testingSet = testing
    bp.trainerConfig.lossFunction = BackpropLossFunction.MSE
    bp.trainerConfig.learningRate = 0.003
    bp.initWeights()
    bp.initBiases()

    bp.layerList.forEach { it.circleMode = true }
    bp.inputLayer.circleSpacingX = 80.0
    bp.outputLayer.circleSpacingX = 80.0
    bp.inputLayer.label = "Input (scaled)"
    bp.outputLayer.label = "Output (scaled)"
    bp.inputLayer.labelArray = inputNames.toTypedArray()
    bp.outputLayer.labelArray = outputNames.toTypedArray()
    // Spread the layers apart vertically so the labels and weights are easier to see
    bp.inputLayer.offset(0.0, 75.0)
    bp.outputLayer.offset(0.0, -75.0)

    net.addNetworkModels(bp)

    // Arrays showing a car's features and the network's predictions in real units, so they can be read and edited
    // directly. Scaling is done in code rather than with weights because the input layer must stay clamped for
    // training.
    val carArray = NeuronArray(inputNames.size).apply {
        label = "Car (unscaled)"
        circleMode = true
        circleSpacingX = 80.0
        labelArray = inputNames.toTypedArray()
        isClamped = true
        increment = 1.0
        setLocation(bp.inputLayer.locationX - 325, bp.inputLayer.locationY)
    }
    val predictionArray = NeuronArray(outputNames.size).apply {
        label = "Predictions (unscaled)"
        circleMode = true
        circleSpacingX = 80.0
        labelArray = outputNames.toTypedArray()
        isClamped = true
        setLocation(bp.outputLayer.locationX + 325, bp.outputLayer.locationY)
    }
    net.addNetworkModels(listOf(carArray, predictionArray), usePlacementManager = false)

    fun applyCarToInputs() {
        val (cylinders, weight, manual) = carArray.activationArray.toList()
        bp.inputLayer.activationArray = doubleArrayOf(cylinderRange.scale(cylinders), weightRange.scale(weight), manual)
    }
    carArray.events.updated.on(Dispatchers.Default) { applyCarToInputs() }
    // Also apply the car at the start of each iteration, since training and the training dialog change the input layer
    net.updateManager.addAction(0, updateAction("Apply car to inputs") { applyCarToInputs() })
    bp.outputLayer.events.updated.on(Dispatchers.Default) {
        predictionArray.activationArray = bp.outputLayer.activationArray.zip(outputRanges) { output, range ->
            range.start + output * (range.endInclusive - range.start)
        }.toDoubleArray()
    }

    // Start with the first car in the dataset
    cars.first().let { carArray.activationArray = doubleArrayOf(it.cylinders, it.weight, it.manual) }

    withGui {
        place(networkComponent, SIM_WINDOW_GAP, SIM_WINDOW_GAP, 900, 650)
    }

    addSidebarInfo(
        """
        # Cars Dataset

        This simulation uses backprop to predict three things about a car, its fuel efficiency in miles per gallon (MPG), its horsepower, and its quarter-mile time, from its number of cylinders, its weight, and whether it has a manual transmission.

        This is a regression task. In a regression task the targets are real numbers that represent quantities, here miles per gallon, horsepower, and seconds. In a classification task, by contrast, the targets are binary values that represent categories, as in the `Three object detector` simulation.

        This simulation accompanies the "Classification and Regression" section of the "Supervised Learning" chapter of _Neural Networks in Cognitive Science_, available [here](https://downloads.jeffyoshimi.net/NeuralNetworksCogsci.pdf). The mtcars dataset itself is introduced in the "Data Science and Learning Basics" chapter.

        # Simulation Details

        The data come from the [mtcars](https://stat.ethz.ch/R-manual/R-devel/library/datasets/html/mtcars.html) dataset in R, which describes `32` cars from the 1973 and 1974 model years, as reported in _Motor Trend_ magazine. The data are in `simulations/tables/mtcars.csv`.

        The network has three input nodes:

        - `Cylinders`: number of cylinders (`4`, `6`, or `8`)
        - `Weight`: weight in thousands of pounds
        - `Manual`: `1` for a manual transmission, `0` for an automatic

        and three output nodes, which are the network's predictions:

        - `MPG`: miles per gallon
        - `Horsepower`: gross horsepower
        - `Quarter mile`: time in seconds to drive a quarter mile from a standing start

        The output nodes use a linear activation function, as is usual for regression, so their values are not confined to a fixed range. The six hidden nodes are sigmoidal.

        Every column except `Manual` was min-max scaled to the range `0` to `1` using all `32` cars, so the smallest value in each column becomes `0` and the largest becomes `1`. `Manual` is already `0` or `1`. In the training dialog the rows are labeled with each car's model name.

        The cars were split at random into `25` training cars and `7` testing cars. The network never trains on the testing cars, so how well it does on them shows how well it generalizes to new cars. Error is measured as mean squared error (MSE).

        ## Real Units

        The network itself works with scaled values, which are hard to read, so two extra arrays show the same values in real units. `Car (real units)`, to the left of the input layer, holds a car's cylinders, weight (in thousands of pounds), and transmission, and is copied into the input layer. `Predictions (real units)`, to the right of the output layer, shows the network's predictions in miles per gallon, horsepower, and seconds.

        **Edit the car by clicking one of its nodes and pressing the up and down arrow keys, then iterate the workspace (`Step` or `Run`) to see what the network predicts.**

        # What to Do

        1. Right-click the backprop network and choose `Train...` (or double-click it) to open the `Train Network` dialog.

        2. Press `Run` to train, and `Stop` once the training error (red) levels off, usually within a thousand or so iterations. The testing error (blue) shows how well the network does on the `7` testing cars. If you keep training well past that point, the testing error often starts to rise while the training error keeps falling: the network is memorizing the `25` training cars rather than learning something that carries over to new ones (overfitting).

        3. Open the `Testing data` tab. Select a car, press `Apply inputs`, and compare the three output nodes with the car's targets in the `Targets` table. The error for that car is shown next to `Apply inputs`. With `Auto advance` checked, pressing `Apply inputs` again steps through the testing cars.

        4. Look at `Predictions (real units)` to see the network's predictions for that car in miles per gallon, horsepower, and seconds. (`Apply inputs` does not change `Car (real units)`; the next time you iterate the workspace, the network goes back to the car shown there.)

        Notice that the outputs are rarely exactly equal to the targets. In regression, a near miss is partly right: predicting `20` MPG for a car that gets `21` is a good prediction, and it has a small error. This differs from classification, where an output is usually counted as either the right category or the wrong one.

        Try the same thing on the `Training data` tab. After enough training the network usually does better on the cars it was trained on than on the testing cars. You can also press `Init parameters` and train again to see how much the results vary from run to run.

        ## Why Does It Generalize So Well?

        The network often does surprisingly well on the testing cars. There are several reasons for this:

        - The relationships in this dataset are strong and fairly simple. Heavier cars with more cylinders get fewer miles per gallon and have more horsepower. A plain linear regression on the three inputs already accounts for roughly `75` to `85` percent of the variation in each output, so the network does not need to memorize anything to do well on new cars.
        - Several cars have near-twins, like the Mazda RX4 and RX4 Wag, the Merc 280 and 280C, and the three Merc 450 models. When one twin is in the training set and the other is in the testing set, the network is mostly filling in between cars it has already seen, rather than facing anything really new.
        - The network is small, with only `45` weights and biases, which limits how much it can memorize.
        - Errors on scaled values look smaller than they are. A mean squared error of `0.003` on the `MPG` output is still a miss of about `1.3` miles per gallon.

        This does not mean the network has learned how cars work. Try some cars of your own in `Car (real units)`. Cars like the ones in the dataset, such as a light 4-cylinder car with a manual transmission or a heavy 8-cylinder automatic, get sensible predictions. But cars unlike any in the dataset, such as a `12`-cylinder car or one that weighs `1,000` pounds, can get predictions that are far off, or even impossible. The network generalizes well only to cars that resemble the ones it was trained on.

        With only `7` testing cars, the results also depend a lot on which cars happen to land in the testing set. For a harder test of generalization, see the `Feed-forward MNIST` and `CNN MNIST` simulations, which test on shifted, rotated, and noisy digits.

        # Credits

        [Jeff Yoshimi](https://jeffyoshimi.net/index.html)

        """.trimIndent()
    )

}

private class Car(
    val model: String,
    val mpg: Double,
    val cylinders: Double,
    val horsepower: Double,
    val weight: Double,
    val quarterMile: Double,
    val manual: Double
)

/**
 * Parse the mtcars table, whose header row is followed by one row per car: model name, MPG, cylinders,
 * horsepower, weight, quarter-mile time, and manual.
 */
private fun parseMtcars(csv: String) = csv.lines().drop(1).filter { it.isNotBlank() }.map { line ->
    val fields = line.split(",").map { it.trim() }
    val values = fields.drop(1).map { it.toDouble() }
    Car(fields[0], values[0], values[1], values[2], values[3], values[4], values[5])
}

