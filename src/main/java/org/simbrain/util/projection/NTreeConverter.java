package org.simbrain.util.projection;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

import java.util.ArrayList;
import java.util.List;

public class NTreeConverter implements Converter {

    @Override
    public boolean canConvert(Class type) {
        return type == NTree.class;
    }

    @Override
    public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
        NTree ntree =  ((NTree) source);

        writer.startNode("dimensions");
        context.convertAnother(ntree.dimensions);
        writer.endNode();

        writer.startNode("datapoints");
        context.convertAnother(ntree.asArrayList());
        writer.endNode();

    }

    @Override
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
        reader.moveDown();
        int dims = Integer.parseInt(reader.getValue());
        reader.moveUp();

        reader.moveDown();
        List<DataPoint> datapoints = (List<DataPoint>) context.convertAnother(reader.getValue(), ArrayList.class);
        reader.moveUp();

        var ntree = new NTree(dims);
        datapoints.forEach(ntree::add);
        return ntree;
    }

}
