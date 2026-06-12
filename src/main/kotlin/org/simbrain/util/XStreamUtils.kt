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
import org.simbrain.network.core.NetworkModel
import org.simbrain.network.core.XStreamConstructor
import org.simbrain.network.trainers.TrainingDataset
import org.simbrain.util.piccolo.Tile
import org.simbrain.util.piccolo.TileMapLayer
import org.simbrain.util.projection.Projector
import org.simbrain.util.propertyeditor.EditableObject
import org.simbrain.util.table.BasicDataFrame
import org.simbrain.world.imageworld.ImageAlbum
import org.simbrain.world.imageworld.ImageWorld
import org.simbrain.world.odorworld.OdorWorld
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty1
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
        registerConverter(BasicDataFrameConverter())
        registerConverter(
            createConstructorCallingConverter(
                listOf(
                    EditableObject::class.java,
                    NetworkModel::class.java,
                    ImageWorld::class.java,
                    // Not an EditableObject, but uses the property-converter hook
                    // (see TrainingDataset companion) to compact-encode `inputs`/`targets`.
                    TrainingDataset::class.java,
                ),
                mapper,
                reflectionProvider,
                excludedTypes = listOf(
                    Network::class.java,
                    OdorWorld::class.java,
                    Tile::class.java,
                    TileMapLayer::class.java,
                    Projector::class.java,
                    BasicDataFrame::class.java,  // Exclude since we have custom converter
                    ImageAlbum::class.java       // Exclude to use default XStream converter
                )
            )
        )
    }
}

/**
 * If [obj]'s class declares a `readResolve()` method, invoke it (via reflection, ignoring
 * access modifiers) and return whatever it returns. Otherwise return [obj] unchanged.
 *
 * Mirrors the behavior of `ObjectInputStream.readObject` for the standard Java
 * serialization protocol — only the runtime class's own declared method is considered,
 * not inherited ones, matching JLS rules.
 */
private fun invokeReadResolveIfPresent(obj: Any): Any {
    return try {
        val method = obj::class.java.getDeclaredMethod("readResolve")
        method.isAccessible = true
        method.invoke(obj) ?: obj
    } catch (_: NoSuchMethodException) {
        obj
    } catch (e: Exception) {
        System.err.println("readResolve on ${obj::class.java.name} threw: ${e.message}")
        obj
    }
}

/**
 * XStream support for Kotlin classes that require a constructor call. Which constructor to use can be specified by
 * [XStreamConstructor].
 *
 * Must be used to properly serialize Kotlin classes that use delegation.
 *
 * Field names must match constructor param names.
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
            val customMarshaller = (source::class.companionObjectInstance as? WithXStreamPropertyConverter)
                ?.xStreamPropertyConverter
                ?.createMarshaller()

            (listOf(source::class) +  source::class.allSuperclasses)
                .flatMap { it.declaredMemberProperties }
                .filter { it.shouldSerialize() }
                .forEach { property ->
                    // invoke the custom marshaller if it exists
                    val processedByCustomMarshaller = customMarshaller?.invoke(source, property, writer, context) == true
                    if (!processedByCustomMarshaller) {
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
        }

        private fun isXStreamBasicType(value: Any): Boolean {
            return when (value) {
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

            if (cls.objectInstance != null) {
                return cls.objectInstance!!
            }

            // Map from variable names to corresponding Kotlin properties.
            // Ex: activation -> Neuron::activation
            val propertyMap = (listOf(cls) + cls.allSuperclasses)
                .map { it.declaredMemberProperties }
                .flatten()
                .groupBy { it.name }
                // if there are multiple properties with the same name, take the one closest to the implementing class
                .map { (name, properties) -> name to properties.first() }
                .toMap()

            // Map from names to values. Ex: activation -> 1.0
            val propertyNameToDeserializedValueMap = HashMap<String, Any?>()

            val objectCompletedEvent = ConvertedObjectEvent()

            val customUnmarshaller = (cls.companionObjectInstance as? WithXStreamPropertyConverter)
                ?.xStreamPropertyConverter
                ?.createUnmarshaller(objectCompletedEvent)

            fun read() {
                val nodeName = reader.nodeName
                // originally designed to improve backward compatibility when a property was removed from a class
                // while the xml still contained it.
                // but there is a bug in the stepIn function that reads the same node twice, and this prevents the
                // second read from being processed, since it would not be in the propertyMap of the inner object,
                // unless the inner object has a same property name as the outer object. for example:
                // synapse
                //   spikeResponder (UDF)
                //     spikeResponder (ConvolvedJumpAndDecay)
                // this would cause the stepIn to keep reading the UDF node and cause a stack overflow.
                propertyMap[nodeName]?.let {
                    if (customUnmarshaller?.invoke(reader, context) == true) return
                    propertyNameToDeserializedValueMap[nodeName] = if (reader.getAttribute("class") != null) {
                        context.convertAnother(reader.value, Class.forName(reader.getAttribute("class")))
                    } else {
                        // For parameterized property types (e.g. `List<String>?`) the javaType is a
                        // ParameterizedType, not a Class. Unwrap to the raw class — XStream's
                        // default converters can still deserialize children using that as the hint.
                        val javaType = it.returnType.javaType
                        val targetClass: Class<*> = when (javaType) {
                            is Class<*> -> javaType
                            is java.lang.reflect.ParameterizedType -> javaType.rawType as Class<*>
                            else -> throw IllegalStateException("Cannot resolve target class for $nodeName (type $javaType)")
                        }
                        context.convertAnother(reader.value, targetClass)
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
                        }
                            ?: throw IllegalArgumentException("Property $property for class ${cls.simpleName} does not have a backing field.")
                    }
                }
            }

            objectCompletedEvent.objectCompleted.fireAndBlock(convertedObject)

            // XStream's standard ReflectionConverter invokes readResolve via Java's
            // serialization machinery, but our custom unmarshal bypasses that path.
            // Invoke it explicitly so classes can re-wire transient listeners, recreate
            // coroutine scopes, etc. — without this, anything depending on readResolve
            // (event listeners, channels, etc.) silently fails after deserialization.
            return invokeReadResolveIfPresent(convertedObject)
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


/**
 * Allows a subset of properties to be marshalled and unmarshalled by custom converters.
 * Useful when the properties have references to an instance of this class that is not yet fully constructed.
 *
 * For example: TimeSeries has a reference to its parent TimeSeriesModel, which is not yet fully constructed when
 * the TimeSeries list is being unmarshalled.
 *
 * Can only be used in classes using [createConstructorCallingConverter].
 */
