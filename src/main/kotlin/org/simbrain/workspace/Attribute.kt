/**
 * The attribute layer of the coupling system: [Attribute] and its two concrete forms, [Producer] and
 * [Consumer], which a [org.simbrain.workspace.couplings.Coupling] pairs so that each workspace tick copies
 * the producer's value to the consumer.
 *
 * Invocation is suspend-native. An attribute method may be an ordinary function or a suspend function;
 * suspending attributes are invoked through the same cached [Method] as plain ones, passing the caller's
 * [Continuation] directly (see [invokeSuspending]), so the never-suspends path costs a plain reflective
 * call and kotlin-reflect is never touched. Suspend-specific detection, value-type recovery, and
 * validation live here as [Method] extensions; they are evaluated once per method when
 * [org.simbrain.workspace.couplings.CouplingCache] builds attributes, never per tick.
 */
package org.simbrain.workspace

import org.simbrain.util.Theme
import org.simbrain.util.Utils
import org.simbrain.workspace.couplings.LOW_PRIORITY
import smile.math.matrix.Matrix
import java.awt.Color
import javax.swing.UIManager
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.WildcardType
import kotlin.coroutines.Continuation
import kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn

/**
 * An object with a getter or setter method that a coupling can invoke to produce or consume a value,
 * together with how the attribute is described in the GUI.
 */
abstract class Attribute(
    /**
     * The object that contains the getter or setter to be called.
     */
    val baseObject: AttributeContainer,

    /**
     * The getter method (for producers) or setter method (for consumers).
     */
    val method: Method,

    /**
     * Custom description text; when empty, the description is built from the id and method name.
     */
    var description: String = "",

    /**
     * When coupling containers automatically, the highest priority producer and consumer are paired.
     * Use priority = 1 ([org.simbrain.workspace.couplings.HIGH_PRIORITY]) for highest priority.
     */
    var priority: Int = LOW_PRIORITY,

    /**
     * Optional method that supplies a per-object description, overriding [description].
     */
    private val customDescriptionMethod: Method? = null
) {

    /**
     * The type of the attribute: what a producer returns, or what a consumer's value parameter accepts.
     */
    abstract val type: Type

    /**
     * A string id, e.g. "Neuron15" or "Sensor5".
     */
    val id: String
        get() = baseObject.id ?: baseObject.javaClass.simpleName

    /**
     * The name used for this attribute's container in descriptions: the container's
     * [AttributeContainer.attributeName] when it provides one, else [id].
     */
    val containerDisplayName: String
        get() = baseObject.attributeName?.takeIf { it.isNotEmpty() } ?: id

    /**
     * A human-readable description, e.g. "Neuron1:Activation". Used by list renderers in the coupling GUI.
     */
    val simpleDescription: String
        get() {
            val containerName = baseObject.containerName?.let { "$it:" } ?: ""
            customDescription?.let { return containerName + it }
            if (description.isNotEmpty()) {
                return "$containerName$containerDisplayName:$description"
            }
            return "$containerName$containerDisplayName:$simpleMethodName"
        }

    /**
     * The method name in display form, with the get/set prefix removed, camel case split into words, and
     * the first letter capitalized. E.g. "getActivation" becomes "Activation".
     */
    val simpleMethodName: String
        get() {
            val stripped = method.name
                .removePrefix("get")
                .removePrefix("set")
            return Utils.upperCaseFirstLetter(Utils.splitCamelCase(stripped).lowercase())
        }

    private val customDescription: String?
        get() = customDescriptionMethod?.let { invokePlain(it, baseObject) as String? }

    override fun toString() = simpleDescription

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        other as Attribute
        return baseObject == other.baseObject && method == other.method
    }

    override fun hashCode() = 31 * baseObject.hashCode() + method.hashCode()
}

/**
 * The part of a coupling that produces values for a [Consumer].
 */
