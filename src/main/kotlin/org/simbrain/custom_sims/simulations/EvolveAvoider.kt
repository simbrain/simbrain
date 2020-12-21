package org.simbrain.custom_sims.simulations

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.simbrain.custom_sims.RegisteredSimulation
import org.simbrain.network.core.Synapse
import org.simbrain.network.neuron_update_rules.DecayRule
import org.simbrain.network.neuron_update_rules.LinearRule
import org.simbrain.network.neuron_update_rules.SigmoidalRule
import org.simbrain.network.neuron_update_rules.interfaces.BiasedUpdateRule
import org.simbrain.util.format
import org.simbrain.util.geneticalgorithms.*
import org.simbrain.util.point
import org.simbrain.util.widgets.ProgressWindow
import org.simbrain.workspace.gui.SimbrainDesktop
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.entities.OdorWorldEntity
import java.io.File

// Not working...

// Rename to Avoider
class EvolveAvoider(desktop: SimbrainDesktop?) : RegisteredSimulation(desktop) {

    private val mainScope = MainScope()

    override fun getName() = "Evolve Avoider"

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

    // Questions:
    // How to lay out hidden units in a nicer way


    private fun createEvolution(): Evaluator {
        val environmentBuilder = environmentBuilder(1) {

            // Set up the chromosomes.

            val inputs = chromosome(3) {
                nodeGene()
            }

            val hiddens = chromosome(8) {
                nodeGene {
                    updateRule = DecayRule().apply {
                        decayFraction = .01
                    }
                }
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
                    setObjectType(EntityType.POISON)
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

            fun OdorWorldEntity.reset() {
                setCenterLocation(random.nextDouble()*300,random.nextDouble()*300)
                velocityX = random.nextDouble(-1.0,1.0)
                velocityY = random.nextDouble(-1.0,1.0)
            }
            fun addPoison() = useEntity(EntityType.POISON) {
                reset()
            }

            val poison1 = addPoison();
            val poison2 = addPoison();
            val poison3 = addPoison();
            // TODO: What to do for many...

            // Take the current chromosomes,and express them via an agent in a world.
            // Everything needed to build one generation
            // Called once for each genome at each generation
            onBuild { pretty ->
                    workspace {
                        network {
                            if (pretty) {
                                +inputs.asGroup {
                                    label = "Input"
                                    location = point(0, 200)
                                }
                                +hiddens.asGroup {
                                    label = "Hidden"
                                    location = point(0, 100)
                                }
                                +outputs.asGroup {
                                    label = "Output"
                                    location = point(0, 0)
                                }
                            } else {
                                // This is update when graphics are off
                                +inputs
                                +hiddens
                                +outputs
                            }
                            +connections
                            // Not possible to connect nodes here, because the product does not exist yet
                            // A neat framework could address this (but is it worth it?)
                        }
                        odorworld {
                            mouse {
                                +sensors
                                +straightMovement
                                +turning
                            }
                            +poison1
                            +poison2
                            +poison3
                        }
                        couplingManager {
                            couple(sensors, inputs)
                            couple(outputs[0], straightMovement[0])
                            couple(outputs[1], turning[0])
                            couple(outputs[2], turning[1])
                        }
                    }
                }


            //
            // Mutate the chromosomes. Specify what things are mutated at each generation.
            //
            onMutate {
                hiddens.eachMutate {
                    updateRule.let {
                        if (it is BiasedUpdateRule) it.bias += random.nextDouble(-0.2, 0.2)
                    }
                }
                outputs.eachMutate {
                    when (random.nextInt(3)) {
                        0 -> updateRule = SigmoidalRule()
                        1 -> updateRule = DecayRule()
                        2 -> {
                        } // Leave the same
                    }
                }
                connections.eachMutate {
                    strength += random.nextDouble(-0.5, 0.5)
                }

                // Random source neuron
                val source = (inputs.genes + hiddens.genes).let {
                    val index = random.nextInt(0, it.size)
                    it[index]
                }
                // Random target neuron
                val target = (outputs.genes + hiddens.genes).let {
                    val index = random.nextInt(0, it.size)
                    it[index]
                }
                // Add the connection
                connections.genes.add(connectionGene(source, target) {
                    strength = random.nextDouble(-0.2, 0.2)
                })
            }

            //
            // Evaluate the current generation.
            //
            onEval {
                var score = 0.0

                mouse.product.setCenterLocation(Math.random()*300, Math.random()*300)

                fun OdorWorldEntity.handleCollision() {
                    onCollide { other ->
                        if (other === mouse.product) {
                            score -= 1
                        }
                        reset()
                    }
                }
                poison1.product.handleCollision();
                poison2.product.handleCollision();
                poison3.product.handleCollision();

                workspace {
                    repeat(1000) { simpleIterate() }
                }

                // val distFromPoison = SimbrainMath.clip(
                //     mouse.product.getRadiusTo(poison.product) / 100,
                //     0.0, 1.0
                // )
                score
            }

            // Todo: work on getting rid of winner.zip
            onPeek {
                workspace { save(File("winner.zip")) }
                sim.workspace.openWorkspace(File("winner.zip"))
            }
        }

        return evaluator(environmentBuilder) {
            populationSize = 100
            eliminationRatio = 0.5
            runUntil { generation == 50 || fitness > 50 }
        }
    }

    override fun instantiate(desktop: SimbrainDesktop?): RegisteredSimulation {
        return EvolveAvoider(desktop)
    }

}