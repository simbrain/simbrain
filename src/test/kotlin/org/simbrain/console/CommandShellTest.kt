package org.simbrain.console

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.NetworkModel
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.NeuronCollection
import org.simbrain.network.core.Synapse
import org.simbrain.network.subnetworks.Hopfield
import org.simbrain.network.updaterules.SigmoidalRule
import org.simbrain.workspace.Workspace
import java.util.concurrent.CopyOnWriteArrayList

class CommandShellTest {

    private fun shellWithNetwork(name: String = "net1"): CommandShell {
        val workspace = Workspace()
        val component = NetworkComponent(name)
        val source = Neuron().apply { activation = 0.75 }
        val target = Neuron()
        runBlocking { component.network.addNetworkModels(source, target, Synapse(source, target)) }
        workspace.addWorkspaceComponent(component)
        return CommandShell(workspace)
    }

    private fun CommandShell.run(line: String) = runBlocking { withTimeout(30_000) { execute(line) } }

    @Test
    fun `tokenize splits on whitespace and keeps quoted words together`() {
        assertEquals(listOf("ls", "my net/Neuron_1", "x"), tokenize("  ls  \"my net\"/Neuron_1   'x' "))
    }

    @Test
    fun `an unterminated quote is reported`() {
        assertEquals("Unterminated quote.", shellWithNetwork().run("ls \"net1"))
    }

    @Test
    fun `a blank line produces no output`() {
        assertEquals("", shellWithNetwork().run("   "))
    }

    @Test
    fun `an unknown command is reported`() {
        assertEquals("Unknown command: foo. Enter help for a list.", shellWithNetwork().run("foo"))
    }

    @Test
    fun `ls by itself lists workspace components`() {
        assertEquals("net1", shellWithNetwork().run("ls"))
    }

    @Test
    fun `ls on an empty workspace says so`() {
        assertEquals("(empty)", CommandShell(Workspace()).run("ls"))
    }

    @Test
    fun `ls of a network lists its models`() {
        assertEquals("Neuron_1  Neuron_2  Synapse_1", shellWithNetwork().run("ls net1").split("  ").sorted().joinToString("  "))
    }

    @Test
    fun `ls quotes names containing spaces`() {
        assertEquals("\"my net\"", shellWithNetwork("my net").run("ls"))
    }

    @Test
    fun `names containing spaces can be addressed with quotes`() {
        assertTrue(shellWithNetwork("my net").run("ls \"my net\"").contains("Neuron_1"))
    }

    @Test
    fun `names match regardless of case`() {
        assertTrue(shellWithNetwork().run("ls NET1").contains("Neuron_1"))
    }

    @Test
    fun `ls of something without contents points to show`() {
        assertEquals(
            "Neuron_1 has no contents. Use show to see its properties.",
            shellWithNetwork().run("ls net1/Neuron_1")
        )
    }

    @Test
    fun `a missing path is reported`() {
        assertEquals("Not found: nope", shellWithNetwork().run("ls nope"))
    }

    @Test
    fun `show lists editable properties and connectivity for a neuron`() {
        val output = shellWithNetwork().run("show net1/Neuron_1")
        assertTrue(output.startsWith("Neuron_1 (Neuron)"), output)
        assertTrue(output.contains("activation: 0.75"), output)
        assertTrue(output.contains("updateRule: Linear"), output)
        assertTrue(output.contains("fanOut: 1"), output)
    }

    @Test
    fun `show gives the endpoints of a synapse`() {
        val output = shellWithNetwork().run("show net1/Synapse_1")
        assertTrue(output.contains("source: Neuron_1"), output)
        assertTrue(output.contains("target: Neuron_2"), output)
    }

    @Test
    fun `show of a network counts its models`() {
        val output = shellWithNetwork().run("show net1")
        assertTrue(output.contains("Neuron: 2"), output)
        assertTrue(output.contains("Synapse: 1"), output)
    }

    @Test
    fun `show by itself describes the workspace`() {
        val output = shellWithNetwork().run("show")
        assertTrue(output.contains("time: 0"), output)
        assertTrue(output.contains("components: 1"), output)
    }

    @Test
    fun `cd changes the prompt and makes paths relative`() {
        val shell = shellWithNetwork()
        assertEquals("", shell.run("cd net1"))
        assertEquals("net1> ", shell.prompt)
        assertTrue(shell.run("show Neuron_1").contains("activation: 0.75"))
    }

