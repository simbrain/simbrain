/**
 * The Simbrain command language: a line is a command name followed by words and `key=value` options, and the workspace
 * is navigated like a filesystem of components and their contents. A bare name is found wherever it is, as long as it
 * is in only one place. Paths may use `*`, `?`, `**`, and shell-style `{1..10}` ranges, and `@` stands for what is
 * selected in the GUI. A `where` condition at the end of a line filters what a command acts on, and numeric values
 * can be expressions evaluated for each object (see `Expressions.kt`). Output can be filtered with `| grep`. There
 * are deliberately no variables or loops.
 *
 * [CommandShell] parses lines, tracks the current location, resolves paths, and completes partial input. The commands
 * themselves are declared with [commands] in `WorkspaceCommands.kt`.
 */
package org.simbrain.console

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Network
import org.simbrain.network.core.NetworkModel
import org.simbrain.network.core.NeuronCollection
import org.simbrain.network.core.SynapseGroup
import org.simbrain.network.desktop.NetworkDesktopComponent
import org.simbrain.network.subnetworks.Subnetwork
import org.simbrain.workspace.Workspace
import org.simbrain.workspace.WorkspaceComponent
import org.simbrain.workspace.gui.SimbrainDesktop
import java.lang.reflect.InvocationTargetException
import java.util.IdentityHashMap
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.math.abs

private const val MAX_LISTED = 500
private const val MAX_PLACES = 5
private const val MAX_EXPANSION = 100_000
private const val PROGRESS_INTERVAL = 256
private const val SEARCH_REPORT_INTERVAL = 1024

/** A problem with what the user entered. The message is shown as is. */
class CommandException(message: String) : Exception(message)

fun fail(message: String): Nothing = throw CommandException(message)

/**
 * A possible completion.
 *
 * @param text what to insert
 * @param suffix what follows it when it is the only match: a space to start the next word, `/` to continue a path, or
 * `=` to supply a value
 */
class Candidate(val text: String, val suffix: String = " ")

/** Offers completions for a word, given the words before it. */
typealias Completer = CommandShell.(priorArgs: List<String>, partial: String) -> List<Candidate>

val noCompletion: Completer = { _, _ -> emptyList() }

/**
 * Offers completions for a word in a `where` condition, given the command's arguments before `where` and the words of
 * the condition before the one being completed.
 */
typealias ConditionCompleter = CommandShell.(args: List<String>, priorWords: List<String>, partial: String) -> List<Candidate>
val pathCompletion: Completer = { _, partial -> pathCandidates(partial) }
val commandCompletion: Completer = { _, partial -> wordCandidates(registry.all.map { it.name }, partial) }

/**
 * An example in help: what it does, then lines that can be copied and pasted back to run it. The description is shown
 * as a comment, so it can be copied along with the lines.
 */
class Example(val description: String, vararg lines: String) {
    val lines: List<String> = lines.toList()

    override fun toString() = plainText(styled())

    /** The description as a comment, then the commands in bold, as they appear when typed. */
    fun styled() = (listOf("# $description") + lines.map(::bold)).joinToString("\n")
}

private const val BOLD_START = '\u0001'
private const val BOLD_END = '\u0002'

/** Marks text to be shown in bold by a terminal. [CommandShell.execute] removes the marks. */
fun bold(text: String) = "$BOLD_START$text$BOLD_END"

fun plainText(styled: String) = styled.filterNot { it == BOLD_START || it == BOLD_END }

/** The pieces of text marked with [bold], each with whether it is bold. */
fun styledSegments(styled: String): List<Pair<String, Boolean>> =
    Regex("$BOLD_START([^$BOLD_END]*)$BOLD_END|[^$BOLD_START]+").findAll(styled).map { match ->
        if (match.value.startsWith(BOLD_START)) match.groupValues[1] to true else match.value to false
    }.toList()

/**
 * @param group the heading it appears under in help
 * @param listsItems if true, the action returns one item per line, which are shown side by side unless piped
 * @param immediate if true, the command runs as soon as it is entered instead of waiting for earlier commands, as
 * `stop` must
 * @param takesWhere whether a `where` condition at the end of the line filters what the command acts on
 */
