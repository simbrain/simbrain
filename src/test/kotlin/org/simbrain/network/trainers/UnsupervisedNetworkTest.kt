package org.simbrain.network.trainers

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.simbrain.network.subnetworks.CompetitiveNetwork
import org.simbrain.network.subnetworks.Hopfield
import org.simbrain.network.subnetworks.RestrictedBoltzmannMachine
import org.simbrain.network.subnetworks.SOMNetwork
import org.simbrain.util.randomMutableList
import kotlin.random.Random

class UnsupervisedNetworkTest {

    @Test
    fun `test CompetitiveNetwork data structure`() {
        val competitiveNet = CompetitiveNetwork(3, 5)
        
        // Verify data structure types
        assertNotNull(competitiveNet.trainingData)
        assertNotNull(competitiveNet.testingData)
        assertTrue(competitiveNet.trainingData is MutableList<*>)
        assertTrue(competitiveNet.testingData is MutableList<*>)
        
        // Verify initial data has correct dimensions
        if (competitiveNet.trainingData.isNotEmpty()) {
            assertEquals(3, competitiveNet.trainingData[0].size)
        }
        if (competitiveNet.testingData.isNotEmpty()) {
            assertEquals(3, competitiveNet.testingData[0].size)
        }
        
        // Test setting custom data
        val customData = mutableListOf(
            mutableListOf(1.0, 0.0, 1.0),
            mutableListOf(0.0, 1.0, 0.0),
            mutableListOf(1.0, 1.0, 0.0)
        )
        competitiveNet.trainingData = customData
        assertEquals(3, competitiveNet.trainingData.size)
        assertEquals(1.0, competitiveNet.trainingData[0][0])
    }

    @Test
    fun `test SOMNetwork data structure`() {
        val somNet = SOMNetwork(4, 9)
        
        // Verify data structure types
        assertNotNull(somNet.trainingData)
        assertNotNull(somNet.testingData)
        assertTrue(somNet.trainingData is MutableList<*>)
        assertTrue(somNet.testingData is MutableList<*>)
        
        // Verify initial data has correct dimensions
        if (somNet.trainingData.isNotEmpty()) {
            assertEquals(4, somNet.trainingData[0].size)
        }
        if (somNet.testingData.isNotEmpty()) {
            assertEquals(4, somNet.testingData[0].size)
        }
        
        // Test with custom binary data
        val binaryData = mutableListOf(
            mutableListOf(1.0, 0.0, 1.0, 0.0),
            mutableListOf(0.0, 1.0, 0.0, 1.0),
            mutableListOf(1.0, 1.0, 0.0, 0.0),
            mutableListOf(0.0, 0.0, 1.0, 1.0)
        )
        somNet.trainingData = binaryData
        assertEquals(4, somNet.trainingData.size)
        assertEquals(1.0, somNet.trainingData[0][0])
    }

    @Test
    fun `test Hopfield data structure`() {
        val hopfield = Hopfield(5)
        
        // Verify data structure types
        assertNotNull(hopfield.trainingData)
        assertNotNull(hopfield.testingData)
        assertTrue(hopfield.trainingData is MutableList<*>)
        assertTrue(hopfield.testingData is MutableList<*>)
        
        // Verify initial data has correct dimensions
        if (hopfield.trainingData.isNotEmpty()) {
            assertEquals(5, hopfield.trainingData[0].size)
        }
        if (hopfield.testingData.isNotEmpty()) {
            assertEquals(5, hopfield.testingData[0].size)
        }
        
        // Test with custom patterns
        val patterns = mutableListOf(
            mutableListOf(1.0, -1.0, 1.0, -1.0, 1.0),
            mutableListOf(-1.0, 1.0, -1.0, 1.0, -1.0),
            mutableListOf(1.0, 1.0, -1.0, -1.0, 1.0)
        )
        hopfield.trainingData = patterns
        assertEquals(3, hopfield.trainingData.size)
        assertEquals(1.0, hopfield.trainingData[0][0])
    }

    @Test
    fun `test RestrictedBoltzmannMachine data structure`() {
        val rbm = RestrictedBoltzmannMachine(6, 3)
        
        // Verify data structure types
        assertNotNull(rbm.trainingData)
        assertNotNull(rbm.testingData)
        assertTrue(rbm.trainingData is MutableList<*>)
        assertTrue(rbm.testingData is MutableList<*>)
        
        // Verify initial data has correct dimensions
        if (rbm.trainingData.isNotEmpty()) {
            assertEquals(6, rbm.trainingData[0].size)
        }
        if (rbm.testingData.isNotEmpty()) {
            assertEquals(6, rbm.testingData[0].size)
        }
        
        // Test with custom data
        val customData = mutableListOf(
            mutableListOf(1.0, 0.0, 1.0, 0.0, 1.0, 0.0),
            mutableListOf(0.0, 1.0, 0.0, 1.0, 0.0, 1.0),
            mutableListOf(1.0, 1.0, 0.0, 0.0, 1.0, 1.0)
        )
        rbm.trainingData = customData
        assertEquals(3, rbm.trainingData.size)
        assertEquals(1.0, rbm.trainingData[0][0])
    }

