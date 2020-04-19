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
 * Custom conversion of INDArrays
 */
public class Nd4jConverter implements Converter {
    @Override
    public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
        INDArray array = (INDArray) source;
        writer.startNode("isMatrix");
        context.convertAnother(array.isMatrix());
        writer.endNode();
        writer.startNode("values");
        if(array.isMatrix()) {
            context.convertAnother(array.toFloatMatrix());
        } else {
            context.convertAnother(array.toFloatVector());
        }
        writer.endNode();
        writer.startNode("rows");
        context.convertAnother(array.rows());
        writer.endNode();
        writer.startNode("columns");
        context.convertAnother(array.columns());
        writer.endNode();
    }

    @Override
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
        Object values;
        reader.moveDown();
        boolean isMatrix = (boolean) context.convertAnother(null, boolean.class);
        reader.moveUp();
        reader.moveDown();
        if(isMatrix) {
            values = (float[][]) context.convertAnother(null, float[][].class);
        } else {
            values = (float[]) context.convertAnother(null, float[].class);
        }
        reader.moveUp();
        reader.moveDown();
        long rows = (long) context.convertAnother(null, long.class);
        reader.moveUp();
        reader.moveDown();
        long columns = (long) context.convertAnother(null, long.class);
        reader.moveUp();
        INDArray array;
        if(isMatrix) {
            array = Nd4j.createFromArray((float[][])values);
        } else {
            array = Nd4j.createFromArray((float[])values);
        }
        array.reshape(rows, columns);
        return array;
    }

    @Override
    public boolean canConvert(Class type) {
        return type.equals(INDArray.class) || type.equals(NDArray.class);
    }
}
