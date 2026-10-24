/**
 * Times the Tiny Language Model demo generating text, per etc/benchmarks/HEADED_SPEC.md: a prompt typed into the
 * demo's text world, then one workspace step per generated token, as pressing Play does. The model is first trained
 * for [trainIterations] passes over the text so it generates words rather than noise; timing starts once the context
 * window is full, so every token is a forward pass over the whole window.
 *
 * Modes: headless (no desktop), fast (runAsFastAsPossible, the demo's own setting: the views refresh at frame rate),
 * paced (each step drawn before the next).
 *
 * Usage: GenerationHeadedBenchmarkKt <headless|fast|paced> [tokens=2000] [warmupTokens=200] [trainIterations=20]
 *   [tinyLmSize=24,20,4,30,1]
 *
 * The size is contextSize,embeddingDimension,numHeads,hiddenSize,numLayers. The demo reads its text from
 * simulations/texts, so it runs from the repository root.
 */
package org.simbrain.benchmarks

import kotlinx.coroutines.runBlocking
import org.simbrain.custom_sims.simulations.nlp.tinyLanguageModelSim
import org.simbrain.network.NetworkComponent
import org.simbrain.network.llm.TinyLanguageModel
import org.simbrain.util.updateAction
import org.simbrain.world.textworld.TextWorldComponent
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val mode = args[0]
    require(mode in listOf("headless", "fast", "paced")) { "mode: headless | fast | paced" }
    val tokens = args.getOrNull(1)?.toInt() ?: 2000
    val warmup = args.getOrNull(2)?.toInt() ?: 200
    val trainIterations = args.getOrNull(3)?.toInt() ?: 20
    val size = args.getOrNull(4) ?: "24,20,4,30,1"
    runBlocking { run(mode, tokens, warmup, trainIterations, size) }
    exitProcess(0)
}

private suspend fun run(mode: String, tokens: Int, warmup: Int, trainIterations: Int, size: String) {
    val headed = mode != "headless"
    val t0 = System.nanoTime()
    val (contextSize, embeddingDimension, numHeads, hiddenSize, numLayers) = size.split(",")
    val options = """{"textFile":"casual_texting.txt","contextSize":$contextSize,
        |"embeddingDimension":$embeddingDimension,"numHeads":$numHeads,"hiddenSize":$hiddenSize,
        |"numLayers":$numLayers}""".trimMargin()
    val workspace = openDemo(tinyLanguageModelSim, headed, options)
    val component = workspace.componentList.filterIsInstance<NetworkComponent>().single()
    val model = component.network.allModels.filterIsInstance<TinyLanguageModel>().single()
    val textWorld = workspace.componentList.filterIsInstance<TextWorldComponent>().single().world
    workspace.runAsFastAsPossible = mode != "paced"

    val trainStart = System.nanoTime()
    repeat(trainIterations) { model.trainer.trainOnce() }
    val trainS = (System.nanoTime() - trainStart) / 1e9

    // The opening words of the training text, as a user would type a prompt
    textWorld.text = model.decode(model.corpusTokenIds!!.copyOfRange(0, 8))

    var generated = 0L
    component.network.addUpdateAction(updateAction("Count generated tokens") {
        if (model.generatedToken.isNotEmpty()) generated++
    })

    val counter = if (headed) component.countFrames() else null
    if (headed) settleDesktop()
    val buildS = (System.nanoTime() - t0) / 1e9 - trainS

    val t1 = System.nanoTime()
    workspace.iterateSuspend(warmup)
    val warmupS = (System.nanoTime() - t1) / 1e9
    check(model.contextTokens.size == contextSize.toInt()) { "context window not full after the warm-up" }

    val generated0 = generated
    val frames0 = counter?.frames?.get() ?: 0
    val t2 = System.nanoTime()
    workspace.iterateSuspend(tokens)
    val runS = (System.nanoTime() - t2) / 1e9
    val frames = (counter?.frames?.get() ?: 0) - frames0
    val tokensGenerated = generated - generated0

    println(
        "{\"simulator\":\"simbrain-gui\",\"benchmark\":\"tiny-lm-demo-generation\",\"size\":\"$size\"," +
                "\"mode\":\"$mode\",\"steps\":$tokens,\"tokens\":$tokensGenerated,\"train_iterations\":$trainIterations," +
                "\"build_s\":%.3f,\"train_s\":%.3f,\"warmup_s\":%.3f,\"run_s\":%.4f,".format(buildS, trainS, warmupS, runS) +
                "\"ms_per_token\":%.4f,\"tokens_per_s\":%.1f,\"frames\":$frames,\"frames_per_s\":%.1f,".format(
                    runS / tokensGenerated * 1e3, tokensGenerated / runS, frames / runS
                ) +
                "\"train_loss\":%.4f}".format(model.trainer.lastTrainingError)
    )
}