class Command(
    val name: String,
    val usage: String,
    val description: String,
    val group: String,
    val examples: List<Example>,
    val listsItems: Boolean,
    val immediate: Boolean,
    val takesWhere: Boolean,
    val completer: Completer,
    val action: suspend CommandShell.(args: List<String>) -> String
)

class CommandRegistry {

    private val commandsByName = linkedMapOf<String, Command>()

    val all: Collection<Command> get() = commandsByName.values

    operator fun get(name: String): Command? = commandsByName[name]

    var conditionCompleter: ConditionCompleter = { _, _, _ -> emptyList() }

    fun command(
        name: String,
        usage: String = name,
        description: String,
        group: String = "",
        examples: List<Example> = emptyList(),
        listsItems: Boolean = false,
        immediate: Boolean = false,
        takesWhere: Boolean = false,
        completer: Completer = noCompletion,
        action: suspend CommandShell.(args: List<String>) -> String
    ) {
        commandsByName[name] = Command(name, usage, description, group, examples, listsItems, immediate, takesWhere, completer, action)
    }
}

fun commands(block: CommandRegistry.() -> Unit) = CommandRegistry().apply(block)

/** A location in the workspace: the names leading to it and the object found there. */
class Node(val path: List<String>, val value: Any) {
    val name: String get() = path.lastOrNull() ?: "Workspace"
}

/**
 * @param line the line after completion
 * @param options the possible completions, when there is more than one
 */
class Completion(val line: String, val options: List<String>)

/**
 * @param desktop the desktop, if there is one. Commands that need a GUI report that they cannot run without it.
 * @param selectedModels what `@` refers to
 */
