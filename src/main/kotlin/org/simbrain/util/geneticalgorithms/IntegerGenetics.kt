package org.simbrain.util.geneticalgorithms

import org.simbrain.util.propertyeditor.CopyableObject
import kotlin.random.Random


// Yulin will make work and make corresponding API changes
// Also cohere with IntEvolution and IntEvolutionDSL.

class IntWrapper (var value: Int = 0) : CopyableObject {
    override fun toString(): String {
        return "" + value
    }

    override fun copy(): IntWrapper {
        return IntWrapper(value)
    }
}

inline fun intGene(initVal : IntWrapper.() -> Unit = { }): IntGene {
    return IntGene(IntWrapper().apply(initVal))
}

class IntGene(template: IntWrapper) : TopLevelGene<IntWrapper>(template) {

    override fun copy(): IntGene {
        return IntGene(template.copy());
    }

    override fun TopLevelBuilderContext.build() : IntWrapper {
        return template.copy();
    }
}

fun main() {

    val environmentBuilder = environmentBuilder {

        val intChromosome = chromosome(100) {
            intGene()
        }

        onMutate {
            intChromosome.eachMutate {
                value = Random.nextInt(-2,2)
            }
        }

        onBuild {
            +intChromosome
        }

        onEval {
            val total = intChromosome.products.sumByDouble {it.value.toDouble() }
            println(intChromosome.products)
            Math.abs(total - 10)
        }

    }

    val evolution  = evaluator(environmentBuilder) {
        populationSize = 100
        eliminationRatio = 0.5
        optimizationMethod = Evaluator.OptimizationMethod.MINIMIZE_FITNESS
        runUntil { generation == 100 || fitness < .2 }
    }

    val lastGen = evolution.start().last()
    print(lastGen.map { it.fitness })

}




