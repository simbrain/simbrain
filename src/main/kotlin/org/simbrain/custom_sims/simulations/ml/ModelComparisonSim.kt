/**
 * The four classifiers from the decision boundary simulation trained on real tabular datasets, so students can
 * compare training and test accuracy, see a guessing baseline, and see how feature scaling affects distance-based
 * models. Changing the dataset rebuilds the [ClassifierNetwork], since its input and output sizes change.
 */
package org.simbrain.custom_sims.simulations.ml

import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import org.simbrain.custom_sims.*
import org.simbrain.network.core.setLabels
import org.simbrain.network.layouts.LineLayout
import org.simbrain.network.smile.ClassifierNetwork
import org.simbrain.network.trainers.ClassificationDataset
import org.simbrain.network.trainers.createClassificationDataset
import org.simbrain.network.trainers.split
import org.simbrain.network.trainers.standardized
import org.simbrain.network.util.Alignment
import org.simbrain.network.util.alignNetworkModels
import org.simbrain.util.createClassifierProjectionPlot
import org.simbrain.util.place
import smile.io.Read
import java.awt.Rectangle

val modelComparison = newSim {
    workspace.clearWorkspace()

    var tableChoice = TableDataset.Diabetes
    var standardize = false
    var classifierChoice = ClassifierChoice.LogisticRegression

    val networkComponent = addNetworkComponent("Network")
    val network = networkComponent.network

    var data = tableChoice.load()

    // Standardize with statistics from the training rows only, which are the rows every classifier's own split
    // (ratio 0.8, seed 42) assigns to training, so nothing about the test rows reaches the models
    fun currentDataset() = if (standardize) data.dataset.run { standardized(reference = split(0.8).first) } else data.dataset

    fun buildClassifierNetwork() = ClassifierNetwork(trainClassifier(classifierChoice, currentDataset())).apply {
        if (data.featureNames.size <= 10) {
            inputNeuronGroup.setLabels(data.featureNames)
        }
        outputNeuronGroup.setLabels(data.classNames)
        (outputNeuronGroup.layout as LineLayout).spacing = (data.classNames.maxOf { it.length } * 7.0 + 20).coerceAtLeast(60.0)
        outputNeuronGroup.applyLayout()
        alignNetworkModels(inputNeuronGroup, outputNeuronGroup, Alignment.VERTICAL)
    }

    var classifierNetwork = buildClassifierNetwork()
    network.addNetworkModels(classifierNetwork)

    addSidebarInfo(
        """
        # Model Comparison

        This simulation takes the four classifiers from *Simulations › Machine learning › Iris classifier* to three harder, real-world tables. Iris is small and clean, and its species separate clearly, even in a plot. These tables have more features, more classes, or more noise. The models disagree, some fail unless the data is prepared, and the classes overlap heavily in the plot. That makes them good practice for judging and comparing models.

        Each table is a set of rows, one per example. Each column is a measured *feature*, and one column is the *class* to predict. To see how the four models draw their boundaries, try *Simulations › Machine learning › Decision boundaries*.

        # Simulation Details

        ## The datasets

        - `Diabetes`: 768 patients from the Pima Indian diabetes study. The 8 features are number of pregnancies (`preg`), blood glucose (`plas`), blood pressure (`pres`), skin fold thickness (`skin`), insulin (`insu`), body mass index (`mass`), a family history score (`pedi`), and `age`. The class is whether the patient tested positive for diabetes.
        - `Glass`: 214 glass fragments, of the kind collected at crime scenes. The 9 features are refractive index (`RI`) and the amounts of 8 chemical elements. The class is one of 6 types of glass, such as window glass or headlamps.
        - `Image segments`: 1,500 small patches of outdoor photographs. The 19 features describe color, brightness, and edges in each patch. The class is one of 7 surfaces: brick, sky, foliage, cement, window, path, or grass.

        ## Training, test, and the baseline

        Each table is split at random: 80% of the rows train the model and 20% are held back for testing. Training accuracy shows how well a model fits the rows it learned from. Test accuracy, on rows it never saw, is the honest score.

        A useful accuracy also has to beat a trivial strategy. The *baseline* is the score you would get by always guessing whichever class is most common in the training data. In `Diabetes` about two thirds of patients tested negative, so a model must beat roughly 67% to have learned anything.

        ## Feature scaling

        Features come in very different units. In `Diabetes`, insulin values run into the hundreds, while the family history score is usually below 1. K nearest neighbors and support vector machines both judge similarity by distance between rows, so without rescaling the large-valued features dominate and the small ones are effectively ignored. *Standardizing* rescales every feature to have mean 0 and standard deviation 1, so each counts equally. Logistic regression and random forests are much less affected by scale.

        ## The plot

        The plot squeezes all of a table's features onto two axes using principal component analysis (PCA), which keeps as much of the spread between rows as two axes can. Each row is a dot colored by its true class: training rows are filled and test rows are hollow. The row currently on the network's inputs is outlined in red. Classes that overlap in the plot are often the ones the models confuse.

        PCA is only one way to fit many features onto two axes. Choose `Sammon` from the menu at the top left of the plot and press the run button in the plot's toolbar: it moves the dots around until rows that are close in the full feature space are close in the plot too. `TSNE` works in a similar way, but it does not work as well on `Image segments`.

        ## Model settings

        `K nearest neighbors` uses `K` = 5. `Support vector machine` uses a Gaussian kernel with width 3. `Random forest` uses 100 trees with no depth limit. To change these, double-click the classifier in the network, edit its parameters, and press `Train` in that dialog.

        ## Control Panel Settings

        - `Dataset`: which table to use. Changing it rebuilds the network to match the number of features and classes.
        - `Standardize features`: rescale every feature to mean 0 and standard deviation 1 before training.
        - `Classifier`: which model is shown in the network. It is trained as soon as you pick it.
        - `Training accuracy` and `Test accuracy`: scores for the model in the network.
        - `Compare all models`: train all four models on the current dataset and show their training and test accuracy, with the baseline, in a table.
        - `Class`: the class of the row that the buttons below load.
        - `Test row` and `Training row`: put a randomly chosen test or training row of the selected class on the inputs.
        - `Previous` and `Next`: step through the rows in order, loading each onto the inputs. They use test rows or training rows, whichever you loaded last.
        - `Status`: which row is loaded, its true class, and the model's prediction.

        # What to Do

        1. Training accuracy can mislead. With `Diabetes` selected, press `Compare all models`. The random forest and the support vector machine both score 100% on the training data, but far less on the test data. A perfect training score usually means the model memorized the rows, not that it will work on new patients.
        2. Simple can win. On the same table, plain logistic regression has the best test accuracy, near 79%. Compare every score to the baseline of about two thirds. Medical data like this is noisy, and a flexible model has more room to fit that noise.
        3. Scaling matters for some models. The support vector machine's test accuracy is no better than the baseline: it has memorized the training rows and calls every new patient negative. Check `Standardize features` and press `Compare all models` again. The support vector machine improves by about 10 points, while logistic regression and the random forest do not change.
        4. A bigger, cleaner table. Set `Dataset` to `Image segments` and compare the models, with and without `Standardize features`. Without it, the support vector machine gets only about half of the test rows right; with it, all four models get at least 94%. The random forest is the best of the four with or without scaling, which is typical for tables like this.
        5. A small, hard table. Set `Dataset` to `Glass`. With 6 classes and only 43 test rows, every model makes many mistakes, and a single lucky or unlucky row moves test accuracy by more than 2 points. Small differences between models here mean little.
        6. Try some rows. Pick a `Classifier`, choose a `Class`, and press `Test row` a few times. Each press puts a random test row of that class on the input neurons, the output neuron for the predicted class turns on, and `Status` says whether the prediction is right. Try it on `Glass` to see which kinds of glass get mixed up. Then choose `Random forest` and press `Training row` a few times: it gets every one right, because it has seen them all before.

        ## Takeaways

        - Judge a model by test accuracy, and compare it to the baseline.
        - The best model depends on the data. Try a simple model first.
        - Standardize features before using distance-based models such as K nearest neighbors and support vector machines.
        - Random forests are a strong default for tables and need little preparation.
        - On small datasets, differences of a few points between models are often just luck in which rows landed in the test set.
        """.trimIndent()
    )

    withGui {
        val explorer = ClassifierExplorer(
            workspace = workspace,
            network = classifierNetwork,
            dataset = currentDataset(),
            classNames = data.classNames,
            classifierChoice = classifierChoice,
            exampleNoun = "row",
            describeData = { "$tableChoice, features ${if (standardize) "standardized" else "not standardized"}" }
        )
        var plotBounds = Rectangle()
        var plot = createClassifierProjectionPlot(classifierNetwork, "Data")

        val controlPanel = createControlPanel("Model Comparison", SIM_WINDOW_GAP, SIM_WINDOW_GAP) {
            addComboBox(
                "Dataset",
                TableDataset.entries,
                tableChoice,
                toolTip = "Table of examples to train and test on."
            ) {
                tableChoice = it
                launch {
                    explorer.lock.withLock {
                        data = tableChoice.load()
                        classifierChoice = explorer.classifierChoice
                        val old = classifierNetwork
                        classifierNetwork = buildClassifierNetwork()
                        old.delete()
                        network.addNetworkModels(classifierNetwork)
                        explorer.network = classifierNetwork
                        explorer.dataset = currentDataset()
                        explorer.classNames = data.classNames
                        explorer.resetLocked()
                        plot = createClassifierProjectionPlot(classifierNetwork, "Data")
                        place(plot, plotBounds.x, plotBounds.y, plotBounds.width, plotBounds.height)
                    }
                }
            }
            addCheckBox(
                "Standardize features",
                standardize,
                toolTip = "Rescale every feature to mean 0 and standard deviation 1 before training."
            ) {
                standardize = it
                explorer.lock.withLock {
                    explorer.dataset = currentDataset()
                    explorer.retrainLocked()
                }
            }
            addSeparator()
            explorer.addModelControls(this)
            addSeparator()
            explorer.addExampleControls(this, ClassifierExplorer.ExampleStyle.ClassList)
        }.awaitLayout()
        explorer.showAccuracy()

        plotBounds = placeExplorerWindows(controlPanel, plot, networkComponent)
    }
}

enum class TableDataset(private val description: String, val fileName: String) {
    Diabetes("Diabetes", "diabetes.arff"),
    Glass("Glass", "glass.arff"),
    ImageSegments("Image segments", "segment-challenge.arff");

    override fun toString() = description

    /**
     * Read the table from `simulations/tables`, treating the last column as the class.
     */
    fun load(): LoadedTable {
        val frame = Read.arff("simulations/tables/$fileName")
        val targetColumn = frame.ncol() - 1
        val dataset = createClassificationDataset(frame, IntArray(targetColumn) { it }, targetColumn)
        val classNames = frame.column(targetColumn).toStringArray().toSortedSet().map { label ->
            label.trim('\'').replace('_', ' ').split(' ').joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }
        }
        return LoadedTable(dataset, frame.names().take(targetColumn), classNames)
    }
}

class LoadedTable(val dataset: ClassificationDataset, val featureNames: List<String>, val classNames: List<String>)
