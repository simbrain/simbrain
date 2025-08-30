package org.simbrain.network.trainers

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Layer
import org.simbrain.network.core.Network
import org.simbrain.network.core.NeuronArray
import org.simbrain.network.core.WeightMatrix
import org.simbrain.network.updaterules.LinearRule
import org.simbrain.network.updaterules.SoftmaxRule
import org.simbrain.util.*
import smile.math.matrix.Matrix
import kotlin.random.Random

class TrainingUtilsTest {

    val net = Network()
    val na1 = NeuronArray(2).apply { isClamped = true }
    val na2 = NeuronArray(3)
    val na3 = NeuronArray(2)
    val wm1 = WeightMatrix(na1, na2)
    val wm2 = WeightMatrix(na2, na3)

    init {
        listOf(na1, na2, na3).forEach {
            it.clear()
        }
        net.addNetworkModelsAsync(na1, na2, na3, wm1, wm2)
    }

    @Test
    fun `test neuron array error`() {
        na1.setActivations(doubleArrayOf(-1.0, 1.0))
        val error = BackpropLossFunction.SSE.outputError(na1.activations, doubleArrayOf(1.0, 1.0).toColumnVector())
        assertArrayEquals(doubleArrayOf(4.0, 0.0), error.toDoubleArray())
    }

    @Test
    fun `test bias update`() {
        na1.biases = doubleArrayOf(1.0, 1.0).toColumnVector()
        val errors = doubleArrayOf(0.0, 1.0).toColumnVector() - na1.activations
        // Change to bias is 0,1, so biases should become 1,2
        na1.updateBiases(errors, 1.0)
        assertArrayEquals(doubleArrayOf(1.0, 2.0 ), na1.biases.toDoubleArray())
        na1.updateBiases(errors, 1.0)
        assertArrayEquals(doubleArrayOf(1.0, 3.0 ), na1.biases.toDoubleArray())
        na1.updateBiases(errors, .1)
        assertArrayEquals(doubleArrayOf(1.0, 3.1 ), na1.biases.toDoubleArray())
        errors.mul(-1.0)
        na1.updateBiases(errors, 1.0)
        assertArrayEquals(doubleArrayOf(1.0, 2.1 ), na1.biases.toDoubleArray())
    }

    @Test
    fun `test forward pass`() {
        val inputs = Matrix.column(doubleArrayOf(-1.0, 1.0))
        with(net) {
            val layers = computeOrderedUpdatePath(setOf(na1), na3)
            layers.forwardPass(listOf(inputs), listOf(na1))
            //listOf(wm1, wm2).printActivationsAndWeights(true)
        }
        assertArrayEquals(inputs.toDoubleArray(), wm2.target.activations.toDoubleArray())
    }

    @Test
    fun `test connector chain`() {
        // Should return [wm1, wm2]
        val chain = getConnectorChain(na1, na3)
        assertEquals(2, chain.size)
        assertEquals(wm1, chain[0])
        assertEquals(wm2, chain[1])
    }

    @Test
    fun `test weight delta computation with specific values`() {
        val na1 = NeuronArray(2)
        val na2 = NeuronArray(3)
        val wm = WeightMatrix(na1, na2).apply {
            weights.copyFrom(Matrix.of(arrayOf(
                doubleArrayOf(1.0, 2.0),
                doubleArrayOf(3.0, 4.0),
                doubleArrayOf(5.0, 6.0)
        )))
        }
        na1.setActivations(doubleArrayOf(1.0, 2.0))
        val errorSignal = Matrix.column(doubleArrayOf(.5, -.5, .5))

        val initialWeights = wm.weights.clone()
        val weightDeltas = wm.computeWeightDeltas(errorSignal)
        // Expected weight deltas: errorSignal * source.activations.T
        val expectedDeltas = errorSignal.mm(na1.activations.transpose())
        
        // Verify that computeWeightDeltas returns the correct deltas
        assertArrayEquals(expectedDeltas.flatten(), weightDeltas.flatten())
        
        // Manually apply the deltas to test the expected behavior
        wm.weights.add(weightDeltas)
        val expectedWeights = initialWeights.add(expectedDeltas)
        assertArrayEquals(expectedWeights.flatten(), wm.weightArray)
    }

    @Test
    fun `test backpropagated error with specific values`() {
        val na1 = NeuronArray(2)
        val na2 = NeuronArray(3)
        val wm = WeightMatrix(na1, na2)

        na1.setActivations(doubleArrayOf(1.0, 2.0))
        wm.weights.copyFrom(Matrix.of(arrayOf(
            doubleArrayOf(5.0, 8.0),
            doubleArrayOf(1.0, -1.0),
            doubleArrayOf(2.0, -2.0)
        )))

        val errorSignal = Matrix.column(doubleArrayOf(0.0, 1.0, 0.5))

        // Expected backpropagated error: weightMatrix.T * errorSignal
        // Expect 2, -2
        val expectedBackpropagatedErrors = wm.weights.transpose().mm(errorSignal)

        val backpropagatedErrors = wm.backpropagateError(errorSignal)
        for (i in 0 until backpropagatedErrors.nrow()) {
            assertEquals(expectedBackpropagatedErrors[i, 0], backpropagatedErrors[i, 0], 1e-6)
        }
    }

