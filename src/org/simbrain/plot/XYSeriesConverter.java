package org.simbrain.plot;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import org.jfree.data.xy.XYSeries;
import org.simbrain.util.DoubleArrayConverter;

import java.util.Objects;

public class XYSeriesConverter implements Converter {

    @Override
    public boolean canConvert(Class type) {
        return type == XYSeries.class;
    }

    @Override
    public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {

        writer.startNode("key");
        context.convertAnother( ((XYSeries) source).getKey());
        writer.endNode();

        var desc =  ((XYSeries) source).getDescription();
        writer.startNode("description");
        context.convertAnother(Objects.requireNonNullElse(desc, ""));
        writer.endNode();

        writer.startNode("indices");
        var array = ((XYSeries) source).toArray();
        context.convertAnother(array[0]);
        writer.endNode();

        writer.startNode("values");
        context.convertAnother(array[1]);
        writer.endNode();
    }

    @Override
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {

        reader.moveDown();
        var key = reader.getValue();
        reader.moveUp();

        reader.moveDown();
        var description = reader.getValue();
        reader.moveUp();

        reader.moveDown();
        var indices = DoubleArrayConverter.stringToArray(reader.getValue());
        reader.moveUp();

        reader.moveDown();
        var values = DoubleArrayConverter.stringToArray(reader.getValue());
        reader.moveUp();

        var series = new XYSeries(key);
        series.setDescription(description);
        for (int i = 0; i < indices.length; i++) {
            series.add(indices[i], values[i]);
        }
        return series;
    }
}