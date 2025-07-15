package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.addNetworkComponent
import org.simbrain.custom_sims.addSidebarInfo
import org.simbrain.custom_sims.newSim
import org.simbrain.network.core.setLabels
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
        inputNeuronGroup.isAllClamped = true

        (outputNeuronGroup.layout as LineLayout).spacing = 100.0
        outputNeuronGroup.applyLayout()
        inputNeuronGroup.setLabels(listOf("Sepal\nlength", "Sepal\nwidth", "Petal\nlength", "Petal\nwidth"))
        outputNeuronGroup.setLabels(listOf("Setosa", "Versicolor", "Virginica"))
        alignNetworkModels(inputNeuronGroup, outputNeuronGroup, Alignment.VERTICAL)
    }

    // Set input data for iris to training data
    smileClassifier.inputNeuronGroup.inputData = classifier.trainingData.inputs.toMatrix()

    network.addNetworkModels(smileClassifier)

    addSidebarInfo(
    """
    # Introduction

    This simulation helps you learn how a machine learning model can classify data based on simple inputs, without needing to write any code.
   
    ## Background
    
    This is a famous dataset in machine learning, the [Iris Dataset](https://en.wikipedia.org/wiki/Iris_flower_data_set). If you are interested, you can learn more
    about the dataset [here](https://archive.ics.uci.edu/ml/datasets/iris).
    
    The dataset has `150` rows and `5` columns. Each row corresponds to measurements for a flower. The first four columns contain numerical data and correspond to 
    measurements of the petals and sepals (a sepal is a petal-like structure that encloses the bud as the plant develops). The fifth column contains categorical data
    and says which of three kinds of iris the measurements correspond to. The first `50` rows correspond to Setosa, the second `50` to versicolor, the last `50` to 
    Virginica. Notice that the Setosa looks different than the other two. Here are pictures of the three kinds of iris.
    
    ![Iris-setosa](//localfiles/simulations/images/iris/irissetosa.png)  
    ![Iris-versicolor](//localfiles/simulations/images/iris/irisversicolor.png)  
    ![Iris-virginica](//localfiles/simulations/images/iris/irisvirginica.png)  
    
    # Simulation Details
    
    In this simulation, there are two types of machine learning algorithms that you use to classify the Iris Dataset, the K-Nearest Neighbor (KNN) and the Logistic
    Regression algorithms. In simple terms, the K-Nearest Neighbor predicts the output of a new data point by choosing the closest data point to your input that exists
    in the dataset. Whereas in the Logistic Regression, it predicts the output of your inputs based on a probability value. In this case, the output is the predicted
    flower species. Note that machine learning is just pattern recognition. This model was trained on real flower data and now you're using it to make predictions.
    
    In the network window, the output layer represents the flower species where each neuron is a flower species and will activate based on the model's prediction. The
    input layer represents the `4` measurements of each flower species, here you will change the activations of each neuron and that will produce a prediction in the
    output layer.
    
    # What to Do
    
    In this simulation, most of the exploration comes from the input layer after you have trained the model, Below are the steps:
    
    1) Choose which algorithm you want to use.
    
    2) Double click on the classifier (e.g., `K Nearest Neighbor`) and click the `Train` button.
    
    3) Now, click the `play` button to run the simulation.
    
    4) Then, try changing one feature at a time by adjusting the activations of the neurons by clicking on the neurons and pressing the up/down arrow keys. 
        
        - Note: You can press `Ctrl+E` after clicking on a neuron to change the neuron's activation more accurately or by double clicking a neuron.
        
    4) Observe how each feature influences the predicted output.
    
    ## Sample Values
    
    Here's some sample values that you can use from the Iris Dataset to get a view of the landscape of the dataset using the note mentioned above.
    
    `Flower Species`: `Sepal length`, `Sepal width`, `Petal length`, `Petal width`.
    
    - `Setosa`: `5.1`, `3.5`, `1.4`, `0.2` 
    - `Versicolor`: `6.0`, `2.2`, `4.0`, `1.0`  
    - `Virginica`: `6.5`, `3.0`, `5.8`, `2.2`

    # Credits
    
    Daisy Mayorga
    
    [Jeff Yoshimi](www.jeffyoshimi.net)
    
    Kanly Thao
    
        """.trimIndent()
    )

    // Location of the network in the desktop
    withGui {
        place(networkComponent) {
            location = point(0, 0)
            width = 800
            height = 500
        }
    }

}