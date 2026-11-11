/**
 * Four standard classifiers trained on small two-feature datasets, with the plane shaded by each model's predictions
 * so students can see how the models divide the input space and how each model's main parameter trades
 * underfitting against overfitting. Every control retrains immediately by replacing the classifier inside one
 * [ClassifierNetwork], whose plot then redraws its data points and regions.
 */
package org.simbrain.custom_sims.simulations.ml

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.simbrain.custom_sims.*
import org.simbrain.network.core.setLabels
import org.simbrain.network.smile.ClassifierNetwork
import org.simbrain.network.trainers.*
import org.simbrain.network.util.Direction
import org.simbrain.network.util.offsetNeuronCollections
import org.simbrain.util.createClassifierProjectionPlot
import org.simbrain.util.place
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.JComponent

val decisionBoundaries = newSim {
    workspace.clearWorkspace()

    val settings = DecisionBoundarySettings()
    val networkComponent = addNetworkComponent("Network")
    val network = networkComponent.network

    val classifierNetwork = ClassifierNetwork(settings.createTrainedClassifier(settings.createDataset())).apply {
        inputNeuronGroup.setLabels(listOf("x", "y"))
        outputNeuronGroup.setLabels(listOf("Blue", "Orange"))
        // A shorter gap than the default lets the small network zoom larger in its window
        offsetNeuronCollections(inputNeuronGroup, outputNeuronGroup, Direction.NORTH, 90.0)
    }
    network.addNetworkModels(classifierNetwork)

    addSidebarInfo(
        """
        # Decision Boundaries

        A *classifier* takes a list of numbers, called *features*, and assigns it to a category, called a *class*. Here every data point has just two features, `x` and `y`, so the whole input space is a flat plane. The plot shades each part of that plane by the class the model would predict there. The edges between the shaded areas are the model's *decision boundaries*.

        Different models draw very different boundaries on the same data. Watching those shapes change is the quickest way to get a feel for what each model can learn, and for the two ways a model can go wrong: being too simple, or memorizing noise.

        # Simulation Details

        ## Reading the plot

        - Each dot is a data point, colored by its true class, blue or orange. Filled dots are training points and hollow dots are test points.
        - The shading is the model's prediction at that spot.
        - A dot sitting on shading of the other color is a point the model gets wrong. A filled one is a training error; a hollow one is a test error, a mistake on data the model never saw.

        ## Training and test data

        Each dataset has 200 points. At random, 160 of them are used to *train* the model and the other 40 are held back as *test* data the model never sees. In the plot, training points are filled and test points are hollow.

        `Training accuracy` says how well the model fits the examples it learned from. `Test accuracy` estimates how well it will do on new data, which is what we actually care about. If training accuracy is much higher than test accuracy, the model has *overfit*: it learned quirks of this particular sample, including its noise. If both are low, the model is too simple for the data. This is called *underfitting*.

        With only 40 test points, each one is worth 2.5%, so small differences in test accuracy are mostly luck.

        ## The datasets

        Each dataset is a small puzzle that some models can solve and others cannot.

        - `Blobs`: two round clusters. A straight line separates them, so every model should do well. This is the easy case and a sanity check: when a simple boundary is the right answer, flexible models have nothing to add.
        - `Moons`: two interlocking half circles. They are almost, but not quite, separable by a straight line, so a straight boundary gets most points right and misses the tips. This tests whether a model can bend.
        - `Circles`: one class forms a ring around the other. Any straight line cuts both classes in half, so a linear model does no better than guessing. A model has to draw a closed loop.
        - `XOR`: four clusters where diagonally opposite corners share a class, the pattern of the logical function exclusive or. Neither `x` alone nor `y` alone says anything about the class; only the combination does. XOR is famous because a one-layer neural network cannot learn it (Minsky & Papert, 1969), which helped motivate multi-layer networks trained with backpropagation.

        ## The four models

        - `Logistic regression` draws one straight line and predicts by which side of the line a point falls on.
        - `K nearest neighbors` (KNN) memorizes the training data. To classify a new point it finds the `K` closest training points and takes a majority vote.
        - `Support vector machine` (SVM) finds the boundary that leaves the widest possible gap, or *margin*, between the classes. The Gaussian kernel used here lets the boundary bend. The kernel width sets how far each training point's influence reaches.
        - `Random forest` trains many decision trees, 100 to start, and takes a vote. Each tree asks a series of yes/no questions such as "is `x` greater than 0.4?". Each tree sees a slightly different random resample of the data, so the trees disagree in useful ways.

        ## Control Panel Settings

        - `Dataset`: which of the four datasets above to use.
        - `Noise`: how widely the points scatter. More noise means the classes overlap more.
        - `New sample`: draws a fresh random sample of the same dataset with the same noise.
        - `Classifier`: which model to train.
        - `K (nearest neighbors)`: how many neighbors vote. Enabled only for KNN.
        - `Kernel width (SVM)`: how far each training point's influence reaches. Enabled only for the SVM.
        - `Max depth (random forest)`: the most yes/no questions any tree may ask about a point. Enabled only for the random forest.
        - `Trees (random forest)`: how many trees vote. Enabled only for the random forest.
        - `Training accuracy` and `Test accuracy`: percent correct on the 160 training points and the 40 test points.
        - `Test point` and `Training point`: put a randomly chosen test or training point on the input neurons. The output neuron for the predicted class turns on, and the point is outlined in red in the plot.
        - `Previous` and `Next`: step through the points in order, loading each onto the inputs. They use test points or training points, whichever you loaded last.
        - `Status`: which point is loaded, its true class, and the model's prediction.

        Every control retrains the model immediately. To see all of a model's parameters, such as the SVM's soft margin penalty, double-click the classifier in the network. When the classifier is a random forest, right-click it in the network and choose `Show trees...` to see each tree in the forest as a list of yes/no questions, each ending in a predicted class. Use the arrow buttons to step from tree to tree.

        # Activities

        Change one thing at a time. After each change, look at the shading, then compare the two accuracies.

        ## Activity 1: Straight lines

        The simulation starts with `Moons` and `Logistic regression`. The boundary is a straight line, so the tip of each moon lands on the wrong side. Logistic regression can only draw straight boundaries, however much data it gets.

        ## Activity 2: Nearest neighbors

        Set `Classifier` to `K nearest neighbors`. With `K` at 15 the boundary curves to follow the moons, and accuracy goes up.

        ## Activity 3: Overfitting

        Drag `K` down to 1. The boundary turns jagged, with little islands around single points. Training accuracy is 100%, because each training point is its own nearest neighbor, but test accuracy does not improve. Press `New sample` a few times. The islands jump around because they follow noise in each sample, not the real shape of the moons.

        ## Activity 4: Underfitting

        Drag `K` up to 100. Each vote now averages over most of the data, the boundary flattens toward a line, and both accuracies drop. The best `K` is somewhere in between.

        ## Activity 5: Support vector machine

        Set `Classifier` to `Support vector machine`. With `Kernel width` at 0.5 the boundary is a smooth curve. Try 0.1: each point's influence is tiny, so the model draws tight bubbles around points and overfits. Try 3: each point's influence is broad, and the boundary flattens and underfits. This is the same trade-off you saw with `K`.

        ## Activity 6: Random forest

        Set `Classifier` to `Random forest`. The boundary is made of horizontal and vertical steps, because every question a tree asks compares one feature to a threshold. Set `Max depth` to 1, so each tree asks one question and makes one straight cut. Raise it a step at a time and watch the boundary gain detail.

        ## Activity 7: One tree

        Set `Max depth` to 2 and `Trees` to 1, then right-click the classifier in the network and choose `Show trees...`. With a single tree the shaded regions are exactly that tree's rules: find each threshold in the plot as a horizontal or vertical edge. Now raise `Trees` to 10 and step through the trees with the arrow buttons: each asks different questions. Each tree saw a different resample of the data and drew its edges in slightly different places, so the vote of many trees gives a finer, steadier boundary than any one tree.

        ## Activity 8: Harder shapes

        Set `Dataset` to `Circles`, then `XOR`, and try each model again. Logistic regression gets about 50% or worse, no better than guessing, because no straight line separates these classes. On `XOR`, a random forest with `Max depth` 1 also fails: knowing only `x`, or only `y`, says nothing about the class. It takes two questions, one about each feature. Raise `Max depth` to 2 and the forest starts to work.

        ## Activity 9: More noise

        Go back to `Moons` and raise `Noise` to 0.5. The classes now overlap, so no model can be right everywhere. Flexible settings, such as `K` = 1, a small kernel width, or deep trees, now show a large gap between training and test accuracy.

        ## Activity 10: Probe the model

        The network shows the classifier as a box connecting two input neurons, `x` and `y`, to two output neurons. Press `Test point` to put a random test point on the inputs. The output neuron for the predicted class turns on, the point is outlined in red in the plot, and `Status` says whether the prediction is right. Press `Next` to step through the test points one by one; the wrong ones are the hollow points sitting on shading of the other color.

        You can also make up a point. Double-click an input neuron, set its activation to a location in the plot, and press the `Iterate workspace` button (the step icon) in the main toolbar. A new dot appears at that spot, colored by the prediction.

        ## Takeaways

        - Every model has a setting that controls how flexible it is. Too little flexibility underfits, too much overfits, and test data is how you tell the difference.
        - Logistic regression is a good first try. It rarely overfits and is easy to interpret, but it cannot bend.
        - KNN needs no real training and makes few assumptions, but it gets slow with large datasets and depends on how distance is measured.
        - SVMs draw smooth, flexible boundaries and work well on small and medium datasets, but they need the right kernel width.
        - Random forests work well with little tuning and are often the strongest choice for tables of mixed features.

        To try all four models on real datasets, see the simulation *Simulations › Machine learning › Model comparison*.

        # References

        Minsky, M., & Papert, S. (1969). *Perceptrons: An introduction to computational geometry*. MIT Press.
        """.trimIndent()
    )

    withGui {
        val parameterControls = mutableMapOf<ClassifierChoice, MutableList<JComponent>>()

        fun <T : JComponent> T.enabledFor(choice: ClassifierChoice) = also {
            parameterControls.getOrPut(choice) { mutableListOf() }.add(it)
        }

        suspend fun enableParameterControls() = withContext(Dispatchers.Swing) {
            parameterControls.forEach { (choice, controls) -> controls.forEach { it.isEnabled = choice == settings.classifier } }
        }
        val explorer = ClassifierExplorer(
            workspace = workspace,
            network = classifierNetwork,
            dataset = classifierNetwork.classifier.dataset,
            classNames = listOf("Blue", "Orange"),
            classifierChoice = settings.classifier,
            exampleNoun = "point",
            describeData = { settings.dataset.toString() }
        )

        // Retrain requests are conflated so dragging a slider runs one retrain for the latest value rather than one per
        // step. A request for new data is remembered until a retrain consumes it.
        val retrainRequests = Channel<Unit>(Channel.CONFLATED)
        val needsNewData = AtomicBoolean(false)

        fun requestRetrain(newData: Boolean = false) {
            if (newData) needsNewData.set(true)
            retrainRequests.trySend(Unit)
        }

        suspend fun retrain(newData: Boolean) = explorer.lock.withLock {
            val data = if (newData) settings.createDataset() else classifierNetwork.classifier.dataset
            classifierNetwork.classifier = settings.createTrainedClassifier(data)
            explorer.classifierChangedLocked(newData)
        }

        val controlPanel = createControlPanel("Decision Boundaries", SIM_WINDOW_GAP, SIM_WINDOW_GAP) {
            launch {
                while (true) {
                    retrainRequests.receive()
                    retrain(needsNewData.getAndSet(false))
                }
            }
            addComboBox(
                "Dataset",
                ToyDataset.entries,
                settings.dataset,
                toolTip = "Shape of the two classes in the plane."
            ) {
                settings.dataset = it
                requestRetrain(newData = true)
            }
            addSliderWithTextField(
                "Noise",
                minValue = 0.0,
                maxValue = 1.0,
                initValue = settings.noise,
                increment = 0.05,
                showValueField = true,
                toolTip = "How widely points scatter around the dataset's shape."
            ) {
                settings.noise = it
                requestRetrain(newData = true)
            }
            addButton("New sample") {
                settings.seed++
                requestRetrain(newData = true)
            }.apply { toolTipText = "Draw a fresh random sample of the same dataset." }
            addSeparator()
            addComboBox(
                "Classifier",
                ClassifierChoice.entries,
                settings.classifier,
                toolTip = "Model to train on the data."
            ) {
                settings.classifier = it
                launch { enableParameterControls() }
                requestRetrain()
            }
            addSliderWithTextField(
                "K (nearest neighbors)",
                minValue = 1.0,
                maxValue = 100.0,
                initValue = settings.k.toDouble(),
                increment = 1.0,
                showValueField = true,
                toolTip = "Number of nearest training points that vote. Used only by K nearest neighbors."
            ) {
                settings.k = it.toInt()
                if (settings.classifier == ClassifierChoice.KNN) requestRetrain()
            }.enabledFor(ClassifierChoice.KNN)
            addSliderWithTextField(
                "Kernel width (SVM)",
                minValue = 0.05,
                maxValue = 3.0,
                initValue = settings.kernelWidth,
                increment = 0.05,
                showValueField = true,
                toolTip = "How far each training point's influence reaches. Used only by the support vector machine."
            ) {
                settings.kernelWidth = it
                if (settings.classifier == ClassifierChoice.SVM) requestRetrain()
            }.enabledFor(ClassifierChoice.SVM)
            addSliderWithTextField(
                "Max depth (random forest)",
                minValue = 1.0,
                maxValue = 20.0,
                initValue = settings.maxDepth.toDouble(),
                increment = 1.0,
                showValueField = true,
                toolTip = "Most yes/no questions a tree may ask about a point. Used only by the random forest."
            ) {
                settings.maxDepth = it.toInt()
                if (settings.classifier == ClassifierChoice.RandomForest) requestRetrain()
            }.enabledFor(ClassifierChoice.RandomForest)
            addSliderWithTextField(
                "Trees (random forest)",
                minValue = 1.0,
                maxValue = 100.0,
                initValue = settings.numTrees.toDouble(),
                increment = 1.0,
                showValueField = true,
                toolTip = "Number of trees that vote. Used only by the random forest."
            ) {
                settings.numTrees = it.toInt()
                if (settings.classifier == ClassifierChoice.RandomForest) requestRetrain()
            }.enabledFor(ClassifierChoice.RandomForest)
            addSeparator()
            explorer.addAccuracyControls(this)
            addSeparator()
            explorer.addExampleControls(this, ClassifierExplorer.ExampleStyle.AnyClass)
        }.awaitLayout()
        enableParameterControls()
        explorer.showAccuracy()

        val plotSize = 560
        val plotX = controlPanel.rightEdgeWithGap()
        val plot = createClassifierProjectionPlot(classifierNetwork, "Decision Regions")
        place(plot, plotX, SIM_WINDOW_GAP, plotSize, plotSize)
        // The network gets a tall column beside the plot when the desktop is wide enough, else the space under the
        // control panel
        val networkX = plotX + plotSize + SIM_WINDOW_GAP
        val room = desktopPane.width - networkX - SIM_WINDOW_GAP
        if (room >= 240) {
            place(networkComponent, networkX, SIM_WINDOW_GAP, room.coerceAtMost(360), plotSize)
        } else {
            val networkY = controlPanel.bottomEdgeWithGap()
            val networkHeight = (desktopPane.height - networkY - SIM_WINDOW_GAP).coerceIn(220, 340)
            place(networkComponent, SIM_WINDOW_GAP, networkY, controlPanel.width, networkHeight)
        }
    }
}

