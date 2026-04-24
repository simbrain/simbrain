package org.simbrain.network.trainers

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.simbrain.network.core.Network
import org.simbrain.network.subnetworks.BackpropNetwork

class SupervisedTrainerTest {

    val net = Network()
    val bp = BackpropNetwork(intArrayOf(10,8,10), null)

    @Test
    fun `test trainer state`() {
        val trainer = SupervisedTrainer(net, bp)
        assertEquals(false, trainer.isRunning)
        runBlocking {
            trainer.startTraining()
            assertEquals(true, trainer.isRunning)
            trainer.stopTraining()
            assertEquals(false, trainer.isRunning)
        }
    }
    
    // StoppingCondition Tests
    
    @Test
    fun `test maxIterations stops training`() {
        val condition = SupervisedTrainer.StoppingCondition().apply {
            maxIterations = 100
        }
        
        // Should not stop before maxIterations
        assertFalse(condition.validate(99, 0.5))
        
        // Should stop at maxIterations
        assertTrue(condition.validate(100, 0.5))
        
        // Should stop after maxIterations
        assertTrue(condition.validate(101, 0.5))
    }
    
    @Test
    fun `test errorThreshold stops training when enabled`() {
        val condition = SupervisedTrainer.StoppingCondition().apply {
            maxIterations = 1000
            useErrorThreshold = true
            errorThreshold = 0.1
        }
        
        // Should not stop when error is above threshold
        assertFalse(condition.validate(50, 0.15))
        
        // Should stop when error is below threshold
        assertTrue(condition.validate(50, 0.05))
        
        // Should NOT stop when error equals threshold (boundary case - uses strict <)
        assertFalse(condition.validate(50, 0.1))
    }
    
    @Test
    fun `test errorThreshold disabled does not stop training`() {
        val condition = SupervisedTrainer.StoppingCondition().apply {
            maxIterations = 1000
            useErrorThreshold = false
            errorThreshold = 0.1
        }
        
        // Should not stop even when error is below threshold (disabled)
        assertFalse(condition.validate(50, 0.01))
        
        // Should eventually stop at maxIterations
        assertTrue(condition.validate(1000, 0.01))
    }
    
    @Test
    fun `test early stopping with patience`() {
        val condition = SupervisedTrainer.StoppingCondition().apply {
            maxIterations = 10000
            useEarlyStopping = true
            earlyStoppingPatience = 3
            earlyStoppingMinDelta = 0.0
        }
        
        // First test with improving error - should not stop
        assertFalse(condition.validate(10, 1.0, 1.0))
        assertFalse(condition.validate(20, 1.0, 0.9))  // Improvement
        assertFalse(condition.validate(30, 1.0, 0.8))  // Improvement
        
        // Now test with no improvement - should increment patience
        assertFalse(condition.validate(40, 1.0, 0.85)) // No improvement (patience = 1)
        assertFalse(condition.validate(50, 1.0, 0.81)) // No improvement (patience = 2)
        assertTrue(condition.validate(60, 1.0, 0.82))  // No improvement (patience = 3, should stop)
    }
    
    @Test
    fun `test early stopping with minDelta threshold`() {
        val condition = SupervisedTrainer.StoppingCondition().apply {
            maxIterations = 10000
            useEarlyStopping = true
            earlyStoppingPatience = 2
            earlyStoppingMinDelta = 0.05  // Require at least 0.05 improvement
        }
        
        // Initial error (sets bestTestError to 1.0)
        assertFalse(condition.validate(10, 1.0, 1.0))
        
        // Small improvement (< minDelta) should count as no improvement
        assertFalse(condition.validate(20, 1.0, 0.97)) // Only 0.03 improvement (patience = 1)
        assertTrue(condition.validate(30, 1.0, 0.96)) // Only 0.04 total (patience = 2, should stop)
        
        // Reset for next test
        condition.resetEarlyStopping()
        
        // Large improvement (> minDelta) should reset patience
        assertFalse(condition.validate(10, 1.0, 1.0))
        assertFalse(condition.validate(20, 1.0, 0.94)) // 0.06 improvement - should reset patience
        assertFalse(condition.validate(30, 1.0, 0.95)) // No improvement (patience = 1)
        assertTrue(condition.validate(40, 1.0, 0.96))  // No improvement (patience = 2, should stop)
    }
    
    @Test
    fun `test early stopping disabled does not stop`() {
        val condition = SupervisedTrainer.StoppingCondition().apply {
            maxIterations = 10000
            useEarlyStopping = false
            earlyStoppingPatience = 1
        }
        
        // Even with no improvement, should not stop when disabled
        assertFalse(condition.validate(10, 1.0, 1.0))
        assertFalse(condition.validate(20, 1.0, 1.1)) // Getting worse
        assertFalse(condition.validate(30, 1.0, 1.2)) // Still getting worse
        assertFalse(condition.validate(40, 1.0, 1.3)) // Still getting worse
    }
    
    @Test
    fun `test early stopping without test error does not stop`() {
        val condition = SupervisedTrainer.StoppingCondition().apply {
            maxIterations = 10000
            useEarlyStopping = true
            earlyStoppingPatience = 1
        }
        
        // Without test error (null), early stopping should not trigger
        assertFalse(condition.validate(10, 1.0, null))
        assertFalse(condition.validate(20, 1.0, null))
        assertFalse(condition.validate(30, 1.0, null))
    }
    
