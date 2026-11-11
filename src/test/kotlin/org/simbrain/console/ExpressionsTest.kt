package org.simbrain.console

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.Synapse
import org.simbrain.workspace.Workspace

class ExpressionsTest {

    private fun value(text: String, vararg names: Pair<String, Any>) =
        Expression.parse(text).evaluate(ExpressionContext(index = 2, count = 5) { names.toMap()[it] ?: Expression.MISSING })

    private fun shell(): CommandShell {
        val workspace = Workspace()
        val component = NetworkComponent("net1")
        val neurons = List(4) { Neuron() }
        runBlocking { component.network.addNetworkModels(neurons + Synapse(neurons[0], neurons[1])) }
        workspace.addWorkspaceComponent(component)
        return CommandShell(workspace)
    }

    private fun CommandShell.run(line: String) = runBlocking { withTimeout(30_000) { execute(line) } }

    @Test
    fun `arithmetic follows the usual precedence`() {
        assertEquals(7.0, value("1+2*3"))
        assertEquals(9.0, value("(1+2)*3"))
        assertEquals(-8.0, value("-2^3"))
        assertEquals(1.0, value("7 % 3"))
        assertEquals(0.4, value("i/n"))
    }

    @Test
    fun `names refer to properties and functions are available`() {
        assertEquals(1.5, value("activation*3", "activation" to 0.5))
        assertEquals(3.0, value("max(1, abs(-3), 2)"))
        val random = value("rand(5, 6)") as Double
        assertTrue(random >= 5 && random < 6)
    }

    @Test
    fun `conditions combine with and, or, and not`() {
        assertEquals(true, value("x>1 and y=on", "x" to 2.0, "y" to "on"))
        assertEquals(true, value("x<1 or not y=off", "x" to 2.0, "y" to "on"))
        assertEquals(false, value("type=Synapse", "type" to "Neuron"))
    }

    @Test
    fun `a missing property makes a condition false`() {
        val expression = Expression.parse("activation>0.5")
        assertFalse(expression.test(ExpressionContext { Expression.MISSING }))
        assertEquals(setOf("activation"), expression.missingNames)
    }

    @Test
    fun `bad expressions are reported`() {
        assertEquals("Missing ) in (1+2", runCatching { Expression.parse("(1+2") }.exceptionOrNull()?.message)
        assertTrue(runCatching { value("nope(1)") }.exceptionOrNull()?.message!!.startsWith("Unknown function: nope"))
    }

    @Test
    fun `set gives each object its own random value`() {
        val shell = shell()
        assertEquals("Set 4 objects.", shell.run("set net1/Neuron_* activation=rand()*10"))
        val values = shell.run("get net1/Neuron_* activation").lines().map { it.substringAfter(": ").toDouble() }
        assertTrue(values.all { it in 0.0..10.0 }, values.toString())
        assertNotEquals(1, values.toSet().size)
    }

    @Test
    fun `set can use the object's own values and its position`() {
        val shell = shell()
        shell.run("set net1/Neuron_* activation=i/n")
        assertEquals("Neuron_1: 0\nNeuron_2: 0.25\nNeuron_3: 0.5\nNeuron_4: 0.75", shell.run("get net1/Neuron_* activation"))
        shell.run("set net1/Neuron_* activation=activation*2+1")
        assertEquals("2.5", shell.run("get Neuron_4 activation"))
    }

    @Test
    fun `where filters what set changes`() {
        val shell = shell()
        shell.run("set net1/Neuron_{1..2} activation=1")
        assertEquals("Set 2 objects.", shell.run("set net1/* activation=5 where type=Neuron and activation>0.5"))
        assertEquals("Neuron_1: 5\nNeuron_2: 5\nNeuron_3: 0\nNeuron_4: 0", shell.run("get net1/Neuron_* activation"))
        assertEquals("Neuron_1  Neuron_2", shell.run("ls net1 where activation >= 5"))
    }

    @Test
    fun `where works with other commands and reports a misspelling`() {
        val shell = shell()
        assertEquals("Synapse_1", shell.run("ls where type=Synapse").let { shell.run("cd net1"); shell.run("ls where type=Synapse") })
        assertEquals("Nothing matches the condition.", shell.run("rm net1/* where activation>100"))
        assertEquals("Nothing has a property called actvation. Use show to see property names.", shell.run("set net1/* activation=1 where actvation>0"))
        assertEquals("run can't be used with where.", shell.run("run 1 where activation>0"))
    }

    @Test
    fun `new neurons and synapses can be given expressions`() {
        val shell = shell()
        shell.run("add neurons 3 in=net1 activation=i+1")
        assertEquals("1", shell.run("get Neuron_5 activation"))
        assertEquals("3", shell.run("get Neuron_7 activation"))
        assertEquals("Added 1 synapse(s).", shell.run("connect net1/Neuron_5 net1/Neuron_6 strength=2*3"))
        assertEquals("6", shell.run("get net1/Synapse_2 strength"))
    }

    @Test
    fun `a whole-number property refuses a fraction`() {
        val shell = shell()
        val output = shell.run("set net1/Neuron_1 updatePriority=1.5")
        assertTrue(output.contains("needs a whole number") || output.contains("no property"), output)
    }
}
