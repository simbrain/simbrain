/**
 * The commands available at the Simbrain command line. To add one, add a `command` block to [workspaceCommands].
 */
package org.simbrain.console

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.simbrain.custom_sims.simulations
import org.simbrain.network.NetworkComponent
import org.simbrain.network.connections.AllToAll
import org.simbrain.network.connections.ConnectionStrategy
import org.simbrain.network.connections.connectionTypes
import org.simbrain.network.core.Network
import org.simbrain.network.core.NetworkModel
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.NeuronCollection
import org.simbrain.network.core.Synapse
import org.simbrain.network.core.SynapseGroup
import org.simbrain.network.core.topLeftLocation
import org.simbrain.network.gui.subnetworkProtectedModels
import org.simbrain.network.layouts.GridLayout
import org.simbrain.network.layouts.Layout
import org.simbrain.util.callNoArgConstructor
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.workspace.Attribute
import org.simbrain.workspace.AttributeContainer
import org.simbrain.workspace.Consumer
import org.simbrain.workspace.Producer
import org.simbrain.workspace.Workspace
import org.simbrain.workspace.WorkspaceComponent
import org.simbrain.workspace.attributeTypesMatch
import org.simbrain.workspace.couplings.Coupling
import java.io.File

private const val MAX_SHOWN = 50
private const val WORKSPACE_EXTENSION = "zip"
private val NEURON_TYPE_WORDS = listOf("neurons", "collection")

