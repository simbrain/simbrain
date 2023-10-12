package org.simbrain.custom_sims.simulations
//
// import kotlinx.coroutines.Dispatchers
// import kotlinx.coroutines.MainScope
// import kotlinx.coroutines.launch
// import kotlinx.coroutines.runBlocking
// import org.simbrain.custom_sims.addNetworkComponent
// import org.simbrain.custom_sims.addOdorWorldComponent
// import org.simbrain.custom_sims.couplingManager
// import org.simbrain.custom_sims.newSim
// import org.simbrain.network.core.Synapse
// import org.simbrain.network.core.activations
// import org.simbrain.network.core.labels
// import org.simbrain.network.layouts.GridLayout
// import org.simbrain.network.layouts.LineLayout
// import org.simbrain.network.neuron_update_rules.LinearRule
// import org.simbrain.network.util.BiasedScalarData
// import org.simbrain.util.distanceTo
// import org.simbrain.util.format
// import org.simbrain.util.geneticalgorithms.*
// import org.simbrain.util.point
// import org.simbrain.util.stats.distributions.UniformRealDistribution
// import org.simbrain.util.widgets.ProgressWindow
// import org.simbrain.workspace.Workspace
// import org.simbrain.world.odorworld.entities.EntityType
// import org.simbrain.world.odorworld.entities.OdorWorldEntity
// import kotlin.math.abs
//
// /**
//  * Evolve agent to be a Braitenberg vehicle that pursues cheese
//  */
// val evolvePursuer = newSim {
//
//     val mainScope = MainScope()
//
//     /**
//      * Max generation to run before giving up
//      */
//     val maxGenerations = 100
//
//     fun createEvolution(): Evaluator {
//         val evolutionarySimulation = evolutionarySimulation(1) {
//
//             val inputs = chromosome(3) {
//                 nodeGene {
//                     upperBound = 5.0
//                 }
//             }
//
//             val hiddens = chromosome(8) {
//                 nodeGene {
//                     upperBound = 10.0
//                 }
//             }
//
//             val outputs = chromosome(3) {
//                 nodeGene {
//                     updateRule.let {
//                         if (it is LinearRule) {
//                             it.lowerBound = 0.0
//                             it.upperBound = 10.0
//                         }
//                     }
//                 }
//             }
//
//             val connections = chromosome<Synapse, ConnectionGene>()
//
//             val evolutionWorkspace = Workspace()
//
//             val networkComponent = evolutionWorkspace { addNetworkComponent("Network") }
//             val network = networkComponent.network
//
//             val odorworldComponent = evolutionWorkspace { addOdorWorldComponent("Odor World") }
//             val odorworld = odorworldComponent.world.apply {
//                 isObjectsBlockMovement = true
//                 wrapAround = true
//             }
//
//             val sensors = chromosome(3) {
//                 objectSensorGene {
//                     setObjectType(EntityType.SWISS)
//                     theta = (it - 1) * 60.0
//                     radius = 32.0
//                     decayFunction.dispersion = 200.0
//                 }
//             }
//
//             val straightMovement = chromosome(1) {
//                 straightMovementGene()
//             }
//
//             val turning = chromosome(
//                 turningGene { direction = -1.0 },
//                 turningGene { direction = 1.0 }
//             )
//
//             val mouse = odorworld.addEntity(EntityType.MOUSE).apply {
//                 location = point(50.0, 200.0)
//             }
//
//             fun OdorWorldEntity.reset() {
//                 location = point(random.nextDouble() * 300, random.nextDouble() * 300)
//             }
//
//             fun createCheese() = odorworld.addEntity(EntityType.SWISS).apply {
//                 location = point(random.nextDouble() * 300, random.nextDouble() * 300)
//                 heading = UniformRealDistribution(0.0, 360.0).sampleDouble()
//                 speed = 3.0
//                 events.collided.on {
//                     if (it === mouse) reset()
//                 }
//             }
//
//             val cheeses = List(3) { createCheese() }
//
//             onBuild { visible ->
//                 network {
//                     if (visible) {
//                         val inputGroup = +inputs.asNeuronCollection {
//                             label = "Input"
//                             layout(LineLayout())
//                             location = point(250, 280)
//                         }
//                         inputGroup.neuronList.labels = listOf("center", "left", "right")
//                         val hiddenGroup = +hiddens.asNeuronCollection {
//                             label = "Hidden"
//                             layout(GridLayout())
//                             location = point(0, 100)
//                         }
//                         val outputGroup = +outputs.asNeuronCollection {
//                             label = "Output"
//                             layout(LineLayout())
//                             location = point(250, 40)
//                             setNeuronType(outputs[0].template.updateRule)
//                         }
//                         outputGroup.neuronList.labels = listOf("straight", "left", "right")
//                     } else {
//                         +inputs
//                         +hiddens
//                         +outputs
//                     }
//                     +connections
//                 }
//                 mouse {
//                     +sensors
//                     +straightMovement
//                     +turning
//                 }
//                 evolutionWorkspace {
//                     runBlocking {
//                         couplingManager.apply {
//                             val (straightNeuron, leftNeuron, rightNeuron) = outputs.getProducts()
//                             val (straightConsumer) = straightMovement.getProducts()
//                             val (left, right) = turning.getProducts()
//
//                             sensors.getProducts() couple inputs.getProducts()
//                             straightNeuron couple straightConsumer
//                             leftNeuron couple left
//                             rightNeuron couple right
//                         }
//                     }
//                 }
//
//                 cheeses.forEach { cheese ->
//                     cheese.events.collided.on {
//                         cheese.location = point(
//                             random.nextDouble(100.0, 300.0),
//                             random.nextDouble(0.0, 300.0)
//                         )
//                     }
//                 }
//             }
//
//             onMutate {
//                 hiddens.forEach {
//                     it.mutate {
//                         dataHolder.let {
//                             if (it is BiasedScalarData) it.bias += random.nextDouble(-0.2, 0.2)
//                         }
//                     }
//                 }
//                 connections.forEach {
//                     it.mutate {
//                         strength += random.nextDouble(-0.2, 0.2)
//                     }
//                 }
//                 val source = (inputs + hiddens).selectRandom()
//                 val target = (outputs + hiddens).selectRandom()
//                 connections += connectionGene(source, target) {
//                     strength = random.nextDouble(-10.0, 10.0)
//                 }
//             }
//
//             onEval {
//                 var score = 0.0
//
//                 cheeses.forEach {
//                     it.events.collided.on { other ->
//                         if (other === mouse) {
//                             score += 1
//                         }
//                     }
//                 }
//
//                 evolutionWorkspace.addUpdateAction("compute energy") {
//                     val energy = abs(outputs.getProducts().activations.sum()) + 5
//                     score -= energy / 1000
//                 }
//
//                 evolutionWorkspace.apply {
//                     iterateSuspend(1000)
//                 }
//
//                 val partial = cheeses.map { cheese -> 100 - mouse.location.distanceTo(cheese.location) }
//                     .maxOf { it }
//                     .let { if (it < 0) 0.0 else it } / 100
//
//                 score + partial
//             }
//
//             onPeek {
//                 workspace.openFromZipData(evolutionWorkspace.zipData)
//             }
//
//         }
//
//         return evaluator(evolutionarySimulation) {
//             populationSize = 100
//             eliminationRatio = 0.5
//             runUntil { generation == maxGenerations || fitness > 16 }
//         }
//     }
//
//     mainScope.launch {
//
//         workspace.clearWorkspace()
//
//         val progressWindow = ProgressWindow(maxGenerations, "Error")
//
//         launch(Dispatchers.Default) {
//
//             val generations = createEvolution().start().onEachGenerationBest { agent, gen ->
//                 progressWindow.value = gen
//                 progressWindow.text = "Fitness: ${agent.fitness.format(2)}"
//             }
//             val (best, _) = generations.best
//
//             println(best)
//
//             best.visibleBuild().peek()
//
//             progressWindow.close()
//         }
//
//     }
//
// }