    @Test
    fun `test computeOrderedUpdatePath for linear path from start to end`() {
        val a = NeuronArray(2)
        val b = NeuronArray(2)
        val c = NeuronArray(2)
        WeightMatrix(a, b)
        WeightMatrix(b, c)
        val order = computeOrderedUpdatePath(setOf(a), c).toList()
        assertEquals(listOf(a, b, c), order)
    }

    @Test
    fun `test computeOrderedUpdatePath when start equals end`() {
        // Not a case we want but a valid use case for the function
        val a = NeuronArray(2)
        val order = computeOrderedUpdatePath(setOf(a), a).toList()
        assertEquals(listOf(a), order)
    }

    @Test
    fun `test computeOrderedUpdatePath throws IllegalArgumentException when start and end are disconnected`() {
        val a = NeuronArray(2)
        val b = NeuronArray(2) // no connection between a and b

        org.junit.jupiter.api.assertThrows<IllegalArgumentException> {
            computeOrderedUpdatePath(setOf(a), b)
        }
    }

    @Test
    fun `test computeOrderedUpdatePath with branching connections`() {
        val a = NeuronArray(2)
        val b = NeuronArray(2)
        val c = NeuronArray(2)
        val d = NeuronArray(2)

        WeightMatrix(a, b)
        WeightMatrix(a, c)
        WeightMatrix(c, d)

        val order = computeOrderedUpdatePath(setOf(a), d).toList()

        // Ensure topological order and presence
        assertTrue(order.indexOf(a) < order.indexOf(c))
        assertTrue(order.indexOf(c) < order.indexOf(d))
        assertTrue(order.containsAll(listOf(a, c, d)))
    }

    @Test
    fun `test skip connection backprop with different layer sizes`() {
        val net = Network()
        
        // Create layers with different sizes to test skip connection handling
        val inputLayer = NeuronArray(3).apply { 
            label = "input"
            isClamped = true 
        }
        val hiddenLayer = NeuronArray(5).apply { 
            label = "hidden" 
        }
        val outputLayer = NeuronArray(2).apply { 
            label = "output" 
        }

        // Create connections: input -> hidden -> output, plus skip connection input -> output
        val wm1 = WeightMatrix(inputLayer, hiddenLayer).apply { label = "input_to_hidden" }
        val wm2 = WeightMatrix(hiddenLayer, outputLayer).apply { label = "hidden_to_output" }
        val skipWm = WeightMatrix(inputLayer, outputLayer).apply { label = "skip_connection" }

        runBlocking {
            net.addNetworkModelsAsync(inputLayer, hiddenLayer, outputLayer, wm1, wm2, skipWm)
        }

        // Initialize weights to known values
        wm1.weights.fill(0.1)
        wm2.weights.fill(0.2) 
        skipWm.weights.fill(0.3)

        // Create supervised model and test data
        val supervisedModel = SupervisedModel(inputLayer, outputLayer)
        runBlocking {
            net.addNetworkModelAsync(supervisedModel)
        }

        val inputData = Matrix.of(arrayOf(
            doubleArrayOf(1.0, 0.0, 1.0),
            doubleArrayOf(0.0, 1.0, 0.0),
            doubleArrayOf(1.0, 1.0, 1.0)
        ))
        val targetData = Matrix.of(arrayOf(
            doubleArrayOf(1.0, 0.0),
            doubleArrayOf(0.0, 1.0),
            doubleArrayOf(1.0, 1.0)
        ))

        supervisedModel.trainingSet = TrainingDataset(inputData.toMutableListOfLists(), targetData.toMutableListOfLists())

        // Test that forward pass works with skip connections
        runBlocking {
            with(net) {
                supervisedModel.inputLayer.activations = inputData.row(0).toColumnVector()
                supervisedModel.forwardPass()
                
                // Verify outputs are reasonable (not NaN, finite)
                supervisedModel.outputLayer.activationArray.forEach { activation ->
                    assertTrue(activation.isFinite(), "Output activation should be finite")
                }
            }

            // Test backprop with skip connections
            val trainer = SupervisedTrainer(net, supervisedModel)
            val initialError = with(net) { 
                supervisedModel.inputLayer.activations = inputData.row(0).toColumnVector()
                supervisedModel.forwardPass()
                BackpropLossFunction.SSE.scalarLoss(supervisedModel.outputLayer.activations, targetData.row(0).toColumnVector())
            }

            // Train for a few iterations
            repeat(10) {
                trainer.trainOnce()
            }

            // Verify error decreased
            val finalError = trainer.lastTrainingError
            assertTrue(finalError < initialError, "Training error should decrease with skip connections: $initialError -> $finalError")
        }
    }

