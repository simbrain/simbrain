package org.simbrain.world.imageworld.serialization;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

import java.nio.ByteBuffer;

public class CouplingArrayConverter implements Converter {

    @Override
    public boolean canConvert(Class type) {
        return type.equals(double[][].class) || type.equals(int[].class);
    }

    @Override
    public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
        if (source instanceof double[][]) {
            double[][] data = (double[][]) source;
            writer.startNode("channels");
            context.convertAnother(data.length);
            writer.endNode();
            for (int i = 0; i < data.length; ++i) {
                double[] channel = data[i];
                ByteBuffer buffer = ByteBuffer.allocate(channel.length * 8);
                for (double value : channel) {
                    buffer.putDouble(value);
                }
                writer.startNode("channel" + i);
                context.convertAnother(buffer.array());
                writer.endNode();
            }
        } else if (source instanceof int[]) {
            writer.startNode("int");
            writer.endNode();
            int[] data = (int[]) source;
            ByteBuffer buffer = ByteBuffer.allocate(data.length * 4);
            for (int value : data) {
                buffer.putInt(value);
            }
            writer.startNode("data");
            context.convertAnother(buffer.array());
            writer.endNode();
        }
    }

    @Override
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
        reader.moveDown();
        if (reader.getNodeName().equals("channels")) {
            int channels = (int) context.convertAnother(null, int.class);
            reader.moveUp();
            double[][] data = new double[channels][];
            for (int i = 0; i < channels; ++i) {
                reader.moveDown();
                byte[] bytes = (byte[]) context.convertAnother(null, byte[].class);
                reader.moveUp();
                ByteBuffer buffer = ByteBuffer.wrap(bytes);
                int length = buffer.remaining() / 8;
                data[i] = new double[length];
                for (int j = 0; j < length; ++j) {
                    data[i][j] = buffer.getDouble();
                }
            }
            return data;
        } else if (reader.getNodeName().equals("int")) {
            reader.moveUp();
            reader.moveDown();
            byte[] bytes = (byte[]) context.convertAnother(null, byte[].class);
            reader.moveUp();

            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            int length = buffer.remaining() / 4;
            int[] data = new int[length];
            for (int i = 0; i < length; ++i) {
                data[i++] = buffer.getInt();
            }
            return data;
        }
        return null;
    }

}
