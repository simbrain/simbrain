/**
 * Small expressions for the command line, used for numeric values in `set` and `add`, as in `bias=rand()*10`, and for
 * `where` conditions, as in `activation>0.5 and type=Neuron`. An expression is parsed once and evaluated for each
 * object, so names in it refer to that object's properties, `i` to the object's position among those being changed
 * (from 0), and `n` to how many there are.
 */
package org.simbrain.console

import java.util.concurrent.ThreadLocalRandom
import kotlin.math.pow

/**
 * What names in an expression refer to.
 *
 * @param lookup the value of a name, or [Expression.MISSING] if there is no such property
 */
class ExpressionContext(val index: Int = 0, val count: Int = 1, val lookup: (String) -> Any? = { Expression.MISSING })

/**
 * @param missingNames names that were looked up and not found, so that a misspelled name can be reported
 */
class Expression private constructor(private val root: Node, val text: String, val missingNames: Set<String>) {

    fun evaluate(context: ExpressionContext): Any? = root.evaluate(context)

    fun evaluateNumber(context: ExpressionContext): Double {
        val value = evaluate(context)
        if (value === Missing) fail("Unknown name in $text: ${missingNames.joinToString(", ")}. Use show to see property names.")
        return toNumber(value) ?: fail("$text is not a number: ${formatValue(value)}")
    }

    /** Whether a condition holds. A comparison with a property the object lacks is false. */
    fun test(context: ExpressionContext): Boolean = when (val value = evaluate(context)) {
        is Boolean -> value
        Missing -> false
        else -> fail("$text is not a condition. Compare with >, <, =, or !=.")
    }

    private sealed class Node {
        abstract fun evaluate(context: ExpressionContext): Any?
    }

    private class Literal(val value: Any) : Node() {
        override fun evaluate(context: ExpressionContext) = value
    }

    /** @param wordIfMissing whether an unknown name stands for itself, as `Synapse` does in `type=Synapse` */
    private class Name(val name: String, val wordIfMissing: Boolean, val missingNames: MutableSet<String>) : Node() {
        override fun evaluate(context: ExpressionContext): Any? {
            val value = context.lookup(name)
            if (value !== MISSING) return value
            when (name) {
                "i" -> return context.index.toDouble()
                "n" -> return context.count.toDouble()
                "pi" -> return Math.PI
                "e" -> return Math.E
                "true" -> return true
                "false" -> return false
            }
            if (wordIfMissing) return name
            missingNames += name
            return Missing
        }
    }

    private class Call(val name: String, val args: List<Node>) : Node() {
        override fun evaluate(context: ExpressionContext): Any {
            val values = args.map { arg -> arg.evaluate(context).let { toNumber(it) ?: return Missing } }
            val function = functions[name] ?: fail("Unknown function: $name. Functions: ${functions.keys.joinToString(", ")}")
            return function(values)
        }
    }

    private class Unary(val operator: String, val operand: Node) : Node() {
        override fun evaluate(context: ExpressionContext): Any {
            val value = operand.evaluate(context)
            if (value === Missing) return Missing
            return when (operator) {
                "not" -> !(value as? Boolean ?: fail("not needs a condition."))
                else -> -(toNumber(value) ?: fail("Can't negate ${formatValue(value)}."))
            }
        }
    }

    private class Binary(val operator: String, val left: Node, val right: Node) : Node() {
        override fun evaluate(context: ExpressionContext): Any {
            if (operator == "and" || operator == "or") {
                val first = left.evaluate(context) == true
                if (operator == "and" && !first) return false
                if (operator == "or" && first) return true
                return right.evaluate(context) == true
            }
            val a = left.evaluate(context)
            val b = right.evaluate(context)
            if (a === Missing || b === Missing) return Missing
            val x = toNumber(a)
            val y = toNumber(b)
            if (operator in comparisons) {
                return if (x != null && y != null) compareNumbers(operator, x, y) else compareText(operator, a, b)
            }
            if (x == null || y == null) fail("Can't use $operator with ${formatValue(if (x == null) a else b)}.")
            return when (operator) {
                "+" -> x + y
                "-" -> x - y
                "*" -> x * y
                "/" -> x / y
                "%" -> x % y
                else -> x.pow(y)
            }
        }

        private fun compareNumbers(operator: String, x: Double, y: Double) = when (operator) {
            ">" -> x > y
            "<" -> x < y
            ">=" -> x >= y
            "<=" -> x <= y
            "!=" -> x != y
            else -> x == y
        }

        private fun compareText(operator: String, a: Any?, b: Any?): Boolean {
            val same = formatValue(a).equals(formatValue(b), ignoreCase = true)
            return when (operator) {
                "=", "==" -> same
                "!=" -> !same
                else -> fail("Can't compare ${formatValue(a)} and ${formatValue(b)} with $operator.")
            }
        }
    }

    /** A value that could not be found. Arithmetic and comparisons with it give it back, and a condition is false. */
    private object Missing

    private class Parser(val text: String, val missingNames: MutableSet<String>) {
        private val tokens = tokenizeExpression(text)
        private var position = 0

        private fun peek() = tokens.getOrNull(position)
        private fun next() = tokens.getOrNull(position++) ?: fail("$text ends too soon.")
        private fun accept(vararg options: String) = peek()?.takeIf { it in options }?.also { position++ }

