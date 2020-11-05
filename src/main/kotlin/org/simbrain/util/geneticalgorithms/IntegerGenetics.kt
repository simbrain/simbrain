package org.simbrain.util.geneticalgorithms

import kotlin.random.Random


// Yulin will make work and make corresponding API changes
// Also cohere with IntEvolution and IntEvolutionDSL.

class IntWrapper (var value: Int = 0)

inline fun intGene(initVal : IntWrapper.() -> Unit = { }): IntGene {
    return IntGene(IntWrapper().apply(initVal))
}

class IntGene (template: IntWrapper) : Gene<IntWrapper>(template) {

    override fun copy(): IntGene {
        return IntGene(template);
    }

    fun build() : IntWrapper {
        return template;
    }
}

fun main() {

    val environmentBuilder = environmentBuilder {

        val intChromosome = chromosome(100) {
            intGene{value = 5}
        }

        onMutate {
            intChromosome.eachMutate {
                value = Random.nextInt(-2,2)
            }
        }

        onBuild {
            intChromosome.genes.forEach{
                it.build()
            }
        }

        onEval {
            val total = intChromosome.products.sumByDouble {it.value.toDouble() }
            Math.abs(total - 10)
        }

    }

    val evolution  = evaluator(environmentBuilder) {
        populationSize = 100
        eliminationRatio = 0.5
        optimizationMethod = Evaluator.OptimizationMethod.MINIMIZE_FITNESS
        runUntil { generation == 500 || fitness < .2 }
    }

    val best = evolution.start().last().first()
    print(best)

}




