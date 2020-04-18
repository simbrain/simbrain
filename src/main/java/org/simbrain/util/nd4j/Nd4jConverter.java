package org.simbrain.util.nd4j;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.cpu.nativecpu.NDArray;
import org.nd4j.linalg.factory.Nd4j;

/**
 * Custon conversion of INDArrays
 */
public class Nd4jConverter implements Converter {
    @Override
    public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
        INDArray array = (INDArray) source;
        writer.startNode("values");
        context.convertAnother(array.toFloatVector());
        writer.endNode();
        writer.startNode("shape");
        context.convertAnother(array.shape());
        writer.endNode();
    }

    @Override
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
        reader.moveDown();
        float[] values = (float[]) context.convertAnother(null, float[].class);
        reader.moveUp();
        reader.moveDown();
        long[] shape = (long[]) context.convertAnother(null, long[].class);
        reader.moveUp();

        INDArray array = Nd4j.createFromArray(values);
        array.reshape(shape);
        return array;
    }

    @Override
    public boolean canConvert(Class type) {
        return type.equals(INDArray.class) || type.equals(NDArray.class);
    }
}
