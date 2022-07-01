package org.simbrain.util.geneticalgorithms

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.activations

import org.simbrain.workspace.Workspace

class GeneticsTest {

    private val chromosome = Chromosome<Neuron, NodeGene>()

    @Test
    fun `node copy copies neuron properties correctly`() {
        val node = nodeGene() {
            activation = .5
            lowerBound = -1.2
            upperBound = 1.5
        }
        val copy = node.copy()
        assertEquals(.5, copy.template.activation)
        assertEquals(-1.2, copy.template.lowerBound)
        assertEquals(1.5, copy.template.upperBound)
    }

    @Test
    fun `node gene creates product specified in template`() {
        val node = nodeGene { activation = 0.7 }
        val neuron = node.buildWithContext(NetworkGeneticsContext(Network()))
        assertEquals(0.7, neuron.activation, 0.01)
    }

    @Test
    fun `node gene creates specified product after copied`() {
        val node = nodeGene { activation = 0.7 }
        val copy = node.copy()
        val neuron = copy.buildWithContext(NetworkGeneticsContext(Network()))
        assertEquals(0.7, neuron.activation, 0.01)
    }

    @Test
    fun `node events are working properly`() {
        val node = nodeGene()
        var counter = 0
        node.events.onCopy { counter++ }
        node.copy()
        assertEquals(counter, 1)
    }

    @Test
    fun `node chromosome with repeating default genes creates specified neurons`() {
        val environment = evolutionarySimulation {

            val network = Network()

            val nodes = chromosome(5) {
                nodeGene { activation = 0.7 }
            }

            onBuild {
                network {
                    +nodes
                }
            }

            onEval {
                nodes.products.forEach { neuron ->
                    assertEquals(0.7, neuron.activation, 0.01)
                }
                0.0
            }
        }

        environment.build().eval()
    }

    @Test
    fun `node chromosome with individually specified default genes creates corresponding neurons`() {
        val defaultActivations = listOf(0.2, 0.7, 0.3, 0.6, 0.5)
        val environment = evolutionarySimulation {

            val network = Network()

            defaultActivations.map {
                chromosome += nodeGene { activation = it }
            }

            onBuild {
                network {
                    +chromosome
                }
            }

            onEval {
                (chromosome.products.activations zip defaultActivations).forEach { (actual, expected) ->
                    assertEquals(expected, actual, 0.01)
                }
                0.0
            }
        }

        environment.build().eval()
    }

    @Test
    fun `genes are deeply copied after calling copy on environment`() {
        val refs = mutableListOf(mutableListOf<Any>())

        val network = Network()

        val environment = evolutionarySimulation {
            val inputs = chromosome(2) {
                nodeGene {
                    activation = 0.75
                    isClamped = true
                }
            }

            onBuild {
                network {
                    +inputs
                }
            }

            onEval {
                inputs.products.activations.forEach { assertEquals(0.75, it, 0.01) }
                refs.add(inputs.toMutableList())
                0.0
            }
        }
        val e1 = environment.copy()
        e1.build().eval()
        val e2 = e1.copy()
        e2.build().eval()
        assertTrue(refs[0].zip(refs[1]).none { (first, second) -> first !== second })
    }

    @Test
    fun `connection genes have correct references to node genes after copy`() {
        val environment = evolutionarySimulation {

            val network = Network()

            val inputs = chromosome(2) {
                nodeGene()
            }

            val outputs = chromosome(2) {
                nodeGene()
            }

            val synapses = chromosome(
                connectionGene(inputs[0], outputs[1]),
                connectionGene(inputs[1], outputs[0])
            )

            onBuild {
                network {
                    +inputs
                    +outputs
                    +synapses
                }
            }

            onEval {
                assertTrue(synapses[0].let {
                    it.source === inputs[0]
                            && it.target === outputs[1]
                })
                assertTrue(synapses[1].let {
                    it.source === inputs[1]
                            && it.target === outputs[0]
                })
                0.0
            }
        }

        environment.copy().copy().copy().build().eval()
    }

    @Test
    fun `connection genes have correct references to node genes after mutation`() {
        val environment = evolutionarySimulation {

            val network = Network()

            val inputs = chromosome(2) {
                nodeGene()
            }

            val outputs = chromosome(2) {
                nodeGene()
            }

            val synapses = chromosome(
                connectionGene(inputs[0], outputs[1]),
                connectionGene(inputs[1], outputs[0])
            )

            onBuild {
                network {
                    +inputs
                    +outputs
                    +synapses
                }
            }

            onMutate {
                val source = inputs.addAndReturnGene(nodeGene())
                val target = outputs.addAndReturnGene(nodeGene())
                synapses += connectionGene(source, target)
            }

            onEval {
                val condition = synapses.all {
                    val result = it.source in inputs && it.target in outputs
                    result
                }
                assertTrue(condition)
                0.0
            }
        }

        sequence {
            var newEnv = environment.copy()
            while (true) {
                yield(newEnv.build().eval())
                newEnv = newEnv.copy().apply { mutate() }
            }
        }.take(5).last()
    }

    @Test
    fun `coupling node chromosome with node chromosome creates correct couplings`() {
        val evolutionarySimulation = evolutionarySimulation {

            val workspace = Workspace()

            val nc = NetworkComponent("network")
            val network = nc.network
            workspace.addWorkspaceComponent(nc)

            val inputs = chromosome(3) {
                nodeGene {
                    isClamped = true
                }
            }

            val outputs = chromosome(3) {
                nodeGene()
            }

            onBuild {
                network {
                    +inputs
                    +outputs
                }

                with(workspace.couplingManager) {
                    inputs.products couple outputs.products
                }
            }


            onEval {

                inputs.products.activations = listOf(1.0, 1.0, 1.0)

                workspace.simpleIterate()

                assertArrayEquals(
                    inputs.products.activations.toTypedArray(),
                    outputs.products.activations.toTypedArray()
                )

                0.0
            }

        }

        val build = evolutionarySimulation.build()

        build.eval()
    }
}