class CommandShell(
    val workspace: Workspace,
    val desktop: SimbrainDesktop? = null,
    val registry: CommandRegistry = workspaceCommands,
    private val selectedModels: suspend () -> List<NetworkModel> = { desktopSelection(workspace, desktop) }
) {

    @Volatile
    private var currentPath: List<String> = emptyList()

    /**
     * Set for a shell in a Console window, which is part of the workspace and closes when the workspace is replaced.
     * Commands that replace the workspace are sent here instead, to run in the desktop's Terminal tab.
     */
    var relayToTerminal: ((String) -> Unit)? = null

    /** Called by the `cls` command. */
    var onClearScreen: () -> Unit = {}

    /** How far the running command has got, if it reports progress. Cleared by whoever starts a command. */
    @Volatile
    var progress: Progress? = null

    private val root get() = Node(emptyList(), workspace)

    val prompt: String get() = current.path.joinToString("/") + "> "

    /** The current location, which falls back to the workspace if what it pointed to has been removed or renamed. */
    val current: Node
        get() = walk(root, currentPath).singleOrNull() ?: root.also { currentPath = emptyList() }

    /**
     * Run a line and return the text to show for it, which is empty when there is nothing to say. Problems with the
     * line or the command are returned as text as well.
     */
    suspend fun execute(line: String): String = plainText(executeStyled(line))

    /** Like [execute], but parts of the text that should be bold are marked with [bold]. */
    suspend fun executeStyled(line: String): String {
        return try {
            runLine(line)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            problemText(e)
        }
    }

    /** Run a line, throwing a [CommandException] for a problem instead of returning it as text. */
    internal suspend fun executeOrThrow(line: String): String = runLine(line)

    /** Whether [line] is a command that should not wait for earlier ones. */
    fun isImmediate(line: String): Boolean =
        runCatching { parseLine(line).words.firstOrNull()?.let { registry[it]?.immediate } }.getOrNull() == true

    private fun problemText(e: Exception): String {
        if (e is CommandException) return e.message.orEmpty()
        val cause = (e as? InvocationTargetException)?.targetException ?: e
        return "Error: ${cause.message ?: cause::class.simpleName}"
    }

    private suspend fun runLine(line: String): String {
        val parsed = parseLine(line)
        val name = parsed.words.firstOrNull() ?: return ""
        val command = registry[name] ?: fail("Unknown command: $name. Enter help for a list.")
        // For a command that takes no condition, where as the last word is an ordinary word, as in help where
        val whereAt = parsed.words.indexOf("where").takeIf { it > 0 && (command.takesWhere || it < parsed.words.lastIndex) }
        val args = parsed.words.subList(1, whereAt ?: parsed.words.size)
        val condition = whereAt?.let { at ->
            if (!command.takesWhere) fail("$name can't be used with where.")
            val text = parsed.conditionAfter(at).ifBlank { fail("Expected a condition after where.") }
            WhereCondition(Expression.parse(text))
        }
        val output = withContext(condition ?: EmptyCoroutineContext) { command.action(this@CommandShell, args) }
        return when {
            parsed.filter != null -> output.lines().filter { plainText(it).contains(parsed.filter, ignoreCase = true) }.joinToString("\n")
            command.listsItems -> sideBySide(output.lines())
            else -> output
        }
    }

    private fun sideBySide(items: List<String>): String {
        val shown = items.take(MAX_LISTED).joinToString("  ")
        return if (items.size > MAX_LISTED) "$shown\n... and ${items.size - MAX_LISTED} more. Narrow with a longer path or where." else shown
    }

    /** Find the one thing a path refers to. */
    suspend fun resolve(path: String): Node {
        val found = resolveAll(path)
        return when (found.size) {
            0 -> fail(notFound(path))
            1 -> found.single()
            else -> fail("$path matches ${found.size} things. Expected one.")
        }
    }

    /**
     * Find everything a path refers to. A path starting with `/` is from the workspace, any other from [current]. A
     * name that is not found there is looked for everywhere, and must then be in only one place. `*` and `?` match
     * within one level, `**` matches any number of levels, and `{1..3}` or `{a,b}` stands for each of its values.
     */
    suspend fun resolveAll(path: String): List<Node> = find(path, searchTick())

    /** Like [resolveAll] but empty rather than failing, and not cancellable. For completion. */
    fun resolveQuietly(path: String): List<Node> = runCatching { find(path) {} }.getOrDefault(emptyList())

    /**
     * Run [action] on each item, reporting progress and letting the command be cancelled. For commands that may act
     * on many objects.
     */
    suspend fun <T> forEachWithProgress(items: List<T>, what: String, action: suspend (T) -> Unit) {
        items.forEachIndexed { index, item ->
            if (index % PROGRESS_INTERVAL == 0) {
                yield()
                progress = Progress(what, index, items.size)
            }
            action(item)
        }
    }

    private fun find(path: String, tick: () -> Unit): List<Node> {
        val lookup = Lookup(tick)
        val start = current
        val paths = expandBraces(path)
        val local = paths.flatMap { walkPath(start, it, lookup) }
        if (local.isNotEmpty() || !searchesEverywhere(path)) return distinctValues(local)
        return distinctValues(paths.flatMap { searchEverywhere(it, lookup) })
    }

    private fun walkPath(start: Node, path: String, lookup: Lookup): List<Node> {
        lookup.tick()
        return walk(if (path.startsWith("/")) root else start, path.split("/").filter { it.isNotEmpty() }, lookup)
    }

    /** Whether a path not found from the current location is looked for everywhere: a relative path starting with a name. */
    private fun searchesEverywhere(path: String): Boolean {
        val first = path.split("/").firstOrNull { it.isNotEmpty() } ?: return false
        return !path.startsWith("/") && first !in listOf(".", "..", "**") && !isPattern(first)
    }

    private fun searchEverywhere(path: String, lookup: Lookup): List<Node> {
        lookup.tick()
        val segments = path.split("/").filter { it.isNotEmpty() }
        val starts = distinctValues(lookup.everythingNamed(segments.first()) { descendants(root, lookup) })
        val found = starts.map { walk(it, segments.drop(1), lookup) }.filter { it.isNotEmpty() }
        if (found.size > 1) {
            val places = found.map { it.first().path.joinToString("/") }
            fail("$path is in ${places.size} places: ${places.take(MAX_PLACES).joinToString(", ")}" +
                    (if (places.size > MAX_PLACES) ", ..." else "") + ". Use a longer path.")
        }
        return found.singleOrNull().orEmpty()
    }

    /** Counts the objects a search visits, reporting progress and stopping if the command is cancelled. */
    private suspend fun searchTick(): () -> Unit {
        val job = currentCoroutineContext()[Job]
        var searched = 0
        return {
            if (++searched % SEARCH_REPORT_INTERVAL == 0) {
                job?.ensureActive()
                progress = Progress("Searching", searched)
            }
        }
    }

    fun notFound(path: String) =
        if (isPattern(path) && !path.startsWith("/") && !path.startsWith("**")) {
            "Not found: $path. To look everywhere, use **/$path."
        } else {
            "Not found: $path"
        }

    /**
     * Find what a command acts on: what a path refers to, or the GUI selection for `@`, filtered by the command's
     * `where` condition if it has one.
     */
    suspend fun targets(path: String): List<Node> {
        val found = if (path == "@") selection() else resolveAll(path).ifEmpty { fail(notFound(path)) }
        return applyWhere(found).ifEmpty { fail("Nothing matches the condition.") }
    }

    /** The models selected in the GUI. */
    suspend fun selection(): List<Node> {
        val selected = selectedModels().ifEmpty { fail("Nothing is selected.") }
        val nodesByModel = IdentityHashMap<Any, Node>()
        networkComponents.forEach { component ->
            descendants(Node(listOf(component.name), component)).forEach { nodesByModel.putIfAbsent(it.value, it) }
        }
        return selected.map { model -> nodesByModel[model] ?: Node(listOf(model.id.orEmpty()), model) }
    }

    /** Keep the nodes that meet the running command's `where` condition, or all of them if there is none. */
    suspend fun applyWhere(nodes: List<Node>): List<Node> {
        val condition = currentCoroutineContext()[WhereCondition]?.expression ?: return nodes
        val found = ConcurrentHashMap.newKeySet<String>()
        val kept = buildList {
            forEachWithProgress(nodes, "Checking") { node ->
                val context = ExpressionContext { name -> valueOf(node, name).also { if (it !== Expression.MISSING) found += name } }
                if (condition.test(context)) add(node)
            }
        }
        val unknown = condition.missingNames - found
        if (kept.isEmpty() && unknown.isNotEmpty()) fail("Nothing has a property called ${unknown.first()}. Use show to see property names.")
        return kept
    }

    /**
     * Everything below [node], each before its own contents. A neuron in a collection appears under both.
     *
     * @param tick called for each object visited
     */
    fun descendants(node: Node, tick: () -> Unit = {}): Sequence<Node> = descendants(node, Lookup(tick))

    private fun descendants(node: Node, lookup: Lookup): Sequence<Node> = sequence {
        val children = lookup.contentsOf(node.value) ?: return@sequence
        for ((name, child) in children) {
            lookup.tick()
            val childNode = Node(node.path + name, child)
            yield(childNode)
            yieldAll(descendants(childNode, lookup))
        }
    }

    val networkComponents: List<NetworkComponent>
        get() = workspace.componentList.filterIsInstance<NetworkComponent>()

    /** The network that a node is in, if any. */
    fun networkOf(node: Node): Network? =
        networkComponents.firstOrNull { it.name == node.path.firstOrNull() }?.network

    fun changeLocation(node: Node) {
        currentPath = node.path
    }

    /** Complete the last word of [line]: a command name if it is the first word, otherwise as its command specifies. */
    fun complete(line: String): Completion {
        val scanned = scan(line)
        if (scanned.pipes.isNotEmpty()) return Completion(line, emptyList())
        val tokenStart = scanned.lastTokenStart ?: line.length
        val partial = if (scanned.lastTokenStart != null) scanned.tokens.last() else ""
        val priorTokens = if (scanned.lastTokenStart != null) scanned.tokens.dropLast(1) else scanned.tokens

        val command = priorTokens.firstOrNull()?.let { registry[it] }
        val whereAt = priorTokens.indexOf("where")
        val candidates = when {
            priorTokens.isEmpty() -> wordCandidates(registry.all.map { it.name }, partial)
            command == null -> emptyList()
            command.takesWhere && whereAt > 0 ->
                registry.conditionCompleter(this, priorTokens.subList(1, whereAt), priorTokens.drop(whereAt + 1), partial)
            else -> command.completer(this, priorTokens.drop(1), partial)
        }

        val head = line.substring(0, tokenStart)
        return when (candidates.size) {
            0 -> Completion(line, emptyList())
            1 -> {
                val only = candidates.single()
                val completed = if (only.suffix.isBlank()) quoted(only.text) + only.suffix else quoted(only.text + only.suffix)
                Completion(head + completed, emptyList())
            }
            else -> {
                val shared = candidates.map { it.text }.reduce { a, b -> a.commonPrefixWith(b, ignoreCase = true) }
                val completed = if (shared.length > partial.length) head + quoted(shared) else line
                Completion(completed, candidates.map { it.text })
            }
        }
    }

    fun wordCandidates(words: Collection<String>, partial: String) =
        words.filter { it.startsWith(partial, ignoreCase = true) }.map { Candidate(it) }

    fun pathCandidates(partial: String): List<Candidate> {
        val prefix = partial.substring(0, partial.lastIndexOf('/') + 1)
        val parent = if (prefix.isEmpty()) current else resolveQuietly(prefix).singleOrNull() ?: return emptyList()
        val children = childrenOf(parent.value) ?: return emptyList()
        return children
            .filterKeys { it.startsWith(partial.removePrefix(prefix), ignoreCase = true) }
            .map { (name, child) -> Candidate(prefix + name, if (childrenOf(child) != null) "/" else " ") }
    }

    private fun walk(start: Node, segments: List<String>, lookup: Lookup = Lookup()): List<Node> =
        segments.fold(listOf(start)) { nodes, segment -> nodes.flatMap { step(it, segment, lookup) } }

    private fun step(node: Node, segment: String, lookup: Lookup): List<Node> {
        if (segment == ".") return listOf(node)
        if (segment == "..") return walk(root, node.path.dropLast(1), lookup)
        if (segment == "**") return listOf(node) + descendants(node, lookup)
        val children = lookup.contentsOf(node.value) ?: return emptyList()
        val names = if (isPattern(segment)) {
            val pattern = globToRegex(segment)
            children.keys.filter { pattern.matches(it) }
        } else {
            listOfNotNull(findChildName(children, segment))
        }
        return names.map { Node(node.path + it, children.getValue(it)) }
    }

    /** Prefers an exact match, then a match ignoring case, then a label, then a match ignoring punctuation. */
    private fun findChildName(children: Map<String, Any>, segment: String): String? =
        segment.takeIf { it in children }
            ?: children.keys.filter { it.equals(segment, ignoreCase = true) }.singleOrNull()
            ?: children.filterValues { (it as? NetworkModel)?.label.equals(segment, ignoreCase = true) }.keys.singleOrNull()
            ?: children.keys.filter { looselyEqual(it, segment) }.singleOrNull()
}