class Producer(
    baseObject: AttributeContainer,
    method: Method,
    description: String = "",
    priority: Int = LOW_PRIORITY,
    customDescriptionMethod: Method? = null,

    /**
     * See [Producible.arrayComponentsMethod]. So far the only use cases are for producers; if consumer
     * use cases are found this can move to the attribute level.
     */
    private val arrayComponentsMethod: Method? = null
) : Attribute(baseObject, method, description, priority, customDescriptionMethod) {

    private val isSuspending = method.isSuspendAttribute

    override val type: Type = method.producibleType

    /**
     * Return the current value of the producer, suspending if the underlying method does.
     */
    suspend fun getValue(): Any? = if (isSuspending) {
        invokeSuspending(method, baseObject)
    } else {
        invokePlain(method, baseObject)
    }

    /**
     * The components of what this producer sends, in order, or empty when it declares no
     * [Producible.arrayComponentsMethod]. Names here are raw, so they may repeat; see [displayComponents]
     * for the form a consumer should show.
     */
    @Suppress("UNCHECKED_CAST")
    val components: List<AttributeComponent>
        get() {
            val raw = arrayComponentsMethod?.let { invokePlain(it, baseObject) } as List<AttributeComponent>?
            // A plain ArrayList, not an immutable list: these end up stored on plot models, and XStream
            // refuses to restore the serialization proxy that Java's immutable lists write themselves as.
            return if (raw == null) ArrayList() else ArrayList(raw)
        }

    /**
     * The components of what this producer sends, ready to display: as [components] but with repeated
     * names given a positional suffix so a consumer can tell them apart. Consumers naming a whole
     * attribute rather than its components, such as a single time series fed by a scalar coupling, should
     * use [simpleDescription] instead.
     */
    val displayComponents: List<AttributeComponent>
        get() = components.disambiguateNames()

    /**
     * The display names of [displayComponents], for consumers that only label what they show and keep no
     * per-component state.
     */
    val displayNames: List<String>
        get() = displayComponents.mapTo(ArrayList()) { it.name }
}

/**
 * The part of a coupling that receives values from a [Producer].
 */
class Consumer(
    baseObject: AttributeContainer,
    method: Method,
    description: String = "",
    priority: Int = LOW_PRIORITY,
    customDescriptionMethod: Method? = null
) : Attribute(baseObject, method, description, priority, customDescriptionMethod) {

    private val isSuspending = method.isSuspendAttribute

    override val type: Type = method.consumableType

    /**
     * Update the consumer by setting its value, suspending if the underlying method does.
     */
    suspend fun setValue(value: Any?) {
        if (isSuspending) {
            invokeSuspending(method, baseObject, value)
        } else {
            invokePlain(method, baseObject, value)
        }
    }
}

/**
 * Whether this method is the JVM form of a suspend function: its last parameter is the [Continuation]
 * the compiler appends.
 */
val Method.isSuspendAttribute: Boolean
    get() = parameterCount > 0 && parameterTypes[parameterCount - 1] == Continuation::class.java

/**
 * The value type a producible method yields. For a plain method this is its return type. A suspend
 * method's JVM return type is Object, so its value type is recovered from the generic signature of the
 * trailing continuation parameter (`Continuation<? super Double>` yields `Double`), normalized from the
 * boxed wrapper to the primitive so it matches what plain getters declare.
 */
val Method.producibleType: Type
    get() {
        if (!isSuspendAttribute) return returnType
        val continuationType = genericParameterTypes.last() as? ParameterizedType
            ?: throw IllegalArgumentException(
                "Suspending producible ${declaringClass.simpleName}.$name has no generic continuation " +
                        "signature to recover its value type from."
            )
        val argument = continuationType.actualTypeArguments[0]
        val valueType = (argument as? WildcardType)?.lowerBounds?.firstOrNull() ?: argument
        return (valueType as? Class<*>)?.let { wrapperToPrimitive[it] } ?: valueType
    }

/**
 * The value type a consumable method accepts: its first parameter, which for a suspend method is still
 * the value (the continuation trails it).
 */
val Method.consumableType: Type
    get() = genericParameterTypes[0]

/**
 * Reject attribute methods whose JVM form cannot be invoked the way this layer invokes them. Called once
 * per method when the coupling cache first builds an attribute for it.
 */
fun Method.validateAttributeMethod() {
    require(!name.contains('-')) {
        "Attribute method ${declaringClass.simpleName}.$name has a mangled JVM name, which means a value " +
                "class appears in its signature. Value classes are not supported in attribute methods."
    }
    require(declaringClass.methods.none { it.name == "$name\$default" }) {
        "Attribute method ${declaringClass.simpleName}.$name has default parameter values, which are not " +
                "supported in attribute methods."
    }
}