    @Test 
    fun `test multiple skip connections accumulate correctly`() {
        val net = Network()
        
        // Create a network with multiple paths to the same layer
        val inputLayer = NeuronArray(2).apply { 
            label = "input"
            isClamped = true 
        }
        val branch1Layer = NeuronArray(3).apply { label = "branch1" }
        val branch2Layer = NeuronArray(4).apply { label = "branch2" } 
        val outputLayer = NeuronArray(2).apply { label = "output" }

        // Create multiple paths: input -> branch1 -> output, input -> branch2 -> output, input -> output
        val wm1 = WeightMatrix(inputLayer, branch1Layer)
        val wm2 = WeightMatrix(inputLayer, branch2Layer)
        val wm3 = WeightMatrix(branch1Layer, outputLayer)
        val wm4 = WeightMatrix(branch2Layer, outputLayer)
        val skipWm = WeightMatrix(inputLayer, outputLayer)

        runBlocking {
            net.addNetworkModelsAsync(inputLayer, branch1Layer, branch2Layer, outputLayer, wm1, wm2, wm3, wm4, skipWm)
        }

        // Initialize weights
        listOf(wm1, wm2, wm3, wm4, skipWm).forEach { wm ->
            wm.weights.fill(0.1)
        }

        val supervisedModel = SupervisedModel(inputLayer, outputLayer)
        runBlocking {
            net.addNetworkModelAsync(supervisedModel)
        }

        val inputData = Matrix.of(arrayOf(doubleArrayOf(1.0, -1.0)))
        val targetData = Matrix.of(arrayOf(doubleArrayOf(0.5, -0.5)))

        supervisedModel.trainingSet = TrainingDataset(inputData.toMutableListOfLists(), targetData.toMutableListOfLists())

        // Test that accumulation works properly - no exceptions thrown
        val trainer = SupervisedTrainer(net, supervisedModel).apply {
            config.learningRate = 0.01  // Set learning rate so weights actually update
        }
        
        runBlocking {
            // Verify gradients were applied to all weight matrices
            val initialWeights = mapOf(
                "wm1" to wm1.weights.clone(),
                "wm2" to wm2.weights.clone(), 
                "wm3" to wm3.weights.clone(),
                "wm4" to wm4.weights.clone(),
                "skip" to skipWm.weights.clone()
            )
            
            // Run multiple training iterations
            repeat(10) {
                trainer.trainOnce()
            }
            

            
            // Check that all weights were updated (indicating proper gradient flow)
            fun weightsChanged(current: Matrix, initial: Matrix): Boolean {
                return (0 until current.nrow()).any { i ->
                    (0 until current.ncol()).any { j ->
                        kotlin.math.abs(current[i,j] - initial[i,j]) > 1e-10
                    }
                }
            }
            
            assertTrue(weightsChanged(wm1.weights, initialWeights["wm1"]!!), "wm1 weights should be updated")
            assertTrue(weightsChanged(wm2.weights, initialWeights["wm2"]!!), "wm2 weights should be updated")
            assertTrue(weightsChanged(wm3.weights, initialWeights["wm3"]!!), "wm3 weights should be updated")
            assertTrue(weightsChanged(wm4.weights, initialWeights["wm4"]!!), "wm4 weights should be updated")
            assertTrue(weightsChanged(skipWm.weights, initialWeights["skip"]!!), "skip weights should be updated")
        }
    }

    @Test
    fun `test residual connection pattern`() {
        val net = Network()
        
        // Create a residual block pattern: input -> hidden -> output, with input -> output skip
        val inputLayer = NeuronArray(4).apply { 
            label = "input"
            isClamped = true 
        }
        val hiddenLayer = NeuronArray(4).apply { 
            label = "hidden" 
        }
        val outputLayer = NeuronArray(4).apply { 
            label = "output" 
        }

        val mainPath = WeightMatrix(inputLayer, hiddenLayer)
        val skipConnection = WeightMatrix(inputLayer, outputLayer) 
        val outputPath = WeightMatrix(hiddenLayer, outputLayer)

        runBlocking {
            net.addNetworkModelsAsync(inputLayer, hiddenLayer, outputLayer, mainPath, skipConnection, outputPath)
        }

        // Initialize weights  
        mainPath.weights.fill(0.1)
        outputPath.weights.fill(0.1)
        skipConnection.weights.setValuesInPlace { i, j -> if (i == j) 0.5 else 0.05 } // Near-identity for residual

        val supervisedModel = SupervisedModel(inputLayer, outputLayer)
        runBlocking {
            net.addNetworkModelAsync(supervisedModel)
        }

        val inputData = Matrix.of(arrayOf(
            doubleArrayOf(1.0, 0.0, -1.0, 0.5),
            doubleArrayOf(0.5, -0.5, 1.0, -1.0)
        ))
        val targetData = Matrix.of(arrayOf(
            doubleArrayOf(1.5, 0.2, -0.8, 0.7),  
            doubleArrayOf(0.3, -0.3, 1.2, -0.8)
        ))

        supervisedModel.trainingSet = TrainingDataset(inputData.toMutableListOfLists(), targetData.toMutableListOfLists())

        val trainer = SupervisedTrainer(net, supervisedModel).apply {
            config.learningRate = 0.01
        }

        runBlocking {
            run {
                trainer.trainBatch(0 until 1)
                trainer.lastTrainingError
            }

            // Train multiple iterations
            repeat(50) {
                trainer.trainOnce()
            }

            val finalError = trainer.lastTrainingError
            // With residual connections, the network should be able to learn and achieve reasonable error
            assertTrue(finalError.isFinite(), "Error should be finite")
            assertTrue(finalError < 2.0, "Should achieve reasonable error with residual connections: final=$finalError")
        }
    }

