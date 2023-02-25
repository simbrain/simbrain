package org.simbrain.custom_sims.simulations

import org.simbrain.custom_sims.newSim
import java.util.*

/**
 * See https://www.sciencedirect.com/science/article/pii/S0006899321004352
 */
val multiScalePatternCompletion = newSim {

    class State(val name: String) {
        override fun toString(): String {
            return name
        }
    }
    class Transition(val from: State, val to: State, val probability: Double)

    fun List<Transition>.sampleFirst(): State {
        val randomIndex = Random().nextInt(size)
        return this[randomIndex].from
    }

    fun List<Transition>.sampleNext(firstState: State): State {
        var cumulativeProb = 0.0
        val randNum = Random().nextDouble()
        filter { it.from == firstState}.forEach {
            cumulativeProb += it.probability
            if (randNum < cumulativeProb ) return it.to
        }
        return State("ERROR")
    }

    // Nouns
    val man = State("man")
    val dog = State("dog")

    // Verbs
    val walks = State("walks")
    val bites = State("bites")

    val nounVerbTransitions = listOf(
        Transition(man, walks, 0.75),
        Transition(man, bites, 0.25),
        Transition(dog, walks, 0.25),
        Transition(dog, bites, 0.75))

    val verbNounTransitions = listOf(
        Transition(walks, dog, 0.75),
        Transition(walks, man, 0.25),
        Transition(bites, dog, 0.25),
        Transition(bites, man, 0.75))

    repeat(10) {
        val firstWord = nounVerbTransitions.sampleFirst()
        val secondWord = nounVerbTransitions.sampleNext(firstWord)
        val thirdWord = verbNounTransitions.sampleNext(secondWord)
        println("$firstWord $secondWord $thirdWord")
    }

}