val workspaceCommands: CommandRegistry = commands {

    command(
        name = "ls",
        group = "Inspect",
        examples = listOf(
            Example("List the components in the workspace", "ls"),
            Example("List what is in net1", "ls net1"),
            Example("List the neurons in net1", "ls net1/Neuron_*"),
            Example("List neurons anywhere in the workspace", "ls **/Neuron_*"),
            Example("List the neurons in net1 whose activation is above 0.5", "ls net1 where activation>0.5"),
            Example("List the synapses in net1", "ls net1 where type=Synapse"),
            Example("List the neurons in net1 that nothing connects to", "ls net1 where type=Neuron and fanIn=0"),
            Example("List what is in net1, keeping lines that mention Synapse", "ls net1 | grep Synapse")
        ),
        usage = "ls [path] [where condition]",
        description = "List contents, or what matches a pattern.",
        listsItems = true,
        takesWhere = true,
        completer = pathCompletion
    ) { args ->
        val nodes = applyWhere(listedNodes(args))
        if (nodes.isEmpty()) "(empty)" else nodes.joinToString("\n") { quoted(it.name) }
    }

    command(
        name = "cd",
        group = "Inspect",
        examples = listOf(
            Example("Go into net1, so that paths start there", "cd net1"),
            Example("Go up one level", "cd .."),
            Example("Go back to the workspace", "cd")
        ),
        usage = "cd [path]",
        description = "Change the current location. With no path, return to the workspace.",
        completer = pathCompletion
    ) { args ->
        if (args.size > 1) fail("Expected one path. Put names containing spaces in quotes.")
        val node = resolve(args.firstOrNull() ?: "/")
        if (childrenOf(node.value) == null) fail("${node.name} has no contents to enter.")
        changeLocation(node)
        ""
    }

    command(
        name = "show",
        group = "Inspect",
        takesWhere = true,
        examples = listOf(
            Example("Describe the workspace", "show"),
            Example("Show the properties of a neuron", "show net1/Neuron_1"),
            Example("Show what is selected in the GUI", "show @"),
            Example("Show only the lines about a neuron's update rule", "show net1/Neuron_1 | grep rule")
        ),
        usage = "show [path] [where condition]",
        description = "Show the properties of one or more objects.",
        completer = pathCompletion
    ) { args ->
        val nodes = if (args.isEmpty()) listOf(current) else targets(onePath(args))
        val shown = nodes.take(MAX_SHOWN).joinToString("\n\n") { describe(it) }
        if (nodes.size > MAX_SHOWN) "$shown\n\n... and ${nodes.size - MAX_SHOWN} more" else shown
    }

    command(
        name = "get",
        group = "Inspect",
        takesWhere = true,
        examples = listOf(
            Example("Print a neuron's activation", "get net1/Neuron_1 activation"),
            Example("Print the activation of every neuron in net1", "get net1/Neuron_* activation"),
            Example("Print the strength of every synapse in net1", "get net1/* strength where type=Synapse")
        ),
        usage = "get <path> <property> [where condition]",
        description = "Print one property of one or more objects.",
        completer = { prior, partial ->
            if (prior.isEmpty()) pathCandidates(partial) else propertyCandidates(prior.first(), partial, "")
        }
    ) { args ->
        if (args.size != 2) fail("Usage: get <path> <property>")
        val nodes = targets(args[0])
        val values = buildList {
            forEachWithProgress(nodes, "Reading") { add(it to (readValue(it, args[1]) ?: fail("${it.name} has no property ${args[1]}."))) }
        }
        values.singleOrNull()?.second?.let(::formatValue)
            ?: values.joinToString("\n") { (node, value) -> "${node.name}: ${formatValue(value)}" }
    }

    command(
        name = "select",
        group = "Inspect",
        takesWhere = true,
        examples = listOf(
            Example("Select the neurons in net1", "select net1/Neuron_*"),
            Example("Make a collection labelled input, then select it by its label", "add collection 3 in=net1 label=input", "select net1/input"),
            Example("Select the neurons in net1 whose activation is above 0.5", "select net1/* where activation>0.5"),
            Example("Clear the selection", "select none")
        ),
        usage = "select <path or none> [where condition]",
        description = "Select models in the GUI, replacing the current selection. Refer to them afterwards with @.",
        completer = pathCompletion
    ) { args ->
        val word = onePath(args)
        val nodes = if (word == "none") emptyList() else targets(word)
        nodes.firstOrNull { it.value !is NetworkModel }?.let { fail("${it.name} can't be selected.") }
        val byNetwork = nodes.groupBy { networkOf(it) }
        networkComponents.forEach { component ->
            component.network.selectModels(byNetwork[component.network].orEmpty().map { it.value as NetworkModel })
        }
        if (word == "none") "" else "Selected ${nodes.size} object(s)."
    }

    command(
        name = "set",
        group = "Build",
        examples = listOf(
            Example("Set a neuron's activation", "set net1/Neuron_1 activation=1"),
            Example("Make the neurons in net1 sigmoidal, then change a setting of the new rule", "set net1/Neuron_* rule=Sigmoidal rule.slope=2"),
            Example("Clamp what is selected in the GUI", "set @ clamped=true"),
            Example("Give two neurons their own random activations between 0 and 10", "set net1/Neuron_{1..2} activation=rand()*10"),
            Example("Give every synapse in net1 a small random strength", "set net1/* strength=randn(0,0.1) where type=Synapse"),
            Example("Spread the activations in net1 evenly from 0 up toward 1", "set net1/Neuron_* activation=i/n"),
            Example("Double every activation in net1", "set net1/Neuron_* activation=activation*2")
        ),
        usage = "set <path> key=value ... [where condition]",
        description = "Set properties. Selectable types are set by name, and their settings use dots. A number can be " +
                "an expression, worked out for each object: use + - * / ^, rand(), rand(a,b), randn(mean,sd), abs, " +
                "sqrt, exp, log, min, max, round, the object's own properties, i for its position from 0, and n for " +
                "how many there are. Leave out spaces, or put the expression in quotes.",
        takesWhere = true,
        completer = { prior, partial ->
            when {
                prior.isEmpty() -> pathCandidates(partial)
                "=" in partial -> valueCandidates(prior.first(), partial)
                else -> propertyCandidates(prior.first(), partial, "=")
            }
        }
    ) { args ->
        val parsed = Arguments.parse(args)
        if (parsed.words.size != 1 || parsed.options.isEmpty()) fail("Usage: set <path> key=value ...")
        val nodes = targets(parsed.words.single())
        var index = 0
        forEachWithProgress(nodes, "Setting") { node ->
            val context = expressionContext(node, index++, nodes.size)
            parsed.options.forEach { (key, value) -> assign(editableObject(node), key, value, context) }
        }
        if (nodes.size == 1) "" else "Set ${nodes.size} objects."
    }

    command(
        name = "add",
        group = "Build",
        examples = listOf(
            Example("Add a network named net2", "add network net2"),
            Example("Add ten sigmoidal neurons to net1", "add neurons 10 in=net1 rule=Sigmoidal"),
            Example("Add five neurons with random activations", "add neurons 5 in=net1 activation=rand()"),
            Example("Add a collection of five neurons in a line, labelled input", "add collection 5 in=net1 label=input layout=line"),
            Example("Add a time series plot named plot2", "add timeseries plot2"),
            Example("Build a small network from scratch and give it random activations", "add network demo", "add neurons 10 in=demo", "set demo/Neuron_* activation=rand()")
        ),
        usage = "add <type> [count or name] [in=path] [layout=type] [key=value ...]",
        description = "Add neurons, a collection, or a component.",
        completer = { prior, partial -> if (prior.isEmpty()) wordCandidates(addableTypes(), partial) else emptyList() }
    ) { args ->
        val parsed = Arguments.parse(args)
        val type = parsed.words.firstOrNull() ?: fail("Usage: add <type> [count or name]. Types: ${addableTypes().joinToString(", ")}")
        when (normalized(type)) {
            "neuron", "neurons" -> addNeurons(parsed)
            "collection" -> addCollection(parsed)
            else -> addComponent(type, parsed)
        }
    }

    command(
        name = "connect",
        group = "Build",
        takesWhere = true,
        examples = listOf(
            Example("Connect two neurons with a strength of 0.5", "connect net1/Neuron_1 net1/Neuron_2 strength=0.5"),
            Example("Connect the neurons in net1 to each other sparsely, with random strengths", "connect net1/Neuron_* net1/Neuron_* sparse strength=randn(0,0.5)"),
            Example("Make two collections and join them with a synapse group", "add collection 3 in=net1 label=left", "add collection 3 in=net1 label=right", "connect net1/left net1/right")
        ),
        usage = "connect <source> <target> [strategy] [key=value ...]",
        description = "Connect neurons or collections. Strategies: " +
                typeOptions<ConnectionStrategy>(connectionTypes).keys.joinToString(", "),
        completer = { prior, partial ->
            if (prior.size < 2) pathCandidates(partial)
            else wordCandidates(typeOptions<ConnectionStrategy>(connectionTypes).keys, partial)
        }
    ) { args -> connect(Arguments.parse(args)) }

    command(
        name = "couple",
        group = "Build",
        takesWhere = true,
        examples = listOf(
            Example("Make a plot and send a neuron's activation to it", "add timeseries plot1", "couple net1/Neuron_1 \"plot1/Series 1\""),
            Example("Send one neuron's activation to another neuron's input", "couple net1/Neuron_1:activation net1/Neuron_2:addInputValue")
        ),
        usage = "couple <producer[:attribute]> <consumer[:attribute]>",
        description = "Couple objects so that values flow between them each iteration. See attrs for attribute names.",
        completer = pathCompletion
    ) { args ->
        if (args.size != 2) fail("Usage: couple <producer[:attribute]> <consumer[:attribute]>")
        couple(args[0], args[1])
    }

    command(
        name = "couplings",
        group = "Inspect",
        examples = listOf(
            Example("List the couplings, numbered for uncouple", "couplings")
        ),
        description = "List the couplings in the workspace."
    ) { _ ->
        workspace.couplingManager.couplings
            .mapIndexed { index, coupling -> "${index + 1}  ${coupling.id}" }
            .joinToString("\n")
            .ifEmpty { "(none)" }
    }

    command(
        name = "uncouple",
        group = "Build",
        examples = listOf(
            Example("Remove the first coupling listed by couplings", "uncouple 1"),
            Example("Remove every coupling", "uncouple all")
        ),
        usage = "uncouple <number or all>",
        description = "Remove a coupling, using its number from couplings."
    ) { args ->
        val manager = workspace.couplingManager
        val word = args.singleOrNull() ?: fail("Usage: uncouple <number or all>")
        val toRemove: List<Coupling> = if (word == "all") {
            manager.couplings
        } else {
            listOf(word.toIntOrNull()?.let { manager.couplings.getOrNull(it - 1) } ?: fail("No coupling numbered $word."))
        }
        manager.removeCouplings(toRemove)
        "Removed ${toRemove.size} coupling(s)."
    }

    command(
        name = "attrs",
        group = "Inspect",
        examples = listOf(
            Example("See what a neuron can send to and receive from other components", "attrs net1/Neuron_1")
        ),
        usage = "attrs <path>",
        description = "List what an object can produce and consume, for couple.",
        completer = pathCompletion
    ) { args ->
        val node = resolve(onePath(args))
        val container = attributeContainer(node)
        with(workspace.couplingManager) {
            "Produces: ${container.producers.joinToString(", ") { attributeName(it) }}\n" +
                    "Consumes: ${container.consumers.joinToString(", ") { attributeName(it) }}"
        }
    }

    command(
        name = "rm",
        group = "Build",
        takesWhere = true,
        examples = listOf(
            Example("Remove a synapse", "rm net1/Synapse_1"),
            Example("Remove every synapse in net1", "rm net1/* where type=Synapse"),
            Example("Remove a whole component", "rm net1")
        ),
        usage = "rm <path> [where condition]",
        description = "Remove models or components. There is no undo.",
        completer = pathCompletion
    ) { args -> remove(targets(onePath(args))) }

    command(
        name = "randomize",
        group = "Build",
        takesWhere = true,
        examples = listOf(
            Example("Randomize everything in net1", "randomize net1"),
            Example("Randomize one neuron", "randomize net1/Neuron_1"),
            Example("Randomize what is selected in the GUI", "randomize @")
        ),
        usage = "randomize <path> [where condition]",
        description = "Randomize models, or everything in a network.",
        completer = pathCompletion
    ) { args ->
        val models = modelsIn(targets(onePath(args)))
        models.forEach { it.randomize() }
        "Randomized ${models.size} model(s)."
    }

    command(
        name = "clear",
        group = "Build",
        takesWhere = true,
        examples = listOf(
            Example("Clear the activations in net1", "clear net1"),
            Example("Clear a neuron, wherever it is", "clear Neuron_1"),
            Example("Clear what is selected in the GUI", "clear @")
        ),
        usage = "clear <path> [where condition]",
        description = "Clear models, or the activations in a network.",
        completer = pathCompletion
    ) { args ->
        var cleared = 0
        for (node in targets(onePath(args))) {
            when (val value = node.value) {
                is NetworkComponent -> value.network.clearActivations()
                is NetworkModel -> value.clear()
                else -> fail("${node.name} can't be cleared.")
            }
            cleared++
        }
        "Cleared $cleared object(s)."
    }

    command(
        name = "run",
        group = "Run",
        examples = listOf(
            Example("Run for 100 iterations", "run 100"),
            Example("Run until stopped. Enter stop to halt", "run")
        ),
        usage = "run [iterations]",
        description = "Run the workspace for a number of iterations, or until stop if none is given."
    ) { args ->
        if (args.size > 1) fail("Usage: run [iterations]")
        if (workspace.updater.isRunning) fail("Already running. Enter stop first.")
        if (args.isEmpty()) {
            workspace.run()
            "Running. Enter stop to halt."
        } else {
            val iterations = args.single().toIntOrNull()?.takeIf { it > 0 }
                ?: fail("Iterations must be a positive whole number: ${args.single()}")
            var remaining = iterations
            val context = currentCoroutineContext()
            workspace.iterateWhile {
                progress = Progress("Running", iterations - remaining + 1, iterations)
                --remaining > 0 && workspace.updater.isRunning && context.isActive
            }
            "Ran ${iterations - remaining} of $iterations iterations. Time: ${workspace.time}"
        }
    }

    command(
        name = "stop",
        group = "Run",
        examples = listOf(
            Example("Stop running", "stop")
        ),
        description = "Stop the workspace after the current iteration. Runs at once, even while another command is running.",
        immediate = true
    ) { _ ->
        workspace.stop()
        "Stopped. Time: ${workspace.time}"
    }

    command(
        name = "sims",
        group = "Run",
        examples = listOf(
            Example("List every simulation", "sims"),
            Example("List the simulations whose names mention xor", "sims xor")
        ),
        usage = "sims [text]",
        description = "List simulations, optionally those whose names contain some text."
    ) { args ->
        val text = args.joinToString(" ")
        simulations.items.map { it.first }.filter { it.contains(text, ignoreCase = true) }.toList()
            .joinToString("\n").ifEmpty { "(none)" }
    }

    command(
        name = "sim",
        group = "Run",
        examples = listOf(
            Example("Open the XOR simulation, discarding unsaved changes", "sim XOR force"),
            Example("Open the XOR simulation and look at its layers", "sim XOR force", "ls XOR where type=NeuronCollection", "get XOR/** activation where type=Neuron")
        ),
        usage = "sim <name> [force]",
        description = "Open a simulation, replacing the current workspace. force discards unsaved changes. A Console " +
                "window closes with the workspace, so use the Terminal tab at the bottom of the desktop to keep going.",
        completer = { _, partial -> wordCandidates(simulations.items.map { it.first }.toList(), partial) }
    ) { args ->
        val theDesktop = desktop ?: fail("Simulations can only be opened from the desktop.")
        val force = args.size > 1 && args.last() == "force"
        val nameArgs = if (force) args.dropLast(1) else args
        if (nameArgs.isEmpty()) fail("Usage: sim <name> [force]")
        val sim = chooseOption(simulations.items.toMap(), nameArgs.joinToString(" "), "simulation")
        requireSafeToReplace(force, "sim <name> force")
        withContext(Dispatchers.Default) { sim.run(theDesktop) }
        ""
    }

    command(
        name = "open",
        group = "Files",
        examples = listOf(
            Example("Open a workspace file", "open mywork.zip"),
            Example("Open a workspace file, discarding unsaved changes", "open mywork.zip force")
        ),
        usage = "open <file> [force]",
        description = "Open a workspace file, replacing the current workspace. force discards unsaved changes. A " +
                "Console window closes with the workspace, so use the Terminal tab at the bottom of the desktop to keep going."
    ) { args ->
        if (args.isEmpty() || args.size > 2 || (args.size == 2 && args[1] != "force")) fail("Usage: open <file> [force]")
        val file = workspaceFile(args[0])
        if (!file.isFile) fail("No such file: ${file.path}")
        requireSafeToReplace(force = args.size == 2, "open <file> force")
        workspace.openWorkspace(file, useDesktop = desktop != null)
        "Opened ${file.name}."
    }

    command(
        name = "save",
        group = "Files",
        examples = listOf(
            Example("Save to the file the workspace came from", "save"),
            Example("Save to a new file", "save mywork.zip"),
            Example("Save over an existing file", "save mywork.zip force")
        ),
        usage = "save [file] [force]",
        description = "Save the workspace. force replaces an existing file."
    ) { args ->
        val force = args.lastOrNull() == "force"
        val pathArgs = if (force) args.dropLast(1) else args
        if (pathArgs.size > 1) fail("Usage: save [file] [force]")
        val file = pathArgs.firstOrNull()?.let(::workspaceFile)
            ?: workspace.currentFile
            ?: fail("This workspace has no file yet. Use save <file>.")
        if (file.exists() && file != workspace.currentFile && !force) fail("${file.name} exists. Use save <file> force.")
        if (file.absoluteFile.parentFile?.isDirectory == false) fail("No such folder: ${file.absoluteFile.parent}")
        if (!workspace.save(file, headless = desktop == null)) fail("Could not save ${file.path}.")
        workspace.currentFile = file
        "Saved ${file.name}."
    }

    command(
        name = "source",
        group = "Files",
        examples = listOf(
            Example("Run the commands in a text file, one per line", "source build.txt")
        ),
        usage = "source <file>",
        description = "Run the commands in a text file. Lines starting with # are comments."
    ) { args ->
        val file = File(expandHome(args.singleOrNull() ?: fail("Usage: source <file>")))
        if (!file.isFile) fail("No such file: ${file.path}")
        runFile(file)
    }

    command(
        name = "cls",
        group = "Other",
        examples = listOf(
            Example("Clear the screen", "cls")
        ),
        description = "Clear the screen."
    ) { _ ->
        onClearScreen()
        ""
    }

    command(
        name = "help",
        group = "Other",
        examples = listOf(
            Example("List the commands", "help"),
            Example("Describe ls, with examples", "help ls")
        ),
        usage = "help [command]",
        description = "List commands, or describe one.",
        completer = commandCompletion
    ) { args ->
        if (args.isEmpty()) {
            registry.all.groupBy { it.group }.entries.joinToString("\n\n") { (group, members) ->
                group + "\n" + members.joinToString("\n") { "  ${it.usage}" }
            } + "\n\nEnter help <command> for details and examples. Paths look like net1/Neuron_1, and a name by " +
                    "itself is found wherever it is. Use .. to go up, / for the workspace, * and ? to match several, " +
                    "** to match at any depth, {1..5} for a range, and @ for what is selected in the GUI. End a line with " +
                    "where <condition> to act only on some objects, and | grep <text> to filter output. Numbers can be " +
                    "expressions, as in rand()*10 (see help set). Tab completes, and Esc cancels a long command."
        } else {
            val command = registry[args.first()] ?: fail("Unknown command: ${args.first()}")
            val examples = command.examples.joinToString("\n\n") { it.toString() }
            "${command.usage}\n${command.description}" + if (examples.isEmpty()) "" else "\n\nExamples, which can be copied and pasted:\n\n$examples"
        }
    }
}

