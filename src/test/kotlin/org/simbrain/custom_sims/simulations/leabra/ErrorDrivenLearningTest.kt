package org.simbrain.custom_sims.simulations.leabra

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.custom_sims.SimulationScope
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.NeuronArray
import org.simbrain.network.core.WeightMatrix
import org.simbrain.network.updaterules.LeabraRule
import org.simbrain.workspace.serialization.WorkspaceSerializer
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.math.abs

class ErrorDrivenLearningTest {

    @Test
    fun `flashcard pictures are all 7 by 7 and distinct`() {
        flashcards.forEach { assertEquals(49, it.picture.size, it.english) }
        assertEquals(flashcards.size, flashcards.map { it.picture.toList() }.toSet().size)
    }

    @Test
    fun `simulation settles headlessly and survives save and reopen`() = runBlocking {
        val scope = SimulationScope()
        errorDrivenLearning.task.invoke(scope, null)
        val workspace = scope.workspace

        fun network() = (workspace.getComponent("Leabra network") as NetworkComponent).network
        fun settleHidden(): Double {
            val network = network()
            val picture = network.getModels<NeuronArray>().first { it.label == "Picture" }
            picture.isClamped = true
            picture.activationArray = flashcards.first().picture
            repeat(50) { network.update() }
            return network.getModels<NeuronArray>().first { it.label == "Hidden" }.activationArray.sum()
        }

        assertEquals(3, network().getModels<NeuronArray>().size)
        assertEquals(3, network().getModels<WeightMatrix>().size)
        assertTrue(settleHidden() > 0.0)

        val serializer = WorkspaceSerializer(workspace)
        val bytes = ByteArrayOutputStream().also { serializer.serialize(it, true) }.toByteArray()
        workspace.clearWorkspace()
        serializer.deserialize(ByteArrayInputStream(bytes))
        errorDrivenLearning.reopen(workspace)

        val word = network().getModels<NeuronArray>().first { it.label == "Word" }
        assertTrue(word.updateRule is LeabraRule)
        assertEquals("sol", word.labelArray.first())
        assertTrue(settleHidden() > 0.0)
    }

    @Test
    fun `showing the answer shifts hidden units and practice on one card makes the network guess its word`() = runBlocking {
        val scope = SimulationScope()
        errorDrivenLearning.task.invoke(scope, null)
        val network = (scope.workspace.getComponent("Leabra network") as NetworkComponent).network
        val trainer = flashcardTrainer(network)
        val (_, hidden, word) = trainer.layers
        val sun = flashcards.first()

        fun trial(): Triple<Int, DoubleArray, DoubleArray> {
            trainer.test(sun.picture, fireEvents = false)
            val minus = trainer.snapshot()
            val acts = word.activationArray
            val guess = acts.indices.maxBy { acts[it] }
            trainer.clamp(word, oneHot(flashcards.size, 0))
            repeat(trainer.plusCycles) { trainer.cycle(fireEvents = false) }
            val plus = trainer.snapshot()
            trainer.learn(minus, plus)
            fun change(layer: NeuronArray) = DoubleArray(layer.size) { plus.getValue(layer)[it] - minus.getValue(layer)[it] }
            return Triple(guess, change(word), change(hidden))
        }

        val (_, _, firstHidden) = trial()
        assertTrue(
            firstHidden.count { abs(it) > 0.2 } >= 2,
            "hidden units turn on or off when the answer is shown: ${firstHidden.joinToString { "%.2f".format(it) }}"
        )

        var lastGuess = -1
        repeat(5) { lastGuess = trial().first }
        assertEquals(0, lastGuess, "guesses sol after practice")
    }

    @Test
    fun `deck is learned, half pictures are named above chance, and a wrong answer is learned and undone`() = runBlocking {
        val scope = SimulationScope()
        errorDrivenLearning.task.invoke(scope, null)
        val network = (scope.workspace.getComponent("Leabra network") as NetworkComponent).network
        val trainer = flashcardTrainer(network)
        val word = trainer.outputLayer
        val pictures = flashcards.map { it.picture }
        val answers = flashcards.indices.map { oneHot(flashcards.size, it) }
        fun guess(input: DoubleArray): Int {
            trainer.test(input, fireEvents = false)
            val acts = word.activationArray
            val best = acts.indices.maxBy { acts[it] }
            return if (acts[best] > 0.5) best else -1
        }

        repeat(30) { if (trainer.errorRate(pictures, answers) > 0) trainer.trainEpoch(pictures, answers) }
        assertEquals(0.0, trainer.errorRate(pictures, answers))

        val trials = 20
        val named = (1..trials).sumOf {
            flashcards.indices.count { card ->
                val lit = pictures[card].indices.filter { pictures[card][it] > 0 }.shuffled()
                val half = lit.take(lit.size / 2).toSet()
                guess(DoubleArray(pictures[card].size) { if (it in half) 1.0 else 0.0 }) == card
            }
        }
        // Chance is one in eight
        assertTrue(named > trials * flashcards.size / 3, "named $named of ${trials * flashcards.size}")

        val heart = flashcards.indexOfFirst { it.english == "heart" }
        val pez = flashcards.indexOfFirst { it.spanish == "pez" }
        var lessons = 0
        while (guess(pictures[heart]) != pez && lessons < 3) {
            trainer.trainTrial(pictures[heart], answers[pez])
            lessons++
        }
        assertEquals(pez, guess(pictures[heart]))

        repeat(3) { if (guess(pictures[heart]) != heart) trainer.trainEpoch(pictures, answers) }
        assertEquals(heart, guess(pictures[heart]))
    }
}
