@file:JvmName("XStreamUtils")

package org.simbrain.util

import com.thoughtworks.xstream.XStream
import com.thoughtworks.xstream.converters.MarshallingContext
import com.thoughtworks.xstream.converters.UnmarshallingContext
import com.thoughtworks.xstream.converters.reflection.ReflectionConverter
import com.thoughtworks.xstream.converters.reflection.ReflectionProvider
import com.thoughtworks.xstream.io.HierarchicalStreamReader
import com.thoughtworks.xstream.io.HierarchicalStreamWriter
import com.thoughtworks.xstream.io.xml.DomDriver
import com.thoughtworks.xstream.mapper.Mapper
import org.simbrain.network.core.Network
import org.simbrain.network.core.XStreamConstructor
import org.simbrain.util.piccolo.Tile
import org.simbrain.util.projection.Projector
import org.simbrain.util.propertyeditor.EditableObject
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.*
import kotlin.reflect.javaType
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaField

/**
 * Returns an XStream instance with default Simbrain settings, including backwards compatibility with earlier xml,
 * and turning off security warning, and formatting xml as utf-8.
 *
 * @return the properly initialized XStream object
 */
fun getSimbrainXStream(): XStream {
    return XStream(DomDriver("UTF-8")).apply {
        ignoreUnknownElements()
        allowTypesByWildcard(
            // be sure to sync these with the build.gradle simbrainJvmArgs --add-opens items
            arrayOf(
                "org.simbrain.**",
                "java.awt.**",
                "java.awt.geom.**",
                "org.jfree.**",
                "javax.swing.event.**",
                "java.beans.**",
                "smile.math.**",
                "java.util.concurrent.**"
            )
        )
        registerConverter(DoubleArrayConverter())
        registerConverter(MatrixConverter())
        registerConverter(
            createConstructorCallingConverter(
                listOf(
                    EditableObject::class.java,
                ),
                mapper,
                reflectionProvider,
                excludedTypes = listOf(
                    Network::class.java,
                    Tile::class.java,
                    Projector::class.java
                )
            )
        )
    }
}

/**
 * XStream support for Kotlin classes that require a constructor call. Which constructor to use can be specified by
 * [XStreamConstructor].
 *
 * Must be used to properly serialize Kotlin classes that use delegation.
 *
 * Field names must match constructor param names.
 *
 * @param clazz: the class we want to convert
 */
@JvmOverloads
fun createConstructorCallingConverter(
    classes: List<Class<*>>,
    mapper: Mapper,
    reflectionProvider: ReflectionProvider,
    excludedTypes: List<Class<*>> = listOf()
): ReflectionConverter {
    return object : ReflectionConverter(mapper, reflectionProvider) {

        override fun marshal(source: Any, writer: HierarchicalStreamWriter, context: MarshallingContext) {
            (source::class.allSuperclasses + source::class)
                .map { it.declaredMemberProperties }
                .flatten()
                .filter { it.javaField?.isTransient() == false }
                .forEach { property ->
                    // Get the value of the property and write it into xml
                    property.withTempPublicAccess { getter.call(source) }?.let { value ->
                        writer.startNode(property.name)
                        if (!isXStreamBasicType(value)) {
                            // xstream expects these class annotations
                            writer.addAttribute("class", value::class.java.name)
                        }
                        context.convertAnother(value)
                        writer.endNode()
                    }
                }
        }

        private fun isXStreamBasicType(value: Any): Boolean {
            return when(value) {
                is Int, is Char, is Boolean, is Byte, is Short, is Long, is Float, is Double, is String, is Enum<*> -> true
                else -> false
            }
        }

        @OptIn(ExperimentalStdlibApi::class)
        override fun unmarshal(reader: HierarchicalStreamReader, context: UnmarshallingContext): Any {

            // Get a class from an xml node
            @Suppress("UNCHECKED_CAST")
            val cls: KClass<*> = (try {
                Class.forName(reader.nodeName).kotlin
            } catch (e: ClassNotFoundException) {
                Class.forName(reader.getAttribute("class")).kotlin
            })

            // Map from variable names to corresponding Kotlin properties.
            // Ex: activation -> Neuron::activation
            val propertyMap = (listOf(cls) + cls.allSuperclasses)
                .map { it.declaredMemberProperties }
                .flatten()
                .groupBy { it.name }
                .map { (name, properties) -> name to properties.first() }
                .toMap()

            // Map from names to values. Ex: activation -> 1.0
            val propertyNameToDeserializedValueMap = HashMap<String, Any?>()

            fun read() {
                val nodeName = reader.nodeName
                propertyMap[nodeName]?.let {
                    propertyNameToDeserializedValueMap[nodeName] = if (reader.getAttribute("class") != null) {
                        context.convertAnother(reader.value, Class.forName(reader.getAttribute("class")))
                    } else {
                        context.convertAnother(reader.value, it.returnType.javaType as Class<*>)
                    }
                }
            }

            fun stepIn() {
                read()
                while (reader.hasMoreChildren()) {
                    reader.moveDown()
                    stepIn()
                    reader.moveUp()
                }
            }
            stepIn()

            // The object we get from calling the constructor
            val convertedObject = if (cls.objectInstance != null) {
                cls.objectInstance!!
            } else {
                // The constructor used to create the object
                val constructor = cls.constructors
                    .firstOrNull { it.hasAnnotation<XStreamConstructor>() }
                    ?: cls.primaryConstructor
                    ?: cls.constructors.firstOrNull { it.parameters.none { p -> !p.isOptional } } // no arg constructor
                    ?: throw IllegalArgumentException("Class $cls does not have a primary constructor or a no arg constructor.")

                val parameterNamesFromAnnotation = constructor.findAnnotation<XStreamConstructor>()?.names?.toList()

                val paramNameToParamMap = if (!parameterNamesFromAnnotation.isNullOrEmpty()) {
                    (parameterNamesFromAnnotation zip constructor.parameters).toMap()
                } else {
                    constructor.parameters.associateBy { it.name }
                }

                val paramToValueMap = paramNameToParamMap.entries
                    .map { (name, param) -> param to propertyNameToDeserializedValueMap[name] }
                    // if the parameter is optional, don't include the null value (to deal with properties that are transient)
                    .filter { (param, value) -> !param.isOptional || value != null }
                    .toMap()

                constructor.withTempPublicAccess {
                    callBy(paramToValueMap)
                }
            }

            propertyNameToDeserializedValueMap.forEach { (name, value) ->
                propertyMap[name]?.let { property ->
                    if (property is KMutableProperty<*>) {
                        // property is a var
                        val oldAccessible = property.isAccessible
                        property.isAccessible = true
                        property.setter.call(convertedObject, value)
                        property.isAccessible = oldAccessible
                    } else {
                        // property is a val
                        property.javaField?.let { field ->
                            val oldAccessible = field.isAccessible
                            field.isAccessible = true
                            field.set(convertedObject, value)
                            field.isAccessible = oldAccessible
                        } ?: throw IllegalArgumentException("Property $property for class ${cls.simpleName} does not have a backing field.")
                    }
                }
            }

            return convertedObject
        }

        override fun canConvert(type: Class<*>?): Boolean {
            if (classes.any { it.kotlin == type }) return true
            if (excludedTypes.contains(type)) return false
            if (type?.isKotlinClass() == false) return false
            return try {
                classes.any { clazz -> type?.kotlin?.isSubclassOf(clazz.kotlin) == true }
            } catch (e: Error) {
                false
            }
        }
    }
}