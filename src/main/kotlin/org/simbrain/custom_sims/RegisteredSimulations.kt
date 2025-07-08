package org.simbrain.custom_sims

import org.simbrain.custom_sims.simulations.*
import org.simbrain.custom_sims.simulations.backprop.activationSequenceThreeLayer
import org.simbrain.custom_sims.simulations.backprop.activationSequenceTwoLayer
import org.simbrain.custom_sims.simulations.behaviorism.classicalConditioning
import org.simbrain.custom_sims.simulations.behaviorism.operantWithEnvironment
import org.simbrain.custom_sims.simulations.behaviorism.simpleOperant
import org.simbrain.custom_sims.simulations.braitenberg.avoider
import org.simbrain.custom_sims.simulations.braitenberg.braitenbergGame
import org.simbrain.custom_sims.simulations.braitenberg.braitenbergSim
import org.simbrain.custom_sims.simulations.braitenberg.pursuer
import org.simbrain.custom_sims.simulations.demos.competitiveSim
import org.simbrain.custom_sims.simulations.edge_of_chaos.EdgeOfChaos
import org.simbrain.custom_sims.simulations.edge_of_chaos.EdgeOfChaosBitStream
import org.simbrain.custom_sims.simulations.neuroscience.cortexLayers
import org.simbrain.custom_sims.simulations.neuroscience.spikingNetworkSimulation
import org.simbrain.custom_sims.simulations.nlp.tinyLanguageModel
import org.simbrain.custom_sims.simulations.patterns_of_activity.cogMap3Objects
import org.simbrain.custom_sims.simulations.patterns_of_activity.iacJetsSharks5People
import org.simbrain.custom_sims.simulations.rl.braitenbergRL
import org.simbrain.util.StructureDir
import org.simbrain.util.dir

/**
 * Use this DSL to create the structure of the simulation menu in the Simbrain Desktop.
 *
 * `dir` is a JMenu
 * `item` is a JMenuItem.
 *
 * Note that items in a dir appear in the order given unless `alphabetize` is set to true. See [StructureDir].
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
        item("Tiny MNIST") { tinyMNIST }
    }

    dir("Braitenberg") {
        item("Pursuer") { pursuer }
        item("Avoider") { avoider }
        item("Braitenberg game") { braitenbergGame }
        item("Two Braitenberg vehicles") { braitenbergSim }
        item("Isopod simulation") { isopodSim }
    }

    dir("Reservoir networks") {
        item("Edge of chaos bit stream") { EdgeOfChaosBitStream() }
        item("Edge of chaos embodied") { EdgeOfChaos() }
        item("Binary reservoir") { binaryReservoir }
        // item("Pattern Completion") { allostaticPatternCompletion }
        item("Object tracking reservoir") { objectTrackingSim }
    }

    dir("Behaviorism") {
        item("Simple operant") { simpleOperant }
        item("Classical conditioning") { classicalConditioning }
        //item("Operant conditioning") { OperantConditioning() }
        item("Operant with environment") { operantWithEnvironment }
    }

    dir("Cognitive maps") {
        item("Agent trails") { kAgentTrails }
        //item("RandomizedPursuer") { RandomizedPursuer() }
        item("Three object recurrent") { cogMap3Objects }
        // item("ModularOscillatoryNetwork") {ModularOscillatoryNetwork()}
        //item("KuramotoOscillators") { KuramotoOscillators() }
    }

    dir("Language models") {
        item("Basic word embeddings") { nlpSimBasic }
        item("Next-word prediction") { srnElmanSentences }
        //item("Tiny language model") { tinyLanguageModelFF }
        item("Tiny language model") { tinyLanguageModel }
    }

    dir("Neuroscience") {
        item("Spiking neuron") { spikingNeuron }
        item("Spiking neurons two inputs") { spikingNeuronTwoInputs }
        item("Spike responders") { spikeResponderSim }
        item("Spike responders (array)") { spikeResponderSimArray }
        item("STDP") { stdpSim }
        // item("Hippocampus") { Hippocampus() }
        // item("Cerebellum") { Cerebellum() }
        item("Spiking Network") { spikingNetworkSimulation }
        item("Cortical layers") { cortexLayers }
        //item("Cortical areas") { cortexKuramoto }
    }

    dir("Evolution") {
        // item("Evolve Avoider") { evolveAvoider }
        // item("Evolve Thirsty Cows") { evolveCow }
        item("Evolve grazing cows") { grazingCows }
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
        item("Competitive image network") { competitiveImageSim }
        item("SOM network") { SOMSim }
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
    }

    dir("Machine learning") {
        item("Iris classifier") { irisClassifier }
    }


    dir("Recurrent networks") {
        item("Recurrent net") { recurrentProjection }
        item("Recurrent net (array)") { recurrentNetArrayBased }
        item("IAC Jets Sharks 5 People") { iacJetsSharks5People }
        item("Spivey net") { spiveyNet }
    }

    dir("Reinforcement learning") {
        item("Braitenberg RL") { braitenbergRL }
        item("Actor critic") { actorCritic }
    }

    // dir("Other Demos") {
    //     item("SRN - Temporal XOR") { srnXORSim }
    //     item("Deep Net - Mnist") { deepNetSim }
    //     item("Mnist Images") { mnistSim }
    //     item("LSTM") { lstmBlock() }
    // }

    dir("Image world") {
        item("Simple drawings (10 x 10)") { simpleImageWorld }
        item("Photo album (100 x 100)") { photoAlbumExample }
    }

    dir("Temp") {
        item("Activation sequence (2 layers)") { activationSequenceTwoLayer }
        item("Activation sequence (3 layers)") { activationSequenceThreeLayer }
    }

    //dir("Testing") {
    //    // item("Test Sim") { testSim }
    //    //item("Linked Neuron List") { linkedNeuronList }
    //    // dir("Defunct?") {
    //    //     item("ConvertSim") { ConvertSim() }
    //    //     item("Creatures") { CreaturesSim() }
    //    //     item("MPFS") { MpfsSOM() }
    //    //     item("PatternsOfActivity") { PatternsOfActivity() }
    //    //     item("SORN") { SORN() }
    //    //     item("Cortical Branching") { CorticalBranching() }
    //    // }
    //}

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
    when (sim) {
        is NewSimulation -> sim.run(optionString = if(args.size > 1) args[1] else null)
        is Simulation -> sim.run()
    }
}
