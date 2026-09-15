package org.simbrain.util.geneticalgorithm

import org.junit.jupiter.api.Test
import org.simbrain.network.updaterules.ThresholdRule

class GeneticUtilsTest {

    @Test
    fun `mutateType leaves rule unchanged when probability is zero`() {
        class ThresholdRuleGenotype : Genotype(40L) {
            val gene = neuronRuleGene(ThresholdRule())
            override fun createNew(seed: Long) = ThresholdRuleGenotype()
            override fun mutate() {
                gene.mutateType(probabilityOfChange = 0.0)
            }
        }
        val genotype = ThresholdRuleGenotype()
        genotype.mutate()
        assert(genotype.gene.template.updateRule is ThresholdRule)
    }
}
