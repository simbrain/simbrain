/**
 * A classifier trained on the Iris flower dataset, with a plot of the flowers and the shared [ClassifierExplorer]
 * control panel for choosing and comparing models and loading training or test flowers onto the inputs. Choosing a
 * model replaces the classifier inside one [ClassifierNetwork], so the network and the plot stay in place.
 */
package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.*
import org.simbrain.custom_sims.simulations.ml.ClassifierChoice
import org.simbrain.custom_sims.simulations.ml.ClassifierExplorer
import org.simbrain.custom_sims.simulations.ml.placeExplorerWindows
import org.simbrain.custom_sims.simulations.ml.trainClassifier
import org.simbrain.network.core.setLabels
import org.simbrain.network.layouts.LineLayout
import org.simbrain.network.smile.ClassifierNetwork
import org.simbrain.network.trainers.createClassificationDataset
import org.simbrain.network.util.Alignment
import org.simbrain.network.util.alignNetworkModels
import org.simbrain.util.createClassifierProjectionPlot
import org.simbrain.util.toMatrix
import smile.io.Read

val irisClassifier = newSim {
    workspace.clearWorkspace()

    // Last column is the species
    val iris = Read.arff("simulations/tables/iris.arff")
    val dataset = createClassificationDataset(dataFrame = iris, inputColumns = intArrayOf(0, 1, 2, 3), targetColumn = 4)
    val species = listOf("Setosa", "Versicolor", "Virginica")
    val classifierChoice = ClassifierChoice.KNN

    val networkComponent = addNetworkComponent("Network")
    val network = networkComponent.network

    val classifierNetwork = ClassifierNetwork(trainClassifier(classifierChoice, dataset)).apply {
        (outputNeuronGroup.layout as LineLayout).spacing = 100.0
        outputNeuronGroup.applyLayout()
        inputNeuronGroup.setLabels(listOf("Sepal\nlength", "Sepal\nwidth", "Petal\nlength", "Petal\nwidth"))
        outputNeuronGroup.setLabels(species)
        alignNetworkModels(inputNeuronGroup, outputNeuronGroup, Alignment.VERTICAL)
        inputNeuronGroup.inputData = classifier.trainingData.inputs.toMatrix()
    }
    network.addNetworkModels(classifierNetwork)

    addSidebarInfo(
    """
    # Iris Classifier

    This simulation shows how a machine learning model can tell three kinds of iris apart from four simple measurements, without writing any code.

    ## Background

    This is a famous dataset in machine learning, the [Iris Dataset](https://en.wikipedia.org/wiki/Iris_flower_data_set). If you are interested, you can learn more about the dataset [here](https://archive.ics.uci.edu/ml/datasets/iris).

    The dataset has `150` rows, one per flower, and `5` columns. The first four columns are measurements in centimeters of the petals and the sepals (a sepal is a petal-like structure that encloses the bud as the plant develops). The fifth column says which of three kinds of iris the flower is: Setosa, Versicolor, or Virginica. There are `50` of each. Notice that the Setosa looks different from the other two.

    ![Iris-setosa](//localfiles/simulations/images/iris/irissetosa.png)
    ![Iris-versicolor](//localfiles/simulations/images/iris/irisversicolor.png)
    ![Iris-virginica](//localfiles/simulations/images/iris/irisvirginica.png)

    # Simulation Details

    The network shows the classifier as a box. The four input neurons hold a flower's measurements, and the output neuron for the predicted species turns on.

    The flowers are split at random: `120` are used to train the model, and the other `30` are held back as test flowers the model never sees (`8` Setosa, `12` Versicolor, and `10` Virginica). Test accuracy is the honest measure of how well the model works on new flowers.

    You can choose one of four kinds of model. K nearest neighbors finds the `5` training flowers with the most similar measurements and takes a vote. Logistic regression separates the species with straight boundaries. A support vector machine finds boundaries that leave the widest gap between the species. A random forest asks many yes/no questions about the measurements, such as "is the petal longer than 2.5?", and takes a vote. To see how these models differ, try the simulations *Simulations › Machine learning › Decision boundaries* and *Simulations › Machine learning › Model comparison*.

    The plot squeezes the four measurements onto two axes using principal component analysis (PCA), which keeps as much of the spread between flowers as two axes can. Each flower is a dot colored by its true species: training flowers are filled and test flowers are hollow. The flower currently on the inputs is outlined in red. If you make up a new flower by editing the measurements, it is added as a dot colored by the model's prediction.

    PCA is one of several ways to fit four measurements onto two axes. Choose `Sammon` from the menu at the top left of the plot and press the run button in the plot's toolbar: it moves the dots around until flowers that are similar in all four measurements end up close together.

    ## Control Panel Settings

    - `Classifier`: which kind of model to use. The model is trained as soon as you pick it.
    - `Training accuracy` and `Test accuracy`: percent correct on the `120` training flowers and the `30` test flowers.
    - `Compare all models`: train all four kinds of model and show their training and test accuracy in a table, along with a baseline: the score you would get by always guessing the most common species.
    - `Flowers`: whether the buttons below use the `30` test flowers or the `120` training flowers.
    - `Setosa`, `Versicolor`, `Virginica`: load a randomly chosen flower of that species onto the inputs and show the prediction.
    - `Previous` and `Next`: step through the flowers in order, loading each onto the inputs.
    - `Status`: which flower is loaded, its true species, and the model's prediction.

    # What to Do

    ## Getting started

    1. Compare the models. Press `Compare all models`. Every model gets `93.3%` of the test flowers right, far above the baseline, but training accuracy runs from `97.5%` for K nearest neighbors to `100%` for the random forest. Models always do better on flowers they were trained on, which is why test accuracy is the honest score.
    2. Try some test flowers. Pick a classifier, and with `Flowers` set to `Test flowers`, press `Setosa`, `Versicolor`, and `Virginica` a few times each. Each press loads a random flower of that species onto the input neurons, the output neuron for the predicted species turns on, and `Status` says whether the prediction is right. The flower is outlined in red in the plot.
    3. Try some training flowers. Set `Flowers` to `Training flowers` and press the species buttons again. The model has seen these flowers before, so it gets nearly all of them right.

    ## Going further

    4. Look at the clouds. In the plot, Setosa forms a cloud of its own, far from the others, so it is easy to recognize. Versicolor and Virginica overlap, so some of those flowers are hard to tell apart.
    5. Change one measurement. Press `Versicolor`. Double-click the `Petal length` input neuron, set its activation to `6`, press `OK`, and then press the `Iterate workspace` button (the step icon) in the main toolbar. The prediction switches to `Virginica`. Now set petal length to `1.5`: the flower becomes a `Setosa`.
    6. Find what matters. Press `Versicolor` again and change `Sepal length` instead, anywhere from `3` to `8`. With `K nearest neighbors` selected, the prediction almost always stays `Versicolor`. The petals carry most of the information that separates the species.
    7. Find the hard flowers. Set `Flowers` to `Test flowers` and press `Next` repeatedly, watching `Status`. Test flowers `26` and `30` are Versicolor, but the model calls them Virginica. Look at their input neurons: their petals are about `5` cm long and `1.6` to `1.7` cm wide, which is typical of a Virginica. In the plot they land near where the Versicolor and Virginica clouds meet. Change `Classifier` and check them again: every model misses the same two flowers. When every model makes the same mistake, the problem is not the model: those flowers really do look like the other species.

    # Credits

    Daisy Mayorga

    [Jeff Yoshimi](https://www.jeffyoshimi.net)

    Kanly Thao
    """.trimIndent()
    )

    withGui {
        val explorer = ClassifierExplorer(
            workspace = workspace,
            network = classifierNetwork,
            dataset = dataset,
            classNames = species,
            classifierChoice = classifierChoice,
            exampleNoun = "flower",
            describeData = { "Iris flowers" }
        )
        val controlPanel = createControlPanel("Iris Classifier", SIM_WINDOW_GAP, SIM_WINDOW_GAP) {
            explorer.addModelControls(this)
            addSeparator()
            explorer.addExampleControls(this, ClassifierExplorer.ExampleStyle.ClassButtons)
        }.awaitLayout()
        explorer.showAccuracy()
        val plot = createClassifierProjectionPlot(classifierNetwork, "Iris Flowers")
        placeExplorerWindows(controlPanel, plot, networkComponent)
    }
}