    @Test
    fun `cd up, to root, and with no argument return to the workspace`() {
        val shell = shellWithNetwork()
        for (back in listOf("cd ..", "cd /", "cd")) {
            shell.run("cd net1")
            shell.run(back)
            assertEquals("> ", shell.prompt, back)
        }
    }

    @Test
    fun `an absolute path works from inside a component`() {
        val shell = shellWithNetwork()
        shell.run("cd net1")
        assertEquals("net1", shell.run("ls /"))
    }

    @Test
    fun `cd into something without contents is refused`() {
        val shell = shellWithNetwork()
        assertEquals("Neuron_1 has no contents to enter.", shell.run("cd net1/Neuron_1"))
        assertEquals("> ", shell.prompt)
    }

    @Test
    fun `location falls back to the workspace when its component is removed`() {
        val shell = shellWithNetwork()
        shell.run("cd net1")
        shell.workspace.removeAllComponents()
        assertEquals("> ", shell.prompt)
    }

    @Test
    fun `run iterates the given number of times`() {
        val shell = shellWithNetwork()
        assertEquals("Ran 3 of 3 iterations. Time: 3", shell.run("run 3"))
        assertEquals(3, shell.workspace.time)
    }

    @Test
    fun `run rejects a bad iteration count`() {
        val shell = shellWithNetwork()
        for (bad in listOf("0", "-1", "abc")) {
            assertEquals("Iterations must be a positive whole number: $bad", shell.run("run $bad"))
        }
        assertEquals(0, shell.workspace.time)
    }

    @Test
    fun `run with no count continues until stop`() {
        val shell = shellWithNetwork()
        assertEquals("Running. Enter stop to halt.", shell.run("run"))
        runBlocking {
            withTimeout(30_000) {
                while (shell.workspace.time < 2) delay(5)
                shell.execute("stop")
                while (shell.workspace.updater.isRunning) delay(5)
            }
        }
        assertFalse(shell.workspace.updater.isRunning)
    }

    @Test
    fun `stop ends a counted run early`() {
        val shell = shellWithNetwork()
        val output = runBlocking {
            withTimeout(30_000) {
                val job = async(Dispatchers.Default) { shell.execute("run 100000000") }
                while (shell.workspace.time < 2) delay(5)
                shell.execute("stop")
                job.await()
            }
        }
        assertTrue(output.startsWith("Ran "), output)
        assertTrue(shell.workspace.time < 100_000_000)
    }

    @Test
    fun `help lists commands and describes one`() {
        val shell = shellWithNetwork()
        val listing = shell.run("help")
        listOf("ls", "cd", "show", "run", "stop", "help").forEach { assertTrue(listing.contains(it), it) }
        assertTrue(shell.run("help ls").startsWith("ls [path]"))
    }

    @Test
    fun `a unique command name completes with a trailing space`() {
        assertEquals("show ", shellWithNetwork().complete("sh").line)
    }

    @Test
    fun `several matching commands are offered`() {
        val completion = CommandShell(Workspace(), registry = commands {
            command("alpha", description = "") { "" }
            command("alps", description = "") { "" }
        }).complete("a")
        assertEquals("alp", completion.line)
        assertEquals(listOf("alpha", "alps"), completion.options)
    }

    @Test
    fun `a component completes with a slash and then into its contents`() {
        val shell = shellWithNetwork()
        assertEquals("ls net1/", shell.complete("ls ne").line)
        val completion = shell.complete("show net1/Neu")
        assertEquals("show net1/Neuron_", completion.line)
        assertEquals(listOf("net1/Neuron_1", "net1/Neuron_2"), completion.options.sorted())
    }

    @Test
    fun `a name containing spaces completes in quotes and can be continued`() {
        val shell = shellWithNetwork("my net")
        assertEquals("ls \"my net/\"", shell.complete("ls my").line)
        assertEquals(2, shell.complete("ls \"my net/\"Neu").options.size)
    }

    @Test
    fun `commands without path arguments do not complete paths`() {
        assertEquals("run ne", shellWithNetwork().complete("run ne").line)
    }

    private fun CommandShell.network() = networkComponents.first().network

    private fun CommandShell.neuron(id: String) = network().getModels<Neuron>().first { it.id == id }

    @Test
    fun `a comment line produces no output`() {
        assertEquals("", shellWithNetwork().run("  # ls net1"))
    }

