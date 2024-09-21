package org.simbrain.custom_sims

import org.simbrain.custom_sims.simulations.*
import org.simbrain.custom_sims.simulations.behaviorism.ClassicalConditioning
import org.simbrain.custom_sims.simulations.behaviorism.OperantConditioning
import org.simbrain.custom_sims.simulations.behaviorism.SimpleOperant
import org.simbrain.custom_sims.simulations.behaviorism.operantWithEnvironment
import org.simbrain.custom_sims.simulations.braitenberg.RandomizedPursuer
import org.simbrain.custom_sims.simulations.demos.competitiveSim
import org.simbrain.custom_sims.simulations.edge_of_chaos.EdgeOfChaos
import org.simbrain.custom_sims.simulations.edge_of_chaos.EdgeOfChaosBitStream
import org.simbrain.custom_sims.simulations.neuroscience.cortexSimple
import org.simbrain.custom_sims.simulations.nlp.tinyLanguageModel
import org.simbrain.custom_sims.simulations.patterns_of_activity.KuramotoOscillators
import org.simbrain.custom_sims.simulations.patterns_of_activity.cogMap3Objects
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

    dir("Reinforcement Learning") {
        item("Actor Critic") { actorCritic }
    }

    dir("Backprop") {
        item("Three Layer") { backpropSim }
        item("Softmax") { softmaxSim }
    }

    dir("Braitenberg") {
        item("Isopod Simulation") { isopodSim }
        item("Braitenberg") { braitenbergSim }
    }

    dir("Reservoir Networks") {
        item("Edge Of Chaos Bit Stream") { EdgeOfChaosBitStream() }
        item("Edge Of Chaos Embodied") { EdgeOfChaos() }
        item("Binary Reservoir") { binaryReservoir }
        // item("Pattern Completion") { allostaticPatternCompletion }
        item("Object Tracking Reservoir") { objectTrackingSim }
    }

    dir("Behaviorism") {
        item("Simple Operant") { SimpleOperant() }
        item("Classical Conditioning") { ClassicalConditioning() }
        item("Operant Conditioning") { OperantConditioning() }
        item("Operant With Environment") { operantWithEnvironment }
    }

    dir("Cognitive Maps") {
        item("Agent Trails") { kAgentTrails }
        //item("RandomizedPursuer") { RandomizedPursuer() }
        item("Generic 3 objects") { cogMap3Objects }
        // item("ModularOscillatoryNetwork") {ModularOscillatoryNetwork()}
        //item("KuramotoOscillators") { KuramotoOscillators() }
    }

    dir("Language Models") {
        item("Basic Word Embeddings") { nlpSimBasic }
        item("Next-Word Prediction") { srnElmanSentences }
        item("Tiny Language Model") { tinyLanguageModel }
    }

    dir("Neuroscience") {
        item("Spiking Neuron") { spikingNeuron }
        item("Spike Responders") { spikeResponderSim }
        item("Spike Responders (Array)") { spikeResponderSimArray }
        item("Cortical areas") { cortexKuramoto }
        // item("Hippocampus") { Hippocampus() }
        // item("Cerebellum") { Cerebellum() }
        item("Cortical layers") { cortexSimple }
    }

    dir("Evolution") {
        // item("Evolve Avoider") { evolveAvoider }
        // item("Evolve Thirsty Cows") { evolveCow }
        item("Evolve Grazing Cows") { grazingCows }
        // item("Evolve AutoEncoder") { evolveAutoAssociator }
        // item("Evolve Modular (Clune et. al.)") { evolveModularity }
        item("Evolve Network") { evolveNetwork }
        item("Evolve Resource Pursuer") { evolveResourcePursuer }
        // item("Evolve Mouse (Sandbox)") { evolveMouse }
        // item("Evolve XOR") { evolveXor }
        item("Evolve XOR") { evolveXor }
    }

    dir("Competitive") {
        item("Competitive Network (Simple)") { competitiveSim }
        item("Competitive Grid Network") { competitiveGridSim }
        item("Competitive Image Network") { competitiveImageSim }
        item("SOM Network") { SOMSim }
    }

    dir("Leabra") {
        item("Point neuron") { pointNeuronSim }
    }

    dir("Hopfield and Boltzmann") {
        item("Hopfield") { hopfieldSim }
        item("Restricted Boltzmann Machine") { rbmSim }
        item("Room Schema") { roomSchemaSim }
    }

    dir("Machine Learning") {
        item("Iris Classifier") { irisClassifier }
    }

    dir("Projection") {
        item("PCA Projection") { projectionSim }
    }

    dir("Recurrent Networks") {
        item("Basic recurrent net") { recurrentProjection }
    }

    // dir("Other Demos") {
    //     item("SRN - Temporal XOR") { srnXORSim }
    //     item("Deep Net - Mnist") { deepNetSim }
    //     item("Mnist Images") { mnistSim }
    //     item("LSTM") { lstmBlock() }
    // }

    dir("Image World") {
        item("Simple drawings (10 x 10)") { simpleImageWorld }
        item("Photo album (100 x 100)") { photoAlbumExample }
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
