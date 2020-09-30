package org.simbrain.custom_sims.simulations

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.simbrain.custom_sims.RegisteredSimulation
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Synapse
import org.simbrain.network.neuron_update_rules.LinearRule
import org.simbrain.network.neuron_update_rules.interfaces.BiasedUpdateRule
import org.simbrain.util.format
import org.simbrain.util.geneticalgorithm.*
import org.simbrain.util.neat.gui.ProgressWindow
import org.simbrain.workspace.couplings.getConsumer
import org.simbrain.workspace.couplings.getProducer
import org.simbrain.workspace.gui.SimbrainDesktop
import org.simbrain.world.odorworld.OdorWorldComponent
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.sensors.ObjectSensor
import java.util.*
import kotlin.streams.toList

class EvolveMouse(desktop: SimbrainDesktop?) : RegisteredSimulation(desktop) {

    private val mainScope = MainScope()

    override fun getName() = "Evolve Mouse"

    override fun getSubmenuName() = "Evolution"

    override fun run() {

        mainScope.launch {

            sim.workspace.clearWorkspace()

            val progressWindow = ProgressWindow(100)

            launch(Dispatchers.Default) {
                val generations = evolve { generation, result ->
                    progressWindow.progressBar.value = generation
                    progressWindow.fitnessScore.text = "Error: ${result[0].second.format(2)}"
                }
                val (best, _) = generations.last().first()

                println(best)

                val list = best.build().evaluationContext.workspace.componentList

                sim.addNetwork(
                        list.filterIsInstance<NetworkComponent>().first(),
                        0, 200, 200, 0
                )

                sim.addOdorWorld(0, 200, 200, 200, "hi", list.filterIsInstance<OdorWorldComponent>().first().world)


                progressWindow.close()
            }

        }

    }

    fun evolve(peek: (generation: Int, result: List<Pair<EnvironmentBuilder, Double>>) -> Unit): Sequence<List<Pair<EnvironmentBuilder, Double>>> {
        val environmentBuilder = environmentBuilder {

            val inputs = memoize {
                chromosome(3) {
                    nodeGene()
                }
            }

            val hiddens = memoize {
                chromosome(2) {
                    nodeGene()
                }
            }

            val outputs = memoize {
                chromosome(3) {
                    nodeGene {
                        updateRule.let {
                            if (it is LinearRule) {
                                it.lowerBound = 0.0
                            }
                        }
                    }
                }
            }

            val connections = memoize {
                chromosome<Synapse, ConnectionGene5>()
            }

            val sensors = memoize {
                chromosome(3) {
                    objectSensorGene {
                        setObjectType(EntityType.SWISS)
                        theta = it * 2 * Math.PI / 3
                        radius = 32.0
                    }
                }
            }

            val straightMovement = memoize {
                chromosome(
                        straightMovementGene()
                )
            }

            val turning = memoize {
                chromosome(
                        turningGene { direction = -0.1 },
                        turningGene { direction = 0.1 }
                )
            }

            val mouse = entity(EntityType.MOUSE) {
                setCenterLocation(100.0, 200.0)
            }

            val cheese = entity(EntityType.SWISS) {
                setCenterLocation(150.0, 200.0)
            }

            onBuild {
                +odorworld {
                    +mouse {
                        +sensors
                        +straightMovement
                        +turning
                    }
                    +cheese
                }
                +network {
                    +inputs
                    +hiddens
                    +outputs
                    +connections
                }
            }

            onPrettyBuild {
                +odorworld {
                    +mouse {
                        +sensors
                        +straightMovement
                        +turning
                    }
                    +cheese
                }
                +network {
                    +inputs.asGroup {
                        label = "Input"
                    }
                    +hiddens
                    +outputs
                    +connections
                }
            }

            onMutate {
                hiddens.current.eachMutate {
                    updateRule.let {
                        if (it is BiasedUpdateRule) it.bias += (Random().nextDouble() - 0.5) * 0.2
                    }
                }
                connections.current.eachMutate {
                    strength += (Random().nextDouble() - 0.5 ) * 0.2
                }
                val source = (inputs.current.genes + hiddens.current.genes).shuffled().first()
                val target = (outputs.current.genes + hiddens.current.genes).shuffled().first()
                connections.current.genes.add(connectionGene(source, target) {
                    strength = (Random().nextDouble() - 0.5 ) * 0.2
                })
            }

            onEval {
                var score = 0.0
                coupling {
                    createOneToOneCouplings(
                            mouse.products.sensors.map { sensor ->
                                (sensor as ObjectSensor).getProducer("getCurrentValue")
                            },
                            inputs.products.map { neuron ->
                                neuron.getConsumer("setActivation")
                            }
                    )
                    createOneToOneCouplings(
                            inputs.products.map { neuron ->
                                neuron.getProducer("getActivation")
                            },
                            mouse.products.effectors.map { effector ->
                                effector.getConsumer("setAmount")
                            }
                    )
                }
                cheese.products.onCollide { other ->
                    if (other === mouse.products) {
                        score += 1
                    }
                    cheese.products.randomizeLocation()
                }
                repeat(100) {
                    workspace.simpleIterate()
                }
                score + mouse.products.sensors.sumByDouble { (it as ObjectSensor).currentValue }
            }

        }

        val population = generateSequence(environmentBuilder.copy()) { it.copy() }.take(100).toList()

        return sequence {
            var next = population
            while (true) {
                val current = next.parallelStream().map {
                    val build = it.build()
                    val score = build.eval()
                    Pair(it, score)
                }.toList().shuffled().sortedBy { it.second }
                val survivors = current.take(current.size / 2)
                next = survivors.map { it.first } + survivors.parallelStream().map { it.first.copy().apply { mutate() } }.toList()
                yield(current)
            }
        }.onEachIndexed(peek).take(1000).takeWhile { it[0].second < 7 }
    }

    override fun instantiate(desktop: SimbrainDesktop?): RegisteredSimulation {
        return EvolveMouse(desktop)
    }

}