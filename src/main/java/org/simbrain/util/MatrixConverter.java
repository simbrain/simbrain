package org.simbrain.util;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import smile.math.matrix.Matrix;

import java.util.Base64;

/**
 * Sae Smile Matrices as base 64 byte-streams from double arrays.
 */
public class MatrixConverter implements Converter {

    @Override
    public boolean canConvert(Class type) {
        return type == Matrix.class;
    }

    @Override
    public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
        Matrix matrix =  ((Matrix) source);

        writer.startNode("rows");
        context.convertAnother(matrix.nrows());
        writer.endNode();

        writer.startNode("cols");
        context.convertAnother(matrix.ncols());
        writer.endNode();

        writer.startNode("data");
        context.convertAnother(Base64.getEncoder().encodeToString(DoubleArrayConverter.toByteArray(matrix.toArray())));
        writer.endNode();
    }

    @Override
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
        reader.moveDown();
        int rows = Integer.parseInt(reader.getValue());
        reader.moveUp();

        reader.moveDown();
        int cols = Integer.parseInt(reader.getValue());
        reader.moveUp();

        reader.moveDown();
        // TODO: Not yet working. Temporary code as a base for the working version.
        double[] flatData = DoubleArrayConverter
                .toDoubleArray(Base64.getDecoder().decode(reader.getValue()));
        reader.moveUp();

        return new Matrix(flatData, rows, cols);
    }

}