    @Test
    fun `test resetEarlyStopping clears state`() {
        val condition = SupervisedTrainer.StoppingCondition().apply {
            maxIterations = 10000
            useEarlyStopping = true
            earlyStoppingPatience = 2
            earlyStoppingMinDelta = 0.0
        }
        
        // Build up patience counter
        assertFalse(condition.validate(10, 1.0, 1.0))
        assertFalse(condition.validate(20, 1.0, 1.1)) // No improvement (patience = 1)
        assertTrue(condition.validate(30, 1.0, 1.2)) // No improvement (patience = 2, should stop)
        
        // Reset should clear state
        condition.resetEarlyStopping()
        
        // Now should take full patience again before stopping
        assertFalse(condition.validate(40, 1.0, 1.0))
        assertFalse(condition.validate(50, 1.0, 1.1)) // No improvement (patience = 1)
        assertTrue(condition.validate(60, 1.0, 1.2))  // No improvement (patience = 2, should stop)
    }
    
    @Test
    fun `test multiple stopping conditions work independently`() {
        val condition = SupervisedTrainer.StoppingCondition().apply {
            maxIterations = 100
            useErrorThreshold = true
            errorThreshold = 0.1
            useEarlyStopping = true
            earlyStoppingPatience = 2
        }
        
        // Should stop on maxIterations
        assertTrue(condition.validate(100, 0.5, 0.5))
        
        // Reset and test error threshold
        condition.resetEarlyStopping()
        assertTrue(condition.validate(50, 0.05, 0.5)) // Training error below threshold
        
        // Reset and test early stopping
        condition.resetEarlyStopping()
        assertFalse(condition.validate(10, 0.5, 1.0))
        assertFalse(condition.validate(20, 0.5, 1.1)) // Patience = 1
        assertTrue(condition.validate(30, 0.5, 1.2))  // Patience = 2, should stop on early stopping
    }
    
    @Test
    fun `test getEarlyStoppingStatus returns correct info`() {
        val condition = SupervisedTrainer.StoppingCondition().apply {
            maxIterations = 10000
            useEarlyStopping = true
            earlyStoppingPatience = 3
        }
        
        // Initial status
        val status1 = condition.getEarlyStoppingStatus()
        assertNotNull(status1)
        assertTrue(status1!!.contains("N/A"))
        assertTrue(status1.contains("0/3"))
        
        // After some iterations
        condition.validate(10, 1.0, 1.0)
        condition.validate(20, 1.0, 1.1) // No improvement
        
        val status2 = condition.getEarlyStoppingStatus()
        assertNotNull(status2)
        assertTrue(status2!!.contains("1.000000"))
        assertTrue(status2.contains("1/3"))
    }
    
    @Test
    fun `test getEarlyStoppingStatus returns null when disabled`() {
        val condition = SupervisedTrainer.StoppingCondition().apply {
            maxIterations = 10000
            useEarlyStopping = false
        }
        
        assertNull(condition.getEarlyStoppingStatus())
    }
    
    @Test
    fun `test copy preserves settings but resets state`() {
        val original = SupervisedTrainer.StoppingCondition().apply {
            maxIterations = 500
            useErrorThreshold = true
            errorThreshold = 0.2
            useEarlyStopping = true
            earlyStoppingPatience = 5
            earlyStoppingMinDelta = 0.01
        }
        
        // Build up some state in original
        original.validate(10, 1.0, 1.0)
        original.validate(20, 1.0, 1.1) // Build patience
        
        val copy = original.copy()
        
        // Settings should be copied
        assertEquals(500, copy.maxIterations)
        assertTrue(copy.useErrorThreshold)
        assertEquals(0.2, copy.errorThreshold)
        assertTrue(copy.useEarlyStopping)
        assertEquals(5, copy.earlyStoppingPatience)
        assertEquals(0.01, copy.earlyStoppingMinDelta)
        
        // State should be reset (verify by checking status)
        val status = copy.getEarlyStoppingStatus()
        assertTrue(status!!.contains("N/A")) // Best error should be reset
        assertTrue(status.contains("0/5"))    // Patience should be reset
    }
    
    @Test
    fun `test early stopping improvement resets patience counter`() {
        val condition = SupervisedTrainer.StoppingCondition().apply {
            maxIterations = 10000
            useEarlyStopping = true
            earlyStoppingPatience = 2
            earlyStoppingMinDelta = 0.0
        }
        
        // Initial error
        assertFalse(condition.validate(10, 1.0, 1.0))
        
        // No improvement builds patience
        assertFalse(condition.validate(20, 1.0, 1.1)) // Patience = 1
        
        // Improvement should reset patience
        assertFalse(condition.validate(30, 1.0, 0.9)) // Improvement! Patience = 0
        
        // Now patience should build from 0 again
        assertFalse(condition.validate(40, 1.0, 0.95)) // Patience = 1
        assertTrue(condition.validate(50, 1.0, 0.96))  // Patience = 2, should stop
    }
    
    @Test
    fun `test boundary condition - zero maxIterations`() {
        val condition = SupervisedTrainer.StoppingCondition().apply {
            maxIterations = 0
        }
        
        // Should stop immediately
        assertTrue(condition.validate(0, 1.0))
        assertTrue(condition.validate(1, 1.0))
    }
    
    @Test
    fun `test boundary condition - exact error threshold match`() {
        val condition = SupervisedTrainer.StoppingCondition().apply {
            maxIterations = 1000
            useErrorThreshold = true
            errorThreshold = 0.123456
        }
        
        // Just above threshold - should not stop
        assertFalse(condition.validate(50, 0.123457))
        
        // Exact match - should NOT stop (uses strict <)
        assertFalse(condition.validate(50, 0.123456))
        
        // Below threshold - should stop
        assertTrue(condition.validate(50, 0.123455))
    }
}