package org.simbrain.custom_sims.simulations.nettalk

import org.simbrain.custom_sims.*
import org.simbrain.network.NetworkComponent
import org.simbrain.network.subnetworks.BackpropNetwork
import org.simbrain.network.trainers.TrainingDataset
import org.simbrain.util.nettalk.NettalkEncoder
import org.simbrain.util.nettalk.NettalkPhonology
import org.simbrain.util.nettalk.loadNettalkCorpus
import org.simbrain.util.place
import org.simbrain.workspace.Workspace
import org.simbrain.world.nettalk.NetTalkComponent

/**
 * NETtalk wired together as two loosely-coupled workspace components:
 *
 *   NetTalkComponent  ──currentWindow─▶  NetworkComponent (BackpropNetwork)
 *   NetTalkComponent  ◀─outputArray──   NetworkComponent
 *
 * Speech is synthesized by NetTalk's own embedded `PhonemeSynthesizer` (its settings
 * appear in the NetTalk panel) — no separate SoundWorld component required.
 *
 * Train the network via its standard right-click → `Edit/Train Backprop...` dialog.
 * Use the workspace play button to read through the text. Audio mode (per-word vs
 * per-letter) is set on the NetTalk panel.
 */
val nettalkComponentSim = newSim("nettalk_component") {

    workspace.clearWorkspace()

    val numWords = 1000
    val hiddenSize = 80
    val windowSize = 7

    val nettalkComp = addNetTalk("NETtalk")
    val nettalk = nettalkComp.nettalk

    val networkComponent = addNetworkComponent("Network")
    val net = networkComponent.network
    val encoder = NettalkEncoder(windowSize)

    val bp = BackpropNetwork(intArrayOf(encoder.inputSize, hiddenSize, encoder.outputSize)).apply {
        trainerConfig.learningRate = 0.01
        trainerConfig.testConfiguration.enabled = false
    }
    net.addNetworkModels(bp)

    // Pre-populate the training set using the same sampling as the original NETtalk sim.
    val corpus = loadNettalkCorpus(byFrequency = true)
        .filter { it.isRegular }
        .take(numWords)
    val tensors = encoder.encodeAsContinuousText(corpus, gap = 1, repeats = 3, shuffleSeed = 42L)
    bp.trainingSet = TrainingDataset(
        inputs = tensors.inputs.map { it.toMutableList() }.toMutableList(),
        targets = tensors.targets.map { it.toMutableList() }.toMutableList(),
        inputRowNames = tensors.rowLabels,
        targetRowNames = tensors.rowLabels,
        targetColumnNames = NettalkPhonology.featureNames + NettalkPhonology.stressNames
    )
    // Replace BackpropNetwork's default randomly-generated testing set with an empty one of
    // the right shape — the NETtalk demo doesn't use validation data.
    bp.testingSet = TrainingDataset(
        inputs = mutableListOf(),
        targets = mutableListOf(),
        inputSize = encoder.inputSize,
        targetSize = encoder.outputSize
    )
    bp.initBiases()
    bp.initWeights()
    bp.inputLayer.gridMode = true
    bp.outputLayer.gridMode = true

    wireNetTalkCouplings(workspace)

    withGui {
        place(nettalkComp, 0, 0, 600, 600)
        place(networkComponent, 610, 0, 700, 600)
    }

    addSidebarInfo(NETTALK_COMPONENT_SIDEBAR)
}.registerReopenFunction { workspace ->
    // Re-establishes the NetTalk ↔ Network couplings after deserialization. The
    // network's trained weights and the NetTalk component's settings are restored
    // from the saved workspace; only the cross-component wiring + sidebar info need
    // rebuilding (the latter is workspace-level state that isn't persisted to disk).
    wireNetTalkCouplings(workspace)
    addSidebarInfo(NETTALK_COMPONENT_SIDEBAR)
}

private val NETTALK_COMPONENT_SIDEBAR: String = """
    # NETtalk (component edition)

    Same NETtalk demo as the simpler `NETtalk` simulation, but built from a dedicated
    `NetTalkComponent` wired by couplings to a separate `NetworkComponent`. NetTalk owns
    its own embedded `PhonemeSynthesizer` — the synth settings (voice, speed, pitch,
    amplitude) appear at the bottom of the NetTalk panel. The training set is pre-loaded
    on the network so you can train it via its standard right-click → `Edit/Train
    Backprop...` dialog.

    # Coupling Topology

    - `NetTalk.currentWindow` → `Network.inputLayer.activationArray` (203-D one-hot window)
    - `Network.outputLayer.activationArray` → `NetTalk.setNetworkOutput` (26-D features)

    Couplings introduce a one-tick latency per hop, so the green "currently speaking"
    highlight follows actual audio while the yellow input cursor is one or two letters
    ahead.

    # What to Do

    1. Train the network: right-click the `Network` component → `Edit/Train Backprop...`,
       then play. Mean error should drop steadily.
    2. Press the workspace play button; the cursor in the NETtalk panel advances through
       the text and the synthesizer speaks one word (or letter, depending on `Audio mode`)
       at a time.
    3. Toggle `Audio mode` on the NETtalk panel between `PER_WORD` and `PER_LETTER`. The
       currently-spoken word is highlighted in green in the reading text.
    4. Edit the reading text directly in the NETtalk panel to see how the network
       pronounces other passages.

    # Saving and Reopening

    Use `File → Save Workspace` to write the trained network and NetTalk state to a `.zip`.
    Reopening the file restores both components' state; this simulation's reopen handler
    re-establishes the couplings and re-renders this sidebar info.

    # Audio Pipeline

    `PhonemeSynthesizer` calls into bundled `libespeak-ng` directly via JNA: each utterance
    flows `text → espeak_Synth → callback(short[]) → SourceDataLine`. Calls to `speakPhonemes`
    enqueue jobs on a rendezvous channel, so the producer (the workspace updater) blocks
    when audio is in flight — this naturally paces iteration with audio playback.

    # Credits

    [Jeff Yoshimi](https://jeffyoshimi.net/index.html)
""".trimIndent()

/**
 * Connect the first NetTalk and Network components in the workspace via the standard
 * NETtalk couplings: window → input layer, output layer → network-output consumer.
 *
 * Used both during the initial sim build and on reopen.
 */
fun SimulationScope.wireNetTalkCouplings(workspace: Workspace) {
    val nettalkComp = workspace.componentList.filterIsInstance<NetTalkComponent>().firstOrNull()
        ?: error("No NetTalkComponent found in workspace.")
    val networkComp = workspace.componentList.filterIsInstance<NetworkComponent>().firstOrNull()
        ?: error("No NetworkComponent found in workspace.")
    val nettalk = nettalkComp.nettalk
    val bp = networkComp.network.allModelsDeep.filterIsInstance<BackpropNetwork>().firstOrNull()
        ?: error("No BackpropNetwork found in the network component.")
    with(workspace.couplingManager) {
        nettalk.getProducer(nettalk::currentWindow) couple bp.inputLayer.getConsumer(bp.inputLayer::setActivations)
        bp.outputLayer.getProducer(bp.outputLayer::activationArray) couple nettalk.getConsumer(nettalk::setNetworkOutput)
    }
}
