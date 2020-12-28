package org.simbrain.custom_sims.simulations

import org.simbrain.util.dir

val simulations = dir<NewSimulation>("Simulations") {
    dir("Demo") {
        item("Test Sim") { testSim }
        item("Agent Trials") { kAgentTrials }
    }
    dir("TestDiv2") {
        dir("TestDiv2-1") {
            item("Test Sim2") { testSim }
            item("Test Sim3") { testSim }
        }
    }
}

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
