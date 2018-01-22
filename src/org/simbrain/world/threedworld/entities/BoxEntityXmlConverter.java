package org.simbrain.world.threedworld.entities;

import org.simbrain.world.threedworld.engine.ThreeDEngine;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

/**
 * XStream XML Converter for BoxEntity class.
 */
public class BoxEntityXmlConverter implements Converter {
    @SuppressWarnings("rawtypes") @Override
    public boolean canConvert(Class type) {
        return type.equals(BoxEntity.class);
    }

    @Override
    public void marshal(Object value, HierarchicalStreamWriter writer, MarshallingContext context) {
        BoxEntity box = (BoxEntity) value;

        writer.startNode("engine");
        context.convertAnother(box.getEngine());
        writer.endNode();

        writer.startNode("name");
        writer.setValue(box.getName());
        writer.endNode();

        writer.startNode("size");
        context.convertAnother(box.getSize());
        writer.endNode();

        writer.startNode("mass");
        context.convertAnother(box.getMass());
        writer.endNode();

        writer.startNode("material");
        writer.setValue(box.getMaterial());
        writer.endNode();

        writer.startNode("position");
        context.convertAnother(box.getPosition());
        writer.endNode();

        writer.startNode("rotation");
        context.convertAnother(box.getRotation());
        writer.endNode();
    }

    @Override
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
        reader.moveDown();
        ThreeDEngine engine = (ThreeDEngine) context.convertAnother(null, ThreeDEngine.class);
        reader.moveUp();

        reader.moveDown();
        String name = reader.getValue();
        reader.moveUp();

        reader.moveDown();
        Vector3f size = (Vector3f) context.convertAnother(null, Vector3f.class);
        reader.moveUp();

        reader.moveDown();
        float mass = (float) context.convertAnother(null, float.class);
        reader.moveUp();

        reader.moveDown();
        String material = reader.getValue();
        reader.moveUp();

        reader.moveDown();
        Vector3f position = (Vector3f) context.convertAnother(null, Vector3f.class);
        reader.moveUp();

        reader.moveDown();
        Quaternion rotation = (Quaternion) context.convertAnother(null, Quaternion.class);
        reader.moveUp();

        BoxEntity box = new BoxEntity(engine, name, size, mass, material);
        box.setPosition(position);
        box.setRotation(rotation);
        return box;
    }

}
