package org.simbrain.network

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.simbrain.network.core.NeuronArray
import org.simbrain.network.core.WeightMatrix
import org.simbrain.network.trainers.*
import org.simbrain.util.*
import org.simbrain.util.stats.distributions.NormalDistribution
import org.simbrain.util.stats.distributions.UniformRealDistribution
import smile.math.matrix.Matrix
import kotlin.math.abs
import kotlin.math.sqrt

class WeightInitializationStrategiesTest {

    @Test
    fun `test Xavier uniform distribution`() {
        val numInputs = 30
        val numOutputs = 10
        val na1 = NeuronArray(numInputs)
        val na2 = NeuronArray(numOutputs)
        val wm = WeightMatrix(na1, na2)
        val xavier = Xavier(42L).apply { distribution = Xavier.Distribution.UNIFORM }
        xavier.initializeWeights(wm)

        val amplitude = sqrt(6.0 / (numInputs + numOutputs))

        assertTrue(wm.weightArray.all { it in -amplitude..amplitude }, 
                  "Xavier uniform weights should be in range [-${amplitude}, ${amplitude}]")
        
        // Test statistical properties
        val mean = wm.weightArray.average()
        assertTrue(abs(mean) < 0.1, "Xavier uniform should have approximately zero mean")
    }

    @Test
    fun `test Xavier normal distribution`() {
        val numInputs = 20
        val numOutputs = 15
        val na1 = NeuronArray(numInputs)
        val na2 = NeuronArray(numOutputs)
        val wm = WeightMatrix(na1, na2)
        val xavier = Xavier(42L).apply { distribution = Xavier.Distribution.NORMAL }
        xavier.initializeWeights(wm)

        val expectedStd = sqrt(2.0 / (numInputs + numOutputs))
        
        // Test statistical properties
        val mean = wm.weightArray.average()
        val variance = wm.weightArray.map { (it - mean) * (it - mean) }.average()
        val std = sqrt(variance)
        
        assertTrue(abs(mean) < 0.1, "Xavier normal should have approximately zero mean")
        assertTrue(abs(std - expectedStd) < 0.1, "Xavier normal standard deviation should be approximately $expectedStd, got $std")
    }

    @Test
    fun `test Xavier with Matrix initialization`() {
        val matrix = Matrix(5, 3)
        val xavier = Xavier(123L).apply { distribution = Xavier.Distribution.UNIFORM }
        xavier.initializeWeights(matrix)
        
        val amplitude = sqrt(6.0 / (5 + 3))  // ncol + nrow
        
        for (i in 0 until matrix.nrow()) {
            for (j in 0 until matrix.ncol()) {
                val value = matrix[i, j]
                assertTrue(value in -amplitude..amplitude, 
                          "Matrix element [$i,$j] = $value should be in range [-$amplitude, $amplitude]")
            }
        }
    }

    @Test
    fun `test He uniform distribution`() {
        val numInputs = 25
        val numOutputs = 12
        val na1 = NeuronArray(numInputs)
        val na2 = NeuronArray(numOutputs)
        val wm = WeightMatrix(na1, na2)
        val he = He(42L).apply { distribution = He.Distribution.UNIFORM }
        he.initializeWeights(wm)

        val amplitude = sqrt(6.0 / numInputs)

        assertTrue(wm.weightArray.all { it in -amplitude..amplitude }, 
                  "He uniform weights should be in range [-${amplitude}, ${amplitude}]")
        
        val mean = wm.weightArray.average()
        assertTrue(abs(mean) < 0.1, "He uniform should have approximately zero mean")
    }

    @Test
    fun `test He normal distribution`() {
        val numInputs = 20
        val numOutputs = 8
        val na1 = NeuronArray(numInputs)
        val na2 = NeuronArray(numOutputs)
        val wm = WeightMatrix(na1, na2)
        val he = He(99L).apply { distribution = He.Distribution.NORMAL }
        he.initializeWeights(wm)

        val expectedStd = sqrt(2.0 / numInputs)
        
        val mean = wm.weightArray.average()
        val variance = wm.weightArray.map { (it - mean) * (it - mean) }.average()
        val std = sqrt(variance)
        
        assertTrue(abs(mean) < 0.1, "He normal should have approximately zero mean")
        assertTrue(abs(std - expectedStd) < 0.15, "He normal standard deviation should be approximately $expectedStd, got $std")
    }