interface WithXStreamPropertyConverter {
    val xStreamPropertyConverter: XStreamPropertyConverter
}

class XStreamPropertyConverter(
    private val marshaller: XStreamPropertyConverterMarshallingContext<*>.() -> Unit,
    private val unmarshaller: XStreamPropertyConverterUnmarshallingContext<*>.() -> Any
) {

    /**
     * Returns a function to be used by the XStream converter to marshal the properties of a class.
     */
    fun createMarshaller(): (source: Any, property: KProperty1<*, *>, writer: HierarchicalStreamWriter, context: MarshallingContext) -> Boolean {
        val postprocessorContext = XStreamPropertyConverterMarshallingContext<Any>()
        postprocessorContext.marshaller()
        return { source, property, writer, context ->
            postprocessorContext.propertyToActionMap[property]?.let { action ->
                action(property.getter.call(source) as Any, writer, context)
                true
            } ?: false
        }
    }

    /**
     * Returns a function to be used by the XStream converter to unmarshal the properties of a class.
     */
    fun createUnmarshaller(objectCompletedEvent: ConvertedObjectEvent): (reader: HierarchicalStreamReader, context: UnmarshallingContext) -> Boolean {
        val postprocessorContext = XStreamPropertyConverterUnmarshallingContext<Any>(objectCompletedEvent)
        postprocessorContext.unmarshaller()
        return { reader, context ->
            postprocessorContext.nodeNameToActionMap[reader.nodeName]?.let { action ->
                action(
                    reader,
                    context
                ); true
            } ?: false
        }
    }
}

class XStreamPropertyConverterMarshallingContext<T> {

    val propertyToActionMap = HashMap<KProperty1<T, *>, Any.(writer: HierarchicalStreamWriter, context: MarshallingContext) -> Unit>()

    fun <P : Any> on(property: KProperty1<T, P>, block: P.(writer: HierarchicalStreamWriter, context: MarshallingContext) -> Unit) {
        propertyToActionMap[property] = block as Any.(HierarchicalStreamWriter, MarshallingContext) -> Unit
    }
}

class XStreamPropertyConverterUnmarshallingContext<T>(private val objectCompletedEvent: ConvertedObjectEvent) {

    val nodeNameToActionMap = HashMap<String, (reader: HierarchicalStreamReader, context: UnmarshallingContext) -> Unit>()

    fun on(
        propertyNodeName: String,
        block: (reader: HierarchicalStreamReader, context: UnmarshallingContext) -> Unit
    ) {
        nodeNameToActionMap[propertyNodeName] = block
    }

