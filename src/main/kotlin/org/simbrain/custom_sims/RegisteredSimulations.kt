package org.simbrain.custom_sims

import org.simbrain.custom_sims.simulations.*
import org.simbrain.custom_sims.simulations.behaviorism.ClassicalConditioning
import org.simbrain.custom_sims.simulations.behaviorism.OperantConditioning
import org.simbrain.custom_sims.simulations.behaviorism.OperantWithEnvironment
import org.simbrain.custom_sims.simulations.behaviorism.SimpleOperant
import org.simbrain.custom_sims.simulations.braitenberg.RandomizedPursuer
import org.simbrain.custom_sims.simulations.cerebellum.Cerebellum
import org.simbrain.custom_sims.simulations.cortex.cortexSimple
import org.simbrain.custom_sims.simulations.edge_of_chaos.EdgeOfChaos
import org.simbrain.custom_sims.simulations.edge_of_chaos.EdgeOfChaosBitStream
import org.simbrain.custom_sims.simulations.hippocampus.Hippocampus
import org.simbrain.custom_sims.simulations.patterns_of_activity.KuramotoOscillators
import org.simbrain.custom_sims.simulations.patterns_of_activity.cogMap3Objects
import org.simbrain.custom_sims.simulations.test.lstmBlock
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
val simulations = dir("Simulations") {

    dir("Cognitive Science") {
        // dir("Hebbian") {
        //     item("1. Basic Hebb") { hebbianAssociatorSimple }
        //     item("2. Feed Forward Associator") { hebbianFeedForward }
        //     item("3. Recurrent Hebb") { hebbianAssociator }
        // }
        dir("RL") {
            item("Actor Critic") { actorCritic }
        }
        dir("Braitenberg") {
            item("Isopod Simulation") { isopodSim }
            item("Braitenberg") { braitenbergSim }
        }
        dir("Reservoir") {
            item("Binary Reservoir") { binaryReservoir }
            item("Edge Of Chaos") { EdgeOfChaos() }
            item("Edge Of Chaos Bit Stream") { EdgeOfChaosBitStream() }
            item("Pattern Completion") { allostaticPatternCompletion }
            item("Object Tracking") { objectTrackingSim }
        }
    }


    dir("Behaviorism") {
        item("Simple Operant") { SimpleOperant() }
        item("Classical Conditioning") { ClassicalConditioning() }
        item("Operant Conditioning") { OperantConditioning() }
        item("Operant With Environment") { OperantWithEnvironment() }
    }

    dir("Cognitive Maps") {
        item("Agent Trails") { kAgentTrails }
        item("RandomizedPursuer") { RandomizedPursuer() }
        item("Generic 3 objects") { cogMap3Objects }
        // item("ModularOscillatoryNetwork") {ModularOscillatoryNetwork()}
        item("KuramotoOscillators") { KuramotoOscillators() }
    }

    dir("NLP") {
        item("Basic Word Embeddings") { nlpSimBasic }
        item("Next-Word Prediction") { srnElmanSentences }
    }

    dir("Neuroscience") {
        item("Cortex (Kuramoto)") { cortexKuramoto }
        item("Hippocampus") { Hippocampus() }
        item("Cerebellum") { Cerebellum() }
        item("Cortex Simple") { cortexSimple }
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

    // TODO: Disaggregate this into more meaningful submenus
    dir("Demos") {
        item("Competitive Image Network") { competitiveImageSim }
        item("Competitive Grid Network") { competitiveGridSim }
        item("Competitive Network (Simple)") { competitiveSim }
        item("Restricted Boltzmann Machine") { rbmSim }
        item("Room Schema") { roomSchemaSim }
        item("Projection") { projectionSim }
        item("Hopfield") { hopfieldSim }
        // item("SRN - Temporal XOR") { srnXORSim }
        item("Recurrent network") { recurrentProjection }
        item("SOM Network") { SOMSim }
        item("Iris Classifier") { irisClassifier }
        // item("Deep Net - Mnist") { deepNetSim }
        item("Spiking Neuron") { spikingNetwork }
        // item("Mnist Images") { mnistSim }
        item("LSTM") { lstmBlock() }
    }

    dir("Image World") {
        item("1. Simple drawings (10 x 10)") { simpleImageWorld }
        item("2. Photo album (100 x 100)") { photoAlbumExample }
    }

    dir("Testing") {
        // item("Test Sim") { testSim }
        item("Linked Neuron List") { linkedNeuronList }
        // dir("Defunct?") {
        //     item("ConvertSim") { ConvertSim() }
        //     item("Creatures") { CreaturesSim() }
        //     item("MPFS") { MpfsSOM() }
        //     item("PatternsOfActivity") { PatternsOfActivity() }
        //     item("SORN") { SORN() }
        //     item("Cortical Branching") { CorticalBranching() }
        // }
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
    when (sim) {
        is NewSimulation -> sim.run(optionString = if(args.size > 1) args[1] else null)
        is Simulation -> sim.run()
    }
}
