/**
 * Tests that stopping evolution takes effect promptly: evaluations in flight are cancelled rather than
 * waited out, and no trailing generation runs after a stop request, which previously slipped through
 * the task queue behind the Stop and burned a full generation of CPU.
 */
package org.simbrain.util.geneticalgorithm

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.simbrain.workspace.Workspace
import java.util.concurrent.atomic.AtomicInteger

@Timeout(30)
class EvolutionRunnerStopTest {

    class StopGenotype(seed: Long) : Genotype(seed) {
        override fun createNew(seed: Long) = StopGenotype(seed)
        override fun mutate() {}
    }

    class CallbackSim(
        genotype: StopGenotype,
        private val onEval: suspend () -> Unit,
        workspace: Workspace = Workspace(),
        metadata: SimMetadata? = null
    ) : EvoSim<StopGenotype>(genotype, workspace, metadata) {
        override suspend fun onBuild() {}
        override fun create(genotype: StopGenotype, workspace: Workspace, metadata: SimMetadata?) =
            CallbackSim(genotype, onEval, workspace, metadata)

        override suspend fun eval(): Double {
            onEval()
            return 0.0
        }
    }

    @Test
    fun `stopping evolution cancels evaluations in flight`() = runBlocking {
        val evalStarted = CompletableDeferred<Unit>()
        val neverReleased = CompletableDeferred<Unit>()
        val runner = EvolutionRunner(
            populatingFunction = { seed ->
                CallbackSim(StopGenotype(seed), onEval = {
                    evalStarted.complete(Unit)
                    neverReleased.await()
                })
            },
            populationSize = 3,
            eliminationRatio = 0.0,
            stoppingFunction = { false }
        )
        val starting = launch { runner.startEvolving() }
        withTimeout(10_000) { evalStarted.await() }

        withTimeout(10_000) { runner.stopEvolving() }
        assertFalse(runner.isRunning)
        starting.join()
    }

    @Test
    fun `no evaluations run after a stop returns`() = runBlocking {
        val evalCount = AtomicInteger()
        val runner = EvolutionRunner(
            populatingFunction = { seed ->
                CallbackSim(StopGenotype(seed), onEval = { evalCount.incrementAndGet() })
            },
            populationSize = 2,
            eliminationRatio = 0.5,
            stoppingFunction = { false }
        )
        val starting = launch { runner.startEvolving() }
        withTimeout(10_000) {
            while (evalCount.get() < 10) yield()
        }
        withTimeout(10_000) { runner.stopEvolving() }
        starting.join()

        // A trailing generation queued behind the stop would add its evaluations within milliseconds
        val settled = evalCount.get()
        delay(300)
        assertEquals(settled, evalCount.get())
        assertFalse(runner.isRunning)
    }
}