/**
 * What one resolution of a path shares across the names the path stands for, so that a range such as `{1..1000}` reads
 * the contents of each object once instead of once per name.
 *
 * @param tick called for each object visited and each name tried
 */
private class Lookup(val tick: () -> Unit = {}) {

    private val contents = IdentityHashMap<Any, Map<String, Any>?>()

    private var names: NameIndex? = null

    fun contentsOf(value: Any): Map<String, Any>? =
        if (contents.containsKey(value)) contents[value] else childrenOf(value).also { contents[value] = it }

    /** The nodes among [everything] that [segment] names, in the order they were found. */
    fun everythingNamed(segment: String, everything: () -> Sequence<Node>): List<Node> =
        (names ?: NameIndex(everything()).also { names = it }).matching(segment)
}

/**
 * Nodes by the names they can be found under: their name or label ignoring case, or their name ignoring punctuation
 * as well, so that neuron1 finds Neuron_1.
 */
private class NameIndex(nodes: Sequence<Node>) {

    private val position = IdentityHashMap<Node, Int>()
    private val byName = HashMap<String, MutableList<Node>>()
    private val byLabel = HashMap<String, MutableList<Node>>()
    private val byLooseName = HashMap<String, MutableList<Node>>()

    init {
        for (node in nodes) {
            position[node] = position.size
            byName.getOrPut(node.name.lowercase()) { mutableListOf() } += node
            (node.value as? NetworkModel)?.label?.let { label -> byLabel.getOrPut(label.lowercase()) { mutableListOf() } += node }
            normalized(node.name).takeIf { it.isNotEmpty() }?.let { loose -> byLooseName.getOrPut(loose) { mutableListOf() } += node }
        }
    }

