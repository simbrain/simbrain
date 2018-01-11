package org.simbrain.world.threedworld.entities;

import org.simbrain.world.threedworld.engine.ThreeDEngine;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

public class ModelEntityXmlConverter implements Converter {
    @Override
    public boolean canConvert(Class type) {
        return type.equals(ModelEntity.class);
    }

    @Override
    public void marshal(Object value, HierarchicalStreamWriter writer, MarshallingContext context) {
        ModelEntity model = (ModelEntity) value;

        writer.startNode("engine");
        context.convertAnother(model.getEngine());
        writer.endNode();

        writer.startNode("name");
        writer.setValue(model.getName());
        writer.endNode();

        writer.startNode("fileName");
        writer.setValue(model.getFileName());
        writer.endNode();

        writer.startNode("position");
        context.convertAnother(model.getPosition());
        writer.endNode();

        writer.startNode("rotation");
        context.convertAnother(model.getRotation());
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
        String fileName = reader.getValue();
        reader.moveUp();

        reader.moveDown();
        Vector3f position = (Vector3f) context.convertAnother(null, Vector3f.class);
        reader.moveUp();

        reader.moveDown();
        Quaternion rotation = (Quaternion) context.convertAnother(null, Quaternion.class);
        reader.moveUp();

        ModelEntity model = ModelEntity.load(engine, name, fileName);
        model.setPosition(position);
        model.setRotation(rotation);
        return model;
    }

}