    @Test
    fun `test error signal accumulation correctness`() {
        val net = Network()
        
        // Simple case: two paths to same source layer  
        val sourceLayer = NeuronArray(2).apply { 
            label = "source"
            isClamped = true 
        }
        val intermediate1 = NeuronArray(2).apply { label = "int1" }
        val intermediate2 = NeuronArray(3).apply { label = "int2" }
        val targetLayer = NeuronArray(2).apply { label = "target" }

        val wm1 = WeightMatrix(sourceLayer, intermediate1)
        val wm2 = WeightMatrix(sourceLayer, intermediate2)  
        val wm3 = WeightMatrix(intermediate1, targetLayer)
        val wm4 = WeightMatrix(intermediate2, targetLayer)

        runBlocking {
            net.addNetworkModelsAsync(sourceLayer, intermediate1, intermediate2, targetLayer, wm1, wm2, wm3, wm4)
        }

        // Set specific weights to test error accumulation
        wm1.weights.setValuesInPlace { i, j -> (i + 1) * (j + 1) * 0.1 }
        wm2.weights.setValuesInPlace { i, j -> (i + 1) * (j + 1) * 0.2 }
        wm3.weights.setValuesInPlace { i, j -> (i + 1) * (j + 1) * 0.1 }
        wm4.weights.setValuesInPlace { i, j -> (i + 1) * (j + 1) * 0.1 }

        val layers: LinkedHashSet<Layer> = linkedSetOf(sourceLayer, intermediate1, intermediate2, targetLayer)
        val inputs = Matrix.of(arrayOf(doubleArrayOf(1.0, -0.5)))
        val targets = Matrix.of(arrayOf(doubleArrayOf(0.5, 0.3)))

        // Test accumulation manually with probe
        val probe = StructuredProbe.MapProbe()
        val weightAccumulator: HashMap<WeightMatrix, Matrix> = HashMap()
        val biasAccumulator: HashMap<Layer, Matrix> = HashMap()
        val sgAccumulator: HashMap<org.simbrain.network.core.SynapseGroup, Matrix> = HashMap()
        val rawMatrixAccumulator: HashMap<Matrix, Matrix> = HashMap()

        val error = runBlocking {
            // Perform forward pass
            with(net) {
                layers.forwardPass(listOf(inputs.row(0).toColumnVector()), listOf(sourceLayer))
            }

            with(net) {
                layers.accumulateBackprop(
                    inputLayers = listOf(sourceLayer),
                    targetValues = targets.row(0).toColumnVector(),
                    outputLayer = targetLayer,
                    weightAccumulator = weightAccumulator,
                    synapseGroupAccumulator = sgAccumulator,
                    biasesAccumulator = biasAccumulator,
                    rawMatrixAccumulator = rawMatrixAccumulator,
                    probe = probe
                )
            }
        }

        // Verify all weight matrices have accumulated gradients
        assertTrue(weightAccumulator.containsKey(wm1), "wm1 should have accumulated gradients")
        assertTrue(weightAccumulator.containsKey(wm2), "wm2 should have accumulated gradients") 
        assertTrue(weightAccumulator.containsKey(wm3), "wm3 should have accumulated gradients")
        assertTrue(weightAccumulator.containsKey(wm4), "wm4 should have accumulated gradients")

        // Verify gradients are finite
        weightAccumulator.values.forEach { gradients ->
            for (i in 0 until gradients.nrow()) {
                for (j in 0 until gradients.ncol()) {
                    assertTrue(gradients[i, j].isFinite(), "All gradients should be finite")
                }
            }
        }

        assertTrue(error.isFinite() && error >= 0, "Error should be finite and non-negative")
    }

    @Test
    fun `test computeOrderedUpdatePath with skip connections`() {
        val inputLayer = NeuronArray(2)
        val hiddenLayer = NeuronArray(3) 
        val outputLayer = NeuronArray(2)

        WeightMatrix(inputLayer, hiddenLayer)
        WeightMatrix(hiddenLayer, outputLayer)
        WeightMatrix(inputLayer, outputLayer) // Skip connection

        val orderedLayers = computeOrderedUpdatePath(setOf(inputLayer), outputLayer)

        assertEquals(3, orderedLayers.size, "Should include all layers")
        assertTrue(orderedLayers.contains(inputLayer), "Should contain input layer")
        assertTrue(orderedLayers.contains(hiddenLayer), "Should contain hidden layer") 
        assertTrue(orderedLayers.contains(outputLayer), "Should contain output layer")

        val layerList = orderedLayers.toList()
        val inputIndex = layerList.indexOf(inputLayer)
        val hiddenIndex = layerList.indexOf(hiddenLayer)
        val outputIndex = layerList.indexOf(outputLayer)

        assertTrue(inputIndex < hiddenIndex, "Input should come before hidden")
        assertTrue(inputIndex < outputIndex, "Input should come before output")
        assertTrue(hiddenIndex < outputIndex, "Hidden should come before output")
    }

