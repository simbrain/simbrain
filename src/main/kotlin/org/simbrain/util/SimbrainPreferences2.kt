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

object SimbrainPreferences2: EditableObject {

    @UserParameter(label = "Workspace base directory", order = 1, editable = false)
    var workspaceBaseDirectory by DirectoryPreference("." + Utils.FS +"simulations")

    @UserParameter(label = "Network background color", order = 2)
    var networkBackgroundColor by ColorPreference(Color.WHITE)

    @UserParameter(label = "Number", order = 3)
    var number = 1.0

    fun revertToDefault() {
        SimbrainPreferences2::class.declaredMemberProperties.filterIsInstance<KMutableProperty1<SimbrainPreferences2, *>>()
            .map { it.apply { isAccessible = true }.getDelegate(this) }
            .filterIsInstance<Preference<*>>()
            .forEach { it.revertToDefault() }
    }

}

val systemPreferences = Preferences.userRoot().node("/org/simbrain")

sealed class Preference<T>(val default: T) {

    lateinit var name: String

    fun revertToDefault() {
        systemPreferences.remove(name)
    }

    operator fun getValue(thisRef: SimbrainPreferences2, property: KProperty<*>): T {
        name = property.name
        return deserialize(systemPreferences.get(property.name, serialize(default)))
    }

    operator fun setValue(thisRef: SimbrainPreferences2, property: KProperty<*>, value: T) {
        systemPreferences.put(property.name, serialize(value))
    }

    abstract fun deserialize(value: String): T

    abstract fun serialize(value: T): String

}

class DirectoryPreference(defaultPath: String): Preference<String>(defaultPath) {

    override fun deserialize(value: String) = value

    override fun serialize(value: String) = value

}

class ColorPreference(defaultColor: Color): Preference<Color>(defaultColor) {

    override fun deserialize(value: String) = Color(value.toInt())

    override fun serialize(value: Color) = value.rgb.toString()

}

fun main() {
    val prefs = SimbrainPreferences2
    println(prefs.workspaceBaseDirectory)
    println(prefs.number)
    val ape = AnnotatedPropertyEditor(prefs)
    val dialog = StandardDialog().apply {
        contentPane = ape
        addButton(JButton("Revert to Default").apply {
            addActionListener {
                val result = showWarningConfirmDialog("Reverting to default preferences. This action cannot be undone.")
                if (result == JOptionPane.YES_OPTION) {
                    prefs.revertToDefault()
                    ape.fillFieldValues()
                }
            }
        })
        addClosingTask {
            ape.commitChanges()
            println("${prefs.workspaceBaseDirectory}, ${prefs.number}")
        }
    }.display()
}