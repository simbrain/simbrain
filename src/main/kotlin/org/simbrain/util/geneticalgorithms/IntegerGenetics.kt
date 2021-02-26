package org.simbrain.util.geneticalgorithms

import org.simbrain.util.propertyeditor.CopyableObject
import java.util.concurrent.CompletableFuture
import kotlin.math.abs
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
class IntGene(private val template: IntWrapper) : Gene<Int>(), TopLevelGene<Int> {

    override val product = CompletableFuture<Int>()

    override fun copy(): IntGene {
        return IntGene(template.copy());
    }

    override fun TopLevelBuilderContext.build(): Int {
        template.copy().also { product.complete(it.value) }
        return product.get()
    }

    fun mutate(block: IntWrapper.() -> Unit) {
        template.apply(block)
    }
}

/**
 * The main integer evolution simulation.
 */
fun main() {

    val environmentBuilder = evolutionarySimulation {

        /**
         * Set number of integers per chromosome here
         */
        val intChromosome = chromosome(5) {
            // Default value for integers
            intGene { value = 1 }
        }

        onMutate {
            intChromosome.forEach {
                it.mutate {
                    value += random.nextInt(-5,5)
                }
            }
        }

        onBuild {
            +intChromosome
        }

        onEval {
            val total = intChromosome.genes.map { it.product.get() }.sumByDouble { it.toDouble() }
            val targetSum = 10
            abs(total - targetSum)
        }

        onPeek {
            print("Integer genes:")
            println(intChromosome.genes.map { it.product.get() }.joinToString(", "))
        }

    }

    val evolution = evaluator(environmentBuilder) {
        populationSize = 100
        eliminationRatio = 0.5
        optimizationMethod = Evaluator.OptimizationMethod.MINIMIZE_FITNESS
        runUntil { generation == 10000 || fitness < .2 }
    }

    val time = measureTimeMillis {
        val (builder, fitness) = evolution.start().best
        builder.build().peek()
        println("Fitness: $fitness")
    }

    println("Finished in ${time / 1000.0}s")

}




