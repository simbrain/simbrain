package org.simbrain.util.geneticalgorithm

import org.junit.jupiter.api.Test
import org.simbrain.network.neuron_update_rules.BinaryRule
import kotlin.random.Random

class GeneticUtilsTest {

    @Test
    fun `test mutate type`() {
        class myGenotype: Genotype {
            override val random: Random = Random(40L)
            val gene = neuronRuleGene(BinaryRule())
            fun mutate() {
                gene.mutateType(probabilityOfChange = 0.0)
            }
        }
        val genotype = myGenotype()
        genotype.mutate()
        assert(genotype.gene.template.updateRule is BinaryRule)
    }
}