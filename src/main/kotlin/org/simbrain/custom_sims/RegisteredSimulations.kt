package org.simbrain.custom_sims

import org.simbrain.custom_sims.simulations.*
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
val simulations = dir<NewSimulation>("Simulations") {

    // This supersedes RegisteredSimulation.java. Will have to move that stuff here.

    dir("Demos") {
        item("Simple Network") { testSim }
        item("Spiking Network") { spikingNetwork }
        item("Mnist Images") { mnistSim }
        item("Agent Trails") { kAgentTrails }
    }

    dir("Evolution") {
        item("Evolve Avoider") { evolveAvoider }
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
    sim.run()
}