        fun parseAll(): Node {
            val node = or()
            if (peek() != null) fail("Unexpected ${peek()} in $text")
            return node
        }

        private fun or(): Node {
            var node = and()
            while (accept("or") != null) node = Binary("or", node, and())
            return node
        }

        private fun and(): Node {
            var node = not()
            while (accept("and") != null) node = Binary("and", node, not())
            return node
        }

        private fun not(): Node = if (accept("not") != null) Unary("not", not()) else comparison()

        private fun comparison(): Node {
            val left = sum()
            val operator = accept(*comparisons.toTypedArray()) ?: return left
            return Binary(operator, left, sum(wordAllowed = operator in listOf("=", "==", "!=")))
        }

        private fun sum(wordAllowed: Boolean = false): Node {
            var node = product(wordAllowed)
            while (true) {
                val operator = accept("+", "-") ?: return node
                node = Binary(operator, node, product(false))
            }
        }

        private fun product(wordAllowed: Boolean): Node {
            var node = power(wordAllowed)
            while (true) {
                val operator = accept("*", "/", "%") ?: return node
                node = Binary(operator, node, power(false))
            }
        }

        private fun power(wordAllowed: Boolean): Node {
            val base = unary(wordAllowed)
            return if (accept("^") != null) Binary("^", base, power(false)) else base
        }

        private fun unary(wordAllowed: Boolean): Node = if (accept("-") != null) Unary("-", unary(false)) else primary(wordAllowed)

        private fun primary(wordAllowed: Boolean): Node {
            val token = next()
            return when {
                token == "(" -> or().also { if (accept(")") == null) fail("Missing ) in $text") }
                token.first() == '"' || token.first() == '\'' -> Literal(token.drop(1).dropLast(1))
                token.first().isDigit() || token.first() == '.' -> Literal(token.toDoubleOrNull() ?: fail("Bad number $token in $text"))
                token.first().isLetter() || token.first() == '_' -> {
                    if (accept("(") == null) return Name(token, wordAllowed, missingNames)
                    val args = mutableListOf<Node>()
                    if (accept(")") == null) {
                        do args += or() while (accept(",") != null)
                        if (accept(")") == null) fail("Missing ) in $text")
                    }
                    Call(token.lowercase(), args)
                }
                else -> fail("Unexpected $token in $text")
            }
        }
    }

    companion object {
        /** What a lookup returns for a name that is not a property. */
        val MISSING = Any()

        private val comparisons = listOf(">=", "<=", "!=", "==", "=", ">", "<")

        private val functions: Map<String, (List<Double>) -> Double> = mapOf(
            "rand" to { a ->
                when (a.size) {
                    0 -> uniform(0.0, 1.0)
                    1 -> uniform(0.0, a[0])
                    else -> uniform(a[0], a[1])
                }
            },
            "randn" to { a -> a.getOrElse(0) { 0.0 } + a.getOrElse(1) { 1.0 } * ThreadLocalRandom.current().nextGaussian() },
            "abs" to { a -> kotlin.math.abs(one(a)) },
            "sqrt" to { a -> kotlin.math.sqrt(one(a)) },
            "exp" to { a -> kotlin.math.exp(one(a)) },
            "log" to { a -> kotlin.math.ln(one(a)) },
            "sin" to { a -> kotlin.math.sin(one(a)) },
            "cos" to { a -> kotlin.math.cos(one(a)) },
            "tanh" to { a -> kotlin.math.tanh(one(a)) },
            "round" to { a -> kotlin.math.round(one(a)) },
            "floor" to { a -> kotlin.math.floor(one(a)) },
            "ceil" to { a -> kotlin.math.ceil(one(a)) },
            "min" to { a -> a.minOrNull() ?: fail("min needs values.") },
            "max" to { a -> a.maxOrNull() ?: fail("max needs values.") }
        )

        private fun uniform(low: Double, high: Double) = low + (high - low) * ThreadLocalRandom.current().nextDouble()

        private fun one(args: List<Double>) = args.singleOrNull() ?: fail("Expected one value, got ${args.size}.")

        fun parse(text: String): Expression {
            val missingNames = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
            return Expression(Parser(text, missingNames).parseAll(), text, missingNames)
        }

        /** Whether text is more than a plain number, so it needs evaluating. */
        fun isExpression(text: String) = text.toDoubleOrNull() == null
    }
}

private fun toNumber(value: Any?): Double? = when (value) {
    is Number -> value.toDouble()
    is String -> value.toDoubleOrNull()
    else -> null
}

private val expressionToken = Regex("""\s*(\d+\.?\d*(?:[eE][-+]?\d+)?|\.\d+(?:[eE][-+]?\d+)?|[A-Za-z_][\w.]*|>=|<=|!=|==|"[^"]*"|'[^']*'|[-+*/%^()<>=,])""")

private fun tokenizeExpression(text: String): List<String> {
    val tokens = mutableListOf<String>()
    var position = 0
    while (position < text.length) {
        if (text[position].isWhitespace()) {
            position++
            continue
        }
        val match = expressionToken.matchAt(text, position) ?: fail("Unexpected ${text[position]} in $text")
        tokens += match.groupValues[1]
        position = match.range.last + 1
    }
    return tokens
}
