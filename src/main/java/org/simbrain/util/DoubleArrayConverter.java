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
 * Save double arrays in a user readable format for shorter arrays and Base64 encodings for large arrays..
 */
public class DoubleArrayConverter implements Converter {

    /**
     * Arrays whose length is above this this threshold will be stored as base 64.
     */
    private static final int compressionThreshold = 100;

    /**
     * Precision to use for shorter arrays.
     */
    private static final int precision = 5;

    @Override
    public boolean canConvert(Class type) {
        return type == double[].class;
    }

    @Override
    public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
        double[] array = (double[]) source;
        context.convertAnother(arrayToString(array));
    }

    @Override
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
        String str = reader.getValue();
        return stringToArray(str);
    }

    /**
     * For arrays below compression threshold return a json-style array string [1,2,3,...].  Otherwise
     * return a base64 encoding of the string.
     */
    public static String arrayToString(double[] array) {
        if (array.length < compressionThreshold) {
            return "[" + Utils.doubleArrayToString(array, precision) + "]";
        } else {
            return Base64.getEncoder().encodeToString(doubleArrayToByteArray(array));
        }
    }

    /**
     * Converts a string representation produced by {@link #arrayToString(double[])} back to a double array.
     */
    public static double[] stringToArray(String str) {
        if (str.startsWith("[")) {
            return Utils.parseVectorString(str.substring(1, str.length()-1));
        } else {
            return byteArrayToDoubleArray(Base64.getDecoder().decode(str));
        }
    }

    /**
     * https://stackoverflow.com/questions/41990732/how-to-convert-double-array-to-base64-string-and-vice-versa-in-java
     */
    public static byte[] doubleArrayToByteArray(double[] doubleArray) {
        ByteBuffer buf = ByteBuffer.allocate(Double.SIZE / Byte.SIZE * doubleArray.length);
        buf.asDoubleBuffer().put(doubleArray);
        return buf.array();
    }

    /**
     * https://stackoverflow.com/questions/41990732/how-to-convert-double-array-to-base64-string-and-vice-versa-in-java
     */
    public static double[] byteArrayToDoubleArray(byte[] bytes) {
        DoubleBuffer buf = ByteBuffer.wrap(bytes).asDoubleBuffer();
        double[] doubleArray = new double[buf.limit()];
        buf.get(doubleArray);
        return doubleArray;
    }

}