/**
 * Whether a value flowing from an attribute or transform of type [from] can be delivered to one of type
 * [to]. Boxed wrappers and primitives are interchangeable — reflection boxes and unboxes at the call
 * boundary — so a nullable `Double?` producer (boxed on the JVM) matches a plain `double` consumer; a
 * null value is skipped before delivery rather than unboxed.
 */
fun attributeTypesMatch(from: Type, to: Type): Boolean = from.normalized == to.normalized

/**
 * Human-readable name for an attribute or transform endpoint type, e.g. "Number" rather than "double"
 * or "Array" rather than "class [D". Used in coupling GUI labels and type-mismatch messages.
 */
val Type.attributeTypeName: String
    get() = when (this.normalized) {
        java.lang.Double.TYPE -> "Number"
        DoubleArray::class.java -> "Array"
        String::class.java -> "Text"
        else -> (this as? Class<*>)?.simpleName ?: toString()
    }

/**
 * Display color for an attribute or transform endpoint type in coupling lists, legends, and the
 * transform editor. Resolved from the look and feel's accent palette on every read, so the colors
 * follow theme switches; the literal fallbacks approximate the FlatLaf light palette. Returned as a
 * plain Color, never a UIResource, so a component foreground set to it survives updateUI sweeps.
 */
val Type.attributeTypeColor: Color
    get() = when (this.normalized) {
        DoubleArray::class.java -> solid(UIManager.getColor("Actions.Green") ?: Color(89, 168, 105))
        String::class.java -> solid(UIManager.getColor("Actions.Blue") ?: Color(56, 159, 214))
        Matrix::class.java -> solid(UIManager.getColor("Actions.Yellow") ?: Color(237, 162, 0))
        else -> solid(Theme.foreground)
    }

private fun solid(color: Color) = Color(color.red, color.green, color.blue)

private val Type.normalized: Type
    get() = (this as? Class<*>)?.let { wrapperToPrimitive[it] } ?: this

private val wrapperToPrimitive: Map<Class<*>, Class<*>> = mapOf(
    java.lang.Double::class.java to java.lang.Double.TYPE,
    java.lang.Float::class.java to java.lang.Float.TYPE,
    java.lang.Long::class.java to java.lang.Long.TYPE,
    java.lang.Integer::class.java to java.lang.Integer.TYPE,
    java.lang.Short::class.java to java.lang.Short.TYPE,
    java.lang.Byte::class.java to java.lang.Byte.TYPE,
    java.lang.Character::class.java to java.lang.Character.TYPE,
    java.lang.Boolean::class.java to java.lang.Boolean.TYPE,
)

private fun invokePlain(method: Method, receiver: Any, vararg args: Any?): Any? = try {
    method.invoke(receiver, *args)
} catch (ex: InvocationTargetException) {
    // Surface what the attribute method actually threw, so failure reporting shows the real cause
    throw ex.cause ?: RuntimeException(ex)
} catch (ex: ReflectiveOperationException) {
    throw RuntimeException(ex)
} catch (ex: IllegalArgumentException) {
    throw RuntimeException(ex)
}

/**
 * Invoke the JVM form of a suspend function through plain reflection: pass the caller's own continuation
 * as the trailing argument. Synchronous completion returns the value straight through; a genuine
 * suspension returns COROUTINE_SUSPENDED and the coroutine machinery takes over. Exceptions thrown before
 * the first suspension surface as InvocationTargetException and are unwrapped to their real cause;
 * exceptions after a resumption propagate as themselves.
 */
private suspend fun invokeSuspending(method: Method, receiver: Any, vararg args: Any?): Any? = try {
    suspendCoroutineUninterceptedOrReturn { continuation: Continuation<Any?> ->
        method.invoke(receiver, *args, continuation)
    }
} catch (ex: InvocationTargetException) {
    throw ex.cause ?: RuntimeException(ex)
} catch (ex: ReflectiveOperationException) {
    throw RuntimeException(ex)
} catch (ex: IllegalArgumentException) {
    throw RuntimeException(ex)
}