    @Test
    fun `test LeCun uniform distribution`() {
        val numInputs = 18
        val numOutputs = 6
        val na1 = NeuronArray(numInputs)
        val na2 = NeuronArray(numOutputs)
        val wm = WeightMatrix(na1, na2)
        val lecun = LeCun(77L).apply { distribution = LeCun.Distribution.UNIFORM }
        lecun.initializeWeights(wm)

        val amplitude = sqrt(3.0 / numInputs)

        assertTrue(wm.weightArray.all { it in -amplitude..amplitude }, 
                  "LeCun uniform weights should be in range [-${amplitude}, ${amplitude}]")
        
        val mean = wm.weightArray.average()
        assertTrue(abs(mean) < 0.1, "LeCun uniform should have approximately zero mean")
    }

    @Test
    fun `test LeCun normal distribution`() {
        val numInputs = 15
        val numOutputs = 10
        val na1 = NeuronArray(numInputs)
        val na2 = NeuronArray(numOutputs)
        val wm = WeightMatrix(na1, na2)
        val lecun = LeCun(33L).apply { distribution = LeCun.Distribution.NORMAL }
        lecun.initializeWeights(wm)

        val expectedStd = sqrt(1.0 / numInputs)
        
        val mean = wm.weightArray.average()
        val variance = wm.weightArray.map { (it - mean) * (it - mean) }.average()
        val std = sqrt(variance)
        
        assertTrue(abs(mean) < 0.1, "LeCun normal should have approximately zero mean")
        assertTrue(abs(std - expectedStd) < 0.1, "LeCun normal standard deviation should be approximately $expectedStd, got $std")
    }

    @Test
    fun `test Randomize with different distributions`() {
        val na1 = NeuronArray(10)
        val na2 = NeuronArray(5)
        val wm = WeightMatrix(na1, na2)
        
        // Test with normal distribution
        val randomizeNormal = Randomize(88L).apply { 
            distribution = NormalDistribution(0.0, 0.5)
        }
        randomizeNormal.initializeWeights(wm)
        
        val mean = wm.weightArray.average()
        assertTrue(abs(mean) < 0.2, "Randomize with normal distribution should have approximately zero mean")
        
        // Test with uniform distribution
        val randomizeUniform = Randomize(88L).apply {
            distribution = UniformRealDistribution(-1.0, 1.0)
        }
        randomizeUniform.initializeWeights(wm)
        
        assertTrue(wm.weightArray.all { it in -1.0..1.0 }, 
                  "Randomize with uniform distribution should respect bounds")
    }

    @Test
    fun `test seed reproducibility`() {
        val seed = 12345L
        
        // Test Xavier reproducibility
        val na1 = NeuronArray(5)
        val na2 = NeuronArray(3)
        val wm1 = WeightMatrix(na1, na2)
        val wm2 = WeightMatrix(na1, na2)
        
        val xavier1 = Xavier(seed)
        val xavier2 = Xavier(seed)
        
        xavier1.initializeWeights(wm1)
        xavier2.initializeWeights(wm2)
        
        assertArrayEquals(wm1.weightArray, wm2.weightArray, 1e-10, 
                         "Same seed should produce identical Xavier initialization")
        
        // Test He reproducibility
        val he1 = He(seed)
        val he2 = He(seed)
        
        he1.initializeWeights(wm1)
        he2.initializeWeights(wm2)
        
        assertArrayEquals(wm1.weightArray, wm2.weightArray, 1e-10, 
                         "Same seed should produce identical He initialization")
        
        // Test LeCun reproducibility
        val lecun1 = LeCun(seed)
        val lecun2 = LeCun(seed)
        
        lecun1.initializeWeights(wm1)
        lecun2.initializeWeights(wm2)
        
        assertArrayEquals(wm1.weightArray, wm2.weightArray, 1e-10, 
                         "Same seed should produce identical LeCun initialization")
    }

    @Test
    fun `test different seeds produce different results`() {
        val na1 = NeuronArray(4)
        val na2 = NeuronArray(3)
        val wm1 = WeightMatrix(na1, na2)
        val wm2 = WeightMatrix(na1, na2)
        
        val xavier1 = Xavier(111L)
        val xavier2 = Xavier(222L)
        
        xavier1.initializeWeights(wm1)
        xavier2.initializeWeights(wm2)
        
        assertFalse(wm1.weightArray.contentEquals(wm2.weightArray), 
                   "Different seeds should produce different initializations")
    }

