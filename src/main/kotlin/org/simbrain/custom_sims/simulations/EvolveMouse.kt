package org.simbrain.custom_sims.simulations

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.simbrain.custom_sims.RegisteredSimulation
import org.simbrain.network.core.Synapse
import org.simbrain.network.neuron_update_rules.LinearRule
import org.simbrain.network.neuron_update_rules.interfaces.BiasedUpdateRule
import org.simbrain.util.format
import org.simbrain.util.geneticalgorithms.*
import org.simbrain.util.nextNegate
import org.simbrain.util.point
import org.simbrain.util.widgets.ProgressWindow
import org.simbrain.workspace.gui.SimbrainDesktop
import org.simbrain.world.odorworld.entities.EntityType
import java.io.File

class EvolveMouse(desktop: SimbrainDesktop?) : RegisteredSimulation(desktop) {

    private val mainScope = MainScope()

    override fun getName() = "Evolve Mouse"

    override fun getSubmenuName() = "Evolution"

    override fun run() {

        mainScope.launch {

            sim.workspace.clearWorkspace()

            val progressWindow = ProgressWindow(200)

            launch(Dispatchers.Default) {

                val generations = createEvolution().start().onEachIndexed { generation, result ->
                    progressWindow.progressBar.value = generation
                    progressWindow.fitnessScore.text = "Fitness: ${result[0].fitness.format(2)}"
                }
                val (best, _) = generations.last().first()

                println(best)

                best.prettyBuild().peek()

                progressWindow.close()
            }

        }

    }

    private fun createEvolution(): Evaluator {
        val environmentBuilder = environmentBuilder(1) {

            val inputs = chromosome(3) {
                nodeGene()
            }

            val hiddens = chromosome(8) {
                nodeGene()
            }

            val outputs = chromosome(3) {
                nodeGene {
                    updateRule.let {
                        if (it is LinearRule) {
                            it.lowerBound = 0.0
                        }
                    }
                }
            }

            val connections = chromosome<Synapse, ConnectionGene>()

            val workspace = useWorkspace()

            val network = useNetwork()

            val odorworld = useOdorWorld()

            val sensors = chromosome(3) {
                objectSensorGene {
                    setObjectType(EntityType.SWISS)
                    theta = it * 2 * Math.PI / 3
                    radius = 32.0
                    decayFunction.dispersion = 200.0
                }
            }

            val straightMovement = chromosome(
                    straightMovementGene()
            )

            val turning = chromosome(
                    turningGene { direction = -1.0 },
                    turningGene { direction = 1.0 }
            )

            val mouse = useEntity(EntityType.MOUSE) {
                setCenterLocation(100.0, 200.0)
            }

            val cheese = useEntity(EntityType.SWISS) {
                setCenterLocation(150.0, 200.0)
            }

            onBuild { pretty ->
                workspace {
                    network {
                        if (pretty) {
                            +inputs.asGroup {
                                label = "Input"
                                location = point(0, 100)
                            }
                            +hiddens
                            +outputs.asGroup {
                                label = "Output"
                                location = point(0, -100)
                            }
                        } else {
                            +inputs
                            +hiddens
                            +outputs
                        }
                        +connections
                    }
                    odorworld {
                        mouse {
                            +sensors
                            +straightMovement
                            +turning
                        }
                        +cheese
                    }
                    couplingManager {
                        couple(sensors, inputs)
                        couple(outputs[0], straightMovement[0])
                        couple(outputs[1], turning[0])
                        couple(outputs[2], turning[1])
                    }
                }
            }

            onMutate {
                hiddens.eachMutate {
                    updateRule.let {
                        if (it is BiasedUpdateRule) it.bias += random.nextDouble(-0.2, 0.2)
                    }
                }
                connections.eachMutate {
                    strength += random.nextDouble(-0.2, 0.2)
                }
                val source = (inputs.genes + hiddens.genes).let {
                    val index = random.nextInt(0, it.size)
                    it[index]
                }
                val target = (outputs.genes + hiddens.genes).let {
                    val index = random.nextInt(0, it.size)
                    it[index]
                }
                connections.genes.add(connectionGene(source, target) {
                    strength = random.nextDouble(-0.2, 0.2)
                })
            }

            onEval {
                var score = 0.0

                cheese {
                    val x = mouse { centerX } + evalRand.nextDouble(30.0, 70.0) * evalRand.nextNegate()
                    val y = mouse { centerY } + evalRand.nextDouble(30.0, 70.0) * evalRand.nextNegate()
                    setCenterLocation(x, y)
                }

                cheese.product.setCenterLocation(
                        mouse.product.centerX + evalRand.nextDouble(30.0, 70.0) * evalRand.nextNegate(),
                        mouse.product.centerY + evalRand.nextDouble(30.0, 70.0) * evalRand.nextNegate()
                )

                cheese.product.onCollide { other ->
                    if (other === mouse.product) {
                        score += 1
                    }
                    cheese.product.setCenterLocation(
                            mouse.product.centerX + evalRand.nextDouble(30.0, 70.0) * evalRand.nextNegate(),
                            mouse.product.centerY + evalRand.nextDouble(30.0, 70.0) * evalRand.nextNegate()
                    )
                }
                workspace {
                    repeat(1000) { simpleIterate() }
                }
                val partial = (100 - mouse.product.getRadiusTo(cheese.product)).let { if (it < 0) 0.0 else it } / 100
                score + partial
            }

            onPeek {
                workspace { save(File("winner.zip")) }
                sim.workspace.openWorkspace(File("winner.zip"))
            }

        }

        return evaluator(environmentBuilder) {
            populationSize = 100
            eliminationRatio = 0.5
            runUntil { generation == 200 || fitness > 50 }
        }
    }

    override fun instantiate(desktop: SimbrainDesktop?): RegisteredSimulation {
        return EvolveMouse(desktop)
    }

}