/** The words of a command line, separated from its `key=value` options. */
private class Arguments(val words: List<String>, val options: Map<String, String>) {
    companion object {
        fun parse(args: List<String>): Arguments {
            val (options, words) = args.partition { "=" in it }
            return Arguments(words, options.associate { it.substringBefore("=") to it.substringAfter("=") })
        }
    }
}

private fun onePath(args: List<String>): String =
    args.singleOrNull() ?: fail("Expected one path. Put names containing spaces in quotes.")

private suspend fun CommandShell.listedNodes(args: List<String>): List<Node> {
    val path = when (args.size) {
        0 -> null
        1 -> args.single()
        else -> fail("Expected one path. Put names containing spaces in quotes.")
    }
    if (path == "@") return selection()
    val found = if (path == null) listOf(current) else resolveAll(path).ifEmpty { fail(notFound(path)) }
    if (found.size > 1 || path?.let { '*' in it || '?' in it || '{' in it } == true) return found
    val node = found.single()
    val children = childrenOf(node.value) ?: fail("${node.name} has no contents. Use show to see its properties.")
    return children.map { (name, value) -> Node(node.path + name, value) }
}

private fun editableObject(node: Node): Any = (node.value as? NetworkComponent)?.network ?: node.value

private fun describe(node: Node): String {
    val value = node.value
    val lines = mutableListOf("${node.name} (${value::class.simpleName})")
    propertyLines(editableObject(node), indent = "  ", nestedLevels = 1, lines)
    extraFacts(value).forEach { (key, fact) -> lines += "  $key: ${formatValue(fact)}" }
    return lines.joinToString("\n")
}