    @Test
    fun `test copy functionality preserves parameters`() {
        // Test Xavier copy
        val xavier = Xavier(555L).apply { distribution = Xavier.Distribution.NORMAL }
        val xavierCopy = xavier.copy()
        assertEquals(xavier.distribution, xavierCopy.distribution)
        assertNotSame(xavier, xavierCopy)
        
        // Test He copy
        val he = He(666L).apply { distribution = He.Distribution.NORMAL }
        val heCopy = he.copy()
        assertEquals(he.distribution, heCopy.distribution)
        assertNotSame(he, heCopy)
        
        // Test LeCun copy
        val lecun = LeCun(777L).apply { distribution = LeCun.Distribution.NORMAL }
        val lecunCopy = lecun.copy()
        assertEquals(lecun.distribution, lecunCopy.distribution)
        assertNotSame(lecun, lecunCopy)
        
        // Test Randomize copy
        val randomize = Randomize(888L).apply { 
            distribution = UniformRealDistribution(-2.0, 2.0)
        }
        val randomizeCopy = randomize.copy()
        assertEquals(randomize.distribution.javaClass, randomizeCopy.distribution.javaClass)
        assertNotSame(randomize, randomizeCopy)
        assertNotSame(randomize.distribution, randomizeCopy.distribution)
    }

    @Test
    fun `test initialization strategy type list completeness`() {
        val strategy = Xavier()
        val typeList = strategy.getTypeList()
        
        assertEquals(4, typeList.size, "Should include all four initialization strategies")
        assertTrue(typeList.contains(Randomize::class.java))
        assertTrue(typeList.contains(Xavier::class.java))
        assertTrue(typeList.contains(He::class.java))
        assertTrue(typeList.contains(LeCun::class.java))
    }

    @Test
    fun `test initialization with small and large networks`() {
        // Test with very small network
        val smallInput = NeuronArray(1)
        val smallOutput = NeuronArray(1)
        val smallWm = WeightMatrix(smallInput, smallOutput)
        
        val xavier = Xavier(111L)
        xavier.initializeWeights(smallWm)
        
        assertTrue(smallWm.weightArray.all { it.isFinite() }, 
                  "Small network initialization should produce finite values")
        
        // Test with larger network
        val largeInput = NeuronArray(1000)
        val largeOutput = NeuronArray(500)
        val largeWm = WeightMatrix(largeInput, largeOutput)
        
        xavier.initializeWeights(largeWm)
        
        assertTrue(largeWm.weightArray.all { it.isFinite() }, 
                  "Large network initialization should produce finite values")
        
        // Large network should have smaller weight magnitudes due to normalization
        val smallMagnitude = smallWm.weightArray.map { abs(it) }.average()
        val largeMagnitude = largeWm.weightArray.map { abs(it) }.average()
        
        assertTrue(largeMagnitude < smallMagnitude, 
                  "Larger networks should have smaller weight magnitudes due to fan-in/fan-out normalization")
    }

    @Test
    fun `test He initialization is optimal for ReLU networks`() {
        // He initialization is specifically designed for ReLU activations
        val numInputs = 100
        val numOutputs = 50
        val na1 = NeuronArray(numInputs)
        val na2 = NeuronArray(numOutputs)
        val wm = WeightMatrix(na1, na2)
        
        val he = He(42L).apply { distribution = He.Distribution.NORMAL }
        he.initializeWeights(wm)
        
        val expectedStd = sqrt(2.0 / numInputs)
        val mean = wm.weightArray.average()
        val variance = wm.weightArray.map { (it - mean) * (it - mean) }.average()
        val actualStd = sqrt(variance)
        
        assertTrue(abs(mean) < 0.05, "He initialization should have zero mean")
        assertTrue(abs(actualStd - expectedStd) < 0.05, 
                  "He initialization should have std=$expectedStd for ReLU networks, got $actualStd")
    }

    @Test
    fun `test LeCun initialization is optimal for SELU networks`() {
        // LeCun initialization is specifically designed for SELU activations
        val numInputs = 80
        val numOutputs = 40
        val na1 = NeuronArray(numInputs)
        val na2 = NeuronArray(numOutputs)
        val wm = WeightMatrix(na1, na2)
        
        val lecun = LeCun(42L).apply { distribution = LeCun.Distribution.NORMAL }
        lecun.initializeWeights(wm)
        
        val expectedStd = sqrt(1.0 / numInputs)
        val mean = wm.weightArray.average()
        val variance = wm.weightArray.map { (it - mean) * (it - mean) }.average()
        val actualStd = sqrt(variance)
        
        assertTrue(abs(mean) < 0.05, "LeCun initialization should have zero mean")
        assertTrue(abs(actualStd - expectedStd) < 0.05, 
                  "LeCun initialization should have std=$expectedStd for SELU networks, got $actualStd")
    }

