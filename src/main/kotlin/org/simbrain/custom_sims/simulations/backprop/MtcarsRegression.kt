/**
 * Backprop regression on R's Motor Trend cars (mtcars) dataset: predict a car's miles per gallon from its
 * cylinders, horsepower, weight, and transmission. The data are read from `simulations/tables/mtcars.csv`.
 */
package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.*
import org.simbrain.network.subnetworks.BackpropNetwork
import org.simbrain.network.trainers.BackpropLossFunction
import org.simbrain.network.trainers.TrainingDataset
import org.simbrain.network.trainers.splitDataSet
import org.simbrain.network.updaterules.LinearRule
import org.simbrain.util.div
import org.simbrain.util.place
import java.util.Locale

val mtcarsRegression = newSim("mtcars_regression") {

    workspace.clearWorkspace()
    val networkComponent = addNetworkComponent("mtcars Regression")
    val net = networkComponent.network

    val cars = parseMtcars(readSimulationFileContents("tables" / "mtcars.csv"))

    // Min-max scale every column to [0, 1] using all 32 cars. Manual is already 0 or 1.
    fun range(column: (Car) -> Double) = cars.minOf(column)..cars.maxOf(column)
    val mpgRange = range { it.mpg }
    val inputRanges = listOf(range { it.cylinders }, range { it.horsepower }, range { it.weight })
    fun ClosedFloatingPointRange<Double>.scale(value: Double) = (value - start) / (endInclusive - start)

    val dataset = TrainingDataset(
        inputs = cars.map { car ->
            mutableListOf(
                inputRanges[0].scale(car.cylinders),
                inputRanges[1].scale(car.horsepower),
                inputRanges[2].scale(car.weight),
                car.manual
            )
        }.toMutableList(),
        targets = cars.map { mutableListOf(mpgRange.scale(it.mpg)) }.toMutableList(),
        inputRowNames = cars.map { it.model },
        targetRowNames = cars.map { it.model },
        inputColumnNames = listOf("Cylinders", "Horsepower", "Weight", "Manual"),
        targetColumnNames = listOf("MPG")
    )

    // 25 training cars and 7 testing cars
    val (training, testing) = splitDataSet(dataset, 0.8)

    // A linear output node, as is usual for regression, so the prediction is not squashed into a fixed range
    val bp = BackpropNetwork(intArrayOf(4, 5, 1))
    bp.outputLayer.updateRule = LinearRule().apply { clippingType = LinearRule.ClippingType.NoClipping }
    bp.trainingSet = training
    bp.testingSet = testing
    bp.trainerConfig.lossFunction = BackpropLossFunction.MSE
    bp.trainerConfig.learningRate = 0.003
    bp.initWeights()
    bp.initBiases()

    bp.layerList.forEach { it.circleMode = true }
    bp.inputLayer.circleSpacingX = 80.0
    bp.inputLayer.labelArray = arrayOf("Cylinders", "Horsepower", "Weight", "Manual")
    bp.outputLayer.labelArray = arrayOf("MPG")

    net.addNetworkModels(bp)

    withGui {
        place(networkComponent, SIM_WINDOW_GAP, SIM_WINDOW_GAP, 500, 550)
    }

    val mpgMin = mpgRange.start
    val mpgMax = mpgRange.endInclusive
    val mpgSpan = mpgMax - mpgMin

    addSidebarInfo(
        """
        # mtcars Regression

        This simulation uses backprop to predict a car's fuel efficiency, in miles per gallon (MPG), from a few of its other features.

        This is a regression task. In a regression task the targets are real numbers that represent quantities, here miles per gallon. In a classification task, by contrast, the targets are binary values that represent categories, as in the `Three object detector` simulation.

        This simulation accompanies the "Classification and Regression" section of the "Supervised Learning" chapter of _Neural Networks in Cognitive Science_, available [here](https://downloads.jeffyoshimi.net/NeuralNetworksCogsci.pdf). The mtcars dataset itself is introduced in the "Data Science and Learning Basics" chapter.

        # Simulation Details

        The data come from the [mtcars](https://stat.ethz.ch/R-manual/R-devel/library/datasets/html/mtcars.html) dataset in R, which describes `32` cars from the 1973 and 1974 model years, as reported in _Motor Trend_ magazine. The data are in `simulations/tables/mtcars.csv`.

        The network has four input nodes:

        - `Cylinders`: number of cylinders (`4`, `6`, or `8`)
        - `Horsepower`: gross horsepower
        - `Weight`: weight in thousands of pounds
        - `Manual`: `1` for a manual transmission, `0` for an automatic

        The single output node, `MPG`, is the network's prediction of the car's miles per gallon. It uses a linear activation function, as is usual for regression, so its value is not confined to a fixed range. The five hidden nodes are sigmoidal.

        Cylinders, horsepower, weight, and MPG were each min-max scaled to the range `0` to `1` using all `32` cars, so the smallest value in each column becomes `0` and the largest becomes `1`. `Manual` is already `0` or `1`. In the training dialog the rows are labeled with each car's model name.

        The cars were split at random into `25` training cars and `7` testing cars. The network never trains on the testing cars, so how well it does on them shows how well it generalizes to new cars. Error is measured as mean squared error (MSE).

        ## Converting the Output to Miles per Gallon

        The lowest MPG in the dataset is `${mpgMin.formatMpg()}` and the highest is `${mpgMax.formatMpg()}`. To convert a scaled output back to miles per gallon, multiply it by the difference between them, `${mpgSpan.formatMpg()}`, and add the minimum:

        MPG = `${mpgMin.formatMpg()}` + `${mpgSpan.formatMpg()}` × output

        For example, an output of `0.4` corresponds to `${(mpgMin + 0.4 * mpgSpan).formatMpg()}` MPG, and an output of `0.8` corresponds to `${(mpgMin + 0.8 * mpgSpan).formatMpg()}` MPG.

        # What to Do

        1. Right-click the backprop network and choose `Train...` (or double-click it) to open the `Train Network` dialog.

        2. Press `Run` to train, and `Stop` once the training error (red) levels off, usually within a thousand or so iterations. The testing error (blue) shows how well the network does on the `7` testing cars. If you keep training well past that point, the testing error often starts to rise while the training error keeps falling: the network is memorizing the `25` training cars rather than learning something that carries over to new ones (overfitting).

        3. Open the `Testing data` tab. Select a car, press `Apply inputs`, and compare the `MPG` output node with the car's target in the `Targets` table. The error for that car is shown next to `Apply inputs`. With `Auto advance` checked, pressing `Apply inputs` again steps through the testing cars.

        4. Convert a few outputs and targets back to miles per gallon using the formula above, to see how many miles per gallon the network is off by.

        Notice that the outputs are rarely exactly equal to the targets. In regression, a near miss is partly right: predicting `20` MPG for a car that gets `21` is a good prediction, and it has a small error. This differs from classification, where an output is usually counted as either the right category or the wrong one.

        Try the same thing on the `Training data` tab. After enough training the network usually does better on the cars it was trained on than on the testing cars. You can also press `Init parameters` and train again to see how much the results vary from run to run.

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
    val manual: Double
)

/**
 * Parse the mtcars table, whose header row is followed by one row per car: model name, MPG, cylinders,
 * horsepower, weight, and manual.
 */
private fun parseMtcars(csv: String) = csv.lines().drop(1).filter { it.isNotBlank() }.map { line ->
    val fields = line.split(",").map { it.trim() }
    val values = fields.drop(1).map { it.toDouble() }
    Car(fields[0], values[0], values[1], values[2], values[3], values[4])
}

private fun Double.formatMpg() = "%.1f".format(Locale.US, this)