private fun propertyLines(obj: Any, indent: String, nestedLevels: Int, lines: MutableList<String>) {
    for (property in editablePropertiesOf(obj)) {
        lines += "$indent${property.name}: ${formatValue(property.value)}"
        val nested = property.value
        if (nestedLevels > 0 && nested is EditableObject) {
            propertyLines(nested, "$indent  ", nestedLevels - 1, lines)
        }
    }
}

private fun extraFacts(value: Any): Map<String, Any> = when (value) {
    is Workspace -> linkedMapOf(
        "time" to value.time,
        "running" to value.updater.isRunning,
        "components" to value.componentList.size
    )
    is NetworkComponent -> value.network.allModels.groupingBy { it::class.simpleName.orEmpty() }.eachCount()
    is WorkspaceComponent -> mapOf("contents" to (childrenOf(value)?.size ?: 0))
    is Neuron -> linkedMapOf("fanIn" to value.fanInUnsafe.size, "fanOut" to value.fanOutUnsafe.size)
    is Synapse -> linkedMapOf("source" to value.source.id.orEmpty(), "target" to value.target.id.orEmpty())
    else -> emptyMap()
}

/** The value of a name in a `where` condition or an expression about [node], or [Expression.MISSING]. */
fun valueOf(node: Node, name: String): Any? {
    if (name.equals("type", ignoreCase = true)) return node.value::class.simpleName
    val value = propertyValue(editableObject(node), name)
    if (value !== Expression.MISSING) return value
    return extraFacts(node.value)[name] ?: Expression.MISSING
}

