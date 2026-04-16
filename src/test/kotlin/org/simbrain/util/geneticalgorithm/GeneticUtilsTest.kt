package org.simbrain.util.geneticalgorithm

import org.junit.jupiter.api.Test
import org.simbrain.network.updaterules.BinaryRule

class GeneticUtilsTest {

    @Test
    fun `test mutate type`() {
        class MyGenotype: Genotype(40L) {
            val gene = neuronRuleGene(BinaryRule())
            override fun createNew(seed: Long) = MyGenotype()
            override fun mutate() {
                gene.mutateType(probabilityOfChange = 0.0)
            }
        }
        val genotype = MyGenotype()
        genotype.mutate()
        assert(genotype.gene.template.updateRule is BinaryRule)
    }
}