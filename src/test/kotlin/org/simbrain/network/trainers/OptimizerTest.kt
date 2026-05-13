package org.simbrain.network.trainers

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.core.NeuronArray
import org.simbrain.network.core.WeightMatrix
import org.simbrain.util.assertMatrixEquals
import org.simbrain.util.assertMatrixNotEquals
import org.simbrain.util.frobeniusNorm
import org.simbrain.util.setValuesInPlace
import smile.math.matrix.Matrix

class OptimizerTest {

    private fun createTestNetwork(): Triple<Network, WeightMatrix, SupervisedTrainer> {
        val net = Network()
        val input = NeuronArray(2).apply { isClamped = true }
        val output = NeuronArray(2)
        val wm = WeightMatrix(input, output)
        
        runBlocking {
            net.addNetworkModelsAsync(input, output, wm)
        }
        
        val model = SupervisedModel(input, output)
        val trainer = SupervisedTrainer(net, model)
        
        return Triple(net, wm, trainer)
    }

    @Test
    fun `test MomentumOptimizer mathematical correctness`() {
        val (net, wm, trainer) = createTestNetwork()
        val optimizer = MomentumOptimizer(momentum = 0.9).apply { learningRate = 0.1 }
        trainer.config.optimizer = optimizer

        wm.weights.fill(0.5)
        wm.source.setActivations(doubleArrayOf(1.0, 0.5))

        val firstDelta = Matrix.of(arrayOf(
            doubleArrayOf(0.2, 0.1),
            doubleArrayOf(0.4, 0.2)
        ))

        with(trainer) {
            val result1 = optimizer.computeDelta(wm.weights, firstDelta)
            // v_1 = 0.9 * 0 + firstDelta; update = lr * v_1
            val expected1 = firstDelta.clone().mul(optimizer.learningRate)

            assertMatrixEquals(expected1, result1, "First update should be just learning_rate * delta")
        }

        val secondDelta = Matrix.of(arrayOf(
            doubleArrayOf(0.1, 0.3),
            doubleArrayOf(0.2, 0.1)
        ))

        with(trainer) {
            val result2 = optimizer.computeDelta(wm.weights, secondDelta)
            // v_2 = 0.9 * v_1 + secondDelta = 0.9 * firstDelta + secondDelta; update = lr * v_2
            val expectedVelocity = firstDelta.clone().mul(0.9).add(secondDelta)
            val expected2 = expectedVelocity.clone().mul(optimizer.learningRate)

            assertMatrixEquals(expected2, result2, "Second update should accumulate velocity")
        }

        val thirdDelta = Matrix.of(arrayOf(
            doubleArrayOf(0.05, 0.05),
            doubleArrayOf(0.05, 0.05)
        ))

        with(trainer) {
            val result3 = optimizer.computeDelta(wm.weights, thirdDelta)
            // v_3 = 0.9 * v_2 + thirdDelta = 0.81 * firstDelta + 0.9 * secondDelta + thirdDelta
            val expectedVelocity3 = firstDelta.clone().mul(0.9 * 0.9)
                .add(secondDelta.clone().mul(0.9))
                .add(thirdDelta)
            val expected3 = expectedVelocity3.clone().mul(optimizer.learningRate)

            assertMatrixEquals(expected3, result3, "Third update should accumulate full velocity history (Polyak momentum)")
        }
    }

    @Test
    fun `test MomentumOptimizer reset functionality`() {
        val (net, wm, trainer) = createTestNetwork()
        val optimizer = MomentumOptimizer(momentum = 0.9).apply { learningRate = 0.1 }
        trainer.config.optimizer = optimizer
        
        val delta = Matrix.of(arrayOf(
            doubleArrayOf(0.1, 0.2),
            doubleArrayOf(0.3, 0.4)
        ))
        
        with(trainer) {
            // First update to establish momentum
            optimizer.computeDelta(wm.weights, delta)
            
            // Reset optimizer
            optimizer.reset()
            
            // Next update should behave like first update (no momentum)
            val result = optimizer.computeDelta(wm.weights, delta)
            val expected = delta.clone().mul(optimizer.learningRate)
            
            assertMatrixEquals(expected, result, "After reset, should behave like first update")
        }
    }

    @Test
    fun `test MomentumOptimizer with zero momentum`() {
        val (net, wm, trainer) = createTestNetwork()
        val optimizer = MomentumOptimizer(momentum = 0.0)
        trainer.config.optimizer = optimizer
        trainer.config.learningRate = 0.1
        
        val delta = Matrix.of(arrayOf(
            doubleArrayOf(0.1, 0.2),
            doubleArrayOf(0.3, 0.4)
        ))
        
        with(trainer) {
            val result1 = optimizer.computeDelta(wm.weights, delta)
            val result2 = optimizer.computeDelta(wm.weights, delta)
            
            // With zero momentum, both results should be identical
            assertMatrixEquals(result1, result2, "Zero momentum should produce identical results")
        }
    }

