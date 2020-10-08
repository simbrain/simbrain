package org.simbrain.util.geneticalgorithm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.simbrain.network.util.activations

class Genetics5Test {

    @Test
    fun `test if copy makes deep copy`() {
        val refs = mutableListOf(mutableListOf<Any>())

        val environment = environmentBuilder {
            val inputs = chromosome(2) {
                nodeGene {
                    activation = 0.75
                    isClamped = true
                }
            }

            onBuild {
                +network {
                    +inputs
                }
            }

            onEval {
                inputs.products.activations.forEach { assertEquals(0.75, it, 0.01) }
                refs.add(inputs.current.genes.toMutableList())
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
    fun `test if connection genes has correct reference to node genes after copy`() {
        val environment = environmentBuilder {
            val inputs = chromosome(2) {
                nodeGene()
            }

            val outputs = chromosome(2) {
                nodeGene()
            }

            val synapses = chromosome(
                    connectionGene(inputs.current.genes[0], outputs.current.genes[1]),
                    connectionGene(inputs.current.genes[1], outputs.current.genes[0]),
            )

            onBuild {
                +network {
                    +inputs
                    +outputs
                    +synapses
                }
            }

            onEval {
                assertTrue(synapses.current.genes[0].let {
                    it.source === inputs.current.genes[0]
                            && it.target === outputs.current.genes[1]
                })
                assertTrue(synapses.current.genes[1].let {
                    it.source === inputs.current.genes[1]
                            && it.target === outputs.current.genes[0]
                })
                0.0
            }
        }

        environment.copy().copy().copy().build().eval()
    }

    @Test
    fun `test if connection genes has correct reference to node genes after mutation`() {
        val environment = environmentBuilder {
            val inputs = chromosome(2) {
                nodeGene()
            }

            val outputs = chromosome(2) {
                nodeGene()
            }

            val synapses = chromosome(
                    connectionGene(inputs.current.genes[0], outputs.current.genes[1]),
                    connectionGene(inputs.current.genes[1], outputs.current.genes[0]),
            )

            onBuild {
                +network {
                    +inputs
                    +outputs
                    +synapses
                }
            }

            onMutate {
                val source = nodeGene()
                val target = nodeGene()
                inputs.current.genes.add(source)
                outputs.current.genes.add(target)
                synapses.current.genes.add(connectionGene(source, target))
            }

            onEval {
                val condition = synapses.current.genes.all {
                    val result = it.source in inputs.current.genes && it.target in outputs.current.genes
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
        }.onEach { println(it) }.take(100).last()
    }
}