    @Test
    fun `test Matrix vs WeightMatrix initialization consistency`() {
        val numInputs = 10
        val numOutputs = 5
        
        // Initialize using WeightMatrix
        val na1 = NeuronArray(numInputs)
        val na2 = NeuronArray(numOutputs)
        val wm = WeightMatrix(na1, na2)
        val xavier1 = Xavier(42L)
        xavier1.initializeWeights(wm)
        val wmWeights = wm.weights.clone()
        
        // Initialize using Matrix directly
        val matrix = Matrix(numOutputs, numInputs)  // Note: Matrix dimensions are (output, input)
        val xavier2 = Xavier(42L)
        xavier2.initializeWeights(matrix)
        
        // Both should use the same initialization logic
        assertMatrixEquals(wmWeights, matrix, "WeightMatrix and Matrix initialization should be consistent")
    }

    @Test
    fun `test Randomize with custom distribution`() {
        val na1 = NeuronArray(8)
        val na2 = NeuronArray(4)
        val wm = WeightMatrix(na1, na2)
        
        val customDist = UniformRealDistribution(-0.5, 0.5)
        val randomize = Randomize(42L).apply { distribution = customDist }
        randomize.initializeWeights(wm)
        
        assertTrue(wm.weightArray.all { it in -0.5..0.5 }, 
                  "Randomize should respect custom distribution bounds")
        
        // Test with normal distribution
        val normalDist = NormalDistribution(1.0, 0.1)  // mean=1.0, std=0.1
        randomize.distribution = normalDist
        randomize.initializeWeights(wm)
        
        val mean = wm.weightArray.average()
        assertTrue(abs(mean - 1.0) < 0.2, "Randomize should respect custom normal distribution mean")
    }

    @Test
    fun `test all strategies handle edge cases`() {
        // Test with minimum size network (1x1)
        val minInput = NeuronArray(1)
        val minOutput = NeuronArray(1)
        val minWm = WeightMatrix(minInput, minOutput)
        
        val strategies = listOf(
            Xavier(42L),
            He(42L),
            LeCun(42L),
            Randomize(42L)
        )
        
        strategies.forEach { strategy ->
            assertDoesNotThrow {
                strategy.initializeWeights(minWm)
                assertTrue(minWm.weightArray.all { it.isFinite() }, 
                          "${strategy::class.simpleName} should produce finite values")
            }
        }
    }

    @Test
    fun `test initialization strategy differences`() {
        val numInputs = 20
        val numOutputs = 10
        val na1 = NeuronArray(numInputs)
        val na2 = NeuronArray(numOutputs)
        
        // Create multiple weight matrices for comparison
        val wmXavier = WeightMatrix(na1, na2)
        val wmHe = WeightMatrix(na1, na2)
        val wmLeCun = WeightMatrix(na1, na2)
        
        Xavier(42L).initializeWeights(wmXavier)
        He(42L).initializeWeights(wmHe)
        LeCun(42L).initializeWeights(wmLeCun)
        
        // Different strategies should produce different weight patterns
        assertFalse(wmXavier.weightArray.contentEquals(wmHe.weightArray), 
                   "Xavier and He should produce different weight patterns")
        assertFalse(wmXavier.weightArray.contentEquals(wmLeCun.weightArray), 
                   "Xavier and LeCun should produce different weight patterns")
        assertFalse(wmHe.weightArray.contentEquals(wmLeCun.weightArray), 
                   "He and LeCun should produce different weight patterns")
        
        // But all should produce reasonable weight distributions
        listOf(wmXavier, wmHe, wmLeCun).forEach { wm ->
            val mean = wm.weightArray.average()
            assertTrue(abs(mean) < 0.2, "All strategies should produce approximately zero mean")
            assertTrue(wm.weightArray.all { it.isFinite() }, "All strategies should produce finite weights")
        }
    }

    @Test
    fun `test null seed handling`() {
        val na1 = NeuronArray(5)
        val na2 = NeuronArray(3)
        val wm1 = WeightMatrix(na1, na2)
        val wm2 = WeightMatrix(na1, na2)
        
        // Initialize with null seeds (should use random seeding)
        val xavier1 = Xavier(null)
        val xavier2 = Xavier(null)
        
        xavier1.initializeWeights(wm1)
        xavier2.initializeWeights(wm2)
        
        // With null seeds, results should likely be different (though not guaranteed)
        // We just test that it doesn't crash and produces valid values
        assertTrue(wm1.weightArray.all { it.isFinite() }, "Null seed should produce finite values")
        assertTrue(wm2.weightArray.all { it.isFinite() }, "Null seed should produce finite values")
    }

}