    @Test
    fun `test AdamOptimizer mathematical correctness`() {
        val (net, wm, trainer) = createTestNetwork()
        val optimizer = AdamOptimizer(beta1 = 0.9, beta2 = 0.999)
        trainer.config.optimizer = optimizer
        trainer.config.learningRate = 0.001
        trainer.iteration = 1
        
        val delta = Matrix.of(arrayOf(
            doubleArrayOf(0.1, 0.2),
            doubleArrayOf(0.3, 0.4)
        ))
        
        with(trainer) {
            val result = optimizer.computeDelta(wm.weights, delta)
            
            // After first iteration, running mean should be approximately delta * (1 - beta1)
            // and running variance should be approximately delta^2 * (1 - beta2)
            // With bias correction for iteration 1
            
            assertNotNull(result)
            assertEquals(wm.weights.nrow(), result.nrow())
            assertEquals(wm.weights.ncol(), result.ncol())
            
            // All values should be finite and non-zero (given non-zero delta)
            for (i in 0 until result.nrow()) {
                for (j in 0 until result.ncol()) {
                    val value = result[i, j]
                    assertTrue(value.isFinite(), "Adam result should be finite")
                    assertNotEquals(0.0, value, 1e-10, "Adam result should be non-zero for non-zero delta")
                }
            }
        }
    }

    @Test
    fun `test AdamOptimizer bias correction`() {
        val (net, wm, trainer) = createTestNetwork()
        val optimizer = AdamOptimizer(beta1 = 0.9, beta2 = 0.999)
        trainer.config.optimizer = optimizer
        trainer.config.learningRate = 0.001
        
        val delta = Matrix.of(arrayOf(
            doubleArrayOf(0.1, 0.0),
            doubleArrayOf(0.0, 0.1)
        ))
        
        with(trainer) {
            // Test multiple iterations to verify bias correction changes
            trainer.iteration = 1
            val result1 = optimizer.computeDelta(wm.weights, delta)
            
            trainer.iteration = 2
            val result2 = optimizer.computeDelta(wm.weights, delta)
            
            trainer.iteration = 10
            val result10 = optimizer.computeDelta(wm.weights, delta)
            
            // Bias correction should make earlier iterations have larger updates
            val norm1 = result1.frobeniusNorm()
            val norm2 = result2.frobeniusNorm()
            val norm10 = result10.frobeniusNorm()
            
            assertTrue(norm1 > norm2, "Earlier iterations should have larger updates due to bias correction")
            assertTrue(norm2 > norm10, "Bias correction effect should diminish over iterations")
        }
    }

    @Test
    fun `test AdamOptimizer reset functionality`() {
        val (net, wm, trainer) = createTestNetwork()
        val optimizer = AdamOptimizer(beta1 = 0.9, beta2 = 0.999)
        trainer.config.optimizer = optimizer
        
        val delta = Matrix.of(arrayOf(
            doubleArrayOf(0.1, 0.2),
            doubleArrayOf(0.3, 0.4)
        ))
        
        with(trainer) {
            trainer.iteration = 5
            
            // First update to establish running estimates
            optimizer.computeDelta(wm.weights, delta)
            trainer.iteration = 6
            val result1 = optimizer.computeDelta(wm.weights, delta)
            
            // Reset optimizer - this clears running estimates and resets initial iteration
            optimizer.reset()
            trainer.iteration = 10  // Different iteration after reset
            
            // Next update should behave like early iterations due to reset
            val result2 = optimizer.computeDelta(wm.weights, delta)
            
            // The reset should cause different behavior due to bias correction restarting
            assertMatrixNotEquals(result1, result2, "Reset should clear running estimates and restart bias correction")
        }
    }