enum class ToyDataset(private val description: String) {
    Blobs("Blobs"),
    Moons("Moons"),
    Circles("Circles"),
    Xor("XOR");

    override fun toString() = description
}

class DecisionBoundarySettings {
    var dataset = ToyDataset.Moons
    var noise = 0.25
    var seed = 1L
    var classifier = ClassifierChoice.LogisticRegression
    var k = 15
    var kernelWidth = 0.5
    var maxDepth = 20
    var numTrees = 100
    val numSamples = 200
    private val blobCenters = listOf(doubleArrayOf(-1.0, -1.0), doubleArrayOf(1.0, 1.0))

    fun createDataset(): ClassificationDataset = when (dataset) {
        ToyDataset.Blobs -> makeBlobs(numSamples, 2, 2, noise, randomSeed = seed, centers = blobCenters)
        ToyDataset.Moons -> makeMoons(numSamples, noise, seed)
        ToyDataset.Circles -> makeCircles(numSamples, factor = 0.5, noise = noise, randomSeed = seed)
        ToyDataset.Xor -> makeXorClusters(numSamples, noise, seed)
    }

    fun createTrainedClassifier(data: ClassificationDataset) =
        trainClassifier(classifier, data, k = k, kernelWidth = kernelWidth, maxDepth = maxDepth, numTrees = numTrees)
}
