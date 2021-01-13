package org.simbrain.custom_sims

import org.simbrain.custom_sims.simulations.*
import org.simbrain.util.dir

/**
 * Use this DSL to create the structure of the simulation menu in the Simbrain Desktop.
 * - dir is a JMenu
 * - item is a JMenuItem. The label given is used both as the Menu Item name, and in the command line as the name to
 * use to call them using "run sim" (see build.gradle#runSim). If duplicate labels are used the first one encountered
 * will be run from the command line.
 */
val simulations = dir<NewSimulation>("Simulations") {

    // This supersedes RegisteredSimulation.java. Will have to move that stuff here.

    dir("Demos") {
        item("Simple Network") { testSim }
        item("Mnist Images") { imageSim }
        item("Agent Trails") { kAgentTrails }
    }
    // dir("TestDiv2") {
    //     dir("TestDiv2-1") {
    //         item("Test Sim2") { testSim }
    //         item("Test Sim3") { testSim }
    //     }
    // }
    dir("Evolution") {
        item("Evolve Network") { evolveNetwork2 }
        item("Evolve Avoider") { evolveAvoider }
        item("Evolve Mouse") { evolveMouse }
    }
}

/**
 * Called by build.gradle#runSim when invoking a headless simulation from the command line.
 */
fun main(args: Array<String>) {

    if (args.isEmpty()) throw IllegalArgumentException("""
        Please supply a simulation name. A list of possible values are:
        ${simulations.items.map { (name, _) -> name }.joinToString(", ")}
    """.trimIndent())

    val name = args[0]
    val (_, sim) = simulations.items
        .firstOrNull { (key, _) -> key == name } ?:
            throw IllegalArgumentException("""
                Simulation $name not found. A list of possible values are:
                ${simulations.items.map { (name, _) -> name }.joinToString(", ")}
            """.trimIndent())

    sim.run()
}
