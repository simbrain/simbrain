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

    // TODO: Finish classifying
    dir("Imported") {
        item("LSTM") { lstmBlock() }
        item("MPFS") { MpfsSOM() }
        item("Braitenberg") { Braitenberg() }
        item("Edge Of Chaos") { EdgeOfChaos() }
        item("Edge Of Chaos Bit Stream") { EdgeOfChaosBitStream() }
        item("Hippocampus") { Hippocampus() }
        item("RL_Sim_Main") { RL_Sim_Main() }
        item("Cerebellum") { Cerebellum() }
        item("Creatures") { CreaturesSim() }
        item("Actor Critic") { ActorCritic() }
        item("Operant With Environment") { OperantWithEnvironment() }
        item("Classical Conditioning") { ClassicalConditioning() }
        item("Operant Conditioning") {OperantConditioning()}
        item("Simple Operant") {SimpleOperant()}
        item("Cortical Branching") {CorticalBranching()}
        item("Cortex Simple") {CortexSimple()}
        item("ModularOscillatoryNetwork") {ModularOscillatoryNetwork()}
        item("RandomizedPursuer") { RandomizedPursuer() }
        item("PatternsOfActivity") {PatternsOfActivity()}
        item("KuramotoOscillators") {KuramotoOscillators()}
        item("SORN") { SORN() }
        item("lstmBlock") {lstmBlock()}
        item("ConvertSim") { ConvertSim() }
        item("ReadSim") { ReadSim() }
    }

    dir("Demos") {
        item("Test Sim") { testSim }
        item("Linked Neuron List") { linkedNeuronList }
        item("Smile Classifier") { smileSim }
        item("Projection") { projectionSim }
        item("NLP") { nlpSim }
        item("Deep Net") { deepNetSim }
        item("Spiking Neuron") { spikingNetwork }
        item("Mnist Images") { mnistSim }
        item("Agent Trails") { kAgentTrails }
    }

    dir("Cognitive Science") {
        item("Cortex (PCI)") { cortexPCI }
        item("Object Tracking") { objectTrackingSim }
        item("Binary Reservoir") {binaryReservoir }
        item("Isopod Simulation") { isopodSim }
    }

    dir("Evolution") {
        item("Evolve Avoider") { evolveAvoider }
        item("Evolve Resource Pursuer") { evolveResourcePursuer }
        item("Multi Agent Resource Pursuer") { evolveMultiAgentResourcePursuer }
        item("Evolve AutoEncoder") { evolveAutoAssociator }
        item("Evolve Modular (Clune et. al.)") { evolveModularity }
        item("Evolve Network (Sandbox)") { evolveNetwork }
        item("Evolve Pursuer") { evolvePursuer }
        item("Evolve Mouse (Sandbox)") { evolveMouse }
        item("Evolve XOR") { evolveXor }
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
