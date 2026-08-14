package org.simbrain.custom_sims

import org.simbrain.custom_sims.simulations.*
import org.simbrain.custom_sims.simulations.backprop.cnnMNIST
import org.simbrain.custom_sims.simulations.behaviorism.classicalConditioning
import org.simbrain.custom_sims.simulations.behaviorism.operantWithEnvironment
import org.simbrain.custom_sims.simulations.behaviorism.simpleOperant
import org.simbrain.custom_sims.simulations.braitenberg.avoider
import org.simbrain.custom_sims.simulations.braitenberg.braitenbergGame
import org.simbrain.custom_sims.simulations.braitenberg.braitenbergSim
import org.simbrain.custom_sims.simulations.braitenberg.pursuer
import org.simbrain.custom_sims.simulations.cogsci.landmarkCognitiveMap
import org.simbrain.custom_sims.simulations.demos.*
import org.simbrain.custom_sims.simulations.dynamical_systems.lorenzSystem
import org.simbrain.custom_sims.simulations.dynamical_systems.lorenzSystemSimbrain
import org.simbrain.custom_sims.simulations.edge_of_chaos.edgeOfChaos
import org.simbrain.custom_sims.simulations.edge_of_chaos.edgeOfChaosBitStream
import org.simbrain.custom_sims.simulations.imageworld.attentionAsGain
import org.simbrain.custom_sims.simulations.imageworld.cnnObjectDetector
import org.simbrain.custom_sims.simulations.nettalk.nettalkComponentSim
import org.simbrain.custom_sims.simulations.nettalk.synthesizerDemo
import org.simbrain.custom_sims.simulations.neuroscience.corticalLayers
import org.simbrain.custom_sims.simulations.neuroscience.excitatoryInhibitoryBalance
import org.simbrain.custom_sims.simulations.neuroscience.nematodeThermotaxis
import org.simbrain.custom_sims.simulations.neuroscience.spikingNetworkSimulation
import org.simbrain.custom_sims.simulations.nlp.lfm2LanguageModel
import org.simbrain.custom_sims.simulations.nlp.tinyLanguageModel
import org.simbrain.custom_sims.simulations.patterns_of_activity.cogMap3Objects
import org.simbrain.custom_sims.simulations.patterns_of_activity.iacJetsSharks5People
import org.simbrain.custom_sims.simulations.psychology.*
import org.simbrain.custom_sims.simulations.rl.braitenbergRL
import org.simbrain.custom_sims.simulations.rl.braitenbergRLPrograms
import org.simbrain.util.StructureDir
import org.simbrain.util.dir

/**
 * Use this DSL to create the structure of the simulation menu in the Simbrain Desktop.
 *
 * `dir` is a JMenu
 * `item` is a JMenuItem.
 *
 * Note that items in a dir appear in the order given unless `alphabetical` is set to true. See [StructureDir].
 *
 * The label given is used both as the Menu Item name, and in the command line as the name to
 * use to call them using "run sim" (see build.gradle#runSim). If duplicate labels are used the first one encountered
 * will be run from the command line.
 */