    fun matching(segment: String): List<Node> {
        val lowercase = segment.lowercase()
        return (byName[lowercase].orEmpty() + byLabel[lowercase].orEmpty() + byLooseName[normalized(segment)].orEmpty())
            .distinct()
            .sortedBy { position.getValue(it) }
    }
}

/** Equal ignoring case and punctuation, so that neuron1 matches Neuron_1. */
private fun looselyEqual(name: String, typed: String) = normalized(typed).let { it.isNotEmpty() && it == normalized(name) }

/** The `where` condition of the running command. */
private class WhereCondition(val expression: Expression) : AbstractCoroutineContextElement(WhereCondition) {
    companion object Key : CoroutineContext.Key<WhereCondition>
}

/**
 * How far a long command has got, for a progress display.
 *
 * @param total how many items there are, if known
 */
class Progress(val what: String, val done: Int, val total: Int? = null)

private fun isPattern(text: String) = '*' in text || '?' in text

/** One node for each object, the one with the shortest path, in the order they were found. */
private fun distinctValues(nodes: List<Node>): List<Node> {
    val best = IdentityHashMap<Any, Node>()
    for (node in nodes) {
        val previous = best[node.value]
        if (previous == null || node.path.size < previous.path.size) best[node.value] = node
    }
    return nodes.filter { best[it.value] === it }
}

