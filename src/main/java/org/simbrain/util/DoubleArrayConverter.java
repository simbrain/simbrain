package org.simbrain.util;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.util.Base64;

/**
 * Save double arrays as Base64 encodings.
 */
public class DoubleArrayConverter implements Converter {

    @Override
    public boolean canConvert(Class type) {
        return type == double[].class;
    }

    @Override
    public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
        double[] array = (double[]) source;
        if (array.length < 100) {
            context.convertAnother(Utils.doubleArrayToString(array));
        } else {
            context.convertAnother(Base64.getEncoder().encodeToString(doubleToByteArray(array)));
        }
    }

    @Override
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
        String stringRep = reader.getValue();
        // TODO: This is not done obviously. Need a clean way to determine storage type.
        if (stringRep.startsWith("[")) {
            return Utils.getVectorString(stringRep, ",");
        } else {
            return Base64.getDecoder().decode(reader.getValue());
        }
    }

    /**
     * https://stackoverflow.com/questions/41990732/how-to-convert-double-array-to-base64-string-and-vice-versa-in-java
     */
    private static byte[] doubleToByteArray(double[] doubleArray) {
        ByteBuffer buf = ByteBuffer.allocate(Double.SIZE / Byte.SIZE * doubleArray.length);
        buf.asDoubleBuffer().put(doubleArray);
        return buf.array();
    }

    /**
     * https://stackoverflow.com/questions/41990732/how-to-convert-double-array-to-base64-string-and-vice-versa-in-java
     */
    private static double[] byteToDoubleArray(byte[] bytes) {
        DoubleBuffer buf = ByteBuffer.wrap(bytes).asDoubleBuffer();
        double[] doubleArray = new double[buf.limit()];
        buf.get(doubleArray);
        return doubleArray;
    }


}
