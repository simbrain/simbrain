/**
 * Reads and writes the properties of workspace objects by name, for the command line. A property is available if it is
 * editable in a dialog, through [GuiEditable] or [UserParameter], so anything added to a dialog is available here too.
 */
package org.simbrain.console

import org.simbrain.util.UserParameter
import org.simbrain.util.callNoArgConstructor
import org.simbrain.util.invokeLegacySetter
import org.simbrain.util.propertyeditor.CopyableObject
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.util.propertyeditor.GuiEditable
import org.simbrain.util.propertyeditor.TypeOptionVisibility
import java.math.BigDecimal
import java.math.MathContext
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.floor
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1
import kotlin.reflect.full.allSuperclasses
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.jvm.isAccessible

private const val MAX_VALUE_LENGTH = 200
private const val NUMBER_PRECISION = 6
private const val MAX_SUGGESTIONS = 3
private const val MAX_CACHED_EXPRESSIONS = 200

class EditableProperty(
    val owner: Any,
    val property: KProperty1<Any, *>,
    val label: String,
    val order: Int,
    val displayOnly: Boolean,
    val useLegacySetter: Boolean,
    val value: Any?
) {
    val name: String get() = property.name
}

/** The editable properties of each class, found by reflection once per class. */
private val editableByClass = ConcurrentHashMap<KClass<*>, List<KProperty1<Any, *>>>()

@Suppress("UNCHECKED_CAST")
fun editablePropertiesOf(obj: Any): List<EditableProperty> {
    val known = editableByClass[obj::class]
    if (known != null) return known.mapNotNull { editableProperty(obj, it) }.sortedBy { it.order }
    val found = (obj::class.allSuperclasses + obj::class)
        .flatMap { it.declaredMemberProperties }
        .mapNotNull { property -> editableProperty(obj, property as KProperty1<Any, *>) }
        .distinctBy { it.name }
        .sortedBy { it.order }
    editableByClass[obj::class] = found.map { it.property }
    return found
}

private fun editableProperty(obj: Any, property: KProperty1<Any, *>): EditableProperty? = runCatching {
    property.isAccessible = true
    val annotation = property.findAnnotation<UserParameter>()
    val delegate = property.getDelegate(obj) as? GuiEditable<*, *>
    if (annotation == null && delegate == null) return null
    // Read first: a GuiEditable learns its label from the first access
    val value = property.get(obj)
    when {
        delegate != null -> EditableProperty(
            obj, property, delegate.label, delegate.order, delegate.displayOnly, delegate.useLegacySetter, value
        )
        else -> EditableProperty(
            obj, property, annotation!!.label, annotation.order, annotation.displayOnly, annotation.useLegacySetter, value
        )
    }
}.getOrNull()

/**
 * Find a property by key, which is its name or its dialog label in full, ignoring case and spaces, and can reach into
 * nested objects, as in `updateRule.slope`.
 */
fun findProperty(obj: Any, key: String): EditableProperty? {
    val head = key.substringBefore(".")
    val properties = editablePropertiesOf(obj)
    val wanted = normalized(head)
    val property = properties.firstOrNull { it.name == head }
        ?: properties.firstOrNull { normalized(it.name) == wanted || normalized(it.label) == wanted }
        ?: return null
    if ("." !in key) return property
    return findProperty(property.value ?: return null, key.substringAfter("."))
}

/** What a key that names no property of [obj] may have been meant to be, as the end of a sentence, or nothing. */
private fun didYouMean(obj: Any, key: String): String {
    val owner = if ("." in key) findProperty(obj, key.substringBeforeLast("."))?.value ?: return "" else obj
    val close = looseMatches(editablePropertiesOf(owner).map { it.name }, key.substringAfterLast("."))
    return if (close.isEmpty()) "" else " Did you mean ${close.take(MAX_SUGGESTIONS).joinToString(", ")}?"
}

/**
 * Set a property from text, converting to the type of the property. A number can be given as an expression, as in
 * `rand()*10` or `activation*2`, evaluated in [context].
 */
fun assign(obj: Any, key: String, text: String, context: ExpressionContext = ExpressionContext { propertyValue(obj, it) }) {
    val assignment = assignment(obj, key, text, context)
    val found = assignment.property
    if (found.useLegacySetter) {
        assignment.mutable.invokeLegacySetter(found.owner, assignment.value)
    } else {
        assignment.mutable.setter.call(found.owner, assignment.value)
    }
    (found.owner as? EditableObject)?.onCommit()
}

/** Fail as [assign] would for this key and text, without changing anything. */
fun checkAssignable(obj: Any, key: String, text: String, context: ExpressionContext = ExpressionContext { propertyValue(obj, it) }) {
    assignment(obj, key, text, context)
}

/** A property that can be set, and the value that some text stands for. */
private class Assignment(val property: EditableProperty, val mutable: KMutableProperty1<Any, Any?>, val value: Any?)

@Suppress("UNCHECKED_CAST")
private fun assignment(obj: Any, key: String, text: String, context: ExpressionContext): Assignment {
    val found = findProperty(obj, key)
        ?: fail("${obj::class.simpleName} has no property $key.${didYouMean(obj, key)} Use show to see its properties.")
    if (found.displayOnly) fail("${found.name} is read-only.")
    val mutable = found.property as? KMutableProperty1<Any, Any?> ?: fail("${found.name} is read-only.")
    return Assignment(found, mutable, convert(found, text, context))
}