    @Test
    fun `test data copying in UnsupervisedNetwork implementations`() {
        val originalData = mutableListOf(
            mutableListOf(1.0, 2.0, 3.0),
            mutableListOf(4.0, 5.0, 6.0)
        )
        
        // Test CompetitiveNetwork
        val competitiveNet = CompetitiveNetwork(3, 5)
        competitiveNet.trainingData = originalData.map { it.toMutableList() }.toMutableList()
        val competitiveCopy = competitiveNet.copy()
        
        // Modify original data directly in the network
        competitiveNet.trainingData[0][0] = 999.0
        
        // Copy should be independent (data should be 1.0, not 999.0)
        assertEquals(1.0, competitiveCopy.trainingData[0][0])
        
        // Test SOMNetwork
        val somNet = SOMNetwork(3, 9)
        val freshData = mutableListOf(
            mutableListOf(10.0, 20.0, 30.0),
            mutableListOf(40.0, 50.0, 60.0)
        )
        somNet.trainingData = freshData
        val somCopy = somNet.copy()
        
        // Modify original data in the network
        somNet.trainingData[0][0] = 888.0
        
        // Copy should be independent (data should be 10.0, not 888.0)
        assertEquals(10.0, somCopy.trainingData[0][0])
    }

    @Test
    fun `test UnsupervisedTrainer properties`() {
        val competitiveNet = CompetitiveNetwork(2, 3)
        val trainer = competitiveNet.trainer
        
        // Test trainer properties
        assertEquals(1000, trainer.maxIterations)
        assertEquals(0.01, trainer.learningRate, 0.001)
        assertFalse(trainer.isRunning)
        assertEquals(0, trainer.iteration)
        
        // Set custom training data
        val trainingData = mutableListOf(
            mutableListOf(1.0, 0.0),
            mutableListOf(0.0, 1.0),
            mutableListOf(0.5, 0.5)
        )
        competitiveNet.trainingData = trainingData
        assertEquals(3, competitiveNet.trainingData.size)
    }

    @Test
    fun `test randomMutableList utility function`() {
        val data = randomMutableList(5, 3, Random(42))
        
        // Should have correct dimensions
        assertEquals(5, data.size)
        data.forEach { row ->
            assertEquals(3, row.size)
        }
        
        // Should contain reasonable random values (between 0 and 1 for rand)
        data.flatten().forEach { value ->
            assertTrue(value >= 0.0 && value <= 1.0, "Value $value should be between 0 and 1")
        }
        
        // Should be different on different random seeds
        val data2 = randomMutableList(5, 3, Random(123))
        assertNotEquals(data, data2)
    }

    @Test
    fun `test UnsupervisedNetwork toString methods`() {
        val competitiveNet = CompetitiveNetwork(4, 6)
        val somNet = SOMNetwork(3, 9)
        
        val competitiveString = competitiveNet.toString()
        val somString = somNet.toString()
        
        // Should contain relevant information
        assertTrue(competitiveString.contains("Competitive Network"))
        assertTrue(competitiveString.contains("4 neurons"))
        assertTrue(competitiveString.contains("6 neurons"))
        
        assertTrue(somString.contains("SOM Network"))
        assertTrue(somString.contains("3 neurons"))
        assertTrue(somString.contains("9 neurons"))
    }

    @Test
    fun `test training data modification and persistence`() {
        val competitiveNet = CompetitiveNetwork(2, 3)
        
        // Original data should not be empty (from constructor)
        assertTrue(competitiveNet.trainingData.isNotEmpty() || competitiveNet.testingData.isNotEmpty())
        
        // Set new training data
        val newData = mutableListOf(
            mutableListOf(0.1, 0.9),
            mutableListOf(0.8, 0.2),
            mutableListOf(0.5, 0.5)
        )
        competitiveNet.trainingData = newData
        
        // Data should be preserved
        assertEquals(3, competitiveNet.trainingData.size)
        assertEquals(0.1, competitiveNet.trainingData[0][0])
        assertEquals(0.9, competitiveNet.trainingData[0][1])
        
        // Modify the data
        competitiveNet.trainingData[0][0] = 0.99
        assertEquals(0.99, competitiveNet.trainingData[0][0])
    }

    @Test
    fun `test empty data handling`() {
        val competitiveNet = CompetitiveNetwork(3, 5)
        
        // Set empty training data
        competitiveNet.trainingData = mutableListOf()
        assertEquals(0, competitiveNet.trainingData.size)
        
        // Should be able to set and retrieve empty data
        assertTrue(competitiveNet.trainingData.isEmpty())
        
        // Should be able to add data later
        competitiveNet.trainingData.add(mutableListOf(1.0, 2.0, 3.0))
        assertEquals(1, competitiveNet.trainingData.size)
    }
}