    @Test
    fun `test splitDataSet for unsupervised learning with MutableList`() {
        val inputs = mutableListOf(
            mutableListOf(1.0, 2.0, 3.0),
            mutableListOf(4.0, 5.0, 6.0),
            mutableListOf(7.0, 8.0, 9.0),
            mutableListOf(10.0, 11.0, 12.0),
            mutableListOf(13.0, 14.0, 15.0)
        )
        
        val (training, testing) = splitDataSet(inputs, 0.6, Random(42))
        
        // Should split 5 rows into ~3 training and ~2 testing
        assertEquals(3, training.size)
        assertEquals(2, testing.size)
        
        // Each row should maintain 3 columns
        training.forEach { row ->
            assertEquals(3, row.size)
        }
        testing.forEach { row ->
            assertEquals(3, row.size)
        }
        
        // Total rows should equal original
        assertEquals(inputs.size, training.size + testing.size)
        
        // No row should appear in both sets
        val allTrainingData = training.flatten()
        val allTestingData = testing.flatten()
        val allOriginalData = inputs.flatten()
        
        assertEquals(allOriginalData.size, allTrainingData.size + allTestingData.size)
    }

    @Test
    fun `test splitDataSet unsupervised with edge case ratios`() {
        val inputs = mutableListOf(
            mutableListOf(1.0, 2.0),
            mutableListOf(3.0, 4.0),
            mutableListOf(5.0, 6.0)
        )
        
        // Test ratio 1.0 - all data goes to training
        val (training1, testing1) = splitDataSet(inputs, 1.0)
        assertEquals(3, training1.size)
        assertEquals(0, testing1.size)
        
        // Test ratio 0.0 - all data goes to testing
        val (training0, testing0) = splitDataSet(inputs, 0.0)
        assertEquals(0, training0.size)
        assertEquals(3, testing0.size)
        
        // Verify data integrity
        training1.forEach { row ->
            assertEquals(2, row.size)
        }
        testing0.forEach { row ->
            assertEquals(2, row.size)
        }
    }

    @Test
    fun `test splitDataSet unsupervised with single row`() {
        val inputs = mutableListOf(
            mutableListOf(42.0, 43.0, 44.0)
        )
        
        val (training, testing) = splitDataSet(inputs, 0.7)
        
        // With only one row, should go to training based on ratio calculation
        assertEquals(0, training.size) // floor(1 * 0.7) = 0
        assertEquals(1, testing.size)
        
        assertEquals(3, testing[0].size)
        assertEquals(42.0, testing[0][0])
        assertEquals(43.0, testing[0][1])
        assertEquals(44.0, testing[0][2])
    }

    @Test
    fun `test splitDataSet unsupervised with empty input`() {
        val inputs = mutableListOf<MutableList<Double>>()
        
        val (training, testing) = splitDataSet(inputs, 0.5)
        
        assertEquals(0, training.size)
        assertEquals(0, testing.size)
    }

    @Test
    fun `test splitDataSet unsupervised maintains data independence`() {
        val inputs = mutableListOf(
            mutableListOf(1.0, 2.0),
            mutableListOf(3.0, 4.0),
            mutableListOf(5.0, 6.0),
            mutableListOf(7.0, 8.0)
        )
        
        val (training, testing) = splitDataSet(inputs, 0.5, Random(123))
        
        // Modify original data
        inputs[0][0] = 999.0
        
        // Split data should be independent (deep copied)
        assertFalse(training.any { row -> row.contains(999.0) })
        assertFalse(testing.any { row -> row.contains(999.0) })
        
        // Modify split data
        if (training.isNotEmpty()) {
            training[0][0] = 888.0
        }
        
        // Original should be unaffected by split modifications
        assertFalse(inputs.flatten().contains(888.0))
    }

    @Test
    fun `test CrossEntropy accuracy with perfect predictions`() {
        // Test case where predictions exactly match targets
        val predictions = Matrix.of(arrayOf(
            doubleArrayOf(0.1, 0.9, 0.0),  // Predicted class 1
            doubleArrayOf(0.8, 0.1, 0.1),  // Predicted class 0
            doubleArrayOf(0.0, 0.0, 1.0)   // Predicted class 2
        ))
        val targets = Matrix.of(arrayOf(
            doubleArrayOf(0.0, 1.0, 0.0),  // Target class 1
            doubleArrayOf(1.0, 0.0, 0.0),  // Target class 0
            doubleArrayOf(0.0, 0.0, 1.0)   // Target class 2
        ))
        
        val accuracy = BackpropLossFunction.CrossEntropy.accuracy(predictions, targets)
        assertEquals(1.0, accuracy, 1e-6, "Perfect predictions should have 100% accuracy")
    }