private fun expressionContext(node: Node, index: Int, count: Int) = ExpressionContext(index, count) { valueOf(node, it) }

private fun readValue(node: Node, key: String): Any? =
    findProperty(editableObject(node), key)?.let { it.value ?: "none" } ?: extraFacts(node.value)[key]

private fun CommandShell.propertyCandidates(path: String, partial: String, suffix: String): List<Candidate> {
    val node = resolveQuietly(path).firstOrNull() ?: return emptyList()
    return editablePropertiesOf(editableObject(node))
        .filter { it.name.startsWith(partial, ignoreCase = true) }
        .map { Candidate(it.name, suffix.ifEmpty { " " }) }
}

private fun CommandShell.valueCandidates(path: String, partial: String): List<Candidate> {
    val node = resolveQuietly(path).firstOrNull() ?: return emptyList()
    val key = partial.substringBefore("=")
    val property = findProperty(editableObject(node), key) ?: return emptyList()
    return valueOptions(property)
        .filter { it.startsWith(partial.substringAfter("="), ignoreCase = true) }
        .map { Candidate("$key=$it") }
}

private fun CommandShell.addableTypes(): List<String> =
    NEURON_TYPE_WORDS + workspace.componentFactory.workspaceComponentTypes.map { normalized(it) }.sorted()