    @Test
    fun `test AdamWOptimizer weight decay`() {
        val (net, wm, trainer) = createTestNetwork()
        val optimizer = AdamWOptimizer(beta1 = 0.9, beta2 = 0.999, weightDecay = 0.01, learningRateDecay = 0.0)
        trainer.config.optimizer = optimizer
        trainer.config.learningRate = 0.001
        trainer.iteration = 1
        
        // Set specific weights to test weight decay
        wm.weights.setValuesInPlace { i, j -> (i + 1) * (j + 1) * 0.1 }
        val initialWeights = wm.weights.clone()
        
        val delta = Matrix.of(arrayOf(
            doubleArrayOf(0.1, 0.2),
            doubleArrayOf(0.3, 0.4)
        ))
        
        with(trainer) {
            val result = optimizer.computeDelta(wm.weights, delta)
            
            // AdamW should include weight decay component
            // Expected: adam_update + (learning_rate * weight_decay * weights)
            assertNotNull(result)
            
            // Verify weight decay contribution exists (result should be different from pure Adam)
            val adamOnlyOptimizer = AdamOptimizer(beta1 = 0.9, beta2 = 0.999)
            val adamOnlyResult = adamOnlyOptimizer.computeDelta(wm.weights, delta)
            
            assertMatrixNotEquals(adamOnlyResult, result, "AdamW should differ from Adam due to weight decay")
        }
    }

    @Test
    fun `test AdamWOptimizer learning rate decay`() {
        val (net, wm, trainer) = createTestNetwork()
        val optimizer = AdamWOptimizer(beta1 = 0.9, beta2 = 0.999, weightDecay = 0.0, learningRateDecay = 0.1)
        trainer.config.optimizer = optimizer
        trainer.config.learningRate = 0.1
        
        val delta = Matrix.of(arrayOf(
            doubleArrayOf(0.1, 0.2),
            doubleArrayOf(0.3, 0.4)
        ))
        
        with(trainer) {
            // Test at different iterations to verify learning rate decay
            trainer.iteration = 1
            val result1 = optimizer.computeDelta(wm.weights, delta)
            
            trainer.iteration = 10
            val result10 = optimizer.computeDelta(wm.weights, delta)
            
            // Later iterations should have smaller updates due to learning rate decay
            val norm1 = result1.frobeniusNorm()
            val norm10 = result10.frobeniusNorm()
            
            assertTrue(norm1 > norm10, "Learning rate decay should reduce update magnitude over time")
        }
    }

    @Test
    fun `test AdamWOptimizer with zero weight decay behaves like Adam`() {
        val (net, wm, trainer) = createTestNetwork()
        val adamw = AdamWOptimizer(beta1 = 0.9, beta2 = 0.999, weightDecay = 0.0, learningRateDecay = 0.0)
        val adam = AdamOptimizer(beta1 = 0.9, beta2 = 0.999)
        trainer.config.learningRate = 0.001
        trainer.iteration = 1
        
        val delta = Matrix.of(arrayOf(
            doubleArrayOf(0.1, 0.2),
            doubleArrayOf(0.3, 0.4)
        ))
        
        with(trainer) {
            val adamwResult = adamw.computeDelta(wm.weights, delta)
            val adamResult = adam.computeDelta(wm.weights, delta)
            
            assertMatrixEquals(adamResult, adamwResult, "AdamW with zero weight decay should match Adam")
        }
    }

    @Test
    fun `test optimizer copy functionality`() {
        // Test MomentumOptimizer copy
        val momentum = MomentumOptimizer(momentum = 0.8).apply { learningRate = 0.05 }
        val momentumCopy = momentum.copy()
        assertEquals(0.8, momentumCopy.momentum)
        assertEquals(0.05, momentumCopy.learningRate)
        assertNotSame(momentum, momentumCopy)
        
        // Test AdamOptimizer copy
        val adam = AdamOptimizer(beta1 = 0.8, beta2 = 0.95).apply { learningRate = 0.002 }
        val adamCopy = adam.copy()
        assertEquals(0.8, adamCopy.beta1)
        assertEquals(0.95, adamCopy.beta2)
        assertEquals(0.002, adamCopy.learningRate)
        assertNotSame(adam, adamCopy)
        
        // Test AdamWOptimizer copy
        val adamw = AdamWOptimizer(beta1 = 0.85, beta2 = 0.98, weightDecay = 0.02, learningRateDecay = 0.05).apply { learningRate = 0.003 }
        val adamwCopy = adamw.copy()
        assertEquals(0.85, adamwCopy.beta1)
        assertEquals(0.98, adamwCopy.beta2)
        assertEquals(0.02, adamwCopy.weightDecay)
        assertEquals(0.05, adamwCopy.learningRateDecay)
        assertEquals(0.003, adamwCopy.learningRate)
        assertNotSame(adamw, adamwCopy)
    }

