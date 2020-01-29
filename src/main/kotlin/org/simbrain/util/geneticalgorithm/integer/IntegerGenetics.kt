package org.simbrain.util.geneticalgorithm.integer

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import org.simbrain.custom_sims.RegisteredSimulation
import org.simbrain.util.clip
import org.simbrain.util.format
import org.simbrain.util.geneticalgorithm.*
import org.simbrain.util.nextBoolean
import org.simbrain.workspace.gui.SimbrainDesktop
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.run as run1

class IntEvolution(desktop: SimbrainDesktop? = null) : RegisteredSimulation(desktop) {

    override fun run() {

        val config = IntegerGeneticConfig(
                min = 0,
                max = 20,
                initialLength = 10,
                populationSize = 100
        )

        val env = IntegerEnvironment(config) {

            // Try to get the ints to sum to exactly 8
            // abs(chromosome.genes.map { it.value }.sum().toDouble()-8)

            // Try to get the ints to sum to exactly 125
            abs(chromosome.genes.map { it.value }.sum().toDouble()-125)

            // TODO: Make it easy to add more options here
        }

        var counter = 0

        val thing = env.newEvolution(optimizeFor = Optimize.small)
                .upToGeneration(500)
                .untilFitnessScore { it < 1 }
                .onEach { println("|${++counter}|${it.best()}") }
                .last()
                .best()

        println("FINAL RESULT -> $thing")
    }

    override fun getName() = "Int Evolution Kotlin"
    override fun getSubmenuName() = "Evolution"
    override fun instantiate(desktop: SimbrainDesktop?) = IntEvolution(desktop)

}

class IntegerEnvironment(override val config: IntegerGeneticConfig, eval: IntegerGenome.() -> Double)
    : Environment<IntegerAgent, IntegerGenome>(
        seed = config.seed,
        name = "IntGenome", eval = eval
), CoroutineScope by MainScope() {

    /**
     * Seed Population. Creates a list of integer agents.
     */
    override val initialPopulation: List<IntegerAgent> =
            generateSequence {
                IntegerGenome(
                        id = simpleId.id,
                        chromosome = config.defaultChromosome ?: IntegerChromosome(
                                genes = List(config.initialLength) {
                                    IntegerGene((config.min..config.max).random())
                                }
                        ),
                        config = config
                )
            }.take(config.populationSize).init()

    override fun IntegerAgent.eval() = copy(fitness = genome.eval())

    override fun IntegerGenome.express() = IntegerAgent(this, Double.NaN)

    override infix fun IntegerGenome.crossOver(other: IntegerGenome) = copy(
            id = simpleId.id,
            chromosome = this.chromosome crossOver other.chromosome
    )

    private infix fun IntegerChromosome.crossOver(other: IntegerChromosome) = copy(
            genes = this.genes singlePointCrossOver other.genes
    )


    private fun IntegerGene.mutate() = copy(
            value = (value + (-config.step..config.step).random())
                    .clip(config.min, config.max)
    )

    private fun IntegerChromosome.mutate() = copy(
            genes = if (geneticsRandom.nextBoolean(config.newGeneMutationProbability)) {
                genes.map { it.mutate() } + IntegerGene()
            } else {
                genes.map { it.mutate() }
            }
    )

    override fun IntegerGenome.mutate() = copy(chromosome = chromosome.mutate())

}

data class IntegerGeneticConfig(
        val min: Int = -100,
        val max: Int = 100,
        val step: Int = 1,
        val initialLength: Int = 5,
        val newGeneMutationProbability: Double = 0.2,
        val seed: Long = System.currentTimeMillis(),
        override val populationSize: Int,
        val defaultChromosome: IntegerChromosome? = null
) : EnvironmentConfig

data class IntegerAgent(override val genome: IntegerGenome, override val fitness: Double) : Agent2<IntegerGenome>() {
    override fun toString() = "Fitness: ${fitness.format(3)}, Genome: $genome"
}

data class IntegerGenome(
        val chromosome: IntegerChromosome,
        val config: IntegerGeneticConfig,
        override val id: String
) : Genome2() {
    override fun toString() = "[$id]$chromosome"
}

data class IntegerChromosome(override val genes: List<IntegerGene>) : Chromosome2<IntegerGene>() {
    override fun toString() = "(${genes.size})$genes"
}

data class IntegerGene(val value: Int = 0, val mutable: Boolean = true) : Gene2() {
    override fun toString() = value.toString()
}

/**
 * Test main.
 */
fun main() {
    val ie = IntEvolution()
    ie.run()
}