    @Test
    fun `set changes a number`() {
        val shell = shellWithNetwork()
        assertEquals("", shell.run("set net1/Neuron_2 activation=0.5"))
        assertEquals(0.5, shell.neuron("Neuron_2").activation)
    }

    @Test
    fun `set applies to everything a pattern matches`() {
        val shell = shellWithNetwork()
        assertEquals("Set 2 objects.", shell.run("set net1/Neuron_* activation=0.25"))
        assertTrue(shell.network().getModels<Neuron>().all { it.activation == 0.25 })
    }

    @Test
    fun `set replaces an update rule by name and reaches into it`() {
        val shell = shellWithNetwork()
        shell.run("set net1/Neuron_1 rule=Sigmoidal updateRule.slope=2")
        val rule = shell.neuron("Neuron_1").updateRule
        assertTrue(rule is SigmoidalRule, rule.toString())
        assertEquals(2.0, (rule as SigmoidalRule).slope)
    }

    @Test
    fun `set gives each object its own update rule`() {
        val shell = shellWithNetwork()
        shell.run("set net1/Neuron_* rule=Sigmoidal")
        assertNotSame(shell.neuron("Neuron_1").updateRule, shell.neuron("Neuron_2").updateRule)
    }

    @Test
    fun `set accepts a dialog label`() {
        val shell = shellWithNetwork()
        shell.run("set net1/Neuron_1 \"Update rule\"=Sigmoidal")
        assertTrue(shell.neuron("Neuron_1").updateRule is SigmoidalRule)
    }

    @Test
    fun `set handles booleans and enums`() {
        val shell = shellWithNetwork()
        shell.run("set net1/Neuron_1 clamped=true polarity=Excitatory")
        assertTrue(shell.neuron("Neuron_1").clamped)
        assertEquals("Excitatory", shell.run("get net1/Neuron_1 polarity"))
    }

    @Test
    fun `set reports an unknown property, a bad number, and an unknown type`() {
        val shell = shellWithNetwork()
        assertEquals(
            "Neuron has no property nope. Use show to see its properties.",
            shell.run("set net1/Neuron_1 nope=1")
        )
        assertEquals("activation needs a number: abc", shell.run("set net1/Neuron_1 activation=abc"))
        assertTrue(shell.run("set net1/Neuron_1 rule=zzz").startsWith("Unknown value for updateRule: zzz. Options:"))
    }

    @Test
    fun `get prints one value, or one per object`() {
        val shell = shellWithNetwork()
        assertEquals("0.75", shell.run("get net1/Neuron_1 activation"))
        assertEquals("Neuron_1: 0.75\nNeuron_2: 0", shell.run("get net1/Neuron_* activation"))
    }

    @Test
    fun `ls lists what a pattern matches`() {
        assertEquals("Neuron_1  Neuron_2", shellWithNetwork().run("ls net1/Neuron_*"))
    }

    @Test
    fun `ls filters with where`() {
        val shell = shellWithNetwork()
        assertEquals("Neuron_1", shell.run("ls net1 where activation>0.5"))
        assertEquals("Neuron_1", shell.run("ls net1 where activation > 0.5"))
        assertEquals("Synapse_1", shell.run("ls net1 where type=Synapse"))
        assertEquals("(empty)", shell.run("ls net1 where activation>5"))
    }

    @Test
    fun `a bad condition is reported`() {
        assertEquals("Nothing has a property called oops. Use show to see property names.", shellWithNetwork().run("ls net1 where oops"))
    }

    @Test
    fun `at refers to the selection`() {
        val workspace = Workspace()
        val component = NetworkComponent("net1")
        val neuron = Neuron().apply { activation = 0.5 }
        runBlocking { component.network.addNetworkModels(neuron) }
        workspace.addWorkspaceComponent(component)
        val shell = CommandShell(workspace, selectedModels = { listOf(neuron) })
        assertEquals("0.5", shell.run("get @ activation"))
        shell.run("set @ activation=1")
        assertEquals(1.0, neuron.activation)
    }

    @Test
    fun `at reports an empty selection`() {
        assertEquals("Nothing is selected.", shellWithNetwork().run("show @"))
    }

    @Test
    fun `add creates a named component`() {
        val shell = CommandShell(Workspace())
        assertEquals("Added net1.", shell.run("add network net1"))
        assertTrue(shell.workspace.getComponent("net1") is NetworkComponent)
    }

