package org.simbrain.util.geneticalgorithms

import org.simbrain.util.propertyeditor.CopyableObject
import kotlin.random.Random
import kotlin.system.measureTimeMillis


// Yulin will make work and make corresponding API changes
// Also cohere with IntEvolution and IntEvolutionDSL.

class IntWrapper(var value: Int = 0) : CopyableObject {
    override fun toString(): String {
        return "" + value
    }

    override fun copy(): IntWrapper {
        return IntWrapper(value)
    }
}

inline fun intGene(initVal: IntWrapper.() -> Unit = { }): IntGene {
    return IntGene(IntWrapper().apply(initVal))
}

class IntGene(template: IntWrapper) : Gene<IntWrapper>(template), TopLevelGene<IntWrapper> {

    override fun copy(): IntGene {
        return IntGene(template.copy());
    }

    override fun TopLevelBuilderContext.build(): IntWrapper {
        return template.copy();
    }
}

fun main() {

    val environmentBuilder = environmentBuilder {

        val intChromosome = chromosome(10) {
            intGene { value = -100 }
        }

        onMutate {
            intChromosome.eachMutate {
                value += Random.nextInt(-2, 2)
            }
        }

        onBuild {
            +intChromosome
        }

        onEval {
            val total = intChromosome.products.sumByDouble { it.value.toDouble() }
            Math.abs(total - 10)
        }

        onPeek {
            println(intChromosome.products.map { it.value }.joinToString(", "))
        }

    }

    val evolution = evaluator(environmentBuilder) {
        populationSize = 100
        eliminationRatio = 0.5
        optimizationMethod = Evaluator.OptimizationMethod.MINIMIZE_FITNESS
        runUntil { generation == 10000 || fitness < .2 }
    }

    val time = measureTimeMillis {
        val (builder, fitness) = evolution.start().last().first()
        builder.build().peek()
        println(fitness)
    }

    println("Finished in ${time / 1000.0}s")

}




