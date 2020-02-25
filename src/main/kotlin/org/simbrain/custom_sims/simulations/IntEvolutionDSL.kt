package org.simbrain.custom_sims.simulations

//import org.simbrain.util.geneticalgorithm.EnvironmentBuilder
//import org.simbrain.util.geneticalgorithm.EvolutionBuilder
//
//fun main() {
//    val environment = org.simbrain.util.geneticalgorithm.environment {
//
//        val outputCoupling = +coupling
//
//        val evolveNet = network {
//
//            val inputs = neuronGroup {
//                size = 2
//                label = "Input"
//            }
//
//            val hidden = nodeChromosome {
//                maxSize = 100
//                updateRules = allUpdateRules
//            }
//
//            val outputs = neuronGroup {
//                size = 1
//                label = "Output"
//            }
//
//            outputCoupling.producer = outputs
//
//            val connections = connectionChromosome {
//                newConnectionProbability = 0.05
//                inputs connectsTo outputs
//                inputs connectsTo hidden
//                hidden connectsTo hidden
//                hidden connectsTo outputs
//            }
//        }
//
//        tasks {
//            +evolution {
//                population = 100
//                eliminationRate = 0.5
//                evaluation {
//                    outputCoupling.consume { activations ->
//                        activations sse 1
//                    }
//                }
//                endConditions {
//                    agents.any { it.fitness greaterThan 1 }
//                }
//                failConditions {
//                    generation equals 500
//                }
//                onEachGeneration { (generation, agents) ->
//                    println("|${generation}|${agents.best()}")
//                }
//                onCompleted { (_, agents) ->
//                    println("FINAL RESULT -> ${agents.best()}")
//                }
//            }
//            +evolution {
//                evaluation { agent -> agent.doSomeOtherStuff() }
//                endConditions { fitness lessThan 3 }
//                failConditions { generation equals 500 }
//                onEachGeneration(standardOutput)
//                onCompleted(standardOutput)
//            }
//        }
//
//
//    }
//
//    environment.runTasks()
//}
//
//class EnvironmentBuilder {
//
//    val components = mutableListOf<() -> Any>()
//
//    fun network(init: NetworkBuilder.() -> Unit): NetworkBuilder {
//        val builder = NetworkBuilder()
//        builder.init()
//        return builder
//    }
//
//    fun tasks(init: TaskBuilder.() -> Unit) {
//
//    }
//
//
//}
//
//class EvolutionBuilder {
//
//    var population = 100
//    var eliminationRate = 0.5
//        set(value) {
//            field = value.clip(0.0..1.0)
//        }
//
//    class Condition {
//        infix fun Double.greaterThan(i: Int) {
//
//        }
//    }
//
//    var _evaluation: (NetworkAgent) -> Double = { 0.0 }
//    fun evaluation(eval: (NetworkAgent) -> Double) {
//        _evaluation = eval
//    }
//
//    var _endConditions: Condition = Condition()
//    fun endConditions(condition: Condition.(NetworkAgent) -> Unit) {
//
//    }
//
//    var _failConditions: Condition = Condition()
//    fun failConditions(condition: Condition.(NetworkAgent) -> Unit) {
//
//    }
//}
//
//class TaskBuilder {
//    val agents = listOf<Agent2<NetworkGenome>>()
//    val generation = 1
//    fun evolution(init: EvolutionBuilder.() -> Unit) {
//
//    }
//}
//
//class NodeChromosomeBuilder {
//    val allUpdateRules = NeuronUpdateRule.RULE_LIST.toList()
//    var maxSize = Int.MAX_VALUE
//    var initialSize = 0
//    var updateRules = allUpdateRules
//}
//
//class NeuronGroupBuilder {
//    var label: String? = null
//    var size = 0
//}
//
//class NeuronGroupPrototype
//
//class ConnectionChromosomeBuilder {
//    var newConnectionProbability = 0.05
//    infix fun NeuronGroupPrototype.connectsTo(target: NeuronGroupPrototype) {
//
//    }
//}
//
//class NetworkBuilder {
//
//    val network = Network()
//
//    val nodes = mutableListOf<NodeChromosome>()
//
//    fun nodeChromosome(init: NodeChromosomeBuilder.() -> Unit): NeuronGroupPrototype {
//        val builder = NodeChromosomeBuilder().apply(init)
//        with(builder) {
//            nodes.add(NodeChromosome(
//                    List(initialSize) { NodeGene(Neuron(network)) } // TODO
//            ))
//        }
//    }
//
//    inline fun neuronGroup(init: NeuronGroupBuilder.() -> Unit) = NeuronGroupBuilder().apply(init).let { builder ->
//        NeuronGroup(network, builder.size).also { ng ->
//            builder.label?.let { label -> ng.label = label }
//            network.addNeuronGroup(ng)
//        }
//    }
//
//    fun connectionChromosome(init: ConnectionChromosomeBuilder.() -> Unit) {
//
//    }
//}
//
//class Environment {
//
//
//    fun execute() {
//        TODO("not implemented")
//    }
//}
//
//fun environment(init: EnvironmentBuilder.() -> Unit): Environment {
//    val environment = Environment()
//}

//class NetworkBuilder {
//
//    val network = Network()
//
//    operator fun Neuron.unaryPlus() = network.addLooseNeuron(this)
//
//    inline fun neuron(init: Neuron.() -> Unit = { })
//            = Neuron(network).apply(init).also { network.addLooseNeuron(it) }
//
//}
//
//data class Ball(var color: Color = Color.black, var radius: Double = 1.0)
//
//fun ball(init: (Ball) -> Unit): Ball {
//    val ball = Ball()
//    init(ball)
//    return ball
//}
//
//// or:
//val redBall = ball { it.color = Color.RED }
//
//
//inline fun network(init: NetworkBuilder.() -> Unit = { }) = NetworkBuilder().apply(init).network
//
//fun test() {
//    val network = network {
//        +neuron()
//        +neuron { lowerBound = 0.0 }
//    }
//}