    @Test
    fun `add refuses a duplicate name and an unknown type`() {
        val shell = shellWithNetwork()
        assertEquals("There is already a component named net1.", shell.run("add network net1"))
        assertTrue(shell.run("add zzz").startsWith("Unknown type: zzz. Types: neurons, collection"))
    }

    @Test
    fun `add neurons uses the only network and applies options`() {
        val shell = shellWithNetwork()
        assertEquals("Added Neuron_3 to Neuron_5.", shell.run("add neurons 3 rule=Sigmoidal"))
        assertEquals(5, shell.network().getModels<Neuron>().size)
        assertTrue(shell.neuron("Neuron_4").updateRule is SigmoidalRule)
    }

    @Test
    fun `add neurons adds nothing when an option is bad`() {
        val shell = shellWithNetwork()
        shell.run("add neurons 3 nope=1")
        assertEquals(2, shell.network().getModels<Neuron>().size)
    }

    @Test
    fun `add neurons asks which network when there are several`() {
        val shell = shellWithNetwork()
        shell.run("add network net2")
        assertEquals("Which network? Use cd or in=<network>.", shell.run("add neurons 2"))
        shell.run("add neurons 2 in=net2")
        assertEquals(2, (shell.workspace.getComponent("net2") as NetworkComponent).network.getModels<Neuron>().size)
    }

    @Test
    fun `add neurons reports a missing network`() {
        assertEquals("There is no network. Use add network.", CommandShell(Workspace()).run("add neurons 2"))
    }

    @Test
    fun `a collection can be labelled, reached by label, and given a rule`() {
        val shell = shellWithNetwork()
        assertEquals("Added NeuronCollection_1 with 3 neuron(s).", shell.run("add collection 3 label=input rule=Sigmoidal"))
        assertEquals(3, shell.run("ls net1/input").split("  ").size)
        assertTrue(shell.network().getModels<NeuronCollection>().single().neuronList.all { it.updateRule is SigmoidalRule })
    }

    @Test
    fun `connect links two neurons with the given strength`() {
        val shell = shellWithNetwork()
        assertEquals("Added 1 synapse(s).", shell.run("connect net1/Neuron_2 net1/Neuron_1 strength=0.5"))
        val synapse = shell.neuron("Neuron_1").fanInUnsafe.single()
        assertEquals(0.5, synapse.strength)
    }

    @Test
    fun `connect makes a synapse group between collections`() {
        val shell = shellWithNetwork()
        shell.run("add collection 2 label=a")
        shell.run("add collection 3 label=b")
        assertEquals("Added SynapseGroup_1 with 6 synapse(s).", shell.run("connect net1/a net1/b"))
    }

    @Test
    fun `connect accepts a strategy`() {
        val shell = shellWithNetwork()
        shell.run("add collection 3 label=a")
        shell.run("add collection 3 label=b")
        assertEquals("Added SynapseGroup_1 with 3 synapse(s).", shell.run("connect net1/a net1/b onetoone"))
    }

    @Test
    fun `couple, couplings, and uncouple manage couplings`() {
        val shell = shellWithNetwork()
        shell.run("add network net2")
        shell.run("add neurons 1 in=net2")
        assertEquals("Neuron_1 > Neuron_1", shell.run("couple net1/Neuron_1 net2/Neuron_1"))
        assertEquals("1  Neuron_1 > Neuron_1", shell.run("couplings"))
        assertEquals("Removed 1 coupling(s).", shell.run("uncouple 1"))
        assertEquals("(none)", shell.run("couplings"))
    }

    @Test
    fun `a neuron can be coupled to a time series`() {
        val shell = shellWithNetwork()
        shell.run("add timeseries plot1")
        shell.run("couple net1/Neuron_1 \"plot1/Series 1\"")
        assertEquals(1, shell.workspace.couplingManager.couplings.size)
    }

    @Test
    fun `couple can name attributes`() {
        val shell = shellWithNetwork()
        shell.run("add network net2")
        shell.run("add neurons 1 in=net2")
        shell.run("couple net1/Neuron_1:activation net2/Neuron_1:addInputValue")
        val coupling = shell.workspace.couplingManager.couplings.single()
        assertEquals("getActivation", coupling.producer.method.name)
        assertEquals("addInputValue", coupling.consumer.method.name)
    }

    @Test
    fun `attrs lists attributes and couple reports an unknown one`() {
        val shell = shellWithNetwork()
        assertTrue(shell.run("attrs net1/Neuron_1").contains("activation"))
        assertEquals("Neuron_1 has no attribute nope. See attrs.", shell.run("couple net1/Neuron_1:nope net1/Neuron_2"))
    }

