package org.simbrain.world.imageworld.serialization;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class BufferedImageConverter implements Converter {

    @Override
    public boolean canConvert(Class type) {
        return type.equals(BufferedImage.class);
    }

    @Override
    public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
        BufferedImage image = (BufferedImage) source;

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] bytes = new byte[] {};
        try {
            ImageIO.write(image, "png", outputStream);
            bytes = outputStream.toByteArray();
        } catch (IOException ex) {
            // Do Nothing
        }
        writer.startNode("png");
        context.convertAnother(bytes);
        writer.endNode();
    }

    @Override
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
        reader.moveDown();
        byte[] bytes = (byte[]) context.convertAnother(null, byte[].class);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
        reader.moveUp();

        try {
            return ImageIO.read(inputStream);
        } catch (IOException ex) {
            // Do nothing
        }
        return null;
    }

}