/** The values that can be chosen from for a property, if it has a fixed set of them. */
fun valueOptions(property: EditableProperty): List<String> {
    val type = property.property.returnType.classifier as? KClass<*>
    return when {
        type == Boolean::class -> listOf("true", "false")
        type?.java?.isEnum == true -> type.java.enumConstants.map { it.toString() }
        else -> typeList(property)?.keys?.toList().orEmpty()
    }
}

/** The value of a property for use in an expression, or [Expression.MISSING] if there is none. */
fun propertyValue(obj: Any, name: String): Any? {
    val found = findProperty(obj, name) ?: return Expression.MISSING
    return found.value
}

private val parsedExpressions = ConcurrentHashMap<String, Expression>()

private fun parsedExpression(text: String): Expression {
    if (parsedExpressions.size > MAX_CACHED_EXPRESSIONS) parsedExpressions.clear()
    return parsedExpressions.getOrPut(text) { Expression.parse(text) }
}

private val identifier = Regex("[A-Za-z_][A-Za-z0-9_]*")

private fun evaluateNumber(property: EditableProperty, text: String, context: ExpressionContext): Double {
    text.toDoubleOrNull()?.let { return it }
    return try {
        parsedExpression(text).evaluateNumber(context)
    } catch (e: CommandException) {
        if (identifier.matches(text)) fail("${property.name} needs a number: $text") else throw e
    }
}

private fun convert(property: EditableProperty, text: String, context: ExpressionContext): Any? {
    val type = property.property.returnType
    if (text.equals("none", ignoreCase = true) && type.isMarkedNullable) return null
    val cls = type.classifier as? KClass<*>
    fun number() = evaluateNumber(property, text, context)
    fun whole(): Long {
        val value = number()
        if (value != floor(value) || value.isInfinite()) fail("${property.name} needs a whole number: ${formatValue(value)}")
        return value.toLong()
    }
    return when {
        cls == Double::class -> number()
        cls == Float::class -> number().toFloat()
        cls == Int::class -> whole().toInt()
        cls == Long::class -> whole()
        cls == Short::class -> whole().toShort()
        cls == Boolean::class -> parseBoolean(property, text)
        cls == String::class -> text
        cls?.java?.isEnum == true -> chooseOption(cls.java.enumConstants.associateBy { it.toString() }, text, property.name)
        else -> {
            val types = typeList(property) ?: fail("${property.name} can't be set from the command line.")
            chooseOption(types, text, property.name).kotlin.callNoArgConstructor()
        }
    }
}

private fun parseBoolean(property: EditableProperty, text: String): Boolean = when (text.lowercase()) {
    "true", "on", "yes" -> true
    "false", "off", "no" -> false
    else -> fail("${property.name} needs true or false: $text")
}

/** The types a property can be set to, leaving out hidden types as its dialog does. */
private fun typeList(property: EditableProperty): Map<String, Class<out CopyableObject>>? {
    val current = property.value as? CopyableObject ?: return null
    val types = current.getTypeList() ?: return null
    return typeOptions(types.filter { TypeOptionVisibility.isVisible(it.kotlin, listOf(current::class)) })
}

/** Names for a list of selectable types, as they would be typed: a rule or a layout is named without that word. */
fun <T : Any> typeOptions(types: List<Class<out T>>): Map<String, Class<out T>> =
    types.associateBy { it.simpleName.removeSuffix("Rule").removeSuffix("Layout") }

/** Pick from named options by a name in full, ignoring case and punctuation. */
fun <T> chooseOption(options: Map<String, T>, text: String, what: String): T {
    val wanted = normalized(text)
    options.entries.firstOrNull { normalized(it.key) == wanted }?.let { return it.value }
    val close = looseMatches(options.keys, text)
    if (close.isEmpty()) fail("Unknown value for $what: $text. Options: ${options.keys.joinToString(", ")}")
    fail("Unknown value for $what: $text. Did you mean ${close.take(MAX_SUGGESTIONS).joinToString(", ")}?")
}

fun normalized(text: String) = text.lowercase().filter { it.isLetterOrDigit() }

fun formatValue(value: Any?): String {
    val text = when (value) {
        null -> "none"
        is Double -> formatNumber(value)
        is Float -> formatNumber(value.toDouble())
        is DoubleArray -> value.joinToString(", ", limit = 20) { formatNumber(it) }
        is FloatArray -> value.joinToString(", ", limit = 20) { formatNumber(it.toDouble()) }
        is IntArray -> value.joinToString(", ", limit = 20)
        is Collection<*> -> value.joinToString(", ", limit = 20)
        is EditableObject -> value.name.takeUnless { it.startsWith("No-name") } ?: value::class.simpleName.orEmpty()
        else -> value.toString()
    }
    val singleLine = text.replace(Regex("\\s*\\n\\s*"), " ")
    return if (singleLine.length > MAX_VALUE_LENGTH) singleLine.take(MAX_VALUE_LENGTH) + "..." else singleLine
}

private fun formatNumber(value: Double): String =
    if (value.isNaN() || value.isInfinite()) {
        value.toString()
    } else {
        BigDecimal(value).round(MathContext(NUMBER_PRECISION)).stripTrailingZeros().toPlainString()
    }
