package org.simbrain.plot

import com.thoughtworks.xstream.converters.Converter
import com.thoughtworks.xstream.converters.MarshallingContext
import com.thoughtworks.xstream.converters.UnmarshallingContext
import com.thoughtworks.xstream.io.HierarchicalStreamReader
import com.thoughtworks.xstream.io.HierarchicalStreamWriter
import org.jfree.data.xy.XYSeries
import org.simbrain.util.DoubleArrayConverter

class XYSeriesConverter: Converter {

    override fun canConvert(cls: Class<*>): Boolean {
        return cls == XYSeries::class.java
    }

    override fun marshal(source: Any, writer: HierarchicalStreamWriter, context: MarshallingContext) {
        source as XYSeries

        writer.startNode("description")
        context.convertAnother(source.description)
        writer.endNode()

        writer.startNode("indices")
        val array = source.toArray()
        context.convertAnother(array[0])
        writer.endNode()

        writer.startNode("values")
        context.convertAnother(array[1])
        writer.endNode()
    }

    override fun unmarshal(reader: HierarchicalStreamReader, context: UnmarshallingContext): Any {
        reader.moveDown()
        val description = reader.value;
        reader.moveUp()

        reader.moveDown()
        val indices = DoubleArrayConverter.stringToArray(reader.value);
        reader.moveUp()

        reader.moveDown()
        val values = DoubleArrayConverter.stringToArray(reader.value);
        reader.moveUp()

        var series = XYSeries(description)
        series.description = description
        indices.zip(values).forEach { series.add(it.first, it.second) }
        return series
    }
}