private val bracePattern = Regex("""\{([^{}]*)}""")
private val rangePattern = Regex("""^(-?\d+)\.\.(-?\d+)$""")

/** Expand `{1..3}` and `{a,b}` into each of their values, as a shell does. */
fun expandBraces(text: String): List<String> {
    val match = bracePattern.find(text) ?: return listOf(text)
    val inside = match.groupValues[1]
    val range = rangePattern.find(inside)
    val values = when {
        range != null -> {
            val (from, to) = range.destructured.toList().map { it.toLong() }
            if (abs(to - from) >= MAX_EXPANSION) fail("The range {$inside} is too large.")
            (if (from <= to) from..to else from downTo to).map { it.toString() }
        }
        "," in inside -> inside.split(",")
        else -> return listOf(text)
    }
    val expanded = mutableListOf<String>()
    for (value in values) {
        expanded += expandBraces(text.replaceRange(match.range, value))
        if (expanded.size > MAX_EXPANSION) fail("$text stands for too many names.")
    }
    return expanded
}

private suspend fun desktopSelection(workspace: Workspace, desktop: SimbrainDesktop?): List<NetworkModel> {
    if (desktop == null) return emptyList()
    return workspace.componentList.filterIsInstance<NetworkComponent>().filter { it.isGuiOn }.flatMap { component ->
        (desktop.getDesktopComponent(component) as NetworkDesktopComponent).networkPanel.selectionManager.selectedModels
    }
}

private fun globToRegex(glob: String) = Regex(
    glob.split("*").joinToString(".*") { part -> part.split("?").joinToString(".") { Regex.escape(it) } },
    RegexOption.IGNORE_CASE
)

/** The named contents of a workspace object, in display order, or null if it is not something that has contents. */
fun childrenOf(value: Any): Map<String, Any>? = when (value) {
    is Workspace -> value.componentList.associateBy { it.name }
    is NetworkComponent -> value.network.allModels.named { it.id }
    is WorkspaceComponent -> value.attributeContainers.named { it.id }
    is Subnetwork -> value.modelList.all.named { it.id }
    is NeuronCollection -> value.neuronList.named { it.id }
    is SynapseGroup -> value.synapses.named { it.id }
    else -> null
}