    @Test
    fun `rm removes models and components`() {
        val shell = shellWithNetwork()
        assertEquals("Removed 1 object(s).", shell.run("rm net1/Synapse_1"))
        assertEquals(0, shell.network().getModels<Synapse>().size)
        shell.run("rm net1")
        assertEquals("(empty)", shell.run("ls"))
    }

    @Test
    fun `randomize and clear change activations`() {
        val shell = shellWithNetwork()
        assertEquals("Cleared 1 object(s).", shell.run("clear net1"))
        assertEquals(0.0, shell.neuron("Neuron_1").activation)
        assertEquals("Randomized 1 model(s).", shell.run("randomize net1/Neuron_1"))
        assertNotEquals(0.0, shell.neuron("Neuron_1").activation)
    }

    @Test
    fun `sims lists simulations and sim needs the desktop`() {
        val shell = shellWithNetwork()
        assertTrue(shell.run("sims xor").contains("XOR"))
        assertEquals("Simulations can only be opened from the desktop.", shell.run("sim XOR"))
    }

    @Test
    fun `examples shows one of each command, or all for one command`() {
        val shell = shellWithNetwork()
        val all = shell.run("examples")
        assertTrue(all.contains("# List the components in the workspace\nls\n\n# Go into net1"), all)
        assertTrue(shell.run("tour").endsWith(tour.joinToString("\n\n")))
        assertEquals(workspaceCommands["set"]!!.examples.joinToString("\n\n"), shell.run("examples set"))
    }

    @Test
    fun `the tour runs in order in an empty workspace`() {
        val shell = CommandShell(Workspace())
        runBlocking {
            for (line in tour.flatMap { it.lines }) {
                try {
                    withTimeout(30_000) { shell.executeOrThrow(line) }
                } catch (e: CommandException) {
                    throw AssertionError("$line: ${e.message}")
                }
            }
        }
        shell.workspace.stop()
    }

    @Test
    fun `help marks command names bold for the terminal`() {
        val shell = shellWithNetwork()
        val styled = runBlocking { shell.executeStyled("help") }
        assertTrue(styled.contains("  ${bold("ls")} [path]"), styled)
        assertEquals("ls [path]", plainText(bold("ls") + " [path]"))
    }

    @Test
    fun `cls calls back to clear the screen`() {
        var cleared = false
        val shell = shellWithNetwork().apply { onClearScreen = { cleared = true } }
        shell.run("cls")
        assertTrue(cleared)
    }

    @Test
    fun `set completes property names and then values`() {
        val shell = shellWithNetwork()
        assertEquals("set net1/Neuron_1 activation=", shell.complete("set net1/Neuron_1 activ").line)
        assertEquals("set net1/Neuron_1 updateRule=Sigmoidal ", shell.complete("set net1/Neuron_1 updateRule=Sigmoidal").line)
    }

    @Test
    fun `get completes facts as well as properties`() {
        val shell = shellWithNetwork()
        assertEquals("get net1/Neuron_1 fanOut ", shell.complete("get net1/Neuron_1 fanO").line)
        assertEquals("set net1/Neuron_1 fanO", shell.complete("set net1/Neuron_1 fanO").line)
    }

    @Test
    fun `where completes names from what the path contains`() {
        val shell = shellWithNetwork()
        assertEquals("ls net1 where fanOut", shell.complete("ls net1 where fanO").line)
        val options = shell.complete("ls net1 where ").options
        assertTrue("activation" in options && "strength" in options && "type" in options && "not" in options, "$options")
        assertEquals("set net1/* strength=0 where (fanOut", shell.complete("set net1/* strength=0 where (fanO").line)
    }

    @Test
    fun `where completes values after equals`() {
        val shell = shellWithNetwork()
        assertEquals(listOf("type=Neuron", "type=Synapse"), shell.complete("ls net1 where type=").options.sorted())
        assertEquals("ls net1 where type=Synapse ", shell.complete("ls net1 where type=Syn").line)
        assertEquals("ls net1 where type = Synapse ", shell.complete("ls net1 where type = Syn").line)
        assertEquals("ls net1 where clamped=true ", shell.complete("ls net1 where clamped=t").line)
    }

