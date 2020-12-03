package org.simbrain.util.geneticalgorithms

import org.simbrain.util.propertyeditor.CopyableObject
import kotlin.random.Random
import kotlin.system.measureTimeMillis

/**
 * The "gene product" that we are evolving. Must be a [CopyableObject].
 * Must take some mutable var property (here value) so that something can happen during mutation.
 */
class IntWrapper(var value: Int = 0) : CopyableObject {
    override fun toString(): String {
        return "" + value
    }

    override fun copy(): IntWrapper {
        return IntWrapper(value)
    }
}

/**
 * Builder function for [IntGene].
 */
inline fun intGene(initVal: IntWrapper.() -> Unit = { }): IntGene {
    return IntGene(IntWrapper().apply(initVal))
}

/**
 * The integer gene, which takes a template "product" ([IntWrapper]) as an argument and extends
 * [Gene] and implements [TopLevelGene], which allows you to add this gene directly into an onBuild context.
 * function.
 */
class IntGene(template: IntWrapper) : Gene<IntWrapper>(template), TopLevelGene<IntWrapper> {

    override fun copy(): IntGene {
        return IntGene(template.copy());
    }

    override fun TopLevelBuilderContext.build(): IntWrapper {
        return template.copy();
    }
}

/**
 * The main integer evolution simulation.
 */
fun main() {

    val environmentBuilder = environmentBuilder {

        /**
         * Set number of integers per chromosome here
         */
        val intChromosome = chromosome(5) {
            // Default value for integers
            intGene { value = 1 }
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
            val targetSum = 10
            Math.abs(total - targetSum)
        }

        onPeek {
            print("Integer genes:")
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
        println("Fitness: $fitness $")
    }

    println("Finished in ${time / 1000.0}s")

}