private fun <T : Any> Iterable<T>.named(name: (T) -> String?): Map<String, Any> =
    mapNotNull { item -> name(item)?.let { it to item } }.toMap(linkedMapOf())

/** Puts a word in quotes if it needs them to be read back as one word. */
fun quoted(word: String) = if (word.any { it.isWhitespace() }) "\"$word\"" else word

/**
 * @param tokens the words of the line, with quotes removed
 * @param tokenStarts where each word begins in the line
 * @param lastTokenStart where the last word begins, or null if the line does not end in a word
 * @param pipes for each `|` outside quotes, how many words came before it
 * @param pipeOffsets where each of those is in the line
 * @param end where the line stops being read: at a comment, or its end
 */
private class ScannedLine(
    val tokens: List<String>,
    val tokenStarts: List<Int>,
    val lastTokenStart: Int?,
    val unterminatedQuote: Boolean,
    val pipes: List<Int>,
    val pipeOffsets: List<Int>,
    val end: Int
)

private fun scan(line: String): ScannedLine {
    val tokens = mutableListOf<String>()
    val tokenStarts = mutableListOf<Int>()
    val pipes = mutableListOf<Int>()
    val pipeOffsets = mutableListOf<Int>()
    val word = StringBuilder()
    var wordStart: Int? = null
    var quote: Char? = null
    var end = line.length

    fun endWord() {
        wordStart?.let { start ->
            tokens += word.toString()
            tokenStarts += start
            word.clear()
            wordStart = null
        }
    }

    for ((index, char) in line.withIndex()) {
        when {
            quote != null -> if (char == quote) quote = null else word.append(char)
            char == '#' && wordStart == null -> {
                end = index
                break
            }
            char.isWhitespace() -> endWord()
            char == '|' -> {
                endWord()
                pipes += tokens.size
                pipeOffsets += index
            }
            else -> {
                if (wordStart == null) wordStart = index
                if (char == '"' || char == '\'') quote = char else word.append(char)
            }
        }
    }
    val lastStart = wordStart
    endWord()
    return ScannedLine(tokens, tokenStarts, lastStart, quote != null, pipes, pipeOffsets, end)
}

/**
 * @param words the words of the command
 * @param filter the text given to `| grep`, if any
 * @param line the line as it was entered
 * @param wordStarts where each word begins in [line]
 * @param end where the command stops in [line]: at a pipe, a comment, or the end
 */
private class ParsedLine(
    val words: List<String>,
    val filter: String?,
    private val line: String,
    private val wordStarts: List<Int>,
    private val end: Int
) {

    /**
     * The condition that follows the word at [whereAt], as it was entered, so that quotes around a value reach the
     * expression parser. A condition given as one quoted word is taken without its quotes.
     */
    fun conditionAfter(whereAt: Int): String {
        val start = wordStarts.getOrNull(whereAt + 1) ?: return ""
        val entered = line.substring(start, end).trim()
        val quote = entered.first()
        val oneQuotedWord = words.size == whereAt + 2 && (quote == '"' || quote == '\'') &&
                entered.last() == quote && entered.count { it == quote } == 2
        return if (oneQuotedWord) words.last() else entered
    }
}

private fun parseLine(line: String): ParsedLine {
    val scanned = scan(line)
    if (scanned.unterminatedQuote) fail("Unterminated quote.")
    val pipe = scanned.pipes.firstOrNull()
        ?: return ParsedLine(scanned.tokens, null, line, scanned.tokenStarts, scanned.end)
    val piped = scanned.tokens.drop(pipe)
    if (scanned.pipes.size > 1 || piped.size < 2 || piped.first() != "grep") fail("The only pipe is | grep <text>.")
    return ParsedLine(scanned.tokens.take(pipe), piped.drop(1).joinToString(" "), line, scanned.tokenStarts, scanned.pipeOffsets.first())
}

/** Split a line into words, respecting quotes and dropping a `#` comment. */
fun tokenize(line: String): List<String> = parseLine(line).words