private suspend fun CommandShell.targetNetwork(inPath: String?): Network {
    if (inPath != null) {
        val node = resolve(inPath)
        return networkOf(node) ?: fail("${node.name} is not in a network.")
    }
    networkOf(current)?.let { return it }
    return networkComponents.singleOrNull()?.network
        ?: fail(if (networkComponents.isEmpty()) "There is no network. Use add network." else "Which network? Use cd or in=<network>.")
}

private fun countFrom(words: List<String>): Int {
    if (words.size > 1) fail("Expected a count.")
    val word = words.firstOrNull() ?: return 1
    return word.toIntOrNull()?.takeIf { it > 0 } ?: fail("Count must be a positive whole number: $word")
}

private fun layoutFrom(word: String?): Layout {
    if (word == null) return GridLayout()
    return chooseOption(typeOptions<Layout>(Layout.TYPE_LIST.filterIsInstance<Class<out Layout>>()), word, "layout")
        .kotlin.callNoArgConstructor()
}

private suspend fun CommandShell.addNeurons(arguments: Arguments): String {
    val options = arguments.options.toMutableMap()
    val network = targetNetwork(options.remove("in"))
    val layout = layoutFrom(options.remove("layout"))
    val count = countFrom(arguments.words.drop(1))
    val neurons = List(count) { index ->
        Neuron().apply {
            val context = ExpressionContext(index, count) { propertyValue(this, it) }
            options.forEach { (key, value) -> assign(this, key, value, context) }
        }
    }
    network.addNetworkModels(neurons)
    layout.setInitialLocation(neurons.topLeftLocation)
    layout.layoutNeurons(neurons)
    return "Added ${describeRange(neurons.map { it.id.orEmpty() })}."
}

