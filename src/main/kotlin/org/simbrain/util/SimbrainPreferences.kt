package org.simbrain.util

import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor
import org.simbrain.util.propertyeditor.EditableObject
import java.awt.Color
import java.util.prefs.Preferences
import javax.swing.JButton
import javax.swing.JOptionPane
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.isAccessible

/**
 * Marker interface for classes that hold [Preference] objects.
 */
open class PreferenceHolder: EditableObject {

    val events = mutableSetOf<() -> Unit>()

    fun registerChangeListener(preferenceLoader: () -> Unit) {
        events.add(preferenceLoader)
    }

    fun unregisterChangeListener(preferenceLoader: () -> Unit) {
        events.remove(preferenceLoader)
    }

    override fun onCommit() {
        events.forEach { it() }
    }
}

val systemPreferences = Preferences.userRoot().node("/org/simbrain")

/**
 * Wrapper around system preference API.
 *
 * @See https://docs.oracle.com/javase/8/docs/api/java/util/prefs/Preferences.html
 */
sealed class Preference<T>(val default: T) {

    /**
     * Unique identifier for a preference. It is inferred from a variable name.
     */
    private lateinit var name: String

    private var cachedValue: T? = null

    /**
     * Revert all preferences to their default value.
     */
    fun revertToDefault() {
        systemPreferences.remove(name)
        cachedValue = null
    }

    operator fun <H: PreferenceHolder> getValue(thisRef: H, property: KProperty<*>): T {
        name = property.name
        if (cachedValue == null) {
            cachedValue = deserialize(systemPreferences.get(property.name, serialize(default)))
        }
        return cachedValue!!
    }

    operator fun <H: PreferenceHolder> setValue(thisRef: H, property: KProperty<*>, value: T) {
        systemPreferences.put(property.name, serialize(value))
        cachedValue = value
    }

    /**
     * Obtain the preference from a string so it can be stored
     */
    abstract fun deserialize(value: String): T

    /**
     * Write the preference to a string
     */
    abstract fun serialize(value: T): String

}

fun PreferenceHolder.revertToDefault() {
    this::class.declaredMemberProperties.filterIsInstance<KMutableProperty1<Any, *>>()
        .map { it.apply { isAccessible = true }.getDelegate(this) }
        .filterIsInstance<Preference<*>>()
        .forEach { it.revertToDefault() }
}

class IntegerPreference(defaultVal: Int): Preference<Int>(defaultVal) {
    override fun deserialize(value: String): Int = value.toInt()
    override fun serialize(value: Int) = value.toString()
}

class FloatPreference(defaultVal: Float): Preference<Float>(defaultVal) {
    override fun deserialize(value: String): Float = value.toFloat()
    override fun serialize(value: Float) = value.toString()
}

class DoublePreference(defaultVal: Double): Preference<Double>(defaultVal) {
    override fun deserialize(value: String): Double = value.toDouble()
    override fun serialize(value: Double) = value.toString()
}

class BooleanPreference(defaultVal: Boolean): Preference<Boolean>(defaultVal) {
    override fun deserialize(value: String): Boolean = value.toBoolean()
    override fun serialize(value: Boolean) = value.toString()
}

class StringPreference(defaultPath: String): Preference<String>(defaultPath) {
    override fun deserialize(value: String) = value
    override fun serialize(value: String) = value
}

class ColorPreference(defaultColor: Color): Preference<Color>(defaultColor) {
    override fun deserialize(value: String) = Color(value.toInt())
    override fun serialize(value: Color) = value.rgb.toString()
}

fun getPreferenceDialog(prefHolder: PreferenceHolder): StandardDialog {
    val ape = AnnotatedPropertyEditor(prefHolder)
    return StandardDialog().apply {
        contentPane = ape
        addButton(JButton("Revert to Default").apply {
            addActionListener {
                val result = showWarningConfirmDialog("Reverting to default preferences. This action cannot be undone.")
                if (result == JOptionPane.YES_OPTION) {
                    prefHolder.revertToDefault()
                }
            }
        })
        addClosingTask {
            ape.commitChanges()
        }
    }
}

object TestPrefs: PreferenceHolder() {

    @UserParameter(label = "Test directory")
    var testDir by StringPreference("." + Utils.FS +"simulations")

    @UserParameter(label = "Test color")
    var testColor by ColorPreference(Color.BLACK)

    @UserParameter(label = "Test int")
    var testInt by IntegerPreference(2)

    @UserParameter(label = "Test double")
    var testDouble by DoublePreference(.1)

}

fun main() = getPreferenceDialog(TestPrefs).display()