    @Test
    fun `where completes and or after a comparison`() {
        val shell = shellWithNetwork()
        assertEquals(listOf("and", "or"), shell.complete("ls net1 where activation>0.5 ").options)
        assertEquals("ls net1 where fanIn > 0 and ", shell.complete("ls net1 where fanIn > 0 a").line)
        assertEquals("ls net1 where activation>0.5", shell.complete("ls net1 where activation>0.5").line)
    }

    @Test
    fun `add completes types`() {
        assertEquals("add network ", shellWithNetwork().complete("add netw").line)
    }

    @Test
    fun `grep filters the lines of show`() {
        val output = shellWithNetwork().run("show net1/Neuron_1 | grep activation")
        assertEquals("  activation: 0.75", output)
    }

    @Test
    fun `grep ignores case and accepts several words`() {
        assertEquals("  activation: 0.75", shellWithNetwork().run("show net1/Neuron_1 | grep ACTIVATION: 0"))
    }

    @Test
    fun `ls is one item per line when piped`() {
        assertEquals("Neuron_1\nNeuron_2", shellWithNetwork().run("ls net1 | grep Neuron"))
    }

    @Test
    fun `grep works without spaces around the pipe`() {
        assertEquals("Synapse_1", shellWithNetwork().run("ls net1|grep Syn"))
    }

    @Test
    fun `a pipe inside quotes is an ordinary character`() {
        assertEquals(listOf("set", "x", "label=a|b"), tokenize("set x \"label=a|b\""))
    }

    @Test
    fun `only grep can follow a pipe`() {
        val shell = shellWithNetwork()
        for (line in listOf("ls | more", "ls | grep", "ls | grep a | grep b")) {
            assertEquals("The only pipe is | grep <text>.", shell.run(line), line)
        }
    }

    @Test
    fun `select fires a selection on the model's network`() {
        val shell = shellWithNetwork()
        val selections = CopyOnWriteArrayList<List<NetworkModel>>()
        shell.network().events.selected.on(Dispatchers.Unconfined) { selections.add(it) }
        assertEquals("Selected 2 object(s).", shell.run("select net1/Neuron_*"))
        runBlocking { withTimeout(30_000) { while (selections.isEmpty()) delay(5) } }
        assertEquals(listOf("Neuron_1", "Neuron_2"), selections.single().mapNotNull { it.id }.sorted())
    }

    @Test
    fun `select rejects something that is not a model`() {
        assertEquals("net1 can't be selected.", shellWithNetwork().run("select net1"))
    }

    @Test
    fun `select and at reach models inside a subnetwork`() {
        val workspace = Workspace()
        val component = NetworkComponent("net1")
        val hopfield = Hopfield(3)
        runBlocking { component.network.addNetworkModels(hopfield) }
        workspace.addWorkspaceComponent(component)
        val inner = hopfield.neuronGroup.neuronList.first()
        val shell = CommandShell(workspace, selectedModels = { listOf(inner) })
        val selections = CopyOnWriteArrayList<List<NetworkModel>>()
        component.network.events.selected.on(Dispatchers.Unconfined) { selections.add(it) }

        assertEquals("Selected 3 object(s).", shell.run("select net1/${hopfield.id}/${hopfield.neuronGroup.id}/*"))
        assertEquals("Selected 1 object(s).", shell.run("select @"))
        runBlocking { withTimeout(30_000) { while (selections.size < 2) delay(5) } }
        assertEquals(3, selections[0].size)
        assertEquals(listOf(inner), selections[1])
    }

    @Test
    fun `adding a component with a bad option leaves nothing behind`() {
        val shell = CommandShell(Workspace())
        assertTrue(shell.run("add network net1 foo=1").startsWith("Network has no property foo"))
        assertEquals("(empty)", shell.run("ls"))
        assertEquals("Added net1.", shell.run("add network net1"))
    }

    @Test
    fun `types hidden from dialogs are hidden here too`() {
        val shell = shellWithNetwork()
        val output = shell.run("set net1/Neuron_1 rule=Allostatic")
        assertTrue(output.startsWith("Unknown value for updateRule: Allostatic."), output)
        assertFalse(shell.complete("set net1/Neuron_1 rule=").options.any { it.contains("Allostatic") })
    }

    private fun shellWithTwoNetworks(): CommandShell {
        val shell = shellWithNetwork()
        val second = NetworkComponent("net2")
        runBlocking { second.network.addNetworkModels(Neuron().apply { activation = 0.25 }) }
        shell.workspace.addWorkspaceComponent(second)
        return shell
    }