    @Test
    fun `test CrossEntropy accuracy with no correct predictions`() {
        // Test case where no predictions match targets
        val predictions = Matrix.of(arrayOf(
            doubleArrayOf(0.9, 0.1, 0.0),  // Predicted class 0
            doubleArrayOf(0.1, 0.8, 0.1),  // Predicted class 1
            doubleArrayOf(1.0, 0.0, 0.0)   // Predicted class 0
        ))
        val targets = Matrix.of(arrayOf(
            doubleArrayOf(0.0, 1.0, 0.0),  // Target class 1
            doubleArrayOf(0.0, 0.0, 1.0),  // Target class 2
            doubleArrayOf(0.0, 0.0, 1.0)   // Target class 2
        ))
        
        val accuracy = BackpropLossFunction.CrossEntropy.accuracy(predictions, targets)
        assertEquals(0.0, accuracy, 1e-6, "No correct predictions should have 0% accuracy")
    }

    @Test
    fun `test CrossEntropy accuracy with partial correct predictions`() {
        // Test case with 2 out of 3 correct predictions
        val predictions = Matrix.of(arrayOf(
            doubleArrayOf(0.1, 0.9, 0.0),  // Predicted class 1 ✓
            doubleArrayOf(0.8, 0.1, 0.1),  // Predicted class 0 ✗ (target is class 2)
            doubleArrayOf(0.0, 0.0, 1.0)   // Predicted class 2 ✓
        ))
        val targets = Matrix.of(arrayOf(
            doubleArrayOf(0.0, 1.0, 0.0),  // Target class 1
            doubleArrayOf(0.0, 0.0, 1.0),  // Target class 2
            doubleArrayOf(0.0, 0.0, 1.0)   // Target class 2
        ))
        
        val accuracy = BackpropLossFunction.CrossEntropy.accuracy(predictions, targets)
        assertEquals(2.0/3.0, accuracy, 1e-6, "2 out of 3 correct should be 66.67% accuracy")
    }

    @Test
    fun `test CrossEntropy accuracy with single prediction column vector`() {
        // Test single prediction as column vector
        val predictions = doubleArrayOf(0.1, 0.9, 0.0).toColumnVector()
        val targets = doubleArrayOf(0.0, 1.0, 0.0).toColumnVector()
        
        val accuracy = BackpropLossFunction.CrossEntropy.accuracy(predictions, targets)
        assertEquals(1.0, accuracy, 1e-6, "Single correct prediction should have 100% accuracy")
    }

    @Test
    fun `test CrossEntropy accuracy with single incorrect prediction column vector`() {
        // Test single incorrect prediction as column vector
        val predictions = doubleArrayOf(0.9, 0.1, 0.0).toColumnVector()  // Predicted class 0
        val targets = doubleArrayOf(0.0, 1.0, 0.0).toColumnVector()      // Target class 1
        
        val accuracy = BackpropLossFunction.CrossEntropy.accuracy(predictions, targets)
        assertEquals(0.0, accuracy, 1e-6, "Single incorrect prediction should have 0% accuracy")
    }

    @Test
    fun `test CrossEntropy accuracy with tie in predictions`() {
        // Test case where highest probability is tied (should pick first occurrence)
        val predictions = Matrix.of(arrayOf(
            doubleArrayOf(0.5, 0.5, 0.0),  // Tie between class 0 and 1, maxByOrNull picks 0
            doubleArrayOf(0.3, 0.3, 0.4)   // Class 2 wins
        ))
        val targets = Matrix.of(arrayOf(
            doubleArrayOf(1.0, 0.0, 0.0),  // Target class 0 ✓
            doubleArrayOf(0.0, 0.0, 1.0)   // Target class 2 ✓
        ))
        
        val accuracy = BackpropLossFunction.CrossEntropy.accuracy(predictions, targets)
        assertEquals(1.0, accuracy, 1e-6, "Both predictions should be correct")
    }

    @Test
    fun `test CrossEntropy accuracy with sequence data`() {
        // Test sequence-to-sequence accuracy (multiple rows)
        val predictions = Matrix.of(arrayOf(
            doubleArrayOf(0.1, 0.9),  // Position 0: predicted class 1
            doubleArrayOf(0.8, 0.2),  // Position 1: predicted class 0
            doubleArrayOf(0.3, 0.7)   // Position 2: predicted class 1
        ))
        val targets = Matrix.of(arrayOf(
            doubleArrayOf(0.0, 1.0),  // Position 0: target class 1 ✓
            doubleArrayOf(1.0, 0.0),  // Position 1: target class 0 ✓
            doubleArrayOf(1.0, 0.0)   // Position 2: target class 0 ✗
        ))
        
        val accuracy = BackpropLossFunction.CrossEntropy.accuracy(predictions, targets)
        assertEquals(2.0/3.0, accuracy, 1e-6, "2 out of 3 sequence positions correct")
    }

    @Test
    fun `test CrossEntropy accuracy with binary classification`() {
        // Test binary classification (2 classes)
        val predictions = Matrix.of(arrayOf(
            doubleArrayOf(0.8, 0.2),  // Predicted class 0
            doubleArrayOf(0.3, 0.7),  // Predicted class 1
            doubleArrayOf(0.6, 0.4),  // Predicted class 0
            doubleArrayOf(0.1, 0.9)   // Predicted class 1
        ))
        val targets = Matrix.of(arrayOf(
            doubleArrayOf(1.0, 0.0),  // Target class 0 ✓
            doubleArrayOf(0.0, 1.0),  // Target class 1 ✓
            doubleArrayOf(0.0, 1.0),  // Target class 1 ✗
            doubleArrayOf(0.0, 1.0)   // Target class 1 ✓
        ))
        
        val accuracy = BackpropLossFunction.CrossEntropy.accuracy(predictions, targets)
        assertEquals(0.75, accuracy, 1e-6, "3 out of 4 correct should be 75% accuracy")
    }

