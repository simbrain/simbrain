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
import org.simbrain.network.core.XStreamConstructor
import java.lang.reflect.Modifier
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
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
    }
}

/**
 * XStream support for classes that require a primary constructor call.
 *
 * Limitation: Field names must match primary constructor param names.
 * Untested on Java classes.
 */
@JvmOverloads
fun <T : Any> createConstructorCallingConverter(
    clazz: Class<T>,
    mapper: Mapper,
    reflectionProvider: ReflectionProvider,
    excludedTypes: List<Class<*>> = listOf()
): ReflectionConverter {
    return object : ReflectionConverter(mapper, reflectionProvider) {

        override fun marshal(source: Any, writer: HierarchicalStreamWriter, context: MarshallingContext) {
            source::class.memberProperties
                .filter { it.javaField?.modifiers?.let { mod -> Modifier.isTransient(mod) } != true }
                .forEach { property ->
                    property.withTempPublicAccess { getter.call(source) }?.let { value ->
                        writer.startNode(property.name)
                        if (!isXStreamBasicType(value)) {
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

            // cls can be a subclass of clazz
            @Suppress("UNCHECKED_CAST") // checked by canConvert
            val cls: KClass<out T> = (try {
                Class.forName(reader.nodeName).kotlin
            } catch (e: ClassNotFoundException) {
                Class.forName(reader.getAttribute("class")).kotlin
            }) as KClass<out T>

            // Kotlin Properties
            val propertyMap =
                cls.memberProperties
                    .associateBy { it.name }
            val propertyValueMap = HashMap<String, Any?>()

            fun read() {
                val nodeName = reader.nodeName
                propertyMap[nodeName]?.let {
                    propertyValueMap[nodeName] = if (reader.getAttribute("class") != null) {
                        context.convertAnother(reader.value, Class.forName(reader.getAttribute("class")))
                    } else {
                        context.convertAnother(reader.value, it.returnType.javaType as Class<*>)
                    }
                }
            }

            fun stepIn() {
                if (!reader.nodeName.startsWith("\$\$delegate")) {
                    read()
                }
                while (reader.hasMoreChildren()) {
                    reader.moveDown()
                    stepIn()
                    reader.moveUp()
                }
            }
            stepIn()

            val unmarshallingObject = if (cls.objectInstance != null) {
                cls.objectInstance!!
            } else {
                val primaryConstructor = cls.constructors
                    .firstOrNull { it.hasAnnotation<XStreamConstructor>() }
                    ?: cls.primaryConstructor
                    ?: throw IllegalArgumentException("Class $cls does not have a primary constructor.")

                primaryConstructor.let { constructor ->
                    val paramNames = constructor.parameters
                    val paramValues = paramNames.associateWith { param -> param.name?.let { propertyValueMap[it] } }
                        .filterValues { it != null }
                    constructor.callBy(paramValues)
                }
            }

            propertyValueMap.forEach { (name, value) ->
                propertyMap[name]?.let { property ->
                    if (property is KMutableProperty<*>) {
                        val oldAccessible = property.isAccessible
                        property.isAccessible = true
                        property.setter.call(unmarshallingObject, value)
                        property.isAccessible = oldAccessible
                    }
                }
            }

            return unmarshallingObject
        }

        override fun canConvert(type: Class<*>?): Boolean {
            if (excludedTypes.contains(type)) return false
            if (type?.isKotlinClass() == false) return false
            return try {
                type?.kotlin?.isSubclassOf(clazz.kotlin) == true
            } catch (e: Error) {
                false
            }
        }
    }
}