    @Test
    fun `a bare name is found wherever it is`() {
        val shell = shellWithNetwork()
        assertEquals("0.75", shell.run("get Neuron_1 activation"))
        shell.run("set Synapse_1 strength=2")
        assertEquals("2", shell.run("get net1/Synapse_1 strength"))
    }

    @Test
    fun `a name in several places is reported with its places`() {
        assertEquals(
            "Neuron_1 is in 2 places: net1/Neuron_1, net2/Neuron_1. Use a longer path.",
            shellWithTwoNetworks().run("get Neuron_1 activation")
        )
    }

    @Test
    fun `the current location is searched before everywhere`() {
        val shell = shellWithTwoNetworks()
        shell.run("cd net2")
        assertEquals("0.25", shell.run("get Neuron_1 activation"))
        assertEquals("0.75", shell.run("get net1/Neuron_1 activation"))
    }

    @Test
    fun `a name matches ignoring case and punctuation`() {
        assertEquals("0.75", shellWithNetwork().run("get neuron1 activation"))
    }

    @Test
    fun `a component is found from inside another`() {
        val shell = shellWithTwoNetworks()
        shell.run("cd net1")
        assertEquals("Neuron_1", shell.run("ls net2"))
    }

    @Test
    fun `a neuron in a collection is one object, not two places`() {
        val shell = shellWithNetwork()
        shell.run("add collection 2 in=net1 label=group")
        val inCollection = shell.run("ls net1/group").split("  ").first()
        assertEquals("0", shell.run("get $inCollection activation"))
    }

    @Test
    fun `a range stands for each of its values`() {
        val shell = shellWithNetwork()
        shell.run("add neurons 3 in=net1")
        assertEquals("Set 3 objects.", shell.run("set net1/Neuron_{2..4} activation=0.5"))
        assertEquals("Neuron_1: 0.75\nNeuron_2: 0.5\nNeuron_3: 0.5\nNeuron_4: 0.5\nNeuron_5: 0", shell.run("get net1/Neuron_* activation"))
        assertEquals("Neuron_1: 0.75\nNeuron_5: 0", shell.run("get Neuron_{1,5} activation"))
    }

    @Test
    fun `two stars match at any depth`() {
        val shell = shellWithTwoNetworks()
        assertEquals("Neuron_1  Neuron_2  Neuron_1", shell.run("ls **/Neuron_*"))
    }

    @Test
    fun `a pattern that matches nothing suggests looking everywhere`() {
        assertEquals("Not found: Neuron_*. To look everywhere, use **/Neuron_*.", shellWithNetwork().run("ls Neuron_*"))
    }

    @Test
    fun `braces expand like a shell`() {
        assertEquals(listOf("n1", "n2", "n3"), expandBraces("n{1..3}"))
        assertEquals(listOf("n3", "n2"), expandBraces("n{3..2}"))
        assertEquals(listOf("a/x1", "a/x2", "b/x1", "b/x2"), expandBraces("{a,b}/x{1..2}"))
        assertEquals(listOf("plain{x}"), expandBraces("plain{x}"))
        assertTrue(shellWithNetwork().run("ls Neuron_{1..1000000}").contains("too large"))
    }

    @Test
    fun `a long command reports progress`() {
        val shell = shellWithNetwork()
        shell.run("add neurons 1000 in=net1")
        shell.run("set net1/Neuron_* activation=1")
        val progress = shell.progress!!
        assertEquals("Setting", progress.what)
        assertEquals(1002, progress.total)
    }

    @Test
    fun `help groups commands`() {
        val listing = shellWithNetwork().run("help")
        assertTrue(listing.startsWith("Inspect\n  ls"), listing)
        listOf("Build", "Run", "Other").forEach { assertTrue(it in listing, it) }
    }

    @Test
    fun `help on a command shows examples as comments and commands`() {
        assertTrue(shellWithNetwork().run("help ls").contains("# List the components in the workspace\nls\n\n# List what is in net1\nls net1"))
    }

    @Test
    fun `every example line is a known command or a comment`() {
        for (command in workspaceCommands.all) {
            assertTrue(command.examples.isNotEmpty(), command.name)
            for (example in command.examples) {
                val firstWords = example.toString().lines().mapNotNull { tokenize(it).firstOrNull() }
                firstWords.forEach { assertTrue(workspaceCommands[it] != null, "$it in $example") }
                assertTrue(command.name in firstWords, example.toString())
            }
        }
    }

