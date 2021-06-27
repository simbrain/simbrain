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
        // TODO: Finish this. Make it configurable.
        //  Ability to send degree of precision and threshold.
        // if (array.length < 100) {
        //     context.convertAnother(Utils.doubleArrayToString(array));
        context.convertAnother(Base64.getEncoder().encodeToString(toByteArray(array)));
    }

    @Override
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
        return toDoubleArray(Base64.getDecoder().decode(reader.getValue()));
    }

    /**
     * https://stackoverflow.com/questions/41990732/how-to-convert-double-array-to-base64-string-and-vice-versa-in-java
     */
    public static byte[] toByteArray(double[] doubleArray) {
        ByteBuffer buf = ByteBuffer.allocate(Double.SIZE / Byte.SIZE * doubleArray.length);
        buf.asDoubleBuffer().put(doubleArray);
        return buf.array();
    }

    public static byte[] toByteArray(double[][] doubleArray) {
        ByteBuffer buf = ByteBuffer.allocate(Double.SIZE / Byte.SIZE * doubleArray.length * doubleArray[0].length);
        for (int i = 0; i < doubleArray.length; i++) {
            buf.put(toByteArray(doubleArray[i]));
        }
        return buf.array();
    }

    /**
     * https://stackoverflow.com/questions/41990732/how-to-convert-double-array-to-base64-string-and-vice-versa-in-java
     */
    public static double[] toDoubleArray(byte[] bytes) {
        DoubleBuffer buf = ByteBuffer.wrap(bytes).asDoubleBuffer();
        double[] doubleArray = new double[buf.limit()];
        buf.get(doubleArray);
        return doubleArray;
    }

}
