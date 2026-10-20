/**
 * Headless tiny language model training benchmark for the Simbrain 4 paper's PyTorch comparison. Tokenizes a text the
 * way the Tiny Language Model simulation does (SimpleTokenizer with punctuation, sequential 60/40 split), writes the
 * token ids so the PyTorch script trains on identical windows, and times epochs of [TinyLmModel.trainStep], which
 * is what [org.simbrain.network.trainers.TapeTrainer] runs: one window per optimizer step, windows in corpus order.
 *
 * Usage: TinyLmBenchmarkKt <textFile> <idsOut> <epochs> <contextSize> <embedDim> <numHeads> <hiddenDim> <numLayers>
 */
package org.simbrain.benchmarks

import org.simbrain.network.llm.TinyLmConfig
import org.simbrain.network.llm.TinyLmModel
import org.simbrain.util.SimpleTokenizer
import org.simbrain.util.tokenize
import java.io.File
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val text = File(args[0]).readText()
    val idsOut = File(args[1])
    val epochs = args[2].toInt()
    val (contextSize, embedDim, numHeads, hiddenDim, numLayers) = args.drop(3).take(5).map { it.toInt() }

    val tokens = text.tokenize(SimpleTokenizer(usePunctuation = true)).map { it.token }
    val vocab = tokens.distinct()
    val index = vocab.withIndex().associate { (i, token) -> token to i }
    val ids = tokens.map { index.getValue(it) }.toIntArray()
    val trainIds = ids.copyOfRange(0, (ids.size * 0.6).toInt())
    idsOut.writeText("${vocab.size}\n${trainIds.joinToString(" ")}\n")

    val windows = (0 until trainIds.size - contextSize).map { start ->
        trainIds.copyOfRange(start, start + contextSize) to trainIds.copyOfRange(start + 1, start + contextSize + 1)
    }
    val model = TinyLmModel(TinyLmConfig(contextSize, embedDim, numHeads, hiddenDim, vocab.size, numLayers))

    val epochTimes = mutableListOf<Double>()
    var lastLoss = 0.0
    repeat(epochs) {
        var total = 0.0
        val t0 = System.nanoTime()
        for ((input, target) in windows) total += model.trainStep(input, target)
        epochTimes += (System.nanoTime() - t0) / 1e9
        lastLoss = total / windows.size
    }
    println(
        "{\"framework\":\"simbrain-tinylm\",\"vocab\":${vocab.size},\"windows\":${windows.size}," +
                "\"config\":[$contextSize,$embedDim,$numHeads,$hiddenDim,$numLayers],\"epochs\":$epochs," +
                "\"epoch_s\":[${epochTimes.joinToString(",") { "%.3f".format(it) }}],\"final_train_loss\":%.4f}".format(lastLoss)
    )
    exitProcess(0)
}
