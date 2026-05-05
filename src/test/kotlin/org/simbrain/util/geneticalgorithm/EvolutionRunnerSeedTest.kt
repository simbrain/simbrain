package org.simbrain.util.geneticalgorithm

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import org.simbrain.workspace.Workspace

class EvolutionRunnerSeedTest {

    class SeedGenotype(seed: Long) : Genotype(seed) {
        override fun createNew(seed: Long) = SeedGenotype(seed)
        override fun mutate() {}
    }

    class SeedSim(
        genotype: SeedGenotype,
        workspace: Workspace = Workspace(),
        metadata: SimMetadata? = null
    ) : EvoSim<SeedGenotype>(genotype, workspace, metadata) {
        override suspend fun onBuild() {}
        override fun create(genotype: SeedGenotype, workspace: Workspace, metadata: SimMetadata?) =
            SeedSim(genotype, workspace, metadata)

        override suspend fun eval() = 0.0
    }

    private fun collectInitialPopulationSeeds(runnerSeed: Long, populationSize: Int = 5): List<Long> = runBlocking {
        val seeds = mutableListOf<Long>()
        val runner = EvolutionRunner(
            populatingFunction = { seed ->
                seeds.add(seed)
                SeedSim(SeedGenotype(seed))
            },
            populationSize = populationSize,
            eliminationRatio = 0.0,
            stoppingFunction = { generation >= 1 },
            seed = runnerSeed
        )
        runner.evolveOnce()
        seeds.toList()
    }

    @Test
    fun `initial population members receive distinct seeds`() {
        val seeds = collectInitialPopulationSeeds(runnerSeed = 42L)

        assertEquals(5, seeds.size)
        assertEquals(seeds.size, seeds.toSet().size)
    }

    @Test
    fun `same runner seed produces same initial population seed sequence`() {
        val firstRun = collectInitialPopulationSeeds(runnerSeed = 42L)
        val secondRun = collectInitialPopulationSeeds(runnerSeed = 42L)

        assertEquals(firstRun, secondRun)
    }

    @Test
    fun `different runner seeds produce different initial population seed sequences`() {
        val firstRun = collectInitialPopulationSeeds(runnerSeed = 42L)
        val secondRun = collectInitialPopulationSeeds(runnerSeed = 43L)

        assertNotEquals(firstRun, secondRun)
    }
}