val simulations = dir("Simulations", alphabetical = true ) {

    // dir("Hebbian") {
    //     item("1. Basic Hebb") { hebbianAssociatorSimple }
    //     item("2. Feed Forward Associator") { hebbianFeedForward }
    //     item("3. Recurrent Hebb") { hebbianAssociator }
    // }

    dir("Backprop") {
        item("XOR") { xorSim }
        item("Three Object Detector") { threeObjectDetector }
        item("Three layer auto-encoder") { backpropAutoEncoder }
        item("Feed-Forward MNIST") { tinyMNIST }
        item("CNN MNIST") { cnnMNIST }
        // item("Delayed recall (BPTT)") { bpttDelayedRecall }
        // item("Curriculum (BPTT)") { bpttCurriculum }
    }

    dir("Braitenberg") {
        item("Pursuer") { pursuer }
        item("Avoider") { avoider }
        item("Braitenberg game") { braitenbergGame }
        item("Two Braitenberg vehicles") { braitenbergSim }
        item("Isopod simulation") { isopodSim }
    }

    dir("NPC Behaviors") {
        item("NPC Basic Demo") { npcSteeringDemo }
    }

    dir("Reservoir networks") {
        item("Edge of chaos bit stream") { edgeOfChaosBitStream }
        item("Edge of chaos embodied") { edgeOfChaos }
        item("Binary reservoir") { binaryReservoir }
        // item("Pattern Completion") { allostaticPatternCompletion }
        item("Object tracking reservoir") { objectTrackingSim }
    }

    dir("Psychology") {
        item("Categorical perception", beta = true) { categoricalPerception }
        item("Heat-cold illusion") { heatColdSim }
        item("IAC Jets Sharks 5 people") { iacJetsSharks5People }
        item("Mouse and eye tracking") { spiveyNet }
        item("Burgess-Hitch positional context memory", beta = true) { serialOrderContextMemory }
        item("Botvinick-Plaut serial recall (BPTT)") { botvinickPlautSerialRecall }
        item("Temporal attention network", beta = true) { denisonNet }
    }

    dir("Behaviorism") {
        item("Simple operant") { simpleOperant }
        item("Classical conditioning") { classicalConditioning }
        item("Operant with environment") { operantWithEnvironment }
    }

    dir("Cognitive maps", alphabetical = true) {
        item("Landmark cognitive map", beta = true) { landmarkCognitiveMap }
        item("Agent trails") { kAgentTrails }
        item("Field image (sensors)") { fieldImageDemo }
        item("Field image (place cells)") { fieldImagePlaceCellsDemo }
        item("Three object recurrent") { cogMap3Objects }
    }

    dir("Language") {
        item("Basic word embeddings") { nlpSimBasic }
        item("Synthesizer demo") { synthesizerDemo }
        item("NETtalk") { nettalkComponentSim }
        item("Next-word prediction (SRN)") { srnElmanSentences }
        //item("Tiny language model") { tinyLanguageModelFF }
        item("Tiny Language Model") { tinyLanguageModel }
        // The "(Beta)" prefix is deliberate (per Jeff): discoverable without the beta opt-in,
        // while still flagged as beta.
        item("(Beta) Pretrained Language Model (LFM2.5)") { lfm2LanguageModel }
    }

    dir("Neuroscience") {
        item("Spiking neuron") { spikingNeuron }
        item("Spiking neurons two inputs") { spikingNeuronTwoInputs }
        item("Spike responders") { spikeResponderSim }
        item("Spike responders (array)") { spikeResponderSimArray }
        item("STDP") { stdpSim }
        item("Spiking Network") { spikingNetworkSimulation }
        item("E/I Balance") { excitatoryInhibitoryBalance }
        item("Nematode simulation") { nematodeThermotaxis }
        item("Cortical layers", beta = true) { corticalLayers }
         //item("Cortical areas") { cortexKuramoto }
    }

    dir("Evolution") {
        // item("Evolve Avoider") { evolveAvoider }
        // item("Evolve Thirsty Cows") { evolveCow }
        item("Evolve grazing cows") { grazingCows }
        item("Evolve mouse pursuer", beta = true) { evolveMousePursuer }
        // item("Evolve AutoEncoder") { evolveAutoAssociator }
        // item("Evolve Modular (Clune et. al.)") { evolveModularity }
        item("Evolve network") { evolveNetwork }
        item("Evolve resource pursuer") { evolveResourcePursuer }
        // item("Evolve Mouse (Sandbox)") { evolveMouse }
        // item("Evolve XOR") { evolveXor }
        item("Evolve XOR") { evolveXor }
    }

    dir("Competitive") {
        item("Competitive network (simple)") { competitiveSim }
        item("Competitive grid network") { competitiveGridSim }
        //item("Competitive image network") { competitiveImageSim }
        item("SOM network") { SOMSim }
        item("SOM Network (Smell)") { somNetSmells }
    }

    dir("Dynamical systems") {
        item("Lorenz attractor") { lorenzSystem }
        item("Lorenz attractor (Pure Simbrain)") { lorenzSystemSimbrain }
    }

    //dir("Leabra") {
    //    item("Point neuron") { pointNeuronSim }
    //}

    dir("Hopfield and Boltzmann") {
        item("Hopfield patterns") { hopfieldPatterns }
        item("Discrete Hopfield") { discreteHopfieldSim }
        item("Continuous Hopfield") { hopfieldSimContinuous }
        item("Restricted Boltzmann machine") { rbmSim }
        item("Room schema") { roomSchemaSim }
        item("Modern Hopfield Network (Beta)") { modernHopfieldSim }
    }

    dir("Machine learning") {
        item("Iris classifier") { irisClassifier }
    }

    dir("Recurrent networks") {
        item("Recurrent net") { recurrentProjection }
        item("Recurrent net (array)") { recurrentNetArrayBased }
    }

    dir("Reinforcement learning") {
        item("Actor critic") { actorCritic }
        item("Braitenberg RL") { braitenbergRL }
        item("Braitenberg Program Learning", beta = true) { braitenbergRLPrograms }
    }

    dir("Vision") {
        item("Simple drawings (10 x 10)") { simpleImageWorld }
        //item("Photo album (100 x 100)") { photoAlbumExample }
        //item("3D View Example") { view3dDemo }
        item("3D Navigation and Image World") { view3dNavigationPipelineDemo }
        item("3D Object Recognition") { objectRecognition3D }
        item("3D Navigation and Lateral Inhibition") { view3dNavigationLateralInhibitionDemo }
        item("Attention as gain", beta = true) { attentionAsGain }
        //item("CNN Demo") { cnnDemo } // Develop into 3d demo
        item("CNN Simple Demo") { cnnSimpleLineDetector }
        item("CNN Object Recognition (100 x 100)") { cnnObjectDetector }
    }

}

/**
 * Called by build.gradle#runSim when invoking a headless simulation from the command line.
 */
suspend fun main(args: Array<String>) {

    if (args.isEmpty()) throw IllegalArgumentException(
        "Please supply a simulation name or an index. A list of possible values are:\n" +
                simulations.items.mapIndexed { index, (name, _) ->
                    "\t$index. $name"
                }.joinToString("\n")
    )

    val (_, sim) = try {
        val index = args[0].toInt()
        simulations.items.drop(index).first()
    } catch (e: NumberFormatException) {
        val name = args[0]
        simulations.items
            .firstOrNull { (key, _) -> key == name } ?: throw IllegalArgumentException(
            "Simulation $name not found. A list of possible values are:\n" +
                    simulations.items.mapIndexed { index, (name, _) -> "\t$index. $name" }.joinToString("\n")
        )
    } catch (e: NoSuchElementException) {
        throw IllegalArgumentException("Index is out of bound")
    }
    sim.run(optionString = if(args.size > 1) args[1] else null)
}