    @Test
    fun `test CrossEntropy accuracy validates matrix shapes`() {
        val predictions = Matrix.of(arrayOf(doubleArrayOf(0.5, 0.5)))
        val wrongTargets = Matrix.of(arrayOf(doubleArrayOf(1.0, 0.0, 0.0)))  // Different shape
        
        org.junit.jupiter.api.assertThrows<IllegalArgumentException> {
            BackpropLossFunction.CrossEntropy.accuracy(predictions, wrongTargets)
        }
    }

    @Test
    fun `test accuracy computation can be disabled in trainer config`() {
        val net = Network()
        val inputLayer = NeuronArray(3).apply { 
            isClamped = true 
            updateRule = LinearRule()
        }
        val outputLayer = NeuronArray(3).apply {
            updateRule = SoftmaxRule()
        }
        val wm = WeightMatrix(inputLayer, outputLayer)
        
        runBlocking {
            net.addNetworkModelsAsync(inputLayer, outputLayer, wm)
        }
        
        val supervisedModel = SupervisedModel(inputLayer, outputLayer)
        supervisedModel.trainerConfig.lossFunction = BackpropLossFunction.CrossEntropy
        
        // Test with accuracy computation enabled (default)
        supervisedModel.trainerConfig.computeAccuracy = true
        val trainerEnabled = SupervisedTrainer(net, supervisedModel)
        
        runBlocking {
            trainerEnabled.trainOnce()
        }
        
        assertNotNull(trainerEnabled.lastTrainingAccuracy, "Accuracy should be computed when enabled")
        
        // Test with accuracy computation disabled
        supervisedModel.trainerConfig.computeAccuracy = false
        val trainerDisabled = SupervisedTrainer(net, supervisedModel)
        
        runBlocking {
            trainerDisabled.trainOnce()
        }
        
        assertNull(trainerDisabled.lastTrainingAccuracy, "Accuracy should not be computed when disabled")
    }

    @Test
    fun `test optimized accuracy computation performance`() {
        val net = Network()
        val inputLayer = NeuronArray(10).apply { 
            isClamped = true 
            updateRule = LinearRule()
        }
        val outputLayer = NeuronArray(5).apply {
            updateRule = SoftmaxRule()
        }
        val wm = WeightMatrix(inputLayer, outputLayer)
        
        runBlocking {
            net.addNetworkModelsAsync(inputLayer, outputLayer, wm)
        }
        
        val supervisedModel = SupervisedModel(inputLayer, outputLayer)
        supervisedModel.trainerConfig.lossFunction = BackpropLossFunction.CrossEntropy
        supervisedModel.trainerConfig.computeAccuracy = true
        
        // Create a larger training set to test performance
        val largeInputs = (0 until 100).map { 
            (0 until 10).map { Random.nextDouble() }.toMutableList() 
        }.toMutableList()
        val largeTargets = (0 until 100).map { 
            val oneHot = DoubleArray(5) { 0.0 }
            oneHot[Random.nextInt(5)] = 1.0
            oneHot.toMutableList()
        }.toMutableList()
        
        supervisedModel.trainingSet = TrainingDataset(largeInputs, largeTargets)
        
        val trainer = SupervisedTrainer(net, supervisedModel)
        
        // Test that accuracy is computed efficiently (should not take long)
        val startTime = System.currentTimeMillis()
        runBlocking {
            repeat(5) {
                trainer.trainOnce()
            }
        }
        val endTime = System.currentTimeMillis()
        
        // Verify accuracy was computed
        assertNotNull(trainer.lastTrainingAccuracy, "Accuracy should be computed")
        assertTrue(trainer.lastTrainingAccuracy!! >= 0.0 && trainer.lastTrainingAccuracy!! <= 1.0, 
                  "Accuracy should be between 0 and 1")
        
        // Performance should be reasonable (less than 5 seconds for 5 iterations on 100 samples)
        val duration = endTime - startTime
        assertTrue(duration < 5000, "Training with accuracy should complete in reasonable time: ${duration}ms")
    }