    fun withConstructedObject(block: T.() -> Unit) {
        objectCompletedEvent.objectCompleted.on {
            (it as T).block()
        }
    }
}

fun <T> createXStreamPropertyConverter(
    marshal: XStreamPropertyConverterMarshallingContext<T>.() -> Unit,
    unmarshal: XStreamPropertyConverterUnmarshallingContext<T>.() -> Unit
): XStreamPropertyConverter {
    return XStreamPropertyConverter(
        { (this as XStreamPropertyConverterMarshallingContext<T>).marshal() },
        { (this as XStreamPropertyConverterUnmarshallingContext<T>).unmarshal() }
    )
}

class ConvertedObjectEvent: FlowEvents() {
    val objectCompleted = AwaitableEvent<Any>()
}

/**
 * Custom converter for BasicDataFrame to avoid AbstractTableModel serialization issues
 */
class BasicDataFrameConverter : com.thoughtworks.xstream.converters.Converter {
    
    override fun canConvert(type: Class<*>?): Boolean {
        return type == BasicDataFrame::class.java
    }
    
    override fun marshal(source: Any, writer: HierarchicalStreamWriter, context: MarshallingContext) {
        val dataFrame = source as BasicDataFrame
        
        // Serialize data with explicit type information to preserve strings
        writer.startNode("data")
        writer.startNode("rows")
        for (row in dataFrame.data) {
            writer.startNode("row")
            for (item in row) {
                writer.startNode("item")
                writer.addAttribute("type", item?.javaClass?.simpleName ?: "null")
                if (item != null) {
                    writer.setValue(item.toString())
                }
                writer.endNode()
            }
            writer.endNode()
        }
        writer.endNode()
        writer.endNode()
        
        writer.startNode("columns")
        context.convertAnother(dataFrame.columns)
        writer.endNode()
        
        writer.startNode("currentRowIndex")
        context.convertAnother(dataFrame.currentRowIndex)
        writer.endNode()
        
        writer.startNode("rowNames")
        context.convertAnother(dataFrame.rowNames)
        writer.endNode()
    }
    
    override fun unmarshal(reader: HierarchicalStreamReader, context: UnmarshallingContext): Any {
        var data: MutableList<MutableList<Any?>>? = null
        var columns: MutableList<org.simbrain.util.table.Column>? = null
        var currentRowIndex = 0
        var rowNames: List<String?> = emptyList()
        
        while (reader.hasMoreChildren()) {
            reader.moveDown()
            when (reader.nodeName) {
                "data" -> {
                    // Parse custom data format with type preservation
                    data = mutableListOf()
                    reader.moveDown() // Move to "rows"
                    while (reader.hasMoreChildren()) {
                        reader.moveDown() // Move to "row"
                        val row = mutableListOf<Any?>()
                        while (reader.hasMoreChildren()) {
                            reader.moveDown() // Move to "item"
                            val typeAttr = reader.getAttribute("type")
                            val value = reader.value
                            
                            val convertedValue = when (typeAttr) {
                                "String" -> value
                                "Integer" -> value?.toIntOrNull()
                                "Double" -> value?.toDoubleOrNull()
                                "Float" -> value?.toFloatOrNull()
                                "Long" -> value?.toLongOrNull()
                                "Boolean" -> value?.toBooleanStrictOrNull()
                                "null" -> null
                                else -> value // Default to string if unknown type
                            }
                            row.add(convertedValue)
                            reader.moveUp() // Back from "item"
                        }
                        data.add(row)
                        reader.moveUp() // Back from "row"
                    }
                    reader.moveUp() // Back from "rows"
                }
                "columns" -> {
                    @Suppress("UNCHECKED_CAST")
                    columns = context.convertAnother(null, MutableList::class.java) as MutableList<org.simbrain.util.table.Column>
                }
                "currentRowIndex" -> {
                    currentRowIndex = context.convertAnother(null, Int::class.java) as Int
                }
                "rowNames" -> {
                    @Suppress("UNCHECKED_CAST")
                    rowNames = context.convertAnother(null, List::class.java) as List<String?>
                }
            }
            reader.moveUp()
        }

        val dataFrame = BasicDataFrame(emptyList(), mutableListOf())
        dataFrame.data = data ?: mutableListOf()
        dataFrame.columns = columns ?: mutableListOf()
        // Set properties directly to avoid triggering any conversion
        dataFrame.currentRowIndex = currentRowIndex
        dataFrame.rowNames = rowNames
        
        return dataFrame
    }
}