private suspend fun CommandShell.addCollection(arguments: Arguments): String {
    val options = arguments.options.toMutableMap()
    val network = targetNetwork(options.remove("in"))
    val layout = layoutFrom(options.remove("layout"))
    val neurons = List(countFrom(arguments.words.drop(1))) { Neuron() }
    val collection = NeuronCollection(neurons)
    for ((key, value) in options) {
        if (findProperty(collection, key, exactOnly = true) != null) {
            assign(collection, key, value)
        } else {
            neurons.forEachIndexed { index, neuron ->
                assign(neuron, key, value, ExpressionContext(index, neurons.size) { propertyValue(neuron, it) })
            }
        }
    }
    network.addNetworkModels(neurons)
    network.addNetworkModel(collection)
    collection.layout = layout
    collection.applyLayout()
    return "Added ${collection.id} with ${neurons.size} neuron(s)."
}

private fun describeRange(ids: List<String>) = if (ids.size > 1) "${ids.first()} to ${ids.last()}" else ids.first()

private fun CommandShell.addComponent(type: String, arguments: Arguments): String {
    val types = workspace.componentFactory.workspaceComponentTypes.associateBy { it }
    val factoryName = try {
        chooseOption(types, type, "type")
    } catch (e: CommandException) {
        fail("Unknown type: $type. Types: ${addableTypes().joinToString(", ")}")
    }
    if (arguments.words.size > 2) fail("Expected one name. Put names containing spaces in quotes.")
    val name = arguments.words.getOrNull(1).orEmpty()
    if ("/" in name) fail("Names can't contain /.")
    if (name.isNotEmpty() && workspace.getComponent(name) != null) fail("There is already a component named $name.")
    val component = workspace.componentFactory.newWorkspaceComponent(factoryName)
    component.name = name
    arguments.options.forEach { (key, value) -> assign((component as? NetworkComponent)?.network ?: component, key, value) }
    workspace.addWorkspaceComponent(component)
    return "Added ${component.name}."
}

private suspend fun CommandShell.connect(arguments: Arguments): String {
    if (arguments.words.size !in 2..3) fail("Usage: connect <source> <target> [strategy] [key=value ...]")
    val sources = targets(arguments.words[0])
    val destinations = targets(arguments.words[1])
    val network = networkOf(sources.first()) ?: fail("${sources.first().name} is not in a network.")
    if (networkOf(destinations.first()) !== network) fail("Source and target must be in the same network.")

    val strategy = arguments.words.getOrNull(2)
        ?.let { chooseOption(typeOptions<ConnectionStrategy>(connectionTypes), it, "strategy").kotlin.callNoArgConstructor() }
        ?: AllToAll()
    val (strategyOptions, synapseOptions) = arguments.options.entries.partition { findProperty(strategy, it.key, exactOnly = true) != null }
    strategyOptions.forEach { (key, value) -> assign(strategy, key, value) }

    val sourceCollection = sources.singleOrNull()?.value as? NeuronCollection
    val targetCollection = destinations.singleOrNull()?.value as? NeuronCollection
    if (sourceCollection != null && targetCollection != null) {
        val group = SynapseGroup(sourceCollection, targetCollection, strategy)
        group.synapses.forEachIndexed { index, synapse -> configureSynapse(synapse, synapseOptions, index, group.synapses.size) }
        network.addNetworkModel(group)
        return "Added ${group.id} with ${group.synapses.size} synapse(s)."
    }
    val synapses = strategy.connectNeurons(neuronsIn(sources), neuronsIn(destinations))
    synapses.forEachIndexed { index, synapse -> configureSynapse(synapse, synapseOptions, index, synapses.size) }
    network.addNetworkModels(synapses)
    return "Added ${synapses.size} synapse(s)."
}

private fun configureSynapse(synapse: Synapse, options: List<Map.Entry<String, String>>, index: Int, count: Int) {
    val context = ExpressionContext(index, count) { propertyValue(synapse, it) }
    options.forEach { (key, value) -> assign(synapse, key, value, context) }
}

private fun neuronsIn(nodes: List<Node>): List<Neuron> = nodes.flatMap { node ->
    when (val value = node.value) {
        is Neuron -> listOf(value)
        is NeuronCollection -> value.neuronList
        else -> fail("${node.name} is not a neuron or collection.")
    }
}

private fun attributeContainer(node: Node): AttributeContainer =
    node.value as? AttributeContainer ?: fail("${node.name} can't be coupled. Use a neuron, synapse, plot series, or similar.")