    @Test
    fun `every example runs in a small workspace`() {
        val needsDesktop = setOf("sim")
        for (command in workspaceCommands.all.filter { it.name !in needsDesktop }) {
            for (example in command.examples) {
                val workspace = Workspace()
                val component = NetworkComponent("net1")
                val first = Neuron().apply { activation = 0.75 }
                val second = Neuron()
                runBlocking { component.network.addNetworkModels(first, second, Synapse(first, second)) }
                workspace.addWorkspaceComponent(component)
                val shell = CommandShell(workspace, selectedModels = { listOf(first) })
                runBlocking {
                    shell.executeOrThrow("couple net1/Neuron_1:activation net1/Neuron_2:addInputValue")
                    for (line in example.lines) {
                        try {
                            withTimeout(30_000) { shell.executeOrThrow(line) }
                        } catch (e: CommandException) {
                            throw AssertionError("$line in \"${example.description}\": ${e.message}")
                        }
                    }
                }
                workspace.stop()
                runBlocking { withTimeout(30_000) { while (workspace.updater.isRunning) delay(5) } }
            }
        }
    }

    @Test
    fun `stop entered right after run stops the workspace`() {
        repeat(40) {
            val shell = shellWithNetwork()
            shell.run("run")
            shell.run("stop")
            // Long enough for a run that had not started when stop arrived to start
            runBlocking { delay(50) }
            assertFalse(shell.workspace.updater.isRunning)
        }
    }

    @Test
    fun `changing what is already there counts as unsaved work`() {
        val edits = listOf(
            "set net1/Neuron_1 activation=1",
            "set net1/Synapse_1 strength=2",
            "randomize net1",
            "clear net1/Neuron_1",
            "uncouple all"
        )
        for (line in edits) {
            val shell = shellWithNetwork()
            shell.run("couple net1/Neuron_1:activation net1/Neuron_2:addInputValue")
            shell.workspace.markClean()
            assertFalse(shell.workspace.hasUnsavedWork(), line)
            shell.run(line)
            assertTrue(shell.workspace.hasUnsavedWork(), line)
        }
    }

    @Test
    fun `help where describes conditions`() {
        val help = shellWithNetwork().run("help where")
        assertTrue(help.startsWith("where <condition>"), help)
        assertTrue("ls net1 where label=\"hidden layer\"" in help, help)
    }

    @Test
    fun `a condition after a command that takes none is reported`() {
        assertEquals("add can't be used with where.", shellWithNetwork().run("add neurons 2 where activation>0"))
    }

    @Test
    fun `the where examples run in a small workspace`() {
        val shell = shellWithNetwork()
        shell.run("set net1/Synapse_1 strength=0.9")
        runBlocking { whereExamples.flatMap { it.lines }.forEach { shell.executeOrThrow(it) } }
    }

    @Test
    fun `a where value containing spaces can be quoted`() {
        val shell = shellWithNetwork()
        shell.run("set net1/Neuron_1 label=\"my neuron\"")
        assertEquals("Neuron_1", shell.run("ls net1 where label=\"my neuron\""))
        assertEquals("Neuron_1", shell.run("ls net1 where label = 'my neuron'"))
        assertEquals("Neuron_1", shell.run("ls net1 where \"label='my neuron'\""))
        assertEquals("Neuron_1", shell.run("ls net1 where label=\"my neuron\" | grep Neuron"))
        assertEquals("Neuron_1", shell.run("ls net1 where label=\"my neuron\" # the labelled one"))
    }

    @Test
    fun `set changes nothing when a value is bad or some objects lack the property`() {
        val shell = shellWithNetwork()
        assertEquals("bias needs a number: oops", shell.run("set net1/Neuron_* activation=0.2 bias=oops"))
        assertEquals("Neuron_1: 0.75\nNeuron_2: 0", shell.run("get net1/Neuron_* activation"))
        assertEquals(
            "Synapse has no property activation. Use show to see its properties.",
            shell.run("set net1/* activation=0.2")
        )
        assertEquals("Neuron_1: 0.75\nNeuron_2: 0", shell.run("get net1/Neuron_* activation"))
    }

    @Test
    fun `a range of ranges that stands for too many names is refused`() {
        assertEquals(
            "{1..99999}{1..99999} stands for too many names.",
            shellWithNetwork().run("ls {1..99999}{1..99999}")
        )
    }
}
