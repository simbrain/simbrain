package org.simbrain.util.geneticalgorithm

import org.junit.jupiter.api.Test
import org.simbrain.network.updaterules.BinaryRule

class GeneticUtilsTest {

    @Test
    fun `mutateType leaves rule unchanged when probability is zero`() {
        class BinaryRuleGenotype : Genotype(40L) {
            val gene = neuronRuleGene(BinaryRule())
            override fun createNew(seed: Long) = BinaryRuleGenotype()
            override fun mutate() {
                gene.mutateType(probabilityOfChange = 0.0)
            }
        }
        val genotype = BinaryRuleGenotype()
        genotype.mutate()
        assert(genotype.gene.template.updateRule is BinaryRule)
    }
}
