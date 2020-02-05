package org.simbrain.util.geneticalgorithm

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import org.simbrain.util.clip
import org.simbrain.util.format
import org.simbrain.util.nextBoolean

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
                        chromosome = config.defaultChromosome
                                ?: IntegerChromosome(
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