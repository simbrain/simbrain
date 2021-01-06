package org.simbrain.custom_sims.simulations

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.simbrain.custom_sims.addNetworkComponent
import org.simbrain.custom_sims.addOdorWorldComponent
import org.simbrain.custom_sims.couplingManager
import org.simbrain.custom_sims.newSim
import org.simbrain.network.core.Synapse
import org.simbrain.network.neuron_update_rules.DecayRule
import org.simbrain.network.neuron_update_rules.LinearRule
import org.simbrain.network.neuron_update_rules.SigmoidalRule
import org.simbrain.network.neuron_update_rules.interfaces.BiasedUpdateRule
import org.simbrain.util.format
import org.simbrain.util.geneticalgorithms.*
import org.simbrain.util.point
import org.simbrain.util.widgets.ProgressWindow
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.entities.OdorWorldEntity
import java.io.File

// Not working...

// Rename to Avoider

val evolveAvoider = newSim {

    val scope = MainScope()

    fun createEvolution(): Evaluator {
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

            val networkComponent = addNetworkComponent("Avoider")

            val network = networkComponent.network

            val odorworldComponent = addOdorWorldComponent("World")

            val odorworld = odorworldComponent.world

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

            val mouse = odorworld.addEntity(EntityType.MOUSE).apply {
                setCenterLocation(100.0, 200.0)
            }

            fun OdorWorldEntity.reset() {
                setCenterLocation(random.nextDouble()*300,random.nextDouble()*300)
                velocityX = random.nextDouble(-1.0,1.0)
                velocityY = random.nextDouble(-1.0,1.0)
            }

            fun addPoison() = odorworld.addEntity(EntityType.POISON).apply {
                setCenterLocation(100.0, 200.0)
            }

            val poison1 = addPoison();
            val poison2 = addPoison();
            val poison3 = addPoison();
            // TODO: What to do for many...

            // Take the current chromosomes,and express them via an agent in a world.
            // Everything needed to build one generation
            // Called once for each genome at each generation
            onBuild { pretty ->
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
                }
                mouse {
                    +sensors
                    +straightMovement
                    +turning
                }

                couplingManager.apply {
                    val (straightNeuron, leftNeuron, rightNeuron) = outputs.products
                    val (straightConsumer) = straightMovement.products
                    val (left, right) = turning.products

                    sensors.products couple inputs.products
                    straightNeuron couple straightConsumer
                    leftNeuron couple left
                    rightNeuron couple right
                }
            }


            //
            // Mutate the chromosomes. Specify what things are mutated at each generation.
            //
            onMutate {
                hiddens.genes.forEach {
                    it.mutate {
                        updateRule.let {
                            if (it is BiasedUpdateRule) it.bias += random.nextDouble(-0.2, 0.2)
                        }
                    }
                }
                outputs.genes.forEach {
                    it.mutate {
                        when (random.nextInt(3)) {
                            0 -> updateRule = SigmoidalRule()
                            1 -> updateRule = DecayRule()
                            2 -> {
                            } // Leave the same
                        }
                    }
                }
                connections.genes.forEach {
                    it.mutate {
                        strength += random.nextDouble(-0.5, 0.5)
                    }
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

                mouse.setCenterLocation(Math.random()*300, Math.random()*300)

                fun OdorWorldEntity.handleCollision() {
                    onCollide { other ->
                        if (other === mouse) {
                            score -= 1
                        }
                        reset()
                    }
                }
                poison1.handleCollision();
                poison2.handleCollision();
                poison3.handleCollision();

                workspace.apply {
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
                workspace.apply { save(File("winner.zip")) }
                workspace.openWorkspace(File("winner.zip"))
            }
        }

        return evaluator(environmentBuilder) {
            populationSize = 100
            eliminationRatio = 0.5
            runUntil { generation == 50 || fitness > 50 }
        }
    }

    scope.launch {
        workspace.clearWorkspace()

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