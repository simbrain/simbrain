package org.simbrain.custom_sims

import org.simbrain.custom_sims.simulations.*
import org.simbrain.custom_sims.simulations.actor_critic.ActorCritic
import org.simbrain.custom_sims.simulations.behaviorism.ClassicalConditioning
import org.simbrain.custom_sims.simulations.behaviorism.OperantConditioning
import org.simbrain.custom_sims.simulations.behaviorism.OperantWithEnvironment
import org.simbrain.custom_sims.simulations.behaviorism.SimpleOperant
import org.simbrain.custom_sims.simulations.braitenberg.Braitenberg
import org.simbrain.custom_sims.simulations.braitenberg.RandomizedPursuer
import org.simbrain.custom_sims.simulations.cerebellum.Cerebellum
import org.simbrain.custom_sims.simulations.cortex.CortexSimple
import org.simbrain.custom_sims.simulations.cortex.CorticalBranching
import org.simbrain.custom_sims.simulations.creatures.CreaturesSim
import org.simbrain.custom_sims.simulations.edge_of_chaos.EdgeOfChaos
import org.simbrain.custom_sims.simulations.edge_of_chaos.EdgeOfChaosBitStream
import org.simbrain.custom_sims.simulations.hippocampus.Hippocampus
import org.simbrain.custom_sims.simulations.mpfs_som.MpfsSOM
import org.simbrain.custom_sims.simulations.patterns_of_activity.KuramotoOscillators
import org.simbrain.custom_sims.simulations.patterns_of_activity.ModularOscillatoryNetwork
import org.simbrain.custom_sims.simulations.patterns_of_activity.PatternsOfActivity
import org.simbrain.custom_sims.simulations.rl_sim.RL_Sim_Main
import org.simbrain.custom_sims.simulations.sorn.SORN
import org.simbrain.custom_sims.simulations.test.ConvertSim
import org.simbrain.custom_sims.simulations.test.ReadSim
import org.simbrain.custom_sims.simulations.test.lstmBlock
import org.simbrain.util.dir

/**
 * Use this DSL to create the structure of the simulation menu in the Simbrain Desktop.
 *
 * - dir is a JMenu
 * - item is a JMenuItem.
 *
 * The label given is used both as the Menu Item name, and in the command line as the name to
 * use to call them using "run sim" (see build.gradle#runSim). If duplicate labels are used the first one encountered
 * will be run from the command line.
 */
val simulations = dir<Any>("Simulations") {

    dir("Course Materials") {
        dir("Behaviorism") {
            item("Operant With Environment") { OperantWithEnvironment() }
            item("Classical Conditioning") { ClassicalConditioning() }
            item("Operant Conditioning") {OperantConditioning()}
            item("Simple Operant") {SimpleOperant()}
        }
        dir("NLP") {
            // item("NLP") { nlpSim }
            item("1. Introduction") { nlpSim_basic }
            item("2. Geometric Spaces") {nlpSim_geomSpace}
            item("3. Corpus Quality") {nlpSim_corpusQuality}
            item("4. Neural Networks") {nlpSim_neuralNetworks}
        }
    }

    dir("Demos") {
        item("Projection") { projectionSim }
        item("Iris Classifier") { irisClassifier }
        item("Deep Net - Mnist") { deepNetSim }
        item("Spiking Neuron") { spikingNetwork }
        item("Mnist Images") { mnistSim }
        item("LSTM") { lstmBlock() }
    }

    dir("Cognitive Science") {
        dir("Neuroscience") {
            item("Cortex (Kuramoto)") { cortexKuramoto }
            item("Hippocampus") { Hippocampus() }
            item("Cerebellum") { Cerebellum() }
            item("Cortex Simple") {CortexSimple()}
        }
        dir("Cognitive Maps") {
            item("Agent Trails") { kAgentTrails }
            item("ModularOscillatoryNetwork") {ModularOscillatoryNetwork()}
            item("KuramotoOscillators") {KuramotoOscillators()}
        }
        dir("RL") {
            item("RL_Sim_Main") { RL_Sim_Main() }
            item("Actor Critic") { ActorCritic() }
        }
        dir("Agents") {
            item("RandomizedPursuer") { RandomizedPursuer() }
            item("Isopod Simulation") { isopodSim }
            item("Braitenberg") { Braitenberg() }
        }
        dir("Reservoir") {
            item("Binary Reservoir") {binaryReservoir }
            item("Edge Of Chaos") { EdgeOfChaos() }
            item("Edge Of Chaos Bit Stream") { EdgeOfChaosBitStream() }
        }
        item("Object Tracking") { objectTrackingSim }
    }

    dir("Evolution") {
        item("Evolve Avoider") { evolveAvoider }
        item("Evolve Resource Pursuer") { evolveResourcePursuer }
        item("Evolve Thirsty Cows") { evolveCow }
        item("Evolve Grazing Cows") { grazingCows }
        item("Evolve AutoEncoder") { evolveAutoAssociator }
        item("Evolve Modular (Clune et. al.)") { evolveModularity }
        item("Evolve Network (Sandbox)") { evolveNetwork }
        item("Evolve Pursuer") { evolvePursuer }
        item("Evolve Mouse (Sandbox)") { evolveMouse }
        item("Evolve XOR") { evolveXor }
        item("Evolve XOR2") { evolveXor2 }
    }

    dir("Testing") {
        item("Test Sim") { testSim }
        item("Linked Neuron List") { linkedNeuronList }
        dir("Defunct?") {
            item("ConvertSim") { ConvertSim() }
            item("ReadSim") { ReadSim() }
            item("Creatures") { CreaturesSim() }
            item("MPFS") { MpfsSOM() }
            item("PatternsOfActivity") {PatternsOfActivity()}
            item("SORN") { SORN() }
            item("Cortical Branching") {CorticalBranching()}
        }
    }

}

/**
 * Called by build.gradle#runSim when invoking a headless simulation from the command line.
 */
fun main(args: Array<String>) {

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
            .firstOrNull { (key, _) -> key == name } ?:
        throw IllegalArgumentException(
                "Simulation $name not found. A list of possible values are:\n" +
                simulations.items.mapIndexed { index, (name, _) -> "\t$index. $name" }.joinToString("\n")
            )
    } catch (e: NoSuchElementException) {
        throw IllegalArgumentException("Index is out of bound")
    }
    when (sim) {
        is NewSimulation ->  sim.run()
        is Simulation -> sim.run()
    }
}
