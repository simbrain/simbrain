package org.simbrain.custom_sims

import org.simbrain.custom_sims.simulations.*
import org.simbrain.custom_sims.simulations.behaviorism.ClassicalConditioning
import org.simbrain.custom_sims.simulations.behaviorism.OperantConditioning
import org.simbrain.custom_sims.simulations.behaviorism.OperantWithEnvironment
import org.simbrain.custom_sims.simulations.behaviorism.SimpleOperant
import org.simbrain.custom_sims.simulations.braitenberg.RandomizedPursuer
import org.simbrain.custom_sims.simulations.cerebellum.Cerebellum
import org.simbrain.custom_sims.simulations.cortex.CortexSimple
import org.simbrain.custom_sims.simulations.edge_of_chaos.EdgeOfChaos
import org.simbrain.custom_sims.simulations.edge_of_chaos.EdgeOfChaosBitStream
import org.simbrain.custom_sims.simulations.hippocampus.Hippocampus
import org.simbrain.custom_sims.simulations.patterns_of_activity.KuramotoOscillators
import org.simbrain.custom_sims.simulations.patterns_of_activity.cogMap3Objects
import org.simbrain.custom_sims.simulations.test.lstmBlock
import org.simbrain.util.dir

/**
 * Use this DSL to create the structure of the simulation menu in the Simbrain Desktop.
 *
 * `dir` is a JMenu
 * `item` is a JMenuItem.
 *
 * The label given is used both as the Menu Item name, and in the command line as the name to
 * use to call them using "run sim" (see build.gradle#runSim). If duplicate labels are used the first one encountered
 * will be run from the command line.
 */
val simulations = dir<Any>("Simulations") {

    dir("Cognitive Science") {
        dir("Hebbian") {
            item("1. Basic Hebb") { hebbianAssociatorSimple }
            item("2. Feed Forward Associator") { hebbianFeedForward }
            item("3. Recurrent Hebb") { hebbianAssociator }
            item("4. Hopfield") { hopfieldSim }
        }
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
        item("Operant With Environment") { OperantWithEnvironment() }
        item("Classical Conditioning") { ClassicalConditioning() }
        item("Operant Conditioning") { OperantConditioning() }
        item("Simple Operant") { SimpleOperant() }
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
        item("Cortex Simple") { CortexSimple() }
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
        item("Projection") { projectionSim }
        // item("SRN - Temporal XOR") { srnXORSim }
        item("Recurrent network") { recurrentProjection }
        item("Iris Classifier") { irisClassifier }
        item("Deep Net - Mnist") { deepNetSim }
        item("Spiking Neuron") { spikingNetwork }
        item("Mnist Images") { mnistSim }
        item("Image World") { imageWorldSim }
        item("LSTM") { lstmBlock() }
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
