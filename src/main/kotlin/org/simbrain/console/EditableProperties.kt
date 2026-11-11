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
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1
import kotlin.reflect.full.allSuperclasses
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.jvm.isAccessible

private const val MAX_VALUE_LENGTH = 200
private const val NUMBER_PRECISION = 6

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
 * Find a property by key, which can be its name or its dialog label and can reach into nested objects, as in
 * `updateRule.slope`.
 *
 * @param exactOnly if false, a word that is part of exactly one property name is also accepted, such as `rule`
 */
fun findProperty(obj: Any, key: String, exactOnly: Boolean = false): EditableProperty? {
    val head = key.substringBefore(".")
    val properties = editablePropertiesOf(obj)
    val wanted = normalized(head)
    val property = properties.firstOrNull { it.name == head }
        ?: properties.firstOrNull { normalized(it.name) == wanted || normalized(it.label) == wanted }
        ?: properties.filter { !exactOnly && wanted in normalized(it.name) }.singleOrNull()
        ?: return null
    if ("." !in key) return property
    return findProperty(property.value ?: return null, key.substringAfter("."), exactOnly)
}

/** Set a property from text, converting to the type of the property. */
@Suppress("UNCHECKED_CAST")
fun assign(obj: Any, key: String, text: String) {
    val found = findProperty(obj, key)
        ?: fail("${obj::class.simpleName} has no property $key. Use show to see its properties.")
    if (found.displayOnly) fail("${found.name} is read-only.")
    val property = found.property as? KMutableProperty1<Any, Any?> ?: fail("${found.name} is read-only.")
    val value = convert(found, text)
    if (found.useLegacySetter) property.invokeLegacySetter(found.owner, value) else property.setter.call(found.owner, value)
    (found.owner as? EditableObject)?.onCommit()
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

private fun convert(property: EditableProperty, text: String): Any? {
    val type = property.property.returnType
    if (text.equals("none", ignoreCase = true) && type.isMarkedNullable) return null
    val cls = type.classifier as? KClass<*>
    fun <T : Any> number(parse: (String) -> T?): T = parse(text) ?: fail("${property.name} needs a number: $text")
    return when {
        cls == Double::class -> number(String::toDoubleOrNull)
        cls == Float::class -> number(String::toFloatOrNull)
        cls == Int::class -> number(String::toIntOrNull)
        cls == Long::class -> number(String::toLongOrNull)
        cls == Short::class -> number(String::toShortOrNull)
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

/** Names for a list of selectable types, as they would be typed. */
fun <T : Any> typeOptions(types: List<Class<out T>>): Map<String, Class<out T>> =
    types.associateBy { it.simpleName.removeSuffix("Rule") }

/** Pick from named options by exact name, then by the start of a name, then by part of a name. */
fun <T> chooseOption(options: Map<String, T>, text: String, what: String): T {
    val wanted = normalized(text)
    val match = options.entries.firstOrNull { normalized(it.key) == wanted }
        ?: options.entries.filter { normalized(it.key).startsWith(wanted) }.singleOrNull()
        ?: options.entries.filter { wanted in normalized(it.key) }.singleOrNull()
        ?: fail("Unknown value for $what: $text. Options: ${options.keys.joinToString(", ")}")
    return match.value
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