private fun attributeName(attribute: Attribute): String {
    val method = attribute.method.name
    val stem = listOf("get", "set", "is").firstOrNull { method.startsWith(it) && method.length > it.length }
        ?.let { method.removePrefix(it) } ?: method
    return stem.replaceFirstChar { it.lowercase() }
}

private suspend fun CommandShell.couple(producerText: String, consumerText: String): String {
    val (producerPath, producerAttribute) = splitEndpoint(producerText)
    val (consumerPath, consumerAttribute) = splitEndpoint(consumerText)
    val producers = targets(producerPath).map(::attributeContainer)
    val consumers = targets(consumerPath).map(::attributeContainer)
    val pairs = when {
        producers.size == consumers.size -> producers.zip(consumers)
        producers.size == 1 -> consumers.map { producers.single() to it }
        consumers.size == 1 -> producers.map { it to consumers.single() }
        else -> fail("Can't pair ${producers.size} producers with ${consumers.size} consumers.")
    }
    val couplings = pairs.map { (producer, consumer) -> coupleOne(producer, producerAttribute, consumer, consumerAttribute) }
    return couplings.singleOrNull()?.id ?: "Added ${couplings.size} couplings."
}

private fun splitEndpoint(text: String): Pair<String, String?> =
    if (":" in text) text.substringBeforeLast(":") to text.substringAfterLast(":") else text to null

private fun CommandShell.coupleOne(
    producingContainer: AttributeContainer,
    producerAttribute: String?,
    consumingContainer: AttributeContainer,
    consumerAttribute: String?
): Coupling = with(workspace.couplingManager) {
    val producerOptions: List<Producer> = producingContainer.producers.toList().named(producerAttribute, producingContainer)
    val consumerOptions: List<Consumer> = consumingContainer.consumers.toList().named(consumerAttribute, consumingContainer)
    val (producer, consumer) = producerOptions
        .flatMap { p -> consumerOptions.map { c -> p to c } }
        .filter { (p, c) -> attributeTypesMatch(p.type, c.type) }
        .minByOrNull { (p, c) -> p.priority + c.priority + (if (p.type == String::class.java) 1 else 0) }
        ?: fail("No compatible attributes between ${producingContainer.id} and ${consumingContainer.id}. See attrs.")
    createCoupling(producer, consumer)
}

private fun <T : Attribute> List<T>.named(attribute: String?, container: AttributeContainer): List<T> {
    if (attribute == null) return this
    val wanted = normalized(attribute)
    return filter { normalized(attributeName(it)) == wanted }
        .ifEmpty { filter { wanted in normalized(attributeName(it)) } }
        .ifEmpty { fail("${container.id} has no attribute $attribute. See attrs.") }
}

private suspend fun CommandShell.remove(nodes: List<Node>): String {
    val components = nodes.mapNotNull { it.value as? WorkspaceComponent }
    val modelNodes = nodes.filter { it.value is NetworkModel }
    nodes.firstOrNull { it.value !is WorkspaceComponent && it.value !is NetworkModel }?.let { fail("${it.name} can't be removed.") }
    for ((network, group) in modelNodes.groupBy { networkOf(it) ?: fail("${it.name} is not in a network.") }) {
        val models = group.map { it.value as NetworkModel }
        val protectedModels = network.subnetworkProtectedModels(models)
        if (protectedModels.isNotEmpty()) fail("${protectedModels.first().displayName} is part of a larger structure and can't be removed by itself.")
        network.deleteModels(models)
    }
    components.forEach { it.close() }
    return "Removed ${nodes.size} object(s)."
}

private fun modelsIn(nodes: List<Node>): List<NetworkModel> = nodes.flatMap { node ->
    when (val value = node.value) {
        is NetworkComponent -> value.network.allModels.toList()
        is NetworkModel -> listOf(value)
        else -> fail("${node.name} has no network models.")
    }
}

/**
 * Before the workspace is replaced: unless [force] is given, fails if this is a Console window, which would close, or
 * if work would be lost. Then stops the workspace.
 */
private fun CommandShell.requireSafeToReplace(force: Boolean, forceUsage: String) {
    if (!force && inWorkspaceWindow) {
        fail("This would close this console window, which is part of the workspace. Use the Terminal tab at the " +
                "bottom of the desktop, which stays open, or use $forceUsage.")
    }
    workspace.stop()
    if (!force && workspace.componentList.isNotEmpty() && workspace.hasUnsavedWork()) {
        fail("There are unsaved changes. Save first, or use $forceUsage.")
    }
}

private fun expandHome(path: String) =
    if (path == "~" || path.startsWith("~/")) System.getProperty("user.home") + path.drop(1) else path

private fun workspaceFile(path: String): File {
    val file = File(expandHome(path))
    return if (file.extension.isEmpty()) File(file.path + "." + WORKSPACE_EXTENSION) else file
}