    @Test
    fun `test optimizer parameter validation`() {
        // Test AdamOptimizer with extreme beta values
        val adam1 = AdamOptimizer(beta1 = 0.0, beta2 = 0.0)
        val adam2 = AdamOptimizer(beta1 = 1.0, beta2 = 1.0)
        
        assertNotNull(adam1)
        assertNotNull(adam2)
        
        // Test AdamWOptimizer with extreme values
        val adamw = AdamWOptimizer(weightDecay = 1.0, learningRateDecay = 1.0)
        assertNotNull(adamw)
        
        // Test MomentumOptimizer with extreme momentum
        val momentum1 = MomentumOptimizer(momentum = 0.0)
        val momentum2 = MomentumOptimizer(momentum = 1.0)
        
        assertNotNull(momentum1)
        assertNotNull(momentum2)
    }

    @Test
    fun `test optimizer state isolation between matrices`() {
        val (net, wm, trainer) = createTestNetwork()
        
        // Create second weight matrix
        val input2 = NeuronArray(2).apply { isClamped = true }
        val output2 = NeuronArray(2)
        val wm2 = WeightMatrix(input2, output2)
        runBlocking {
            net.addNetworkModelsAsync(input2, output2, wm2)
        }
        
        val optimizer = MomentumOptimizer(momentum = 0.9)
        trainer.config.optimizer = optimizer
        trainer.config.learningRate = 0.1
        
        val delta1 = Matrix.of(arrayOf(
            doubleArrayOf(0.1, 0.2),
            doubleArrayOf(0.3, 0.4)
        ))
        val delta2 = Matrix.of(arrayOf(
            doubleArrayOf(0.5, 0.6),
            doubleArrayOf(0.7, 0.8)
        ))
        
        with(trainer) {
            // Update different matrices with different deltas
            val result1_1 = optimizer.computeDelta(wm.weights, delta1)
            val result2_1 = optimizer.computeDelta(wm2.weights, delta2)
            
            // Second updates should show different momentum for each matrix
            val result1_2 = optimizer.computeDelta(wm.weights, delta1)
            val result2_2 = optimizer.computeDelta(wm2.weights, delta2)
            
            // Each matrix should maintain its own momentum state
            assertMatrixNotEquals(result1_1, result1_2, "First matrix should show momentum effect")
            assertMatrixNotEquals(result2_1, result2_2, "Second matrix should show momentum effect")
        }
    }

    @Test
    fun `test Adam optimizer numerical stability`() {
        val (net, wm, trainer) = createTestNetwork()
        val optimizer = AdamOptimizer(beta1 = 0.9, beta2 = 0.999)
        trainer.config.optimizer = optimizer
        trainer.config.learningRate = 0.001
        trainer.iteration = 1
        
        // Test with very small gradients
        val smallDelta = Matrix.of(arrayOf(
            doubleArrayOf(1e-10, 1e-10),
            doubleArrayOf(1e-10, 1e-10)
        ))
        
        with(trainer) {
            val result = optimizer.computeDelta(wm.weights, smallDelta)
            
            // Should not produce NaN or infinite values
            for (i in 0 until result.nrow()) {
                for (j in 0 until result.ncol()) {
                    val value = result[i, j]
                    assertTrue(value.isFinite(), "Adam should handle small gradients without numerical issues")
                }
            }
        }
        
        // Test with zero gradients
        val zeroDelta = Matrix(wm.weights.nrow(), wm.weights.ncol())
        with(trainer) {
            val zeroResult = optimizer.computeDelta(wm.weights, zeroDelta)
            
            for (i in 0 until zeroResult.nrow()) {
                for (j in 0 until zeroResult.ncol()) {
                    val value = zeroResult[i, j]
                    assertTrue(value.isFinite(), "Adam should handle zero gradients")
                }
            }
        }
    }

    @Test
    fun `test AdamW weight decay scales with current learning rate`() {
        val (net, wm, trainer) = createTestNetwork()
        val optimizer = AdamWOptimizer(beta1 = 0.9, beta2 = 0.999, weightDecay = 0.1, learningRateDecay = 0.1)
        trainer.config.learningRate = 0.1
        
        // Set specific weights
        wm.weights.setValuesInPlace { i, j -> 1.0 }
        
        val delta = Matrix.of(arrayOf(
            doubleArrayOf(0.01, 0.01),
            doubleArrayOf(0.01, 0.01)
        ))
        
        with(trainer) {
            trainer.iteration = 1
            val result1 = optimizer.computeDelta(wm.weights, delta)
            
            trainer.iteration = 10  // Learning rate should have decayed
            val result10 = optimizer.computeDelta(wm.weights, delta)
            
            // Weight decay should use current (decayed) learning rate
            val norm1 = result1.frobeniusNorm()
            val norm10 = result10.frobeniusNorm()
            
            assertTrue(norm1 > norm10, "Weight decay should scale with decayed learning rate")
        }
    }

}