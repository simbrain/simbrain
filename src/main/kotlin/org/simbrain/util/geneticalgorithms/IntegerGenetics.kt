package org.simbrain.util.geneticalgorithms

// TODO: Design problem Yulin mentioned.  Also cohere with IntEvolution and IntEvolutionDSL.
inline fun intGene(initVal : Int): IntGene {
    return IntGene(initVal)
}

class IntGene (template: Int) : Gene<Int>(template) {

    override fun copy(): IntGene {
        return IntGene(template);
    }

    fun build() : Int {
        return template;
    }

}




