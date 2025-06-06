package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.addNetworkComponent
import org.simbrain.custom_sims.addSidebarInfo
import org.simbrain.custom_sims.newSim
import org.simbrain.network.layouts.LineLayout
import org.simbrain.network.smile.ClassifierNetwork
import org.simbrain.network.smile.classifiers.KNNClassifier
import org.simbrain.network.smile.classifiers.LogisticRegClassifier
import org.simbrain.network.trainers.createClassificationDataset
import org.simbrain.network.util.Alignment
import org.simbrain.network.util.alignNetworkModels
import org.simbrain.util.place
import org.simbrain.util.point
import org.simbrain.util.showOptionDialog
import org.simbrain.util.toMatrix
import smile.io.Read

/**
 * Train a smile classifier on Iris data.
 */
val irisClassifier = newSim {

    val option = showOptionDialog(
        "Choose Classifier Type",
        "Choose a classifier type to train on Iris data.",
        arrayOf("KNN", "Logistic Regression")
    )

    // Last column is target data
    val iris = Read.arff("simulations/tables/iris.arff")

    val classificationDataset = createClassificationDataset(
        dataFrame = iris,
        inputColumns = intArrayOf(0, 1, 2, 3),
        targetColumn = 4
    )

    val classifier = when (option) {
        0 -> KNNClassifier(classificationDataset)
        1 -> LogisticRegClassifier(classificationDataset)
        else -> return@newSim
    }

    // Basic setup
    workspace.clearWorkspace()
    val networkComponent = addNetworkComponent("Network")
    val network = networkComponent.network

    val smileClassifier = ClassifierNetwork(classifier).apply {
    // Last column is target data
    val iris = Read.arff("simulations/tables/iris.arff")

    val docViewerText = when (classifier) {
        is LogisticRegClassifier -> """
        # Iris Classifier: What to Do

        ## What's the Goal of This Simulation?
        This simulation helps you **learn how a machine learning model can classify data** based on simple inputs, without needing to write any code.
        
        You're using a model called **logistic regression** to predict which type of iris flower a set of measurements most likely belongs to. It’s based on real-world data and helps you understand how pattern recognition works in machine learning.
        
        If you’re interested, [you can learn more about the dataset here](https://archive.ics.uci.edu/ml/datasets/iris).
        
        
        ---
        ### Background
        
        This is a famous dataset in machine learning, the Iris Dataset
        (https://en.wikipedia.org/wiki/Iris_flower_data_set).
        
        Here is some background about it.
        
        The dataset has 150 rows and 5 columns. Each row corresponds to measurements for a flower. The
        first four columns contain numerical data and correspond to measurements of the petals and sepals
        (a sepal is a petal-like structure that encloses the bud as the plant develops). The fifth column
        contains categorical data and says which of three kinds of iris the measurements correspond to. The
        first 50 rows correspond to Setosa, the second 50 to versicolor, the last 50 to Virginica Notice that the
        Setosa looks different than the other two. Here are pictures of the three kinds of iris.
        
        ![Iris-setosa](//localfiles/simulations/images/irisimages/irissetosa.png)  
        ![Iris-versicolor](//localfiles/simulations/images/irisimages/irisversicolor.png)  
        ![Iris-virginica](//localfiles/simulations/images/irisimages/irisvirginica.png)  
        
        ---
        
        
        ## What You’re Seeing in the Network
        
        - **Input Layer (bottom 4 circles):**  
          Each circle represents one of the 4 measurements. You’ll enter numeric values here (like `5.1`, `3.5`, `1.4`, `0.2`).
        
        - **Yellow Arrow:**  
          Shows that your inputs are being sent to the classifier.
        
        - **Logistic Regression Block:**  
          Holds the machine learning model (from the Smile library). It takes your inputs and tries to predict the flower species.
        
        - **Output Layer (top 3 circles):**  
          Each circle represents a flower species. One of these will activate to show the model’s prediction.
        
        ---
        
        ## What You Can Do
        
        1. **Try different inputs**  
           Enter values in the input layer to simulate new flowers and see how the classifier responds.
        
        2. **Compare flower types**  
           Use these sample values from the dataset:
        
           - *Setosa:* `5.1, 3.5, 1.4, 0.2`  
           - *Versicolor:* `6.0, 2.2, 4.0, 1.0`  
           - *Virginica:* `6.5, 3.0, 5.8, 2.2`
        
        3. **Explore the logic**  
           Try changing one feature at a time. See how the prediction changes and think about what that means.
        
        4. **Reflect**  
           Machine learning is just pattern recognition. This model was trained on real flower data and now you're using it to make predictions.
        
        ### 💡 Tip: Adjusting Inputs in Detail

        Click on any of the input neurons to open the **Edit Neuron** window. There you can:

        - Set the activation value directly
        - Clamp the neuron to hold your input steady
        - Use the arrows to nudge values up or down
        - (Advanced) Explore how different update rules behave

        These tools let you fine-tune your inputs and explore how the model reacts.

        
            """.trimIndent()

                is KNNClassifier -> """
                # K-Nearest Neighbors Classifier
        
                This classifier assigns a class to a new sample by finding the most common label among its k-nearest training samples.
                
                - No actual "training" — it memorizes all training data.
                - Predictions are made based on distance (usually Euclidean).
                - Great for non-linear or messy boundaries.
            """.trimIndent()

                else -> "No classifier documentation available."
    }

    val docViewer = addSidebarInfo(docViewerText)

    classifier.trainingData.featureVectors = iris.select(0,1,2,3).toArray()
    classifier.trainingData.targetLabels = iris.column(4).toStringArray()
    val smileClassifier = SmileClassifier(classifier).apply {
        inputNeuronGroup.isAllClamped = true
        inputNeuronGroup.setLabels(listOf("Sepal\nlength", "Sepal\nwidth", "Petal\nlength", "Petal\nwidth"))

        (outputNeuronGroup.layout as LineLayout).spacing = 100.0
        outputNeuronGroup.applyLayout()

        alignNetworkModels(inputNeuronGroup, outputNeuronGroup, Alignment.VERTICAL)
    }
    smileClassifier.train()

    // Set input data for iris to training data
    smileClassifier.inputNeuronGroup.inputData = classifier.trainingData.inputs.toMatrix()

    network.addNetworkModels(smileClassifier)

    // Location of the network in the desktop
    withGui {
        place(networkComponent) {
            location = point(0, 0)
            width = 800
            height = 500
        }
    }

}