    @Test
    fun `test training and testing accuracy computation`() {
        val net = Network()
        val inputLayer = NeuronArray(3).apply { 
            isClamped = true 
            updateRule = LinearRule()
        }
        val outputLayer = NeuronArray(3).apply {
            updateRule = SoftmaxRule()
        }
        val wm = WeightMatrix(inputLayer, outputLayer)
        
        runBlocking {
            net.addNetworkModelsAsync(inputLayer, outputLayer, wm)
        }
        
        val supervisedModel = SupervisedModel(inputLayer, outputLayer)
        supervisedModel.trainerConfig.lossFunction = BackpropLossFunction.CrossEntropy
        supervisedModel.trainerConfig.computeAccuracy = true
        supervisedModel.trainerConfig.testConfiguration.enabled = true
        supervisedModel.trainerConfig.testConfiguration.testFrequency = 1 // Test every iteration
        
        val trainer = SupervisedTrainer(net, supervisedModel)
        
        runBlocking {
            trainer.trainOnce()
        }
        
        // Both training and testing accuracy should be computed
        assertNotNull(trainer.lastTrainingAccuracy, "Training accuracy should be computed")
        assertNotNull(trainer.lastTestingAccuracy, "Testing accuracy should be computed when test is enabled")
        
        assertTrue(trainer.lastTrainingAccuracy!! >= 0.0 && trainer.lastTrainingAccuracy!! <= 1.0, 
                  "Training accuracy should be between 0 and 1")
        assertTrue(trainer.lastTestingAccuracy!! >= 0.0 && trainer.lastTestingAccuracy!! <= 1.0, 
                  "Testing accuracy should be between 0 and 1")
        
        // Test with testing disabled
        supervisedModel.trainerConfig.testConfiguration.enabled = false
        val trainerNoTest = SupervisedTrainer(net, supervisedModel)
        
        runBlocking {
            trainerNoTest.trainOnce()
        }
        
        // Only training accuracy should be computed
        assertNotNull(trainerNoTest.lastTrainingAccuracy, "Training accuracy should be computed")
        assertNull(trainerNoTest.lastTestingAccuracy, "Testing accuracy should not be computed when test is disabled")
    }

    @Test
    fun `test accuracy computation can be disabled and enabled dynamically`() {
        val net = Network()
        val inputLayer = NeuronArray(3).apply { 
            isClamped = true 
            updateRule = LinearRule()
        }
        val outputLayer = NeuronArray(3).apply {
            updateRule = SoftmaxRule()
        }
        val wm = WeightMatrix(inputLayer, outputLayer)
        
        runBlocking {
            net.addNetworkModelsAsync(inputLayer, outputLayer, wm)
        }
        
        val supervisedModel = SupervisedModel(inputLayer, outputLayer)
        supervisedModel.trainerConfig.lossFunction = BackpropLossFunction.CrossEntropy
        
        // Start with accuracy disabled
        supervisedModel.trainerConfig.computeAccuracy = false
        val trainer = SupervisedTrainer(net, supervisedModel)
        
        runBlocking {
            trainer.trainOnce()
        }
        
        // No accuracy should be computed
        assertNull(trainer.lastTrainingAccuracy, "Training accuracy should not be computed when disabled")
        assertNull(trainer.lastTestingAccuracy, "Testing accuracy should not be computed when disabled")
        
        // Enable accuracy computation
        supervisedModel.trainerConfig.computeAccuracy = true
        
        runBlocking {
            trainer.trainOnce()
        }
        
        // Now accuracy should be computed
        assertNotNull(trainer.lastTrainingAccuracy, "Training accuracy should be computed when enabled")
        assertTrue(trainer.lastTrainingAccuracy!! >= 0.0 && trainer.lastTrainingAccuracy!! <= 1.0, 
                  "Training accuracy should be between 0 and 1")
    }

    @Test
    fun `test testing accuracy persists between test runs`() {
        val net = Network()
        val inputLayer = NeuronArray(3).apply { 
            isClamped = true 
            updateRule = LinearRule()
        }
        val outputLayer = NeuronArray(3).apply {
            updateRule = SoftmaxRule()
        }
        val wm = WeightMatrix(inputLayer, outputLayer)
        
        runBlocking {
            net.addNetworkModelsAsync(inputLayer, outputLayer, wm)
        }
        
        val supervisedModel = SupervisedModel(inputLayer, outputLayer)
        supervisedModel.trainerConfig.lossFunction = BackpropLossFunction.CrossEntropy
        supervisedModel.trainerConfig.computeAccuracy = true
        supervisedModel.trainerConfig.testConfiguration.enabled = true
        supervisedModel.trainerConfig.testConfiguration.testFrequency = 2 // Test every 2 iterations
        
        val trainer = SupervisedTrainer(net, supervisedModel)
        
        // First iteration - no test accuracy yet
        runBlocking {
            trainer.trainOnce() // iteration 1
        }
        
        assertNull(trainer.lastTestingAccuracy, "Testing accuracy should be null on iteration 1")
        
        // Second iteration - test accuracy should be computed
        runBlocking {
            trainer.trainOnce() // iteration 2
        }
        
        assertNotNull(trainer.lastTestingAccuracy, "Testing accuracy should be computed on iteration 2")
        val firstTestAccuracy = trainer.lastTestingAccuracy!!
        
        // Third iteration - test accuracy should persist (not reset to null)
        runBlocking {
            trainer.trainOnce() // iteration 3
        }
        
        assertEquals(firstTestAccuracy, trainer.lastTestingAccuracy, 
                    "Testing accuracy should persist from previous test run")
        
        // Fourth iteration - test accuracy should be updated again
        runBlocking {
            trainer.trainOnce() // iteration 4
        }
        
        assertNotNull(trainer.lastTestingAccuracy, "Testing accuracy should be computed again on iteration 4")
        // Note: We don't assert it's different because with random weights it might be the same value
    }

}