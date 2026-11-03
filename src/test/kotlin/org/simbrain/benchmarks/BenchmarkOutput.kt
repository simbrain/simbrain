/**
 * The one JSON line each Simbrain 4 harness prints per run. Numbers are written with a fixed number of decimals
 * whatever the machine's locale, and any -Dbench.* diagnostic switch that was set is recorded, so a result file says
 * when a run measured something other than the default.
 */
package org.simbrain.benchmarks

import java.util.Locale

/** A number written with [digits] decimals. */
internal class Fixed(private val value: Double, private val digits: Int) {
    override fun toString(): String = String.format(Locale.ROOT, "%.${digits}f", value)
}

internal fun fixed(value: Double, digits: Int) = Fixed(value, digits)

/** The -Dbench.* system properties set for this run, by name without the prefix. */
private fun benchSwitches(): Map<String, String> = System.getProperties().stringPropertyNames()
    .filter { it.startsWith("bench.") }
    .sorted()
    .associate { it.removePrefix("bench.") to System.getProperty(it) }

/**
 * Prints [fields] as one JSON object. Values may be strings, numbers ([Fixed] for a set number of decimals), booleans,
 * null, lists and maps; any -Dbench.* switch is added as "bench_switches".
 */
internal fun emitResult(vararg fields: Pair<String, Any?>) {
    val switches = benchSwitches()
    val all = if (switches.isEmpty()) fields.toList() else fields.toList() + ("bench_switches" to switches)
    println(json(all.toMap()))
}

private fun json(value: Any?): String = when (value) {
    null -> "null"
    is String -> quote(value)
    // Kotlin writes Int, Long, Double and Boolean the same in every locale
    is Fixed, is Int, is Long, is Double, is Boolean -> value.toString()
    is List<*> -> value.joinToString(",", "[", "]") { json(it) }
    is Map<*, *> -> value.entries.joinToString(",", "{", "}") { (key, v) -> "${quote(key.toString())}:${json(v)}" }
    else -> error("no JSON form for ${value::class}")
}

private fun quote(s: String) = "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
